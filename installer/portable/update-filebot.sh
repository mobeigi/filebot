#!/bin/bash
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
INSTALL_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

echo "Update $INSTALL_DIR/FileBot.jar"
curl -L -O -z "$INSTALL_DIR/FileBot.jar" "http://sourceforge.net/projects/filebot/files/filebot/HEAD/FileBot.jar"
