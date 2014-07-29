package net.filebot;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.filebot.Settings.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.swing.JFileChooser;

import net.filebot.util.FileUtilities.ExtensionFileFilter;

public class UserFiles {

	private static FileChooser defaultFileChooser = getPreferredFileChooser();

	public static void setDefaultFileChooser(FileChooser fileChooser) {
		defaultFileChooser = fileChooser;
	}

	public static List<File> showLoadDialogSelectFiles(boolean folderMode, boolean multiSelection, File defaultFile, ExtensionFileFilter filter, String title, Object parent) {
		List<File> files = defaultFileChooser.showLoadDialogSelectFiles(folderMode, multiSelection, getFileChooserDefaultFile(KEY_OPEN, defaultFile), filter, title, parent);
		if (files.size() > 0) {
			setFileChooserDefaultFile(KEY_OPEN, files.get(0));
		}
		return files;
	}

	public static File showSaveDialogSelectFile(boolean folderMode, File defaultFile, String title, Object parent) {
		File file = defaultFileChooser.showSaveDialogSelectFile(folderMode, getFileChooserDefaultFile(KEY_SAVE, defaultFile), title, parent);
		if (file != null) {
			setFileChooserDefaultFile(KEY_SAVE, file);
		}
		return file;
	}

	public static File showOpenDialogSelectFolder(File defaultFile, String title, Object parent) {
		List<File> folder = defaultFileChooser.showLoadDialogSelectFiles(true, false, defaultFile, null, title, parent);
		return folder.size() > 0 ? folder.get(0) : null;
	}

	private static final String PREF_KEY_PREFIX = "file.dialog.";
	private static final String KEY_OPEN = "open";
	private static final String KEY_SAVE = "save";

	protected static File getFileChooserDefaultFile(String name, File userValue) {
		if (userValue != null && userValue.getParentFile() != null)
			return userValue;

		String path = Settings.forPackage(UserFiles.class).get(PREF_KEY_PREFIX + name);
		if (path == null || path.isEmpty())
			return userValue;

		if (userValue != null && userValue.getParentFile() == null)
			return new File(new File(path).getParentFile(), userValue.getName());

		return new File(path);
	}

	protected static void setFileChooserDefaultFile(String name, File file) {
		Settings.forPackage(UserFiles.class).put(PREF_KEY_PREFIX + name, file.getPath());
	}

	public enum FileChooser {

		Swing {
			@Override
			public List<File> showLoadDialogSelectFiles(boolean folderMode, boolean multiSelection, File defaultFile, ExtensionFileFilter filter, String title, Object parent) {
				JFileChooser chooser = new JFileChooser();
				chooser.setMultiSelectionEnabled(multiSelection);
				chooser.setFileSelectionMode(folderMode && filter == null ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_AND_DIRECTORIES);
				chooser.setSelectedFile(defaultFile);
				if (filter != null) {
					chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(filter.toString(), filter.extensions()));
				}

				if (chooser.showOpenDialog(getWindow(parent)) == JFileChooser.APPROVE_OPTION) {
					if (chooser.getSelectedFiles().length > 0)
						return asList(chooser.getSelectedFiles());
					if (chooser.getSelectedFile() != null)
						return asList(chooser.getSelectedFile());
				}
				return asList(new File[0]);
			}

			@Override
			public File showSaveDialogSelectFile(boolean folderMode, File defaultFile, String title, Object parent) {
				JFileChooser chooser = new JFileChooser();
				chooser.setMultiSelectionEnabled(false);
				chooser.setFileSelectionMode(folderMode ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_AND_DIRECTORIES);
				chooser.setSelectedFile(defaultFile);

				if (chooser.showSaveDialog(getWindow(parent)) != JFileChooser.APPROVE_OPTION) {
					return null;
				}
				return chooser.getSelectedFile();
			}
		},

