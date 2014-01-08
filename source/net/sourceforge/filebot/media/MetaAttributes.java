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

	public MetaAttributes(File file) throws IOException {
		this.metaAttributeView = new MetaAttributeView(file);
		this.fileAttributeView = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class);
	}

	public void setCreationDate(long millis) throws IOException {
		fileAttributeView.setTimes(null, null, FileTime.fromMillis(millis));
	}

	public long getCreationDate(long time) {
		try {
			return fileAttributeView.readAttributes().creationTime().toMillis();
		} catch (Exception e) {
			return 0;
		}
	}

	public void setOriginalName(String name) {
		metaAttributeView.put(FILENAME_KEY, name);
	}

	public String getOriginalName() {
		return metaAttributeView.get(FILENAME_KEY);
	}

	public void setObject(Object object) {
		metaAttributeView.put(METADATA_KEY, JsonWriter.toJson(object));
	}

	public Object getObject() {
		return JsonReader.toJava(metaAttributeView.get(METADATA_KEY));
	}

	public void clear() {
		metaAttributeView.remove(FILENAME_KEY);
		metaAttributeView.remove(METADATA_KEY);
	}

}
