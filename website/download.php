<?php
$version = isset($_GET['version']) ? $_GET['version'] : '@{version}'; // default version is hard-coded via deployment script
$arch = $_GET['arch'];
$type = $_GET['type'];

$folder = 'http://sourceforge.net/projects/filebot/files/filebot/FileBot_'.$version;
$file = 'undefined';

if ($type == 'nsis') {
	$folder = 'http://sourceforge.net/projects/filebot/files/filebot/HEAD';
	$file = 'FileBot-setup.exe';
} else if ($type == 'msi')
	$file = 'FileBot_'.$version.'_'.$arch.'.msi';
else if ($type == 'deb')
	$file =  'filebot_'.$version.'_'.$arch.'.deb';
else if ($type == 'app')
	$file = 'FileBot_'.$version.'.app.tar.gz';
else if ($type == 'jar')
	$file = 'FileBot_'.$version.'.jar';
else if ($type == 'portable')
	$file = 'FileBot_'.$version.'-portable.zip';
else if ($type == 'ipkg')
	$file =  'filebot_'.$version.'_'.$arch.'.ipk';
else if ($type == 'src')
	$file = 'filebot-'.$version.'-src.zip';


$downloadPage = $folder.'/'.$file.'/download';

header('HTTP/1.1 302 Found');
header('Location: '.$downloadPage);
?>
