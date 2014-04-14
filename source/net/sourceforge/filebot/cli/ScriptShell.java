package net.sourceforge.filebot.cli;

import groovy.lang.GroovyClassLoader;

import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;
import java.util.ResourceBundle;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import net.sourceforge.filebot.format.ExpressionFormat;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.codehaus.groovy.runtime.StackTraceUtils;

public class ScriptShell {

	private final ScriptEngine engine;
	private final ScriptProvider scriptProvider;

	public ScriptShell(ScriptProvider scriptProvider, Map<String, ?> globals) throws ScriptException {
		this.engine = createScriptEngine();
		this.scriptProvider = scriptProvider;

		// setup bindings
		Bindings bindings = engine.createBindings();
		bindings.putAll(globals);

		// bind API objects
		// TODO remove
		bindings.put("_cli", new CmdlineOperations());
		bindings.put("_shell", this);

		// setup script context
		ScriptContext context = new SimpleScriptContext();
		context.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
		engine.setContext(context);

		// import additional functions into the shell environment
		// TODO remove
		engine.eval(new InputStreamReader(ExpressionFormat.class.getResourceAsStream("ExpressionFormat.lib.groovy")));
		engine.eval(new InputStreamReader(ScriptShell.class.getResourceAsStream("ScriptShell.lib.groovy")));
	}

	public ScriptEngine createScriptEngine() {
		ResourceBundle bundle = ResourceBundle.getBundle(ScriptShell.class.getName());

		CompilerConfiguration config = new CompilerConfiguration();
		config.setScriptBaseClass(bundle.getString("scriptBaseClass"));
		config.setRecompileGroovySource(false);
		config.setDebug(false);

		// default imports
		ImportCustomizer imports = new ImportCustomizer();
		imports.addStarImports(bundle.getString("starImport").split(", "));
		imports.addStaticStars(bundle.getString("starStaticImport").split(", "));
		config.addCompilationCustomizers(imports);

		GroovyClassLoader classLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), config);
		return new GroovyScriptEngineImpl(classLoader);
	}

	public Object evaluate(final String script, final Bindings bindings) throws Throwable {
		try {
			return engine.eval(script, bindings);
		} catch (Throwable e) {
			while (e.getClass() == ScriptException.class && e.getCause() != null) {
				e = e.getCause();
			}
			throw StackTraceUtils.deepSanitize(e); // make Groovy stacktrace human-readable
		}
	}

	public static interface ScriptProvider {

		public URI getScriptLocation(String input) throws Exception;

		public String fetchScript(URI uri) throws Exception;
	}

	public Object runScript(String input, Bindings bindings) throws Throwable {
		return runScript(scriptProvider.getScriptLocation(input), bindings);
	}

	public Object runScript(URI resource, Bindings bindings) throws Throwable {
		return evaluate(scriptProvider.fetchScript(resource), bindings);
	}

}
