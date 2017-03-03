package net.filebot.ui.rename;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import net.filebot.media.XattrMetaInfoProvider;
import net.filebot.similarity.Match;
import net.filebot.web.SortOrder;

public class XattrFileMatcher extends XattrMetaInfoProvider implements AutoCompleteMatcher {

	public static final XattrFileMatcher INSTANCE = new XattrFileMatcher();

	@Override
	public List<Match<File, ?>> match(Collection<File> files, boolean strict, SortOrder order, Locale locale, boolean autodetection, Component parent) throws Exception {
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();

		// use strict mode to exclude files that are not xattr tagged
		match(files, true).forEach((k, v) -> {
			matches.add(new Match<File, Object>(k, v));
		});

		return matches;
	}

}
