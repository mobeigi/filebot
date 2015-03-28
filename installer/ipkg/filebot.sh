#!/bin/sh

APP_ROOT="/opt/share/filebot"
APP_DATA="$APP_ROOT/data/$USER"

# add APP_ROOT to LD_LIBRARY_PATH
if [ ! -z "$LD_LIBRARY_PATH" ]
then
    export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$APP_ROOT"
else
    export LD_LIBRARY_PATH="$APP_ROOT"
fi

# force JVM language and encoding settings
export LANG="en_US.UTF-8"
export LC_ALL="en_US.UTF-8"

java -Dunixfs=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Dfile.encoding=UTF-8 -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Dapplication.deployment=ipkg -Dnet.filebot.Archive.extractor=SevenZipExecutable "-Dnet.filebot.Archive.7z=7z" "-Dnet.filebot.AcoustID.fpcalc=fpcalc" "-Duser.home=$APP_DATA" "-Dapplication.dir=$APP_DATA" "-Djava.io.tmpdir=$APP_DATA/temp" -jar "$APP_ROOT/FileBot.jar" "$@"
