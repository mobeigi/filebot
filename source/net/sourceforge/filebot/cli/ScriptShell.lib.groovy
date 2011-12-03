// File selector methods
import static groovy.io.FileType.*

File.metaClass.node = { path -> new File(delegate, path) }
File.metaClass.listFiles = { c -> delegate.isDirectory() ? delegate.listFiles().findAll(c) : []}

File.metaClass.isVideo = { _types.getFilter("video").accept(delegate) }
File.metaClass.isAudio = { _types.getFilter("audio").accept(delegate) }
File.metaClass.isSubtitle = { _types.getFilter("subtitle").accept(delegate) }
File.metaClass.isVerification = { _types.getFilter("verification").accept(delegate) }

File.metaClass.dir = { getParentFile() }
File.metaClass.hasFile = { c -> isDirectory() && listFiles().find(c) }

String.metaClass.getFiles = { c -> new File(delegate).getFiles(c) }
File.metaClass.getFiles = { c -> def files = []; traverse(type:FILES) { files += it }; return c ? files.findAll(c) : files }
List.metaClass.getFiles = { c -> findResults{ it.getFiles(c) }.flatten().unique() }

String.metaClass.getFolders = { c -> new File(delegate).getFolders(c) }
File.metaClass.getFolders = { c -> def folders = []; traverse(type:DIRECTORIES, visitRoot:true) { folders += it }; return c ? folders.findAll(c) : folders }
List.metaClass.getFolders = { c -> findResults{ it.getFolders(c) }.flatten().unique() }

String.metaClass.eachMediaFolder = { c -> new File(delegate).eachMediaFolder(c) }
File.metaClass.eachMediaFolder = { c -> getFolders().findAll{ it.hasFile{ it.isVideo() } }.each(c) }
List.metaClass.eachMediaFolder = { c -> getFolders().findAll{ it.hasFile{ it.isVideo() } }.each(c) }


// File utility methods
import static net.sourceforge.tuned.FileUtilities.*;

File.metaClass.getNameWithoutExtension = { getNameWithoutExtension(delegate.getName()) }
File.metaClass.getPathWithoutExtension = { new File(delegate.getParentFile(), getNameWithoutExtension(delegate.getName())).getPath() }
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
	def cmd = args.toList()
	if (isWindows()) {
		// normalize file separator for windows and run with cmd so any executable in PATH will just work
		cmd = ['cmd', '/c'] + cmd*.replace('/','\\')
	}
	
	if (!_args.trustScript) {
		_log.severe("Execute failed: Script is not trusted: " + cmd)
		return -1
	}
	
	// run command and print output
	def process = cmd.execute()
	process.waitForProcessOutput(System.out, System.err)
	
	return process.exitValue()
}


// WatchService helper
import net.sourceforge.filebot.cli.FolderWatchService;

def getWatchService(Closure callback, List folders) {
	// sanity check
	folders.find{ if (!it.isDirectory()) throw new Exception("Must be a folder: " + it) }
	
	// create watch service and setup callback
	def watchService = new FolderWatchService() {
		
		@Override
		def void processCommitSet(File[] fileset) {
			callback(fileset.toList())
		}
	}
	
	// collect updates for 5 minutes and then batch process
	watchService.setCommitDelay(5 * 60 * 1000)
	
	// start watching given files
	folders.each { watchService.watch(it) }
	
	return watchService
}

File.metaClass.watch = { c -> getWatchService(c, [delegate]) }
List.metaClass.watch = { c -> getWatchService(c, delegate) }



// CLI bindings
def rename(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.rename(_files(args), args.query, args.format, args.db, args.lang, args.strict) }
	}
}

def getSubtitles(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.getSubtitles(_files(args), args.query, args.lang, args.output, args.encoding, args.strict) }
	}
}

def getMissingSubtitles(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.getMissingSubtitles(_files(args), args.query, args.lang, args.output, args.encoding, args.strict) }
	}
}

def check(args) {
	synchronized (_cli) {
		_guarded { _cli.check(_files(args)) }
	}
}

def compute(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.compute(_files(args), args.output, args.encoding) }
	}
}

def fetchEpisodeList(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.fetchEpisodeList(args.query, args.format, args.db, args.lang) }
	}
}

def getMediaInfo(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.getMediaInfo(args.file, args.format) }
	}
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
this.metaClass._guarded = { c -> try { return c.call() } catch (e) { _log.severe(e.getMessage()); return null }}
