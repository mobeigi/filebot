
package net.sourceforge.filebot;


import static javax.swing.JFrame.EXIT_ON_CLOSE;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.sourceforge.filebot.ui.MainFrame;
import net.sourceforge.filebot.ui.NotificationLoggingHandler;
import net.sourceforge.filebot.ui.SinglePanelFrame;
import net.sourceforge.filebot.ui.panel.analyze.AnalyzePanelBuilder;
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
				
				if (argumentBean.analyze()) {
					frame = new SinglePanelFrame(new AnalyzePanelBuilder()).publish(argumentBean.transferable());
				} else if (argumentBean.sfv()) {
					frame = new SinglePanelFrame(new SfvPanelBuilder()).publish(argumentBean.transferable());
				} else {
					// default
					frame = new MainFrame();
				}
				
				frame.setLocationByPlatform(true);
				frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
				
				// start
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
