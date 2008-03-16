
package net.sourceforge.filebot.ui.panel.sfv;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.FileFormat;
import net.sourceforge.filebot.ui.transferablepolicies.BackgroundFileTransferablePolicy;


public class SfvTransferablePolicy extends BackgroundFileTransferablePolicy<SfvTableModel.Entry> {
	
	private SfvTableModel tableModel;
	
	
	public SfvTransferablePolicy(SfvTableModel tableModel) {
		this.tableModel = tableModel;
	}
	

	@Override
	protected boolean accept(File file) {
		return file.isFile() || file.isDirectory();
	}
	

	@Override
	protected void clear() {
		cancelAll();
		tableModel.clear();
	}
	

	@Override
	protected void process(List<SfvTableModel.Entry> chunks) {
		tableModel.addAll(chunks);
	}
	

	protected void loadSfvFile(File sfvFile) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(sfvFile)));
			
			String line = null;
			Pattern pattern = Pattern.compile("(.*)\\s+(\\p{XDigit}{8})");
			
			while (((line = in.readLine()) != null) && !Thread.currentThread().isInterrupted()) {
				if (line.startsWith(";"))
					continue;
				
				Matcher matcher = pattern.matcher(line);
				
				if (!matcher.matches())
					continue;
				
				String filename = matcher.group(1);
				String checksumString = matcher.group(2);
				
				publish(new SfvTableModel.Entry(filename, new Checksum(checksumString), sfvFile));
				
				File compareColumnRoot = sfvFile.getParentFile();
				File compareFile = new File(compareColumnRoot, filename);
				
				if (compareFile.exists()) {
					publish(new SfvTableModel.Entry(filename, new Checksum(compareFile), compareColumnRoot));
				}
			}
			
			in.close();
		} catch (IOException e) {
			// should not happen
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
		}
	}
	

	@Override
	public String getDescription() {
		return "files, folders and sfv files";
	}
	

	private boolean isSfvFileList(List<File> files) {
		for (File file : files) {
			if (!FileFormat.getSuffix(file).equalsIgnoreCase("sfv"))
				return false;
		}
		
		return true;
	}
	

	@Override
	protected void load(List<File> files) {
		synchronized (ChecksumComputationExecutor.getInstance()) {
			ChecksumComputationExecutor.getInstance().pause();
			
			if (isSfvFileList(files)) {
				// one or more sfv files
				for (File file : files) {
					loadSfvFile(file);
				}
			} else if ((files.size() == 1) && files.get(0).isDirectory()) {
				// one single folder
				File file = files.get(0);
				
				for (File f : file.listFiles()) {
					load(f, file, "");
				}
			} else {
				// bunch of files
				for (File f : files) {
					load(f, f.getParentFile(), "");
				}
			}
			
			ChecksumComputationExecutor.getInstance().resume();
		}
	}
	

	protected void load(File file, File columnRoot, String prefix) {
		if (Thread.currentThread().isInterrupted())
			return;
		
		if (file.isDirectory()) {
			// load all files in the file tree
			String newPrefix = prefix + file.getName() + "/";
			for (File f : file.listFiles()) {
				load(f, columnRoot, newPrefix);
			}
		} else if (file.isFile()) {
			publish(new SfvTableModel.Entry(prefix + file.getName(), new Checksum(file), columnRoot));
		}
	}
	
}
