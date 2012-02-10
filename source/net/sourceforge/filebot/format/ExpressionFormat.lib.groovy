// File operations
import static net.sourceforge.tuned.FileUtilities.*;


/**
* Allow getAt() for File paths
*
* e.g. file[0] -> "F:"
*/
File.metaClass.getAt = { Range range -> listPath(delegate).collect{ replacePathSeparators(getName(it)).trim() }.getAt(range).join(File.separator) }
File.metaClass.getAt = { int index -> listPath(delegate).collect{ replacePathSeparators(getName(it)).trim() }.getAt(index) }


/**
 * Convenience methods for String.toLowerCase()and String.toUpperCase()
 */
String.metaClass.lower = { toLowerCase() }
String.metaClass.upper = { toUpperCase() }


/**
 * Allow comparison of Strings and Numbers (overloading of comparison operators is not supported yet though)
 */
String.metaClass.compareTo = { Number other -> delegate.compareTo(other.toString()) }
Number.metaClass.compareTo = { String other -> delegate.toString().compareTo(other) }


/**
 * Pad strings or numbers with given characters ('0' by default).
 *
 * e.g. "1" -> "01"
 */
String.metaClass.pad = Number.metaClass.pad = { length = 2, padding = "0" -> delegate.toString().padLeft(length, padding) }


/**
 * Return a substring matching the given pattern or break.
 */
String.metaClass.match = { def matcher = delegate =~ it; if (matcher.find()) return matcher[0] else throw new Exception("Match failed") }


/**
 * Use empty string as default replacement.
 */
String.metaClass.replaceAll = { String pattern -> replaceAll(pattern, "") }


/**
 * Replace space characters with a given characters.
 *
 * e.g. "Doctor Who" -> "Doctor_Who"
 */
String.metaClass.space = { replacement -> replaceAll(/\s+/, replacement) }


/**
 * Upper-case all initials.
 * 
 * e.g. "The Day a new Demon was born" -> "The Day A New Demon Was Born"
 */
String.metaClass.upperInitial = { replaceAll(/\b[a-z]/, { it.toUpperCase() }) }


/**
 * Lower-case all letters that are not initials.
 * 
 * e.g. "Gundam SEED" -> "Gundam Seed"
 */
String.metaClass.lowerTrail = { replaceAll(/\b(\p{Alpha})(\p{Alpha}+)\b/, { match, initial, trail -> initial + trail.toLowerCase() }) }


/**
 * Return substring before the given pattern.
 */
String.metaClass.before = {
	def matcher = delegate =~ it
	
	// pattern was found, return leading substring, else return original value
	return matcher.find() ? delegate.substring(0, matcher.start()) : delegate
}


/**
 * Return substring after the given pattern.
 */
String.metaClass.after = {
	def matcher = delegate =~ it
	
	// pattern was found, return trailing substring, else return original value
	return matcher.find() ? delegate.substring(matcher.end(), delegate.length()) : delegate
}


/**
 * Replace trailing parenthesis including any leading whitespace.
 * 
 * e.g. "The IT Crowd (UK)" -> "The IT Crowd"
 */
String.metaClass.replaceTrailingBraces = 
String.metaClass.replaceTrailingBrackets = { replacement = "" -> replaceAll(/\s*[(]([^)]*)[)]$/, replacement) }


/**
 * Replace 'part identifier'.
 * 
 * e.g. "Today Is the Day: Part 1" -> "Today Is the Day, Part 1"
 * 		"Today Is the Day (1)" -> "Today Is the Day, Part 1"
 */
String.metaClass.replacePart = { replacement = "" ->
	// handle '(n)', '(Part n)' and ': Part n' like syntax
	for (pattern in [/\s*[(](\w+)[)]$/, /(?i)\W*Part (\w+)\W*$/]) {
		if ((delegate =~ pattern).find()) {
			return replaceAll(pattern, replacement);
		}
	}

	// no pattern matches, nothing to replace
	return delegate;
}


/**
 * Apply ICU transliteration
 * @see http://userguide.icu-project.org/transforms/general
 */
String.metaClass.transliterate = { transformIdentifier -> com.ibm.icu.text.Transliterator.getInstance(transformIdentifier).transform(delegate) }


/**
* Convert Unicode to ASCII as best as possible. Works with most alphabets/scripts used in the world.
*
* e.g. "Österreich" -> "Osterreich"
*      "カタカナ" -> "katakana"
*/
String.metaClass.ascii = { delegate.transliterate("Any-Latin;Latin-ASCII;[:Diacritic:]remove").replaceAll("[^\\p{ASCII}]", "?") }
