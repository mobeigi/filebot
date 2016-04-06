package net.filebot.ui.rename;

import static java.util.Collections.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import net.filebot.Language;
import net.filebot.Settings;
import net.filebot.StandardRenameAction;
import net.filebot.WebServices;
import net.filebot.format.ExpressionFileFilter;
import net.filebot.format.ExpressionFilter;
import net.filebot.format.ExpressionFormat;
import net.filebot.mac.MacAppUtilities;
import net.filebot.util.FileUtilities;
import net.filebot.web.Datasource;
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

	public Preset(String name, File path, ExpressionFilter includes, ExpressionFormat format, Datasource database, SortOrder sortOrder, String matchMode, Language language, StandardRenameAction action) {
		this.name = name;
		this.path = path == null ? null : path.getPath();
		this.includes = includes == null ? null : includes.getExpression();
		this.format = format == null ? null : format.getExpression();
		this.database = database == null ? null : database.getIdentifier();
		this.sortOrder = sortOrder == null ? null : sortOrder.name();
		this.matchMode = matchMode == null ? null : matchMode;
		this.language = language == null ? null : language.getCode();
		this.action = action == null ? null : action.name();
	}

	public String getName() {
		return name;
	}

	public File getInputFolder() {
		return path == null || path.isEmpty() ? null : new File(path);
	}

	public ExpressionFileFilter getIncludeFilter() {
		try {
			return path == null || path.isEmpty() || includes == null || includes.isEmpty() ? null : new ExpressionFileFilter(new ExpressionFilter(includes), false);
		} catch (Exception e) {
			return null;
		}
	}

	public ExpressionFormat getFormat() {
		try {
			return format == null || format.isEmpty() ? null : new ExpressionFormat(format);
		} catch (Exception e) {
			return null;
		}
	}

	public List<File> selectInputFiles(ActionEvent evt) {
		File folder = getInputFolder();
		ExpressionFileFilter filter = getIncludeFilter();

		if (folder == null) {
			return null;
		}

		if (Settings.isMacSandbox()) {
			if (!MacAppUtilities.askUnlockFolders(getWindow(evt.getSource()), singleton(getInputFolder()))) {
				throw new IllegalStateException("Unable to access folder: " + folder);
			}
		}

		List<File> files = FileUtilities.listFiles(getInputFolder());
		if (filter != null) {
			files = FileUtilities.filter(files, filter);
		}
		return files;
	}

	public AutoCompleteMatcher getAutoCompleteMatcher() {
		MovieIdentificationService mdb = WebServices.getMovieIdentificationService(database);
		if (mdb != null) {
			return new MovieMatcher(mdb);
		}

		EpisodeListProvider sdb = WebServices.getEpisodeListProvider(database);
		if (sdb != null) {
			return new EpisodeListMatcher(sdb, sdb == WebServices.AniDB);
		}

		MusicIdentificationService adb = WebServices.getMusicIdentificationService(database);
		if (adb != null) {
			return new MusicMatcher(adb);
		}

		if (PlainFileMatcher.getInstance().getIdentifier().equals(database)) {
			return PlainFileMatcher.getInstance();
		}

		throw new IllegalStateException(database);
	}

	public Datasource getDatasource() {
		if (database == null || database.isEmpty()) {
			return null;
		}

		MovieIdentificationService mdb = WebServices.getMovieIdentificationService(database);
		if (mdb != null) {
			return mdb;
		}

		EpisodeListProvider sdb = WebServices.getEpisodeListProvider(database);
		if (sdb != null) {
			return sdb;
		}

		MusicIdentificationService adb = WebServices.getMusicIdentificationService(database);
		if (adb != null) {
			return adb;
		}

		if (PlainFileMatcher.getInstance().getIdentifier().equals(database)) {
			return PlainFileMatcher.getInstance();
		}

		throw new IllegalStateException(database);
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

	public Language getLanguage() {
		return language == null || language.isEmpty() ? null : Language.getLanguage(language);
	}

	public StandardRenameAction getRenameAction() {
		try {
			return StandardRenameAction.forName(action);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return name;
	}

}
