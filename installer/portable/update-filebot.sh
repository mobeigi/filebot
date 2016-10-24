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
JAR_XZ_FILE="$APP_ROOT/FileBot.jar.xz"
JAR_XZ_URL="https://sourceforge.net/projects/filebot/files/filebot/HEAD/FileBot.jar.xz"

# check if file has changed
JAR_XZ_SHA1_EXPECTED=`curl --retry 5 "$JAR_XZ_URL/list" | egrep -o "[a-z0-9]{40}"`
JAR_XZ_SHA1=`sha1sum $JAR_XZ_FILE | cut -d' ' -f1`

if [ -z "$JAR_XZ_SHA1_EXPECTED" ]; then
	echo "SHA1 hash unknown"
	exit 1
fi

if [ "$JAR_XZ_SHA1" == "$JAR_XZ_SHA1_EXPECTED" ]; then
	echo "$JAR_XZ_FILE [SHA1: $JAR_XZ_SHA1]"
	exit 0
fi

echo "Update $JAR_XZ_FILE"
curl -L -o "$JAR_XZ_FILE" -z "$JAR_XZ_FILE" --retry 5 "$JAR_XZ_URL"	# FRS will redirect to (unsecure) HTTP download link

# check if file has been corrupted (or modified) in transit
JAR_XZ_SHA1=`sha1sum $JAR_XZ_FILE | cut -d' ' -f1`
echo "$JAR_XZ_FILE [SHA1: $JAR_XZ_SHA1]"

if [ "$JAR_XZ_SHA1" != "$JAR_XZ_SHA1_EXPECTED" ]; then
	echo "SHA1 hash mismatch [SHA1: $JAR_XZ_SHA1_EXPECTED]"
	rm -vf "$JAR_XZ_FILE"
	exit 1
fi

# unpack new jar
xz --decompress --force --keep "$JAR_XZ_FILE"
