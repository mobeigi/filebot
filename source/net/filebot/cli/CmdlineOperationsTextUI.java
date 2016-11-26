package net.filebot.cli;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.filebot.media.MediaDetection.*;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.CheckBoxList;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.LocalizedString;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Panels;
import com.googlecode.lanterna.gui2.Separator;
import com.googlecode.lanterna.gui2.Window.Hint;
import com.googlecode.lanterna.gui2.dialogs.ListSelectDialogBuilder;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;

import net.filebot.RenameAction;
import net.filebot.similarity.Match;
import net.filebot.web.SearchResult;

public class CmdlineOperationsTextUI extends CmdlineOperations {

	public static final String DEFAULT_THEME = "businessmachine";

	private Terminal terminal;
	private Screen screen;
	private MultiWindowTextGUI ui;

	public CmdlineOperationsTextUI() throws Exception {
		terminal = new DefaultTerminalFactory().setTerminalEmulatorFrameAutoCloseTrigger(TerminalEmulatorAutoCloseTrigger.CloseOnEscape).createTerminal();
		screen = new TerminalScreen(terminal);
		ui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.DEFAULT));

		// use green matrix-style theme
		ui.setTheme(LanternaThemes.getRegisteredTheme(DEFAULT_THEME));
	}

	public synchronized <T> T onScreen(Supplier<T> dialog) throws Exception {
		try {
			screen.startScreen();
			return dialog.get();
		} finally {
			screen.stopScreen();
		}
	}

	@Override
	public List<File> renameAll(Map<File, File> renameMap, RenameAction renameAction, ConflictAction conflictAction, List<Match<File, ?>> matches) throws Exception {
		// default behavior if rename map is empty
		if (renameMap.isEmpty()) {
			return super.renameAll(renameMap, renameAction, conflictAction, matches);
		}

		// manually confirm each file mapping
		Map<File, File> selection = onScreen(() -> confirmRenameMap(renameMap, renameAction, conflictAction));

		// no selection, do nothing and return successfully
		if (selection.isEmpty()) {
			return emptyList();
		}

		return super.renameAll(selection, renameAction, conflictAction, matches);
	}

	@Override
	protected List<SearchResult> selectSearchResult(String query, Collection<? extends SearchResult> options, boolean alias, boolean strict) throws Exception {
		List<SearchResult> matches = getProbableMatches(query, options, alias, false);

		if (matches.size() < 2) {
			return matches;
		}

		return onScreen(() -> confirmSearchResult(query, matches)); // manually select option if there is more than one
	}

	protected List<SearchResult> confirmSearchResult(String query, List<SearchResult> options) {
		ListSelectDialogBuilder<SearchResult> dialog = new ListSelectDialogBuilder<SearchResult>();
		dialog.setTitle("Multiple Options");
		dialog.setDescription(String.format("Select best match for \"%s\"", query));
		dialog.setExtraWindowHints(singleton(Hint.CENTERED));

		options.forEach(dialog::addListItem);

		// show UI
		SearchResult selection = dialog.build().showDialog(ui);
		if (selection == null) {
			return emptyList();
		}

		return singletonList(selection);
	}

	protected Map<File, File> confirmRenameMap(Map<File, File> renameMap, RenameAction renameAction, ConflictAction conflictAction) {
		Map<File, File> selection = new LinkedHashMap<File, File>();

		BasicWindow dialog = new BasicWindow();
		dialog.setTitle(String.format("%s / %s", renameAction, conflictAction));
		dialog.setHints(asList(Hint.MODAL, Hint.CENTERED));

		CheckBoxList<CheckBoxListItem> checkBoxList = new CheckBoxList<CheckBoxListItem>();

		int columnSize = renameMap.keySet().stream().mapToInt(f -> f.getName().length()).max().orElse(0);
		String labelFormat = "%-" + columnSize + "s\t=>\t%s";

		renameMap.forEach((k, v) -> {
			checkBoxList.addItem(new CheckBoxListItem(String.format(labelFormat, k.getName(), v.getName()), k, v), true);
		});

		Button okButton = new Button(LocalizedString.OK.toString(), () -> {
			checkBoxList.getCheckedItems().forEach(it -> selection.put(it.key, it.value));
			dialog.close();
		});

		Button cancelButton = new Button(LocalizedString.Cancel.toString(), () -> {
			selection.clear();
			dialog.close();
		});

		Panel contentPane = new Panel();
		contentPane.setLayoutManager(new GridLayout(1));

		contentPane.addComponent(checkBoxList.setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.BEGINNING, true, true, 1, 1)));
		contentPane.addComponent(new Separator(Direction.HORIZONTAL).setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.CENTER, true, false, 1, 1)));
		contentPane.addComponent(Panels.grid(2, okButton, cancelButton).setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, false, false, 1, 1)));

		dialog.setComponent(contentPane);

		ui.addWindowAndWait(dialog);

		return selection;
	}

	protected static class CheckBoxListItem {

		public final String label;

		public final File key;
		public final File value;

		public CheckBoxListItem(String label, File key, File value) {
			this.label = label;
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString() {
			return label;
		}

	}

}
