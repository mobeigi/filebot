
package net.sourceforge.filebot.ui;


import static java.awt.Font.BOLD;
import static java.awt.Font.MONOSPACED;
import static java.awt.Font.PLAIN;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.Format;
import java.text.ParseException;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultFormatterFactory;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.Episode.EpisodeFormat;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;
import net.sourceforge.tuned.ui.GradientStyle;
import net.sourceforge.tuned.ui.LinkButton;
import net.sourceforge.tuned.ui.TunedUtilities;
import net.sourceforge.tuned.ui.notification.SeparatorBorder;
import net.sourceforge.tuned.ui.notification.SeparatorBorder.Position;


public class EpisodeFormatDialog extends JDialog {
	
	private Format selectedFormat = null;
	
	protected final JFormattedTextField preview = new JFormattedTextField();
	
	protected final JLabel errorMessage = new JLabel(ResourceManager.getIcon("dialog.cancel"));
	protected final JTextField editor = new JTextField();
	
	protected Color defaultColor = preview.getForeground();
	protected Color errorColor = Color.red;
	
	protected final PreferencesEntry<String> persistentFormat = Settings.userRoot().entry("dialog.format");
	protected final PreferencesEntry<String> persistentSample = Settings.userRoot().entry("dialog.sample");
	
	
	public EpisodeFormatDialog(Window owner) {
		super(owner, "Episode Format", ModalityType.DOCUMENT_MODAL);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		editor.setFont(new Font(MONOSPACED, PLAIN, 14));
		
		// restore state
		preview.setValue(getPreviewSample());
		editor.setText(persistentFormat.getValue());
		
		preview.setBorder(BorderFactory.createEmptyBorder());
		
		// update preview to current format
		checkEpisodeFormat();
		
		// bold title label in header
		JLabel title = new JLabel(this.getTitle());
		title.setFont(title.getFont().deriveFont(BOLD));
		
		//		status.setVisible(false);
		JPanel header = new JPanel(new MigLayout("insets dialog, nogrid, fillx"));
		
		header.setBackground(Color.white);
		header.setBorder(new SeparatorBorder(1, new Color(0xB4B4B4), new Color(0xACACAC), GradientStyle.LEFT_TO_RIGHT, Position.BOTTOM));
		
		header.add(title, "wrap unrel:push");
		header.add(errorMessage, "gap indent, hidemode 3");
		header.add(preview, "gap indent, hidemode 3, growx");
		
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
		
		pack();
		setLocation(TunedUtilities.getPreferredLocation(this));
		
		TunedUtilities.putActionForKeystroke(pane, KeyStroke.getKeyStroke("released ESCAPE"), cancelAction);
		
		// update format on change
		editor.getDocument().addDocumentListener(new DocumentAdapter() {
			
			@Override
			public void update(DocumentEvent evt) {
				checkEpisodeFormat();
			}
		});
		
		// keep focus on preview, if current text doesn't fit episode format
		preview.setInputVerifier(new InputVerifier() {
			
			@Override
			public boolean verify(JComponent input) {
				return checkPreviewSample();
			}
		});
		
		// check edit format on change
		preview.getDocument().addDocumentListener(new DocumentAdapter() {
			
			@Override
			public void update(DocumentEvent evt) {
				checkPreviewSample();
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
	

	protected JPanel createSyntaxPanel() {
		JPanel panel = new JPanel(new MigLayout("fill, nogrid"));
		
		panel.setBorder(new LineBorder(new Color(0xACA899)));
		panel.setBackground(new Color(0xFFFFE1));
		panel.setOpaque(true);
		
		panel.add(new JLabel(ResourceBundle.getBundle(getClass().getName()).getString("syntax")));
		
		return panel;
	}
	

	protected JPanel createExamplesPanel() {
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
	

	protected Episode getPreviewSample() {
		String sample = persistentSample.getValue();
		
		if (sample != null) {
			try {
				return EpisodeFormat.getInstance().parseObject(sample);
			} catch (Exception e) {
				Logger.getLogger("global").log(Level.WARNING, e.getMessage(), e);
			}
		}
		
		// default sample
		return new Episode("Dark Angel", "3", "1", "Labyrinth");
	}
	

	protected boolean checkPreviewSample() {
		// check if field is being edited
		if (preview.hasFocus()) {
			try {
				// try to parse text
				preview.getFormatter().stringToValue(preview.getText());
			} catch (Exception e) {
				preview.setForeground(errorColor);
				// failed to parse text
				return false;
			}
		}
		
		preview.setForeground(defaultColor);
		return true;
	}
	

	protected DefaultFormatterFactory createFormatterFactory(Format display) {
		DefaultFormatterFactory factory = new DefaultFormatterFactory();
		
		factory.setEditFormatter(new SimpleFormatter(EpisodeFormat.getInstance()));
		
		if (display != null) {
			factory.setDisplayFormatter(new SimpleFormatter(display));
		}
		
		return factory;
	}
	

	protected boolean checkEpisodeFormat() {
		Exception exception = null;
		
		try {
			Format format = new EpisodeExpressionFormat(editor.getText().trim());
			
			// check if format produces empty strings
			if (format.format(preview.getValue()).trim().isEmpty()) {
				throw new IllegalArgumentException("Format must not be empty.");
			}
			
			// update preview
			preview.setFormatterFactory(createFormatterFactory(format));
		} catch (Exception e) {
			exception = e;
		}
		
		errorMessage.setText(exception != null ? ExceptionUtilities.getRootCauseMessage(exception) : null);
		errorMessage.setVisible(exception != null);
		
		preview.setVisible(exception == null);
		editor.setForeground(exception == null ? defaultColor : errorColor);
		
		return exception == null;
	}
	

	public Format getSelectedFormat() {
		return selectedFormat;
	}
	

	private void finish(Format format) {
		this.selectedFormat = format;
		
		setVisible(false);
		dispose();
		
		if (checkEpisodeFormat()) {
			persistentFormat.setValue(editor.getText());
		}
		
		if (checkPreviewSample()) {
			persistentSample.setValue(EpisodeFormat.getInstance().format(preview.getValue()));
		}
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
			} catch (ScriptException e) {
				Logger.getLogger("ui").log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
			}
		}
	};
	
	
	public static Format showDialog(Component parent) {
		EpisodeFormatDialog dialog = new EpisodeFormatDialog(parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
		
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
			updateText(preview.getValue());
			
			// bind text to preview
			preview.addPropertyChangeListener("value", new PropertyChangeListener() {
				
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
	

	protected static class SimpleFormatter extends AbstractFormatter {
		
		private final Format format;
		
		
		public SimpleFormatter(Format format) {
			this.format = format;
		}
		

		@Override
		public String valueToString(Object value) throws ParseException {
			return format.format(value);
		}
		

		@Override
		public Object stringToValue(String text) throws ParseException {
			return format.parseObject(text);
		}
		
	}
	

	protected static class DocumentAdapter implements DocumentListener {
		
		@Override
		public void changedUpdate(DocumentEvent e) {
			update(e);
		}
		

		@Override
		public void insertUpdate(DocumentEvent e) {
			update(e);
		}
		

		@Override
		public void removeUpdate(DocumentEvent e) {
			update(e);
		}
		

		public void update(DocumentEvent e) {
			
		}
		
	}
	
}
