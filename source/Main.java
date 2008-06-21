import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.ui.FileBotWindow;
import net.sourceforge.tuned.MessageBus;


public class Main {
	
	/**
	 * @param args
	 */
	public static void main(String... args) {
		
		final Arguments arguments = new Arguments(args);
		
		if (arguments.containsParameter("clear")) {
			Settings.getSettings().clear();
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
				arguments.publishMessages();
				
				// start
				window.setVisible(true);
			}
		});
	}
	
	
	private static class Arguments {
		
		private final Set<String> parameters = new HashSet<String>(3);
		private final Map<String, List<String>> messages = new LinkedHashMap<String, List<String>>();
		
		
		public Arguments(String[] args) {
			Pattern topicPattern = Pattern.compile("--(\\w+)");
			
			String currentTopic = null;
			
			for (String arg : args) {
				Matcher m = topicPattern.matcher(arg);
				
				if (m.matches()) {
					currentTopic = m.group(1).toLowerCase();
					messages.put(currentTopic, new ArrayList<String>(1));
				} else if (currentTopic != null) {
					messages.get(currentTopic).add(arg);
				} else {
					parameters.add(arg.toLowerCase());
				}
			}
		}
		

		public boolean containsParameter(String argument) {
			return parameters.contains(argument);
		}
		

		public void publishMessages() {
			for (String topic : messages.keySet()) {
				MessageBus.getDefault().publish(topic, messages.get(topic).toArray(new String[0]));
			}
		}
	}
	
}
