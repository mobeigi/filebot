package net.filebot.ui.list;

import net.filebot.format.ExpressionFormat;

public class ListItem {

	private IndexedBindingBean bindings;
	private ExpressionFormat format;

	private String value;

	public ListItem(IndexedBindingBean bindings, ExpressionFormat format) {
		this.bindings = bindings;
		this.format = format;
		this.value = format != null ? null : bindings.getInfoObject().toString();
	}

	public IndexedBindingBean getBindings() {
		return bindings;
	}

	public Object getObject() {
		return bindings.getInfoObject();
	}

	public ExpressionFormat getFormat() {
		return format;
	}

	public String getFormattedValue() {
		if (value == null) {
			value = format.format(bindings);

			if (value == null && format.caughtScriptException() != null) {
				value = format.caughtScriptException().getMessage();
			}
		}
		return value;
	}

	@Override
	public String toString() {
		return getObject().toString();
	}

}
