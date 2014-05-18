#!/bin/bash
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
SOURCE="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

APP_ROOT="$SOURCE/../.."

java -Djava.awt.headless=true -Dunixfs=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Djava.net.useSystemProxies=true -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Djna.nosys=true -Dapplication.deployment=app "-Djna.library.path=$APP_ROOT/Contents/MacOS" "-Djava.library.path=$APP_ROOT/Contents/MacOS" "-Dnet.filebot.AcoustID.fpcalc=$APP_ROOT/Contents/MacOS/fpcalc" -jar "$APP_ROOT"/Contents/Java/* "$@"
