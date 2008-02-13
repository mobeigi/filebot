import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.sourceforge.filebot.ui.FileBotWindow;


public class Main {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).setLevel(Level.ALL);
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			
			public void run() {
				new FileBotWindow().setVisible(true);
			}
		});
	}
	
}
