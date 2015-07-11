package net.filebot.ui.rename;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

import net.filebot.StandardRenameAction;
import net.filebot.WebServices;
import net.filebot.format.ExpressionFormat;
import net.filebot.ui.rename.FormatDialog.Mode;
import net.filebot.web.EpisodeListProvider;
import net.filebot.web.MovieIdentificationService;
import net.filebot.web.MusicIdentificationService;
import net.filebot.web.SortOrder;

public class Preset {

	public String name;
	public String path;
	public String includes;
	public String format;
	public String database;
	public String sortOrder;
	public String matchMode;
	public String language;
	public String action;

	public Preset(String name, String path, String includes, String format, String database, String sortOrder, String matchMode, String language, String action) {
		this.name = name;
		this.path = path;
		this.includes = includes;
		this.format = format;
		this.database = database;
		this.sortOrder = sortOrder;
		this.matchMode = matchMode;
		this.language = language;
		this.action = action;
	}

	public String getName() {
		return name;
	}

	public File getInputFolder() {
		return new File(path);
	}

	public Pattern getIncludePattern() {
		return includes == null || includes.isEmpty() ? null : Pattern.compile(includes, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
	}

	public ExpressionFormat getFormat() {
		try {
			return new ExpressionFormat(format);
		} catch (Exception e) {
			return null;
		}
	}

	public AutoCompleteMatcher getAutoCompleteMatcher() {
		EpisodeListProvider sdb = WebServices.getEpisodeListProvider(database);
		if (sdb != null) {
			return new EpisodeListMatcher(sdb, sdb != WebServices.AniDB, sdb == WebServices.AniDB);
		}

		MovieIdentificationService mdb = WebServices.getMovieIdentificationService(database);
		if (mdb != null) {
			return new MovieHashMatcher(mdb);
		}

		MusicIdentificationService adb = WebServices.getMusicIdentificationService(database);
		if (adb != null) {
			return new AudioFingerprintMatcher(adb);
		}

		throw new IllegalStateException(database);
	}

	public Mode getMode() {
		EpisodeListProvider sdb = WebServices.getEpisodeListProvider(database);
		if (sdb != null) {
			return Mode.Episode;
		}

		MovieIdentificationService mdb = WebServices.getMovieIdentificationService(database);
		if (mdb != null) {
			return Mode.Movie;
		}

		MusicIdentificationService adb = WebServices.getMusicIdentificationService(database);
		if (adb != null) {
			return Mode.Music;
		}

		return Mode.File;
	}

	public String getMatchMode() {
		return matchMode == null || matchMode.isEmpty() ? null : matchMode;
	}

	public SortOrder getSortOrder() {
		try {
			return SortOrder.forName(sortOrder);
		} catch (Exception e) {
			return null;
		}
	}

	public Locale getLanguage() {
		return language == null || language.isEmpty() ? null : new Locale(language);
	}

	public StandardRenameAction getRenameAction() {
		try {
			return StandardRenameAction.forName(action);
		} catch (Exception e) {
			return null;
		}
	}

}
