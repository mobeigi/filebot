package net.sourceforge.filebot;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

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
	public static void main(String... args) {
		
		setupLogging();
		
		final ArgumentBean argumentBean = handleArguments(args);
		
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
	

	private static void setupLogging() {
		Logger uiLogger = Logger.getLogger("ui");
		uiLogger.addHandler(new NotificationLoggingHandler());
		uiLogger.setUseParentHandlers(false);
	}
	

	private static ArgumentBean handleArguments(String... args) {
		
		ArgumentBean argumentBean = new ArgumentBean();
		CmdLineParser argumentParser = new CmdLineParser(argumentBean);
		
		try {
			argumentParser.parseArgument(args);
		} catch (CmdLineException e) {
			Logger.getLogger("global").log(Level.WARNING, e.getMessage());
		}
		
		if (argumentBean.isHelp()) {
			System.out.println("Options:");
			argumentParser.printUsage(System.out);
			
			// just print help message and exit afterwards
			System.exit(0);
		}
		
		if (argumentBean.isClear()) {
			// clear preferences
			try {
				Preferences.userNodeForPackage(FileBotUtil.class).removeNode();
			} catch (BackingStoreException e) {
				Logger.getLogger("global").log(Level.SEVERE, e.toString(), e);
			}
		}
		
		return argumentBean;
	}
	
}
