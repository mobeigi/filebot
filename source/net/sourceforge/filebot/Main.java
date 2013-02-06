
package net.sourceforge.filebot;


import static java.awt.GraphicsEnvironment.*;
import static java.util.regex.Pattern.*;
import static javax.swing.JFrame.*;
import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Desktop;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import net.sourceforge.filebot.ui.SinglePanelFrame;
import net.sourceforge.filebot.ui.sfv.SfvPanelBuilder;
import net.sourceforge.filebot.ui.transfer.FileTransferable;
import net.sourceforge.filebot.web.CachedResource;
import net.sourceforge.tuned.ByteBufferInputStream;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;

import org.kohsuke.args4j.CmdLineException;
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
			
			if (args.printHelp() || args.printVersion() || (!args.runCLI() && isHeadless())) {
				System.out.format("%s / %s%n%n", getApplicationIdentifier(), getJavaRuntimeIdentifier());
				
				if (args.printHelp() || (!args.printVersion() && isHeadless())) {
					cli.printHelp(args);
				}
				
				// just print help message or version string and then exit
				System.exit(0);
			}
			
			// make sure java.io.tmpdir exists
			File tmpdir = new File(System.getProperty("java.io.tmpdir"));
			tmpdir.mkdirs();
			
			// initialize this stuff before anything else
			initializeCache();
			initializeSecurityManager();
			
			if (args.clearUserData()) {
				System.out.println("Reset preferences");
				Settings.forPackage(Main.class).clear();
			}
			
			if (args.clearCache()) {
				// clear preferences and cache
				System.out.println("Clear cache and temporary files");
				for (File cache : getApplicationFolder().listFiles(FOLDERS)) {
					if (matches("cache|temp|grape", cache.getName())) {
						for (File it : cache.listFiles()) {
							delete(it);
						}
					}
				}
				CacheManager.getInstance().clearAll();
			}
			
			// update system properties
			if (System.getProperty("http.agent") == null) {
				System.setProperty("http.agent", String.format("%s %s", getApplicationName(), getApplicationVersion()));
			}
			if (args.unixfs) {
				System.setProperty("unixfs", "true");
			}
			if (args.disableAnalytics) {
				System.setProperty("application.analytics", "false");
			}
			
			// initialize analytics
			Analytics.setEnabled(System.getProperty("application.analytics") == null ? true : Boolean.getBoolean(System.getProperty("application.analytics")));
			
			// CLI mode => run command-line interface and then exit
			if (args.runCLI()) {
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
			
			// pre-load certain resources in the background
			if (!"skip".equals(System.getProperty("application.warmup"))) {
				warmupCachedResources();
			}
			
			// check for application updates (only when installed, i.e. not running via fatjar or webstart)
			if (!"skip".equals(System.getProperty("application.update"))) {
				try {
					checkUpdate();
				} catch (Exception e) {
					Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Failed to check for updates", e);
				}
			}
		} catch (CmdLineException e) {
			// illegal arguments => just print CLI error message and stop
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}
	
	
	private static void startUserInterface(ArgumentBean args) {
		JFrame frame;
		
		if (args.openSFV()) {
			// single panel frame
			FileTransferable files = new FileTransferable(args.getFiles(false));
			frame = new SinglePanelFrame(new SfvPanelBuilder()).publish(files);
		} else {
			// default frame
			frame = new MainFrame();
		}
		
		frame.setLocationByPlatform(true);
		frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		try {
			// restore previous size and location
			restoreWindowBounds(frame, Settings.forPackage(MainFrame.class));
		} catch (Exception e) {
			// don't care, doesn't make a difference
		}
		
		// start application
		frame.setVisible(true);
	}
	
	
	/**
	 * Show update notifications if updates are available
	 */
	private static void checkUpdate() throws Exception {
		final PreferencesEntry<String> updateIgnoreRevision = Settings.forPackage(Main.class).entry("update.revision.ignore");
		final Properties updateProperties = new CachedResource<Properties>(getApplicationProperty("update.url"), Properties.class, 24 * 60 * 60 * 1000) {
			
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
		int latestIgnoreRev = Math.max(getApplicationRevisionNumber(), updateIgnoreRevision.getValue() == null ? 0 : Integer.parseInt(updateIgnoreRevision.getValue()));
		
		if (latestRev > latestIgnoreRev) {
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
							updateIgnoreRevision.setValue(updateProperties.getProperty("revision"));
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
	
	
	private static void warmupCachedResources() {
		Thread warmup = new Thread("warmupCachedResources") {
			
			@Override
			public void run() {
				try {
					// pre-load filter data
					MediaDetection.getClutterFileFilter();
					MediaDetection.getDiskFolderFilter();
					
					// pre-load movie/series index
					List<String> dummy = Collections.singletonList("");
					MediaDetection.stripReleaseInfo(dummy, true);
					MediaDetection.matchSeriesByName(dummy, -1);
					MediaDetection.matchMovieName(dummy, true, -1);
				} catch (Exception e) {
					Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
				}
			}
		};
		
		// start background thread
		warmup.setDaemon(true);
		warmup.setPriority(Thread.MIN_PRIORITY);
		warmup.start();
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
				
				File lockFile = new File(cache, ".lock");
				final RandomAccessFile handle = new RandomAccessFile(lockFile, "rw");
				final FileLock lock = handle.getChannel().tryLock();
				if (lock != null) {
					// setup cache dir for ehcache
					System.setProperty("ehcache.disk.store.dir", cache.getAbsolutePath());
					
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
	 * Initialize default SecurityManager and grant all permissions via security policy.
	 * Initialization is required in order to run {@link ExpressionFormat} in a secure sandbox.
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
