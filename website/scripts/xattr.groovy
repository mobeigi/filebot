// filebot -script fn:xattr --action clear /path/to/files

args.getFiles{ it.xattr.size() > 0 }.each{
	println it
	it.xattr.each{ k, v ->
		println "\t$k: $v"
	}
	// clear xattr mode
	if (_args.action == 'clear') {
		it.xattr.clear()
		println '*** CLEARED ***'
	}
}
