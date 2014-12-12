#!/bin/bash
APP_ROOT="/opt/filebot"
JAVA_CMD="$APP_ROOT/jre/bin/java"

$JAVA_CMD -Dunixfs=false -DuseGVFS=true -DuseExtendedFileAttributes=true -DuseCreationDate=false -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Dsun.java2d.xrender=true -Djava.net.useSystemProxies=true -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Djna.nosys=true -Dapplication.update=skip -Dapplication.deployment=usc -Dnet.filebot.UserFiles.fileChooser=JavaFX "-Dapplication.dir=$HOME/.config/FileBot" "-Dapplication.cache=$HOME/.cache/FileBot/ehcache.disk.store" "-Djava.io.tmpdir=$HOME/.cache/FileBot/java.io.tmpdir" "-Djna.library.path=$APP_ROOT" "-Djava.library.path=$APP_ROOT" "-Dnet.filebot.AcoustID.fpcalc=$APP_ROOT/fpcalc" -jar "$APP_ROOT/FileBot.jar" "$@"
