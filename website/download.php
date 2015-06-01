<?php
$version = isset($_GET['version']) ? $_GET['version'] : '@{version}'; // default version is hard-coded via deployment script
$arch = $_GET['arch'];
$type = $_GET['type'];

$folder = 'https://sourceforge.net/projects/filebot/files/filebot/FileBot_'.$version;
$file = 'undefined';

if ($type == 'nsis') {
	$folder = 'https://sourceforge.net/projects/filebot/files/filebot/HEAD';
	$file = 'FileBot-setup.exe';
} else if ($type == 'jar') {
	$folder = 'https://sourceforge.net/projects/filebot/files/filebot/HEAD';
	$file = 'FileBot.jar';
} else if ($type == 'msi')
	$file = 'FileBot_'.$version.'_'.$arch.'.msi';
else if ($type == 'deb')
	$file =  'filebot_'.$version.'_'.$arch.'.deb';
else if ($type == 'app')
	$file = 'FileBot_'.$version.'-brew.tar.bz2';
else if ($type == 'portable')
	$file = 'FileBot_'.$version.'-portable.zip';
else if ($type == 'ipkg')
	$file = 'filebot_'.$version.'_noarch.ipk';


$downloadPage = $folder.'/'.$file.'/download';

header('HTTP/1.1 302 Found');
header('Location: '.$downloadPage);
?>
