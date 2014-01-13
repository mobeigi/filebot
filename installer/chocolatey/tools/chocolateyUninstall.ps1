Invoke-Expression 'filebot -clear-cache'

$app = Get-WmiObject -Query "SELECT * FROM Win32_Product WHERE Name = 'FileBot'"
echo $app

if ($app -eq $null) {
	echo 'FileBot is not installed.'
} else {
	$app.Uninstall()
	echo 'FileBot has been uninstalled.'
}
