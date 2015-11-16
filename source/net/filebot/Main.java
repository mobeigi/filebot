package net.filebot;

import static java.awt.GraphicsEnvironment.*;
import static java.util.regex.Pattern.*;
import static javax.swing.JOptionPane.*;
import static net.filebot.Settings.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Desktop;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilderFactory;

import net.filebot.cli.ArgumentBean;
import net.filebot.cli.ArgumentProcessor;
import net.filebot.cli.CmdlineOperations;
import net.filebot.format.ExpressionFormat;
import net.filebot.mac.MacAppUtilities;
import net.filebot.ui.FileBotMenuBar;
import net.filebot.ui.GettingStartedStage;
import net.filebot.ui.MainFrame;
import net.filebot.ui.PanelBuilder;
import net.filebot.ui.SinglePanelFrame;
import net.filebot.ui.transfer.FileTransferable;
import net.filebot.util.ByteBufferInputStream;
import net.filebot.util.PreferencesMap.PreferencesEntry;
import net.filebot.util.TeePrintStream;
import net.filebot.web.CachedResource;
import net.miginfocom.swing.MigLayout;
import net.sf.ehcache.CacheManager;

import org.kohsuke.args4j.CmdLineException;
import org.w3c.dom.NodeList;

public class Main {

	public static void main(String[] argumentArray) {
		try {
			// parse arguments
			final ArgumentBean args = ArgumentBean.parse(argumentArray);

			if (args.printHelp() || args.printVersion() || (!(args.runCLI() || args.clearCache() || args.clearUserData()) && isHeadless())) {
				System.out.format("%s / %s%n%n", getApplicationIdentifier(), getJavaRuntimeIdentifier());

				if (args.printHelp() || (!args.printVersion() && isHeadless())) {
					ArgumentBean.printHelp(args, System.out);
				}

				// just print help message or version string and then exit
				System.exit(0);
			}

			if (args.clearCache() || args.clearUserData()) {
				if (args.clearUserData()) {
					System.out.println("Reset preferences");
					Settings.forPackage(Main.class).clear();
				}

				if (args.clearCache()) {
					// clear preferences and cache
					System.out.println("Clear cache and temporary files");
					for (File folder : getChildren(getApplicationFolder().getAbsoluteFile(), FOLDERS)) {
						if (matches("cache|temp|grape|reports|logs", folder.getName())) {
							if (delete(folder)) {
								System.out.println("* Delete " + folder);
							}
						}
					}

					initializeCache();
					CacheManager.getInstance().clearAll();
				}

				// just clear cache and/or settings and then exit
				System.exit(0);
			}

			// tee stdout and stderr to log file if set
			if (args.logFile != null) {
				File logFile = new File(args.logFile);
				if (!logFile.isAbsolute()) {
					logFile = new File(new File(getApplicationFolder(), "logs"), logFile.getPath()).getAbsoluteFile(); // by default resolve relative paths against {applicationFolder}/logs/{logFile}
				}
				if (!logFile.exists() && !logFile.getParentFile().mkdirs() && !logFile.createNewFile()) {
					throw new IOException("Failed to create log file: " + logFile);
				}

				// open file channel and lock
				FileChannel logChannel = FileChannel.open(logFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
				if (args.logLock) {
					if (args.getLogLevel() == Level.ALL) {
						System.out.println("Locking " + logFile);
					}
					logChannel.lock();
				}

				OutputStream out = Channels.newOutputStream(logChannel);
				System.setOut(new TeePrintStream(out, true, "UTF-8", System.out));
				System.setErr(new TeePrintStream(out, true, "UTF-8", System.err));
			}

			// make sure java.io.tmpdir exists
			File tmpdir = new File(System.getProperty("java.io.tmpdir"));
			createFolders(tmpdir);

			// initialize this stuff before anything else
			initializeCache();
			initializeSecurityManager();

			// update system properties
			System.setProperty("http.agent", String.format("%s %s", getApplicationName(), getApplicationVersion()));
			System.setProperty("swing.crossplatformlaf", "javax.swing.plaf.nimbus.NimbusLookAndFeel");
			System.setProperty("grape.root", new File(getApplicationFolder(), "grape").getAbsolutePath());
			System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

			if (args.unixfs) {
				System.setProperty("unixfs", "true");
			}
			if (args.disableExtendedAttributes) {
				System.setProperty("useExtendedFileAttributes", "false");
				System.setProperty("useCreationDate", "false");
			}
			if (args.action.equalsIgnoreCase("test")) {
				System.setProperty("application.rename.history", "false"); // don't keep history of --action test rename operations
			}

			// make sure we can access application arguments at any time
			Settings.setApplicationArgumentArray(argumentArray);

			// initialize history spooler
			HistorySpooler.getInstance().setPersistentHistoryEnabled(System.getProperty("application.rename.history") == null ? true : Boolean.parseBoolean(System.getProperty("application.rename.history")));

			// CLI mode => run command-line interface and then exit
			if (args.runCLI()) {
				int status = new ArgumentProcessor().process(args, new CmdlineOperations());
				System.out.println();
				System.exit(status);
			}

			// GUI mode => start user interface
			SwingUtilities.invokeAndWait(() -> {
				try {
					// use native laf an all platforms
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					Logger.getLogger(Main.class.getName()).log(Level.WARNING, e.getMessage(), e);
				}
				startUserInterface(args);
			});

			// preload media.types (when loaded during DnD it will freeze the UI for a few hundred milliseconds)
			MediaTypes.getDefault();

			// check for application updates
			if (!"skip".equals(System.getProperty("application.update"))) {
				try {
					checkUpdate();
				} catch (Exception e) {
					Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Failed to check for updates", e);
				}
			}

			// check if application help should be shown
			if (!"skip".equals(System.getProperty("application.help"))) {
				try {
					checkGettingStarted();
				} catch (Exception e) {
					Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Failed to show Getting Started help", e);
				}
			}
		} catch (CmdLineException e) {
			// illegal arguments => print CLI error message
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (Throwable e) {
			// unexpected error => dump stack
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Unexpected error during startup", e);
			System.exit(-1);
		}
	}

	private static void startUserInterface(ArgumentBean args) {
		JFrame frame = null;

		if (args.mode == null) {
			// default frame
			frame = new MainFrame();
		} else {
			// single panel frame
			for (PanelBuilder it : MainFrame.createPanelBuilders()) {
				if (args.mode.equalsIgnoreCase(it.getName())) {
					frame = new SinglePanelFrame(it).publish(new FileTransferable(args.getFiles(false)));
				}
			}
			if (frame == null) {
				throw new IllegalArgumentException("Illegal mode: " + args.mode);
			}
		}

		try {
			// restore previous size and location
			restoreWindowBounds(frame, Settings.forPackage(MainFrame.class));
		} catch (Exception e) {
			// make sure the main window is not displayed out of screen bounds
			frame.setLocation(120, 80);
		}

		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				e.getWindow().setVisible(false);

				// make sure any long running operations are done now and not later on the shutdownhook thread
				HistorySpooler.getInstance().commit();

				// show donation / review reminders to power users (more than 2000 renames)
				int renameCount = HistorySpooler.getInstance().getPersistentHistoryTotalSize();

				if (renameCount > 2000 && Math.random() <= 0.777) {
					if (isAppStore()) {
						showAppStoreReviewReminder();
					} else {
						showDonationReminder();
					}
				}

				System.exit(0);
			}
		});

