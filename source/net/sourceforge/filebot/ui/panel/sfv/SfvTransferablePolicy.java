
package net.sourceforge.filebot.ui.panel.sfv;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.filebot.ui.FileFormat;
import net.sourceforge.filebot.ui.transferablepolicies.BackgroundFileTransferablePolicy;
import net.sourceforge.filebot.ui.transferablepolicies.MultiTransferablePolicy;


public class SfvTransferablePolicy extends MultiTransferablePolicy {
	
	private SfvTableModel tableModel;
	
	
	public SfvTransferablePolicy(SfvTableModel tableModel) {
		this.tableModel = tableModel;
		
		addPolicy(new SfvFilePolicy());
		addPolicy(new DefaultFilePolicy());
	}
	
	
	private class SfvFilePolicy extends BackgroundFileTransferablePolicy<SfvTableModel.Entry> {
		
		@Override
		protected boolean accept(File file) {
			// accept sfv files
			return file.isFile() && FileFormat.getSuffix(file).equalsIgnoreCase("sfv");
		}
		

		@Override
		protected void clear() {
			tableModel.clear();
		}
		

		@Override
		protected void process(List<SfvTableModel.Entry> chunks) {
			tableModel.addAll(chunks);
		}
		

		@Override
		protected void load(List<File> files) {
			synchronized (ChecksumComputationExecutor.getInstance()) {
				ChecksumComputationExecutor.getInstance().pause();
				
				for (File file : files) {
					load(file);
				}
				
				ChecksumComputationExecutor.getInstance().resume();
			}
		}
		

		@Override
		protected boolean load(File sfvFile) {
			
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(sfvFile)));
				
				String line = null;
				Pattern pattern = Pattern.compile("(.*)\\s+(\\p{XDigit}{8})");
				
				while ((line = in.readLine()) != null) {
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
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			
			return true;
		}
		

		@Override
		public String getDescription() {
			return "sfv files";
		}
	};
	

	private class DefaultFilePolicy extends BackgroundFileTransferablePolicy<SfvTableModel.Entry> {
		
		@Override
		protected boolean accept(File file) {
			return true;
		}
		

		@Override
		protected void clear() {
			tableModel.clear();
		}
		

		@Override
		protected void process(List<SfvTableModel.Entry> chunks) {
			tableModel.addAll(chunks);
		}
		

		@Override
		protected void load(List<File> files) {
			if (files.isEmpty())
				return;
			
			synchronized (ChecksumComputationExecutor.getInstance()) {
				ChecksumComputationExecutor.getInstance().pause();
				
				File firstFile = files.get(0);
				
				if ((files.size() == 1) && firstFile.isDirectory()) {
					for (File f : firstFile.listFiles()) {
						load(f, firstFile, "");
					}
				} else {
					for (File f : files) {
						load(f, f.getParentFile(), "");
					}
				}
				
				ChecksumComputationExecutor.getInstance().resume();
			}
		}
		

		protected void load(File file, File columnRoot, String prefix) {
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
		

		/**
		 * this method will not be used
		 */
		@Override
		protected boolean load(File file) {
			return false;
		}
		

		@Override
		public String getDescription() {
			return "files and folders";
		}
		
	};
	
}
