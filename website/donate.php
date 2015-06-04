<?php
$type = isset($_GET['package']) ? $_GET['package'] : 'undefined';
$name = 'undefined';

// packages: msi, app, deb, jar, portable
if ($type == 'msi')
	$name = 'FileBot for Windows';
else if ($type == 'app')
	$name = 'FileBot for Mac';
else if ($type == 'deb')
	$name = 'FileBot for Debian Linux';
else
	$name = 'FileBot ('.$type.')';


// insert product name and redirect to paypal donation page
$url = 'https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=Z8JHALZ4TXGWL&lc=AT&item_name='.urlencode($name).'&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted';

header('HTTP/1.1 302 Found');
header('Location: '.$url);
?>
