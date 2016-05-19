#!/bin/sh

export LC_ALL="en.utf-8"

export PATH="$SNAP/bin:$SNAP/usr/bin"
export LD_LIBRARY_PATH="$SNAP/lib:$SNAP/usr/lib:$SNAP/lib/x86_64-linux-gnu:$SNAP/usr/lib/x86_64-linux-gnu"

export LD_LIBRARY_PATH="$SNAP/usr/lib/x86_64-linux-gnu/mesa:$LD_LIBRARY_PATH"

export JAVA_HOME="$SNAP/usr/lib/jvm/java-8-openjdk-amd64"
export PATH="$JAVA_HOME/bin:$JAVA_HOME/jre/bin:$PATH"


export JRE_LIB="$SNAP/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/amd64"
export LD_LIBRARY_PATH="$JRE_LIB:$JRE_LIB/jli:$LD_LIBRARY_PATH"


export LD_LIBRARY_PATH="$SNAP/usr/lib/x86_64-linux-gnu/mesa:$SNAP/usr/lib/x86_64-linux-gnu/dri:$LD_LIBRARY_PATH"

export LD_LIBRARY_PATH="$SNAP/usr/lib/x86_64-linux-gnu/gtk-2.0/modules:$SNAP/usr/lib/x86_64-linux-gnu/gtk-2.0:$LD_LIBRARY_PATH"

export LD_LIBRARY_PATH="$SNAP/usr/lib/x86_64-linux-gnu:$SNAP/usr/lib/x86_64-linux-gnu/pulseaudio:$LD_LIBRARY_PATH"

export LD_LIBRARY_PATH="$SNAP_LIBRARY_PATH:$LD_LIBRARY_PATH"

export GDK_PIXBUF_MODULEDIR="$SNAP/usr/lib/x86_64-linux-gnu/gdk-pixbuf-2.0/2.10.0"
export GDK_PIXBUF_MODULE_FILE="$SNAP/usr/lib/x86_64-linux-gnu/gdk-pixbuf-2.0/2.10.0/loaders.cache"

export APP_ROOT="$SNAP/filebot"

export APP_DATA="$SNAP_USER_DATA/data"
export APP_CACHE="$SNAP_USER_DATA/cache"

export FONTCONFIG_FILE="$SNAP/fonts.conf"
export LIBGL_DEBUG=verbose
export G_MESSAGES_DEBUG=all

export GTK_MODULES="gail:atk-bridge:unity-gtk-module"
export GTK2_MODULES="overlay-scrollbar"
export GTK_MODULES=""
export GTK2_MODULES=""

glxgears

export JAVA_OPTS="-Dswing.systemlaf=javax.swing.plaf.nimbus.NimbusLookAndFeel"

java -Djava.library.path=$LD_LIBRARY_PATH -Dunixfs=false -DuseGVFS=false -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Dsun.java2d.xrender=true -Djava.net.useSystemProxies=true -Dapplication.update=skip -Dapplication.deployment=usc -Dnet.filebot.UserFiles.fileChooser=JavaFX "-Dapplication.dir=$APP_DATA" "-Dapplication.cache=$APP_CACHE/ehcache.disk.store" "-Djava.io.tmpdir=$APP_CACHE/java.io.tmpdir" "-Dnet.filebot.AcoustID.fpcalc=$SNAP/usr/bin/fpcalc" $JAVA_OPTS -jar "$APP_ROOT/FileBot.jar" "$@"
