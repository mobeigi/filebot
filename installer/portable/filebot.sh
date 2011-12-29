#!/bin/bash
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
dir_bin="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# WARNING: NOT TESTED / HERE THERE BE DRAGONS
java -Dapplication.deployment=portable -Dapplication.dir="$dir_bin" -Duser.home="$dir_bin" -Djava.io.tmpdir="$dir_bin/temp" -Djna.library.path="$dir_bin" -Djava.util.prefs.PreferencesFactory=net.sourceforge.tuned.prefs.FilePreferencesFactory -Dnet.sourceforge.tuned.prefs.file=prefs.properties -Xmx256m -jar "$dir_app/FileBot.jar" "$@"
