package net.filebot.format;

import static net.filebot.media.MediaDetection.*;

import java.io.File;
import java.io.FileFilter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExpressionFileFilter implements FileFilter {

	private final ExpressionFilter filter;
	private final boolean error;

	public ExpressionFileFilter(ExpressionFilter filter, boolean error) {
		this.filter = filter;
		this.error = error;
	}

	public ExpressionFilter getExpressionFilter() {
		return filter;
	}

	@Override
	public boolean accept(File f) {
		try {
			return filter.matches(new MediaBindingBean(readMetaInfo(f), f, null));
		} catch (Exception e) {
			Logger.getLogger(ExpressionFileFilter.class.getName()).log(Level.WARNING, e.toString());
			return error;
		}
	}

}
