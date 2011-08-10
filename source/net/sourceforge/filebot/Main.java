
package net.sourceforge.filebot;


import static javax.swing.JFrame.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.kohsuke.args4j.CmdLineParser;

import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.ui.MainFrame;
import net.sourceforge.filebot.ui.SinglePanelFrame;
import net.sourceforge.filebot.ui.panel.sfv.SfvPanelBuilder;


public class Main {
	
	/**
	 * @param args
	 */
	public static void main(String... args) throws Exception {
		// initialize this stuff before anything else
		initializeCache();
		initializeSecurityManager();
		
		// parse arguments
		final ArgumentBean argumentBean = initializeArgumentBean(args);
		
		if (argumentBean.help()) {
			printUsage(argumentBean);
			
			// just print help message and exit afterwards
			System.exit(0);
		}
		
		if (argumentBean.clear()) {
			// clear preferences and cache
			Settings.forPackage(Main.class).clear();
			CacheManager.getInstance().clearAll();
		}
		
		try {
			// use native laf an all platforms
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			Logger.getLogger(Main.class.getName()).log(Level.WARNING, e.getMessage(), e);
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				JFrame frame;
				
				if (argumentBean.sfv()) {
					// single panel frame
					frame = new SinglePanelFrame(new SfvPanelBuilder()).publish(argumentBean.transferable());
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
		});
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
		Runtime.getRuntime().addShutdownHook(new Thread() {
			
			@Override
			public void run() {
				CacheManager.getInstance().shutdown();
			}
		});
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
	

	/**
	 * Parse command line arguments.
	 */
	private static ArgumentBean initializeArgumentBean(String... args) {
		ArgumentBean argumentBean = new ArgumentBean();
		
		if (args != null && args.length > 0) {
			try {
				CmdLineParser parser = new CmdLineParser(argumentBean);
				parser.parseArgument(args);
			} catch (Exception e) {
				Logger.getLogger(Main.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}
		}
		
		return argumentBean;
	}
	

	/**
	 * Print command line argument usage.
	 */
	private static void printUsage(ArgumentBean argumentBean) {
		System.out.println("Options:");
		
		CmdLineParser parser = new CmdLineParser(argumentBean);
		parser.printUsage(System.out);
	}
	
}
