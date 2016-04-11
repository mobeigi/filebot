<?php
$version = isset($_GET['version']) ? $_GET['version'] : '@{version}';	// default version is hard-coded via deployment script
$arch = $_GET['arch'];
$type = $_GET['type'];

$root = 'https://downloads.sourceforge.net/project/filebot/filebot/';
$folder = $root.'FileBot_'.$version;
$file = '';

if ($type == 'nsis') {
	$folder = $root.'LATEST';
	$file = 'FileBot-setup.exe';
} else if ($type == 'jar') {
	$folder = $root.'HEAD';
	$file = 'FileBot.jar';
} else if ($type == 'msi') {
	$file = 'FileBot_'.$version.'_'.$arch.'.msi';
} else if ($type == 'deb') {
	$file =  'filebot_'.$version.'_'.$arch.'.deb';
} else if ($type == 'app') {
	$file = 'FileBot_'.$version.'-brew.tar.bz2';
} else if ($type == 'portable') {
	$file = 'FileBot_'.$version.'-portable.zip';
} else if ($type == 'ipkg') {
	$file = 'filebot_'.$version.'_noarch.ipk';
} else {
	$folder = 'https://sourceforge.net/projects/filebot/files/filebot/FileBot_'.$version.'/';	// redirect to latest release folder by default
}

header('HTTP/1.1 302 Found');
header('Location: '.$folder.'/'.$file);
?>
