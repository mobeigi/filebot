#!/bin/sh

if [ $SNAP_ARCH != amd64 ]; then
	echo "CPU architecture not supported: $SNAP_ARCH"
	exit 1
fi


export LANG=C.UTF-8
export ARCH=x86_64-linux-gnu


export JAVA_HOME=$SNAP/oracle-java
export PATH=$JAVA_HOME/jre/bin:$PATH

export LD_LIBRARY_PATH=$JAVA_HOME/jre/lib/$SNAP_ARCH/jli:$JAVA_HOME/jre/lib/$SNAP_ARCH:$LD_LIBRARY_PATH
export LD_LIBRARY_PATH=$SNAP/usr/lib/$ARCH/gtk-2.0/2.10.0/engines:$SNAP/usr/lib/$ARCH/gtk-2.0/modules:$SNAP/usr/lib/$ARCH/pulseaudio:$LD_LIBRARY_PATH

export GDK_PIXBUF_MODULEDIR=$SNAP/usr/lib/x86_64-linux-gnu/gdk-pixbuf-2.0/2.10.0
export GDK_PIXBUF_MODULE_FILE=$SNAP/usr/lib/x86_64-linux-gnu/gdk-pixbuf-2.0/2.10.0/loaders.cache

export GTK2_RC_FILES=$SNAP/usr/share/themes/Ambiance/gtk-2.0/gtkrc
export GTK_THEME=$SNAP/usr/share/themes/Ambiance/gtk-3.0/gtk.css

export GTK_PATH=$SNAP/usr/lib/x86_64-linux-gnu/gtk-2.0/2.10.0
export GTK_MODULES=
export GTK2_MODULES=

# export GIO_MODULE_DIR=$SNAP/usr/lib/$ARCH/gio/modules
export GSETTINGS_SCHEMA_DIR=$SNAP/usr/share/glib-2.0/schemas
export XKB_CONFIG_ROOT=$SNAP/usr/share/X11/xkb

export XDG_CONFIG_DIRS=$SNAP/usr/xdg:$SNAP/etc/xdg:$XDG_CONFIG_DIRS
export XDG_DATA_DIRS=$SNAP/usr/share:$SNAP_USER_DATA:$XDG_DATA_DIRS
export XDG_DATA_HOME=$SNAP/usr/share

export XDG_DATA_HOME=$SNAP/usr/share
export FONTCONFIG_PATH=$SNAP/etc/fonts/config.d
export FONTCONFIG_FILE=$SNAP/etc/fonts/fonts.conf

export LIBGL_DRIVERS_PATH=$SNAP/usr/lib/$ARCH/dri


# export JAVA_TOOL_OPTIONS=-javaagent:$SNAP/usr/share/java/jayatanaag.jar
# export JAVA_OPTS=-Dsun.java2d.opengl=True
# export JAVA_OPTS=-Dsun.java2d.xrender=True

export APP_ROOT=$SNAP/filebot
export APP_DATA=$SNAP_USER_DATA/data
export APP_CACHE=$SNAP_USER_DATA/cache
export APP_PREFS=$SNAP_USER_DATA/prefs

java -Djava.library.path="$LD_LIBRARY_PATH" -Djna.library.path="$LD_LIBRARY_PATH" -Dunixfs=false -DuseGVFS=true -DuseExtendedFileAttributes=true -DuseCreationDate=false -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Djava.net.useSystemProxies=true -Dapplication.update=skip -Dapplication.deployment=usc -Dnet.filebot.UserFiles.fileChooser=JavaFX -Dapplication.dir="$APP_DATA" -Dapplication.cache="$APP_CACHE/ehcache.disk.store" -Djava.io.tmpdir="$APP_CACHE/java.io.tmpdir" -Djava.util.prefs.userRoot="$APP_PREFS/user" -Djava.util.prefs.systemRoot="$APP_PREFS/system" -Dnet.filebot.AcoustID.fpcalc="$SNAP/usr/bin/fpcalc" $JAVA_OPTS -jar "$APP_ROOT/FileBot.jar" "$@"
