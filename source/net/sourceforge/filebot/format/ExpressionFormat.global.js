// System, Math, Integer, etc.
importPackage(java.lang);

// Collection, Scanner, Random, UUID, etc.
importPackage(java.util);

/**
 * Pad strings or numbers with given characters ('0' by default).
 *
 * e.g. "1" -> "01"
 */
String.prototype.pad = Number.prototype.pad = function(length, padding) {
	var s = this.toString();
	
	// use default padding, if padding is undefined or empty
	var p = padding ? padding.toString() : '0';
	
	while (s.length < length) {
		s = p + s;
	}
	
	return s;
}


/**
 * Replace space characters with a given characters.
 *
 * e.g. "Doctor Who" -> "Doctor_Who"
 */
String.prototype.space = function(replacement) {
	return this.replace(/\s+/g, replacement);
}


/**
 * Remove trailing parenthesis including any leading whitespace.
 * 
 * e.g. "Doctor Who (2005)" -> "Doctor Who"
 *	    "Bad Wolf (1)" -> "Bad Wolf, Part 1"
 */
String.prototype.replaceTrailingBraces = function(replacement) {
	// use empty string as default replacement
	var r = replacement ? replacement : "";
	
	return this.replace(/\s*\(([^\)]*)\)$/, r);
}
