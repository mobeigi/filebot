#!/bin/sh
PRG="$0"

# resolve relative symlinks
while [ -h "$PRG" ] ; do
	ls=`ls -ld "$PRG"`
	link=`expr "$ls" : '.*-> \(.*\)$'`
	if expr "$link" : '/.*' > /dev/null; then
		PRG="$link"
	else
		PRG="`dirname "$PRG"`/$link"
	fi
done

# get canonical path
WORKING_DIR=`pwd`
PRG_DIR=`dirname "$PRG"`
APP_ROOT=`cd "$PRG_DIR" && pwd`

# add package lib folder to library path
PACKAGE_LIBRARY_PATH="$APP_ROOT/lib/$(uname -m)"

# restore original working dir
cd "$WORKING_DIR"

# make sure required environment variables are set
if [ -z "$USER" ]; then
	export USER=`whoami`
fi

# force JVM language and encoding settings
export LANG="en_US.UTF-8"
export LC_ALL="en_US.UTF-8"

# choose extractor
EXTRACTOR="ApacheVFS"                   # use Apache Commons VFS2 with junrar plugin
# EXTRACTOR="SevenZipExecutable"        # use the 7z executable
# EXTRACTOR="SevenZipNativeBindings"    # use the lib7-Zip-JBinding.so native library

# select application data folder
APP_DATA="$APP_ROOT/data"

# start filebot
java @{java.application.options} -Dapplication.deployment=portable -Djava.awt.headless=true -Dfile.encoding="UTF-8" -Dsun.jnu.encoding="UTF-8" -Dnet.filebot.Archive.extractor="$EXTRACTOR" -Djna.library.path="$PACKAGE_LIBRARY_PATH:$LD_LIBRARY_PATH" -Djava.library.path="$PACKAGE_LIBRARY_PATH:$LD_LIBRARY_PATH" -Dnet.filebot.AcoustID.fpcalc="fpcalc" -Dapplication.dir="$APP_DATA" -Duser.home="$APP_DATA" -Djava.io.tmpdir="$APP_DATA/tmp" -Djava.util.prefs.PreferencesFactory=net.filebot.util.prefs.FilePreferencesFactory -Dnet.filebot.util.prefs.file="$APP_DATA/prefs.properties" $JAVA_OPTS -classpath "$APP_ROOT/*" @{main.class} "$@"
