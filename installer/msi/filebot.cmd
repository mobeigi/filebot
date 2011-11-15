@ECHO OFF
java -Dapplication.deployment=msi -Xmx256m -jar "%~dp0FileBot.jar" %*