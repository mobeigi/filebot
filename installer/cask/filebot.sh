#!/bin/sh
APP_EXE=`readlink /usr/local/bin/filebot`
APP_ROOT=`dirname "$APP_EXE"`

# start filebot
/usr/libexec/java_home --failfast --version "9+" --exec java -Dunixfs=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Djava.net.useSystemProxies=true -Dapple.awt.UIElement=true -Djna.nounpack=true -Dapplication.deployment=cask -Djna.library.path="$APP_ROOT/lib" -Djava.library.path="$APP_ROOT/lib" -Dnet.filebot.AcoustID.fpcalc="$APP_ROOT/lib/fpcalc" $JAVA_OPTS -jar "$APP_ROOT/FileBot.jar" "$@"
