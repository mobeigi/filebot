def dir = 'E:/Downloads'

// extract files from archives and delete archives afterwards
extract(folder:dir, output:dir) && dir.listFiles{ it =~ /rar$/ }*.delete()

getMissingSubtitles(folder:dir, lang:'de', strict:false)
getMissingSubtitles(folder:dir, lang:'en', strict:false)

// rename each file individually in strict mode
dir.listFiles().each {
	rename(file:it, format:"E:/Series/{n}/{n.space('.')}.{s00e00}.{t.space('.')}", strict:true)
}
