
package net.sourceforge.filebot.format;


import java.io.FilePermission;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import net.sourceforge.tuned.ExceptionUtilities;

import org.mozilla.javascript.EcmaError;

import com.sun.phobos.script.javascript.RhinoScriptEngine;


public class ExpressionFormat extends Format {
	
	private final String expression;
	
	private final Object[] compilation;
	
	private ScriptException lastException;
	
	
	public ExpressionFormat(String expression) throws ScriptException {
		this.expression = expression;
		this.compilation = secure(compile(expression, (Compilable) initScriptEngine()));
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
		
		// use privileged bindings so we are not restricted by the script sandbox
		context.setBindings(PrivilegedBindings.newProxy(bindings, AccessController.getContext()), ScriptContext.GLOBAL_SCOPE);
		
		for (Object snipped : compilation) {
			if (snipped instanceof CompiledScript) {
				try {
					Object value = ((CompiledScript) snipped).eval(context);
					
					if (value != null) {
						sb.append(value);
					}
				} catch (ScriptException e) {
					EcmaError ecmaError = ExceptionUtilities.findCause(e, EcmaError.class);
					
					// try to unwrap EcmaError
					if (ecmaError != null) {
						lastException = new ExpressionException(String.format("%s: %s", ecmaError.getName(), ecmaError.getErrorMessage()), e);
					} else {
						lastException = e;
					}
				} catch (RuntimeException e) {
					lastException = new ExpressionException(e);
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
		permissions.add(new PropertyPermission("*", "read"));
		permissions.add(new RuntimePermission("getenv.*"));
		
		return permissions;
	}
	
	
	private static class PrivilegedBindings implements InvocationHandler {
		
		private final Bindings bindings;
		private final AccessControlContext context;
		
		
		private PrivilegedBindings(Bindings bindings, AccessControlContext context) {
			this.bindings = bindings;
			this.context = context;
		}
		

		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
			try {
				return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					
					@Override
					public Object run() throws Exception {
						return method.invoke(bindings, args);
					}
				}, context);
			} catch (PrivilegedActionException e) {
				Throwable cause = e.getException();
				
				// the underlying method may have throw an exception
				if (cause instanceof InvocationTargetException) {
					// get actual cause
					cause = cause.getCause();
				}
				
				// forward cause
				throw cause;
			}
		}
		

		public static Bindings newProxy(Bindings bindings, AccessControlContext context) {
			InvocationHandler invocationHandler = new PrivilegedBindings(bindings, context);
			
			// create dynamic invocation proxy
			return (Bindings) Proxy.newProxyInstance(PrivilegedBindings.class.getClassLoader(), new Class[] { Bindings.class }, invocationHandler);
		}
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
