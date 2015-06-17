del FileBot-setup.exe
makensis filebot.nsi

del MyCA.pvk MyCA.cer MySPC.pvk MySPC.cer MySPC.pfx
makecert -r -pe -n "CN=FileBot CA" -ss CA -sr CurrentUser -a sha256 -cy authority -sky signature -sv MyCA.pvk MyCA.cer
makecert -pe -n "CN=FileBot NSIS" -a sha256 -cy end -sky signature -ic MyCA.cer -iv MyCA.pvk -sv MySPC.pvk MySPC.cer
pvk2pfx -pvk MySPC.pvk -spc MySPC.cer -pfx MySPC.pfx
signtool sign /v /f MySPC.pfx FileBot-setup.exe
