package net.filebot.ui.rename;

import static java.awt.Font.*;
import static java.util.Collections.*;
import static javax.swing.BorderFactory.*;
import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.util.ExceptionUtilities.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.text.Format;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

import net.filebot.ResourceManager;
import net.filebot.Settings;
import net.filebot.UserFiles;
import net.filebot.format.BindingException;
import net.filebot.format.ExpressionFormat;
import net.filebot.format.MediaBindingBean;
import net.filebot.mac.MacAppUtilities;
import net.filebot.util.DefaultThreadFactory;
import net.filebot.util.ExceptionUtilities;
import net.filebot.util.PreferencesList;
import net.filebot.util.PreferencesMap.PreferencesEntry;
import net.filebot.util.ui.GradientStyle;
import net.filebot.util.ui.LazyDocumentListener;
import net.filebot.util.ui.LinkButton;
import net.filebot.util.ui.ProgressIndicator;
import net.filebot.util.ui.SwingUI;
import net.filebot.util.ui.notification.SeparatorBorder;
import net.filebot.util.ui.notification.SeparatorBorder.Position;
import net.filebot.web.AudioTrackFormat;
import net.filebot.web.Datasource;
import net.filebot.web.EpisodeFormat;
import net.filebot.web.EpisodeListProvider;
import net.filebot.web.MovieFormat;
import net.filebot.web.MovieIdentificationService;
import net.filebot.web.MusicIdentificationService;
import net.miginfocom.swing.MigLayout;

public class FormatDialog extends JDialog {

	private boolean submit = false;
	private ExpressionFormat format;

	private Mode mode;
	private boolean locked = false;
	private MediaBindingBean sample = null;

	private ExecutorService executor = createExecutor();
	private RunnableFuture<String> currentPreviewFuture;

	private JLabel preview = new JLabel();
	private JLabel status = new JLabel();

	private RSyntaxTextArea editor = createEditor();
	private ProgressIndicator progressIndicator = new ProgressIndicator();

	private JLabel title = new JLabel();
	private JPanel help = new JPanel(new MigLayout("insets 0, nogrid, fillx"));

	private static final PreferencesEntry<String> persistentSampleFile = Settings.forPackage(FormatDialog.class).entry("format.sample.file");

	public enum Mode {
		Episode, Movie, Music, File;

		public Mode next() {
			// cycle through Episode -> Movie -> Music (but ignore generic File mode)
			return values()[(this.ordinal() + 1) % File.ordinal()];
		}

		public String key() {
			return this.name().toLowerCase();
		}

		public Format getFormat() {
			switch (this) {
			case Episode:
				return new EpisodeFormat();
			case Movie: // case Movie
				return new MovieFormat(true, true, false);
			case Music:
				return new AudioTrackFormat();
			default:
				return new FileNameFormat();
			}
		}

		public PreferencesEntry<String> persistentSample() {
			return Settings.forPackage(FormatDialog.class).entry("format.sample." + key());
		}

		public PreferencesList<String> persistentFormatHistory() {
			return Settings.forPackage(FormatDialog.class).node("format.recent." + key()).asList();
		}

		public String getDefaultFormatExpression() {
			return getSampleExpressions().iterator().next();
		}

		public Iterable<String> getSampleExpressions() {
			ResourceBundle bundle = ResourceBundle.getBundle(FormatDialog.class.getName());
			Map<String, String> examples = new TreeMap<String, String>();

			// extract all example entries and sort by key
			String prefix = key() + ".example";
			for (String key : bundle.keySet()) {
				if (key.startsWith(prefix))
					examples.put(key, bundle.getString(key));
			}
			return examples.values();
		}

		public static Mode getMode(Datasource datasource) {
			if (datasource instanceof EpisodeListProvider)
				return Mode.Episode;
			if (datasource instanceof MovieIdentificationService)
				return Mode.Movie;
			if (datasource instanceof MusicIdentificationService)
				return Mode.Music;
			return Mode.File;
		}
	}

