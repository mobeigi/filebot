
package net.sourceforge.filebot;


import static java.util.Collections.*;

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
	

	private static MediaTypes instance;
	

	public static synchronized MediaTypes getDefault() {
		if (instance == null) {
			try {
				Unmarshaller unmarshaller = JAXBContext.newInstance(MediaTypes.class).createUnmarshaller();
				
				// initialize singleton instance
				instance = (MediaTypes) unmarshaller.unmarshal(MediaTypes.class.getResource("media.types"));
			} catch (JAXBException e) {
				throw new RuntimeException(e);
			}
		}
		
		return instance;
	}
	

	public ExtensionFileFilter filter(String name) {
		return new ExtensionFileFilter(extensions(name));
	}
	

	public String[] extensions(String name) {
		List<String> extensions = new ArrayList<String>();
		
		for (Type type : types) {
			if (type.name.startsWith(name)) {
				addAll(extensions, type.extensions);
			}
		}
		
		return extensions.toArray(new String[0]);
	}
	
}
