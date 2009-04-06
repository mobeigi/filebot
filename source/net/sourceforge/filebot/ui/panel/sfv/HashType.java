
package net.sourceforge.filebot.ui.panel.sfv;


import java.io.File;
import java.util.Formatter;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;


enum HashType {
	
	SFV {
		
		@Override
		public Hash newHash() {
			return new ChecksumHash(new CRC32());
		}
		

		@Override
		public VerificationFileScanner newScanner(Scanner scanner) {
			// adapt default scanner to sfv line syntax
			return new VerificationFileScanner(scanner) {
				
				/**
				 * Pattern used to parse the lines of a sfv file.
				 * 
				 * <pre>
				 * Sample:
				 * folder/file.txt 970E4EF1
				 * |  Group 1    | | Gr.2 |
				 * </pre>
				 */
				private final Pattern pattern = Pattern.compile("(.+)\\s+(\\p{XDigit}{8})");
				
				
				@Override
				protected Entry<File, String> parseLine(String line) {
					Matcher matcher = pattern.matcher(line);
					
					if (!matcher.matches())
						throw new IllegalSyntaxException(getLineNumber(), line);
					
					return entry(new File(matcher.group(1)), matcher.group(2));
				}
			};
		}
		

		@Override
		public VerificationFilePrinter newPrinter(Formatter out) {
			return new VerificationFilePrinter(out, "CRC32") {
				
				@Override
				public void print(String path, String hash) {
					// e.g folder/file.txt 970E4EF1
					out.format(String.format("%s %s", path, hash));
				}
			};
		}
		
	},
	
	MD5 {
		
		@Override
		public Hash newHash() {
			return new MessageDigestHash("MD5");
		}
		

		@Override
		public VerificationFileScanner newScanner(Scanner scanner) {
			return new VerificationFileScanner(scanner);
		}
		

		@Override
		public VerificationFilePrinter newPrinter(Formatter out) {
			// e.g. 50e85fe18e17e3616774637a82968f4c *folder/file.txt
			return new VerificationFilePrinter(out, null);
		}
	},
	
	SHA1 {
		
		@Override
		public Hash newHash() {
			return new MessageDigestHash("SHA-1");
		}
		

		@Override
		public VerificationFileScanner newScanner(Scanner scanner) {
			return new VerificationFileScanner(scanner);
		}
		

		@Override
		public VerificationFilePrinter newPrinter(Formatter out) {
			// e.g 1a02a7c1e9ac91346d08829d5037b240f42ded07 ?SHA1*folder/file.txt
			return new VerificationFilePrinter(out, "SHA1");
		}
		

		@Override
		public String toString() {
			return "SHA-1";
		}
	};
	
	public abstract Hash newHash();
	

	public abstract VerificationFileScanner newScanner(Scanner scanner);
	

	public abstract VerificationFilePrinter newPrinter(Formatter out);
	

	public String getExtension() {
		return name().toLowerCase();
	}
	
}
