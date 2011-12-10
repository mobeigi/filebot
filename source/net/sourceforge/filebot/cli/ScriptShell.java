
package net.sourceforge.filebot.cli;


import static net.sourceforge.filebot.cli.CLILogging.*;

import java.io.File;
import java.io.FilePermission;
import java.io.InputStreamReader;
import java.net.SocketPermission;
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

import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.format.PrivilegedInvocation;
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
		bindings.put("_script", new File(args.script));
		bindings.put("_cli", PrivilegedInvocation.newProxy(CmdlineInterface.class, cli, acc));
		bindings.put("_args", args);
		bindings.put("_types", MediaTypes.getDefault());
		bindings.put("_log", CLILogger);
		
		// initialize web services
		for (EpisodeListProvider service : WebServices.getEpisodeListProviders()) {
			bindings.put(service.getName().toLowerCase(), PrivilegedInvocation.newProxy(EpisodeListProvider.class, service, acc));
		}
		for (MovieIdentificationService service : WebServices.getMovieIdentificationServices()) {
			bindings.put(service.getName().toLowerCase(), PrivilegedInvocation.newProxy(MovieIdentificationService.class, service, acc));
		}
		
		bindings.put("console", System.console());
		return bindings;
	}
	
	
	public Object evaluate(final String script, final Bindings bindings) throws Exception {
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
	}
	
	
	protected AccessControlContext getSandboxAccessControlContext() {
		Permissions permissions = new Permissions();
		
		permissions.add(new RuntimePermission("createClassLoader"));
		permissions.add(new RuntimePermission("accessDeclaredMembers"));
		permissions.add(new FilePermission("<<ALL FILES>>", "read"));
		permissions.add(new SocketPermission("*", "connect"));
		permissions.add(new PropertyPermission("*", "read"));
		permissions.add(new RuntimePermission("getenv.*"));
		
		return new AccessControlContext(new ProtectionDomain[] { new ProtectionDomain(null, permissions) });
	}
	
}
