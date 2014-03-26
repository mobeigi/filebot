#!/bin/sh

# create /bin symlink
ln -s /opt/share/filebot/bin/filebot.sh /opt/bin/filebot

# delete caches and logs
filebot -clear-cache
