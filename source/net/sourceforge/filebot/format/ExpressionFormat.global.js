
String.prototype.pad = Number.prototype.pad = function(length, padding) {
	var s = this.toString();
	
	// use default padding, if padding is undefined or empty
	var p = padding ? padding.toString() : '0';
	
	while (s.length < length) {
		s = p + s;
	}
	
	return s;
}


String.prototype.space = function(replacement) {
	return this.replace(/\s/g, replacement);
}
