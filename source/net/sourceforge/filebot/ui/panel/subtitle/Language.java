
package net.sourceforge.filebot.ui.panel.subtitle;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Language {
	
	public static List<Language> languages = null;
	
	
	public static synchronized List<Language> getLanguages() {
		if (languages == null) {
			languages = parseLanguages();
		}
		
		return Collections.unmodifiableList(languages);
	}
	

	public static Language forName(String name) {
		for (Language language : getLanguages()) {
			for (String languageName : language.getNames()) {
				if (name.equalsIgnoreCase(languageName))
					return language;
			}
		}
		
		return null;
	}
	

	public static Language forCountryCode(String countryCode) {
		for (Language language : getLanguages()) {
			if (countryCode.equalsIgnoreCase(language.getCountryCode()))
				return language;
		}
		
		return null;
	}
	

	private static List<Language> parseLanguages() {
		List<Language> languages = new ArrayList<Language>();
		
		try {
			Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(Language.class.getResourceAsStream("languages.xml"));
			
			NodeList languageNodes = dom.getDocumentElement().getChildNodes();
			
			for (int i = 0; i < languageNodes.getLength(); i++) {
				Node languageNode = languageNodes.item(i);
				
				if (!languageNode.getNodeName().equals("language"))
					continue;
				
				String countryCode = null;
				ArrayList<String> names = new ArrayList<String>();
				
				NodeList nodes = languageNode.getChildNodes();
				
				for (int j = 0; j < nodes.getLength(); j++) {
					Node node = nodes.item(j);
					
					if ((countryCode == null) && node.getNodeName().equals("code"))
						countryCode = node.getTextContent();
					else if (node.getNodeName().equals("name"))
						names.add(node.getTextContent());
				}
				
				languages.add(new Language(countryCode, names));
			}
		} catch (Exception e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
		}
		
		return languages;
	}
	
	private List<String> names;
	private String countryCode;
	
	
	public Language(String countryCode, Collection<String> names) {
		if (names.isEmpty())
			throw new IllegalArgumentException("List must not be empty");
		
		this.countryCode = countryCode;
		
		this.names = new ArrayList<String>(names);
	}
	

	public String getCountryCode() {
		return countryCode;
	}
	

	public Iterable<String> getNames() {
		return names;
	}
	

	@Override
	public String toString() {
		return String.format("[%s] %s", countryCode, names.get(0));
	}
	
}
