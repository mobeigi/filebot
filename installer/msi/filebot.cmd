@ECHO OFF
java -Xmx256m -DuseExtendedFileAttributes=true -Dapplication.deployment=msi -Dapplication.dir="%APPDATA%\FileBot" -Dsun.net.client.defaultConnectTimeout=5000 -Dsun.net.client.defaultReadTimeout=25000 -jar "%~dp0FileBot.jar" %*
