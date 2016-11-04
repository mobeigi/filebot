$app = Get-WmiObject -Query "SELECT * FROM Win32_Product WHERE Name = '@{application.name}'"

if ($app -eq $null) {
	echo 'FileBot is not installed.'
} else {
	$app.Uninstall()
	echo 'FileBot has been uninstalled.'
}
