#!/bin/sh

# create /bin symlink
ln -s /opt/usr/share/filebot/bin/filebot.sh /opt/usr/bin/filebot

# delete caches and logs
filebot -clear-cache
