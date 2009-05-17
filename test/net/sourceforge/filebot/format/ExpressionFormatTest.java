
package net.sourceforge.filebot.format;


import static org.junit.Assert.*;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.junit.Test;


public class ExpressionFormatTest {
	
	@Test
	public void compile() throws Exception {
		ExpressionFormat format = new TestScriptFormat("");
		
		Object[] expression = format.compile("name: {name}, number: {number}", (Compilable) format.initScriptEngine());
		
		assertEquals(4, expression.length, 0);
		
		assertTrue(expression[0] instanceof String);
		assertTrue(expression[1] instanceof CompiledScript);
		assertTrue(expression[2] instanceof String);
		assertTrue(expression[3] instanceof CompiledScript);
	}
	

	@Test
	public void format() throws Exception {
		assertEquals("X5-452", new TestScriptFormat("X5-{value}").format("452"));
		
		// test pad
		assertEquals("[007]", new TestScriptFormat("[{value.pad(3)}]").format("7"));
		
		// choice
		assertEquals("not to be", new TestScriptFormat("{if (value) 'to be'; else 'not to be'}").format(null));
		
		// empty choice
		assertEquals("", new TestScriptFormat("{if (value) 'to be'}").format(null));
		
		// loop
		assertEquals("0123456789", new TestScriptFormat("{var s=''; for (var i=0; i<parseInt(value);i++) s+=i;}").format("10"));
	}
	
	
	protected static class TestScriptFormat extends ExpressionFormat {
		
		public TestScriptFormat(String format) throws ScriptException {
			super(format);
		}
		

		@Override
		public Bindings getBindings(Object value) {
			Bindings bindings = new SimpleBindings();
			
			bindings.put("value", value);
			
			return bindings;
		}
		
	}
	
}
