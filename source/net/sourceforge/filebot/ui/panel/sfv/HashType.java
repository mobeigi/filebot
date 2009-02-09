
package net.sourceforge.filebot.ui.panel.sfv;


import java.util.zip.CRC32;


enum HashType {
	
	CRC32 {
		
		@Override
		public Hash newInstance() {
			return new ChecksumHash(new CRC32());
		}
	},
	MD5 {
		
		@Override
		public Hash newInstance() {
			return new MessageDigestHash("MD5");
		}
	},
	SHA1 {
		
		@Override
		public Hash newInstance() {
			return new MessageDigestHash("SHA-1");
		}
	};
	
	public abstract Hash newInstance();
	
}
