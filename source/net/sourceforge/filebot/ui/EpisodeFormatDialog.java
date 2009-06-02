
package net.sourceforge.filebot.ui;


import static java.awt.Font.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.undo.UndoManager;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.format.EpisodeFormatBindingBean;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeFormat;
import net.sourceforge.tuned.DefaultThreadFactory;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.ui.GradientStyle;
import net.sourceforge.tuned.ui.LazyDocumentListener;
import net.sourceforge.tuned.ui.LinkButton;
import net.sourceforge.tuned.ui.ProgressIndicator;
import net.sourceforge.tuned.ui.TunedUtilities;
import net.sourceforge.tuned.ui.notification.SeparatorBorder;
import net.sourceforge.tuned.ui.notification.SeparatorBorder.Position;


public class EpisodeFormatDialog extends JDialog {
	
	private Option selectedOption = Option.CANCEL;
	
	private JLabel preview = new JLabel();
	
	private JLabel status = new JLabel();
	
	private EpisodeFormatBindingBean previewSample = new EpisodeFormatBindingBean(getPreviewSampleEpisode(), getPreviewSampleMediaFile());
	
	private ExecutorService previewExecutor = createPreviewExecutor();
	
	private ProgressIndicator progressIndicator = new ProgressIndicator();
	
	private JTextField editor = new JTextField();
	
	private Color defaultColor = preview.getForeground();
	private Color errorColor = Color.red;
	

	public enum Option {
		APPROVE,
		CANCEL,
		USE_DEFAULT
	}
	

	public EpisodeFormatDialog(Window owner) {
		super(owner, "Episode Format", ModalityType.DOCUMENT_MODAL);
		
		editor.setText(Settings.userRoot().get("dialog.format"));
		editor.setFont(new Font(MONOSPACED, PLAIN, 14));
		
		// bold title label in header
		JLabel title = new JLabel(this.getTitle());
		title.setFont(title.getFont().deriveFont(BOLD));
		
		JPanel header = new JPanel(new MigLayout("insets dialog, nogrid, fillx"));
		
		header.setBackground(Color.white);
		header.setBorder(new SeparatorBorder(1, new Color(0xB4B4B4), new Color(0xACACAC), GradientStyle.LEFT_TO_RIGHT, Position.BOTTOM));
		
		header.add(progressIndicator, "pos 1al 0al, hidemode 3");
		header.add(title, "wrap unrel:push");
		header.add(preview, "hmin 16px, gap indent, hidemode 3, wmax 90%");
		header.add(status, "hmin 16px, gap indent, hidemode 3, wmax 90%, newline");
		
		JPanel content = new JPanel(new MigLayout("insets dialog, nogrid, fill"));
		
		content.add(editor, "wmin 120px, h 40px!, growx, wrap 8px");
		
		content.add(new JLabel("Syntax"), "gap indent+unrel, wrap 0");
		content.add(createSyntaxPanel(), "gapx indent indent, wrap 8px");
		
		content.add(new JLabel("Examples"), "gap indent+unrel, wrap 0");
		content.add(createExamplesPanel(), "hmin 50px, gapx indent indent, wrap 25px:push");
		
		content.add(new JButton(useDefaultFormatAction), "tag left");
		content.add(new JButton(approveFormatAction), "tag apply");
		content.add(new JButton(cancelAction), "tag cancel");
		
		JComponent pane = (JComponent) getContentPane();
		pane.setLayout(new MigLayout("insets 0, fill"));
		
		pane.add(header, "h 60px, growx, dock north");
		pane.add(content, "grow");
		
		header.setComponentPopupMenu(createPreviewSamplePopup());
		
		// setup undo support
		final UndoManager undo = new UndoManager();
		editor.getDocument().addUndoableEditListener(undo);
		
		// enable undo/redo
		TunedUtilities.installUndoSupport(editor);
		
		// update format on change
		editor.getDocument().addDocumentListener(new LazyDocumentListener() {
			
			@Override
			public void update(DocumentEvent e) {
				checkFormatInBackground();
			}
		});
		
		addPropertyChangeListener("previewSample", new PropertyChangeListener() {
			
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
				finish(Option.CANCEL);
			}
		});
		
