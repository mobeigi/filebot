package net.filebot.web;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import javax.swing.Icon;

public interface MusicIdentificationService extends Datasource {

	String getName();

	Icon getIcon();

	Map<File, AudioTrack> lookup(Collection<File> files) throws Exception;

}
