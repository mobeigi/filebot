#!/bin/sh


# sanity checks
if [ -z "$HOME" ]; then
	echo "The environment variable 'HOME' must be set"
	exit 1
fi

# prepare filebot java call
APP_ROOT="/opt/filebot"
APP_DATA="$HOME/.config/FileBot"
APP_CACHE="$HOME/.cache/FileBot"

# use embedded JRE
JAVA_CMD="$APP_ROOT/jre/bin/java"

# add APP_ROOT to LD_LIBRARY_PATH
if [ ! -z "$LD_LIBRARY_PATH" ]
then
    export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$APP_ROOT"
else
    export LD_LIBRARY_PATH="$APP_ROOT"
fi

"$JAVA_CMD" $JAVA_OPTS -Dunixfs=false -DuseGVFS=true -DuseExtendedFileAttributes=true -DuseCreationDate=false -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Dsun.java2d.xrender=true -Djava.net.useSystemProxies=true -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Djna.nosys=true -Dapplication.update=skip -Dapplication.deployment=usc -Dnet.filebot.UserFiles.fileChooser=JavaFX "-Dapplication.dir=$APP_DATA" "-Dapplication.cache=$APP_CACHE/ehcache.disk.store" "-Djava.io.tmpdir=$APP_CACHE/java.io.tmpdir" "-Dnet.filebot.AcoustID.fpcalc=$APP_ROOT/fpcalc" -jar "$APP_ROOT/FileBot.jar" "$@"
