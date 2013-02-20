<?php
$version = isset($_GET['version']) ? $_GET['version'] : '@{version}'; // default version is hard-coded via deployment script
$arch = $_GET['arch'];
$type = $_GET['type'];
$mode = $_GET['mode'];

$folder = 'http://sourceforge.net/projects/filebot/files/filebot/FileBot_'.$version;
$file = 'undefined';
if ($type == 'msi')
	$file = 'FileBot_'.$version.'_'.$arch.'.msi';
if ($type == 'deb')
	$file =  'filebot_'.$version.'_'.$arch.'.deb';
if ($type == 'app')
	$file = 'FileBot_'.$version.'.app.tar.gz';
if ($type == 'jar')
	$file = 'FileBot_'.$version.'.jar';
if ($type == 'portable')
	$file = 'FileBot_'.$version.'-portable.zip';
if ($type == 'ipkg')
	$file =  'filebot_'.$version.'_'.$arch.'.ipk';
if ($type == 'src')
	$file = 'filebot-'.$version.'-src.zip';

$downloadPage = $folder.'/'.$file.'/download';
if ($mode != 's') {
	header('HTTP/1.1 302 Found');
	header('Location: '.$downloadPage);
} else {
?>
<html>
	<head>
		<title>Download <? print($file) ?> from SourceForge.net</title>
		<link rel="stylesheet" href="base.css" type="text/css" />
	</head>
	<body style="padding:0; margin:0; background:white">
		<div style="font-size: 12px; margin-bottom:0px; border-bottom: 1px solid lightgray;	box-shadow: 0px 0px 4px #a2a2a2; width:100%; height:75px; background:#E6E6E6">
			<div style="float:left; width:630px; margin: 5px 5px 5px 40px; background: white; padding: 2px 5px; border-radius: 4px; box-shadow: 0 2px 5px #A2A2A2;">
				<form style="float:right; margin: 15px 5px 0 5px" action="https://www.paypal.com/cgi-bin/webscr" method="post">
				<input type="hidden" name="cmd" value="_s-xclick">
				<input type="hidden" name="encrypted" value="-----BEGIN PKCS7-----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-----END PKCS7-----">
				<input type="image" src="https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif" border="0" name="submit" alt="PayPal - The safer, easier way to pay online!">
				<img alt="" border="0" src="https://www.paypalobjects.com/en_US/i/scr/pixel.gif" width="1" height="1">
				</form>
				<img src="images/money.png" style="float:left; margin:5px 10px 5px 5px; width:48px; height:48px" />
				<p style="margin: 13px 20px">
					Shudder at the thought of how many hours and days of time FileBot has saved you?
					You can show your appreciation and <b>support for future development</b> by donating.
				</p>
			</div>
		</div>
		<iframe src="<? print($downloadPage) ?>" name="sourceforge" width="100%" height="1200" frameBorder="0" scrolling="no" seamless="seamless"></iframe>
	</body>
</html>
<?
}
?>
