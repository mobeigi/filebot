
package net.sourceforge.filebot.cli;


import static net.sourceforge.filebot.cli.CLILogging.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilePermission;
import java.io.InputStreamReader;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permissions;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.PropertyPermission;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.codehaus.groovy.runtime.StackTraceUtils;

import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.format.AssociativeScriptObject;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.format.PrivilegedInvocation;
import net.sourceforge.filebot.web.CachedResource;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.MovieIdentificationService;


class ScriptShell {
	
	private final ScriptEngine engine = new GroovyScriptEngineFactory().getScriptEngine();
	private final boolean trustScript;
	
	
	public ScriptShell(CmdlineInterface cli, ArgumentBean args, boolean trustScript, AccessControlContext acc) throws ScriptException {
		this.trustScript = trustScript;
		
		// setup script context
		ScriptContext context = new SimpleScriptContext();
		context.setBindings(initializeBindings(cli, args, acc), ScriptContext.GLOBAL_SCOPE);
		engine.setContext(context);
		
		// import additional functions into the shell environment
		engine.eval(new InputStreamReader(ExpressionFormat.class.getResourceAsStream("ExpressionFormat.lib.groovy")));
		engine.eval(new InputStreamReader(ScriptShell.class.getResourceAsStream("ScriptShell.lib.groovy")));
	}
	
	
	protected Bindings initializeBindings(CmdlineInterface cli, ArgumentBean args, AccessControlContext acc) {
		Bindings bindings = new SimpleBindings();
		
		// bind API objects
		bindings.put("_cli", PrivilegedInvocation.newProxy(CmdlineInterface.class, cli, acc));
		bindings.put("_script", new File(args.script));
		bindings.put("_args", args);
		
		bindings.put("_types", MediaTypes.getDefault());
		bindings.put("_log", CLILogger);
		
		// bind Java properties and environment variables
		bindings.put("_prop", new AssociativeScriptObject(System.getProperties()));
		bindings.put("_env", new AssociativeScriptObject(System.getenv()));
		
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
	
	
	public Object run(URL scriptLocation, Bindings bindings) throws Throwable {
		if (scriptLocation.getProtocol().equals("file")) {
			return run(new File(scriptLocation.toURI()), bindings);
		}
		
		// fetch remote script only if modified
		CachedResource<String> script = new CachedResource<String>(scriptLocation.toString(), String.class, 0) {
			
			@Override
			public String process(ByteBuffer data) {
				return Charset.forName("UTF-8").decode(data).toString();
			}
		};
		return evaluate(script.get(), bindings);
	}
	
	
	public Object run(File scriptFile, Bindings bindings) throws Throwable {
		String script = readAll(new InputStreamReader(new FileInputStream(scriptFile), "UTF-8"));
		return evaluate(script, bindings);
	}
	
	
	public Object evaluate(final String script, final Bindings bindings) throws Throwable {
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
			throw StackTraceUtils.deepSanitize(e); // make Groovy stack human-readable
		}
	}
	
	
	protected AccessControlContext getSandboxAccessControlContext() {
		Permissions permissions = new Permissions();
		
		permissions.add(new RuntimePermission("createClassLoader"));
		permissions.add(new FilePermission("<<ALL FILES>>", "read"));
		permissions.add(new SocketPermission("*", "connect"));
		permissions.add(new PropertyPermission("*", "read"));
		permissions.add(new RuntimePermission("getenv.*"));
		
		// write permissions for temp and cache folders
		permissions.add(new FilePermission(new File(System.getProperty("ehcache.disk.store.dir")).getAbsolutePath() + File.separator + "-", "write, delete"));
		permissions.add(new FilePermission(new File(System.getProperty("java.io.tmpdir")).getAbsolutePath() + File.separator + "-", "write, delete"));
		
		// this is probably a security problem but nevermind
		permissions.add(new RuntimePermission("accessDeclaredMembers"));
		permissions.add(new ReflectPermission("suppressAccessChecks"));
		permissions.add(new RuntimePermission("modifyThread"));
		
		return new AccessControlContext(new ProtectionDomain[] { new ProtectionDomain(null, permissions) });
	}
	
}
