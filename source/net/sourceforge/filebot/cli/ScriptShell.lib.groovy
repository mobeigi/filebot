// File selector methods
import static groovy.io.FileType.*
import static groovy.io.FileVisitResult.*

// MediaDetection
import net.sourceforge.filebot.media.*


File.metaClass.resolve = { Object name -> new File(delegate, name.toString()) }
File.metaClass.getAt = { String name -> new File(delegate, name) }
File.metaClass.listFiles = { c -> delegate.isDirectory() ? delegate.listFiles().findAll(c) : []}

File.metaClass.isVideo = { _types.getFilter("video").accept(delegate) }
File.metaClass.isAudio = { _types.getFilter("audio").accept(delegate) }
File.metaClass.isSubtitle = { _types.getFilter("subtitle").accept(delegate) }
File.metaClass.isVerification = { _types.getFilter("verification").accept(delegate) }
File.metaClass.isArchive = { _types.getFilter("archive").accept(delegate) }
File.metaClass.isDisk = { (delegate.isDirectory() && MediaDetection.isDiskFolder(delegate)) || (delegate.isFile() && _types.getFilter("video/iso").accept(delegate) && MediaDetection.isVideoDiskFile(delegate)) }

File.metaClass.getDir = { getParentFile() }
File.metaClass.hasFile = { c -> isDirectory() && listFiles().find(c) }

String.metaClass.getFiles = { c -> new File(delegate).getFiles(c) }
File.metaClass.getFiles = { c -> if (delegate.isFile()) return [delegate]; def files = []; traverse(type:FILES, visitRoot:true) { files += it }; return c ? files.findAll(c).sort() : files.sort() }
List.metaClass.getFiles = { c -> findResults{ it.getFiles(c) }.flatten().unique() }

String.metaClass.getFolders = { c -> new File(delegate).getFolders(c) }
File.metaClass.getFolders = { c -> def folders = []; traverse(type:DIRECTORIES, visitRoot:true) { folders += it }; return c ? folders.findAll(c).sort() : folders.sort() }
List.metaClass.getFolders = { c -> findResults{ it.getFolders(c) }.flatten().unique() }
File.metaClass.listFolders = { c -> delegate.listFiles().findAll{ it.isDirectory() } }

File.metaClass.getMediaFolders = { def folders = []; traverse(type:DIRECTORIES, visitRoot:true, preDir:{ it.isDisk() ? SKIP_SUBTREE : CONTINUE }) { folders += it }; folders.findAll{ it.hasFile{ it.isVideo() } || it.isDisk() }.sort() }
String.metaClass.eachMediaFolder = { c -> new File(delegate).eachMediaFolder(c) }
File.metaClass.eachMediaFolder = { c -> delegate.getMediaFolders().each(c) }
List.metaClass.eachMediaFolder = { c -> delegate.findResults{ it.getMediaFolders() }.flatten().unique().each(c) }


// File utility methods
import static net.sourceforge.tuned.FileUtilities.*

File.metaClass.getNameWithoutExtension = { getNameWithoutExtension(delegate.getName()) }
File.metaClass.getPathWithoutExtension = { new File(delegate.getParentFile(), getNameWithoutExtension(delegate.getName())).getPath() }
File.metaClass.getExtension = { getExtension(delegate) }
File.metaClass.hasExtension = { String... ext -> hasExtension(delegate, ext) }
File.metaClass.isDerived = { f -> isDerived(delegate, f) }
File.metaClass.validateFileName = { validateFileName(delegate) }
File.metaClass.validateFilePath = { validateFilePath(delegate) }
File.metaClass.moveTo = { f -> moveRename(delegate, f as File) }
File.metaClass.copyTo = { dir -> copyAs(delegate, new File(dir, delegate.getName())) }
File.metaClass.getXattr = { new net.sourceforge.filebot.MetaAttributeView(delegate) }
File.metaClass.relativize = { f -> delegate.canonicalFile.toPath().relativize(f.canonicalFile.toPath()).toFile() }
List.metaClass.mapByFolder = { mapByFolder(delegate) }
List.metaClass.mapByExtension = { mapByExtension(delegate) }
String.metaClass.getNameWithoutExtension = { getNameWithoutExtension(delegate) }
String.metaClass.getExtension = { getExtension(delegate) }
String.metaClass.hasExtension = { String... ext -> hasExtension(delegate, ext) }
String.metaClass.validateFileName = { validateFileName(delegate) }

