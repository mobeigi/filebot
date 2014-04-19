package net.filebot.cli;

public class ScriptDeath extends Throwable {

	public ScriptDeath(String message) {
		super(message);
	}

	public ScriptDeath(String message, Throwable cause) {
		super(message, cause);
	}

}
