#!/bin/bash
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
INSTALL_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

JAR_FILE="$INSTALL_DIR/FileBot.jar"
echo "Update $JAR_FILE"
curl -L -o "$JAR_FILE" -z "$JAR_FILE" "http://sourceforge.net/projects/filebot/files/filebot/HEAD/FileBot.jar"