// helper for enforcing filename length limits, e.g. truncate filename but keep extension
String.metaClass.truncateFileName = { int limit = 255 -> def ext = getExtension(delegate); def name = getNameWithoutExtension(delegate); return name.substring(0, Math.min(limit - (ext ? 1+ext.length() : 0), name.length())) + (ext ? '.'+ext : '') }

// helper for simplifying strings
String.metaClass.normalizePunctuation = { net.sourceforge.filebot.similarity.Normalization.normalizePunctuation(delegate) }

// Parallel helper
import java.util.concurrent.*

def parallel(List closures, int threads = Runtime.getRuntime().availableProcessors()) {
	def tasks = closures.collect { it as Callable }
	return Executors.newFixedThreadPool(threads).invokeAll(tasks).collect{ c -> _guarded { c.get() } }
}


// Web and File IO helpers
import java.nio.ByteBuffer
import java.nio.charset.Charset
import static net.sourceforge.filebot.web.WebRequest.*

URL.metaClass.fetch = { fetch(delegate) }
ByteBuffer.metaClass.getText = { csn = "utf-8" -> Charset.forName(csn).decode(delegate.duplicate()).toString() }
ByteBuffer.metaClass.getHtml = { csn = "utf-8" -> new XmlParser(new org.cyberneko.html.parsers.SAXParser()).parseText(delegate.getText(csn)) }
String.metaClass.getHtml = { new XmlParser(new org.cyberneko.html.parsers.SAXParser()).parseText(delegate) }
String.metaClass.getXml = { new XmlParser().parseText(delegate) }

URL.metaClass.get = { delegate.getText() }
URL.metaClass.post = { Map parameters -> post(delegate.openConnection(), parameters) }
URL.metaClass.post = { byte[] data, contentType = 'application/octet-stream' -> post(delegate.openConnection(), data, contentType) }
URL.metaClass.post = { String text, contentType = 'text/plain', csn = 'utf-8' -> delegate.post(text.getBytes(csn), contentType) }

ByteBuffer.metaClass.saveAs = { f -> f = f as File; f = f.absoluteFile; f.parentFile.mkdirs(); writeFile(delegate.duplicate(), f); f }
URL.metaClass.saveAs = { f -> fetch(delegate).saveAs(f) }
String.metaClass.saveAs = { f, csn = "utf-8" -> Charset.forName(csn).encode(delegate).saveAs(f) }

def telnet(host, int port, csn = 'utf-8', Closure handler) {
	def socket = new Socket(host, port)
	try {
		handler.call(new PrintStream(socket.outputStream, true, csn), socket.inputStream.newReader(csn))
	} finally {
		socket.close()
	}
}


// json-io helpers
import com.cedarsoftware.util.io.*

Object.metaClass.objectToJson = { JsonWriter.objectToJson(delegate) }
String.metaClass.jsonToObject = { JsonReader.jsonToJava(delegate) }
String.metaClass.jsonToMap = { JsonReader.jsonToMaps(delegate) }


// Template Engine helpers
import groovy.text.XmlTemplateEngine
import groovy.text.GStringTemplateEngine
import net.sourceforge.filebot.format.PropertyBindings
import net.sourceforge.filebot.format.UndefinedObject

Object.metaClass.applyXml = { template -> new XmlTemplateEngine("\t", false).createTemplate(template).make(new PropertyBindings(delegate, new UndefinedObject(""))).toString() }
Object.metaClass.applyText = { template -> new GStringTemplateEngine().createTemplate(template).make(new PropertyBindings(delegate, new UndefinedObject(""))).toString() }


// MarkupBuilder helper
import groovy.xml.MarkupBuilder

def XML(bc) {
	def out = new StringWriter()
	def xmb = new MarkupBuilder(out)
	xmb.omitNullAttributes = true
	xmb.omitEmptyAttributes = true
	xmb.expandEmptyElements= true
	bc.rehydrate(bc.delegate, xmb, xmb).call() // call closure in MarkupBuilder context
	return out.toString()
}


// Shell helper
import com.sun.jna.Platform

