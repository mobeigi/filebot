
package net.sourceforge.filebot.hash;


import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MessageDigestHash implements Hash {
	
	private final MessageDigest md;
	
	
	public MessageDigestHash(String algorithm) {
		try {
			this.md = MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}
	

	public MessageDigestHash(MessageDigest md) {
		this.md = md;
	}
	

	@Override
	public void update(byte[] bytes, int off, int len) {
		md.update(bytes, off, len);
	}
	

	@Override
	public String digest() {
		// e.g. %032x (format for MD-5)
		return String.format("%0" + (md.getDigestLength() * 2) + "x", new BigInteger(1, md.digest()));
	}
	
}
