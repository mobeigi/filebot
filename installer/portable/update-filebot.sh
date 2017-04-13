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
PACKAGE_NAME="FileBot.jar.xz.gpg"
PACKAGE_FILE="$APP_ROOT/$PACKAGE_NAME"
PACKAGE_URL="https://sourceforge.net/projects/filebot/files/filebot/HEAD/$PACKAGE_NAME"

# check if file has changed
PACKAGE_SHA1_EXPECTED=`curl --retry 5 "$PACKAGE_URL/list" | egrep -o "[a-z0-9]{40}"`
PACKAGE_SHA1=`sha1sum $PACKAGE_FILE | cut -d' ' -f1`

if [ -z "$PACKAGE_SHA1_EXPECTED" ]; then
	echo "SHA1 hash unknown"
	exit 1
fi

if [ "$PACKAGE_SHA1" == "$PACKAGE_SHA1_EXPECTED" ]; then
	echo "$PACKAGE_FILE [SHA1: $PACKAGE_SHA1]"
	exit 0
fi

echo "Update $PACKAGE_FILE"
curl -L -o "$PACKAGE_FILE" -z "$PACKAGE_FILE" --retry 5 "$PACKAGE_URL"	# FRS will redirect to (unsecure) HTTP download link

# check if file has been corrupted (or modified) in transit
PACKAGE_SHA1=`sha1sum $PACKAGE_FILE | cut -d' ' -f1`
echo "$PACKAGE_FILE [SHA1: $PACKAGE_SHA1]"

if [ "$PACKAGE_SHA1" != "$PACKAGE_SHA1_EXPECTED" ]; then
	echo "SHA1 hash mismatch [SHA1: $PACKAGE_SHA1_EXPECTED]"
	rm -vf "$PACKAGE_FILE"
	exit 1
fi


# initialize gpg
GPG_HOME="$APP_ROOT/.gpg"
JAR_XZ_FILE="$APP_ROOT/FileBot.jar.xz"

if [ ! -d "$GPG_HOME" ]; then
	mkdir -p -m 700 "$GPG_HOME" && gpg --homedir "$GPG_HOME" --import "$APP_ROOT/filebot.pub"
fi

# verify signature and extract jar
gpg --batch --yes --homedir "$GPG_HOME" --trusted-key "4E402EBF7C3C6A71" --output "$JAR_XZ_FILE" --decrypt "$PACKAGE_FILE" && xz --decompress --force "$JAR_XZ_FILE"
