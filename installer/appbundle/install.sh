#!/bin/bash
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ] ; do SOURCE="$(readlink "$SOURCE")"; done
SOURCE="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

sudo ln -s -f "$SOURCE/filebot.sh" "/usr/bin/filebot"
