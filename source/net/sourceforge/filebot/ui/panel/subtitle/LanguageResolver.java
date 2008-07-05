
package net.sourceforge.filebot.ui.panel.subtitle;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class LanguageResolver {
	
	private static final LanguageResolver defaultInstance = new LanguageResolver();
	
	
	public static LanguageResolver getDefault() {
		return defaultInstance;
	}
	
	private final Map<String, Locale> cache = new HashMap<String, Locale>();
	
	
	/**
	 * Get the {@link Locale} for a given language name (e.g. "german").
	 * 
	 * @param languageName english name of the language
	 * @return the locale for this language or null if no locale for this language exists
	 */
	public synchronized Locale getLocale(String languageName) {
		languageName = languageName.toLowerCase();
		
		Locale locale = cache.get(languageName);
		
		if (locale == null) {
			locale = findLocale(languageName);
			cache.put(languageName, locale);
		}
		
		return locale;
	}
	

	/**
	 * Get the ISO 639 language code for a language.
	 * 
	 * @param languageName english name of the language
	 * @return lowercase ISO 639 language code
	 * @see Locale#getLanguage()
	 */
	public String getLanguageCode(String languageName) {
		Locale locale = getLocale(languageName);
		
		if (locale != null)
			return locale.getLanguage();
		
		return null;
	}
	

	/**
	 * Find the {@link Locale} for a given language name.
	 * 
	 * @param languageName lower-case language name
	 * @return {@link Locale} for the given language, or null if no matching {@link Locale} is
	 *         available
	 */
	private Locale findLocale(String languageName) {
		for (Locale locale : Locale.getAvailableLocales()) {
			if (locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase().equals(languageName))
				return locale;
		}
		
		return null;
	}
}