	public FormatDialog(Window owner, Mode initMode, MediaBindingBean lockOnBinding) {
		super(owner, ModalityType.DOCUMENT_MODAL);

		// initialize hidden
		progressIndicator.setVisible(false);

		// bold title label in header
		title.setFont(title.getFont().deriveFont(BOLD));

		JPanel header = new JPanel(new MigLayout("insets dialog, nogrid"));

		header.setBackground(Color.white);
		header.setBorder(new SeparatorBorder(1, new Color(0xB4B4B4), new Color(0xACACAC), GradientStyle.LEFT_TO_RIGHT, Position.BOTTOM));

		header.add(progressIndicator, "pos 1al 0al, hidemode 3");
		header.add(title, "wmin 150px, wrap unrel:push");
		header.add(preview, "wmin 150px, hmin 16px, gap indent, hidemode 3, wmax 90%");
		header.add(status, "wmin 150px, hmin 16px, gap indent, hidemode 3, wmax 90%, newline");

		JPanel content = new JPanel(new MigLayout("insets dialog, nogrid, fill"));

		RTextScrollPane editorScrollPane = new RTextScrollPane(editor, false);
		editorScrollPane.setLineNumbersEnabled(false);
		editorScrollPane.setFoldIndicatorEnabled(false);
		editorScrollPane.setIconRowHeaderEnabled(false);

		editorScrollPane.setVerticalScrollBarPolicy(RTextScrollPane.VERTICAL_SCROLLBAR_NEVER);
		editorScrollPane.setHorizontalScrollBarPolicy(RTextScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		editorScrollPane.setViewportBorder(createEmptyBorder(7, 2, 7, 2));
		editorScrollPane.setOpaque(true);
		editorScrollPane.setBorder(new JTextField().getBorder());

		content.add(editorScrollPane, "w 120px:min(pref, 420px), h pref!, growx, wrap 4px, id editor");
		content.add(createImageButton(changeSampleAction), "sg action, w 25!, h 19!, pos n editor.y2+2 editor.x2 n");
		content.add(createImageButton(selectFolderAction), "sg action, w 25!, h 19!, pos n editor.y2+2 editor.x2-(27*1) n");
		content.add(createImageButton(showRecentAction), "sg action, w 25!, h 19!, pos n editor.y2+2 editor.x2-(27*2) n");

		content.add(help, "growx, wrap 25px:push");

		if (lockOnBinding == null) {
			content.add(new JButton(switchEditModeAction), "tag left");
		}
		content.add(new JButton(approveFormatAction), "tag apply");
		content.add(new JButton(cancelAction), "tag cancel");

		JComponent pane = (JComponent) getContentPane();
		pane.setLayout(new MigLayout("insets 0, fill"));

		pane.add(header, "h 60px, growx, dock north");
		pane.add(content, "grow");

		addPropertyChangeListener("sample", new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (isMacSandbox()) {
					if (sample != null && sample.getFileObject() != null && sample.getFileObject().exists()) {
						MacAppUtilities.askUnlockFolders(getWindow(evt.getSource()), singleton(sample.getFileObject()));
					}
				}
				checkFormatInBackground();
			}
		});

		// focus editor by default
		addWindowFocusListener(new WindowAdapter() {

			@Override
			public void windowGainedFocus(WindowEvent e) {
				editor.requestFocusInWindow();
			}
		});

		// finish dialog and close window manually
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowActivated(WindowEvent e) {
				revalidate();
			}

