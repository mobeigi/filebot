#!/bin/sh

function validate {
	/usr/bin/codesign -v /Applications/FileBot.app
}

function purchase {
	/usr/bin/open https://app.filebot.net/purchase/FileBot.app
}

function help {
	/usr/bin/open https://app.filebot.net/mac/brew.html
}

function start {
	/usr/local/bin/filebot
}


validate && (start || help) || purchase
