


def escapeShell(String arg) {
    return '"' + arg.replaceAll(/["$`<>^\\"]/, /\\$0/) + '"'
}



if (java.awt.GraphicsEnvironment.headless && console != null) {
	// CLI mode
	console.printf('Enter: ')
	def s = console.readLine()
	console.println('\n' + escapeShell(s) + '\n')
	System.exit(0)
} else {
	// GUI mode
	new groovy.swing.SwingBuilder().edt{
	    frame(title: 'Escape Tool', size: [350, 230], show: true, defaultCloseOperation: javax.swing.JFrame.EXIT_ON_CLOSE) { 
	        gridLayout(cols: 1, rows: 2)
				scrollPane{
					textArea id: 'value', lineWrap: true, font: new java.awt.Font('Monospaced', 0, 16)
				}
				scrollPane{
	            	textArea id: 'escape', lineWrap: true, text: bind(source:value, sourceProperty:'text', converter: { escapeShell(it) }), font: new java.awt.Font('Monospaced', 0, 16)
				}
		}
	}
	System.in.read() // wait for GUI to close
}