def execute(Object... args) {
	def cmd = (args as List).flatten().collect{ it as String }
	
	if (Platform.isWindows()) {
		// normalize file separator for windows and run with cmd so any executable in PATH will just work
		cmd = ['cmd', '/c'] + cmd
	} else if (cmd.size() == 1) {
		// make unix shell parse arguments
		cmd = ['sh', '-c'] + cmd
	}
	
	// run command and print output
	def process = cmd.execute()
	process.waitForProcessOutput(System.out, System.err)
	
	return process.exitValue()
}


// WatchService helper
import net.sourceforge.filebot.cli.FolderWatchService

def createWatchService(Closure callback, List folders, boolean watchTree) {
	// sanity check
	folders.find{ if (!it.isDirectory()) throw new Exception("Must be a folder: " + it) }
	
	// create watch service and setup callback
	def watchService = new FolderWatchService(true) {
		
		@Override
		def void processCommitSet(File[] fileset, File dir) {
			callback(fileset.toList())
		}
	}
	
	// collect updates for 500 ms and then batch process
	watchService.setCommitDelay(500)
	watchService.setCommitPerFolder(watchTree)
	
	// start watching given files
	folders.each { dir -> _guarded { watchService.watchFolder(dir) } }
	
	return watchService
}

File.metaClass.watch = { c -> createWatchService(c, [delegate], true) }
List.metaClass.watch = { c -> createWatchService(c, delegate, true) }


// FileBot MetaAttributes helpers
import net.sourceforge.filebot.media.*
import net.sourceforge.filebot.format.*
import net.sourceforge.filebot.web.*

File.metaClass.getMetadata = { net.sourceforge.filebot.Settings.useExtendedFileAttributes() ? new MetaAttributes(delegate) : null }
File.metaClass.getMediaBinding = { new MediaBindingBean(delegate.metadata, delegate, null) }
Movie.metaClass.getMediaBinding = Episode.metaClass.getMediaBinding = { new MediaBindingBean(delegate, null, null) }


// Complete or session rename history
def getRenameLog(complete = false) {
	def spooler = net.sourceforge.filebot.HistorySpooler.getInstance()
	def history = complete ? spooler.completeHistory : spooler.sessionHistory
	return history.sequences*.elements.flatten().collectEntries{ [new File(it.dir, it.from), new File(it.to).isAbsolute() ? new File(it.to) : new File(it.dir, it.to)] }
}


// Season / Episode helpers
import net.sourceforge.filebot.similarity.*

def stripReleaseInfo(name, strict = true) {
	def result = MediaDetection.stripReleaseInfo([name], strict)
	return result.size() > 0 ? result[0] : null
}

def isEpisode(path, strict = true) {
	def input = path instanceof File ? path.name : path.toString()
	return MediaDetection.isEpisode(input, strict)
}

def guessMovieFolder(File path) {
	return MediaDetection.guessMovieFolder(path)
}

def parseEpisodeNumber(path, strict = true) {
	def input = path instanceof File ? path.name : path.toString()
	def sxe = MediaDetection.parseEpisodeNumber(input, strict)
	return sxe == null || sxe.isEmpty() ? null : sxe[0]
}

def parseDate(path) {
	def input = path instanceof File ? path.name : path.toString()
	return MediaDetection.parseDate(input)
}

def detectSeriesName(files, boolean useSeriesIndex = true, boolean useAnimeIndex = false, Locale locale = Locale.ENGLISH) {
	def names = MediaDetection.detectSeriesNames(files instanceof Collection ? files : [files as File], useSeriesIndex, useAnimeIndex, locale)
	return names == null || names.isEmpty() ? null : names.toList()[0]
}

def detectMovie(File file, strict = true, queryLookupService = TheMovieDB, hashLookupService = OpenSubtitles, locale = Locale.ENGLISH) {
	def movies = MediaDetection.matchMovieName(file.listPath(4, true).findResults{ it.name ?: null }, true, 0) ?: MediaDetection.detectMovie(file, hashLookupService, queryLookupService, locale, strict)
	return movies == null || movies.isEmpty() ? null : movies.toList()[0]
}

def matchMovie(String filename, strict = true, maxStartIndex = 0) {
	def movies = MediaDetection.matchMovieName([filename], strict, maxStartIndex)
	return movies == null || movies.isEmpty() ? null : movies.toList()[0]
}

def similarity(o1, o2) {
	return new NameSimilarityMetric().getSimilarity(o1, o2)
}

