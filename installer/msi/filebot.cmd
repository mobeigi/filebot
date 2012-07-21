@ECHO OFF
java -Dapplication.deployment=msi -Dapplication.dir="%APPDATA%\FileBot" -Dsun.net.client.defaultConnectTimeout=5000 -Dsun.net.client.defaultReadTimeout=25000 -Xmx256m -jar "%~dp0FileBot.jar" %*
