@ECHO OFF
java -Dapplication.deployment=msi -Dapplication.dir="%APPDATA%\FileBot" -Xmx256m -jar "%~dp0FileBot.jar" %*