			@Override
			public void windowClosing(WindowEvent e) {
				finish(false);
			}
		});

		// install editor suggestions popup
		editor.setComponentPopupMenu(createRecentFormatPopup());

		// initialize window properties
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setMinimumSize(new Dimension(650, 500));

		// initialize data
		setState(initMode, lockOnBinding != null ? lockOnBinding : restoreSample(initMode), lockOnBinding != null);
	}

	public void setState(Mode mode, MediaBindingBean bindings, boolean locked) {
		this.mode = mode;
		this.locked = locked;

		if (locked) {
			this.setTitle(String.format("%s Format", mode));
			title.setText(String.format("%s ⇔ %s", mode, bindings.getInfoObject(), bindings.getFileObject() == null ? null : bindings.getFileObject().getName()));
		} else {
			this.setTitle(String.format("%s Format", mode));
			title.setText(String.format("%s Format", mode));
		}
		status.setVisible(false);

		switchEditModeAction.putValue(Action.NAME, String.format("Switch to %s Format", mode.next()));
		switchEditModeAction.setEnabled(!locked);

		updateHelpPanel(mode);

		// update preview to current format
		sample = bindings;

		// restore editor state
		setFormatCode(mode.persistentFormatHistory().isEmpty() ? mode.getDefaultFormatExpression() : mode.persistentFormatHistory().get(0));

		// update examples
		fireSampleChanged();
	}

	private JComponent updateHelpPanel(Mode mode) {
		help.removeAll();

		help.add(new JLabel("Syntax"), "gap indent+unrel, wrap 0");
		help.add(createSyntaxPanel(mode), "gapx indent indent, wrap 8px");

		help.add(new JLabel("Examples"), "gap indent+unrel, wrap 0");
		help.add(createExamplesPanel(mode), "growx, h pref!, gapx indent indent");

		return help;
	}

	public void setFormatCode(String text) {
		editor.setText(text);
		editor.requestFocusInWindow();

		editor.scrollRectToVisible(new Rectangle(0, 0)); // reset scroll
		editor.setCaretPosition(text.length()); // scroll to end of format
	}

	private RSyntaxTextArea createEditor() {
		final RSyntaxTextArea editor = new RSyntaxTextArea(new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_GROOVY) {
			@Override
			public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
				super.insertString(offs, str.replaceAll("\\R", ""), a); // FORCE SINGLE LINE
			}
		}, null, 1, 80);

		editor.setAntiAliasingEnabled(true);
		editor.setAnimateBracketMatching(false);
		editor.setAutoIndentEnabled(false);
		editor.setClearWhitespaceLinesEnabled(false);
		editor.setBracketMatchingEnabled(true);
		editor.setCloseCurlyBraces(false);
		editor.setCodeFoldingEnabled(false);
		editor.setHyperlinksEnabled(false);
		editor.setUseFocusableTips(false);
		editor.setHighlightCurrentLine(false);
		editor.setLineWrap(false);

		editor.setFont(new Font(MONOSPACED, PLAIN, 14));

		// update format on change
		editor.getDocument().addDocumentListener(new LazyDocumentListener() {

			@Override
			public void update(DocumentEvent e) {
				checkFormatInBackground();
			}
		});

		return editor;
	}

	private JComponent createSyntaxPanel(Mode mode) {
		JPanel panel = new JPanel(new MigLayout("fill, nogrid"));

		panel.setBorder(createLineBorder(new Color(0xACA899)));
		panel.setBackground(new Color(0xFFFFE1));
		panel.setOpaque(true);

		panel.add(new LinkButton(new AbstractAction(ResourceBundle.getBundle(FormatDialog.class.getName()).getString(mode.key() + ".syntax")) {

			@Override
			public void actionPerformed(ActionEvent evt) {
				try {
					Desktop.getDesktop().browse(URI.create(ResourceBundle.getBundle(FormatDialog.class.getName()).getString("help.url")));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}));

		return panel;
	}

	private JComponent createExamplesPanel(Mode mode) {
		JPanel panel = new JPanel(new MigLayout("fill, wrap 3"));

		panel.setBorder(createLineBorder(new Color(0xACA899)));
		panel.setBackground(new Color(0xFFFFE1));

		for (final String format : mode.getSampleExpressions()) {
			LinkButton formatLink = new LinkButton(new AbstractAction(format) {

				@Override
				public void actionPerformed(ActionEvent e) {
					setFormatCode(format);
				}
			});

			formatLink.setFont(new Font(MONOSPACED, PLAIN, 11));

			// compute format label in background
			final JLabel formatExample = new JLabel("[evaluate]");

			// bind text to preview
			addPropertyChangeListener("sample", new PropertyChangeListener() {

				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					new SwingWorker<String, Void>() {

						@Override
						protected String doInBackground() throws Exception {
							return new ExpressionFormat(format).format(sample);
						}

						@Override
						protected void done() {
							try {
								formatExample.setText(get());
							} catch (Exception e) {
								debug.log(Level.SEVERE, e.getMessage(), e);
							}
						}
					}.execute();
				}
			});

			panel.add(formatLink);
			panel.add(new JLabel("…"));
			panel.add(formatExample, "wmin 150px");
		}

		return panel;
	}

	protected MediaBindingBean restoreSample(Mode mode) {
		Object info = null;
		File media = null;

		try {
			// restore sample from user preferences
			String sample = mode.persistentSample().getValue();
			info = JsonReader.jsonToJava(sample);
			if (info == null) {
				throw new NullPointerException();
			}
		} catch (Exception e) {
			try {
				// restore sample from application properties
				ResourceBundle bundle = ResourceBundle.getBundle(getClass().getName());
				String sample = bundle.getString(mode.key() + ".sample");
				info = JsonReader.jsonToJava(sample);
			} catch (Exception illegalSample) {
				debug.log(Level.SEVERE, "Illegal Sample", e);
			}
		}

		// restore media file
		String path = persistentSampleFile.getValue();

		if (path != null && !path.isEmpty()) {
			media = new File(path);
		}

		return new MediaBindingBean(info, media);
	}

	private ExecutorService createExecutor() {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1), new DefaultThreadFactory("PreviewFormatter")) {

			@SuppressWarnings("deprecation")
			@Override
			public List<Runnable> shutdownNow() {
				List<Runnable> remaining = super.shutdownNow();

				try {
					if (!awaitTermination(3, TimeUnit.SECONDS)) {
						// if the thread has not terminated after 4 seconds, it is probably stuck
						ThreadGroup threadGroup = ((DefaultThreadFactory) getThreadFactory()).getThreadGroup();

						// kill background thread by force
						threadGroup.stop();

						// log access of potentially unsafe method
						debug.warning("Thread was forcibly terminated");
					}
				} catch (Exception e) {
					debug.log(Level.WARNING, "Thread was not terminated", e);
				}

				return remaining;
			}
		};

		// only keep the latest task in the queue
		executor.setRejectedExecutionHandler(new DiscardOldestPolicy());

		return executor;
	}

	private void checkFormatInBackground() {
		try {
			// check syntax in foreground
			final ExpressionFormat format = new ExpressionFormat(editor.getText().trim());

			// activate delayed to avoid flickering when formatting takes only a couple of milliseconds
			final Timer progressIndicatorTimer = SwingUI.invokeLater(400, new Runnable() {

				@Override
				public void run() {
					progressIndicator.setVisible(true);
				}
			});

			// cancel old worker later
			Future<String> obsoletePreviewFuture = currentPreviewFuture;

			// create new worker
			currentPreviewFuture = new SwingWorker<String, Void>() {

				@Override
				protected String doInBackground() throws Exception {
					return format.format(sample);
				}

				@Override
				protected void done() {
					try {
						preview.setText(get().trim());

						// check internal script exception
						if (format.caughtScriptException() != null) {
							throw format.caughtScriptException();
						}

						// check empty output
						if (preview.getText().isEmpty()) {
							throw new Exception("Formatted value is empty");
						}

						// no warning or error
						status.setVisible(false);
					} catch (CancellationException e) {
						// ignore, cancelled tasks are obsolete anyway
					} catch (Exception e) {
						BindingException issue = findCause(e, BindingException.class);
						if (issue != null && getMessage(issue).contains(MediaBindingBean.EXCEPTION_SAMPLE_FILE_NOT_SET)) {
							// exception caused by file bindings because sample file has not been set
							status.setText(getMessage(issue));
							status.setIcon(ResourceManager.getIcon("action.variables"));
						} else if (issue != null) {
							// default binding exception handler
							status.setText(getMessage(issue));
							status.setIcon(ResourceManager.getIcon("status.info"));
						} else if (e.getCause() != null && e.getCause().getClass().equals(Exception.class)) {
							// ScriptShellMethods throws Exception type exceptions which are not unexpected
							status.setText(e.getCause().getMessage());
							status.setIcon(ResourceManager.getIcon("status.info"));
						} else {
							// default exception handler
							status.setText(String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage()));
							status.setIcon(ResourceManager.getIcon("status.warning"));
						}
						status.setVisible(true);
					} finally {
						preview.setVisible(preview.getText().trim().length() > 0);
						editor.setForeground(preview.getForeground());

						// stop progress indicator from becoming visible, if we have been fast enough
						progressIndicatorTimer.stop();

						// hide progress indicator, if this still is the current worker
						if (this == currentPreviewFuture) {
							progressIndicator.setVisible(false);
						}
					}
				}
			};

			// cancel old worker, after new worker has been created, because done() might be called from within cancel()
			if (obsoletePreviewFuture != null) {
				obsoletePreviewFuture.cancel(true);
			}

			// submit new worker
			executor.execute(currentPreviewFuture);
		} catch (ScriptException e) {
			// incorrect syntax
			status.setText(ExceptionUtilities.getRootCauseMessage(e));
			status.setIcon(ResourceManager.getIcon("status.error"));
			status.setVisible(true);

			preview.setVisible(false);
			editor.setForeground(Color.red);
		}
	}

	public boolean submit() {
		return submit;
	}

	public Mode getMode() {
		return mode;
	}

	public ExpressionFormat getFormat() {
		return format;
	}

	private void finish(boolean submit) {
		this.submit = submit;

		// force shutdown
		executor.shutdownNow();

		setVisible(false);
		dispose();
	}

	private JPopupMenu createRecentFormatPopup() {
		JPopupMenu popup = new JPopupMenu();
		popup.addPopupMenuListener(new PopupMenuListener() {

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent evt) {
				// make sure to reset state
				popupMenuWillBecomeInvisible(evt);

				JPopupMenu popup = (JPopupMenu) evt.getSource();
				for (final String expression : mode.persistentFormatHistory()) {
					JMenuItem item = popup.add(new AbstractAction(expression) {

						@Override
						public void actionPerformed(ActionEvent evt) {
							setFormatCode(expression);
						}
					});

					item.setFont(new Font(MONOSPACED, PLAIN, 11));
				}
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent evt) {
				JPopupMenu popup = (JPopupMenu) evt.getSource();
				popup.removeAll();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent evt) {
				popupMenuWillBecomeInvisible(evt);
			}
		});
		return popup;
	}

	protected final Action changeSampleAction = new AbstractAction("Change Sample", ResourceManager.getIcon("action.variables")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			BindingDialog dialog = new BindingDialog(getWindow(evt.getSource()), String.format("%s Bindings", mode), mode.getFormat(), !locked);

			dialog.setInfoObject(sample.getInfoObject());
			dialog.setMediaFile(sample.getFileObject());

			// open dialog
			dialog.setLocationRelativeTo((Component) evt.getSource());
			dialog.setVisible(true);

			if (dialog.submit()) {
				Object info = dialog.getInfoObject();
				File file = dialog.getMediaFile();

				// change sample
				sample = new MediaBindingBean(info, file);

				// remember sample
				try {
					mode.persistentSample().setValue(info == null ? "" : JsonWriter.objectToJson(info));
					persistentSampleFile.setValue(file == null ? "" : sample.getFileObject().getAbsolutePath());
				} catch (Exception e) {
					debug.log(Level.WARNING, e.getMessage(), e);
				}

				// reevaluate everything
				fireSampleChanged();
			}
		}
	};

	protected final Action selectFolderAction = new AbstractAction("Change Folder", ResourceManager.getIcon("action.load")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			String relativeFormat = editor.getText().trim();
			File absoluteFolder = null;

			if (relativeFormat.length() > 0) {
				File templatePath = new File(relativeFormat);
				if (templatePath.isAbsolute()) {
					File existingPath = null;
					for (File next : listPath(templatePath)) {
						if (existingPath != null && !next.exists()) {
							absoluteFolder = existingPath;
							relativeFormat = relativeFormat.substring(existingPath.getPath().length() + 1); // account for file separator
							break;
						}
						existingPath = next;
					}
				}
			}

			File selectedFolder = UserFiles.showOpenDialogSelectFolder(absoluteFolder, "Select Folder", evt);
			if (selectedFolder != null) {
				editor.setText(normalizePathSeparators(selectedFolder.getAbsolutePath()) + "/" + relativeFormat);
			}
		}

	};

	protected final Action showRecentAction = new AbstractAction("Change Format", ResourceManager.getIcon("action.menu")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			// display popup below format editor
			JComponent c = (JComponent) evt.getSource();
			editor.getComponentPopupMenu().show(c, 0, c.getHeight() + 3);
		}
	};

	protected final Action cancelAction = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {

		@Override
		public void actionPerformed(ActionEvent e) {
			finish(false);
		}
	};

	protected final Action switchEditModeAction = new AbstractAction("Switch Mode", ResourceManager.getIcon("dialog.switch")) {

		@Override
		public void actionPerformed(ActionEvent e) {
			Mode next = mode.next();
			setState(next, restoreSample(next), false);
		}
	};

	protected final Action approveFormatAction = new AbstractAction("Use Format", ResourceManager.getIcon("dialog.continue")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			try {
				// check syntax
				format = new ExpressionFormat(editor.getText().trim());

				if (format.getExpression().isEmpty()) {
					throw new ScriptException("Expression is empty");
				}

				// create new recent history and ignore duplicates
				Set<String> recent = new LinkedHashSet<String>();

				// add new format first
				recent.add(format.getExpression());

				// save the 8 most recent formats
				for (String expression : mode.persistentFormatHistory()) {
					recent.add(expression);

					if (recent.size() >= 8) {
						break;
					}
				}

				// update persistent history
				mode.persistentFormatHistory().set(recent);

				finish(true);
			} catch (ScriptException e) {
				log.log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e));
			}
		}
	};

	protected void fireSampleChanged() {
		firePropertyChange("sample", null, sample);
	}

}
