package net.filebot.format;

import static net.filebot.Logging.*;
import static net.filebot.media.XattrMetaInfo.*;

import java.io.File;
import java.io.FileFilter;

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
			return filter.matches(new MediaBindingBean(xattr.getMetaInfo(f), f, null));
		} catch (Exception e) {
			debug.warning(format("Expression failed: %s", e));
			return error;
		}
	}

}
