
package net.sourceforge.filebot.ui;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.ui.transfer.FileTransferable;
import net.sourceforge.filebot.ui.transfer.TransferablePolicySupport;
import net.sourceforge.tuned.MessageBus;
import net.sourceforge.tuned.MessageHandler;


public class FileTransferableMessageHandler implements MessageHandler {
	
	private final String name;
	private final TransferablePolicySupport transferablePolicySupport;
	
	
	public FileTransferableMessageHandler(String name, TransferablePolicySupport transferablePolicySupport) {
		this.name = name;
		this.transferablePolicySupport = transferablePolicySupport;
	}
	

	@Override
	public void handle(String topic, String... messages) {
		
		MessageBus.getDefault().publish("panel", name);
		
		List<File> files = new ArrayList<File>(messages.length);
		
		for (String filename : messages) {
			File file = new File(filename);
			
			if (file.exists()) {
				files.add(file);
			} else {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, String.format("Invalid File: %s", filename));
			}
		}
		
		transferablePolicySupport.getTransferablePolicy().handleTransferable(new FileTransferable(files), true);
	}
}
