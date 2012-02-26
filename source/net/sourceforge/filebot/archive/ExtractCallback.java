
package net.sourceforge.filebot.archive;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;


class ExtractCallback implements IArchiveExtractCallback {
	
	private ISevenZipInArchive inArchive;
	private ExtractOutProvider extractOut;
	
	private ExtractOutStream output = null;
	
	
	public ExtractCallback(ISevenZipInArchive inArchive, ExtractOutProvider extractOut) {
		this.inArchive = inArchive;
		this.extractOut = extractOut;
	}
	
	
	public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) throws SevenZipException {
		if (extractAskMode != ExtractAskMode.EXTRACT) {
			return null;
		}
		
		boolean isFolder = (Boolean) inArchive.getProperty(index, PropID.IS_FOLDER);
		if (isFolder) {
			return null;
		}
		
		String path = (String) inArchive.getProperty(index, PropID.PATH);
		try {
			OutputStream target = extractOut.getStream(new File(path));
			if (target == null) {
				return null;
			}
			
			output = new ExtractOutStream(target);
			return output;
		} catch (IOException e) {
			throw new SevenZipException(e);
		}
	}
	
	
	public void prepareOperation(ExtractAskMode extractAskMode) throws SevenZipException {
	}
	
	
	public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {
		if (output != null) {
			try {
				output.close();
			} catch (IOException e) {
				throw new SevenZipException(e);
			} finally {
				output = null;
			}
		}
		
		if (extractOperationResult != ExtractOperationResult.OK) {
			throw new SevenZipException("Extraction Error: " + extractOperationResult);
		}
	}
	
	
	public void setCompleted(long completeValue) throws SevenZipException {
	}
	
	
	public void setTotal(long total) throws SevenZipException {
	}
	
}
