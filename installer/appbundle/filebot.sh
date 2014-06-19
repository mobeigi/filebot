#!/bin/bash
PRG="${BASH_SOURCE[0]}"

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
APP_ROOT=`cd "$PRG_DIR/../.." && pwd`

# restore original working dir
cd "$WORKING_DIR"

java -Djava.awt.headless=true -Dunixfs=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Djava.net.useSystemProxies=true -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Djna.nosys=true -Dapplication.deployment=app "-Djna.library.path=$APP_ROOT/Contents/MacOS" "-Djava.library.path=$APP_ROOT/Contents/MacOS" "-Dnet.filebot.AcoustID.fpcalc=$APP_ROOT/Contents/MacOS/fpcalc" -jar "$APP_ROOT"/Contents/Java/* "$@"
