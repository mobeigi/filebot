package net.filebot.cli;

public enum ConflictAction {

	SKIP, OVERRIDE, FAIL, AUTO, INDEX;

	public static ConflictAction forName(String action) {
		for (ConflictAction it : values()) {
			if (it.name().equalsIgnoreCase(action))
				return it;
		}

		throw new IllegalArgumentException("Illegal conflict action: " + action);
	}

}
