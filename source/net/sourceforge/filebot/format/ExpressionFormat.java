
package net.sourceforge.filebot.format;


import java.io.InputStreamReader;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;


public class ExpressionFormat extends Format {
	
	private final String format;
	
	private final Object[] expressions;
	
	private ScriptException lastException;
	
	
	public ExpressionFormat(String format) throws ScriptException {
		this.format = format;
		this.expressions = compile(format, (Compilable) initScriptEngine());
	}
	

	protected ScriptEngine initScriptEngine() throws ScriptException {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
		
		engine.eval(new InputStreamReader(ExpressionFormat.class.getResourceAsStream("ExpressionFormat.global.js")));
		
		return engine;
	}
	

	public String getFormat() {
		return format;
	}
	

	protected Object[] compile(String format, Compilable engine) throws ScriptException {
		List<Object> expression = new ArrayList<Object>();
		
		Matcher matcher = Pattern.compile("\\{([^\\{]*?)\\}").matcher(format);
		
		int position = 0;
		
		while (matcher.find()) {
			if (position < matcher.start()) {
				// literal before
				expression.add(format.substring(position, matcher.start()));
			}
			
			String script = matcher.group(1);
			
			if (script.length() > 0) {
				// compiled script, or literal
				expression.add(engine.compile(script));
			}
			
			position = matcher.end();
		}
		
		if (position < format.length()) {
			// tail
			expression.add(format.substring(position, format.length()));
		}
		
		return expression.toArray();
	}
	

	protected Bindings getBindings(Object value) {
		// no bindings by default
		return null;
	}
	

	@Override
	public StringBuffer format(Object object, StringBuffer sb, FieldPosition pos) {
		Bindings bindings = getBindings(object);
		
		ScriptContext context = new SimpleScriptContext();
		context.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
		
		try {
			for (Object snipped : expressions) {
				if (snipped instanceof CompiledScript) {
					try {
						Object value = ((CompiledScript) snipped).eval(context);
						
						if (value != null) {
							sb.append(value);
						}
					} catch (ScriptException e) {
						lastException = e;
					}
				} else {
					sb.append(snipped);
				}
			}
		} finally {
			dispose(bindings);
		}
		
		return sb;
	}
	

	protected void dispose(Bindings bindings) {
		
	}
	

	public ScriptException scriptException() {
		return lastException;
	}
	

	@Override
	public Object parseObject(String source, ParsePosition pos) {
		throw new UnsupportedOperationException();
	}
	
}
