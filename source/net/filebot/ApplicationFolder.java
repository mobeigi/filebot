package net.filebot;

import static net.filebot.Settings.*;

import java.io.File;

public enum ApplicationFolder {

	// real user home (the user.home will point to the application-specific container in sandbox environments)
	UserHome(isMacSandbox() ? System.getProperty("UserHome") : System.getProperty("user.home")),

	AppData(System.getProperty("application.dir", UserHome.resolve(".filebot").getPath())),

	Temp(System.getProperty("java.io.tmpdir")),

	Cache(System.getProperty("application.cache", AppData.resolve("cache").getPath()));

	private final File path;

	ApplicationFolder(String path) {
		this.path = new File(path);
	}

	public File getFile() {
		return path;
	}

	public File resolve(String name) {
		return new File(path, name);
	}

}
