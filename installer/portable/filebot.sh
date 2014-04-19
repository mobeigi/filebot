#!/bin/bash
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
dir_bin="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# force JVM language and encoding settings
export LANG=en_US.utf8

java -Dunixfs=false -DuseGVFS=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Dfile.encoding=UTF-8 -Djava.net.useSystemProxies=false -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Djna.nosys=true -Dapplication.deployment=portable -Dapplication.analytics=true -Dapplication.warmup=false -Dnet.filebot.AcoustID.fpcalc=fpcalc "-Dapplication.dir=$dir_bin" "-Djava.io.tmpdir=$dir_bin/temp" "-Duser.home=$dir_bin" "-Djna.library.path=$dir_bin" "-Djava.library.path=$dir_bin" -Djava.util.prefs.PreferencesFactory=net.filebot.util.prefs.FilePreferencesFactory "-Dnet.filebot.util.prefs.file=$dir_bin/prefs.properties" -jar "$dir_bin/FileBot.jar" "$@"