List.metaClass.sortBySimilarity = { prime, Closure toStringFunction = { obj -> obj.toString() } ->
	def simetric = new NameSimilarityMetric() 
	return delegate.sort{ a, b -> simetric.getSimilarity(toStringFunction(b), prime).compareTo(simetric.getSimilarity(toStringFunction(a), prime)) }
}


// call scripts
def executeScript(String input, Map bindings = [:], Object... args) {
	// apply parent script defines
	def parameters = new javax.script.SimpleBindings()
		
	// initialize default parameter
	parameters.putAll(_def)
	parameters.putAll(bindings)
	parameters.put('args', args.toList().flatten().findResults{ it as File })
	
	// run given script
	_shell.runScript(input, parameters)
}

def include(String input, Map bindings = [:], Object... args) {
	// run given script and catch exceptions
	_guarded { executeScript(input, bindings, args) }
}


// CLI bindings
def rename(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.rename(_files(args), _renameFunction(args.action), args.conflict as String, args.output as String, args.format as String, args.db as String, args.query as String, args.order as String, args.filter as String, args.lang as String, args.strict as Boolean) }
	}
}

def getSubtitles(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.getSubtitles(_files(args), args.db as String, args.query as String, args.lang as String, args.output as String, args.encoding as String, args.format as String, args.strict as Boolean) }
	}
}

def getMissingSubtitles(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.getMissingSubtitles(_files(args), args.db as String, args.query as String, args.lang as String, args.output as String, args.encoding as String, args.format as String, args.strict as Boolean) }
	}
}

def check(args) {
	synchronized (_cli) {
		_guarded { _cli.check(_files(args)) }
	}
}

def compute(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.compute(_files(args), args.output as String, args.encoding as String) }
	}
}

def extract(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.extract(_files(args), args.output as String, args.conflict as String, args.filter instanceof Closure ? args.filter as FileFilter : null, args.forceExtractAll != null ? args.forceExtractAll : false) }
	}
}

def fetchEpisodeList(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.fetchEpisodeList(args.query as String, args.format as String, args.db as String, args.order as String, args.lang as String) }
	}
}

def getMediaInfo(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.getMediaInfo(args.file as File, args.format as String) }
	}
}


/**
 * Resolve folders/files to lists of one or more files
 */
def _files(args) {
	def files = [];
	if (args.folder) {
		(args.folder as File).traverse(type:FILES, maxDepth:0) { files += it }
	}
	if (args.file) {
		if (args.file instanceof Iterable || args.file instanceof Object[]) {
			files += args.file as List
		} else {
			files += args.file as File
		}
	}
	
	// ignore invalid input
	return files.flatten().findResults{ it as File }
}


// allow Groovy to hook into rename interface
import net.sourceforge.filebot.*

def _renameFunction(fn) {
	if (fn instanceof String)
		return StandardRenameAction.forName(fn)
	if (fn instanceof Closure)
		return [rename:{ from, to -> def result = fn.call(from, to); result instanceof File ? result : to }, toString:{'CLOSURE'}] as RenameAction
		
	return fn as RenameAction
}


/**
 * Fill in default values from cmdline arguments
 */
def _defaults(args) {
		['action', 'conflict', 'query', 'filter', 'format', 'db', 'order', 'lang', 'output', 'encoding'].each{ k ->
				args[k] = args.containsKey(k) ? args[k] : _args[k]
		}
		args.strict = args.strict != null ? args.strict : !_args.nonStrict // invert strict/non-strict
		return args
}

/**
 * Catch and log exceptions thrown by the closure
 */
def _guarded(c) {
	try {
		return c.call() 
	} catch (Throwable e) {
		_log.severe("${e.class.simpleName}: ${e.message}")
		return null
	}
}

/**
 * Same as the above but without logging anything
 */
def tryQuietly(c) {
	try {
		return c.call() 
	} catch (Throwable e) {
		return null
	}
}

/**
 * Retry given closure until it returns successfully (indefinitely by default)
 */
def retry(n = -1, wait = 0, quiet = false, c) {
	for(int i = 0; n < 0 || i <= n; i++) {
		try {
			return c.call()
		} catch(Throwable e) {
			if (i >= 0 && i >= n) {
				throw e
			} else if (!quiet) {
				_log.warning("retry $i: ${e.class.simpleName}: ${e.message}")
			}
			sleep(wait)
		}
	}
}
