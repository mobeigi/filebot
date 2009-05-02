
package net.sourceforge.filebot.ui.panel.sfv;


import static net.sourceforge.tuned.FileUtilities.containsOnly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.hash.HashType;
import net.sourceforge.filebot.hash.IllegalSyntaxException;
import net.sourceforge.filebot.hash.VerificationFileScanner;
import net.sourceforge.filebot.ui.transfer.BackgroundFileTransferablePolicy;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.FileUtilities.ExtensionFileFilter;


class ChecksumTableTransferablePolicy extends BackgroundFileTransferablePolicy<ChecksumCell> {
	
	private final ChecksumTableModel model;
	private final ChecksumComputationService computationService;
	
	
	public ChecksumTableTransferablePolicy(ChecksumTableModel model, ChecksumComputationService checksumComputationService) {
		this.model = model;
		this.computationService = checksumComputationService;
	}
	

	@Override
	protected boolean accept(List<File> files) {
		return true;
	}
	

	@Override
	protected void clear() {
		super.clear();
		
		computationService.reset();
		model.clear();
	}
	

	@Override
	protected void prepare(List<File> files) {
		HashType type = getVerificationType(files);
		
		if (type != null) {
			model.setHashType(type);
		}
	}
	

	@Override
	protected void process(List<ChecksumCell> chunks) {
		model.addAll(chunks);
	}
	

	@Override
	protected void process(Exception e) {
		Logger.getLogger("ui").log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
	}
	

	protected HashType getVerificationType(List<File> files) {
		for (HashType hash : HashType.values()) {
			if (containsOnly(files, new ExtensionFileFilter(hash.getExtension()))) {
				return hash;
			}
		}
		
		return null;
	}
	

	protected void loadVerificationFile(File file, HashType type) throws IOException {
		// don't use new Scanner(File) because of BUG 6368019 (http://bugs.sun.com/view_bug.do?bug_id=6368019)
		VerificationFileScanner scanner = type.newScanner(new Scanner(new FileInputStream(file), "UTF-8"));
		
		try {
			// root for relative file paths in verification file
			File root = file.getParentFile();
			
			while (scanner.hasNext()) {
				try {
					Entry<File, String> entry = scanner.next();
					
					String name = normalizeRelativePath(entry.getKey());
					String hash = entry.getValue();
					
					ChecksumCell correct = new ChecksumCell(name, file, Collections.singletonMap(type, hash));
					ChecksumCell current = createComputationCell(name, root, type);
					
					publish(correct, current);
					
					if (Thread.interrupted()) {
						break;
					}
				} catch (IllegalSyntaxException e) {
					// tell user about illegal lines in verification file
					publish(e);
				}
			}
		} finally {
			scanner.close();
		}
	}
	
	private final ThreadLocal<ExecutorService> executor = new ThreadLocal<ExecutorService>();
	
	
	@Override
	protected void load(List<File> files) throws IOException {
		// initialize drop parameters
		executor.set(computationService.newExecutor());
		
		try {
			HashType verificationType = getVerificationType(files);
			
			if (verificationType != null) {
				for (File file : files) {
					loadVerificationFile(file, verificationType);
				}
			} else if ((files.size() == 1) && files.get(0).isDirectory()) {
				// one single folder
				File file = files.get(0);
				
				for (File f : file.listFiles()) {
					load(f, null, file);
				}
			} else {
				// bunch of files
				for (File f : files) {
					load(f, null, f.getParentFile());
				}
			}
		} catch (InterruptedException e) {
			// supposed to happen if background execution was aborted
		} finally {
			// shutdown executor after all tasks have been completed
			executor.get().shutdown();
			
			// remove drop parameters
			executor.remove();
		}
	}
	

	protected void load(File file, File relativeFile, File root) throws InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		
		// add next name to relative path
		relativeFile = new File(relativeFile, file.getName());
		
		if (file.isDirectory()) {
			// load all files in the file tree
			for (File child : file.listFiles()) {
				load(child, relativeFile, root);
			}
		} else {
			publish(createComputationCell(normalizeRelativePath(relativeFile), root, model.getHashType()));
		}
	}
	

	protected ChecksumCell createComputationCell(String name, File root, HashType hash) {
		ChecksumCell cell = new ChecksumCell(name, root, new ChecksumComputationTask(new File(root, name), hash));
		
		// start computation task
		executor.get().execute(cell.getTask());
		
		return cell;
	}
	

	protected String normalizeRelativePath(File file) {
		if (file.isAbsolute())
			throw new IllegalArgumentException("Path must be relative");
		
		return file.getPath().replace('\\', '/');
	}
	

	@Override
	public String getFileFilterDescription() {
		return "files, folders and sfv files";
	}
	
}
