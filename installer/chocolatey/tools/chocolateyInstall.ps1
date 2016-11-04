$name = '@{package.name}'
$type = 'msi'
$silent = '/quiet'

$url32 = 'http://downloads.sourceforge.net/project/filebot/filebot/@{release}/@{release}_x86.msi'
$url64 = 'http://downloads.sourceforge.net/project/filebot/filebot/@{release}/@{release}_x64.msi'
$checksum32 = '@{x86.msi.sha256}'
$checksum64 = '@{x64.msi.sha256}'
$algorithm = 'sha256'

Install-ChocolateyPackage $name $type $silent $url32 $url64 -checksum $checksum32 -checksumType $algorithm -checksum64 $checksum64 -checksumType64 $algorithm
