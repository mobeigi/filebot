import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.sourceforge.filebot.ArgumentBean;
import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.ui.FileBotWindow;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


public class Main {
	
	/**
	 * @param args
	 */
	public static void main(String... args) {
		
		final ArgumentBean argumentBean = parseArguments(args);
		
		if (argumentBean.isClear()) {
			try {
				Preferences.userNodeForPackage(FileBotUtil.class).removeNode();
			} catch (BackingStoreException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
		}
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
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
	

	private static ArgumentBean parseArguments(String... args) {
		
		ArgumentBean argumentBean = new ArgumentBean();
		CmdLineParser argumentParser = new CmdLineParser(argumentBean);
		
		try {
			argumentParser.parseArgument(args);
		} catch (CmdLineException e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, e.getMessage());
		}
		
		if (argumentBean.isHelp()) {
			System.out.println("Options:");
			argumentParser.printUsage(System.out);
			
			// just print help message and exit afterwards
			System.exit(0);
		}
		
		return argumentBean;
	}
	
}
