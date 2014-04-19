
package net.sourceforge.filebot.cli;


import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;


public class BindingsHandler extends OptionHandler<Entry<String, String>> {
	
	public BindingsHandler(CmdLineParser parser, OptionDef option, Setter<Entry<String, String>> setter) {
		super(parser, option, setter);
	}
	
	
	@Override
	public String getDefaultMetaVariable() {
		return "[name=value]";
	}
	
	
	@Override
	public int parseArguments(Parameters params) throws CmdLineException {
		int pos = 0;
		while (pos < params.size()) {
			String[] nv = params.getParameter(pos).split("=", 2);
			
			if (nv.length < 2 || nv[0].startsWith("-")) {
				return pos;
			}
			
			String n = nv[0].trim();
			String v = nv[1].trim();
			
			if (!isIdentifier(n)) {
				throw new CmdLineException(owner, String.format("\"%s\" is not a valid identifier", n));
			}
			
			setter.addValue(new SimpleEntry<String, String>(n, v));
			pos++;
		}
		
		return pos;
	}
	
	
	public boolean isIdentifier(String n) {
		if (n.isEmpty())
			return false;
		
		for (int i = 0; i < n.length();) {
			int c = n.codePointAt(i);
			
			if (i == 0) {
				if (!Character.isUnicodeIdentifierStart(c))
					return false;
			} else {
				if (!Character.isUnicodeIdentifierPart(c))
					return false;
			}
			
			i += Character.charCount(c);
		}
		
		return true;
	}
}
