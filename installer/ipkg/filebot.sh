#!/bin/sh

APP_ROOT="/opt/share/filebot"
APP_DATA="$APP_ROOT/data/$USER"

# add APP_ROOT to LD_LIBRARY_PATH
if [ ! -z "$LD_LIBRARY_PATH" ]
then
	export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$APP_ROOT"
else
	export LD_LIBRARY_PATH="$APP_ROOT"
fi

# force JVM language and encoding settings
export LANG="en_US.UTF-8"
export LC_ALL="en_US.UTF-8"

# FileBot settings
# EXTRACTOR="SevenZipNativeBindings"	# use the lib7-Zip-JBinding.so native library
# EXTRACTOR="SevenZipExecutable"		# use the 7z executable
EXTRACTOR="ApacheVFS"					# use Apache Commons VFS2 with junrar plugin

java -Dunixfs=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Dfile.encoding="UTF-8" -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Dapplication.deployment=ipkg -Dnet.filebot.Archive.extractor="$EXTRACTOR" -Dnet.filebot.AcoustID.fpcalc="fpcalc" -Dapplication.dir="$APP_DATA" -Djava.io.tmpdir="$APP_DATA/temp" -Duser.home="$APP_DATA" -jar "$APP_ROOT/FileBot.jar" "$@"
