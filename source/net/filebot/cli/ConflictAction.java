package net.sourceforge.filebot.cli;

public enum ConflictAction {

	SKIP, OVERRIDE, FAIL, AUTO;

	public static ConflictAction forName(String action) {
		for (ConflictAction it : values()) {
			if (it.name().equalsIgnoreCase(action))
				return it;
		}

		throw new IllegalArgumentException("Illegal conflict action: " + action);
	}

}
