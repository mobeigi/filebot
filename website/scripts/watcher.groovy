// EXPERIMENTAL // HERE THERE BE DRAGONS

// BEGIN SANITY CHECK
if (_prop['java.runtime.version'] < '1.7') throw new Exception('Java 7 required')
if (!(new File(_args.format ?: '').absolute)) throw new Exception('Absolute target path format required')
// END


// watch folders and print files that were added/modified (requires Java 7)
def watchman = args.watch { changes ->
   println "Processing $changes"
   rename(file:changes)
}

// process after 10 minutes without any changes to the folder
watchman.setCommitDelay(10 * 60 * 1000)

println "Waiting for events"
console.readLine() // keep running and watch for changes
