
package net.sourceforge.filebot.ui.sfv;


import static java.util.Collections.*;
import static net.sourceforge.filebot.hash.VerificationUtilities.*;
import static net.sourceforge.filebot.ui.NotificationLogging.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.hash.HashType;
import net.sourceforge.filebot.hash.VerificationFileReader;
import net.sourceforge.filebot.ui.transfer.BackgroundFileTransferablePolicy;
import net.sourceforge.tuned.ExceptionUtilities;


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
		if (files.size() == 1 && getHashType(files.get(0)) != null) {
			model.setHashType(getHashType(files.get(0)));
		}
	}
	

	@Override
	protected void process(List<ChecksumCell> chunks) {
		model.addAll(chunks);
	}
	

	@Override
	protected void process(Exception e) {
		UILogger.log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
	}
	

	private final ThreadLocal<ExecutorService> executor = new ThreadLocal<ExecutorService>();
	private final ThreadLocal<VerificationTracker> verificationTracker = new ThreadLocal<VerificationTracker>();
	

	@Override
	protected void load(List<File> files) throws IOException {
		// initialize drop parameters
		executor.set(computationService.newExecutor());
		verificationTracker.set(new VerificationTracker(3));
		
		try {
			// handle single verification file drop
			if (files.size() == 1 && getHashType(files.get(0)) != null) {
				loadVerificationFile(files.get(0), getHashType(files.get(0)));
			}
			// handle single folder drop
			else if (files.size() == 1 && files.get(0).isDirectory()) {
				for (File file : files.get(0).listFiles()) {
					load(file, null, files.get(0));
				}
			}
			// handle all other drops
			else {
				for (File file : files) {
					load(file, null, file.getParentFile());
				}
			}
		} catch (InterruptedException e) {
			// supposed to happen if background execution is aborted
		} finally {
			// shutdown executor after all tasks have been completed
			executor.get().shutdown();
			
			// remove drop parameters
			executor.remove();
			verificationTracker.remove();
		}
	}
	

	protected void loadVerificationFile(File file, HashType type) throws IOException, InterruptedException {
		VerificationFileReader parser = new VerificationFileReader(createTextReader(file), type.getFormat());
		
		try {
			// root for relative file paths in verification file
			File baseFolder = file.getParentFile();
			
			while (parser.hasNext()) {
				// make this possibly long-running operation interruptible
				if (Thread.interrupted())
					throw new InterruptedException();
				
				Entry<File, String> entry = parser.next();
				
				String name = normalizePathSeparators(entry.getKey().getPath());
				String hash = new String(entry.getValue());
				
				ChecksumCell correct = new ChecksumCell(name, file, singletonMap(type, hash));
				ChecksumCell current = createComputationCell(name, baseFolder, type);
				
				publish(correct, current);
			}
		} finally {
			parser.close();
		}
	}
	

	protected void load(File absoluteFile, File relativeFile, File root) throws IOException, InterruptedException {
		if (Thread.interrupted())
			throw new InterruptedException();
		
		// ignore hidden files/folders
		if (absoluteFile.isHidden())
			return;
		
		// add next name to relative path
		relativeFile = new File(relativeFile, absoluteFile.getName());
		
		if (absoluteFile.isDirectory()) {
			// load all files in the file tree
			for (File child : absoluteFile.listFiles()) {
				load(child, relativeFile, root);
			}
		} else {
			String name = normalizePathSeparators(relativeFile.getPath());
			
			// publish computation cell first
			publish(createComputationCell(name, root, model.getHashType()));
			
			// publish verification cell, if we can
			Map<File, String> hashByVerificationFile = verificationTracker.get().getHashByVerificationFile(absoluteFile);
			
			for (Entry<File, String> entry : hashByVerificationFile.entrySet()) {
				HashType hashType = verificationTracker.get().getVerificationFileType(entry.getKey());
				publish(new ChecksumCell(name, entry.getKey(), singletonMap(hashType, entry.getValue())));
			}
		}
	}
	

	protected ChecksumCell createComputationCell(String name, File root, HashType hash) {
		ChecksumCell cell = new ChecksumCell(name, root, new ChecksumComputationTask(new File(root, name), hash));
		
		// start computation task
		executor.get().execute(cell.getTask());
		
		return cell;
	}
	

	@Override
	public String getFileFilterDescription() {
		return "files, folders and sfv files";
	}
	

	private static class VerificationTracker {
		
		private final Map<File, Integer> seen = new HashMap<File, Integer>();
		private final Map<File, Map<File, String>> cache = new HashMap<File, Map<File, String>>();
		private final Map<File, HashType> types = new HashMap<File, HashType>();
		
		private final int maxDepth;
		

		public VerificationTracker(int maxDepth) {
			this.maxDepth = maxDepth;
		}
		

		public Map<File, String> getHashByVerificationFile(File file) throws IOException {
			// cache all verification files
			File folder = file.getParentFile();
			int depth = 0;
			
			while (folder != null && depth <= maxDepth) {
				Integer seenLevel = seen.get(folder);
				
				if (seenLevel != null && seenLevel <= depth) {
					// we have completely seen this parent tree before 
					break;
				}
				
				if (seenLevel == null) {
					// folder we have never encountered before
					for (File verificationFile : folder.listFiles(MediaTypes.getDefaultFilter("verification"))) {
						HashType hashType = getHashType(verificationFile);
						cache.put(verificationFile, importVerificationFile(verificationFile, hashType, verificationFile.getParentFile()));
						types.put(verificationFile, hashType);
					}
				}
				
				// update
				seen.put(folder, depth);
				
				// step down
				folder = folder.getParentFile();
				depth++;
			}
			
			// just return if we know we won't find anything
			if (cache.isEmpty()) {
				return emptyMap();
			}
			
			// search all cached verification files
			Map<File, String> result = new HashMap<File, String>(2);
			
			for (Entry<File, Map<File, String>> entry : cache.entrySet()) {
				String hash = entry.getValue().get(file);
				
				if (hash != null) {
					result.put(entry.getKey(), hash);
				}
			}
			
			return result;
		}
		

		public HashType getVerificationFileType(File verificationFile) {
			return types.get(verificationFile);
		}
		

		/**
		 * Completely read a verification file and resolve all relative file paths against a given base folder
		 */
		private Map<File, String> importVerificationFile(File verificationFile, HashType hashType, File baseFolder) throws IOException {
			VerificationFileReader parser = new VerificationFileReader(createTextReader(verificationFile), hashType.getFormat());
			Map<File, String> result = new HashMap<File, String>();
			
			try {
				while (parser.hasNext()) {
					Entry<File, String> entry = parser.next();
					
					// resolve relative path, the hash is probably a substring, so we compact it, for memory reasons
					result.put(new File(baseFolder, entry.getKey().getPath()), new String(entry.getValue()));
				}
			} finally {
				parser.close();
			}
			
			return result;
		}
	}
	
}
