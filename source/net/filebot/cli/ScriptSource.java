package net.filebot.cli;

import static java.util.Arrays.*;
import static net.filebot.Settings.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

import net.filebot.Cache;
import net.filebot.CacheType;

public enum ScriptSource {

	GITHUB_STABLE {

		@Override
		public String accept(String input) {
			return input.startsWith("fn:") ? input.substring(3) : null;
		}

		@Override
		public ScriptProvider getScriptProvider(String input) throws Exception {
			return getScriptBundle(this, "github.stable", Cache.ONE_WEEK);
		}

	},

	GITHUB_MASTER {

		@Override
		public String accept(String input) {
			return input.startsWith("dev:") ? input.substring(4) : null;
		}

		@Override
		public ScriptProvider getScriptProvider(String input) throws Exception {
			return getScriptBundle(this, "github.master", Cache.ONE_DAY);
		}

	},

	INLINE_GROOVY {

		@Override
		public String accept(String input) {
			return input.startsWith("g:") ? input.substring(2) : null;
		}

		@Override
		public ScriptProvider getScriptProvider(String input) throws Exception {
			return g -> g;
		}

	},

	REMOTE_URL {

		@Override
		public String accept(String input) {
			try {
				URI uri = new URI(input);
				if (uri.isAbsolute()) {
					return getName(new File(uri.getPath()));
				}
			} catch (Exception e) {
			}
			return null;
		}

		@Override
		public ScriptProvider getScriptProvider(String input) throws Exception {
			Cache cache = Cache.getCache(name(), CacheType.Persistent);
			URI parent = new URI(input).resolve(".");

			return f -> cache.text(f, s -> parent.resolve(s + ".groovy").toURL()).expire(Duration.ZERO).get();
		}

	},

	LOCAL_FILE {

		@Override
		public String accept(String input) {
			try {
				return getName(new File(input).getCanonicalFile());
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public ScriptProvider getScriptProvider(String input) throws Exception {
			File base = new File(input).getParentFile();

			return f -> readTextFile(new File(base, f + ".groovy"));
		}

	};

	public abstract String accept(String input);

	public abstract ScriptProvider getScriptProvider(String input) throws Exception;

	public static ScriptSource findScriptProvider(String input) throws Exception {
		return stream(values()).filter(s -> s.accept(input) != null).findFirst().get();
	}

	private static ScriptProvider getScriptBundle(ScriptSource source, String branch, Duration expirationTime) throws Exception {
		Cache cache = Cache.getCache(source.name(), CacheType.Persistent);
		byte[] bytes = cache.bytes("repository.jar", f -> new URL(getApplicationProperty(branch) + f)).expire(expirationTime).get();

		return new ScriptBundle(bytes, source.getClass().getResourceAsStream("repository.cer"));
	}

}
