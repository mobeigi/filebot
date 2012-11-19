// https://github.com/sonatype/nexus/blob/2f0e154ec565969b4fd8698883ab76a461210f4f/nexus/nexus-test-harness/nexus-it-helper-plugin/src/main/java/org/sonatype/nexus/rt/prefs/FilePreferencesFactory.java

package net.sourceforge.tuned.prefs;


import java.io.File;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;


/**
 * PreferencesFactory implementation that stores the preferences in a user-defined file. To use it, set the system
 * property <tt>java.util.prefs.PreferencesFactory</tt> to <tt>net.sourceforge.tuned.pref.FilePreferencesFactory</tt>
 * <p/>
 * The file defaults to [user.home]/.fileprefs, but may be overridden with the system property
 * <tt>net.sourceforge.tuned.pref.FilePreferencesFactory.file</tt>. Modified by cstamas, switched to SLF4J logging, and
 * exposed preferences file property.
 * 
 * @author David Croft (<a href="http://www.davidc.net">www.davidc.net</a>)
 * @version $Id: FilePreferencesFactory.java 282 2009-06-18 17:05:18Z david $
 */
public class FilePreferencesFactory implements PreferencesFactory {
	
	Preferences rootPreferences;
	
	public static final String SYSTEM_PROPERTY_FILE = "net.sourceforge.tuned.prefs.file";
	
	
	public Preferences systemRoot() {
		return userRoot();
	}
	
	
	public Preferences userRoot() {
		if (rootPreferences == null) {
			rootPreferences = new FilePreferences(null, "");
		}
		
		return rootPreferences;
	}
	
	private static File preferencesFile;
	
	
	public static File getPreferencesFile() {
		if (preferencesFile == null) {
			String prefsFile = System.getProperty(SYSTEM_PROPERTY_FILE);
			
			if (prefsFile == null || prefsFile.length() == 0) {
				prefsFile = System.getProperty("user.home") + File.separator + ".fileprefs";
			}
			
			preferencesFile = new File(prefsFile).getAbsoluteFile();
		}
		
		return preferencesFile;
	}
	
	
	public static void setPreferencesFile(File file) {
		preferencesFile = file;
	}
}
