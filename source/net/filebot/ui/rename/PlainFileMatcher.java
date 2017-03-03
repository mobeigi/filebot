package net.filebot.ui.rename;

import static java.util.stream.Collectors.*;

import java.awt.Component;
import java.io.File;
import java.util.Collection;
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
	public String getIdentifier() {
		return "file";
	}

	@Override
	public String getName() {
		return "Plain File";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.generic");
	}

	@Override
	public List<Match<File, ?>> match(Collection<File> files, boolean strict, SortOrder order, Locale locale, boolean autodetection, Component parent) throws Exception {
		return files.stream().map(f -> {
			return new Match<File, File>(f, f);
		}).collect(toList());
	}

}
