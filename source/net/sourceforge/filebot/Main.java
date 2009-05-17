
package net.sourceforge.filebot;


import static javax.swing.JFrame.*;

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.ui.MainFrame;
import net.sourceforge.filebot.ui.NotificationLoggingHandler;
import net.sourceforge.filebot.ui.SinglePanelFrame;
import net.sourceforge.filebot.ui.panel.sfv.SfvPanelBuilder;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


public class Main {
	
	/**
	 * @param args
	 */
	public static void main(String... args) throws Exception {
		
		final ArgumentBean argumentBean = initializeArgumentBean(args);
		
		if (argumentBean.help()) {
			printUsage(argumentBean);
			
			// just print help message and exit afterwards
			System.exit(0);
		}
		
		if (argumentBean.clear()) {
			// clear preferences
			Settings.userRoot().clear();
		}
		
		initializeLogging();
		initializeSettings();
		initializeSecurityManager();
		
		try {
			// use native laf an all platforms
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			Logger.getLogger(Main.class.getName()).log(Level.WARNING, e.toString(), e);
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				JFrame frame;
				
				if (argumentBean.sfv()) {
					// sfv frame
					frame = new SinglePanelFrame(new SfvPanelBuilder()).publish(argumentBean.transferable());
				} else {
					// default frame
					frame = new MainFrame();
				}
				
				frame.setLocationByPlatform(true);
				frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
				
				// start application
				frame.setVisible(true);
			}
		});
	}
	

	private static void initializeLogging() {
		Logger uiLogger = Logger.getLogger("ui");
		
		// don't use parent handlers
		uiLogger.setUseParentHandlers(false);
		
		// ui handler
		uiLogger.addHandler(new NotificationLoggingHandler());
		
		// console handler (for warnings and errors only)
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.WARNING);
		
		uiLogger.addHandler(consoleHandler);
	}
	

	/**
	 * Preset the default thetvdb.apikey.
	 */
	private static void initializeSettings() {
		Settings.userRoot().putDefault("thetvdb.apikey", "58B4AA94C59AD656");
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
	private static ArgumentBean initializeArgumentBean(String... args) throws CmdLineException {
		ArgumentBean argumentBean = new ArgumentBean();
		
		new CmdLineParser(argumentBean).parseArgument(args);
		
		return argumentBean;
	}
	

	/**
	 * Print command line argument usage.
	 */
	private static void printUsage(ArgumentBean argumentBean) {
		System.out.println("Options:");
		
		new CmdLineParser(argumentBean).printUsage(System.out);
	}
	
}
