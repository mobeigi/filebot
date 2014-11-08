package net.filebot.mac;

import static ca.weblite.objc.RuntimeUtils.*;
import static ca.weblite.objc.util.CocoaUtils.*;

import java.awt.FileDialog;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ca.weblite.objc.NSObject;
import ca.weblite.objc.Proxy;

import com.sun.jna.Pointer;

/**
 * A wrapper around NSSavePanel and NSOpenPanel with some methods similar to java.awt.FileDialog.
 * 
 * <p>
 * This class includes wrappers for most settings of both NSSavePanel and NSOpen panel so that you have full flexibility (e.g. select directories only, files only, force a certain extension, allow user to add folders, show hidden files, etc...
 * </p>
 * 
 * <h3>Example Save Panel</h3> <code><pre>
 *  NativeFileDialog dlg = new NativeFileDialog("Save as...", FileDialog.SAVE);
    dlg.setMessage("Will save only as .txt file");
    dlg.setExtensionHidden(true);
    dlg.setAllowedFileTypes(Arrays.asList("txt"));
    dlg.setVisible(true);  // this is modal

    String f = dlg.getFile();
    if ( f == null ){
        return;
    }
    File file = new File(f);
    saveFile(file);
 * </pre></code>
 * 
 * <h3>Example Open Panel</h3>
 * 
 * <code><pre>
 *  NativeFileDialog dlg = new NativeFileDialog("Select file to open", FileDialog.LOAD);
    dlg.setVisible(true); // this is modal.
    String f = dlg.getFile();
    if ( f != null ){
        openFile(new File(f));
    }
 * </pre></code>
 * 
 * @author shannah
 * @see <a href="https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html">NSSavePanel Class Reference</a>
 * @see <a href="https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nsopenpanel_Class/Reference/Reference.html">NSOpenPanel Class Reference</a>
 * @see <a href="http://docs.oracle.com/javase/7/docs/api/java/awt/FileDialog.html">java.awt.FileDialog API docs</a>
 */
public class NativeFileDialog extends NSObject {

	/**
	 * Reference to the Obj-C NSOpenPanel or NSSavePanel class.
	 */
	private Proxy peer;

	/**
	 * Either FileDialog.LOAD or FileDialog.SAVE
	 */
	private int mode;

	/**
	 * Creates a new file dialog with the specified title and mode.
	 * 
	 * @param title
	 *            The title for the dialog.
	 * @param mode
	 *            Whether to be an open panel or save panel. Either java.awt.FileDialog.SAVE or java.awt.FileDialog.LOAD
	 */
	public NativeFileDialog(final String title, final int mode) {
		super("NSObject");
		this.mode = mode;
		dispatch_sync(new Runnable() {

			@Override
			public void run() {
				if (mode == FileDialog.LOAD) {
					peer = getClient().sendProxy("NSOpenPanel", "openPanel");
				} else {
					peer = getClient().sendProxy("NSSavePanel", "savePanel");
				}
				peer.send("setTitle:", title);
				peer.send("retain");
			}

		});
	}

	/**
	 * Sets a given selector with a string value on the main thread.
	 * 
	 * @param selector
	 *            An objective-c selector on the NSSavePanel object. e.g. "setTitle:"
	 * @param value
	 *            The value to set the selector.
	 */
	private void set(final String selector, final String value) {
		dispatch_sync(new Runnable() {

			@Override
			public void run() {
				if (peer.getPeer().equals(Pointer.NULL)) {

					throw new RuntimeException("The peer is null");
				}
				peer.send(sel(selector), value);
			}

		});
	}

	/**
	 * Sets a given selector with an int value on the main thread.
	 * 
	 * @param selector
	 *            An objective-c selector on the NSSavePanel object. e.g. "setShowsHiddenFiles:"
	 * @param value
	 *            The int value to set.
	 */
	private void set(final String selector, final int value) {
		dispatch_sync(new Runnable() {

			@Override
			public void run() {
				peer.send(selector, value);
			}

		});
	}

	/**
	 * Returns the result of a selector on the main thread for the NSSavePanel object.
	 * 
	 * @param selector
	 *            The selector to be run. e.g. "title".
	 * @return The result of calling the given selector on the NSSavePanel object.
	 */
	private String getString(final String selector) {
		final String[] out = new String[1];
		dispatch_sync(new Runnable() {

			@Override
			public void run() {
				out[0] = peer.sendString(selector);
			}

		});
		return out[0];
	}

