package net.sourceforge.filebot.ui.rename;

import static javax.swing.JOptionPane.*;
import static javax.swing.SwingUtilities.*;
import static net.sourceforge.filebot.ui.NotificationLogging.*;
import static net.sourceforge.tuned.ExceptionUtilities.*;
import static net.sourceforge.tuned.ui.LoadingOverlayPane.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.History;
import net.sourceforge.filebot.HistorySpooler;
import net.sourceforge.filebot.Language;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.StandardRenameAction;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.format.MediaBindingBean;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.ui.rename.FormatDialog.Mode;
import net.sourceforge.filebot.ui.rename.RenameModel.FormattedFuture;
import net.sourceforge.filebot.web.AudioTrack;
import net.sourceforge.filebot.web.AudioTrackFormat;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeFormat;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.Movie;
import net.sourceforge.filebot.web.MovieFormat;
import net.sourceforge.filebot.web.MovieIdentificationService;
import net.sourceforge.filebot.web.MusicIdentificationService;
import net.sourceforge.filebot.web.SortOrder;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;
import net.sourceforge.tuned.ui.ActionPopup;
import net.sourceforge.tuned.ui.LoadingOverlayPane;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class RenamePanel extends JComponent {

	protected final RenameModel renameModel = new RenameModel();

	protected final RenameList<FormattedFuture> namesList = new RenameList<FormattedFuture>(renameModel.names());

	protected final RenameList<File> filesList = new RenameList<File>(renameModel.files());

	protected final MatchAction matchAction = new MatchAction(renameModel);

	protected final RenameAction renameAction = new RenameAction(renameModel);

	private static final PreferencesEntry<String> persistentEpisodeFormat = Settings.forPackage(RenamePanel.class).entry("rename.format.episode");
	private static final PreferencesEntry<String> persistentMovieFormat = Settings.forPackage(RenamePanel.class).entry("rename.format.movie");
	private static final PreferencesEntry<String> persistentMusicFormat = Settings.forPackage(RenamePanel.class).entry("rename.format.music");

	private static final PreferencesEntry<String> persistentLastFormatState = Settings.forPackage(RenamePanel.class).entry("rename.last.format.state");
	private static final PreferencesEntry<String> persistentPreferredLanguage = Settings.forPackage(RenamePanel.class).entry("rename.language").defaultValue("en");
	private static final PreferencesEntry<String> persistentPreferredEpisodeOrder = Settings.forPackage(RenamePanel.class).entry("rename.episode.order").defaultValue("Airdate");

	public RenamePanel() {
		namesList.setTitle("New Names");
		namesList.setTransferablePolicy(new NamesListTransferablePolicy(renameModel.values()));

		filesList.setTitle("Original Files");
		filesList.setTransferablePolicy(new FilesListTransferablePolicy(renameModel.files()));

		// filename formatter
		renameModel.useFormatter(File.class, new FileNameFormatter(renameModel.preserveExtension()));

		// movie formatter
		renameModel.useFormatter(Movie.class, new MovieFormatter());

		try {
			// restore custom episode formatter
			renameModel.useFormatter(Episode.class, new ExpressionFormatter(persistentEpisodeFormat.getValue(), EpisodeFormat.SeasonEpisode, Episode.class));
		} catch (Exception e) {
			// illegal format, ignore
		}

		try {
			// restore custom movie formatter
			renameModel.useFormatter(Movie.class, new ExpressionFormatter(persistentMovieFormat.getValue(), MovieFormat.NameYear, Movie.class));
		} catch (Exception e) {
			// illegal format, ignore
		}

		RenameListCellRenderer cellrenderer = new RenameListCellRenderer(renameModel);

		namesList.getListComponent().setCellRenderer(cellrenderer);
		filesList.getListComponent().setCellRenderer(cellrenderer);

		EventSelectionModel<Match<Object, File>> selectionModel = new EventSelectionModel<Match<Object, File>>(renameModel.matches());
		selectionModel.setSelectionMode(ListSelection.SINGLE_SELECTION);

		// use the same selection model for both lists to synchronize selection
		namesList.getListComponent().setSelectionModel(selectionModel);
		filesList.getListComponent().setSelectionModel(selectionModel);

		// synchronize viewports
		new ScrollPaneSynchronizer(namesList, filesList);

		// delete items from both lists
		Action removeAction = new AbstractAction("Remove", ResourceManager.getIcon("dialog.cancel")) {

			@Override
			public void actionPerformed(ActionEvent e) {
				// lock cell with once user starts deleting cells (performance hack)
				setFixedCellWidth(true);

				RenameList list = null;
				boolean deleteCell;

				if (e.getSource() instanceof JButton) {
					list = filesList;
					deleteCell = isShiftOrAltDown(e);
				} else {
					list = ((RenameList) e.getSource());
					deleteCell = isShiftOrAltDown(e);
				}

				int index = list.getListComponent().getSelectedIndex();
				if (index >= 0) {
					if (deleteCell) {
						EventList eventList = list.getModel();
						if (index < eventList.size()) {
							list.getModel().remove(index);
						}
					} else {
						renameModel.matches().remove(index);
					}
					int maxIndex = list.getModel().size() - 1;
					if (index > maxIndex) {
						index = maxIndex;
					}
					if (index >= 0) {
						list.getListComponent().setSelectedIndex(index);
					}
				}
			}
		};
		namesList.setRemoveAction(removeAction);
		filesList.setRemoveAction(removeAction);

		// create Match button
		JButton matchButton = new JButton(matchAction);
		matchButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		matchButton.setHorizontalTextPosition(SwingConstants.CENTER);

		// create Rename button
		JButton renameButton = new JButton(renameAction);
		renameButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		renameButton.setHorizontalTextPosition(SwingConstants.CENTER);

		// create fetch popup
		ActionPopup fetchPopup = createFetchPopup();

		final Action fetchPopupAction = new ShowPopupAction("Fetch Data", ResourceManager.getIcon("action.fetch"));
		JButton fetchButton = new JButton(fetchPopupAction);
		filesList.getListComponent().setComponentPopupMenu(fetchPopup);
		namesList.getListComponent().setComponentPopupMenu(fetchPopup);
		fetchButton.setComponentPopupMenu(fetchPopup);
		matchButton.setComponentPopupMenu(fetchPopup);
		namesList.getButtonPanel().add(fetchButton, "gap 0");

		namesList.getListComponent().setComponentPopupMenu(fetchPopup);
		fetchButton.setComponentPopupMenu(fetchPopup);

		// settings popup and button
		ActionPopup settingsPopup = createSettingsPopup();
		final Action settingsPopupAction = new ShowPopupAction("Settings", ResourceManager.getIcon("action.settings"));
		JButton settingsButton = createImageButton(settingsPopupAction);
		settingsButton.setComponentPopupMenu(settingsPopup);
		renameButton.setComponentPopupMenu(settingsPopup);
		namesList.getButtonPanel().add(settingsButton, "gap indent");

		// open rename log button
		filesList.getButtonPanel().add(createImageButton(removeAction), "gap 0", 2);
		filesList.getButtonPanel().add(createImageButton(clearFilesAction), "gap 0");
		filesList.getButtonPanel().add(createImageButton(openHistoryAction), "gap indent");

		matchButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// show popup on actionPerformed only when names list is empty
				if (renameModel.names().isEmpty()) {
					fetchPopupAction.actionPerformed(e);
				}
			}
		});

		// reveal file location on double click
		filesList.getListComponent().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					getWindow(evt.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try {
						JList list = (JList) evt.getSource();
						if (list.getSelectedIndex() >= 0) {
							File item = (File) list.getSelectedValue();
							Desktop.getDesktop().browse(item.getParentFile().toURI());
						}
					} catch (Exception e) {
						Logger.getLogger(RenamePanel.class.getName()).log(Level.WARNING, e.getMessage());
					} finally {
						getWindow(evt.getSource()).setCursor(Cursor.getDefaultCursor());
					}
				}
			}
		});

		// reveal file location on double click
		namesList.getListComponent().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					getWindow(evt.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					try {
						JList list = (JList) evt.getSource();
						if (list.getSelectedIndex() >= 0) {
							Match<Object, File> match = renameModel.getMatch(list.getSelectedIndex());
							Map<File, Object> context = renameModel.getMatchContext();

							MediaBindingBean sample = new MediaBindingBean(match.getValue(), match.getCandidate(), context);
							showFormatEditor(sample);
						}
					} catch (Exception e) {
						Logger.getLogger(RenamePanel.class.getName()).log(Level.WARNING, e.getMessage(), e);
					} finally {
						getWindow(evt.getSource()).setCursor(Cursor.getDefaultCursor());
					}
				}
			}
		});

		setLayout(new MigLayout("fill, insets dialog, gapx 10px", "[fill][align center, pref!][fill]", "align 33%"));
		add(filesList, "grow, sizegroupx list");

		// make buttons larger
		matchButton.setMargin(new Insets(3, 14, 2, 14));
		renameButton.setMargin(new Insets(6, 11, 2, 11));

		add(matchButton, "split 2, flowy, sizegroupx button");
		add(renameButton, "gapy 30px, sizegroupx button");

		add(new LoadingOverlayPane(namesList, namesList, "32px", "30px"), "grow, sizegroupx list");
	}

	protected ActionPopup createFetchPopup() {
		final ActionPopup actionPopup = new ActionPopup("Series / Movie Data", ResourceManager.getIcon("action.fetch"));

		actionPopup.addDescription(new JLabel("Episode Mode:"));

		// create actions for match popup episode list completion
		for (EpisodeListProvider db : WebServices.getEpisodeListProviders()) {
			actionPopup.add(new AutoCompleteAction(db.getName(), db.getIcon(), new EpisodeListMatcher(db, db != WebServices.AniDB, db == WebServices.AniDB)));
		}

		actionPopup.addSeparator();
		actionPopup.addDescription(new JLabel("Movie Mode:"));

		// create action for movie name completion
		for (MovieIdentificationService it : WebServices.getMovieIdentificationServices()) {
			actionPopup.add(new AutoCompleteAction(it.getName(), it.getIcon(), new MovieHashMatcher(it)));
		}

		actionPopup.addSeparator();
		actionPopup.addDescription(new JLabel("Music Mode:"));
		for (MusicIdentificationService it : WebServices.getMusicIdentificationServices()) {
			actionPopup.add(new AutoCompleteAction(it.getName(), it.getIcon(), new AudioFingerprintMatcher(it)));
		}

		actionPopup.addSeparator();
		actionPopup.addDescription(new JLabel("Options:"));

		actionPopup.add(new AbstractAction("Edit Format", ResourceManager.getIcon("action.format")) {

			@Override
			public void actionPerformed(ActionEvent evt) {
				showFormatEditor(null);
			}
		});

		actionPopup.add(new AbstractAction("Preferences", ResourceManager.getIcon("action.preferences")) {

			@Override
			public void actionPerformed(ActionEvent evt) {
				List<Language> languages = new ArrayList<Language>();
				languages.addAll(Language.preferredLanguages()); // add preferred languages first
				languages.addAll(Language.availableLanguages()); // then others

				JComboBox orderCombo = new JComboBox(SortOrder.values());
				JList languageList = new JList(languages.toArray());
				languageList.setCellRenderer(new DefaultListCellRenderer() {

					@Override
					public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
						super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
						if (value != null) {
							setText(((Language) value).getName());
							setIcon(ResourceManager.getFlagIcon(((Language) value).getCode()));
						}
						return this;
					}
				});

				// pre-select current preferences
				try {
					orderCombo.setSelectedItem(SortOrder.forName(persistentPreferredEpisodeOrder.getValue()));
				} catch (IllegalArgumentException e) {
					// ignore
				}
				for (Language language : languages) {
					if (language.getCode().equals(persistentPreferredLanguage.getValue())) {
						languageList.setSelectedValue(language, true);
						break;
					}
				}

				JScrollPane spLanguageList = new JScrollPane(languageList);
				spLanguageList.setBorder(new CompoundBorder(new TitledBorder("Preferred Language"), spLanguageList.getBorder()));
				JScrollPane spOrderCombo = new JScrollPane(orderCombo, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				spOrderCombo.setBorder(new CompoundBorder(new TitledBorder("Preferred Episode Order"), spOrderCombo.getBorder()));

				JPanel message = new JPanel(new MigLayout("fill, flowy, insets 0"));
				message.add(spLanguageList, "grow");
				message.add(spOrderCombo, "grow, hmin 24px");
				JOptionPane pane = new JOptionPane(message, PLAIN_MESSAGE, OK_CANCEL_OPTION);
				pane.createDialog(getWindowAncestor(RenamePanel.this), "Preferences").setVisible(true);

				if (pane.getValue() != null && pane.getValue().equals(OK_OPTION)) {
					persistentPreferredLanguage.setValue(((Language) languageList.getSelectedValue()).getCode());
					persistentPreferredEpisodeOrder.setValue(((SortOrder) orderCombo.getSelectedItem()).name());
				}
			}
		});

		return actionPopup;
	}

	protected ActionPopup createSettingsPopup() {
		ActionPopup actionPopup = new ActionPopup("Rename Options", ResourceManager.getIcon("action.settings"));

		actionPopup.addDescription(new JLabel("Extension:"));
		actionPopup.add(new SetRenameMode(false, "Preserve", ResourceManager.getIcon("action.extension.preserve")));
		actionPopup.add(new SetRenameMode(true, "Override", ResourceManager.getIcon("action.extension.override")));

		actionPopup.addSeparator();

		actionPopup.addDescription(new JLabel("Action:"));
		for (StandardRenameAction action : EnumSet.of(StandardRenameAction.MOVE, StandardRenameAction.COPY, StandardRenameAction.KEEPLINK, StandardRenameAction.SYMLINK, StandardRenameAction.HARDLINK)) {
			actionPopup.add(new SetRenameAction(action, action.getDisplayName(), ResourceManager.getIcon("rename.action." + action.toString().toLowerCase())));
		}

		return actionPopup;
	}

	protected void showFormatEditor(MediaBindingBean lockOnBinding) {
		// default to Episode mode
		Mode initMode = Mode.Episode;

		if (lockOnBinding != null) {
			if (lockOnBinding.getInfoObject() instanceof Episode) {
				initMode = Mode.Episode;
			} else if (lockOnBinding.getInfoObject() instanceof Movie) {
				initMode = Mode.Movie;
			} else if (lockOnBinding.getInfoObject() instanceof AudioTrack) {
				initMode = Mode.Music;
			}
		} else {
			// restore previous mode
			try {
				initMode = Mode.valueOf(persistentLastFormatState.getValue());
			} catch (Exception e) {
				Logger.getLogger(RenamePanel.class.getName()).log(Level.WARNING, e.getMessage());
			}
		}

		FormatDialog dialog = new FormatDialog(getWindowAncestor(RenamePanel.this), initMode, lockOnBinding);
		dialog.setLocation(getOffsetLocation(dialog.getOwner()));
		dialog.setVisible(true);

		if (dialog.submit()) {
			switch (dialog.getMode()) {
			case Episode:
				renameModel.useFormatter(Episode.class, new ExpressionFormatter(dialog.getFormat().getExpression(), EpisodeFormat.SeasonEpisode, Episode.class));
				persistentEpisodeFormat.setValue(dialog.getFormat().getExpression());
				break;
			case Movie:
				renameModel.useFormatter(Movie.class, new ExpressionFormatter(dialog.getFormat().getExpression(), MovieFormat.NameYear, Movie.class));
				persistentMovieFormat.setValue(dialog.getFormat().getExpression());
				break;
			case Music:
				renameModel.useFormatter(AudioTrack.class, new ExpressionFormatter(dialog.getFormat().getExpression(), new AudioTrackFormat(), AudioTrack.class));
				persistentMusicFormat.setValue(dialog.getFormat().getExpression());
				break;
			}
			if (lockOnBinding == null) {
				persistentLastFormatState.setValue(dialog.getMode().name());
			}
		}
	}

	protected final Action clearFilesAction = new AbstractAction("Clear", ResourceManager.getIcon("action.clear")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			if (isShiftOrAltDown(evt)) {
				renameModel.files().clear();
			} else {
				renameModel.clear();
			}

			// lock cell with once user starts deleting cells (performance hack)
			setFixedCellWidth(false);
		}
	};

	protected final Action openHistoryAction = new AbstractAction("Open History", ResourceManager.getIcon("action.report")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			try {
				History model = HistorySpooler.getInstance().getCompleteHistory();

				HistoryDialog dialog = new HistoryDialog(getWindow(RenamePanel.this));
				dialog.setLocationRelativeTo(RenamePanel.this);
				dialog.setModel(model);

				// show and block
				dialog.setVisible(true);
			} catch (Exception e) {
				UILogger.log(Level.WARNING, String.format("%s: %s", getRootCause(e).getClass().getSimpleName(), getRootCauseMessage(e)), e);
			}
		}
	};

	protected static class ShowPopupAction extends AbstractAction {

		public ShowPopupAction(String name, Icon icon) {
			super(name, icon);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// display popup below component
			JComponent source = (JComponent) e.getSource();
			source.getComponentPopupMenu().show(source, -3, source.getHeight() + 4);
		}
	};

	protected class SetRenameMode extends AbstractAction {

		private final boolean activate;

		private SetRenameMode(boolean activate, String name, Icon icon) {
			super(name, icon);
			this.activate = activate;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			renameModel.setPreserveExtension(!activate);

			// use different file name formatter
			renameModel.useFormatter(File.class, new FileNameFormatter(renameModel.preserveExtension()));

			// display changed state
			filesList.repaint();
		}
	}

	protected class SetRenameAction extends AbstractAction {

		private final StandardRenameAction action;

		public SetRenameAction(StandardRenameAction action, String name, Icon icon) {
			super(name, icon);
			this.action = action;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			if (action == StandardRenameAction.MOVE) {
				renameAction.resetValues();
			} else {
				renameAction.putValue(RenameAction.RENAME_ACTION, action);
				renameAction.putValue(NAME, this.getValue(NAME));
				renameAction.putValue(SMALL_ICON, this.getValue(SMALL_ICON));
			}
		}
	}

	protected class AutoCompleteAction extends AbstractAction {

		private final AutoCompleteMatcher matcher;

		public AutoCompleteAction(String name, Icon icon, AutoCompleteMatcher matcher) {
			super(name, icon);

			this.matcher = matcher;

			// disable action while episode list matcher is working
			namesList.addPropertyChangeListener(LOADING_PROPERTY, new PropertyChangeListener() {

				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					// disable action while loading is in progress
					setEnabled(!(Boolean) evt.getNewValue());
				}
			});
		}

		@Override
		public void actionPerformed(final ActionEvent evt) {
			// clear names list
			renameModel.values().clear();

			SwingWorker<List<Match<File, ?>>, Void> worker = new SwingWorker<List<Match<File, ?>>, Void>() {

				private final List<File> remainingFiles = new LinkedList<File>(renameModel.files());
				private final SortOrder order = SortOrder.forName(persistentPreferredEpisodeOrder.getValue());
				private final Locale locale = new Locale(persistentPreferredLanguage.getValue());
				private final boolean autodetection = !isShiftOrAltDown(evt); // skip name auto-detection if SHIFT is pressed

				@Override
				protected List<Match<File, ?>> doInBackground() throws Exception {
					List<Match<File, ?>> matches = matcher.match(remainingFiles, order, locale, autodetection, getWindow(RenamePanel.this));

					// remove matched files
					for (Match<File, ?> match : matches) {
						remainingFiles.remove(match.getValue());
					}

					return matches;
				}

				@Override
				protected void done() {
					try {
						List<Match<Object, File>> matches = new ArrayList<Match<Object, File>>();

						for (Match<File, ?> match : get()) {
							matches.add(new Match<Object, File>(match.getCandidate(), match.getValue()));
						}

						renameModel.clear();
						renameModel.addAll(matches);

						// add remaining file entries
						renameModel.files().addAll(remainingFiles);
					} catch (Exception e) {
						UILogger.log(Level.WARNING, String.format("%s: %s", getRootCause(e).getClass().getSimpleName(), getRootCauseMessage(e)), e);
					} finally {
						// auto-match finished
						namesList.firePropertyChange(LOADING_PROPERTY, true, false);
					}
				}
			};

			// auto-match in progress
			namesList.firePropertyChange(LOADING_PROPERTY, false, true);

			worker.execute();
		}
	}

	public void setFixedCellWidth(boolean fixed) {
		for (RenameList<?> it : new RenameList[] { namesList, filesList }) {
			it.getListComponent().setFixedCellWidth(fixed ? (int) it.getListComponent().getPreferredSize().getWidth() : -1);
		}
	}

}
