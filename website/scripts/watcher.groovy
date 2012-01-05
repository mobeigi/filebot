// filebot -script "http://filebot.sf.net/scripts/watcher.groovy" --format <expression> <folder>

// SANITY CHECK
if (_prop['java.runtime.version'] < '1.7') throw new Exception('Java 7 required')
if (!(new File(_args.format ?: '').absolute)) throw new Exception('Absolute target path format required')


// watch folders and print files that were added/modified (requires Java 7)
def watchman = args.watch { changes ->
   println "Processing $changes"
   rename(file:changes)
}

// process after 5 minutes without any changes to the folder
watchman.setCommitDelay(5 * 60 * 1000)

println "Waiting for events"
console.readLine() // keep running and watch for changes
