
package net.sourceforge.filebot.ui;


import static java.awt.Font.BOLD;
import static java.awt.Font.MONOSPACED;
import static java.awt.Font.PLAIN;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.Format;
import java.util.Arrays;
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
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.format.EpisodeExpressionFormat;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.Episode.EpisodeFormat;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.ui.GradientStyle;
import net.sourceforge.tuned.ui.LinkButton;
import net.sourceforge.tuned.ui.ProgressIndicator;
import net.sourceforge.tuned.ui.TunedUtilities;
import net.sourceforge.tuned.ui.notification.SeparatorBorder;
import net.sourceforge.tuned.ui.notification.SeparatorBorder.Position;


public class EpisodeFormatDialog extends JDialog {
	
	private Format selectedFormat = null;
	
	private JLabel preview = new JLabel();
	
	private JLabel warningMessage = new JLabel(ResourceManager.getIcon("status.warning"));
	private JLabel errorMessage = new JLabel(ResourceManager.getIcon("status.error"));
	
	private Episode previewSampleEpisode = getPreviewSampleEpisode();
	private File previewSampleMediaFile = getPreviewSampleMediaFile();
	
	private ExecutorService previewExecutor = createPreviewExecutor();
	
	private ProgressIndicator progressIndicator = new ProgressIndicator();
	
	private JTextField editor = new JTextField();
	
