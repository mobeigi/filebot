
package net.sourceforge.filebot;


import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.sourceforge.filebot.ui.FileBotWindow;
import net.sourceforge.filebot.ui.NotificationLoggingHandler;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


public class Main {
	
	/**
	 * @param args
	 */
	public static void main(String... args) throws Exception {
		
		final ArgumentBean argumentBean = initializeArgumentBean(args);
		
		if (argumentBean.isHelp()) {
			printUsage(argumentBean);
			
			// just print help message and exit afterwards
			System.exit(0);
		}
		
		if (argumentBean.isClear()) {
			// clear preferences
			Settings.userRoot().clear();
		}
		
		initializeLogging();
		initializeSettings();
		
		try {
			//			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			//			UIManager.setLookAndFeel("a03.swing.plaf.A03LookAndFeel");
			//			UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceBusinessBlueSteelLookAndFeel");
			//			UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceNebulaBrickWallLookAndFeel");
			//			UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceSaharaLookAndFeel");
			
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			Logger.getLogger("global").log(Level.SEVERE, e.toString(), e);
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				FileBotWindow window = new FileBotWindow();
				
				// publish messages from arguments to the newly created components
				argumentBean.publishMessages();
				
				// start
				window.setVisible(true);
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
	

	private static void initializeSettings() {
		Settings.userRoot().putDefault("thetvdb.apikey", "58B4AA94C59AD656");
	}
	

	private static ArgumentBean initializeArgumentBean(String... args) throws CmdLineException {
		ArgumentBean argumentBean = new ArgumentBean();
		
		new CmdLineParser(argumentBean).parseArgument(args);
		
		return argumentBean;
	}
	

	private static void printUsage(ArgumentBean argumentBean) {
		System.out.println("Options:");
		
		new CmdLineParser(argumentBean).printUsage(System.out);
	}
	
}
