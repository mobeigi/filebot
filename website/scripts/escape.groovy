net.sourceforge.filebot.cli.CLILogging.CLILogger.setLevel(java.util.logging.Level.OFF)

console.printf('Enter: ')
def s = console.readLine()

// ‘$’, ‘`’, or ‘\’
console.printf('%n"' + s.replaceAll('["$`\\\\]', {'\\'+it}) + '"%n')
