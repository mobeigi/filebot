// filebot -script fn:replace --action copy --filter "[.]srt$" --def "e=[.](eng|english)" "r=.en"

// imports
import net.sourceforge.filebot.StandardRenameAction

// parameters
def action = StandardRenameAction.forName(_args.action)
def accept = { f -> _args.filter ? f.path =~ _args.filter : true }

// rename
args.getFiles{ accept(it)  }.each{
	if (it.path =~ e) {
		def nfile = new File(it.path.replaceAll(e, r))
		
		// override files only when --conflict override is set
		if (!it.equals(nfile)) {
			if (nfile.exists() && _args.conflict == 'override' && action != StandardRenameAction.TEST) {
				nfile.delete() // resolve conflict
			}
			
			if (!nfile.exists()) {
				println action.rename(it, nfile)
			} else {
				println "Skipped $nfile"
			}
		}
	}
}
