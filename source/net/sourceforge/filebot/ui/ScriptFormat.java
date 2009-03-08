
package net.sourceforge.filebot.ui;


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
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


public abstract class ScriptFormat extends Format {
	
	private final String format;
	
	private final Object[] expressions;
	
	
	public ScriptFormat(String format) throws ScriptException {
		this.format = format;
		this.expressions = compile(format, (Compilable) initScriptEngine());
	}
	

	protected ScriptEngine initScriptEngine() throws ScriptException {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
		
		engine.eval(new InputStreamReader(getClass().getResourceAsStream("ScriptFormat.global.js")));
		
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
	

	protected abstract Bindings getBindings(Object object);
	

	@Override
	public StringBuffer format(Object object, StringBuffer sb, FieldPosition pos) {
		Bindings bindings = getBindings(object);
		
		try {
			for (Object snipped : expressions) {
				if (snipped instanceof String) {
					sb.append(snipped);
				} else {
					Object value = ((CompiledScript) snipped).eval(bindings);
					
					if (value != null) {
						sb.append(value);
					}
				}
			}
		} catch (ScriptException e) {
			throw new IllegalArgumentException(e);
		}
		
		return sb;
	}
	

	@Override
	public Object parseObject(String source, ParsePosition pos) {
		throw new UnsupportedOperationException();
	}
	
}
