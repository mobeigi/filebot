package net.sourceforge.filebot.ui.transfer;

import static net.sourceforge.filebot.ui.NotificationLogging.*;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JFileChooser;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.ui.transfer.TransferablePolicy.TransferAction;

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

	public void actionPerformed(ActionEvent evt) {
		// get transferable policy from action properties
		TransferablePolicy transferablePolicy = (TransferablePolicy) getValue(TRANSFERABLE_POLICY);
		if (transferablePolicy == null)
			return;

		JFileChooser chooser = new JFileChooser(getDefaultFolder());
		chooser.setFileFilter(new TransferablePolicyFileFilter(transferablePolicy));
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setMultiSelectionEnabled(true);

		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
			return;
		}

		FileTransferable transferable = new FileTransferable(chooser.getSelectedFiles());
		try {
			if (transferablePolicy.accept(transferable)) {
				transferablePolicy.handleTransferable(transferable, getTransferAction(evt));
			}
		} catch (Exception e) {
			UILogger.log(Level.WARNING, e.getMessage(), e);
		}

		// remember last location
		Settings.forPackage(LoadAction.class).put("load.location", chooser.getCurrentDirectory().getPath());
	}

}
