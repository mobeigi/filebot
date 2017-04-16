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
PACKAGE_HASH_EXPECTED=`curl --silent --retry 5 "$PACKAGE_URL/list" | egrep -o "\b[a-z0-9]{32}\b"`
PACKAGE_HASH=`openssl dgst -md5 "$PACKAGE_FILE" | egrep -o "\b[a-z0-9]{32}\b"`

if [ -z "$PACKAGE_HASH_EXPECTED" ]; then
	echo "hash unknown"
	exit 1
fi

if [ "$PACKAGE_HASH" == "$PACKAGE_HASH_EXPECTED" ]; then
	echo "$PACKAGE_FILE [$PACKAGE_HASH]"
	exit 0
fi

echo "Update $PACKAGE_FILE"
curl -L -o "$PACKAGE_FILE" -z "$PACKAGE_FILE" --retry 5 "$PACKAGE_URL"	# FRS will redirect to (unsecure) HTTP download link

# check if file has been corrupted (or modified) in transit
PACKAGE_HASH=`openssl dgst -md5 "$PACKAGE_FILE" | egrep -o "\b[a-z0-9]{32}\b"`
echo "$PACKAGE_FILE [$PACKAGE_HASH]"

if [ "$PACKAGE_HASH" != "$PACKAGE_HASH_EXPECTED" ]; then
	echo "HASH hash mismatch [$PACKAGE_HASH_EXPECTED]"
	rm -vf "$PACKAGE_FILE"
	exit 1
fi


# initialize gpg
GPG_HOME="$APP_ROOT/data/.gpg"
JAR_XZ_FILE="$APP_ROOT/FileBot.jar.xz"

if [ ! -d "$GPG_HOME" ]; then
	mkdir -p "$GPG_HOME" && chmod 700 "$GPG_HOME" && gpg --homedir "$GPG_HOME" --import "$APP_ROOT/maintainer.pub"
fi

# verify signature and extract jar
gpg --batch --yes --homedir "$GPG_HOME" --trusted-key "4E402EBF7C3C6A71" --output "$JAR_XZ_FILE" --decrypt "$PACKAGE_FILE" && xz --decompress --force "$JAR_XZ_FILE"
