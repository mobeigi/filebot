def dirs = args.getFolders()

// watch folders and print files that were added/modified (requires Java 7)
dirs.watch { println "Batch: " + it } // default commit delay is 5 minutes
dirs.watch { println "Quick: " + it }.setCommitDelay(100) // 100 ms commit delay

println "Waiting for events"
console.readLine()