	/**
	 * Returns the result of running a selector on the NSSavePanel on the main thread.
	 * 
	 * @param selector
	 *            The selector to be run. E.g. "showsHiddenFiles"
	 * @return The int result.
	 */
	private int getI(final String selector) {
		final int[] out = new int[1];
		dispatch_sync(new Runnable() {

			@Override
			public void run() {
				out[0] = peer.sendInt(sel(selector));
			}

		});
		return out[0];
	}

	/**
	 * Sets title of the dialog.
	 * 
	 * @param title
	 *            The title.
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 */
	public void setTitle(String title) {
		set("setTitle:", title);
	}

	/**
	 * Gets the title of the dialog.
	 * 
	 * @return The title
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 */
	public String getTitle() {
		return getString("title");
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @param prompt
	 */
	public void setPrompt(String prompt) {
		set("setPrompt:", prompt);
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @param label
	 */
	public void setNameFieldLabel(String label) {
		set("setNameFieldLabel:", label);
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @return
	 */
	public String getNameFieldLabel() {
		return getString("nameFieldLabel");
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @param message
	 */
	public void setMessage(String message) {
		set("setMessage:", message);
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @return
	 */
	public String getMessage() {
		return getString("message");
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @return
	 */
	public String getPrompt() {
		return getString("prompt");
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @return
	 */
	public boolean canCreateDirectories() {
		return getI("canCreateDirectories") != 0;
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @param can
	 */
	public void setCanCreateDirectories(boolean can) {
		set("setCanCreateDirectories:", can ? 1 : 0);
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @return
	 */
	public boolean showsHiddenFiles() {
		return getI("showsHiddenFiles") != 0;
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @param shows
	 */
	public void setShowsHiddenFiles(boolean shows) {
		set("setShowsHiddenFiles:", shows ? 1 : 0);
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @return
	 */
	public boolean isExtensionHidden() {
		return getI("isExtensionHidden") != 0;
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @param hidden
	 */
	public void setExtensionHidden(boolean hidden) {
		set("setExtensionHidden:", hidden ? 1 : 0);
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @return
	 */
	public boolean canSelectHiddenExtension() {
		return getI("canSelectHiddenExtension") != 0;
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @param sel
	 */
	public void setCanSelectHiddenExtension(boolean sel) {
		set("setCanSelectHiddenExtension:", sel ? 1 : 0);
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @param val
	 */
	public void setNameFieldStringValue(String val) {
		set("setNameFieldStringValue:", val);
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @return
	 */
	public List<String> getAllowedFileTypes() {
		final List<String> out = new ArrayList<String>();
		dispatch_sync(new Runnable() {

			@Override
			public void run() {
				Proxy types = peer.sendProxy("allowedFileTypes");
				int size = types.getInt("count");
				for (int i = 0; i < size; i++) {
					String nex = types.sendString("objectAtIndex:", i);
					out.add(nex);
				}
			}

		});

		return out;
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @param types
	 */
	public void setAllowedFileTypes(final List<String> types) {
		dispatch_sync(new Runnable() {

			@Override
			public void run() {
				Proxy mutableArray = getClient().sendProxy("NSMutableArray", "arrayWithCapacity:", types.size());

				for (String type : types) {

					mutableArray.send("addObject:", type);

				}

				peer.send("setAllowedFileTypes:", mutableArray);

				// mutableArray.send("release");
			}

		});
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @return
	 */
	public boolean allowsOtherFileTypes() {
		return getI("allowsOtherFileTypes") != 0;
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @param allowed
	 */
	public void setAllowsOtherFileTypes(boolean allowed) {
		set("setAllowsOtherFileTypes:", allowed ? 1 : 0);
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @return
	 */
	public boolean getTreatsFilePackagesAsDirectories() {
		return getI("treatsFilePackagesAsDirectories") != 0;
	}

	/**
	 * @see https://developer.apple.com/library/mac/#documentation/Cocoa/Reference/ApplicationKit/Classes/nssavepanel_Class/Reference/Reference.html
	 * @param treat
	 */
	public void setTreatsFilePackagesAsDirectories(boolean treat) {
		set("setTreatsFilePackagesAsDirectories:", treat ? 1 : 0);
	}

	/**
	 * Returns the path to the directory that was selected.
	 * 
	 * @return
	 */
	public String getDirectory() {
		final String[] out = new String[1];
		dispatch_sync(new Runnable() {

			@Override
			public void run() {
				Proxy dirUrl = peer.sendProxy("directoryURL");
				if (dirUrl.getPeer().equals(Pointer.NULL)) {
					out[0] = null;
				} else {
					out[0] = dirUrl.sendString("path");
				}

			}

		});
		return out[0];
	}

	/**
	 * Gets the path to the file that was selected.
	 * 
	 * @return
	 */
	public String getFile() {
		final String[] out = new String[1];
		dispatch_sync(new Runnable() {

			@Override
			public void run() {
				Proxy fileUrl = peer.sendProxy("URL");
				if (fileUrl == null || fileUrl.getPeer().equals(Pointer.NULL)) {
					out[0] = null;
				} else {
					out[0] = fileUrl.sendString("path");
				}
			}

		});
		return out[0];
	}

	/**
	 * Returns an array of files that were selected by the user.
	 * 
	 * @return
	 */
	public File[] getFiles() {
		final List<File> out = new ArrayList<File>();
		dispatch_sync(new Runnable() {

			@Override
			public void run() {
				if (mode == FileDialog.LOAD) {
					Proxy nsArray = peer.getProxy("URLs");
					if (!nsArray.getPeer().equals(Pointer.NULL)) {
						int size = nsArray.sendInt("count");
						for (int i = 0; i < size; i++) {
							Proxy url = nsArray.sendProxy("objectAtIndex:", i);
							String path = url.sendString("path");
							out.add(new File(path));
						}
					}
				}
			}

		});
		return out.toArray(new File[0]);
	}

	/**
	 * Returns the mode of this dialog.
	 * 
	 * @return either FileDialog.LOAD or FileDialog.SAVE
	 */
	public int getMode() {
		return mode;
	}

	/**
	 * Returns true if the dialog allows multiple selection.
	 * 
	 * @return
	 */
	public boolean isMultipleMode() {
		return getI("allowsMultipleSelection") != 0;
	}

	/**
	 * Sets whether the dialog allows the user to select multiple files or not.
	 * 
	 * @param enable
	 */
	public void setMultipleMode(boolean enable) {
		set("setAllowsMultipleSelection:", enable ? 1 : 0);
	}

	/**
	 * Returns whether the user can select files in this dialog.
	 * 
	 * @return
	 */
	public boolean canChooseFiles() {
		return getI("canChooseFiles") != 0;
	}

	/**
	 * Sets whether the user can select files in this dialog.
	 * 
	 * @param can
	 */
	public void setCanChooseFiles(boolean can) {
		set("setCanChooseFiles:", can ? 1 : 0);
	}

	public boolean getCanChooseDirectories() {
		return getI("canChooseDirectories") != 0;
	}

	public void setCanChooseDirectories(boolean can) {
		set("setCanChooseDirectories:", can ? 1 : 0);
	}

	public boolean getResolvesAliases() {
		return getI("resolvesAliases") != 0;
	}

	public void setResolvesAliases(boolean resolves) {
		set("setResolvesAliases:", resolves ? 1 : 0);
	}

	/**
	 * Sets the directory that the dialog displays.
	 * 
	 * @param dir
	 */
	public void setDirectory(final String dir) {
		dispatch_sync(new Runnable() {

			@Override
			public void run() {
				Proxy url = getClient().sendProxy("NSURL", "fileURLWithPath:isDirectory:", dir, 1);
				peer.send("setDirectoryURL:", url.getPeer());
			}

		});
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}

	public void setVisible(boolean visible) {
		dispatch_sync(new Runnable() {

			@Override
			public void run() {
				int result = peer.sendInt("runModal");
				nsFileHandlingButton.set(result);
			}

		});
	}

	public int getResult() {
		return nsFileHandlingButton.get();
	}

	public boolean isCancelled() {
		return nsFileHandlingButton.get() == NSFileHandlingPanelCancelButton;
	}

	private final AtomicInteger nsFileHandlingButton = new AtomicInteger(NSFileHandlingPanelCancelButton);

	public static final int NSFileHandlingPanelOKButton = 1;
	public static final int NSFileHandlingPanelCancelButton = 0;

}
