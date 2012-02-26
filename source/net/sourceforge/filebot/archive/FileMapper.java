
package net.sourceforge.filebot.archive;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class FileMapper implements ExtractOutProvider {
	
	private File outputDir;
	private boolean flatten;
	
	
	public FileMapper(File outputDir, boolean flatten) {
		this.outputDir = outputDir;
		this.flatten = flatten;
	};
	
	
	public File getOutputFile(File entry) {
		return new File(outputDir, flatten ? entry.getName() : entry.getPath());
	}
	
	
	@Override
	public OutputStream getStream(File entry) throws IOException {
		File outputFile = getOutputFile(entry);
		File outputFolder = outputFile.getParentFile();
		
		// create parent folder if necessary
		if (!outputFolder.isDirectory() && !outputFolder.mkdirs()) {
			throw new IOException("Failed to create folder: " + outputFolder);
		}
		
		return new FileOutputStream(outputFile);
	}
}
