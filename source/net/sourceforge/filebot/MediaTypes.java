
package net.sourceforge.filebot;


import static java.util.Collections.*;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.sourceforge.tuned.FileUtilities.ExtensionFileFilter;


@XmlRootElement(name = "media-types")
public class MediaTypes {
	
	@XmlElement(name = "type")
	private Type[] types;
	

	private static class Type {
		
		@XmlAttribute(name = "name")
		private String name;
		
		@XmlElement(name = "extension")
		private String[] extensions;
	}
	

	private static final MediaTypes defaultInstance = unmarshal();
	

	private static MediaTypes unmarshal() {
		try {
			Unmarshaller unmarshaller = JAXBContext.newInstance(MediaTypes.class).createUnmarshaller();
			return (MediaTypes) unmarshaller.unmarshal(MediaTypes.class.getResource("media.types"));
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
	

	public List<String> getExtensionList(String name) {
		List<String> list = new ArrayList<String>();
		
		for (Type type : defaultInstance.types) {
			if (type.name.startsWith(name)) {
				addAll(list, type.extensions);
			}
		}
		
		return list;
	}
	

	public FileFilter getFilter(String name) {
		return new ExtensionFileFilter(getExtensionList(name));
	}
	

	public static MediaTypes getDefault() {
		return defaultInstance;
	}
	

	public static ExtensionFileFilter getDefaultFilter(String name) {
		return new ExtensionFileFilter(getDefault().getExtensionList(name));
	}
	

	// some convenience filters
	public static final ExtensionFileFilter AUDIO_FILES = MediaTypes.getDefaultFilter("audio");
	public static final ExtensionFileFilter VIDEO_FILES = MediaTypes.getDefaultFilter("video");
	public static final ExtensionFileFilter SUBTITLE_FILES = MediaTypes.getDefaultFilter("subtitle");
}
