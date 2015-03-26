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

# make it fully qualified
WORKING_DIR=`pwd`
PRG_DIR=`dirname "$PRG"`
APP_ROOT=`cd "$PRG_DIR" && pwd`
APP_DATA="$APP_ROOT/data"

# restore original working dir
cd "$WORKING_DIR"

# add 3rd party packages to the library path by default
SYNO_LIBRARY_PATH="/var/packages/mediainfo/target/lib"

# add APP_ROOT to LD_LIBRARY_PATH
if [ ! -z "$LD_LIBRARY_PATH" ]
then
  export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$SYNO_LIBRARY_PATH:$APP_ROOT"
else
  export LD_LIBRARY_PATH="$SYNO_LIBRARY_PATH:$APP_ROOT"
fi

# force JVM language and encoding settings
export LANG="en_US.UTF-8"
export LC_ALL="en_US.UTF-8"

java -Djava.awt.headless=true -Dunixfs=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Dfile.encoding=UTF-8 -Djava.net.useSystemProxies=true -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Djna.nosys=true -Dapplication.deployment=spk "-Dnet.filebot.AcoustID.fpcalc=fpcalc" -Dnet.filebot.Archive.extractor=SevenZipExecutable "-Dnet.filebot.Archive.7z=/usr/syno/bin/7z" "-Dapplication.dir=$APP_DATA" "-Djava.io.tmpdir=$APP_DATA/temp" "-Duser.home=$APP_DATA" -Djava.util.prefs.PreferencesFactory=net.filebot.util.prefs.FilePreferencesFactory "-Dnet.filebot.util.prefs.file=$APP_DATA/prefs.properties" -jar "$APP_ROOT/FileBot.jar" "$@"
