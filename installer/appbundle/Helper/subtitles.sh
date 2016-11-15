#!/bin/sh -x

PKG_NAME="FileBot_Subtitles_Installer.pkg"
PKG_URL="https://app.filebot.net/files/$PKG_NAME"
PKG="/tmp/$PKG_NAME"

curl -L -o "$PKG" -z "$PKG" --retry 5 "$PKG_URL" && spctl -a -v --type install "$PKG" && sudo installer -verbose -pkg "$PKG" -target LocalSystem
