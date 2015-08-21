del FileBot-setup.exe
makensis filebot.nsi

signtool sign /t http://timestamp.verisign.com/scripts/timstamp.dll /v /a FileBot-setup.exe