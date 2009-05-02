
package net.sourceforge.filebot.hash;


import java.util.Formatter;


public class SfvFilePrinter extends VerificationFilePrinter {
	
	public SfvFilePrinter(Formatter out) {
		super(out, "CRC32");
	}
	

	@Override
	public void println(String path, String hash) {
		// e.g folder/file.txt 970E4EF1
		out.format(String.format("%s %s%n", path, hash));
	}
}
