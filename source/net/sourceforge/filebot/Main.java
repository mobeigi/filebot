package net.sourceforge.filebot;

import static java.awt.GraphicsEnvironment.*;
import static java.util.regex.Pattern.*;
import static javax.swing.JOptionPane.*;
import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Desktop;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
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

import net.miginfocom.swing.MigLayout;
import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.cli.ArgumentBean;
import net.sourceforge.filebot.cli.ArgumentProcessor;
import net.sourceforge.filebot.cli.CmdlineOperations;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.gio.GVFS;
import net.sourceforge.filebot.media.MediaDetection;
import net.sourceforge.filebot.ui.MainFrame;
import net.sourceforge.filebot.ui.PanelBuilder;
import net.sourceforge.filebot.ui.SinglePanelFrame;
import net.sourceforge.filebot.web.CachedResource;
import net.sourceforge.tuned.ByteBufferInputStream;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;
import net.sourceforge.tuned.TeePrintStream;

import org.w3c.dom.NodeList;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String... arguments) {
		try {
			// parse arguments
			final ArgumentProcessor cli = new ArgumentProcessor();
			final ArgumentBean args = cli.parse(arguments);

			if (args.printHelp() || args.printVersion() || (!(args.runCLI() || args.clearCache() || args.clearUserData()) && isHeadless())) {
				System.out.format("%s / %s%n%n", getApplicationIdentifier(), getJavaRuntimeIdentifier());

				if (args.printHelp() || (!args.printVersion() && isHeadless())) {
					cli.printHelp(args);
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
					for (File folder : getApplicationFolder().getAbsoluteFile().listFiles(FOLDERS)) {
						if (matches("cache|temp|grape|logs", folder.getName())) {
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
				FileChannel logChannel = new FileOutputStream(logFile, true).getChannel();
				if (args.logLock) {
					System.out.println("Locking " + logFile);
					logChannel.lock();
				}

				OutputStream out = Channels.newOutputStream(logChannel);
				System.setOut(new TeePrintStream(out, true, "UTF-8", System.out));
				System.setErr(new TeePrintStream(out, true, "UTF-8", System.err));
			}

			// make sure java.io.tmpdir exists
			File tmpdir = new File(System.getProperty("java.io.tmpdir"));
			tmpdir.mkdirs();

			// initialize this stuff before anything else
			initializeCache();
			initializeSecurityManager();

			// update system properties
			System.setProperty("grape.root", new File(getApplicationFolder(), "grape").getAbsolutePath());

			if (System.getProperty("http.agent") == null) {
				System.setProperty("http.agent", String.format("%s %s", getApplicationName(), getApplicationVersion()));
			}
			if (args.unixfs) {
				System.setProperty("unixfs", "true");
			}
			if (args.disableAnalytics) {
				System.setProperty("application.analytics", "false");
			}
			if (args.disableExtendedAttributes) {
				System.setProperty("useExtendedFileAttributes", "false");
				System.setProperty("useCreationDate", "false");
			}
			if (args.action.equalsIgnoreCase("test")) {
				System.setProperty("useExtendedFileAttributes", "false");
				System.setProperty("application.analytics", "false");
				System.setProperty("application.rename.history", "false"); // don't keep history of --action test rename operations
			}

			// initialize analytics
			Analytics.setEnabled(System.getProperty("application.analytics") == null ? true : Boolean.parseBoolean(System.getProperty("application.analytics")));
			HistorySpooler.getInstance().setPersistentHistoryEnabled(System.getProperty("application.rename.history") == null ? true : Boolean.parseBoolean(System.getProperty("application.rename.history")));

			// CLI mode => run command-line interface and then exit
			if (args.runCLI()) {
				// commit session history on shutdown
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

					@Override
					public void run() {
						HistorySpooler.getInstance().commit();
					}
				}));

				// default cross-platform laf used in scripting to nimbus instead of metal (if possible)
				if (args.script != null && !isHeadless()) {
					try {
						Class<?> nimbusLook = Class.forName("javax.swing.plaf.nimbus.NimbusLookAndFeel", false, Thread.currentThread().getContextClassLoader());
						System.setProperty("swing.crossplatformlaf", nimbusLook.getName());
					} catch (Throwable e) {
						// ignore all errors and stick with default cross-platform laf
					}
				}

				int status = cli.process(args, new CmdlineOperations());
				System.exit(status);
			}

			// GUI mode => start user interface
			try {
				SwingUtilities.invokeAndWait(new Runnable() {

					@Override
					public void run() {
						try {
							// use native laf an all platforms
							UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
						} catch (Exception e) {
							Logger.getLogger(Main.class.getName()).log(Level.WARNING, e.getMessage(), e);
						}

						startUserInterface(args);
					}
				});
			} catch (InvocationTargetException e) {
				Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getCause().getMessage(), e.getCause());
				System.exit(-1); // starting up UI failed
			} catch (InterruptedException e) {
				throw new RuntimeException(e); // won't happen
			}

			// pre-load media.types and JNA/GIO (when loaded during DnD it will freeze the UI for a few hundred milliseconds)
			MediaTypes.getDefault();

			if (useGVFS()) {
				try {
					GVFS.getDefaultVFS();
				} catch (Throwable e) {
					Logger.getLogger(Main.class.getName()).log(Level.SEVERE, e.getMessage(), e);
				}
			}

			// check for application updates (only when installed, i.e. not running via fatjar or webstart)
			if (!"skip".equals(System.getProperty("application.update"))) {
				try {
					checkUpdate();
				} catch (Exception e) {
					Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Failed to check for updates", e);
				}
			}

			// pre-load certain resources in the background
			if (Boolean.parseBoolean(System.getProperty("application.warmup"))) {
				MediaDetection.warmupCachedResources();
			}
		} catch (Exception e) {
			// illegal arguments => just print CLI error message and stop
			System.err.println(e.getMessage());
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
					frame = new SinglePanelFrame(it);
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
			// don't care, doesn't make a difference
		}

		frame.setLocationByPlatform(true);
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				e.getWindow().setVisible(false);
				HistorySpooler.getInstance().commit();

				if (useDonationReminder()) {
					showDonationReminder();
				}

				System.exit(0);
			}
		});

		// start application
		frame.setVisible(true);
	}

	/**
	 * Show update notifications if updates are available
	 */
	private static void checkUpdate() throws Exception {
		final Properties updateProperties = new CachedResource<Properties>(getApplicationProperty("update.url"), Properties.class, CachedResource.ONE_DAY, 0, 0) {

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

	private static void showDonationReminder() {
		int renameCount = HistorySpooler.getInstance().getPersistentHistoryTotalSize();
		if (renameCount < 2000)
			return;

		PreferencesEntry<String> donation = Settings.forPackage(Main.class).entry("donation").defaultValue("0");
		int donationRev = Integer.parseInt(donation.getValue());
		int currentRev = getApplicationRevisionNumber();
		if (donationRev >= currentRev) {
			return;
		}

		String message = String.format(Locale.ROOT,
				"<html><p style='font-size:16pt; font-weight:bold'>Thank you for using FileBot!</p><br><p>It has taken many nights to develop this application. If you enjoy using it,<br>please consider a donation to the author of this software. It will help to<br>make FileBot even better!<p><p style='font-size:14pt; font-weight:bold'>You've renamed %,d files.</p><br><html>", renameCount);
		String[] actions = new String[] { "Donate! :)", donationRev > 0 ? "Not this time" : "Later" };
		JOptionPane pane = new JOptionPane(message, INFORMATION_MESSAGE, YES_NO_OPTION, ResourceManager.getIcon("message.donate"), actions, actions[0]);
		pane.createDialog(null, "Please Donate").setVisible(true);
		if (pane.getValue() == actions[0]) {
			try {
				Desktop.getDesktop().browse(URI.create(getApplicationProperty("donate.url")));
				donation.setValue(String.valueOf(currentRev));
			} catch (Exception e) {
				Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Failed to open URL.", e);
			}
		} else {
			if (donationRev > 0 && donationRev < currentRev) {
				donation.setValue(String.valueOf(currentRev));
			}
		}
		Analytics.trackEvent("GUI", "Donate", "r" + currentRev, pane.getValue() == actions[0] ? 1 : 0);
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
	private static void initializeCache() {
		// prepare cache folder for this application instance
		File cacheRoot = new File(getApplicationFolder(), "cache");

		try {
			for (int i = 0; true; i++) {
				File cache = new File(cacheRoot, String.format("%d", i));
				if (!cache.isDirectory() && !cache.mkdirs()) {
					throw new IOException("Failed to create cache dir: " + cache);
				}

				final File lockFile = new File(cache, ".lock");
				boolean isNewCache = !lockFile.exists();

				final RandomAccessFile handle = new RandomAccessFile(lockFile, "rw");
				final FileChannel channel = handle.getChannel();
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
						for (File it : cache.listFiles()) {
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
								handle.close();
							} catch (Exception e) {
								// ignore, shutting down anyway
							}
						}
					});

					// cache for this application instance is successfully set up and locked
					return;
				}

				// try next lock file
				handle.close();
			}
		} catch (Exception e) {
			Logger.getLogger(Main.class.getName()).log(Level.WARNING, e.toString(), e);
		}

		// use cache root itself as fail-safe fallback
		System.setProperty("ehcache.disk.store.dir", new File(cacheRoot, "default").getAbsolutePath());
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
