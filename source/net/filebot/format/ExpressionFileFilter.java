package net.filebot.format;

import static net.filebot.Logging.*;
import static net.filebot.media.XattrMetaInfo.*;

import java.io.File;
import java.io.FileFilter;

import javax.script.ScriptException;

public class ExpressionFileFilter implements FileFilter {

	private ExpressionFilter filter;

	public ExpressionFileFilter(String expression) throws ScriptException {
		this.filter = new ExpressionFilter(expression);
	}

	public ExpressionFilter getExpressionFilter() {
		return filter;
	}

	@Override
	public boolean accept(File f) {
		try {
			return filter.matches(new MediaBindingBean(xattr.getMetaInfo(f), f));
		} catch (Exception e) {
			debug.warning("Filter expression failed: " + e);
		}
		return false;
	}

}
