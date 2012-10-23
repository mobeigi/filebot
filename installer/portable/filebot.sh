#!/bin/bash
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
dir_bin="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

java -Xmx256m -Dunixfs=false -DuseExtendedFileAttributes=true -Dapplication.deployment=portable "-Dapplication.dir=$dir_bin" "-Djava.io.tmpdir=$dir_bin/temp" "-Duser.home=$dir_bin" "-Djna.library.path=$dir_bin" "-Djava.library.path=$dir_bin" -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Djava.util.prefs.PreferencesFactory=net.sourceforge.tuned.prefs.FilePreferencesFactory -Dnet.sourceforge.tuned.prefs.file=prefs.properties -jar "$dir_bin/FileBot.jar" "$@"