package net.filebot.ui.subtitle;

import static net.filebot.Settings.*;
import static net.filebot.ui.LanguageComboBoxModel.*;
import static net.filebot.ui.NotificationLogging.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.Path2D;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import net.filebot.Language;
import net.filebot.ResourceManager;
import net.filebot.Settings;
import net.filebot.WebServices;
import net.filebot.media.MediaDetection;
import net.filebot.ui.AbstractSearchPanel;
import net.filebot.ui.LanguageComboBox;
import net.filebot.ui.SelectDialog;
import net.filebot.util.ui.LabelProvider;
import net.filebot.util.ui.LinkButton;
import net.filebot.util.ui.SimpleLabelProvider;
import net.filebot.web.OpenSubtitlesClient;
import net.filebot.web.SearchResult;
import net.filebot.web.SubtitleDescriptor;
import net.filebot.web.SubtitleProvider;
import net.filebot.web.SubtitleSearchResult;
import net.filebot.web.VideoHashSubtitleService;
import net.miginfocom.swing.MigLayout;

public class SubtitlePanel extends AbstractSearchPanel<SubtitleProvider, SubtitlePackage> {

	private LanguageComboBox languageComboBox = new LanguageComboBox(ALL_LANGUAGES, getSettings());

	public SubtitlePanel() {
		historyPanel.setColumnHeader(0, "Show / Movie");
		historyPanel.setColumnHeader(1, "Number of Subtitles");

		// add after text field
		add(languageComboBox, "gap indent, sgy button", 1);
		add(createImageButton(setUserAction), "w pref!, h 2+pref!, gap rel, sgy button", 2);

		// add at the top right corner
		add(uploadDropTarget, "width 1.45cm!, height 1.2cm!, pos n 0% 100%-1.8cm n", 0);
		add(downloadDropTarget, "width 1.45cm!, height 1.2cm!, pos n 0% 100%-0.15cm n", 0);
	}

