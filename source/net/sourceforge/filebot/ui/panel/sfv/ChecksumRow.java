
package net.sourceforge.filebot.ui.panel.sfv;


import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sourceforge.filebot.FileBotUtilities;


public class ChecksumRow {
	
	private String name;
	
	private Map<File, ChecksumCell> hashes = new HashMap<File, ChecksumCell>(4);
	private State state = State.UNKNOWN;
	
	/**
	 * Checksum that is embedded in the file name (e.g. Test[49A93C5F].txt)
	 */
	private String embeddedChecksum;
	
	
	public static enum State {
		UNKNOWN,
		OK,
		WARNING,
		ERROR
	}
	
	
	public ChecksumRow(String name) {
		this.name = name;
		this.embeddedChecksum = FileBotUtilities.getEmbeddedChecksum(name);
	}
	

	public String getName() {
		return name;
	}
	

	public State getState() {
		return state;
	}
	

	public ChecksumCell getChecksum(File root) {
		return hashes.get(root);
	}
	

	public Collection<ChecksumCell> values() {
		return Collections.unmodifiableCollection(hashes.values());
	}
	

	public void add(ChecksumCell entry) {
		hashes.put(entry.getRoot(), entry);
		updateState();
	}
	

	public void updateState() {
		// update state
		state = getState(hashes.values());
	}
	

	protected State getState(Collection<ChecksumCell> entries) {
		// check states before we bother comparing the hash values
		for (ChecksumCell entry : entries) {
			if (entry.getState() == ChecksumCell.State.ERROR) {
				// one error cell -> error state
				return State.ERROR;
			} else if (entry.getState() != ChecksumCell.State.READY) {
				// one cell that is not ready yet -> unknown state
				return State.UNKNOWN;
			}
		}
		
		// compare hash values
		Set<String> checksumSet = new HashSet<String>(2);
		Set<State> verdictSet = EnumSet.noneOf(State.class);
		
		for (HashType type : HashType.values()) {
			checksumSet.clear();
			
			for (ChecksumCell entry : entries) {
				String checksum = entry.getChecksum(type);
				
				if (checksum != null) {
					checksumSet.add(checksum);
				}
			}
			
			verdictSet.add(getVerdict(checksumSet));
		}
		
		// ERROR > WARNING > OK > UNKOWN 
		return Collections.max(verdictSet);
	}
	

	protected State getVerdict(Set<String> checksumSet) {
		if (checksumSet.size() < 1) {
			// no hash values
			return State.UNKNOWN;
		} else if (checksumSet.size() > 1) {
			// hashes don't match, something is wrong
			return State.ERROR;
		} else {
			// all hashes match
			if (embeddedChecksum != null) {
				String checksum = checksumSet.iterator().next();
				
				if (checksum.length() == embeddedChecksum.length() && !checksum.equalsIgnoreCase(embeddedChecksum)) {
					return State.WARNING;
				}
			}
			
			return State.OK;
		}
	}
	

	@Override
	public String toString() {
		return String.format("%s %s", name, hashes);
	}
}
