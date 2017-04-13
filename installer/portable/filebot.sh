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

# add APP_ROOT and PACKAGE_LIBRARY_PATH to LD_LIBRARY_PATH
if [ ! -z "$LD_LIBRARY_PATH" ]; then
	export LD_LIBRARY_PATH="$APP_ROOT:$PACKAGE_LIBRARY_PATH:$LD_LIBRARY_PATH"
else
	export LD_LIBRARY_PATH="$APP_ROOT:$PACKAGE_LIBRARY_PATH"
fi

# choose extractor
EXTRACTOR="ApacheVFS"					# use Apache Commons VFS2 with junrar plugin
# EXTRACTOR="SevenZipExecutable"		# use the 7z executable
# EXTRACTOR="SevenZipNativeBindings"	# use the lib7-Zip-JBinding.so native library

# select application data folder
APP_DATA="$APP_ROOT/data"

# start filebot
java -Dunixfs=false -DuseGVFS=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Djava.net.useSystemProxies=false -Dapplication.deployment=portable -Dfile.encoding="UTF-8" -Dsun.jnu.encoding="UTF-8" -Djna.nosys=false -Djna.nounpack=true -Dnet.filebot.Archive.extractor="$EXTRACTOR" -Dnet.filebot.AcoustID.fpcalc="fpcalc" -Dapplication.dir="$APP_DATA" -Duser.home="$APP_DATA" -Djava.io.tmpdir="$APP_DATA/tmp" -Djava.util.prefs.PreferencesFactory=net.filebot.util.prefs.FilePreferencesFactory -Dnet.filebot.util.prefs.file="$APP_DATA/prefs.properties" $JAVA_OPTS -jar "$APP_ROOT/FileBot.jar" "$@"
