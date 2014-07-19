package net.filebot.media;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

import net.filebot.MetaAttributeView;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

public class MetaAttributes {

	private static final String FILENAME_KEY = "net.filebot.filename";
	private static final String METADATA_KEY = "net.filebot.metadata";

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
		try {
			metaAttributeView.put(METADATA_KEY, JsonWriter.objectToJson(object));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Object getObject() {
		try {
			String jsonObject = metaAttributeView.get(METADATA_KEY);
			if (jsonObject != null && jsonObject.length() > 0) {
				return JsonReader.jsonToJava(jsonObject);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	public void clear() {
		metaAttributeView.put(FILENAME_KEY, null);
		metaAttributeView.put(METADATA_KEY, null);
	}

}
