package net.filebot;

import static java.util.Collections.*;
import static net.filebot.util.XPathUtilities.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilderFactory;

import net.filebot.util.FileUtilities.ExtensionFileFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class MediaTypes {

	private static MediaTypes defaultInstance;

	public static synchronized MediaTypes getDefault() {
		if (defaultInstance == null) {
			defaultInstance = parseDefault();
		}
		return defaultInstance;
	}

	private static MediaTypes parseDefault() {
		try {
			Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(MediaTypes.class.getResourceAsStream("media.types"));
			Map<String, List<String>> types = new LinkedHashMap<String, List<String>>();

			for (Node it : getChildren("type", dom.getFirstChild())) {
				List<String> extensions = new ArrayList<String>(2);
				for (Node ie : getChildren("extension", it)) {
					extensions.add(getTextContent(ie));
				}

				types.put(getAttribute("name", it), extensions);
			}

			return new MediaTypes(types);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, List<String>> types;
	private Map<String, ExtensionFileFilter> filters = synchronizedMap(new HashMap<String, ExtensionFileFilter>());

	public MediaTypes(Map<String, List<String>> types) {
		this.types = types;
	}

	public List<String> getExtensionList(String name) {
		List<String> list = new ArrayList<String>();

		for (Entry<String, List<String>> type : types.entrySet()) {
			if (type.getKey().startsWith(name)) {
				list.addAll(type.getValue());
			}
		}

		return list;
	}

	public ExtensionFileFilter getFilter(String name) {
		ExtensionFileFilter filter = filters.get(name);

		if (filter == null) {
			filter = new ExtensionFileFilter(getExtensionList(name));
			filters.put(name, filter);
		}

		return filter;
	}

	public Map<String, List<String>> getTypes() {
		return types;
	}

	public String getMediaType(String extension) {
		for (Entry<String, List<String>> it : getTypes().entrySet()) {
			if (it.getValue().contains(extension)) {
				return it.getKey();
			}
		}
		return null;
	}

	public static ExtensionFileFilter getDefaultFilter(String name) {
		return getDefault().getFilter(name);
	}

	public static ExtensionFileFilter combineFilter(ExtensionFileFilter... filters) {
		List<String> extensions = new ArrayList<String>();
		for (ExtensionFileFilter it : filters) {
			if (!it.acceptAny()) {
				addAll(extensions, it.extensions());
			}
		}
		return new ExtensionFileFilter(extensions);
	}

	// some convenience filters
	public static final ExtensionFileFilter AUDIO_FILES = getDefaultFilter("audio");
	public static final ExtensionFileFilter VIDEO_FILES = getDefaultFilter("video");
	public static final ExtensionFileFilter SUBTITLE_FILES = getDefaultFilter("subtitle");
	public static final ExtensionFileFilter ARCHIVE_FILES = getDefaultFilter("archive");
	public static final ExtensionFileFilter VERIFICATION_FILES = getDefaultFilter("verification");
	public static final ExtensionFileFilter NFO_FILES = getDefaultFilter("application/nfo");
	public static final ExtensionFileFilter LIST_FILES = getDefaultFilter("application/list");
	public static final ExtensionFileFilter TORRENT_FILES = getDefaultFilter("application/torrent");

}
