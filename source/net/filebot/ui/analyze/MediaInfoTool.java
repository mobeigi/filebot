package net.filebot.ui.analyze;

import java.io.File;

import javax.swing.table.TableModel;

class MediaInfoTool extends Tool<TableModel> {

	public MediaInfoTool() {
		super("MediaInfo");
	}

	@Override
	protected TableModel createModelInBackground(File root) throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void setModel(TableModel model) {
		// TODO Auto-generated method stub

	}

}
