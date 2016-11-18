package net.filebot.gio;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URI;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class PlatformGVFSTest {

	static File gvfsRoot = new File("gvfs");
	static String[] shares = { "smb-share:server=10.0.1.5,share=data", "afp-volume:host=10.0.1.5,user=reinhard,volume=data", "sftp:host=myserver.org,user=nico" };

	GVFS gvfs = new PlatformGVFS(gvfsRoot);

	@BeforeClass
	public static void before() throws Exception {
		stream(shares).forEach(f -> new File(gvfsRoot, f).mkdirs());
	}

	@AfterClass
	public static void after() throws Exception {
	}

	@Test
	public void smb() throws Exception {
		assertEquals("gvfs/smb-share:server=10.0.1.5,share=data/Movies/Avatar.mp4", gvfs.getPathForURI(new URI("smb://10.0.1.5/data/Movies/Avatar.mp4")).getPath());
	}

	@Test
	public void afp() throws Exception {
		assertEquals("gvfs/afp-volume:host=10.0.1.5,user=reinhard,volume=data/Movies/Avatar.mp4", gvfs.getPathForURI(new URI("afp://reinhard@10.0.1.5/data/Movies/Avatar.mp4")).getPath());
	}

	@Test
	public void sftp() throws Exception {
		assertEquals("gvfs/sftp:host=myserver.org,user=nico/home/Movies/Avatar.mp4", gvfs.getPathForURI(new URI("sftp://nico@myserver.org/home/Movies/Avatar.mp4")).getPath());
	}

}
