
package net.sourceforge.filebot;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;


public class ArgumentBeanTest {
	
	@Test
	public void clear() throws Exception {
		ArgumentBean bean = parse("-clear", "--sfv", "One Piece", "Naruto");
		
		assertTrue(bean.clear());
		assertFalse(bean.help());
		
		assertEquals("One Piece", bean.arguments().get(0).toString());
		assertEquals("Naruto", bean.arguments().get(1).toString());
	}
	

	@Test
	public void noClear() throws Exception {
		ArgumentBean bean = parse("--sfv", "One Piece.sfv");
		
		assertFalse(bean.help());
		assertFalse(bean.clear());
		assertEquals("One Piece.sfv", bean.arguments().get(0).toString());
	}
	

	private static ArgumentBean parse(String... args) throws CmdLineException {
		ArgumentBean bean = new ArgumentBean();
		
		new CmdLineParser(bean).parseArgument(args);
		
		return bean;
	}
}
