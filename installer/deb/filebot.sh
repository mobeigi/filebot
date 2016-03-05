#!/bin/sh

if [ ! -w "$HOME" ]; then
	echo '$HOME must be set and writable'
	exit 1
fi

# prepare filebot java call
APP_ROOT="/usr/share/filebot"
APP_DATA="$HOME/.filebot"

# add APP_ROOT to LD_LIBRARY_PATH
if [ ! -z "$LD_LIBRARY_PATH" ]
then
    export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$APP_ROOT"
else
    export LD_LIBRARY_PATH="$APP_ROOT"
fi

java $JAVA_OPTS -Dunixfs=false -DuseGVFS=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Djava.net.useSystemProxies=false -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Djna.nosys=true -Dapplication.deployment=deb "-Dapplication.dir=$APP_DATA" "-Djava.io.tmpdir=$APP_DATA/temp" "-Dnet.filebot.AcoustID.fpcalc=$APP_ROOT/fpcalc" -jar "$APP_ROOT/FileBot.jar" "$@"
