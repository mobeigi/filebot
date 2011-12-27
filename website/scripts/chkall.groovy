// filebot -script "http://filebot.sf.net/scripts/chkall.groovy" <folder>

/*
 * Check all sfv/md5/sha1 files and stop if a conflict is found
 */
args.getFiles().findAll { it.isVerification() }.each {
	if (!check(file:it))
		throw new Exception("*ERROR*")
}
