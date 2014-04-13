package net.sourceforge.filebot.format;

public class ExpressionFormatMethods {

	public static String lower(String self) {
		return self.toLowerCase();
	}

	public static String upper(String self) {
		return self.toUpperCase();
	}

	public static String pad(String self, int length, String padding) {
		while (self.length() < length) {
			self = padding + self;
		}
		return self;
	}

	public static String pad(String self, int length) {
		return pad(self, length, "0");
	}

	public static String pad(Number self, int length) {
		return pad(self.toString(), length);
	}
}
