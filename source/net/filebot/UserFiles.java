package net.filebot;

import static net.filebot.util.ui.TunedUtilities.*;

import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JFileChooser;

public class UserFiles {

	public static boolean useNative = Boolean.getBoolean("net.filebot.UserFiles.useNative");

	public static void useNative(boolean b) {
		useNative = b;
	}

	public static File[] showLoadDialogSelectFiles(boolean folderMode, boolean multiSelection, File defaultFile, final FilenameFilter filter, String title, Object parent) {
		if (useNative) {
			FileDialog fileDialog = createFileDialog(parent, title, FileDialog.LOAD, folderMode);

			if (defaultFile != null) {
				fileDialog.setFile(defaultFile.getPath());
			}
			if (filter != null) {
				fileDialog.setFilenameFilter(filter);
			}
			fileDialog.setMultipleMode(multiSelection);
			fileDialog.setVisible(true);

			return fileDialog.getFiles();
		}

		// use normal Swing JFileChooser by default
		JFileChooser chooser = new JFileChooser();
		if (filter != null) {
			chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

				@Override
				public String getDescription() {
					return filter.toString();
				}

				@Override
				public boolean accept(File f) {
					return f.isDirectory() || filter.accept(f.getParentFile(), f.getName());
				}
			});
		}

		chooser.setSelectedFile(defaultFile);
		chooser.setFileSelectionMode(folderMode && filter == null ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setMultiSelectionEnabled(multiSelection);

		if (chooser.showOpenDialog(getWindow(parent)) == JFileChooser.APPROVE_OPTION) {
			if (chooser.getSelectedFiles().length > 0)
				return chooser.getSelectedFiles();
			if (chooser.getSelectedFile() != null)
				return new File[] { chooser.getSelectedFile() };
		}
		return new File[0];
	}

	public static File showOpenDialogSelectFolder(File defaultFile, String title, Object parent) {
		File[] folder = showLoadDialogSelectFiles(true, false, defaultFile, null, title, parent);
		return folder.length > 0 ? folder[0] : null;
	}

	public static File showSaveDialogSelectFile(boolean folderMode, File defaultFile, String title, Object parent) {
		if (useNative) {
			FileDialog fileDialog = createFileDialog(getWindow(parent), title, FileDialog.SAVE, folderMode);

			if (defaultFile != null) {
				if (defaultFile.getParentFile() != null) {
					fileDialog.setDirectory(defaultFile.getParentFile().getPath());
				}
				fileDialog.setFile(defaultFile.getName());
			}
			fileDialog.setMultipleMode(false);
			fileDialog.setVisible(true);

			File[] files = fileDialog.getFiles();
			return files.length > 0 ? files[0] : null;
		}

		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(defaultFile);
		chooser.setFileSelectionMode(folderMode ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setMultiSelectionEnabled(false);

		if (chooser.showSaveDialog(getWindow(parent)) != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		return chooser.getSelectedFile();
	}

	public static FileDialog createFileDialog(Object parent, String title, int mode, boolean fileDialogForDirectories) {
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

}
