#!/bin/bash
export LANG="en_US.UTF-8"
export LC_ALL="en_US.UTF-8"

export JAVA_HOME="$SNAP/usr/lib/jvm/java-8-openjdk-$SNAP_ARCH"
export PATH="$JAVA_HOME/jre/bin:$PATH"
export LD_LIBRARY_PATH="$SNAP/usr/lib/filebot/$SNAP_ARCH:$LD_LIBRARY_PATH"

export APP_DATA="$SNAP_USER_DATA/data"
export APP_CACHE="$SNAP_USER_DATA/cache"
export APP_PREFS="$SNAP_USER_DATA/prefs"

java @{java.application.options} -Dapplication.deployment=snap -Dapplication.update=skip -Djava.library.path="$LD_LIBRARY_PATH" -Djna.library.path="$LD_LIBRARY_PATH" -Djna.boot.library.path="$SNAP/usr/lib/filebot/$SNAP_ARCH" -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Dnet.filebot.UserFiles.fileChooser=JavaFX -DuseGVFS=true -Dnet.filebot.gio.GVFS="/run/user/$(id -u)/gvfs" -Duser.home="$SNAP_USER_DATA" -Dapplication.dir="$APP_DATA" -Dapplication.cache="$APP_CACHE/ehcache.disk.store" -Djava.io.tmpdir="$APP_CACHE/java.io.tmpdir" -Djava.util.prefs.userRoot="$APP_PREFS/user" -Djava.util.prefs.systemRoot="$APP_PREFS/system" -Dnet.filebot.AcoustID.fpcalc="$SNAP/usr/bin/fpcalc" $JAVA_OPTS -classpath "$SNAP/usr/lib/filebot/java/*" @{main.class} "$@"
