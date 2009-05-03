/**
 * Convenience method to pad strings or numbers with given characters ('0' by default)
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
 * Convenience method to replace space characters with a given characters 
 */
String.prototype.space = function(replacement) {
	return this.replace(/\s/g, replacement);
}
