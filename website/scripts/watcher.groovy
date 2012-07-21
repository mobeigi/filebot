// filebot -script fn:watcher /path/to/folder/ --output /output/folder/ --format <expression>

// watch folders and print files that were added/modified
args.watch { changes ->
	rename(file:changes)
}

println "Waiting for events"
console.readLine() // keep running and watch for changes
