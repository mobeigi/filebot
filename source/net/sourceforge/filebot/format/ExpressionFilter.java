
package net.sourceforge.filebot.format;


import java.io.InputStreamReader;
import java.security.AccessController;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;


public class ExpressionFilter {
	
	private final String expression;
	
	private final CompiledScript script;
	
	
	public ExpressionFilter(String expression) throws ScriptException {
		this.expression = expression;
		this.script = new SecureCompiledScript(((Compilable) initScriptEngine()).compile(expression)); // sandboxed script
	}
	
	
	public String getExpression() {
		return expression;
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
		// use privileged bindings so we are not restricted by the script sandbox
		Bindings priviledgedBindings = PrivilegedInvocation.newProxy(Bindings.class, bindings, AccessController.getContext());
		
		// initialize script context with the privileged bindings
		ScriptContext context = new SimpleScriptContext();
		context.setBindings(priviledgedBindings, ScriptContext.GLOBAL_SCOPE);
		
		try {
			Object value = script.eval(context);
			if (value instanceof Boolean) {
				return (Boolean) value;
			}
		} catch (Throwable e) {
			// ignore any and all scripting exceptions
		}
		
		return false;
	}
	
}
