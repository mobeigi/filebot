
package net.sourceforge.filebot.ui.sfv;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import net.sourceforge.filebot.hash.HashType;
import net.sourceforge.filebot.hash.VerificationFileWriter;
import net.sourceforge.filebot.ui.transfer.TextFileExportHandler;
import net.sourceforge.tuned.FileUtilities;


class ChecksumTableExportHandler extends TextFileExportHandler {
	
	private final ChecksumTableModel model;
	

	public ChecksumTableExportHandler(ChecksumTableModel model) {
		this.model = model;
	}
	

	@Override
	public boolean canExport() {
		return model.getRowCount() > 0 && defaultColumn() != null;
	}
	

	@Override
	public void export(PrintWriter out) {
		export(new VerificationFileWriter(out, model.getHashType().getFormat(), "UTF-8"), defaultColumn(), model.getHashType());
	}
	

	@Override
	public String getDefaultFileName() {
		return getDefaultFileName(defaultColumn());
	}
	

	protected File defaultColumn() {
		// select first column that is not a verification file column
		for (File root : model.getChecksumColumns()) {
			if (root.isDirectory())
				return root;
		}
		
		return null;
	}
	

	public void export(File file, File column) throws IOException {
		VerificationFileWriter writer = new VerificationFileWriter(file, model.getHashType().getFormat(), "UTF-8");
		
		try {
			export(writer, column, model.getHashType());
		} finally {
			writer.close();
		}
	}
	

	public void export(VerificationFileWriter out, File column, HashType type) {
		for (ChecksumRow row : model.rows()) {
			ChecksumCell cell = row.getChecksum(column);
			
			if (cell != null) {
				String hash = cell.getChecksum(type);
				
				if (hash != null) {
					out.write(cell.getName(), hash);
				}
			}
		}
	}
	

	public String getDefaultFileName(File column) {
		StringBuilder sb = new StringBuilder();
		
		// append file name
		sb.append(column != null ? FileUtilities.getName(column) : "name");
		
		// append file extension
		sb.append('.').append(model.getHashType().name().toLowerCase());
		
		return sb.toString();
	}
}