		// update preview to current format
		firePreviewSampleChanged();
		
		// initialize window properties
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setLocation(TunedUtilities.getPreferredLocation(this));
		pack();
	}
	

	private JPopupMenu createPreviewSamplePopup() {
		JPopupMenu actionPopup = new JPopupMenu("Sample");
		
		actionPopup.add(new AbstractAction("Change Episode") {
			
			@Override
			public void actionPerformed(ActionEvent evt) {
				String episodeString = JOptionPane.showInputDialog(EpisodeFormatDialog.this, null, EpisodeFormat.getInstance().format(previewSample.getEpisode()));
				
				if (episodeString != null) {
					try {
						Episode episode = EpisodeFormat.getInstance().parseObject(episodeString);
						
						// change episode
						previewSample = new EpisodeFormatBindingBean(episode, previewSample.getMediaFile());
						Settings.userRoot().put("dialog.sample.episode", episodeString);
						firePreviewSampleChanged();
					} catch (ParseException e) {
						Logger.getLogger("ui").warning(String.format("Cannot parse %s", episodeString));
					}
				}
			}
		});
		
		actionPopup.add(new AbstractAction("Change Media File") {
			
			@Override
			public void actionPerformed(ActionEvent evt) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setSelectedFile(previewSample.getMediaFile());
				fileChooser.setFileFilter(new FileNameExtensionFilter("Media files", "avi", "mkv", "mp4", "ogm"));
				
				if (fileChooser.showOpenDialog(EpisodeFormatDialog.this) == JFileChooser.APPROVE_OPTION) {
					File mediaFile = fileChooser.getSelectedFile();
					
					try {
						MediaInfoPane.showMessageDialog(EpisodeFormatDialog.this, mediaFile);
					} catch (LinkageError e) {
						// MediaInfo native library is missing -> notify user
						Logger.getLogger("ui").log(Level.SEVERE, e.getMessage(), e);
						
						// rethrow error
						throw e;
					}
					
					// change media file
					previewSample = new EpisodeFormatBindingBean(previewSample.getEpisode(), mediaFile);
					Settings.userRoot().put("dialog.sample.file", mediaFile.getAbsolutePath());
					firePreviewSampleChanged();
				}
			}
		});
		
		return actionPopup;
	}
	

	private JPanel createSyntaxPanel() {
		JPanel panel = new JPanel(new MigLayout("fill, nogrid"));
		
		panel.setBorder(new LineBorder(new Color(0xACA899)));
		panel.setBackground(new Color(0xFFFFE1));
		panel.setOpaque(true);
		
		panel.add(new JLabel(ResourceBundle.getBundle(getClass().getName()).getString("syntax")));
		
		return panel;
	}
	

	private JComponent createExamplesPanel() {
		JPanel panel = new JPanel(new MigLayout("fill, wrap 3"));
		
		panel.setBorder(new LineBorder(new Color(0xACA899)));
		panel.setBackground(new Color(0xFFFFE1));
		
		ResourceBundle bundle = ResourceBundle.getBundle(getClass().getName());
		
		// collect example keys
		List<String> examples = new ArrayList<String>();
		
		for (String key : bundle.keySet()) {
			if (key.startsWith("example"))
				examples.add(key);
		}
		
		// sort by example key
		Collections.sort(examples);
		
		for (String key : examples) {
			final String format = bundle.getString(key);
			
			LinkButton formatLink = new LinkButton(new AbstractAction(format) {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					editor.setText(format);
				}
			});
			
			formatLink.setFont(new Font(MONOSPACED, PLAIN, 11));
			
			final JLabel formatExample = new JLabel();
			
			// bind text to preview
			addPropertyChangeListener("previewSample", new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					try {
						formatExample.setText(new ExpressionFormat(format).format(previewSample));
						setForeground(defaultColor);
					} catch (Exception e) {
						formatExample.setText(ExceptionUtilities.getRootCauseMessage(e));
						setForeground(errorColor);
					}
				}
			});
			
			panel.add(formatLink);
			panel.add(new JLabel("..."));
			panel.add(formatExample);
		}
		
		return panel;
	}
	

	private Episode getPreviewSampleEpisode() {
		String sample = Settings.userRoot().get("dialog.sample.episode");
		
		if (sample != null) {
			try {
				return EpisodeFormat.getInstance().parseObject(sample);
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
			}
		}
		
		// default sample
		return new Episode("Dark Angel", "3", "1", "Labyrinth");
	}
	

	private File getPreviewSampleMediaFile() {
		String sample = Settings.userRoot().get("dialog.sample.file");
		
		if (sample != null) {
			try {
				return new File(sample);
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
			}
		}
		
		// default sample
		return null;
	}
	

	private ExecutorService createPreviewExecutor() {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1), new DefaultThreadFactory("PreviewFormatter")) {
			
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
						Logger.getLogger("global").warning("Thread was forcibly terminated");
					}
				} catch (InterruptedException e) {
					Logger.getLogger("global").log(Level.WARNING, "Thread was not terminated", e);
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
			final ExpressionFormat format = new ExpressionFormat(getExpression());
			
			// format in background
			final Timer progressIndicatorTimer = TunedUtilities.invokeLater(400, new Runnable() {
				
				@Override
				public void run() {
					progressIndicator.setVisible(true);
				}
			});
			
			previewExecutor.execute(new SwingWorker<String, Void>() {
				
				@Override
				protected String doInBackground() throws Exception {
					return format.format(previewSample);
				}
				

				@Override
				protected void done() {
					try {
						preview.setText(get());
						
						// check internal script exception
						if (format.scriptException() != null) {
							throw format.scriptException();
						}
						
						// check empty output
						if (get().trim().isEmpty()) {
							throw new RuntimeException("Formatted value is empty");
						}
						
						// no warning or error
						status.setVisible(false);
					} catch (Exception e) {
						status.setText(ExceptionUtilities.getMessage(e));
						status.setIcon(ResourceManager.getIcon("status.warning"));
						status.setVisible(true);
					} finally {
						preview.setVisible(preview.getText().trim().length() > 0);
						editor.setForeground(defaultColor);
						
						progressIndicatorTimer.stop();
						progressIndicator.setVisible(false);
					}
				}
			});
		} catch (ScriptException e) {
			// incorrect syntax 
			status.setText(ExceptionUtilities.getRootCauseMessage(e));
			status.setIcon(ResourceManager.getIcon("status.error"));
			status.setVisible(true);
			
			preview.setVisible(false);
			editor.setForeground(errorColor);
		}
	}
	

	public String getExpression() {
		return editor.getText().trim();
	}
	

	public Option getSelectedOption() {
		return selectedOption;
	}
	

	private void finish(Option option) {
		selectedOption = option;
		
		// force shutdown
		previewExecutor.shutdownNow();
		
		setVisible(false);
		dispose();
	}
	

	protected final Action cancelAction = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			finish(Option.CANCEL);
		}
	};
	
	protected final Action useDefaultFormatAction = new AbstractAction("Default", ResourceManager.getIcon("dialog.default")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			finish(Option.USE_DEFAULT);
		}
	};
	
	protected final Action approveFormatAction = new AbstractAction("Use Format", ResourceManager.getIcon("dialog.continue")) {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			try {
				if (progressIndicator.isVisible())
					throw new IllegalStateException("Format has not been verified yet.");
				
				// check syntax
				ExpressionFormat format = new ExpressionFormat(getExpression());
				
				// remember format
				Settings.userRoot().put("dialog.format", format.getExpression());
				
				finish(Option.APPROVE);
			} catch (Exception e) {
				Logger.getLogger("ui").log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e));
			}
		}
	};
	

	protected void firePreviewSampleChanged() {
		firePropertyChange("previewSample", null, previewSample);
	}
	
}
