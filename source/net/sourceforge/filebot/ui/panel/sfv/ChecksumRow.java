
package net.sourceforge.filebot.ui.panel.sfv;


import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import net.sourceforge.filebot.FileBotUtil;


public class ChecksumRow {
	
	private String name;
	
	private HashMap<File, Checksum> checksumMap = new HashMap<File, Checksum>();
	
	/**
	 * Checksum that is embedded in the file name (e.g. Test[49A93C5F].txt)
	 */
	private final Long embeddedChecksum;
	
	
	public static enum State {
		OK,
		WARNING,
		ERROR,
		UNKNOWN;
	}
	
	
	public ChecksumRow(String name) {
		this.name = name;
		this.embeddedChecksum = getEmbeddedChecksum(name);
	}
	

	/**
	 * Try to parse a CRC32 checksum from the given file name. The checksum is assumed to be in
	 * brackets.
	 * 
	 * <pre>
	 * e.g.
	 * Test[49A93C5F].txt
	 * </pre>
	 * 
	 * @param file name that contains a checksum
	 * @return the checksum or null, if parameter did not contain a checksum
	 */
	private static Long getEmbeddedChecksum(String name) {
		// look for a checksum pattern like [49A93C5F]
		String match = FileBotUtil.getEmbeddedChecksum(name);
		
		if (match != null)
			return Long.parseLong(match, 16);
		
		return null;
	}
	

	public String getName() {
		return name;
	}
	

	public State getState() {
		HashSet<Long> checksums = new HashSet<Long>();
		
		for (Checksum checksum : getChecksums()) {
			if (checksum.getState() == Checksum.State.READY) {
				checksums.add(checksum.getChecksum());
			} else if (checksum.getState() == Checksum.State.ERROR) {
				return State.ERROR;
			} else {
				return State.UNKNOWN;
			}
		}
		
		if (checksums.size() > 1) {
			// checksums do not match
			return State.ERROR;
		}
		
		if (!checksums.isEmpty() && embeddedChecksum != null) {
			// check if the embedded checksum matches
			if (!checksums.contains(embeddedChecksum))
				return State.WARNING;
		}
		
		return State.OK;
	}
	

	public Checksum getChecksum(File column) {
		return checksumMap.get(column);
	}
	

	public Collection<Checksum> getChecksums() {
		return checksumMap.values();
	}
	

	public void putChecksum(File column, Checksum checksum) {
		checksumMap.put(column, checksum);
	}
	

	public void removeChecksum(File column) {
		checksumMap.remove(column);
	}
	
}
