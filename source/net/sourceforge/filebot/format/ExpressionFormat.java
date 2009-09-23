
package net.sourceforge.filebot.format;


import static net.sourceforge.tuned.ExceptionUtilities.*;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.MissingPropertyException;

import java.io.FilePermission;
import java.io.InputStreamReader;
import java.net.SocketPermission;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyPermission;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;

import net.sourceforge.tuned.ExceptionUtilities;


public class ExpressionFormat extends Format {
	
	private final String expression;
	
	private final Object[] compilation;
	
	private ScriptException lastException;
	

	public ExpressionFormat(String expression) throws ScriptException {
		this.expression = expression;
		this.compilation = secure(compile(expression, (Compilable) initScriptEngine()));
	}
	

	protected ScriptEngine initScriptEngine() throws ScriptException {
		// use groovy script engine
		ScriptEngine engine = new GroovyScriptEngineFactory().getScriptEngine();
		
		engine.eval(new InputStreamReader(ExpressionFormat.class.getResourceAsStream("ExpressionFormat.lib.groovy")));
		
		return engine;
	}
	

	public String getExpression() {
		return expression;
	}
	

	protected Object[] compile(String expression, Compilable engine) throws ScriptException {
		List<Object> compilation = new ArrayList<Object>();
		
		char open = '{';
		char close = '}';
		
		StringBuilder token = new StringBuilder();
		int level = 0;
		
		// parse expressions and literals
		for (int i = 0; i < expression.length(); i++) {
			char c = expression.charAt(i);
			
			if (c == open) {
				if (level == 0) {
					if (token.length() > 0) {
						compilation.add(token.toString());
						token.setLength(0);
					}
				} else {
					token.append(c);
				}
				
				level++;
			} else if (c == close) {
				if (level == 1) {
					if (token.length() > 0) {
						try {
							compilation.add(engine.compile(token.toString()));
						} catch (ScriptException e) {
							// try to extract syntax exception
							ScriptException illegalSyntax = e;
							
							try {
								String message = findCause(e, MultipleCompilationErrorsException.class).getErrorCollector().getSyntaxError(0).getOriginalMessage();
								illegalSyntax = new ScriptException("SyntaxError: " + message);
							} catch (Exception ignore) {
								// ignore, just use original exception
							}
							
							throw illegalSyntax;
						} finally {
							token.setLength(0);
						}
					}
				} else {
					token.append(c);
				}
				
				level--;
			} else {
				token.append(c);
			}
			
			// sanity check
			if (level < 0) {
				throw new ScriptException("SyntaxError: unexpected token: " + close);
			}
		}
		
		// sanity check
		if (level != 0) {
			throw new ScriptException("SyntaxError: missing token: " + close);
		}
		
		// append tail
		if (token.length() > 0) {
			compilation.add(token.toString());
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
		// use privileged bindings so we are not restricted by the script sandbox
		Bindings priviledgedBindings = PrivilegedInvocation.newProxy(Bindings.class, bindings, AccessController.getContext());
		
		// initialize script context with the privileged bindings
		ScriptContext context = new SimpleScriptContext();
		context.setBindings(priviledgedBindings, ScriptContext.GLOBAL_SCOPE);
		
		// reset exception state
		lastException = null;
		
		for (Object snipped : compilation) {
			if (snipped instanceof CompiledScript) {
				try {
					Object value = ((CompiledScript) snipped).eval(context);
					
					if (value != null) {
						sb.append(value);
					}
				} catch (ScriptException e) {
					handleException(e);
				}
			} else {
				sb.append(snipped);
			}
		}
		
		return sb;
	}
	

	protected void handleException(ScriptException exception) {
		if (findCause(exception, MissingPropertyException.class) != null) {
			lastException = new ExpressionException(new BindingException(findCause(exception, MissingPropertyException.class).getProperty(), "undefined", exception));
		} else if (findCause(exception, GroovyRuntimeException.class) != null) {
			lastException = new ExpressionException(findCause(exception, GroovyRuntimeException.class).getMessage(), exception);
		} else {
			lastException = exception;
		}
	}
	

	public ScriptException caughtScriptException() {
		return lastException;
	}
	

	private Object[] secure(Object[] compilation) {
		// create sandbox AccessControlContext
		AccessControlContext sandbox = new AccessControlContext(new ProtectionDomain[] { new ProtectionDomain(null, getSandboxPermissions()) });
		
		for (int i = 0; i < compilation.length; i++) {
			Object snipped = compilation[i];
			
			if (snipped instanceof CompiledScript) {
				compilation[i] = new SecureCompiledScript((CompiledScript) snipped, sandbox);
			}
		}
		
		return compilation;
	}
	

	private PermissionCollection getSandboxPermissions() {
		Permissions permissions = new Permissions();
		
		permissions.add(new RuntimePermission("createClassLoader"));
		permissions.add(new FilePermission("<<ALL FILES>>", "read"));
		permissions.add(new SocketPermission("*", "connect"));
		permissions.add(new PropertyPermission("*", "read"));
		permissions.add(new RuntimePermission("getenv.*"));
		
		return permissions;
	}
	

	private static class SecureCompiledScript extends CompiledScript {
		
		private final CompiledScript compiledScript;
		private final AccessControlContext sandbox;
		

		private SecureCompiledScript(CompiledScript compiledScript, AccessControlContext sandbox) {
			this.compiledScript = compiledScript;
			this.sandbox = sandbox;
		}
		

		@Override
		public Object eval(final ScriptContext context) throws ScriptException {
			try {
				return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					
					@Override
					public Object run() throws ScriptException {
						return compiledScript.eval(context);
					}
				}, sandbox);
			} catch (PrivilegedActionException e) {
				AccessControlException accessException = ExceptionUtilities.findCause(e, AccessControlException.class);
				
				// try to unwrap AccessControlException
				if (accessException != null)
					throw new ExpressionException(accessException);
				
				// forward ScriptException
				// e.getException() should be an instance of ScriptException,
				// as only "checked" exceptions will be "wrapped" in a PrivilegedActionException
				throw (ScriptException) e.getException();
			}
		}
		

		@Override
		public ScriptEngine getEngine() {
			return compiledScript.getEngine();
		}
		
	}
	

	@Override
	public Object parseObject(String source, ParsePosition pos) {
		throw new UnsupportedOperationException();
	}
	
}
