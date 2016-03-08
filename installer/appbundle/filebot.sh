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
APP_ROOT=`cd "$PRG_DIR/../.." && pwd`

# restore original working dir
cd "$WORKING_DIR"

# start filebot
/usr/libexec/java_home --failfast --version '1.8+' --exec java -Dunixfs=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Djava.net.useSystemProxies=true -Dapple.awt.UIElement=true -Djna.nounpack=true -Dapplication.deployment=app "-Djna.library.path=$APP_ROOT/Contents/MacOS" "-Djava.library.path=$APP_ROOT/Contents/MacOS" "-Dnet.filebot.AcoustID.fpcalc=$APP_ROOT/Contents/MacOS/fpcalc" $JAVA_OPTS -jar "$APP_ROOT"/Contents/Java/FileBot*.jar "$@"
