
package net.sourceforge.filebot.ui;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.ui.transfer.FileTransferable;
import net.sourceforge.filebot.ui.transferablepolicies.TransferablePolicy;
import net.sourceforge.tuned.MessageBus;
import net.sourceforge.tuned.MessageHandler;


public class FileTransferableMessageHandler implements MessageHandler {
	
	private final String name;
	private final TransferablePolicy transferablePolicy;
	
	
	public FileTransferableMessageHandler(String name, TransferablePolicy transferablePolicy) {
		this.name = name;
		this.transferablePolicy = transferablePolicy;
	}
	

	@Override
	public void handle(String topic, String... messages) {
		// change panel
		MessageBus.getDefault().publish("panel", name);
		
		List<File> files = new ArrayList<File>(messages.length);
		
		for (String filename : messages) {
			try {
				File file = new File(filename);
				
				if (file.exists()) {
					// file might be relative, use absolute file
					files.add(file.getCanonicalFile());
				} else {
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, String.format("Invalid File: %s", filename));
				}
			} catch (IOException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
		}
		
		transferablePolicy.handleTransferable(new FileTransferable(files), true);
	}
}
