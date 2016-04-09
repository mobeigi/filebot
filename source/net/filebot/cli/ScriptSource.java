package net.filebot.cli;

import static java.util.Arrays.*;
import static net.filebot.Settings.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.net.URI;
import java.time.Duration;

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.Resource;

public enum ScriptSource {

	GITHUB_STABLE {

		@Override
		public String accept(String input) {
			return input.startsWith("fn:") ? input.substring(3) : null;
		}

		@Override
		public ScriptProvider getScriptProvider(String input) throws Exception {
			URI resource = new URI(getApplicationProperty("github.stable"));
			Resource<byte[]> bundle = getCache().bytes(resource, URI::toURL).expire(Cache.ONE_WEEK);

			return new ScriptBundle(bundle, getClass().getResourceAsStream("repository.cer"));
		}

	},

	GITHUB_MASTER {

		@Override
		public String accept(String input) {
			return input.startsWith("dev:") ? input.substring(4) : null;
		}

		@Override
		public ScriptProvider getScriptProvider(String input) throws Exception {
			URI parent = new URI(getApplicationProperty("github.master"));

			return n -> getCache().text(n, s -> parent.resolve(s + ".groovy").toURL()).expire(Cache.ONE_DAY).get();
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
			URI parent = new URI(input).resolve(".");

			return n -> getCache().text(n, s -> parent.resolve(s + ".groovy").toURL()).expire(Duration.ZERO).get();
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

	public Cache getCache() {
		return Cache.getCache(name(), CacheType.Persistent);
	}

	public static ScriptSource findScriptProvider(String input) throws Exception {
		return stream(values()).filter(s -> s.accept(input) != null).findFirst().get();
	}

}
