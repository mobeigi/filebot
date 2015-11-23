package net.filebot.ui.rename;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import net.filebot.ResourceManager;
import net.filebot.similarity.Match;
import net.filebot.web.Datasource;
import net.filebot.web.SortOrder;

public class PlainFileMatcher implements Datasource, AutoCompleteMatcher {

	public static final PlainFileMatcher INSTANCE = new PlainFileMatcher();

	@Override
	public String getName() {
		return "Generic File";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.generic");
	}

	@Override
	public List<Match<File, ?>> match(List<File> files, boolean strict, SortOrder order, Locale locale, boolean autodetection, Component parent) throws Exception {
		List<Match<File, ?>> matches = new ArrayList<>();
		for (File f : files) {
			matches.add(new Match<File, File>(f, f));
		}
		return matches;
	}

}
