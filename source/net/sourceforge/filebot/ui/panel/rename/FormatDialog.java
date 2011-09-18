
package net.sourceforge.filebot.ui.panel.rename;


import static java.awt.Font.*;
import static javax.swing.BorderFactory.*;
import static net.sourceforge.filebot.ui.NotificationLogging.*;
import static net.sourceforge.tuned.ExceptionUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.Format;
import java.text.ParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.format.BindingException;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.format.MediaBindingBean;
import net.sourceforge.filebot.web.EpisodeFormat;
import net.sourceforge.filebot.web.MovieFormat;
import net.sourceforge.tuned.DefaultThreadFactory;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.PreferencesList;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;
import net.sourceforge.tuned.ui.GradientStyle;
import net.sourceforge.tuned.ui.LazyDocumentListener;
import net.sourceforge.tuned.ui.LinkButton;
import net.sourceforge.tuned.ui.ProgressIndicator;
import net.sourceforge.tuned.ui.TunedUtilities;
import net.sourceforge.tuned.ui.notification.SeparatorBorder;
import net.sourceforge.tuned.ui.notification.SeparatorBorder.Position;


class FormatDialog extends JDialog {
	
	private boolean submit = false;
	
	private Mode mode;
	private ExpressionFormat format;
	
	private MediaBindingBean sample;
	private ExecutorService executor = createExecutor();
	private RunnableFuture<String> currentPreviewFuture;
	
	private JLabel preview = new JLabel();
	private JLabel status = new JLabel();
	
	private JTextComponent editor = createEditor();
	private ProgressIndicator progressIndicator = new ProgressIndicator();
	
	private JLabel title = new JLabel();
	private JPanel help = new JPanel(new MigLayout("insets 0, nogrid, fillx"));
	
	private static final PreferencesEntry<String> persistentSampleFile = Settings.forPackage(FormatDialog.class).entry("format.sample.file");
	

	public enum Mode {
		Episode,
		Movie;
		
		public Mode next() {
			if (ordinal() < values().length - 1)
				return values()[ordinal() + 1];
			
			return values()[0];
		}
		

		public String key() {
			return this.name().toLowerCase();
		}
		

		public Format getFormat() {
			switch (this) {
				case Episode:
					return new EpisodeFormat(true, true);
				default: // case Movie
					return new MovieFormat(true, true, false);
			}
		}
		

		public PreferencesEntry<String> persistentSample() {
			return Settings.forPackage(FormatDialog.class).entry("format.sample." + key());
		}
		

		public PreferencesList<String> persistentFormatHistory() {
			return Settings.forPackage(FormatDialog.class).node("format.recent." + key()).asList();
		}
	}
	

