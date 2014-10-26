#!/bin/sh

# force JVM language and encoding settings
export LANG="en_US.UTF-8"
export LC_ALL="en_US.UTF-8"

java -Dunixfs=false -DuseExtendedFileAttributes=true -DuseCreationDate=false -Dfile.encoding=UTF-8 -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 -Dapplication.deployment=ipkg -Dapplication.analytics=true -Duser.home=/opt/share/filebot/data -Dapplication.dir=/opt/share/filebot/data -Djava.io.tmpdir=/opt/share/filebot/data/temp -Djna.library.path=/opt/share/filebot -Djava.library.path=/opt/share/filebot -Dnet.filebot.AcoustID.fpcalc=/opt/share/filebot/fpcalc -jar /opt/share/filebot/FileBot.jar "$@"
