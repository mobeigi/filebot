// System, Math, Integer, etc.
importPackage(java.lang);

// Collection, Scanner, Random, UUID, etc.
importPackage(java.util);


/**
 * Convenience methods for String.toLowerCase() and String.toUpperCase().
 */
String.prototype.lower = String.prototype.toLowerCase;
String.prototype.upper = String.prototype.toUpperCase;


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
 * Upper-case all initials.
 * 
 * e.g. "The Day a new Demon was born" -> "The Day A New Demon Was Born"
 */
String.prototype.upperInitial = function() {
	return this.replace(/\b[a-z]/g, function(letter) { return letter.toUpperCase() });
}


/**
 * Lower-case all letters that are not initials.
 * 
 * e.g. "Gundam SEED" -> "Gundam Seed"
 */
String.prototype.lowerTrail = function() {
	return this.replace(/\b([a-z])([a-z]+)\b/gi, function(match, initial, trail) { return initial + trail.toLowerCase() });
}


/**
 * Remove leading and trailing whitespace.
 */
String.prototype.trim = function() {
	return this.replace(/^\s+|\s+$/g, "");
}


/**
 * Return substring before the given delimiter.
 */
String.prototype.before = function(delimiter) {
	var endIndex = delimiter instanceof RegExp ? this.search(delimiter) : this.indexOf(delimiter);
	
	// delimiter was found, return leading substring, else return original value
	return endIndex >= 0 ? this.substring(0, endIndex) : this;
}


/**
 * Return substring after the given delimiter.
 */
String.prototype.after = function(delimiter) {
	if (delimiter instanceof RegExp) {
		var match = this.match(delimiter);
		
		if (match == null)
			return this;
		
		// use pattern match as delimiter
		delimiter = match[0];
	}
	
	var startIndex = this.indexOf(delimiter);
	
	// delimiter was found, return trailing substring, else return original value
	return startIndex >= 0 ? this.substring(startIndex + delimiter.length, this.length) : this;
}


/**
 * Replace trailing parenthesis including any leading whitespace.
 * 
 * e.g. "The IT Crowd (UK)" -> "The IT Crowd"
 */
String.prototype.replaceTrailingBraces = function(replacement) {
	// use empty string as default replacement
	var r = replacement ? replacement : "";
	
	return this.replace(/\s*[(]([^)]*)[)]$/, r);
}


/**
 * Replace 'part section'.
 * 
 * e.g. "Today Is the Day: Part 1" -> "Today Is the Day, Part 1"
 * 		"Today Is the Day (1)" -> "Today Is the Day, Part 1"
 */
String.prototype.replacePart = function (replacement) {
	// use empty string as default replacement
	var r = replacement ? replacement : "";
	
	// handle '(n)', '(Part n)' and ': Part n' like syntax
	var pattern = [/\s*[(](\w+)[)]$/i, /\W*Part (\w+)\W*$/i];
	
	for (var i = 0; i < pattern.length; i++) {
		if (pattern[i].test(this)) {
			return this.replace(pattern[i], r);
		}
	}
	
	// no pattern matches, nothing to replace
	return this;
}
