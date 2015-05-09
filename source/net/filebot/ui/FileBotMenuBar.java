package net.filebot.ui;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import net.filebot.Main;
import net.filebot.Settings;

public class FileBotMenuBar {

	public static JMenuBar createHelp() {
		JMenu help = new JMenu("Help");
		Settings.getHelpURIs().forEach((title, uri) -> {
			help.add(createLink(title, uri));
		});

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(help);
		return menuBar;
	}

	private static Action createLink(final String title, final URI uri) {
		return new AbstractAction(title) {

			@Override
			public void actionPerformed(ActionEvent evt) {
				try {
					Desktop.getDesktop().browse(uri);
				} catch (Exception e) {
					Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Failed to browse URI", e);
				}
			}
		};
	}

}
