package net.filebot.format;

import java.io.File;
import java.io.FileFilter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExpressionFileFilter implements FileFilter {

	private final ExpressionFilter filter;
	private final boolean errorResult;

	public ExpressionFileFilter(ExpressionFilter filter, boolean errorResult) {
		this.filter = filter;
		this.errorResult = errorResult;
	}

	public ExpressionFilter getExpressionFilter() {
		return filter;
	}

	@Override
	public boolean accept(File f) {
		try {
			return filter.matches(new MediaBindingBean(f, f));
		} catch (Exception e) {
			Logger.getLogger(ExpressionFileFilter.class.getName()).log(Level.WARNING, e.toString());
			return errorResult;
		}
	}

}
