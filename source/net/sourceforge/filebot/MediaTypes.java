
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
	

	private static final MediaTypes data = unmarshal();
	

	private static MediaTypes unmarshal() {
		try {
			Unmarshaller unmarshaller = JAXBContext.newInstance(MediaTypes.class).createUnmarshaller();
			
			return (MediaTypes) unmarshaller.unmarshal(MediaTypes.class.getResource("media.types"));
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
	

	public static ExtensionFileFilter getFilter(String name) {
		return new ExtensionFileFilter(getExtensionList(name));
	}
	

	public static List<String> getExtensionList(String name) {
		List<String> list = new ArrayList<String>();
		
		for (Type type : data.types) {
			if (type.name.startsWith(name)) {
				addAll(list, type.extensions);
			}
		}
		
		return list;
	}
	
}
