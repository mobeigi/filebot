// File selector methods
import static groovy.io.FileType.*


File.metaClass.plus = { path -> new File(delegate, path) }
File.metaClass.listFiles = { c -> delegate.isDirectory() ? delegate.listFiles().findAll(c) : []}

File.metaClass.isVideo = { _types.getFilter("video").accept(delegate) }
File.metaClass.isAudio = { _types.getFilter("audio").accept(delegate) }
File.metaClass.isSubtitle = { _types.getFilter("subtitle").accept(delegate) }
File.metaClass.isVerification = { _types.getFilter("verification").accept(delegate) }

File.metaClass.hasFile = { c -> isDirectory() && listFiles().find{ c.call(it) }}

File.metaClass.getFiles = { def files = []; traverse(type:FILES) { files += it }; return files }
String.metaClass.getFiles = { new File(delegate).getFiles() }
List.metaClass.getFiles = { findResults{ it.getFiles() }.flatten().unique() }

File.metaClass.getFolders = { def folders = []; traverse(type:DIRECTORIES, visitRoot:true) { folders += it }; return folders }
String.metaClass.getFolders = { new File(delegate).getFolders() }
List.metaClass.getFolders = { findResults{ it.getFolders() }.flatten().unique() }

File.metaClass.eachMediaFolder = { c -> getFolders().findAll{ it.hasFile{ it.isVideo() } }.each(c) }
String.metaClass.eachMediaFolder = { c -> new File(delegate).eachMediaFolder(c) }
List.metaClass.eachMediaFolder = { c -> getFolders().findAll{ it.hasFile{ it.isVideo() } }.each(c) }


// File utility methods
import static net.sourceforge.tuned.FileUtilities.*;

File.metaClass.getNameWithoutExtension = { getNameWithoutExtension(delegate.getName()) }
File.metaClass.getExtension = { getExtension(delegate) }
File.metaClass.hasExtension = { String... ext -> hasExtension(delegate, ext) }
File.metaClass.isDerived = { f -> isDerived(delegate, f) }
File.metaClass.validateFileName = { validateFileName(delegate) }
File.metaClass.validateFilePath = { validateFilePath(delegate) }
File.metaClass.moveTo = { f -> renameFile(delegate, f) }
List.metaClass.mapByFolder = { mapByFolder(delegate) }
List.metaClass.mapByExtension = { mapByExtension(delegate) }

// Shell helper
import static com.sun.jna.Platform.*;

def execute(String... args) {
	if (!_args.trustScript) {
		_log.severe("Execute failed: Script is not trusted");
		return -1
	}
		
	def cmd = args.toList()
	if (isWindows()) {
		cmd = ["cmd", "/c"] + cmd;
	}
	
	// run command and print output
	def process = cmd.execute()
	process.waitForProcessOutput(System.out, System.err)
	
	return process.exitValue()
}


// Script helper
def require(cond) { if (!cond()) throw new Exception('Require failed') }


// CLI bindings
def rename(args) { args = _defaults(args)
	_guarded { _cli.rename(_files(args), args.query, args.format, args.db, args.lang, args.strict) }
}

def getSubtitles(args) { args = _defaults(args)	
	_guarded { _cli.getSubtitles(_files(args), args.query, args.lang, args.output, args.encoding, args.strict) }
}

def getMissingSubtitles(args) { args = _defaults(args)	
	_guarded { _cli.getMissingSubtitles(_files(args), args.query, args.lang, args.output, args.encoding, args.strict) }
}

def check(args) {
	_guarded { _cli.check(_files(args)) }
}

def compute(args) { args = _defaults(args)
	_guarded { _cli.compute(_files(args), args.output, args.encoding) }
}

def fetchEpisodeList(args) { args = _defaults(args)
	_guarded { _cli.fetchEpisodeList(args.query, args.format, args.db, args.lang) }
}

def getMediaInfo(args) { args = _defaults(args)
	_guarded { _cli.getMediaInfo(args.file, args.format) }
}


/**
 * Resolve folders/files to lists of one or more files
 */
def _files(args) {
	def files = [];
	if (args.folder)
		args.folder.traverse(type:FILES, maxDepth:0) { files += it }
	if (args.file)
		files += args.file

	return files
}

/**
 * Fill in default values from cmdline arguments
 */
def _defaults(args) {
		args.query       = args.query      ?: _args.query
		args.format      = args.format     ?: _args.format
		args.db          = args.db         ?: _args.db
		args.lang        = args.lang       ?: _args.lang
		args.output      = args.output     ?: _args.output
		args.encoding    = args.encoding   ?: _args.encoding
		args.strict      = args.strict     ?: !_args.nonStrict
		return args
}

/**
 * Catch and log exceptions thrown by the closure
 */
this.metaClass._guarded = { c -> try { return c() } catch (e) { _log.severe(e.getMessage()); return null }}
