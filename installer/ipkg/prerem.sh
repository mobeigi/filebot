#!/bin/sh

# delete caches and logs
filebot -clear-cache

# remove /bin symlink
rm /opt/usr/bin/filebot
