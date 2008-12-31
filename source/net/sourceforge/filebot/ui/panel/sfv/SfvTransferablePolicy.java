
package net.sourceforge.filebot.ui.panel.sfv;


import static net.sourceforge.filebot.FileBotUtil.SFV_FILE_EXTENSIONS;
import static net.sourceforge.filebot.FileBotUtil.containsOnly;

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

import net.sourceforge.filebot.ui.transfer.BackgroundFileTransferablePolicy;


class SfvTransferablePolicy extends BackgroundFileTransferablePolicy<ChecksumTableModel.ChecksumCell> {
	
	private final ChecksumTableModel tableModel;
	private final ChecksumComputationService checksumComputationService;
	
	
	public SfvTransferablePolicy(ChecksumTableModel tableModel, ChecksumComputationService checksumComputationService) {
		this.tableModel = tableModel;
		this.checksumComputationService = checksumComputationService;
	}
	

	@Override
	protected boolean accept(List<File> files) {
		return true;
	}
	

	@Override
	protected void clear() {
		checksumComputationService.reset();
		tableModel.clear();
	}
	

	@Override
	protected void process(List<ChecksumTableModel.ChecksumCell> chunks) {
		tableModel.addAll(chunks);
	}
	

	protected void loadSfvFile(File sfvFile) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(sfvFile), "UTF-8"));
			
			String line = null;
			Pattern pattern = Pattern.compile("(.*)\\s+(\\p{XDigit}{8})");
			
			while (((line = in.readLine()) != null) && !Thread.interrupted()) {
				if (line.startsWith(";"))
					continue;
				
				Matcher matcher = pattern.matcher(line);
				
				if (!matcher.matches())
					continue;
				
				String filename = matcher.group(1);
				String checksumString = matcher.group(2);
				
				publish(new ChecksumTableModel.ChecksumCell(filename, new Checksum(checksumString), sfvFile));
				
				File column = sfvFile.getParentFile();
				File file = new File(column, filename);
				
				if (file.exists()) {
					publish(new ChecksumTableModel.ChecksumCell(filename, checksumComputationService.schedule(file, column), column));
				}
			}
			
			in.close();
		} catch (IOException e) {
			// should not happen
			Logger.getLogger("global").log(Level.SEVERE, e.toString(), e);
		}
	}
	

	@Override
	public String getFileFilterDescription() {
		return "files, folders and sfv files";
	}
	

	@Override
	protected void load(List<File> files) {
		try {
			if (containsOnly(files, SFV_FILE_EXTENSIONS)) {
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
		} catch (InterruptedException e) {
			// supposed to happen if background execution was aborted
		}
	}
	

	protected void load(File file, File column, String prefix) throws InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		
		if (file.isDirectory()) {
			// load all files in the file tree
			String newPrefix = prefix + file.getName() + "/";
			for (File f : file.listFiles()) {
				load(f, column, newPrefix);
			}
		} else if (file.isFile()) {
			publish(new ChecksumTableModel.ChecksumCell(prefix + file.getName(), checksumComputationService.schedule(file, column), column));
		}
	}
	
}
