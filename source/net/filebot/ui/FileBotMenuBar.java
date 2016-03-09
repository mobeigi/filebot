package net.filebot.ui;

import static net.filebot.Logging.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import net.filebot.Settings;

public class FileBotMenuBar {

	public static JMenuBar createHelp() {
		JMenu help = new JMenu("Help");
		Settings.getHelpURIs().forEach((title, uri) -> {
			help.add(createLink(title, URI.create(uri)));
		});

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(help);
		return menuBar;
	}

	private static Action createLink(final String title, final URI uri) {
		return newAction(title, null, evt -> {
			try {
				Desktop.getDesktop().browse(uri);
			} catch (Exception e) {
				debug.log(Level.SEVERE, "Failed to open URI: " + uri, e);
			}
		});
	}

}
