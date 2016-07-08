#!/bin/sh
PRG="$0"

# resolve relative symlinks
while [ -h "$PRG" ]; do
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

# restore original working dir
cd "$WORKING_DIR"



# update core application files
JAR_FILE="$APP_ROOT/FileBot.jar"
JAR_URL="https://sourceforge.net/projects/filebot/files/filebot/HEAD/FileBot.jar"

# check if file has changed
JAR_SHA1_EXPECTED=`curl --retry 5 "$JAR_URL/list" | egrep -o "[a-z0-9]{40}"`
JAR_SHA1=`sha1sum $JAR_FILE | cut -d' ' -f1`

if [ -z "$JAR_SHA1_EXPECTED" ]; then
	echo "SHA1 hash unknown"
	exit 1
fi

if [ "$JAR_SHA1" == "$JAR_SHA1_EXPECTED" ]; then
	echo "$JAR_FILE [SHA1: $JAR_SHA1]"
	exit 0
fi

echo "Update $JAR_FILE"
curl -L -o "$JAR_FILE" -z "$JAR_FILE" --retry 5 "$JAR_URL"	# FRS will redirect to (unsecure) HTTP download link

# check if file has been corrupted
JAR_SHA1=`sha1sum $JAR_FILE | cut -d' ' -f1`
echo "$JAR_FILE [SHA1: $JAR_SHA1]"

if [ "$JAR_SHA1" != "$JAR_SHA1_EXPECTED" ]; then
	echo "SHA1 hash mismatch [SHA1: $JAR_SHA1_EXPECTED]"
	rm -vf "$JAR_FILE"
	exit 1
fi
