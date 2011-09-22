
package net.sourceforge.filebot.ui.rename;


import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.text.Format;

import javax.script.Bindings;
import javax.script.ScriptException;

import net.sourceforge.filebot.format.ExpressionBindings;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.format.MediaBindingBean;
import net.sourceforge.filebot.similarity.Match;


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
	public synchronized String format(Match<?, ?> match) throws ScriptException {
		// lazy initialize script engine
		if (format == null) {
			format = new ExpressionFormat(expression) {
				
				@Override
				public Bindings getBindings(Object value) {
					return new ExpressionBindings(value) {
						
						@Override
						public Object get(Object key) {
							Object value = super.get(key);
							
							// if the binding value is a String, remove illegal characters
							if (value instanceof CharSequence) {
								return replacePathSeparators(value.toString()).trim();
							}
							
							// if the binding value is an Object, just leave it
							return value;
						}
					};
				}
			};
		}
		
		// evaluate the expression using the given bindings
		Object bindingBean = new MediaBindingBean(match.getValue(), (File) match.getCandidate());
		String result = format.format(bindingBean).trim();
		
		// if result is empty, check for script exceptions
		if (result.isEmpty() && format.caughtScriptException() != null) {
			throw format.caughtScriptException();
		}
		
		return result;
	}
	
}
