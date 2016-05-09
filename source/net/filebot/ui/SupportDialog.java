package net.filebot.ui;

import static javax.swing.JOptionPane.*;
import static net.filebot.Settings.*;
import static net.filebot.util.ui.SwingUI.*;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import net.filebot.HistorySpooler;
import net.filebot.ResourceManager;
import net.filebot.Settings;
import net.filebot.util.PreferencesMap.PreferencesEntry;

public enum SupportDialog {

	Donation {

		@Override
		String getMessage(int renameCount) {
			return String.format("<html><p style='font-size:16pt; font-weight:bold'>Thank you for using FileBot!</p><br><p>It has taken many nights to develop this application. If you enjoy using it,<br>please consider a donation to me and my work. It will help to<br>make FileBot even better!<p><p style='font-size:14pt; font-weight:bold'>You've renamed %,d files.</p><br><html>", renameCount);
		}

		@Override
		String[] getActions(boolean first) {
			if (first)
				return new String[] { "Donate! :)", "Nope! Maybe next time." };
			else
				return new String[] { "Donate again! :)", "Nope! Not this time." };
		}

		@Override
		Icon getIcon() {
			return ResourceManager.getIcon("message.donate");
		}

		@Override
		String getTitle() {
			return "Please Donate";
		}

		@Override
		String getURI() {
			return getDonateURL();
		}

	},

	AppStoreReview {

		@Override
		String getMessage(int renameCount) {
			return String.format("<html><p style='font-size:16pt; font-weight:bold'>Thank you for using FileBot!</p><br><p>It has taken many nights to develop this application. If you enjoy using it,<br>please consider writing a nice review on the %s.<p><p style='font-size:14pt; font-weight:bold'>You've renamed %,d files.</p><br><html>", getAppStoreName(), renameCount);
		}

		@Override
		String[] getActions(boolean first) {
			if (first)
				return new String[] { "Write a Review! :)", "Nope! Maybe next time." };
			else
				return new String[] { "Update my Review! :)", "Nope! Not this time." };
		}

		@Override
		Icon getIcon() {
			return ResourceManager.getIcon("window.icon.large");
		}

		@Override
		String getTitle() {
			return "Please write a Review";
		}

		@Override
		String getURI() {
			return getAppStoreLink();
		}

	};

	public void show(int renameCount) {
		PreferencesEntry<String> support = Settings.forPackage(SupportDialog.class).entry("support.revision").defaultValue("0");
		int supportRev = Integer.parseInt(support.getValue());
		int currentRev = getApplicationRevisionNumber();

		if (supportRev >= currentRev) {
			return;
		}

		String message = getMessage(renameCount);
		String[] actions = getActions(supportRev <= 0);
		JOptionPane pane = new JOptionPane(message, INFORMATION_MESSAGE, YES_NO_OPTION, getIcon(), actions, actions[0]);
		pane.createDialog(null, getTitle()).setVisible(true);

		// store support revision
		support.setValue(String.valueOf(currentRev));

		// open URI of OK
		if (pane.getValue() == actions[0]) {
			openURI(getURI());
		}
	}

	abstract String getMessage(int renameCount);

	abstract String[] getActions(boolean first);

	abstract Icon getIcon();

	abstract String getTitle();

	abstract String getURI();

	public static void maybeShow() {
		int renameCount = HistorySpooler.getInstance().getPersistentHistoryTotalSize();

		// show donation / review reminders to power users (more than 2000 renames)
		if (renameCount >= 2000 && Math.random() >= 0.777) {
			if (isAppStore()) {
				AppStoreReview.show(renameCount);
			} else {
				Donation.show(renameCount);
			}
		}
	}

}
