// https://github.com/sonatype/nexus/blob/2f0e154ec565969b4fd8698883ab76a461210f4f/nexus/nexus-test-harness/nexus-it-helper-plugin/src/main/java/org/sonatype/nexus/rt/prefs/FilePreferences.java

package net.sourceforge.tuned.prefs;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Preferences implementation that stores to a user-defined file. See FilePreferencesFactory. Modified by cstamas,
 * switched to SLF4J logging, and exposed preferences file property.
 *
 * Modified to use '/' as path separator and not '.' because it breaks when keys containing '.' are used.
 * 
 * @author David Croft (<a href="http://www.davidc.net">www.davidc.net</a>)
 * @version $Id: FilePreferences.java 283 2009-06-18 17:06:58Z david $
 */
public class FilePreferences extends AbstractPreferences {
	
	private static final Logger log = LoggerFactory.getLogger(FilePreferences.class.getName());
	
	private Map<String, String> root;
	
	private Map<String, FilePreferences> children;
	
	private boolean isRemoved = false;
	
	
	public FilePreferences(AbstractPreferences parent, String name) {
		super(parent, name);
		
		log.debug("Instantiating node {}", name);
		
		root = new TreeMap<String, String>();
		children = new TreeMap<String, FilePreferences>();
		
		try {
			sync();
		} catch (BackingStoreException e) {
			log.error("Unable to sync on creation of node " + name, e);
		}
	}
	
	
	@Override
	protected void putSpi(String key, String value) {
		root.put(key, value);
		try {
			flush();
		} catch (BackingStoreException e) {
			log.error("Unable to flush after putting " + key, e);
		}
	}
	
	
	@Override
	protected String getSpi(String key) {
		return root.get(key);
	}
	
	
	@Override
	protected void removeSpi(String key) {
		root.remove(key);
		try {
			flush();
		} catch (BackingStoreException e) {
			log.error("Unable to flush after removing " + key, e);
		}
	}
	
	
	@Override
	protected void removeNodeSpi() throws BackingStoreException {
		isRemoved = true;
		flush();
	}
	
	
	@Override
	protected String[] keysSpi() throws BackingStoreException {
		return root.keySet().toArray(new String[root.keySet().size()]);
	}
	
	
	@Override
	protected String[] childrenNamesSpi() throws BackingStoreException {
		return children.keySet().toArray(new String[children.keySet().size()]);
	}
	
	
	@Override
	protected FilePreferences childSpi(String name) {
		FilePreferences child = children.get(name);
		if (child == null || child.isRemoved()) {
			child = new FilePreferences(this, name);
			children.put(name, child);
		}
		return child;
	}
	
	
	@Override
	protected void syncSpi() throws BackingStoreException {
		if (isRemoved()) {
			return;
		}
		
		final File file = FilePreferencesFactory.getPreferencesFile();
		
		if (!file.exists()) {
			return;
		}
		
		synchronized (file) {
			Properties p = new Properties();
			try {
				p.load(new FileInputStream(file));
				
				StringBuilder sb = new StringBuilder();
				getPath(sb);
				String path = sb.toString();
				
				final Enumeration<?> pnen = p.propertyNames();
				while (pnen.hasMoreElements()) {
					String propKey = (String) pnen.nextElement();
					if (propKey.startsWith(path)) {
						String subKey = propKey.substring(path.length());
						// Only load immediate descendants
						if (subKey.indexOf('/') == -1) {
							root.put(subKey, p.getProperty(propKey));
						}
					}
				}
			} catch (IOException e) {
				throw new BackingStoreException(e);
			}
		}
	}
	
	
	private void getPath(StringBuilder sb) {
		final FilePreferences parent = (FilePreferences) parent();
		if (parent == null) {
			return;
		}
		
		parent.getPath(sb);
		sb.append(name()).append('/');
	}
	
	
	@Override
	protected void flushSpi() throws BackingStoreException {
		final File file = FilePreferencesFactory.getPreferencesFile();
		
		synchronized (file) {
			Properties p = new Properties();
			try {
				
				StringBuilder sb = new StringBuilder();
				getPath(sb);
				String path = sb.toString();
				
				if (file.exists()) {
					p.load(new FileInputStream(file));
					
					List<String> toRemove = new ArrayList<String>();
					
					// Make a list of all direct children of this node to be removed
					final Enumeration<?> pnen = p.propertyNames();
					while (pnen.hasMoreElements()) {
						String propKey = (String) pnen.nextElement();
						if (propKey.startsWith(path)) {
							String subKey = propKey.substring(path.length());
							// Only do immediate descendants
							if (subKey.indexOf('/') == -1) {
								toRemove.add(propKey);
							}
						}
					}
					
					// Remove them now that the enumeration is done with
					for (String propKey : toRemove) {
						p.remove(propKey);
					}
				}
				
				// If this node hasn't been removed, add back in any values
				if (!isRemoved) {
					for (String s : root.keySet()) {
						p.setProperty(path + s, root.get(s));
					}
				}
				
				p.store(new FileOutputStream(file), "FilePreferences");
			} catch (IOException e) {
				throw new BackingStoreException(e);
			}
		}
	}
}
