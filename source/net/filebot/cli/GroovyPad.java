package net.filebot.cli;

import static net.filebot.util.ui.SwingUI.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalExclusionType;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
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

import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;

import net.filebot.ResourceManager;
import net.filebot.Settings;
import net.filebot.Settings.ApplicationFolder;
import net.filebot.util.TeePrintStream;

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
		tools.add(run);
		tools.add(cancel);
		c.add(tools, BorderLayout.NORTH);

		run.setEnabled(true);
		cancel.setEnabled(false);

		installAction(c, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), run);
		installAction(c, KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK), run);

		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(WindowEvent evt) {
				cancel.actionPerformed(null);
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
		output = new TextEditorPane(TextEditorPane.INSERT_MODE, false);
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
		File pad = ApplicationFolder.AppData.resolve(name);
		if (!pad.exists()) {
			// use this default value so people can easily submit bug reports with fn:sysinfo logs
			ScriptShellMethods.saveAs("runScript 'sysinfo'", pad);
		}
		return FileLocation.create(pad);
	}

	protected final ScriptShell shell;

	protected ScriptShell createScriptShell() {
		try {
			return new ScriptShell(s -> ScriptSource.GITHUB_STABLE.getScriptProvider(s).getScript(s), new HashMap<String, Object>());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected final Action run = newAction("Run", ResourceManager.getIcon("script.go"), this::runScript);
	protected final Action cancel = newAction("Cancel", ResourceManager.getIcon("script.cancel"), this::cancelScript);

	private Runner currentRunner = null;

	protected void runScript(ActionEvent evt) {
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
					run.setEnabled(true);
					cancel.setEnabled(false);
				}
			};

			run.setEnabled(false);
			cancel.setEnabled(true);
			currentRunner.execute();
		}
	}

	protected void cancelScript(ActionEvent evt) {
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
					run.setEnabled(true);
					cancel.setEnabled(false);
				}
			};

			run.setEnabled(false);
			cancel.setEnabled(true);
			currentRunner.execute();
		}
	}

	protected class Runner extends SwingWorker<Object, Object> {

		private Thread executionThread;
		private Object result;

		public Runner(final String script) {
			executionThread = new Thread("GroovyPadRunner") {

				@Override
				public void run() {
					try {
						Bindings bindings = new SimpleBindings();
						bindings.put(ScriptShell.SHELL_ARGV_BINDING_NAME, Settings.getApplicationArguments().getArray());
						bindings.put(ScriptShell.ARGV_BINDING_NAME, Settings.getApplicationArguments().getFiles(false));

						result = shell.evaluate(script, bindings);

						// print result and make sure to flush Groovy output
						SimpleBindings resultBindings = new SimpleBindings();
						resultBindings.put("result", result);
						if (result != null) {
							shell.evaluate("print('Result: '); println(result);", resultBindings);
						} else {
							shell.evaluate("println();", resultBindings);
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

			@Override
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

}
