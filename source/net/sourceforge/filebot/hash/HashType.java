
package net.sourceforge.filebot.hash;


import java.util.Formatter;
import java.util.Scanner;
import java.util.zip.CRC32;


public enum HashType {
	
	SFV {
		
		@Override
		public Hash newHash() {
			return new ChecksumHash(new CRC32());
		}
		

		@Override
		public VerificationFileScanner newScanner(Scanner scanner) {
			return new SfvFileScanner(scanner);
		}
		

		@Override
		public VerificationFilePrinter newPrinter(Formatter out) {
			return new SfvFilePrinter(out);
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
