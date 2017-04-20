<?php
$type = isset($_GET['src']) ? $_GET['src'] : '';
$name = 'FileBot Media Renamer';

// packages: msi, app, deb, jar, portable
if ($type == 'msi')
	$name = 'FileBot for Windows';
else if ($type == 'app')
	$name = 'FileBot for Mac';
else if ($type == 'deb')
	$name = 'FileBot for Debian Linux';
else if ($type == 'portable')
	$name = 'FileBot Portable';
else if ($type == 'jar')
	$name = 'FileBot Jar';
else if ($type == 'forum')
	$name = 'Customer Support';
else if (strlen($type) > 0)
	$name = 'FileBot ('.strtolower($type).')';


// insert product name and redirect to paypal donation page
$url = 'https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=rednoah%40filebot%2enet&lc=US&item_name='.urlencode($name).'&amount=10%2e00&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHosted';

header('HTTP/1.1 302 Found');
header('Location: '.$url);
?>
