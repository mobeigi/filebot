
package net.sourceforge.filebot.ui.panel.subtitle;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


class Language {
	
	@XmlAttribute(name = "name")
	private String name;
	
	@XmlAttribute(name = "code")
	private String code;
	
	
	protected Language() {
		// used by JAXB
	}
	

	public Language(String name, String code) {
		this.name = name;
		this.code = code;
	}
	

	public String getName() {
		return name;
	}
	

	public String getCode() {
		return code;
	}
	

	@Override
	public Language clone() {
		return new Language(name, code);
	}
	

	@Override
	public String toString() {
		return name;
	}
	

	public static Language getLanguage(String languageCode) {
		for (Language language : Languages.getInstance().elements()) {
			if (language.getCode().equalsIgnoreCase(languageCode))
				return language;
		}
		
		return null;
	}
	

	public static List<Language> availableLanguages() {
		return Collections.unmodifiableList(Arrays.asList(Languages.getInstance().elements()));
	}
	
	
	@XmlRootElement(name = "languages")
	private static class Languages {
		
		@XmlElement(name = "language")
		private Language[] elements;
		
		// keep singleton instance of all available languages
		private static Languages instance;
		
		
		public static Languages getInstance() {
			if (instance == null) {
				try {
					Unmarshaller unmarshaller = JAXBContext.newInstance(Languages.class).createUnmarshaller();
					
					// load languages from xml files
					return (Languages) unmarshaller.unmarshal(Language.class.getResource("languages.xml"));
				} catch (JAXBException e) {
					throw new RuntimeException(e);
				}
			}
			
			return instance;
		}
		

		public Language[] elements() {
			return elements;
		}
	}
	
}
