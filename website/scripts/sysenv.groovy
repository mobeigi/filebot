// filebot -script fn:sysenv

println '# Java System Properties #'
_system.each{
	println "$it.key: $it.value"
}

println '# Environment Variables #'
_environment.each{
	println "$it.key: $it.value"
}
