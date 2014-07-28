package net.filebot.ui.transfer;

import static net.filebot.UserFiles.*;
import static net.filebot.util.FileUtilities.*;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import net.filebot.ResourceManager;
import net.filebot.Settings;

public class SaveAction extends AbstractAction {

	public static final String EXPORT_HANDLER = "exportHandler";

	public SaveAction(FileExportHandler exportHandler) {
		this("Save as ...", ResourceManager.getIcon("action.save"), exportHandler);
	}

	public SaveAction(String name, Icon icon, FileExportHandler exportHandler) {
		putValue(NAME, name);
		putValue(SMALL_ICON, icon);
		putValue(EXPORT_HANDLER, exportHandler);
	}

	public FileExportHandler getExportHandler() {
		return (FileExportHandler) getValue(EXPORT_HANDLER);
	}

	protected boolean canExport() {
		return getExportHandler().canExport();
	}

	protected void export(File file) throws IOException {
		getExportHandler().export(file);
	}

	protected String getDefaultFileName() {
		return getExportHandler().getDefaultFileName();
	}

	protected File getDefaultFolder() {
		String lastLocation = Settings.forPackage(SaveAction.class).get("save.location");

		if (lastLocation == null || lastLocation.isEmpty())
			return null;

		return new File(lastLocation);
	}

	protected void setDefaultFolder(File folder) {
		Settings.forPackage(LoadAction.class).put("save.location", folder.getPath());
	}

	public void actionPerformed(ActionEvent evt) {
		try {
			if (canExport()) {
				File defaultFile = new File(getDefaultFolder(), validateFileName(getDefaultFileName()));
				File file = showSaveDialogSelectFile(false, defaultFile, (String) getValue(Action.NAME), evt.getSource());

				if (file != null) {
					setDefaultFolder(file.getParentFile());
					export(file);
				}
			}
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.toString(), e);
		}
	}

}
