import static groovy.io.FileType.*
import static net.sourceforge.tuned.FileUtilities.*;


File.metaClass.plus = { path -> new File(delegate, path) }

File.metaClass.isVideo = { _types.getFilter("video").accept(delegate) }
File.metaClass.isAudio = { _types.getFilter("audio").accept(delegate) }
File.metaClass.isSubtitle = { _types.getFilter("subtitle").accept(delegate) }
File.metaClass.isVerification = { _types.getFilter("verification").accept(delegate) }

File.metaClass.isDerived = { file -> isDerived(delegate, file) }
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


def require(cond) { if (!cond()) throw new Exception('Require failed') }


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
