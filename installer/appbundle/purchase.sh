#!/bin/sh
/usr/bin/codesign -v /Applications/FileBot.app && /usr/local/bin/filebot || /usr/bin/open https://app.filebot.net/purchase/FileBot.app
