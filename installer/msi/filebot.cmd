@ECHO OFF
java -Dapplication.dir="%APPDATA%\FileBot" -Dapplication.deployment=msi -Djava.io.tmpdir="%APPDATA%\FileBot\temp" -Xmx256m -jar "%~dp0FileBot.jar" %*
