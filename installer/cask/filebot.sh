#!/bin/sh
APP_EXE=`readlink /usr/local/bin/filebot`
APP_ROOT=`dirname "$APP_EXE"`

# start filebot
/usr/libexec/java_home --failfast --version "@{jvm.version}+" --exec java @{java.application.options} -Dapplication.deployment=cask -Djava.awt.headless=true -Dapple.awt.UIElement=true -Djna.library.path="$APP_ROOT/lib" -Djava.library.path="$APP_ROOT/lib" -Dnet.filebot.AcoustID.fpcalc="$APP_ROOT/lib/fpcalc" $JAVA_OPTS -classpath "$APP_ROOT/*" @{main.class} "$@"
