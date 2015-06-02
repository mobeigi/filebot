$name = 'filebot'
$type = 'msi'
$silent = '/quiet'
$checksumType32 = 'sha1'
$checksumType64 = 'sha1'

$url32 = 'https://downloads.sourceforge.net/project/filebot/filebot/FileBot_@{version}/FileBot_@{version}_x86.msi'
$url64 = 'https://downloads.sourceforge.net/project/filebot/filebot/FileBot_@{version}/FileBot_@{version}_x64.msi'
$checksum32 = '@{x86.msi.sha1}'
$checksum64 = '@{x64.msi.sha1}'

Install-ChocolateyPackage $name $type $silent $url32 $url64 -checksum $checksum32 -checksumType $checksumType32 -checksum64 $checksum64 -checksumType64 $checksumType64
