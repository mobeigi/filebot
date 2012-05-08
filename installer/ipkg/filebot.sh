#!/bin/bash
java -Dunixfs=false -Dapplication.deployment=ipkg -Djna.library.path=/usr/share/filebot -Djava.library.path=/usr/share/filebot -jar /usr/share/filebot/FileBot.jar "$@"
