package net.filebot.mac.sandbox;

import ca.weblite.objc.Client;

public class SandBoxUtil {

	private static final Client objc = new Client();

	public static Object NSData_initWithBase64Encoding(String text) {
		return objc.sendProxy("NSData", "data").send("initWithBase64Encoding:", text);
	}

	public static Object NSURL_bookmarkDataWithOptions(String path) {
		return objc.sendProxy("NSURL", "fileURLWithPath:", path).sendProxy("bookmarkDataWithOptions:includingResourceValuesForKeys:relativeToURL:error:", 2048, null, null, null).sendString("base64Encoding");
	}

	public static Object NSURL_URLByResolvingBookmarkData_startAccessingSecurityScopedResource(String text) {
		return objc.sendProxy("NSURL", "URLByResolvingBookmarkData:options:relativeToURL:bookmarkDataIsStale:error:", NSData_initWithBase64Encoding(text), 1024, null, false, null).send("startAccessingSecurityScopedResource");
	}

}
