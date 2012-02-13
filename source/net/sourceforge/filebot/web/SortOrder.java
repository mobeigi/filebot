
package net.sourceforge.filebot.web;


public enum SortOrder {
	Airdate,
	DVD,
	Absolute;
	
	public static SortOrder forName(String name) {
		for (SortOrder order : SortOrder.values()) {
			if (order.name().equalsIgnoreCase(name)) {
				return order;
			}
		}
		
		throw new IllegalArgumentException("Invalid SortOrder: " + name);
	}
	
	
	@Override
	public String toString() {
		return String.format("%s Order", name());
	}
}
