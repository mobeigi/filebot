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

# restore original working dir (which may be /root and yield permission denied)
if [ -x "$WORKING_DIR" ]; then
	cd "$WORKING_DIR"
else
	cd "/volume1"
fi


# make sure required environment variables are set
if [ -z "$USER" ]; then
	export USER=`whoami`
fi

# force JVM language and encoding settings
export LANG="en_US.UTF-8"
export LC_ALL="en_US.UTF-8"

# add 3rd party packages to the library path by default
SYNO_FPCALC="/usr/local/chromaprint/bin/fpcalc"
SYNO_LIBRARY_PATH="/usr/local/mediainfo/lib:/usr/local/chromaprint/lib"

# add APP_ROOT to LD_LIBRARY_PATH
if [ ! -z "$LD_LIBRARY_PATH" ]; then
	export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$SYNO_LIBRARY_PATH:$APP_ROOT"
else
	export LD_LIBRARY_PATH="$SYNO_LIBRARY_PATH:$APP_ROOT"
fi

# choose extractor
EXTRACTOR="ApacheVFS"					# use Apache Commons VFS2 with junrar plugin
# EXTRACTOR="SevenZipExecutable"		# use the 7z executable
# EXTRACTOR="SevenZipNativeBindings"	# use the lib7-Zip-JBinding.so native library

# select application data folder
APP_DATA="$APP_ROOT/data/$USER"

# start filebot
java -Djava.awt.headless=true -Dunixfs=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Dfile.encoding="UTF-8" -Dsun.jnu.encoding="UTF-8" -Djava.net.useSystemProxies=true -Djna.nosys=true -Dapplication.deployment=spk -Dnet.filebot.Archive.extractor="$EXTRACTOR" -Dnet.filebot.AcoustID.fpcalc="$SYNO_FPCALC" -Dapplication.dir="$APP_DATA" -Djava.io.tmpdir="$APP_DATA/temp" -Duser.home="$APP_DATA" -Djava.util.prefs.PreferencesFactory=net.filebot.util.prefs.FilePreferencesFactory -Dnet.filebot.util.prefs.file="$APP_DATA/prefs.properties" $JAVA_OPTS -jar "$APP_ROOT/FileBot.jar" "$@"
