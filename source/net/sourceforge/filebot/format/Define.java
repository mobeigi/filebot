
package net.sourceforge.filebot.format;


import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


@Retention(RUNTIME)
@Target(METHOD)
public @interface Define {
	
	String[] value();
	
	static final String undefined = "";
}
