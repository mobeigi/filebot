
package net.sourceforge.filebot.media;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

import net.sourceforge.filebot.MetaAttributeView;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;


public class MetaAttributes {
	
	private static final String FILENAME_KEY = "filename";
	private static final String METADATA_KEY = "metadata";
	
	private final BasicFileAttributeView fileAttributeView;
	private final MetaAttributeView metaAttributeView;
	
	
	public MetaAttributes(File file) {
		this.metaAttributeView = new MetaAttributeView(file);
		this.fileAttributeView = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class);
	}
	
	
	public void setCreationDate(long millis) {
		try {
			fileAttributeView.setTimes(null, null, FileTime.fromMillis(millis));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	
	public long getCreationDate(long time) {
		try {
			return fileAttributeView.readAttributes().creationTime().toMillis();
		} catch (IOException e) {
			return 0;
		}
	}
	
	
	public void putFileName(String name) {
		metaAttributeView.put(FILENAME_KEY, name);
	}
	
	
	public void getFileName(String name) {
		metaAttributeView.get(FILENAME_KEY);
	}
	
	
	public void putMetaData(Object object) {
		metaAttributeView.put(METADATA_KEY, JsonWriter.toJson(object));
	}
	
	
	public Object getMetaData() {
		return JsonReader.toJava(metaAttributeView.get(METADATA_KEY));
	}
	
}
