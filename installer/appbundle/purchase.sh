#!/bin/sh
/usr/bin/codesign -v /Applications/FileBot.app && /usr/local/bin/filebot || /usr/bin/open "https://itunes.apple.com/us/app/filebot/id905384638?mt=12&uo=6&at=1l3vupy&ct=darwin"
