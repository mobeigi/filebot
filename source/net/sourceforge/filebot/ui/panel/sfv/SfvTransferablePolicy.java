
package net.sourceforge.filebot.ui.panel.sfv;


import static net.sourceforge.filebot.FileBotUtilities.SFV_FILES;
import static net.sourceforge.tuned.FileUtilities.containsOnly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.ui.transfer.BackgroundFileTransferablePolicy;
import net.sourceforge.tuned.ExceptionUtilities;


class SfvTransferablePolicy extends BackgroundFileTransferablePolicy<ChecksumCell> {
	
	private final ChecksumTableModel model;
	private final ChecksumComputationService checksumComputationService;
	
	
	public SfvTransferablePolicy(ChecksumTableModel model, ChecksumComputationService checksumComputationService) {
		this.model = model;
		this.checksumComputationService = checksumComputationService;
	}
	

	@Override
	protected boolean accept(List<File> files) {
		return true;
	}
	

	@Override
	protected void clear() {
		checksumComputationService.reset();
		model.clear();
	}
	

	@Override
	protected void process(List<ChecksumCell> chunks) {
		model.addAll(chunks);
	}
	

	@Override
	protected void process(Exception e) {
		Logger.getLogger("ui").log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
	}
	

	protected void loadSfvFile(File sfvFile, Executor executor) {
		try {
			// don't use new Scanner(File) because of BUG 6368019 (http://bugs.sun.com/view_bug.do?bug_id=6368019)
			Scanner scanner = new Scanner(new FileInputStream(sfvFile), "utf-8");
			
			try {
				Pattern pattern = Pattern.compile("(.+)\\s+(\\p{XDigit}{8})");
				
				// root for relative file paths in sfv file
				File root = sfvFile.getParentFile();
				
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					
					if (line.startsWith(";"))
						continue;
					
					Matcher matcher = pattern.matcher(line);
					
					if (!matcher.matches())
						continue;
					
					String name = matcher.group(1);
					String checksum = matcher.group(2);
					
					ChecksumCell correct = new ChecksumCell(name, sfvFile, Collections.singletonMap(HashType.CRC32, checksum));
					ChecksumCell current = createChecksumCell(name, root, new File(root, name), executor);
					
					publish(correct, current);
					
					if (Thread.interrupted()) {
						break;
					}
				}
			} finally {
				scanner.close();
			}
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
		ExecutorService executor = checksumComputationService.newExecutor();
		
		try {
			if (containsOnly(files, SFV_FILES)) {
				// one or more sfv files
				for (File file : files) {
					loadSfvFile(file, executor);
				}
			} else if ((files.size() == 1) && files.get(0).isDirectory()) {
				// one single folder
				File file = files.get(0);
				
				for (File f : file.listFiles()) {
					load(f, file, "", executor);
				}
			} else {
				// bunch of files
				for (File f : files) {
					load(f, f.getParentFile(), "", executor);
				}
			}
		} catch (InterruptedException e) {
			// supposed to happen if background execution was aborted
		} finally {
			executor.shutdown();
		}
	}
	

	protected void load(File file, File root, String prefix, Executor executor) throws InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		
		if (file.isDirectory()) {
			// load all files in the file tree
			String newPrefix = prefix + file.getName() + "/";
			
			for (File f : file.listFiles()) {
				load(f, root, newPrefix, executor);
			}
		} else if (file.isFile()) {
			publish(createChecksumCell(prefix + file.getName(), root, file, executor));
		}
	}
	

	protected ChecksumCell createChecksumCell(String name, File root, File file, Executor executor) {
		ChecksumCell cell = new ChecksumCell(name, root, new ChecksumComputationTask(new File(root, name), HashType.CRC32));
		
		// start computation task
		executor.execute(cell.getTask());
		
		return cell;
	}
}