		// configure main window
		if (isMacApp()) {
			// Mac OS X specific configuration
			MacAppUtilities.initializeApplication();
			MacAppUtilities.setWindowCanFullScreen(frame);
			MacAppUtilities.setDefaultMenuBar(FileBotMenuBar.createHelp());
		} else if (isUbuntuApp()) {
			// Ubuntu specific configuration
			String options = System.getenv("JAVA_TOOL_OPTIONS");
			if (options != null && options.contains("jayatanaag.jar")) {
				frame.setJMenuBar(FileBotMenuBar.createHelp()); // menu should be rendered via JAyatana on Ubuntu 15.04 and higher
			}
			frame.setIconImages(ResourceManager.getApplicationIcons());
		} else {
			// Windows / Linux specific configuration
			frame.setIconImages(ResourceManager.getApplicationIcons());
		}

		// start application
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}

	/**
	 * Show update notifications if updates are available
	 */
	private static void checkUpdate() throws Exception {
		final Properties updateProperties = new CachedResource<Properties>(getApplicationProperty("update.url"), Properties.class, CachedResource.ONE_WEEK, 0, 0) {

			@Override
			public Properties process(ByteBuffer data) {
				try {
					Properties properties = new Properties();
					NodeList fields = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteBufferInputStream(data)).getFirstChild().getChildNodes();
					for (int i = 0; i < fields.getLength(); i++) {
						properties.setProperty(fields.item(i).getNodeName(), fields.item(i).getTextContent().trim());
					}
					return properties;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}.get();

		// check if update is required
		int latestRev = Integer.parseInt(updateProperties.getProperty("revision"));
		int currentRev = getApplicationRevisionNumber();

		if (latestRev > currentRev && currentRev > 0) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					final JDialog dialog = new JDialog(JFrame.getFrames()[0], updateProperties.getProperty("title"), ModalityType.APPLICATION_MODAL);
					final JPanel pane = new JPanel(new MigLayout("fill, nogrid, insets dialog"));
					dialog.setContentPane(pane);

					pane.add(new JLabel(ResourceManager.getIcon("window.icon.medium")), "aligny top");
					pane.add(new JLabel(updateProperties.getProperty("message")), "gap 10, wrap paragraph:push");
					pane.add(new JButton(new AbstractAction("Download", ResourceManager.getIcon("dialog.continue")) {

						@Override
						public void actionPerformed(ActionEvent evt) {
							try {
								Desktop.getDesktop().browse(URI.create(updateProperties.getProperty("download")));
							} catch (IOException e) {
								throw new RuntimeException(e);
							} finally {
								dialog.setVisible(false);
							}
						}
					}), "tag ok");
					pane.add(new JButton(new AbstractAction("Details", ResourceManager.getIcon("action.report")) {

						@Override
						public void actionPerformed(ActionEvent evt) {
							try {
								Desktop.getDesktop().browse(URI.create(updateProperties.getProperty("discussion")));
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}
					}), "tag help2");
					pane.add(new JButton(new AbstractAction("Ignore", ResourceManager.getIcon("dialog.cancel")) {

						@Override
						public void actionPerformed(ActionEvent evt) {
							dialog.setVisible(false);
						}
					}), "tag cancel");

					dialog.pack();
					dialog.setLocation(getOffsetLocation(dialog.getOwner()));
					dialog.setVisible(true);
				}
			});
		}
	}

	/**
	 * Show Getting Started to new users
	 */
	private static void checkGettingStarted() throws Exception {
		PreferencesEntry<String> started = Settings.forPackage(Main.class).entry("getting.started").defaultValue("0");
		if ("0".equals(started.getValue())) {
			started.setValue("1");
			started.flush();

			SwingUtilities.invokeLater(() -> {
				GettingStartedStage.start();
			});
		}
	}

	private static void showDonationReminder() {
		PreferencesEntry<String> donation = Settings.forPackage(Main.class).entry("donation").defaultValue("0");
		int donationRev = Integer.parseInt(donation.getValue());
		int currentRev = getApplicationRevisionNumber();
		if (donationRev >= currentRev) {
			return;
		}

		String message = String.format(Locale.ROOT, "<html><p style='font-size:16pt; font-weight:bold'>Thank you for using FileBot!</p><br><p>It has taken many nights to develop this application. If you enjoy using it,<br>please consider a donation to me and my work. It will help to<br>make FileBot even better!<p><p style='font-size:14pt; font-weight:bold'>You've renamed %,d files.</p><br><html>", HistorySpooler.getInstance().getPersistentHistoryTotalSize());
		String[] actions = new String[] { "Donate! :)", donationRev > 0 ? "Not this time" : "Later" };
		JOptionPane pane = new JOptionPane(message, INFORMATION_MESSAGE, YES_NO_OPTION, ResourceManager.getIcon("message.donate"), actions, actions[0]);
		pane.createDialog(null, "Please Donate").setVisible(true);
		if (pane.getValue() == actions[0]) {
			try {
				Desktop.getDesktop().browse(new URI(getDonateURL()));
			} catch (Exception e) {
				Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Failed to browse URI", e);
			} finally {
				donation.setValue(String.valueOf(currentRev));
			}
		} else {
			if (donationRev > 0 && donationRev < currentRev) {
				donation.setValue(String.valueOf(currentRev));
			}
		}
	}

	private static void showAppStoreReviewReminder() {
		PreferencesEntry<String> donation = Settings.forPackage(Main.class).entry("review").defaultValue("0");
		int donationRev = Integer.parseInt(donation.getValue());
		if (donationRev > 0) {
			return;
		}

		// make sure review reminder is shown at most once (per machine)
		int currentRev = getApplicationRevisionNumber();
		donation.setValue(String.valueOf(currentRev));

		String message = String.format(Locale.ROOT, "<html><p style='font-size:16pt; font-weight:bold'>Thank you for using FileBot!</p><br><p>It has taken many nights to develop this application. If you enjoy using it,<br>please consider writing a nice little review on the %s.<p><p style='font-size:14pt; font-weight:bold'>You've renamed %,d files.</p><br><html>", getAppStoreName(), HistorySpooler.getInstance().getPersistentHistoryTotalSize());
		String[] actions = new String[] { "Review! I like FileBot. :)", "Never! Don't bother me again." };
		JOptionPane pane = new JOptionPane(message, INFORMATION_MESSAGE, YES_NO_OPTION, ResourceManager.getIcon("window.icon.large"), actions, actions[0]);
		pane.createDialog(null, "Please rate FileBot").setVisible(true);
		if (pane.getValue() == actions[0]) {
			try {
				Desktop.getDesktop().browse(getAppStoreURI()); // this will naturally only work on Mac or Ubuntu ;)
			} catch (Exception e) {
				Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Failed to browse URI", e);
			}
		}
	}

	private static void restoreWindowBounds(final JFrame window, final Settings settings) {
		// store bounds on close
		window.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				// don't save window bounds if window is maximized
				if (!isMaximized(window)) {
					settings.put("window.x", String.valueOf(window.getX()));
					settings.put("window.y", String.valueOf(window.getY()));
					settings.put("window.width", String.valueOf(window.getWidth()));
					settings.put("window.height", String.valueOf(window.getHeight()));
				}
			}
		});

		// restore bounds
		int x = Integer.parseInt(settings.get("window.x"));
		int y = Integer.parseInt(settings.get("window.y"));
		int width = Integer.parseInt(settings.get("window.width"));
		int height = Integer.parseInt(settings.get("window.height"));
		window.setBounds(x, y, width, height);
	}

	/**
	 * Shutdown ehcache properly, so that disk-persistent stores can actually be saved to disk
	 */
	private static void initializeCache() throws Exception {
		// prepare cache folder for this application instance
		File cacheRoot = getApplicationCache();

		try {
			for (int i = 0; true; i++) {
				File cache = new File(cacheRoot, String.format("%d", i));

				// make sure cache is accessible
				createFolders(cache);

				final File lockFile = new File(cache, ".lock");
				boolean isNewCache = !lockFile.exists();

				final FileChannel channel = FileChannel.open(lockFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
				final FileLock lock = channel.tryLock();

				if (lock != null) {
					// setup cache dir for ehcache
					System.setProperty("ehcache.disk.store.dir", cache.getAbsolutePath());

					int applicationRevision = getApplicationRevisionNumber();
					int cacheRevision = 0;
					try {
						cacheRevision = new Scanner(channel, "UTF-8").nextInt();
					} catch (Exception e) {
						// ignore
					}

					if (cacheRevision != applicationRevision && applicationRevision > 0 && !isNewCache) {
						Logger.getLogger(Main.class.getName()).log(Level.WARNING, String.format("App version (r%d) does not match cache version (r%d): reset cache", applicationRevision, cacheRevision));

						// tag cache with new revision number
						isNewCache = true;

						// delete all files related to previous cache instances
						for (File it : getChildren(cache)) {
							if (!it.equals(lockFile)) {
								delete(cache);
							}
						}
					}

					if (isNewCache) {
						// set new cache revision
						channel.position(0);
						channel.write(Charset.forName("UTF-8").encode(String.valueOf(applicationRevision)));
						channel.truncate(channel.position());
					}

					// make sure to orderly shutdown cache
					Runtime.getRuntime().addShutdownHook(new Thread() {

						@Override
						public void run() {
							try {
								CacheManager.getInstance().shutdown();
							} catch (Exception e) {
								// ignore, shutting down anyway
							}
							try {
								lock.release();
							} catch (Exception e) {
								// ignore, shutting down anyway
							}
							try {
								channel.close();
							} catch (Exception e) {
								// ignore, shutting down anyway
							}
						}
					});

					// cache for this application instance is successfully set up and locked
					return;
				}

				// try next lock file
				channel.close();
			}
		} catch (Exception e) {
			throw new Exception("Failed to initialize cache: " + e.toString(), e);
		}
	}

	/**
	 * Initialize default SecurityManager and grant all permissions via security policy. Initialization is required in order to run {@link ExpressionFormat} in a secure sandbox.
	 */
	private static void initializeSecurityManager() {
		try {
			// initialize security policy used by the default security manager
			// because default the security policy is very restrictive (e.g. no FilePermission)
			Policy.setPolicy(new Policy() {

				@Override
				public boolean implies(ProtectionDomain domain, Permission permission) {
					// all permissions
					return true;
				}

				@Override
				public PermissionCollection getPermissions(CodeSource codesource) {
					// VisualVM can't connect if this method does return
					// a checked immutable PermissionCollection
					return new Permissions();
				}
			});

			// set default security manager
			System.setSecurityManager(new SecurityManager());
		} catch (Exception e) {
			// security manager was probably set via system property
			Logger.getLogger(Main.class.getName()).log(Level.WARNING, e.toString(), e);
		}
	}

}
