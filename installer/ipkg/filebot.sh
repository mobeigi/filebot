#!/bin/bash
java -Dunixfs=false -Dapplication.deployment=ipkg -Dapplication.dir=$HOME/.filebot -Djava.io.tmpdir=$HOME/.filebot/temp -Djna.library.path=/usr/share/filebot -Djava.library.path=/usr/share/filebot -jar /usr/share/filebot/FileBot.jar "$@"
