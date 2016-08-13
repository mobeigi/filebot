package net.filebot.media;

import static java.util.Collections.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

import net.filebot.MetaAttributeView;

public class MetaAttributes {

	public static final String FILENAME_KEY = "net.filebot.filename";
	public static final String METADATA_KEY = "net.filebot.metadata";

	private final BasicFileAttributeView fileAttributeView;
	private final MetaAttributeView metaAttributeView;

	private final Map<String, String> jsonTypeMap;

	public MetaAttributes(File file, Map<String, String> jsonTypeMap) throws IOException {
		this.metaAttributeView = new MetaAttributeView(file);
		this.fileAttributeView = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class);
		this.jsonTypeMap = jsonTypeMap;
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
			metaAttributeView.put(METADATA_KEY, JsonWriter.objectToJson(object, singletonMap(JsonWriter.TYPE_NAME_MAP, jsonTypeMap)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Object getObject() {
		try {
			String jsonObject = metaAttributeView.get(METADATA_KEY);
			if (jsonObject != null && jsonObject.length() > 0) {
				Map<String, Object> options = new HashMap<String, Object>(2);
				options.put(JsonReader.TYPE_NAME_MAP, jsonTypeMap);

				// options must be a modifiable map
				return JsonReader.jsonToJava(jsonObject, options);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	public void clear() {
		metaAttributeView.put(FILENAME_KEY, null);
		metaAttributeView.put(METADATA_KEY, null);
	}

}
