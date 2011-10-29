
package net.sourceforge.filebot.cli;


import static net.sourceforge.filebot.cli.CLILogging.*;

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
import net.sourceforge.filebot.format.PrivilegedInvocation;


public class ScriptShell {
	
	private final ScriptEngine engine = new GroovyScriptEngineFactory().getScriptEngine();;
	

	public ScriptShell(CmdlineInterface cli, ArgumentBean defaults, AccessControlContext acc) throws ScriptException {
		Bindings bindings = new SimpleBindings();
		bindings.put("_cli", PrivilegedInvocation.newProxy(CmdlineInterface.class, cli, acc));
		bindings.put("_args", defaults);
		bindings.put("_types", MediaTypes.getDefault());
		bindings.put("_log", CLILogger);
		
		ScriptContext context = new SimpleScriptContext();
		context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
		engine.setContext(context);
		
		// import additional functions into the shell environment
		engine.eval(new InputStreamReader(ScriptShell.class.getResourceAsStream("ScriptShell.lib.groovy")));
	}
	

	public Object evaluate(final String script, final Bindings bindings) throws Exception {
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
