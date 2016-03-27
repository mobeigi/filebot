package net.filebot.format;

import static net.filebot.similarity.Normalization.*;
import static net.filebot.util.ExceptionUtilities.*;
import static net.filebot.util.FileUtilities.*;

import java.security.AccessController;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.MissingPropertyException;

public class ExpressionFormat extends Format {

	private static ScriptEngine engine;
	private static Map<String, CompiledScript> scriptletCache = new HashMap<String, CompiledScript>();

	protected static ScriptEngine createScriptEngine() {
		CompilerConfiguration config = new CompilerConfiguration();

		// include default functions
		ImportCustomizer imports = new ImportCustomizer();
		imports.addStaticStars(ExpressionFormatFunctions.class.getName());
		config.addCompilationCustomizers(imports);

		GroovyClassLoader classLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), config);
		return new GroovyScriptEngineImpl(classLoader);
	}

	protected static synchronized ScriptEngine getGroovyScriptEngine() throws ScriptException {
		if (engine == null) {
			engine = createScriptEngine();
		}
		return engine;
	}

	protected static synchronized CompiledScript compileScriptlet(String expression) throws ScriptException {
		Compilable engine = (Compilable) getGroovyScriptEngine();
		CompiledScript scriptlet = scriptletCache.get(expression);
		if (scriptlet == null) {
			scriptlet = engine.compile(expression);
			scriptletCache.put(expression, scriptlet);
		}
		return scriptlet;
	}

	private final String expression;

	private final Object[] compilation;

	private ScriptException lastException;

	public ExpressionFormat(String expression) throws ScriptException {
		this.expression = expression;
		this.compilation = secure(compile(expression));
	}

	public String getExpression() {
		return expression;
	}

	protected Object[] compile(String expression) throws ScriptException {
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
							compilation.add(compileScriptlet(token.toString()));
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
		return new ExpressionBindings(value) {

			@Override
			public Object get(Object key) {
				return normalizeBindingValue(super.get(key));
			}
		};
	}

	@Override
	public StringBuffer format(Object object, StringBuffer sb, FieldPosition pos) {
		return sb.append(format(getBindings(object)));
	}

	public String format(Bindings bindings) {
		// use privileged bindings so we are not restricted by the script sandbox
		Bindings priviledgedBindings = PrivilegedInvocation.newProxy(Bindings.class, bindings, AccessController.getContext());

		// initialize script context with the privileged bindings
		ScriptContext context = new SimpleScriptContext();
		context.setBindings(priviledgedBindings, ScriptContext.GLOBAL_SCOPE);

		// reset exception state
		lastException = null;

		StringBuilder sb = new StringBuilder();
		for (Object snippet : compilation) {
			if (snippet instanceof CompiledScript) {
				try {
					CharSequence value = normalizeExpressionValue(((CompiledScript) snippet).eval(context));
					if (value != null) {
						sb.append(value);
					}
				} catch (ScriptException e) {
					handleException(e);
				}
			} else {
				sb.append(snippet);
			}
		}

		return normalizeResult(sb);
	}

	protected Object normalizeBindingValue(Object value) {
		// if the binding value is a String, remove illegal characters
		if (value instanceof CharSequence) {
			return replacePathSeparators((CharSequence) value, " ").trim();
		}

		// if the binding value is an Object, just leave it
		return value;
	}

	protected CharSequence normalizeExpressionValue(Object value) {
		return value == null ? null : value.toString();
	}

	protected String normalizeResult(CharSequence value) {
		return replaceSpace(value.toString(), " ").trim();
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
		for (int i = 0; i < compilation.length; i++) {
			Object snipped = compilation[i];

			if (snipped instanceof CompiledScript) {
				compilation[i] = new SecureCompiledScript((CompiledScript) snipped);
			}
		}

		return compilation;
	}

	@Override
	public Object parseObject(String source, ParsePosition pos) {
		throw new UnsupportedOperationException();
	}

}
