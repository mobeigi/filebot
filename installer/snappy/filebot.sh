#!/bin/sh

export LC_ALL="en.utf-8"

export PATH="$SNAP/bin:$SNAP/usr/bin:$PATH"
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$SNAP/lib:$SNAP/usr/lib:$SNAP/lib/x86_64-linux-gnu:$SNAP/usr/lib/x86_64-linux-gnu"

export LD_LIBRARY_PATH="$SNAP/usr/lib/x86_64-linux-gnu/mesa:$LD_LIBRARY_PATH"

export JAVA_HOME="$SNAP/usr/lib/jvm/default-java"
export PATH="$SNAP/usr/lib/jvm/default-java/bin:$SNAP/usr/lib/jvm/default-java/jre/bin:$PATH"

export LD_LIBRARY_PATH="$SNAP/usr/lib/x86_64-linux-gnu/dri/swrast_dri.so:$SNAP/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/amd64:$SNAP/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/amd64:$SNAP/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/amd64:$SNAP/usr/lib/jvm/java-8-openjdk-amd64/bin/../lib/amd64/jli:$SNAP/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/../lib/amd64/jli:$SNAP/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/amd64:$SNAP/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/amd64/jli:$SNAP/usr/lib/x86_64-linux-gnu:$SNAP/usr/lib/x86_64-linux-gnu/pulseaudio:$LD_LIBRARY_PATH"

export LD_LIBRARY_PATH=$SNAP_LIBRARY_PATH:$LD_LIBRARY_PATH

export JAVA_HOME="$SNAP/usr/lib/jvm/default-java"
export APP_ROOT="$SNAP/filebot"

export APP_DATA="$SNAP_USER_DATA/data"
export APP_CACHE="$SNAP_USER_DATA/cache"

export FONTCONFIG_FILE=$SNAP/fonts.conf
export LIBGL_DEBUG=verbose

# export JAVA_OPTS="-Dswing.defaultlaf=javax.swing.plaf.nimbus.NimbusLookAndFeel -Dswing.systemlaf=javax.swing.plaf.nimbus.NimbusLookAndFeel"

# start filebot
$JAVA_HOME/bin/java -Dunixfs=false -DuseGVFS=true -Dapplication.update=skip -Dapplication.deployment=usc -Dnet.filebot.UserFiles.fileChooser=JavaFX "-Dapplication.dir=$APP_DATA" "-Dapplication.cache=$APP_CACHE/ehcache.disk.store" "-Djava.io.tmpdir=$APP_CACHE/java.io.tmpdir" "-Dnet.filebot.AcoustID.fpcalc=$SNAP/usr/bin/fpcalc" $JAVA_OPTS -jar "$APP_ROOT/FileBot.jar" "$@"
