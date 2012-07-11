
package net.sourceforge.filebot;


import java.lang.reflect.Method;

import org.codehaus.groovy.tools.LoaderConfiguration;
import org.codehaus.groovy.tools.RootLoader;


public class Main {
	
	/**
	 * Invoke ApplicationStarter with Groovy RootLoader
	 */
	public static void main(String[] args) throws Exception {
		LoaderConfiguration lc = new LoaderConfiguration();
		lc.setMainClass("net.sourceforge.filebot.ApplicationStarter");
		
		RootLoader rootLoader = new RootLoader(lc);
		rootLoader.addURL(Main.class.getProtectionDomain().getCodeSource().getLocation());
		
		Class<?> c = rootLoader.loadClass(lc.getMainClass());
		Method m = c.getMethod("main", new Class[] { String[].class });
		m.invoke(null, new Object[] { args });
	}
	
}
