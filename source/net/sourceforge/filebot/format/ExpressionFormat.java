
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
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import com.sun.phobos.script.javascript.RhinoScriptEngine;


public class ExpressionFormat extends Format {
	
	private final String expression;
	
	private final Object[] compilation;
	
	private ScriptException lastException;
	
	
	public ExpressionFormat(String expression) throws ScriptException {
		this.expression = expression;
		this.compilation = compile(expression, (Compilable) initScriptEngine());
	}
	

	protected ScriptEngine initScriptEngine() throws ScriptException {
		// don't use jdk rhino so we can use rhino specific features and classes (e.g. Scriptable)
		ScriptEngine engine = new RhinoScriptEngine();
		
		engine.eval(new InputStreamReader(ExpressionFormat.class.getResourceAsStream("ExpressionFormat.global.js")));
		
		return engine;
	}
	

	public String getExpression() {
		return expression;
	}
	

	protected Object[] compile(String expression, Compilable engine) throws ScriptException {
		List<Object> compilation = new ArrayList<Object>();
		
		Matcher matcher = Pattern.compile("\\{([^\\{]*?)\\}").matcher(expression);
		
		int position = 0;
		
		while (matcher.find()) {
			if (position < matcher.start()) {
				// literal before
				compilation.add(expression.substring(position, matcher.start()));
			}
			
			String script = matcher.group(1);
			
			if (script.length() > 0) {
				// compiled script, or literal
				compilation.add(engine.compile(script));
			}
			
			position = matcher.end();
		}
		
		if (position < expression.length()) {
			// tail
			compilation.add(expression.substring(position, expression.length()));
		}
		
		return compilation.toArray();
	}
	

	public Bindings getBindings(Object value) {
		return new ExpressionBindings(value);
	}
	

	@Override
	public StringBuffer format(Object object, StringBuffer sb, FieldPosition pos) {
		return format(getBindings(object), sb);
	}
	

	public StringBuffer format(Bindings bindings, StringBuffer sb) {
		ScriptContext context = new SimpleScriptContext();
		context.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
		
		for (Object snipped : compilation) {
			if (snipped instanceof CompiledScript) {
				try {
					Object value = ((CompiledScript) snipped).eval(context);
					
					if (value != null) {
						sb.append(value);
					}
				} catch (ScriptException e) {
					lastException = e;
				} catch (Exception e) {
					lastException = new ScriptException(e);
				}
			} else {
				sb.append(snipped);
			}
		}
		
		return sb;
	}
	

	public ScriptException scriptException() {
		return lastException;
	}
	

	@Override
	public Object parseObject(String source, ParsePosition pos) {
		throw new UnsupportedOperationException();
	}
	
}
