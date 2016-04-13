package net.filebot.ui.rename;

import static net.filebot.Logging.*;
import static net.filebot.media.MediaDetection.*;

import java.io.File;
import java.text.Format;
import java.util.Map;
import java.util.logging.Level;

import javax.script.ScriptException;

import net.filebot.format.ExpressionFormat;
import net.filebot.format.MediaBindingBean;
import net.filebot.similarity.Match;

class ExpressionFormatter implements MatchFormatter {

	private final String expression;
	private ExpressionFormat format;

	private Format preview;
	private Class<?> target;

	public ExpressionFormatter(String expression, Format preview, Class<?> target) {
		if (expression == null || expression.isEmpty())
			throw new IllegalArgumentException("Expression must not be null or empty");

		this.expression = expression;
		this.preview = preview;
		this.target = target;
	}

	@Override
	public boolean canFormat(Match<?, ?> match) {
		// target object is required, file is optional
		return target.isInstance(match.getValue()) && (match.getCandidate() == null || match.getCandidate() instanceof File);
	}

	@Override
	public String preview(Match<?, ?> match) {
		return preview != null ? preview.format(match.getValue()) : match.getValue().toString();
	}

	@Override
	public synchronized String format(Match<?, ?> match, Map<?, ?> context) throws ScriptException {
		// lazy initialize script engine
		if (format == null) {
			format = new ExpressionFormat(expression);
		}

		// evaluate the expression using the given bindings
		Object bindingBean = new MediaBindingBean(match.getValue(), (File) match.getCandidate(), (Map) context);
		String destination = format.format(bindingBean).trim();

		// if result is empty, check for script exceptions
		if (destination.isEmpty() && format.caughtScriptException() != null) {
			throw format.caughtScriptException();
		}

		return getPath((File) match.getCandidate(), destination);
	}

	private String getPath(File source, String destination) {
		if (source == null) {
			return destination;
		}

		// resolve against parent folder
		File parent = new File(destination).getParentFile();
		if (parent == null || parent.isAbsolute() || parent.getPath().startsWith(".")) {
			return destination;
		}

		// try to resolve against structure root folder by default
		try {
			File structureRoot = getStructureRoot(source);
			if (structureRoot != null) {
				return new File(structureRoot, destination).getPath();
			}
		} catch (Exception e) {
			debug.log(Level.SEVERE, "Failed to resolve structure root: " + source, e);
		}

		// resolve against parent folder by default
		return destination;
	}

}
