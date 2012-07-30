// File selector methods
import static groovy.io.FileType.*

File.metaClass.resolve = { Object name -> new File(delegate, name.toString()) }
File.metaClass.getAt = { String name -> new File(delegate, name) }
File.metaClass.listFiles = { c -> delegate.isDirectory() ? delegate.listFiles().findAll(c) : []}

File.metaClass.isVideo = { _types.getFilter("video").accept(delegate) }
File.metaClass.isAudio = { _types.getFilter("audio").accept(delegate) }
File.metaClass.isSubtitle = { _types.getFilter("subtitle").accept(delegate) }
File.metaClass.isVerification = { _types.getFilter("verification").accept(delegate) }
File.metaClass.isArchive = { _types.getFilter("archive").accept(delegate) }

File.metaClass.getDir = { getParentFile() }
File.metaClass.hasFile = { c -> isDirectory() && listFiles().find(c) }

String.metaClass.getFiles = { c -> new File(delegate).getFiles(c) }
File.metaClass.getFiles = { c -> if (delegate.isFile()) return [delegate]; def files = []; traverse(type:FILES, visitRoot:true) { files += it }; return c ? files.findAll(c).sort() : files.sort() }
List.metaClass.getFiles = { c -> findResults{ it.getFiles(c) }.flatten().unique() }

String.metaClass.getFolders = { c -> new File(delegate).getFolders(c) }
File.metaClass.getFolders = { c -> def folders = []; traverse(type:DIRECTORIES, visitRoot:true) { folders += it }; return c ? folders.findAll(c).sort() : folders.sort() }
List.metaClass.getFolders = { c -> findResults{ it.getFolders(c) }.flatten().unique() }

String.metaClass.eachMediaFolder = { c -> new File(delegate).eachMediaFolder(c) }
File.metaClass.eachMediaFolder = { c -> getFolders{ it.hasFile{ it.isVideo() } }.each(c) }
List.metaClass.eachMediaFolder = { c -> getFolders{ it.hasFile{ it.isVideo() } }.each(c) }


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
List.metaClass.mapByFolder = { mapByFolder(delegate) }
List.metaClass.mapByExtension = { mapByExtension(delegate) }
String.metaClass.getExtension = { getExtension(delegate) }
String.metaClass.hasExtension = { String... ext -> hasExtension(delegate, ext) }
String.metaClass.validateFileName = { validateFileName(delegate) }
String.metaClass.validateFilePath = { validateFilePath(delegate) }


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

URL.metaClass.getText = { readAll(getReader(delegate.openConnection())) }
URL.metaClass.getHtml = { new XmlParser(new org.cyberneko.html.parsers.SAXParser()).parseText(delegate.getText()) }
URL.metaClass.getXml = { new XmlParser().parseText(delegate.getText()) }
URL.metaClass.fetch = { fetch(delegate) }
ByteBuffer.metaClass.getText = { csn = "utf-8" -> Charset.forName(csn).decode(delegate.duplicate()).toString() }
ByteBuffer.metaClass.getHtml = { csn = "utf-8" -> new XmlParser(new org.cyberneko.html.parsers.SAXParser()).parseText(delegate.getText(csn)) }
String.metaClass.getHtml = { new XmlParser(new org.cyberneko.html.parsers.SAXParser()).parseText(delegate) }
String.metaClass.getXml = { new XmlParser().parseText(delegate) }

URL.metaClass.get = { delegate.getText() }
URL.metaClass.post = { Map parameters -> post(delegate.openConnection(), parameters) }
URL.metaClass.post = { byte[] data, contentType = 'application/octet-stream' -> post(delegate.openConnection(), data, contentType) }
URL.metaClass.post = { String text, csn = 'utf-8' -> delegate.post(text.getBytes(csn), 'text/plain') }

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
	bc.rehydrate(bc.delegate, xmb, xmb).call() // call closure in MarkupBuilder context
	return out.toString()
}


// Shell helper
import static com.sun.jna.Platform.*

