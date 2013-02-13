@ECHO OFF
java -Xmx256m -DuseExtendedFileAttributes=true -Dapplication.dir="%APPDATA%\FileBot" -Dapplication.deployment=msi -Dapplication.analytics=true -Djava.net.useSystemProxies=true -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=60000 "-Djna.library.path=%~dp0." "-Djava.library.path=%~dp0." -jar "%~dp0FileBot.jar" %*
