
package net.sourceforge.filebot;


import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.tuned.MessageBus;

import org.kohsuke.args4j.Option;


public class ArgumentBean {
	
	@Option(name = "-help", usage = "Print this help message")
	private boolean help = false;
	
	@Option(name = "-clear", usage = "Clear history and settings")
	private boolean clear = false;
	
	@Message(topic = "list")
	@Option(name = "--list", usage = "Open file in 'List' panel", metaVar = "<file>")
	private File listPanelFile;
	
	@Message(topic = "analyze")
	@Option(name = "--analyze", usage = "Open file in 'Analyze' panel", metaVar = "<file>")
	private File analyzePanelFile;
	
	@Message(topic = "sfv")
	@Option(name = "--sfv", usage = "Open file in 'SFV' panel", metaVar = "<file>")
	private File sfvPanelFile;
	
	
	public boolean isHelp() {
		return help;
	}
	

	public boolean isClear() {
		return clear;
	}
	

	public File getListPanelFile() {
		return listPanelFile;
	}
	

	public File getAnalyzePanelFile() {
		return analyzePanelFile;
	}
	

	public File getSfvPanelFile() {
		return sfvPanelFile;
	}
	

	public void publishMessages() {
		for (Field field : getClass().getDeclaredFields()) {
			
			Message message = field.getAnnotation(Message.class);
			
			if (message == null)
				continue;
			
			try {
				Object value = field.get(this);
				
				if (value != null) {
					MessageBus.getDefault().publish(message.topic(), value);
				}
			} catch (Exception e) {
				// should not happen
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
		}
	}
	
	
	@Retention(RUNTIME)
	@Target(FIELD)
	private @interface Message {
		
		String topic();
	}
	
}
