
String.prototype.pad = function(length, padding) {
	if (padding == undefined) {
		padding = '0';
	}
	
	var s = this;
	
	if (parseInt(this) >= 0 && padding.length >= 1) {
		while (s.length < length) {
			s = padding.concat(s)
		}
	}
	
	return s;
};
