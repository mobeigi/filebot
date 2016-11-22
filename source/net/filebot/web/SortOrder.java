package net.filebot.web;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;

import java.util.List;

public enum SortOrder {

	Airdate, DVD, Absolute;

	@Override
	public String toString() {
		return name() + " Order";
	}

	public static List<String> names() {
		return stream(values()).map(SortOrder::name).collect(toList());
	}

	public static SortOrder forName(String name) {
		for (SortOrder order : SortOrder.values()) {
			if (order.name().equalsIgnoreCase(name)) {
				return order;
			}
		}

		throw new IllegalArgumentException("Illegal SortOrder: " + name);
	}

}
