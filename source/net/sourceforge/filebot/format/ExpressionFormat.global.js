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
	return this.replace(/\s/g, replacement);
}


/**
 * Remove trailing parenthesis including any leading whitespace.
 * 
 * e.g. "Doctor Who (2005)" -> "Doctor Who"
 */
String.prototype.removeTrailingBraces = function() {
	return this.replace(/\s*\([^\)]*\)$/, "");
}