	public FormatDialog(Window owner) {
		super(owner, ModalityType.DOCUMENT_MODAL);
		
		// initialize hidden
		progressIndicator.setVisible(false);
		
		// bold title label in header
		title.setFont(title.getFont().deriveFont(BOLD));
		
		JPanel header = new JPanel(new MigLayout("insets dialog, nogrid"));
		
		header.setBackground(Color.white);
		header.setBorder(new SeparatorBorder(1, new Color(0xB4B4B4), new Color(0xACACAC), GradientStyle.LEFT_TO_RIGHT, Position.BOTTOM));
		
		header.add(progressIndicator, "pos 1al 0al, hidemode 3");
		header.add(title, "wrap unrel:push");
		header.add(preview, "hmin 16px, gap indent, hidemode 3, wmax 90%");
		header.add(status, "hmin 16px, gap indent, hidemode 3, wmax 90%, newline");
		
		JPanel content = new JPanel(new MigLayout("insets dialog, nogrid, fill"));
		
		content.add(editor, "w 120px:min(pref, 420px), h 40px!, growx, wrap 4px, id editor");
		content.add(createImageButton(changeSampleAction), "w 25!, h 19!, pos n editor.y2+1 editor.x2 n");
		
		content.add(help, "growx, wrap 25px:push");
		
		content.add(new JButton(switchEditModeAction), "tag left");
		content.add(new JButton(approveFormatAction), "tag apply");
		content.add(new JButton(cancelAction), "tag cancel");
		
		JComponent pane = (JComponent) getContentPane();
		pane.setLayout(new MigLayout("insets 0, fill"));
		
		pane.add(header, "h 60px, growx, dock north");
		pane.add(content, "grow");
		
		addPropertyChangeListener("sample", new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				checkFormatInBackground();
			}
		});
		
		// focus editor by default
		addWindowFocusListener(new WindowAdapter() {
			
			@Override
			public void windowGainedFocus(WindowEvent e) {
				editor.requestFocusInWindow();
			}
		});
		
		// finish dialog and close window manually
		addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				finish(false);
			}
		});
		
		// install editor suggestions popup
		TunedUtilities.installAction(editor, KeyStroke.getKeyStroke("DOWN"), displayRecentFormatHistory);
		
		// episode mode by default
		setMode(Mode.Episode);
		
		// initialize window properties
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setSize(520, 400);
	}
	

	public void setMode(Mode mode) {
		this.mode = mode;
		
		this.setTitle(String.format("%s Format", mode));
		title.setText(this.getTitle());
		status.setVisible(false);
		
		switchEditModeAction.putValue(Action.NAME, String.format("%s Format", mode.next()));
		updateHelpPanel(mode);
		
		// update preview to current format
		sample = restoreSample(mode);
		
		// restore editor state
		editor.setText(mode.persistentFormatHistory().isEmpty() ? "" : mode.persistentFormatHistory().get(0));
		
		// update examples
		fireSampleChanged();
	}
	

	private JComponent updateHelpPanel(Mode mode) {
		help.removeAll();
		
		help.add(new JLabel("Syntax"), "gap indent+unrel, wrap 0");
		help.add(createSyntaxPanel(mode), "gapx indent indent, wrap 8px");
		
		help.add(new JLabel("Examples"), "gap indent+unrel, wrap 0");
		help.add(createExamplesPanel(mode), "growx, h pref!, gapx indent indent");
		
		return help;
	}
	

	private JTextComponent createEditor() {
		final JTextComponent editor = new JTextField(new ExpressionFormatDocument(), null, 0);
		editor.setFont(new Font(MONOSPACED, PLAIN, 14));
		
		// enable undo/redo
		installUndoSupport(editor);
		
		// update format on change
		editor.getDocument().addDocumentListener(new LazyDocumentListener() {
			
			@Override
			public void update(DocumentEvent e) {
				checkFormatInBackground();
			}
		});
		
		// improved cursor behaviour, use delayed listener, so we apply our cursor updates, after the text component is finished with its own
		editor.getDocument().addDocumentListener(new LazyDocumentListener(0) {
			
			@Override
			public void update(DocumentEvent evt) {
				if (evt.getType() == DocumentEvent.EventType.INSERT) {
					ExpressionFormatDocument document = (ExpressionFormatDocument) evt.getDocument();
					
					if (document.getLastCompletion() != null) {
						editor.setCaretPosition(editor.getCaretPosition() - 1);
					}
				}
			}
		});
		
		return editor;
	}
	

	private JComponent createSyntaxPanel(Mode mode) {
		JPanel panel = new JPanel(new MigLayout("fill, nogrid"));
		
		panel.setBorder(createLineBorder(new Color(0xACA899)));
		panel.setBackground(new Color(0xFFFFE1));
		panel.setOpaque(true);
		
		panel.add(new JLabel(ResourceBundle.getBundle(getClass().getName()).getString(mode.key() + ".syntax")));
		
		return panel;
	}
	

	private JComponent createExamplesPanel(Mode mode) {
		JPanel panel = new JPanel(new MigLayout("fill, wrap 3"));
		
		panel.setBorder(createLineBorder(new Color(0xACA899)));
		panel.setBackground(new Color(0xFFFFE1));
		
		ResourceBundle bundle = ResourceBundle.getBundle(getClass().getName());
		TreeMap<String, String> examples = new TreeMap<String, String>();
		
		// extract all example entries and sort by key
		for (String key : bundle.keySet()) {
			if (key.startsWith(mode.key() + ".example"))
				examples.put(key, bundle.getString(key));
		}
		
		for (final String format : examples.values()) {
			LinkButton formatLink = new LinkButton(new AbstractAction(format) {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					editor.setText(format);
				}
			});
			
			formatLink.setFont(new Font(MONOSPACED, PLAIN, 11));
			
			// compute format label in background
			final JLabel formatExample = new JLabel("[evaluate]");
			
			// bind text to preview
			addPropertyChangeListener("sample", new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					new SwingWorker<String, Void>() {
						
						@Override
						protected String doInBackground() throws Exception {
							return new ExpressionFormat(format).format(sample);
						}
						

						@Override
						protected void done() {
							try {
								formatExample.setText(get());
							} catch (Exception e) {
								Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
							}
						}
					}.execute();
				}
			});
			
			panel.add(formatLink);
			panel.add(new JLabel("…"));
			panel.add(formatExample);
		}
		
		return panel;
	}
	

	private MediaBindingBean restoreSample(Mode mode) {
		Object info = null;
		File media = null;
		
		try {
			// restore sample from user preferences
			String sample = mode.persistentSample().getValue();
			info = mode.getFormat().parseObject(sample);
		} catch (Exception e) {
			try {
				// restore sample from application properties
				ResourceBundle bundle = ResourceBundle.getBundle(getClass().getName());
				String sample = bundle.getString(mode.key() + ".sample");
				info = mode.getFormat().parseObject(sample);
			} catch (ParseException illegalSample) {
				throw new RuntimeException(illegalSample); // won't happen
			}
		}
		
		// restore media file
		String path = persistentSampleFile.getValue();
		
		if (path != null && !path.isEmpty()) {
			media = new File(path);
		}
		
		return new MediaBindingBean(info, media);
	}
	

	private ExecutorService createExecutor() {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1), new DefaultThreadFactory("PreviewFormatter")) {
			
			@SuppressWarnings("deprecation")
			@Override
			public List<Runnable> shutdownNow() {
				List<Runnable> remaining = super.shutdownNow();
				
				try {
					if (!awaitTermination(3, TimeUnit.SECONDS)) {
						// if the thread has not terminated after 4 seconds, it is probably stuck
						ThreadGroup threadGroup = ((DefaultThreadFactory) getThreadFactory()).getThreadGroup();
						
						// kill background thread by force
						threadGroup.stop();
						
						// log access of potentially unsafe method
						Logger.getLogger(getClass().getName()).warning("Thread was forcibly terminated");
					}
				} catch (Exception e) {
					Logger.getLogger(getClass().getName()).log(Level.WARNING, "Thread was not terminated", e);
				}
				
				return remaining;
			}
		};
		
		// only keep the latest task in the queue
		executor.setRejectedExecutionHandler(new DiscardOldestPolicy());
		
		return executor;
	}
	

	private void checkFormatInBackground() {
		try {
			// check syntax in foreground
			final ExpressionFormat format = new ExpressionFormat(editor.getText().trim());
			
			// activate delayed to avoid flickering when formatting takes only a couple of milliseconds
			final Timer progressIndicatorTimer = TunedUtilities.invokeLater(400, new Runnable() {
				
				@Override
				public void run() {
					progressIndicator.setVisible(true);
				}
			});
			
			// cancel old worker later
			Future<String> obsoletePreviewFuture = currentPreviewFuture;
			
			// create new worker
			currentPreviewFuture = new SwingWorker<String, Void>() {
				
				@Override
				protected String doInBackground() throws Exception {
					return format.format(sample);
				}
				

				@Override
				protected void done() {
					try {
						preview.setText(get());
						
						// check internal script exception
						if (format.caughtScriptException() != null) {
							throw format.caughtScriptException();
						}
						
						// check empty output
						if (get().trim().isEmpty()) {
							throw new RuntimeException("Formatted value is empty");
						}
						
						// no warning or error
						status.setVisible(false);
					} catch (CancellationException e) {
						// ignore, cancelled tasks are obsolete anyway
					} catch (Exception e) {
						Exception cause = findCause(e, BindingException.class);
						status.setText(getMessage(cause != null ? cause : e));
						status.setIcon(ResourceManager.getIcon("status.warning"));
						status.setVisible(true);
					} finally {
						preview.setVisible(preview.getText().trim().length() > 0);
						editor.setForeground(preview.getForeground());
						
						// stop progress indicator from becoming visible, if we have been fast enough
						progressIndicatorTimer.stop();
						
						// hide progress indicator, if this still is the current worker
						if (this == currentPreviewFuture) {
							progressIndicator.setVisible(false);
						}
					}
				}
			};
			
			// cancel old worker, after new worker has been created, because done() might be called from within cancel()
			if (obsoletePreviewFuture != null) {
				obsoletePreviewFuture.cancel(true);
			}
			
			// submit new worker
			executor.execute(currentPreviewFuture);
		} catch (ScriptException e) {
			// incorrect syntax 
			status.setText(ExceptionUtilities.getRootCauseMessage(e));
			status.setIcon(ResourceManager.getIcon("status.error"));
			status.setVisible(true);
			
			preview.setVisible(false);
			editor.setForeground(Color.red);
		}
	}
	

	public boolean submit() {
		return submit;
	}
	

	public Mode getMode() {
		return mode;
	}
	

	public ExpressionFormat getFormat() {
		return format;
	}
	

	private void finish(boolean submit) {
		this.submit = submit;
		
		// force shutdown
		executor.shutdownNow();
		
		setVisible(false);
		dispose();
	}
	

	protected final Action changeSampleAction = new AbstractAction("Change Sample", ResourceManager.getIcon("action.variable")) {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			BindingDialog dialog = new BindingDialog(getWindow(evt.getSource()), String.format("%s Bindings", mode), mode.getFormat());
			
			dialog.setInfoObject(sample.getInfoObject());
			dialog.setMediaFile(sample.getMediaFile());
			
			// open dialog
			dialog.setLocationRelativeTo((Component) evt.getSource());
			dialog.setVisible(true);
			
			if (dialog.submit()) {
				Object info = dialog.getInfoObject();
				File file = dialog.getMediaFile();
				
				// change sample
				sample = new MediaBindingBean(info, file);
				
				// remember
				mode.persistentSample().setValue(info == null ? "" : mode.getFormat().format(info));
				persistentSampleFile.setValue(file == null ? "" : sample.getMediaFile().getAbsolutePath());
				
				// reevaluate everything
				fireSampleChanged();
			}
		}
	};
	
	protected final Action displayRecentFormatHistory = new AbstractAction("Recent") {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			JPopupMenu popup = new JPopupMenu();
			
			for (final String expression : mode.persistentFormatHistory()) {
				JMenuItem item = popup.add(new AbstractAction(expression) {
					
					@Override
					public void actionPerformed(ActionEvent evt) {
						editor.setText(expression);
					}
				});
				
				item.setFont(new Font(MONOSPACED, PLAIN, 11));
			}
			
			// display popup below format editor
			popup.show(editor, 0, editor.getHeight() + 3);
		}
	};
	
	protected final Action cancelAction = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			finish(false);
		}
	};
	
	protected final Action switchEditModeAction = new AbstractAction(null, ResourceManager.getIcon("dialog.switch")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			setMode(mode.next());
		}
	};
	
	protected final Action approveFormatAction = new AbstractAction("Use Format", ResourceManager.getIcon("dialog.continue")) {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			try {
				// check syntax
				format = new ExpressionFormat(editor.getText().trim());
				
				// create new recent history and ignore duplicates
				Set<String> recent = new LinkedHashSet<String>();
				
				// add new format first
				recent.add(format.getExpression());
				
				// add next 4 most recent formats
				for (int i = 0, limit = Math.min(4, mode.persistentFormatHistory().size()); i < limit; i++) {
					recent.add(mode.persistentFormatHistory().get(i));
				}
				
				// update persistent history
				mode.persistentFormatHistory().set(recent);
				
				finish(true);
			} catch (ScriptException e) {
				UILogger.log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e));
			}
		}
	};
	

	protected void fireSampleChanged() {
		firePropertyChange("sample", null, sample);
	}
	
}
