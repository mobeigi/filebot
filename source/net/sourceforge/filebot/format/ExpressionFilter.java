
package net.sourceforge.filebot.format;


import java.io.InputStreamReader;
import java.security.AccessController;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;


public class ExpressionFilter {
	
	private final String expression;
	
	private final CompiledScript userScript;
	private final CompiledScript asBooleanScript;
	
	private Throwable lastException;
	
	
	public ExpressionFilter(String expression) throws ScriptException {
		this.expression = expression;
		
		Compilable engine = (Compilable) initScriptEngine();
		this.userScript = new SecureCompiledScript(engine.compile(expression)); // sandboxed script
		this.asBooleanScript = engine.compile("value as Boolean");
	}
	
	
	public String getExpression() {
		return expression;
	}
	
	
	public Throwable getLastException() {
		return lastException;
	}
	
	
	protected ScriptEngine initScriptEngine() throws ScriptException {
		// use Groovy script engine
		ScriptEngine engine = new GroovyScriptEngineFactory().getScriptEngine();
		engine.eval(new InputStreamReader(ExpressionFormat.class.getResourceAsStream("ExpressionFormat.lib.groovy")));
		return engine;
	}
	
	
	public boolean matches(Object value) throws ScriptException {
		return matches(new ExpressionBindings(value));
	}
	
	
	public boolean matches(Bindings bindings) throws ScriptException {
		this.lastException = null;
		
		// use privileged bindings so we are not restricted by the script sandbox
		Bindings priviledgedBindings = PrivilegedInvocation.newProxy(Bindings.class, bindings, AccessController.getContext());
		
		// initialize script context with the privileged bindings
		ScriptContext context = new SimpleScriptContext();
		context.setBindings(priviledgedBindings, ScriptContext.GLOBAL_SCOPE);
		
		try {
			// evaluate user script
			Object value = userScript.eval(context);
			
			// convert value to boolean
			Bindings valueBinding = new SimpleBindings();
			valueBinding.put("value", value);
			Object result = asBooleanScript.eval(valueBinding);
			if (result instanceof Boolean) {
				return (Boolean) result;
			}
		} catch (Throwable e) {
			// ignore any and all scripting exceptions
			this.lastException = e;
		}
		
		return false;
	}
	
}
