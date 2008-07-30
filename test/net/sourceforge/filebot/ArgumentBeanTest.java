
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
		ArgumentBean bean = parse("--sfv", "One Piece", "-clear");
		
		assertTrue(bean.isClear());
		assertFalse(bean.isHelp());
		assertEquals("One Piece", bean.getSfvPanelFile().getName());
	}
	

	@Test
	public void noClear() throws Exception {
		ArgumentBean bean = parse("-help", "--sfv", "One Piece");
		
		assertTrue(bean.isHelp());
		assertFalse(bean.isClear());
		assertEquals("One Piece", bean.getSfvPanelFile().getName());
	}
	

	@Test
	public void oneArgument() throws Exception {
		ArgumentBean bean = parse("--sfv", "One Piece.sfv");
		
		assertFalse(bean.isClear());
		assertFalse(bean.isHelp());
		assertEquals("One Piece.sfv", bean.getSfvPanelFile().getName());
	}
	

	@Test
	public void mixedArguments() throws Exception {
		ArgumentBean bean = parse("--list", "Twin Peaks.txt", "--sfv", "Death Note.sfv");
		
		assertEquals("Twin Peaks.txt", bean.getListPanelFile().getName());
		assertEquals("Death Note.sfv", bean.getSfvPanelFile().getName());
	}
	

	private static ArgumentBean parse(String... args) throws CmdLineException {
		ArgumentBean bean = new ArgumentBean();
		
		new CmdLineParser(bean).parseArgument(args);
		
		return bean;
	}
}