	private Color defaultColor = preview.getForeground();
	private Color errorColor = Color.red;
	
	
	public EpisodeFormatDialog(Window owner) {
		super(owner, "Episode Format", ModalityType.DOCUMENT_MODAL);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		editor.setText(Settings.userRoot().get("dialog.format"));
		editor.setFont(new Font(MONOSPACED, PLAIN, 14));
		
		// bold title label in header
		JLabel title = new JLabel(this.getTitle());
		title.setFont(title.getFont().deriveFont(BOLD));
		
		JPanel header = new JPanel(new MigLayout("insets dialog, nogrid, fillx"));
		
		header.setBackground(Color.white);
		header.setBorder(new SeparatorBorder(1, new Color(0xB4B4B4), new Color(0xACACAC), GradientStyle.LEFT_TO_RIGHT, Position.BOTTOM));
		
		errorMessage.setVisible(false);
		warningMessage.setVisible(false);
		progressIndicator.setVisible(false);
		
		header.add(progressIndicator, "pos 1al 0al, hidemode 3");
		header.add(title, "wrap unrel:push");
		header.add(preview, "gap indent, hidemode 3, wmax 90%");
		header.add(errorMessage, "gap indent, hidemode 3, wmax 90%, newline");
		header.add(warningMessage, "gap indent, hidemode 3, wmax 90%, newline");
		
		JPanel content = new JPanel(new MigLayout("insets dialog, nogrid, fill"));
		
		content.add(editor, "wmin 120px, h 40px!, growx, wrap 8px");
		
		content.add(new JLabel("Syntax"), "gap indent+unrel, wrap 0");
		content.add(createSyntaxPanel(), "gapx indent indent, wrap 8px");
		
		content.add(new JLabel("Examples"), "gap indent+unrel, wrap 0");
		content.add(createExamplesPanel(), "gapx indent indent, wrap 25px:push");
		
		content.add(new JButton(useDefaultFormatAction), "tag left");
		content.add(new JButton(useCustomFormatAction), "tag apply");
		content.add(new JButton(cancelAction), "tag cancel");
		
		JComponent pane = (JComponent) getContentPane();
		pane.setLayout(new MigLayout("insets 0, fill"));
		
		pane.add(header, "h 60px, growx, dock north");
		pane.add(content, "grow");
		
		setSize(485, 415);
		
		header.setComponentPopupMenu(createPreviewSamplePopup());
		
		setLocation(TunedUtilities.getPreferredLocation(this));
		
		// update preview to current format
		checkFormatInBackground();
		
		// update format on change
		editor.getDocument().addDocumentListener(new LazyDocumentAdapter() {
			
			@Override
			public void update() {
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
	}
	

	private JPopupMenu createPreviewSamplePopup() {
		JPopupMenu actionPopup = new JPopupMenu("Sample");
		
		actionPopup.add(new AbstractAction("Change Episode") {
			
			@Override
			public void actionPerformed(ActionEvent evt) {
				String episode = JOptionPane.showInputDialog(EpisodeFormatDialog.this, null, previewSampleEpisode);
				
				if (episode != null) {
					try {
						previewSampleEpisode = EpisodeFormat.getInstance().parseObject(episode);
						Settings.userRoot().put("dialog.sample.episode", episode);
						
						EpisodeFormatDialog.this.firePropertyChange("previewSample", null, previewSample());
					} catch (Exception e) {
						Logger.getLogger("ui").warning(String.format("Cannot parse %s", episode));
					}
				}
			}
		});
		
		actionPopup.add(new AbstractAction("Change Media File") {
			
			@Override
			public void actionPerformed(ActionEvent evt) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setSelectedFile(previewSampleMediaFile);
				fileChooser.setFileFilter(new FileNameExtensionFilter("Media files", "avi", "mkv", "mp4", "ogm"));
				
				if (fileChooser.showOpenDialog(EpisodeFormatDialog.this) == JFileChooser.APPROVE_OPTION) {
					previewSampleMediaFile = fileChooser.getSelectedFile();
					Settings.userRoot().put("dialog.sample.file", previewSampleMediaFile.getAbsolutePath());
					
					try {
						MediaInfoComponent.showMessageDialog(EpisodeFormatDialog.this, previewSampleMediaFile);
					} catch (LinkageError e) {
						// MediaInfo native library is missing -> notify user
						Logger.getLogger("ui").log(Level.SEVERE, e.getMessage(), e);
						
						// rethrow error
						throw e;
					}
					
					EpisodeFormatDialog.this.firePropertyChange("previewSample", null, previewSample());
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
	

	private JPanel createExamplesPanel() {
		JPanel panel = new JPanel(new MigLayout("fill, wrap 3"));
		
		panel.setBorder(new LineBorder(new Color(0xACA899)));
		panel.setBackground(new Color(0xFFFFE1));
		panel.setOpaque(true);
		
		ResourceBundle bundle = ResourceBundle.getBundle(getClass().getName());
		
		// sort keys
		String[] keys = bundle.keySet().toArray(new String[0]);
		Arrays.sort(keys);
		
		for (String key : keys) {
			if (key.startsWith("example")) {
				String format = bundle.getString(key);
				
				LinkButton formatLink = new LinkButton(new ExampleFormatAction(format));
				formatLink.setFont(new Font(MONOSPACED, PLAIN, 11));
				
				panel.add(formatLink);
				panel.add(new JLabel("..."));
				panel.add(new ExampleFormatLabel(format));
			}
		}
		
		return panel;
	}
	

	private Match<Episode, File> previewSample() {
		return new Match<Episode, File>(previewSampleEpisode, previewSampleMediaFile);
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
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1));
		
		// only keep the latest task in the queue
		executor.setRejectedExecutionHandler(new DiscardOldestPolicy());
		
		return executor;
	}
	

	private void checkFormatInBackground() {
		final Timer progressIndicatorTimer = TunedUtilities.invokeLater(400, new Runnable() {
			
			@Override
			public void run() {
				progressIndicator.setVisible(true);
			}
		});
		
		previewExecutor.execute(new SwingWorker<String, Void>() {
			
			private ScriptException warning = null;
			
			
			@Override
			protected String doInBackground() throws Exception {
				EpisodeExpressionFormat format = new EpisodeExpressionFormat(editor.getText().trim());
				
				String text = format.format(previewSample());
				warning = format.scriptException();
				
				// check if format produces empty strings
				if (text.trim().isEmpty()) {
					throw new IllegalArgumentException("Format must not be empty.");
				}
				
				return text;
			}
			

			@Override
			protected void done() {
				
				Exception error = null;
				
				try {
					preview.setText(get());
				} catch (Exception e) {
					error = e;
				}
				
				errorMessage.setText(error != null ? ExceptionUtilities.getRootCauseMessage(error) : null);
				errorMessage.setVisible(error != null);
				
				warningMessage.setText(warning != null ? warning.getCause().getMessage() : null);
				warningMessage.setVisible(warning != null);
				
				preview.setVisible(error == null);
				editor.setForeground(error == null ? defaultColor : errorColor);
				
				progressIndicatorTimer.stop();
				progressIndicator.setVisible(false);
			}
			
		});
	}
	

	public Format getSelectedFormat() {
		return selectedFormat;
	}
	

	private void finish(Format format) {
		this.selectedFormat = format;
		
		previewExecutor.shutdownNow();
		
		setVisible(false);
		dispose();
	}
	
	protected final Action cancelAction = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			finish(null);
		}
	};
	
	protected final Action useDefaultFormatAction = new AbstractAction("Default", ResourceManager.getIcon("dialog.default")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			finish(EpisodeFormat.getInstance());
		}
	};
	
	protected final Action useCustomFormatAction = new AbstractAction("Use Format", ResourceManager.getIcon("dialog.continue")) {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			try {
				finish(new EpisodeExpressionFormat(editor.getText()));
				Settings.userRoot().put("dialog.format", editor.getText());
			} catch (ScriptException e) {
				Logger.getLogger("ui").log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
			}
		}
	};
	
	
	public static Format showDialog(Component parent) {
		EpisodeFormatDialog dialog = new EpisodeFormatDialog(TunedUtilities.getWindow(parent));
		
		dialog.setVisible(true);
		
		return dialog.getSelectedFormat();
	}
	
	
	protected class ExampleFormatAction extends AbstractAction {
		
		public ExampleFormatAction(String format) {
			super(format);
		}
		

		@Override
		public void actionPerformed(ActionEvent e) {
			editor.setText(getValue(Action.NAME).toString());
		}
	}
	

	protected class ExampleFormatLabel extends JLabel {
		
		private final String format;
		
		
		public ExampleFormatLabel(String format) {
			this.format = format;
			
			// initialize text
			updateText(previewSample());
			
			// bind text to preview
			EpisodeFormatDialog.this.addPropertyChangeListener("previewSample", new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					updateText(evt.getNewValue());
				}
			});
		}
		

		public void updateText(Object episode) {
			try {
				setText(new EpisodeExpressionFormat(format).format(episode));
				setForeground(defaultColor);
			} catch (Exception e) {
				setText(ExceptionUtilities.getRootCauseMessage(e));
				setForeground(errorColor);
			}
		}
	}
	

	protected static abstract class LazyDocumentAdapter implements DocumentListener {
		
		private final Timer timer = new Timer(200, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				update();
			}
		});
		
		
		public LazyDocumentAdapter() {
			timer.setRepeats(false);
		}
		

		@Override
		public void changedUpdate(DocumentEvent e) {
			timer.restart();
		}
		

		@Override
		public void insertUpdate(DocumentEvent e) {
			timer.restart();
		}
		

		@Override
		public void removeUpdate(DocumentEvent e) {
			timer.restart();
		}
		

		public abstract void update();
		
	}
	
}
