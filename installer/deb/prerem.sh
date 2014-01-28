#!/bin/sh

# delete caches and logs
filebot -clear-cache

# remove /bin symlink
rm /usr/bin/filebot
