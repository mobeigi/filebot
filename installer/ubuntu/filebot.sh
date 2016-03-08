#!/bin/sh
APP_ROOT="/opt/filebot"

if [ -z "$HOME" ]; then
	echo '$HOME must be set'
	exit 1
fi

# add APP_ROOT to LD_LIBRARY_PATH
if [ ! -z "$LD_LIBRARY_PATH" ]; then
	export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$APP_ROOT"
else
	export LD_LIBRARY_PATH="$APP_ROOT"
fi

APP_DATA="$HOME/.config/FileBot"
APP_CACHE="$HOME/.cache/FileBot"

# use embedded JRE
JAVA_CMD="$APP_ROOT/jre/bin/java"

# start filebot
"$JAVA_CMD" -Dunixfs=false -DuseGVFS=true -DuseExtendedFileAttributes=true -DuseCreationDate=false -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Dsun.java2d.xrender=true -Djava.net.useSystemProxies=true -Djna.nosys=true -Dapplication.update=skip -Dapplication.deployment=usc -Dnet.filebot.UserFiles.fileChooser=JavaFX "-Dapplication.dir=$APP_DATA" "-Dapplication.cache=$APP_CACHE/ehcache.disk.store" "-Djava.io.tmpdir=$APP_CACHE/java.io.tmpdir" "-Dnet.filebot.AcoustID.fpcalc=$APP_ROOT/fpcalc" $JAVA_OPTS -jar "$APP_ROOT/FileBot.jar" "$@"
