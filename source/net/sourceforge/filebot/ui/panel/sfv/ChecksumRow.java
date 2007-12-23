
package net.sourceforge.filebot.ui.panel.sfv;


import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChecksumRow {
	
	private String name;
	
	private HashMap<File, Checksum> checksumMap = new HashMap<File, Checksum>();
	
	private Long checksumFromFileName = null;
	
	
	public static enum State {
		OK, UNKNOWN, WARNING, ERROR;
	}
	
	
	public ChecksumRow(String name) {
		this.name = name;
		
		// look for a patter like [49A93C5F]
		Pattern pattern = Pattern.compile(".*\\[(\\p{XDigit}{8})\\].*");
		Matcher matcher = pattern.matcher(getName());
		
		if (matcher.matches()) {
			String checksumString = matcher.group(matcher.groupCount());
			checksumFromFileName = Long.parseLong(checksumString, 16);
		}
	}
	

	public String getName() {
		return name;
	}
	

	public State getState() {
		HashSet<Long> checksums = new HashSet<Long>();
		boolean ready = true;
		
		for (Checksum checksum : checksumMap.values()) {
			if (checksum.getState() == Checksum.State.READY)
				checksums.add(checksum.getChecksum());
			else
				ready = false;
		}
		
		if (checksums.size() > 1) {
			// checksums do not match
			return State.ERROR;
		}
		
		if (!checksums.isEmpty() && checksumFromFileName != null) {
			// check if the checksum in the filename matches
			if (!checksums.contains(checksumFromFileName))
				return State.WARNING;
		}
		
		if (!ready) {
			return State.UNKNOWN;
		}
		
		return State.OK;
	}
	

	public Checksum getChecksum(File columnRoot) {
		return checksumMap.get(columnRoot);
	}
	

	public void putChecksum(File columnRoot, Checksum checksum) {
		checksumMap.put(columnRoot, checksum);
	}
	

	public void removeChecksum(File columnRoot) {
		checksumMap.remove(columnRoot);
	}
	
}
