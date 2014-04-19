
package net.sourceforge.filebot.ui.transfer;


import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Set;


public class TextFileTransferable extends ByteBufferTransferable {
	
	private final String text;
	

	public TextFileTransferable(String name, String text) {
		this(name, text, Charset.forName("UTF-8"));
	}
	

	public TextFileTransferable(final String name, final String text, final Charset charset) {
		// lazy data map for file transfer
		super(new AbstractMap<String, ByteBuffer>() {
			
			@Override
			public Set<Entry<String, ByteBuffer>> entrySet() {
				// encode text
				Entry<String, ByteBuffer> entry = new SimpleEntry<String, ByteBuffer>(name, charset.encode(text));
				
				// return memory file entry
				return Collections.singleton(entry);
			}
		});
		
		// text transfer
		this.text = text;
	}
	

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		// check file flavor first, because text/uri-list is also text flavor
		if (super.isDataFlavorSupported(flavor)) {
			return super.getTransferData(flavor);
		}
		
		// check text flavor
		if (flavor.isFlavorTextType()) {
			return text;
		}
		
		throw new UnsupportedFlavorException(flavor);
	}
	

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] {
				DataFlavor.javaFileListFlavor, FileTransferable.uriListFlavor, DataFlavor.stringFlavor
		};
	}
	

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		// file flavor or text flavor
		return super.isDataFlavorSupported(flavor) || flavor.isFlavorTextType();
	}
	
}
