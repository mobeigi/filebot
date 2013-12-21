package net.sourceforge.filebot.cli;

import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalExclusionType;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.cli.ArgumentProcessor.DefaultScriptProvider;
import net.sourceforge.tuned.TeePrintStream;

import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;

public class GroovyPad extends JFrame {

	public GroovyPad() throws IOException {
		super("Groovy Pad");

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, createEditor(), createOutputLog());
		splitPane.setResizeWeight(0.7);

		JComponent c = (JComponent) getContentPane();
		c.setLayout(new BorderLayout(0, 0));
		c.add(splitPane, BorderLayout.CENTER);

		JToolBar tools = new JToolBar("Run", JToolBar.HORIZONTAL);
		tools.setFloatable(true);
		tools.add(action_run);
		tools.add(action_cancel);
		c.add(tools, BorderLayout.NORTH);

		action_run.setEnabled(true);
		action_cancel.setEnabled(false);

		installAction(c, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), action_run);
		installAction(c, KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK), action_run);

		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(WindowEvent evt) {
				action_cancel.actionPerformed(null);
				console.unhook();

				try {
					editor.save();
				} catch (IOException e) {
					// ignore
				}
			}
		});

		console = new MessageConsole(output);
		console.hook();

		shell = createScriptShell();
		editor.requestFocusInWindow();

		setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationByPlatform(true);
		setSize(800, 600);

	}

	protected MessageConsole console;
	protected TextEditorPane editor;
	protected TextEditorPane output;

	protected JComponent createEditor() throws IOException {
		editor = new TextEditorPane(TextEditorPane.INSERT_MODE, false, getFileLocation("pad.groovy"), "UTF-8");
		editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
		editor.setAutoscrolls(false);
		editor.setAnimateBracketMatching(false);
		editor.setAntiAliasingEnabled(true);
		editor.setAutoIndentEnabled(true);
		editor.setBracketMatchingEnabled(true);
		editor.setCloseCurlyBraces(true);
		editor.setClearWhitespaceLinesEnabled(true);
		editor.setCodeFoldingEnabled(true);
		editor.setHighlightSecondaryLanguages(false);
		editor.setRoundedSelectionEdges(false);
		editor.setTabsEmulated(false);

		// restore on open
		editor.reload();

		return new RTextScrollPane(editor, true);
	}

	protected JComponent createOutputLog() throws IOException {
		output = new TextEditorPane(TextEditorPane.OVERWRITE_MODE, false);
		output.setEditable(false);
		output.setReadOnly(true);
		output.setAutoscrolls(true);
		output.setBackground(new Color(255, 255, 218));

		output.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
		output.setAnimateBracketMatching(false);
		output.setAntiAliasingEnabled(true);
		output.setAutoIndentEnabled(false);
		output.setBracketMatchingEnabled(false);
		output.setCloseCurlyBraces(false);
		output.setClearWhitespaceLinesEnabled(false);
		output.setCodeFoldingEnabled(false);
		output.setHighlightCurrentLine(false);
		output.setHighlightSecondaryLanguages(false);
		output.setRoundedSelectionEdges(false);
		output.setTabsEmulated(false);

		return new RTextScrollPane(output, true);
	}

	protected FileLocation getFileLocation(String name) throws IOException {
		File pad = new File(Settings.getApplicationFolder(), name);
		if (!pad.exists()) {
			pad.createNewFile();
		}
		return FileLocation.create(pad);
	}

	protected final ScriptShell shell;

	protected ScriptShell createScriptShell() {
		try {
			DefaultScriptProvider scriptProvider = new DefaultScriptProvider(true);
			scriptProvider.setBaseScheme(new URI("fn", "%s", null));

			return new ScriptShell(new CmdlineOperations(), new ArgumentBean(), AccessController.getContext(), scriptProvider);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected final Action action_run = new AbstractAction("Run", ResourceManager.getIcon("script.go")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			// persist script file and clear output
			try {
				editor.save();
			} catch (IOException e) {
				// won't happen
			}
			output.setText("");

			if (currentRunner == null || currentRunner.isDone()) {
				currentRunner = new Runner(editor.getText()) {

					@Override
					protected void done() {
						action_run.setEnabled(true);
						action_cancel.setEnabled(false);
					}
				};

				action_run.setEnabled(false);
				action_cancel.setEnabled(true);
				currentRunner.execute();
			}
		}
	};

	protected final Action action_cancel = new AbstractAction("Cancel", ResourceManager.getIcon("script.cancel")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			if (currentRunner != null && !currentRunner.isDone()) {
				currentRunner.cancel(true);
				currentRunner.getExecutionThread().stop();

				try {
					currentRunner.get(2, TimeUnit.SECONDS);
				} catch (Exception e) {
					// ignore
				}
			}
		}
	};

	private Runner currentRunner = null;

	protected class Runner extends SwingWorker<Object, Object> {

		private Thread executionThread;
		private Object result;

		public Runner(final String script) {
			executionThread = new Thread("GroovyPadRunner") {

				@Override
				public void run() {
					try {
						result = shell.evaluate(script, new SimpleBindings(), true);

						// print result and make sure to flush Groovy output
						SimpleBindings binding = new SimpleBindings();
						binding.put("result", result);
						if (result != null) {
							shell.evaluate("print('Result: '); println(result);", binding, true);
						} else {
							shell.evaluate("println();", binding, true);
						}
					} catch (ScriptException e) {
						while (e.getCause() instanceof ScriptException) {
							e = (ScriptException) e.getCause();
						}
						e.printStackTrace();
					} catch (Throwable e) {
						e.printStackTrace();
					}
				};
			};

			executionThread.setDaemon(false);
			executionThread.setPriority(Thread.MIN_PRIORITY);
		}

		@Override
		protected Object doInBackground() throws Exception {
			executionThread.start();
			executionThread.join();
			return result;
		}

		public Thread getExecutionThread() {
			return executionThread;
		}
	};

	public static class MessageConsole {

		private final PrintStream system_out = System.out;
		private final PrintStream system_err = System.err;

		private JTextComponent textComponent;

		public MessageConsole(JTextComponent textComponent) {
			this.textComponent = textComponent;
		}

		public void hook() {
			try {
				System.setOut(new TeePrintStream(new ConsoleOutputStream(), true, "UTF-8", system_out));
				System.setErr(new TeePrintStream(new ConsoleOutputStream(), true, "UTF-8", system_err));
			} catch (UnsupportedEncodingException e) {
				// can't happen
			}
		}

		public void unhook() {
			System.setOut(system_out);
			System.setErr(system_err);
		}

		private class ConsoleOutputStream extends ByteArrayOutputStream {

			public void flush() {
				try {
					String message = this.toString("UTF-8");
					reset();

					commit(message);
				} catch (UnsupportedEncodingException e) {
					// can't happen
				}
			}

			private void commit(final String line) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						try {
							int offset = textComponent.getDocument().getLength();
							textComponent.getDocument().insertString(offset, line, null);
							textComponent.setCaretPosition(textComponent.getDocument().getLength());
						} catch (BadLocationException e) {
							// ignore
						}
					}
				});
			}
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				try {
					GroovyPad pad = new GroovyPad();

					List<Image> images = new ArrayList<Image>(3);
					for (String i : new String[] { "window.icon.large", "window.icon.medium", "window.icon.small" }) {
						images.add(ResourceManager.getImage(i));
					}
					pad.setIconImages(images);

					// ignore analytics in developer mode
					Analytics.setEnabled(false);

					pad.setDefaultCloseOperation(EXIT_ON_CLOSE);
					pad.setVisible(true);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

}
