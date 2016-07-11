package net.filebot.gio;

import com.sun.jna.Library;

interface LibC extends Library {

	int getuid();

}
