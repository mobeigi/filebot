package net.sourceforge.filebot.cli;

import static net.sourceforge.filebot.cli.CLILogging.*;

import java.awt.AWTPermission;
import java.io.File;
import java.io.FilePermission;
import java.io.InputStreamReader;
import java.lang.management.ManagementPermission;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.net.URI;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permissions;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PropertyPermission;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.format.AssociativeScriptObject;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.format.PrivilegedInvocation;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.MovieIdentificationService;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.codehaus.groovy.runtime.StackTraceUtils;

public class ScriptShell {

	private final ScriptEngine engine = new GroovyScriptEngineFactory().getScriptEngine();

	private final ScriptProvider scriptProvider;

	public ScriptShell(CmdlineInterface cli, ArgumentBean args, AccessControlContext acc, ScriptProvider scriptProvider) throws ScriptException {
		this.scriptProvider = scriptProvider;

		// setup script context
		ScriptContext context = new SimpleScriptContext();
		context.setBindings(initializeBindings(cli, args, acc), ScriptContext.GLOBAL_SCOPE);
		engine.setContext(context);

		// import additional functions into the shell environment
		engine.eval(new InputStreamReader(ExpressionFormat.class.getResourceAsStream("ExpressionFormat.lib.groovy")));
		engine.eval(new InputStreamReader(ScriptShell.class.getResourceAsStream("ScriptShell.lib.groovy")));
	}

	public static interface ScriptProvider {

		public URI getScriptLocation(String input) throws Exception;

		public Script fetchScript(URI uri) throws Exception;
	}

	public static class Script {

		public final String code;
		public final boolean trusted;

		public Script(String code, boolean trusted) {
			this.code = code;
			this.trusted = trusted;
		}
	}

	public Object runScript(String input, Bindings bindings) throws Throwable {
		return runScript(scriptProvider.getScriptLocation(input), bindings);
	}

	public Object runScript(URI resource, Bindings bindings) throws Throwable {
		Script script = scriptProvider.fetchScript(resource);
		return evaluate(script.code, bindings, script.trusted);
	}

	public Object evaluate(final String script, final Bindings bindings, boolean trustScript) throws Throwable {
		try {
			if (trustScript) {
				return engine.eval(script, bindings);
			}

			try {
				return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {

					@Override
					public Object run() throws ScriptException {
						return engine.eval(script, bindings);
					}
				}, getSandboxAccessControlContext());
			} catch (PrivilegedActionException e) {
				throw e.getException();
			}
		} catch (Throwable e) {
			while (e.getClass() == ScriptException.class && e.getCause() != null) {
				e = e.getCause();
			}
			throw StackTraceUtils.deepSanitize(e); // make Groovy stack human-readable
		}
	}

	protected Bindings initializeBindings(CmdlineInterface cli, ArgumentBean args, AccessControlContext acc) {
		Bindings bindings = new SimpleBindings();

		// bind external parameters
		if (args.bindings != null) {
			for (Entry<String, String> it : args.bindings) {
				bindings.put(it.getKey(), it.getValue());
			}
		}

		// bind API objects
		bindings.put("_cli", PrivilegedInvocation.newProxy(CmdlineInterface.class, cli, acc));
		bindings.put("_script", args.script);
		bindings.put("_args", args);
		bindings.put("_shell", this);

		Map<String, String> defines = new LinkedHashMap<String, String>();
		if (args.bindings != null) {
			for (Entry<String, String> it : args.bindings) {
				defines.put(it.getKey(), it.getValue());
			}
		}
		bindings.put("_def", defines);

		bindings.put("_types", MediaTypes.getDefault());
		bindings.put("_log", CLILogger);

		// bind Java properties and environment variables
		bindings.put("_system", new AssociativeScriptObject(System.getProperties()));
		bindings.put("_environment", new AssociativeScriptObject(System.getenv()));

		// bind console object
		bindings.put("console", System.console());

		// bind Episode data providers
		for (EpisodeListProvider service : WebServices.getEpisodeListProviders()) {
			bindings.put(service.getName(), service);
		}

		// bind Movie data providers
		for (MovieIdentificationService service : WebServices.getMovieIdentificationServices()) {
			bindings.put(service.getName(), service);
		}

		return bindings;
	}

	protected AccessControlContext getSandboxAccessControlContext() {
		Permissions permissions = new Permissions();

		permissions.add(new RuntimePermission("createClassLoader"));
		permissions.add(new RuntimePermission("accessClassInPackage.*"));
		permissions.add(new RuntimePermission("modifyThread"));
		permissions.add(new FilePermission("<<ALL FILES>>", "read"));
		permissions.add(new SocketPermission("*", "connect"));
		permissions.add(new PropertyPermission("*", "read"));
		permissions.add(new RuntimePermission("getenv.*"));
		permissions.add(new RuntimePermission("getFileSystemAttributes"));
		permissions.add(new ManagementPermission("monitor"));

		// write permissions for temp and cache folders
		permissions.add(new FilePermission(new File(System.getProperty("ehcache.disk.store.dir")).getAbsolutePath() + File.separator + "-", "write, delete"));
		permissions.add(new FilePermission(new File(System.getProperty("java.io.tmpdir")).getAbsolutePath() + File.separator + "-", "write, delete"));

		// AWT / Swing permissions
		permissions.add(new AWTPermission("accessEventQueue"));
		permissions.add(new AWTPermission("toolkitModality"));
		permissions.add(new AWTPermission("showWindowWithoutWarningBanner"));

		// this is probably a security problem but nevermind
		permissions.add(new RuntimePermission("accessDeclaredMembers"));
		permissions.add(new ReflectPermission("suppressAccessChecks"));
		permissions.add(new RuntimePermission("modifyThread"));

		return new AccessControlContext(new ProtectionDomain[] { new ProtectionDomain(null, permissions) });
	}

}
