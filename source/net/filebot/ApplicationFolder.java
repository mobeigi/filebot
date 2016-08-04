package net.filebot;

import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.util.logging.Level;

public enum ApplicationFolder {

	// real user home (the user.home will point to the application-specific container in sandbox environments)
	UserHome(isMacSandbox() ? "/Users/" + System.getProperty("user.name") : System.getProperty("user.home")),

	AppData(System.getProperty("application.dir", UserHome.path(".filebot").getPath())),

	Temp(System.getProperty("java.io.tmpdir")),

	Cache(System.getProperty("application.cache", AppData.path("cache").getPath()));

	private final File path;

	ApplicationFolder(String path) {
		this.path = new File(path);
	}

	public File get() {
		return path;
	}

	public File path(String name) {
		return new File(path, name);
	}

	public File resolve(String name) {
		return new File(getCanonicalFile(), name);
	}

	public File getCanonicalFile() {
		try {
			return createFolders(path.getCanonicalFile());
		} catch (Exception e) {
			debug.log(Level.SEVERE, String.format("Failed to create application folder: %s => %s", this, path), e);
			return path;
		}
	}

}
