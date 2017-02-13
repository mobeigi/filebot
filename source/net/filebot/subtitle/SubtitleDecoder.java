package net.filebot.subtitle;

import java.util.List;

public interface SubtitleDecoder {

	List<SubtitleElement> decode(String file);

}