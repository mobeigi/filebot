// filebot -script fn:revert <file or folder>


def accept(from, to) {
	args.find{ to.absolutePath.startsWith(it.absolutePath) } && to.exists()
}

def revert(from, to) {
	def action = net.sourceforge.filebot.StandardRenameAction.forName(_args.action)
	
	println "[$action] Revert [$from] to [$to]"
	if (!from.canonicalFile.equals(to.canonicalFile)) {
		action.rename(from, to) // reverse-rename only if path has changed
	}
	
	// reset extended attributes
	tryQuietly{ to.xattr.clear() }
}


getRenameLog(true).reverseEach { from, to ->
	if (accept(from, to))
		revert(to, from)
}
