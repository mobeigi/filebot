package net.filebot.ui.transfer;

import static net.filebot.UserFiles.*;
import static net.filebot.ui.NotificationLogging.*;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import net.filebot.ResourceManager;
import net.filebot.Settings;
import net.filebot.ui.transfer.TransferablePolicy.TransferAction;

public class LoadAction extends AbstractAction {

	public static final String TRANSFERABLE_POLICY = "transferablePolicy";

	public LoadAction(TransferablePolicy transferablePolicy) {
		this("Load", ResourceManager.getIcon("action.load"), transferablePolicy);
	}

	public LoadAction(String name, Icon icon, TransferablePolicy transferablePolicy) {
		putValue(NAME, name);
		putValue(SMALL_ICON, icon);
		putValue(TRANSFERABLE_POLICY, transferablePolicy);
	}

	public TransferAction getTransferAction(ActionEvent evt) {
		// if CTRL was pressed when the button was clicked, assume ADD action (same as with dnd)
		return ((evt.getModifiers() & ActionEvent.CTRL_MASK) != 0) ? TransferAction.ADD : TransferAction.PUT;
	}

	protected File getDefaultFolder() {
		String lastLocation = Settings.forPackage(LoadAction.class).get("load.location");

		if (lastLocation == null || lastLocation.isEmpty())
			return null;

		return new File(lastLocation);
	}

	protected void setDefaultFolder(File folder) {
		Settings.forPackage(LoadAction.class).put("load.location", folder.getPath());
	}

	public void actionPerformed(ActionEvent evt) {
		try {
			// get transferable policy from action properties
			TransferablePolicy transferablePolicy = (TransferablePolicy) getValue(TRANSFERABLE_POLICY);
			if (transferablePolicy == null) {
				return;
			}

			List<File> files = showLoadDialogSelectFiles(true, true, getDefaultFolder(), new TransferablePolicyFileFilter(transferablePolicy), (String) getValue(Action.NAME), evt.getSource());
			if (files.isEmpty()) {
				return;
			}

			FileTransferable transferable = new FileTransferable(files);
			if (transferablePolicy.accept(transferable)) {
				transferablePolicy.handleTransferable(transferable, getTransferAction(evt));
			}
		} catch (Exception e) {
			UILogger.log(Level.WARNING, e.toString(), e);
		}
	}

}
