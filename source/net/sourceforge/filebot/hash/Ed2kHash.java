
package net.sourceforge.filebot.hash;


import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import jonelo.jacksum.algorithm.Edonkey;


public class Ed2kHash implements Hash {
	
	private final Edonkey ed2k;
	
	
	public Ed2kHash() {
		try {
			this.ed2k = new Edonkey();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	@Override
	public void update(byte[] bytes, int off, int len) {
		ed2k.update(bytes, off, len);
	}
	
	
	@Override
	public String digest() {
		return String.format("%0" + (ed2k.getByteArray().length * 2) + "x", new BigInteger(1, ed2k.getByteArray()));
	}
	
}
