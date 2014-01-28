#!/bin/sh

# create /bin symlink
ln -s /usr/share/filebot/bin/filebot.sh /usr/bin/filebot

# delete caches and logs
filebot -clear-cache