		AWT {
			@Override
			public List<File> showLoadDialogSelectFiles(boolean folderMode, boolean multiSelection, File defaultFile, ExtensionFileFilter filter, String title, Object parent) {
				FileDialog fileDialog = createFileDialog(parent, title, FileDialog.LOAD, folderMode);
				fileDialog.setMultipleMode(multiSelection);
				if (filter != null) {
					fileDialog.setFilenameFilter(filter);
				}
				if (defaultFile != null) {
					fileDialog.setFile(defaultFile.getPath());
				}

				fileDialog.setVisible(true);
				return asList(fileDialog.getFiles());
			}

			@Override
			public File showSaveDialogSelectFile(boolean folderMode, File defaultFile, String title, Object parent) {
				FileDialog fileDialog = createFileDialog(getWindow(parent), title, FileDialog.SAVE, folderMode);
				fileDialog.setMultipleMode(false);
				if (defaultFile != null) {
					if (defaultFile.getParentFile() != null) {
						fileDialog.setDirectory(defaultFile.getParentFile().getPath());
					}
					fileDialog.setFile(defaultFile.getName());
				}

				fileDialog.setVisible(true);
				File[] files = fileDialog.getFiles();
				return files.length > 0 ? files[0] : null;
			}

			public FileDialog createFileDialog(Object parent, String title, int mode, boolean fileDialogForDirectories) {
				// By default, the AWT File Dialog lets you choose a file. Under certain circumstances, however, it may be proper for you to choose a directory instead. If that is the case, set this property to allow for directory selection in a file dialog.
				System.setProperty("apple.awt.fileDialogForDirectories", String.valueOf(fileDialogForDirectories));

				if (parent instanceof Frame) {
					return new FileDialog((Frame) parent, title, mode);
				}
				if (parent instanceof Dialog) {
					return new FileDialog((Dialog) parent, title, mode);
				}

				Frame[] frames = Frame.getFrames();
				return new FileDialog(frames.length > 0 ? frames[0] : null, title, mode);
			}
		},

		JavaFX {

			@Override
			public List<File> showLoadDialogSelectFiles(final boolean folderMode, final boolean multiSelection, final File defaultFile, final ExtensionFileFilter filter, final String title, final Object parent) {
				return runAndWait(new Callable<List<File>>() {

					@Override
					public List<File> call() throws Exception {
						javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
						fileChooser.setTitle(title);
						if (filter != null) {
							String[] globFilter = filter.extensions();
							for (int i = 0; i < globFilter.length; i++) {
								globFilter[i] = "*." + globFilter[i];
							}
							fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(filter.toString(), globFilter));
						}

						if (defaultFile != null) {
							if (defaultFile.getParentFile() != null) {
								fileChooser.setInitialDirectory(defaultFile.getParentFile());
							}
							fileChooser.setInitialFileName(defaultFile.getName());
						}

						if (multiSelection) {
							List<File> files = fileChooser.showOpenMultipleDialog(null);
							if (files != null)
								return files;
						} else {
							File file = fileChooser.showOpenDialog(null);
							if (file != null)
								return singletonList(file);
						}
						return emptyList();
					}

				});
			}

			@Override
			public File showSaveDialogSelectFile(final boolean folderMode, final File defaultFile, final String title, final Object parent) {
				return runAndWait(new Callable<File>() {

					@Override
					public File call() throws Exception {
						javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
						fileChooser.setTitle(title);

						if (defaultFile != null) {
							if (defaultFile.getParentFile() != null) {
								fileChooser.setInitialDirectory(defaultFile.getParentFile());
							}
							fileChooser.setInitialFileName(defaultFile.getName());
						}

						return fileChooser.showSaveDialog(null);
					}

				});
			}

			public <T> T runAndWait(Callable<T> c) {
				try {
					// initialize JavaFX
					new javafx.embed.swing.JFXPanel();

					// run on FX Thread
					FutureTask<T> task = new FutureTask<T>(c);
					javafx.application.Platform.runLater(task);
					return task.get();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};

		public abstract List<File> showLoadDialogSelectFiles(boolean folderMode, boolean multiSelection, File defaultFile, ExtensionFileFilter filter, String title, Object parent);

		public abstract File showSaveDialogSelectFile(boolean folderMode, File defaultFile, String title, Object parent);

	}

}
