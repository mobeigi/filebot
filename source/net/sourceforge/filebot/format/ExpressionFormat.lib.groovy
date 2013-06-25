
import static net.sourceforge.tuned.FileUtilities.*
import java.util.regex.Pattern


// simplified switch/case pattern matching
Object.metaClass.match = { Map cases -> def val = delegate; cases.findResult { switch(val) { case it.key: return it.value} } }
def csv(path, delim = ';', keyIndex = 0, valueIndex = 1) { def f = path as File; def values = [:]; f.splitEachLine(delim) { values.put(it[keyIndex], it[valueIndex]) }; return values }
def c(c) { try { c.call() } catch (Throwable e) { null } }


/**
* Allow getAt() for File paths
*
* e.g. file[0] -> "F:"
*/
File.metaClass.getAt = { Range range -> listPath(delegate).collect{ replacePathSeparators(getName(it)).trim() }.getAt(range).join(File.separator) }
File.metaClass.getAt = { int index -> listPath(delegate).collect{ replacePathSeparators(getName(it)).trim() }.getAt(index) }
File.metaClass.getRoot = { listPath(delegate)[0] }
File.metaClass.listPath = { listPath(delegate) }
File.metaClass.getDiskSpace = { listPath(delegate).reverse().find{ it.exists() }?.usableSpace ?: 0 }


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
String.metaClass.match = { String pattern, matchGroup = null -> 
	def matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE).matcher(delegate)
	if (matcher.find())
		return matcher.groupCount() > 0 && matchGroup == null ? matcher.group(1) : matcher.group(matchGroup ?: 0)
	else
		throw new Exception("Match failed")
}

/**
 * Return a list of all matching patterns or break.
 */
String.metaClass.matchAll = { String pattern, int matchGroup = 0 ->
	def matches = []
	def matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(delegate)
	while(matcher.find())
		matches += matcher.group(matchGroup)
	
	if (matches.size() > 0)
		return matches
	else
		throw new Exception("Match failed")
}


/**
 * Use empty string as default replacement.
 */
String.metaClass.replaceAll = { String pattern -> replaceAll(pattern, "") }


/**
 * Replace space characters with a given characters.
 *
 * e.g. "Doctor Who" -> "Doctor_Who"
 */
String.metaClass.space = { replacement -> replaceAll(/[:?._]/, " ").trim().replaceAll(/\s+/, replacement) }


/**
 * Upper-case all initials.
 * 
 * e.g. "The Day a new Demon was born" -> "The Day A New Demon Was Born"
 */
String.metaClass.upperInitial = { replaceAll(/(?<=[&()+.,-;<=>?\[\]_{|}~ ]|^)[a-z]/, { it.toUpperCase() }) }


/**
* Get acronym, i.e. first letter of each word.
*
* e.g. "Deep Space 9" -> "DS9"
*/
String.metaClass.acronym = { findAll(/(?<=[&()+.,-;<=>?\[\]_{|}~ ]|^)[\p{Alnum}]/).join().toUpperCase() }

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
String.metaClass.replaceTrailingBrackets = { replacement = "" -> replaceAll(/\s*[(]([^)]*)[)]$/, replacement) }


/**
 * Replace 'part identifier'.
 * 
 * e.g. "Today Is the Day: Part 1" -> "Today Is the Day, Part 1"
 * 		"Today Is the Day (1)" -> "Today Is the Day, Part 1"
 */
String.metaClass.replacePart = { replacement = "" ->
	// handle '(n)', '(Part n)' and ': Part n' like syntax
	for (pattern in [/\s*[(](\w+)[)]$/, /(?i)\W+Part (\w+)\W*$/]) {
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
String.metaClass.ascii = { fallback = ' ' -> delegate.transliterate("Any-Latin;Latin-ASCII;[:Diacritic:]remove").replaceAll("[^\\p{ASCII}]+", fallback) }
