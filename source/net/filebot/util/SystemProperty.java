package net.filebot.util;

import static net.filebot.Logging.*;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

public class SystemProperty<T> {

	public static <T> SystemProperty<T> of(String key, Function<String, T> valueFunction, T defaultValue) {
		return new SystemProperty<T>(key, valueFunction, () -> defaultValue);
	}

	public static <T> SystemProperty<T> of(String key, Function<String, T> valueFunction) {
		return new SystemProperty<T>(key, valueFunction, null);
	}

	public static <T> SystemProperty<T> of(String key, Function<String, T> valueFunction, Supplier<T> defaultValue) {
		return new SystemProperty<T>(key, valueFunction, defaultValue);
	}

	private final String key;
	private final Function<String, T> valueFunction;
	private final Supplier<T> defaultValueFunction;

	private SystemProperty(String key, Function<String, T> valueFunction, Supplier<T> defaultValue) {
		this.key = key;
		this.valueFunction = valueFunction;
		this.defaultValueFunction = defaultValue;
	}

	public T get() {
		String prop = System.getProperty(key);

		if (prop != null && prop.length() > 0) {
			try {
				return valueFunction.apply(prop);
			} catch (Exception e) {
				log.logp(Level.WARNING, "SystemProperty", key, e.toString());
			}
		}

		return defaultValueFunction == null ? null : defaultValueFunction.get();
	}

	public Optional<T> optional() {
		return Optional.ofNullable(get());
	}

}
