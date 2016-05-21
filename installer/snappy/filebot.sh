#!/bin/sh

export LC_ALL="en.utf-8"

export PATH="$SNAP/bin:$SNAP/usr/bin"
export LD_LIBRARY_PATH="$SNAP/lib:$SNAP/usr/lib:$SNAP/lib/x86_64-linux-gnu:$SNAP/usr/lib/x86_64-linux-gnu"

export LD_LIBRARY_PATH="$SNAP/usr/lib/x86_64-linux-gnu/mesa:$LD_LIBRARY_PATH"

export JAVA_HOME="$SNAP/oracle-java"
export PATH="$JAVA_HOME/bin:$JAVA_HOME/jre/bin:$PATH"

export LD_LIBRARY_PATH="$SNAP/oracle-java/lib/amd64/jli:$SNAP/oracle-java/lib/amd64:$SNAP/oracle-java/jre/lib/amd64:$LD_LIBRARY_PATH"

export LD_LIBRARY_PATH="$SNAP/usr/lib/x86_64-linux-gnu/mesa:$SNAP/usr/lib/x86_64-linux-gnu/dri:$LD_LIBRARY_PATH"

export LD_LIBRARY_PATH="$SNAP/usr/lib/x86_64-linux-gnu/gtk-2.0/2.10.0/engines:$SNAP/usr/lib/x86_64-linux-gnu/gtk-2.0/modules:$SNAP/usr/lib/x86_64-linux-gnu/gtk-2.0:$LD_LIBRARY_PATH"

export LD_LIBRARY_PATH="$SNAP/usr/lib/x86_64-linux-gnu:$SNAP/usr/lib/x86_64-linux-gnu/pulseaudio:$LD_LIBRARY_PATH"

export LD_LIBRARY_PATH="$SNAP_LIBRARY_PATH:$LD_LIBRARY_PATH"

export GDK_PIXBUF_MODULEDIR="$SNAP/usr/lib/x86_64-linux-gnu/gdk-pixbuf-2.0/2.10.0"
export GDK_PIXBUF_MODULE_FILE="$SNAP/usr/lib/x86_64-linux-gnu/gdk-pixbuf-2.0/2.10.0/loaders.cache"

export LIBGL_DRIVERS_PATH="$SNAP/usr/lib/x86_64-linux-gnu/dri"

export APP_ROOT="$SNAP/filebot"

export APP_DATA="$SNAP_USER_DATA/data"
export APP_CACHE="$SNAP_USER_DATA/cache"

export FONTCONFIG_FILE="$SNAP/fonts.conf"		# causes startup lag the first time the font cache is initialized
export LIBGL_DEBUG=verbose
export G_MESSAGES_DEBUG=all

export GTK_MODULES="gail:atk-bridge:unity-gtk-module"
export GTK2_MODULES="overlay-scrollbar"
export GTK_MODULES=""
export GTK2_MODULES=""

export GTK_PATH="$SNAP/usr/lib/x86_64-linux-gnu/gtk-2.0/2.10.0"

# export GIO_MODULE_DIR="$SNAP/usr/lib/x86_64-linux-gnu/gio/modules"
# export LD_LIBRARY_PATH="$GIO_MODULE_DIR:$LD_LIBRARY_PATH"

export GTK2_RC_FILES="$SNAP/usr/share/themes/Ambiance/gtk-2.0/gtkrc"
export GTK_THEME="$SNAP/usr/share/themes/Ambiance/gtk-3.0/gtk.css"

# Not good, needed for fontconfig
export XDG_DATA_HOME="$SNAP/usr/share"
export GSETTINGS_SCHEMA_DIR="$SNAP/usr/share/glib-2.0/schemas"

# Font Config
export FONTCONFIG_PATH="$SNAP/etc/fonts/config.d"
export FONTCONFIG_FILE="$SNAP/etc/fonts/fonts.conf"

export ORIGIN="$SNAP/usr/lib/x86_64-linux-gnu"

# export JAVA_OPTS="-Dswing.systemlaf=javax.swing.plaf.nimbus.NimbusLookAndFeel $JAVA_OPTS"
# export JAVA_OPTS="-Dsun.java2d.opengl=True $JAVA_OPTS"
# export JAVA_OPTS="-Dsun.java2d.xrender=True $JAVA_OPTS"

java -Djava.library.path="$LD_LIBRARY_PATH" -Djna.library.path="$LD_LIBRARY_PATH" -Dunixfs=false -DuseGVFS=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Djava.net.useSystemProxies=true -Dapplication.update=skip -Dapplication.deployment=usc -Dnet.filebot.UserFiles.fileChooser=JavaFX -Dapplication.dir="$APP_DATA" -Dapplication.cache="$APP_CACHE/ehcache.disk.store" -Djava.io.tmpdir="$APP_CACHE/java.io.tmpdir" "-Dnet.filebot.AcoustID.fpcalc=$SNAP/usr/bin/fpcalc" $JAVA_OPTS -jar "$APP_ROOT/FileBot.jar" "$@"