	private final SubtitleDropTarget uploadDropTarget = new SubtitleDropTarget.Upload() {

		@Override
		public OpenSubtitlesClient getSubtitleService() {
			return WebServices.OpenSubtitles;
		};

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			Path2D path = new Path2D.Float();
			path.moveTo(0, 0);
			path.lineTo(0, getHeight() - 1 - 12);
			path.lineTo(12, getHeight() - 1);
			path.lineTo(getWidth() - 1 - 12, getHeight() - 1);
			path.lineTo(getWidth() - 1, getHeight() - 1 - 12);
			path.lineTo(getWidth() - 1, 0);

			g2d.setPaint(getBackground());
			g2d.fill(path);

			g2d.setPaint(Color.gray);
			g2d.draw(path);

			g2d.translate(2, 0);
			super.paintComponent(g2d);
			g2d.dispose();
		}
	};

	private final SubtitleDropTarget downloadDropTarget = new SubtitleDropTarget.Download() {

		@Override
		public VideoHashSubtitleService[] getVideoHashSubtitleServices() {
			Locale locale = languageComboBox.getModel().getSelectedItem() == ALL_LANGUAGES ? Locale.ROOT : languageComboBox.getModel().getSelectedItem().getLocale();
			return WebServices.getVideoHashSubtitleServices(locale);
		}

		@Override
		public SubtitleProvider[] getSubtitleProviders() {
			return WebServices.getSubtitleProviders();
		}

		@Override
		public OpenSubtitlesClient getSubtitleService() {
			return WebServices.OpenSubtitles;
		};

		@Override
		public String getQueryLanguage() {
			// use currently selected language for drop target
			return languageComboBox.getModel().getSelectedItem() == ALL_LANGUAGES ? null : languageComboBox.getModel().getSelectedItem().getName();
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			Path2D path = new Path2D.Float();
			path.moveTo(0, 0);
			path.lineTo(0, getHeight() - 1 - 12);
			path.lineTo(12, getHeight() - 1);
			path.lineTo(getWidth() - 1 - 12, getHeight() - 1);
			path.lineTo(getWidth() - 1, getHeight() - 1 - 12);
			path.lineTo(getWidth() - 1, 0);

			g2d.setPaint(getBackground());
			g2d.fill(path);

			g2d.setPaint(Color.gray);
			g2d.draw(path);

			g2d.translate(2, 0);
			super.paintComponent(g2d);
			g2d.dispose();
		}
	};

	@Override
	protected Collection<String> getHistory(SubtitleProvider engine) throws Exception {
		List<String> names = new ArrayList<String>();
		for (SubtitleSearchResult it : MediaDetection.releaseInfo.getOpenSubtitlesIndex()) {
			names.add(it.toString());
		}
		return names;
	};

	@Override
	protected SubtitleProvider[] getSearchEngines() {
		return WebServices.getSubtitleProviders();
	}

	@Override
	protected LabelProvider<SubtitleProvider> getSearchEngineLabelProvider() {
		return SimpleLabelProvider.forClass(SubtitleProvider.class);
	}

	@Override
	protected Settings getSettings() {
		return Settings.forPackage(SubtitlePanel.class);
	}

	@Override
	protected SubtitleRequestProcessor createRequestProcessor() {
		SubtitleProvider provider = searchTextField.getSelectButton().getSelectedValue();
		String text = searchTextField.getText().trim();
		Language language = languageComboBox.getModel().getSelectedItem();

		if (provider instanceof OpenSubtitlesClient && ((OpenSubtitlesClient) provider).isAnonymous() && !Settings.isAppStore()) {
			UILogger.info(String.format("%s: Please enter your login details first.", ((OpenSubtitlesClient) provider).getName()));

			// automatically open login dialog
			SwingUtilities.invokeLater(() -> setUserAction.actionPerformed(new ActionEvent(searchTextField, 0, "login")));

			return null;
		}

		return new SubtitleRequestProcessor(new SubtitleRequest(provider, text, language));
	}

	protected static class SubtitleRequest extends Request {

		private final SubtitleProvider provider;
		private final Language language;

		public SubtitleRequest(SubtitleProvider provider, String searchText, Language language) {
			super(searchText);

			this.provider = provider;
			this.language = language;
		}

		public SubtitleProvider getProvider() {
			return provider;
		}

		public String getLanguageName() {
			return language == ALL_LANGUAGES ? null : language.getName();
		}

	}

	protected static class SubtitleRequestProcessor extends RequestProcessor<SubtitleRequest, SubtitlePackage> {

		public SubtitleRequestProcessor(SubtitleRequest request) {
			super(request, new SubtitleDownloadComponent());
		}

		@Override
		public Collection<SubtitleSearchResult> search() throws Exception {
			return request.getProvider().search(request.getSearchText());
		}

		@Override
		public SubtitleSearchResult getSearchResult() {
			return (SubtitleSearchResult) super.getSearchResult();
		}

		@Override
		public Collection<SubtitlePackage> fetch() throws Exception {
			List<SubtitlePackage> packages = new ArrayList<SubtitlePackage>();

			for (SubtitleDescriptor subtitle : request.getProvider().getSubtitleList(getSearchResult(), request.getLanguageName())) {
				packages.add(new SubtitlePackage(request.getProvider(), subtitle));
			}

			return packages;
		}

		@Override
		public URI getLink() {
			return request.getProvider().getSubtitleListLink(getSearchResult(), request.getLanguageName());
		}

		@Override
		public void process(Collection<SubtitlePackage> subtitles) {
			getComponent().setLanguageVisible(request.getLanguageName() == null);
			getComponent().getPackageModel().addAll(subtitles);
		}

		@Override
		public SubtitleDownloadComponent getComponent() {
			return (SubtitleDownloadComponent) super.getComponent();
		}

		@Override
		public String getStatusMessage(Collection<SubtitlePackage> result) {
			return (result.isEmpty()) ? "No subtitles found" : String.format("%d subtitles", result.size());
		}

		@Override
		public Icon getIcon() {
			return request.provider.getIcon();
		}

		@Override
		protected void configureSelectDialog(SelectDialog<SearchResult> selectDialog) {
			super.configureSelectDialog(selectDialog);
			selectDialog.getHeaderLabel().setText("Select a Show / Movie:");
		}

	}

	protected final Action setUserAction = new AbstractAction("Login", ResourceManager.getIcon("action.user")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			final JDialog authPanel = new JDialog(getWindow(SubtitlePanel.this), ModalityType.APPLICATION_MODAL);
			authPanel.setTitle("Login");
			authPanel.setLocation(getOffsetLocation(authPanel.getOwner()));

			JPanel osdbGroup = new JPanel(new MigLayout("fill, insets panel"));
			osdbGroup.setBorder(new TitledBorder("OpenSubtitles"));
			osdbGroup.add(new JLabel("Username:"), "gap rel");
			final JTextField osdbUser = new JTextField(12);
			osdbGroup.add(osdbUser, "growx, wrap rel");

			osdbGroup.add(new JLabel("Password:"), "gap rel");
			final JPasswordField osdbPass = new JPasswordField(12);
			osdbGroup.add(osdbPass, "growx, wrap unrel");

			// restore values
			String[] osdbAuth = WebServices.getLogin(WebServices.LOGIN_OPENSUBTITLES);
			osdbUser.setText(osdbAuth[0]);
			// osdbPass.setText(osdbAuth[1]); // password is stored as MD5 hash so we can't restore it

			if (osdbUser.getText().isEmpty()) {
				osdbGroup.add(new LinkButton("Register Account", "Register to increase your download quota", WebServices.OpenSubtitles.getIcon(), URI.create("http://www.opensubtitles.org/en/newuser")), "spanx 2, tag left");
			} else {
				osdbGroup.add(new LinkButton("Upgrade Account", "Upgrade to increase your download quota", WebServices.OpenSubtitles.getIcon(), URI.create("http://www.opensubtitles.org/en/support")), "spanx 2, tag left");
			}

			JRootPane container = authPanel.getRootPane();
			container.setLayout(new MigLayout("fill, insets dialog"));
			container.removeAll();

			container.add(osdbGroup, "growx, wrap");

			Action ok = new AbstractAction("OK", ResourceManager.getIcon("dialog.continue")) {

				@Override
				public void actionPerformed(ActionEvent evt) {
					authPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					boolean approved = true;

					try {
						if (osdbUser.getText().length() > 0 && osdbPass.getPassword().length > 0) {
							final OpenSubtitlesClient osdb = new OpenSubtitlesClient(getApplicationName(), getApplicationVersion());
							osdb.setUser(osdbUser.getText(), md5(new String(osdbPass.getPassword())));
							osdb.login();

							// do some status checks in background (since OpenSubtitles can be really really slow)
							WebServices.requestThreadPool.submit(() -> {
								try {
									// check download quota for the current user
									Map<?, ?> limits = (Map<?, ?>) osdb.getServerInfo().get("download_limits");
									UILogger.log(Level.INFO, String.format("Your daily download quota is at %s of %s.", limits.get("client_24h_download_count"), limits.get("client_24h_download_limit")));

									// logout from test session
									osdb.logout();
								} catch (Exception e) {
									Logger.getLogger(SubtitlePanel.class.getName()).log(Level.WARNING, e.toString());
								}
							});
						} else if (osdbUser.getText().isEmpty()) {
							WebServices.setLogin(WebServices.LOGIN_OPENSUBTITLES, null, null); // delete login details
						}
					} catch (Exception e) {
						UILogger.log(Level.WARNING, "OpenSubtitles: " + e.getMessage());
						approved = false;
					}

					authPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					if (approved) {
						WebServices.setLogin(WebServices.LOGIN_OPENSUBTITLES, osdbUser.getText(), new String(osdbPass.getPassword()));
						authPanel.setVisible(false);
					}
				}
			};
			Action cancel = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {

				@Override
				public void actionPerformed(ActionEvent evt) {
					authPanel.setVisible(false);
				}
			};
			container.add(new JButton(cancel), "tag cancel, split 2");
			container.add(new JButton(ok), "tag ok");

			authPanel.pack();
			authPanel.setVisible(true);
		}
	};

}
