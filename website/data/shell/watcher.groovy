// watch folders and print files that were added/modified (requires Java 7)
def watchman = args.getFolders().watch { changes ->
   println "Processing $changes"
   rename(file:changes, format:"/media/storage/files/tv/{n}{'/Season '+s}/{episode}")
}

// process after 10 minutes without any changes to the folder
watchman.setCommitDelay(10 * 60 * 1000)

println "Waiting for events"
console.readLine() // keep running and watch for changes
