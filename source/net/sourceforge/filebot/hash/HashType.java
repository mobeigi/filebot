
package net.sourceforge.filebot.hash;


import java.util.zip.CRC32;


public enum HashType {
	
	SFV {
		
		@Override
		public Hash newHash() {
			return new ChecksumHash(new CRC32());
		}
		

		@Override
		public VerificationFormat getFormat() {
			// e.g folder/file.txt 970E4EF1
			return new SfvFormat();
		}
		
	},
	
	MD5 {
		
		@Override
		public Hash newHash() {
			return new MessageDigestHash("MD5");
		}
		

		@Override
		public VerificationFormat getFormat() {
			// e.g. 50e85fe18e17e3616774637a82968f4c *folder/file.txt
			return new VerificationFormat();
		}
		
	},
	
	SHA1 {
		
		@Override
		public Hash newHash() {
			return new MessageDigestHash("SHA-1");
		}
		

		@Override
		public VerificationFormat getFormat() {
			// e.g 1a02a7c1e9ac91346d08829d5037b240f42ded07 ?SHA1*folder/file.txt
			return new VerificationFormat("SHA1");
		}
		

		@Override
		public String toString() {
			return "SHA-1";
		}
		
	};
	
	public abstract Hash newHash();
	

	public abstract VerificationFormat getFormat();
	

	public String getExtension() {
		return name().toLowerCase();
	}
	

	public static HashType forName(String name) {
		for (HashType value : HashType.values()) {
			if (value.name().equalsIgnoreCase(name)) {
				return value;
			}
		}
		
		// value not found
		return null;
	}
	
}
