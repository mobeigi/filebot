package net.filebot.web;

import javax.swing.Icon;

public interface Datasource {

	String getName();

	Icon getIcon();

	default String getIdentifier() {
		return getName();
	}

}