def execute(Object... args) {
	def cmd = args.toList()*.toString()
	if (isWindows()) {
		// normalize file separator for windows and run with cmd so any executable in PATH will just work
		cmd = ['cmd', '/c'] + cmd
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


// Complete or session rename history
def getRenameLog(complete = false) {
	def spooler = net.sourceforge.filebot.HistorySpooler.getInstance()
	def history = complete ? spooler.completeHistory : spooler.sessionHistory
	return history.sequences*.elements.flatten().collectEntries{ [new File(it.dir, it.from), new File(it.to).isAbsolute() ? new File(it.to) : new File(it.dir, it.to)] }
}

// Season / Episode helpers
import net.sourceforge.filebot.media.*
import net.sourceforge.filebot.similarity.*

def stripReleaseInfo(name, strict = true) {
	return MediaDetection.stripReleaseInfo([name], strict)[0]
}

def isEpisode(path, strict = true) {
	def input = path instanceof File ? path.name : path.toString()
	return MediaDetection.isEpisode(input, strict)
}

def guessMovieFolder(path) {
	return MediaDetection.guessMovieFolder(path as File)
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

def detectSeriesName(files, locale = Locale.ENGLISH) {
	def names = MediaDetection.detectSeriesNames(files instanceof Collection ? files : [files as File], locale)
	return names == null || names.isEmpty() ? null : names.toList()[0]
}

def detectMovie(movieFile, strict = true, locale = Locale.ENGLISH, hashLookupService = OpenSubtitles, queryLookupService = TheMovieDB) {
	def movies = MediaDetection.detectMovie(movieFile, hashLookupService, queryLookupService, locale, strict)
	return movies == null || movies.isEmpty() ? null : movies.toList()[0]
}

def matchMovie(movieFile, strict = false) { // same as detectMovie() using only the local movie index making it VERY FAST
	return detectMovie(movieFile, strict, Locale.ENGLISH, null, null)
}


def similarity(o1, o2) {
	return new NameSimilarityMetric().getSimilarity(o1, o2)
}

List.metaClass.sortBySimilarity = { prime, Closure toStringFunction = { obj -> obj.toString() } ->
	return delegate.sort{ a, b -> similarity(toStringFunction(b), prime).compareTo(similarity(toStringFunction(a), prime)) }
}

// call scripts
def include(String input, Map bindings = [:], Object... args) {
	// initialize default parameter
	bindings.args = (args as List).flatten().findResults{ it as File }
	
	// run given script and catch exceptions
	_guarded { _shell.runScript(input, new javax.script.SimpleBindings(bindings)) }
}


// CLI bindings
def rename(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.rename(_files(args), _renameFunction(args.action), args.conflict as String, args.output as String, args.format as String, args.db as String, args.query as String, args.order as String, args.filter as String, args.lang as String, args.strict as Boolean) }
	}
}

def getSubtitles(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.getSubtitles(_files(args), args.db as String, args.query as String, args.lang as String, args.output as String, args.encoding as String, args.strict as Boolean) }
	}
}

def getMissingSubtitles(args) { args = _defaults(args)
	synchronized (_cli) {
		_guarded { _cli.getMissingSubtitles(_files(args), args.db as String, args.query as String, args.lang as String, args.output as String, args.encoding as String, args.strict as Boolean) }
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
		_guarded { _cli.extract(_files(args), args.output as String, args.conflict as String) }
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
		return [rename:fn as Closure, toString:{'CLOSURE'}] as RenameAction
		
	return fn as RenameAction
}


/**
 * Fill in default values from cmdline arguments
 */
def _defaults(args) {
		args.action      = args.action     ?: _args.action
		args.conflict    = args.conflict   ?: _args.conflict
		args.query       = args.query      ?: _args.query
		args.filter      = args.filter     ?: _args.filter
		args.format      = args.format     ?: _args.format
		args.db          = args.db         ?: _args.db
		args.order       = args.order      ?: _args.order
		args.lang        = args.lang       ?: _args.lang
		args.output      = args.output     ?: _args.output
		args.encoding    = args.encoding   ?: _args.encoding
		args.strict      = args.strict     != null ? args.strict : !_args.nonStrict
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
