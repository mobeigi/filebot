
package net.sourceforge.filebot.ui.panel.rename;


import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.ui.NotificationLogging.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.script.Compilable;
import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.format.EpisodeBindingBean;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.mediainfo.MediaInfo;
import net.sourceforge.filebot.mediainfo.MediaInfo.StreamKind;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeFormat;
import net.sourceforge.tuned.DefaultThreadFactory;
import net.sourceforge.tuned.ui.LazyDocumentListener;


class EpisodeBindingDialog extends JDialog {
	
	private final JTextField episodeTextField = new JTextField();
	private final JTextField mediaFileTextField = new JTextField();
	
	private final BindingTableModel bindingModel = new BindingTableModel();
	
	private Option selectedOption = Option.CANCEL;
	

	public enum Option {
		APPROVE,
		CANCEL
	}
	

	public EpisodeBindingDialog(Window owner) {
		super(owner, "Episode Bindings", ModalityType.DOCUMENT_MODAL);
		
		JComponent root = (JComponent) getContentPane();
		root.setLayout(new MigLayout("nogrid, fill, insets dialog"));
		
		// decorative tabbed pane
		JTabbedPane inputContainer = new JTabbedPane();
		inputContainer.setFocusable(false);
		
		JPanel inputPanel = new JPanel(new MigLayout("nogrid, fill"));
		inputPanel.setOpaque(false);
		
		inputPanel.add(new JLabel("Episode:"), "wrap 2px");
		inputPanel.add(episodeTextField, "hmin 20px, growx, wrap paragraph");
		
		inputPanel.add(new JLabel("Media File:"), "wrap 2px");
		inputPanel.add(mediaFileTextField, "hmin 20px, growx");
		inputPanel.add(createImageButton(mediaInfoAction), "gap rel, w 26px!, h 24px!");
		inputPanel.add(createImageButton(selectFileAction), "gap rel, w 26px!, h 24px!, wrap paragraph");
		
		inputContainer.add("Episode Bindings", inputPanel);
		root.add(inputContainer, "growx, wrap paragraph");
		
		root.add(new JLabel("Preview:"), "gap 5px, wrap 2px");
		root.add(new JScrollPane(createBindingTable(bindingModel)), "growx, wrap paragraph:push");
		
		root.add(new JButton(approveAction), "tag apply");
		root.add(new JButton(cancelAction), "tag cancel");
		
		// update preview on change
		DocumentListener changeListener = new LazyDocumentListener(1000) {
			
			@Override
			public void update(DocumentEvent evt) {
				// ignore lazy events that come in after the window has been closed
				if (bindingModel.executor.isShutdown())
					return;
				
				bindingModel.setModel(getSampleExpressions(), new EpisodeBindingBean(getEpisode(), getMediaFile()));
			}
		};
		
		// update example bindings on change
		episodeTextField.getDocument().addDocumentListener(changeListener);
		mediaFileTextField.getDocument().addDocumentListener(changeListener);
		
		// disabled by default
		mediaInfoAction.setEnabled(false);
		
		// disable media info action if media file is invalid
		mediaFileTextField.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				mediaInfoAction.setEnabled(getMediaFile() != null && getMediaFile().isFile());
			}
			

			@Override
			public void removeUpdate(DocumentEvent e) {
				changedUpdate(e);
			}
			

			@Override
			public void insertUpdate(DocumentEvent e) {
				changedUpdate(e);
			}
		});
		
		// finish dialog and close window manually
		addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				finish(Option.CANCEL);
			}
		});
		
		// initialize window properties
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setSize(420, 520);
	}
	

	private JTable createBindingTable(TableModel model) {
		JTable table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);
		table.setBackground(Color.white);
		
		table.setDefaultRenderer(Future.class, new DefaultTableCellRenderer() {
			
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
				
				@SuppressWarnings("unchecked")
				Future<String> future = (Future<String>) value;
				
				// reset state
				setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
				
				try {
					// try to get result
					setText(future.get(0, TimeUnit.MILLISECONDS));
				} catch (TimeoutException e) {
					// not ready yet 
					setText("Pending …");
					
					// highlight cell
					if (!isSelected) {
						setForeground(new Color(0x6495ED)); // CornflowerBlue
					}
				} catch (Exception e) {
					// could not evaluate expression
					setText("undefined");
					
					// highlight cell
					if (!isSelected) {
						setForeground(Color.gray);
					}
				}
				
				return this;
			}
		});
		
		return table;
	}
	

	private Collection<String> getSampleExpressions() {
		ResourceBundle bundle = ResourceBundle.getBundle(getClass().getName());
		TreeMap<String, String> expressions = new TreeMap<String, String>();
		
		// extract all expression entries and sort by key
		for (String key : bundle.keySet()) {
			if (key.startsWith("expr"))
				expressions.put(key, bundle.getString(key));
		}
		
		return expressions.values();
	}
	

	public Option getSelectedOption() {
		return selectedOption;
	}
	

	private void finish(Option option) {
		this.selectedOption = option;
		
		// cancel background evaluators
		bindingModel.executor.shutdownNow();
		
		setVisible(false);
		dispose();
	}
	

	public void setEpisode(Episode episode) {
		episodeTextField.setText(episode == null ? "" : EpisodeFormat.getDefaultInstance().format(episode));
	}
	

	public void setMediaFile(File mediaFile) {
		mediaFileTextField.setText(mediaFile == null ? "" : mediaFile.getAbsolutePath());
	}
	

	public Episode getEpisode() {
		try {
			return EpisodeFormat.getDefaultInstance().parseObject(episodeTextField.getText());
		} catch (Exception e) {
			return null;
		}
	}
	

	public File getMediaFile() {
		File file = new File(mediaFileTextField.getText());
		
		// allow only absolute paths
		return file.isAbsolute() ? file : null;
	}
	

	protected final Action approveAction = new AbstractAction("Use Bindings", ResourceManager.getIcon("dialog.continue")) {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			// check episode and media file
			if (getEpisode() == null) {
				// illegal episode string
				UILogger.warning(String.format("Failed to parse episode: '%s'", episodeTextField.getText()));
			} else if (getMediaFile() == null && !mediaFileTextField.getText().isEmpty()) {
				// illegal file path
				UILogger.warning(String.format("Invalid media file: '%s'", mediaFileTextField.getText()));
			} else {
				// everything seems to be in order
				finish(Option.APPROVE);
			}
		}
	};
	
	protected final Action cancelAction = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			finish(Option.CANCEL);
		}
	};
	
	protected final Action mediaInfoAction = new AbstractAction("Info", ResourceManager.getIcon("action.properties")) {
		
		private Map<StreamKind, List<Map<String, String>>> getMediaInfo(File file) {
			try {
				MediaInfo mediaInfo = new MediaInfo();
				
				// read all media info
				if (mediaInfo.open(file)) {
					try {
						return mediaInfo.snapshot();
					} finally {
						mediaInfo.close();
					}
				}
			} catch (LinkageError e) {
				UILogger.log(Level.SEVERE, "Unable to load native library 'mediainfo'", e);
			}
			
			// could not retrieve media info
			return null;
		}
		

		@Override
		public void actionPerformed(ActionEvent evt) {
			Map<StreamKind, List<Map<String, String>>> mediaInfo = getMediaInfo(getMediaFile());
			
			// check if we could get some info
			if (mediaInfo == null)
				return;
			
			// create table tab for each stream
			JTabbedPane tabbedPane = new JTabbedPane();
			
			ResourceBundle bundle = ResourceBundle.getBundle(EpisodeBindingDialog.class.getName());
			RowFilter<Object, Object> excludeRowFilter = RowFilter.notFilter(RowFilter.regexFilter(bundle.getString("parameter.exclude")));
			
			for (StreamKind streamKind : mediaInfo.keySet()) {
				for (Map<String, String> parameters : mediaInfo.get(streamKind)) {
					JPanel panel = new JPanel(new MigLayout("fill"));
					panel.setOpaque(false);
					
					JTable table = new JTable(new ParameterTableModel(parameters));
					table.setAutoCreateRowSorter(true);
					table.setFillsViewportHeight(true);
					table.setBackground(Color.white);
					
					// set media info exclude filter
					TableRowSorter<?> sorter = (TableRowSorter<?>) table.getRowSorter();
					sorter.setRowFilter(excludeRowFilter);
					
					panel.add(new JScrollPane(table), "grow");
					tabbedPane.addTab(streamKind.toString(), panel);
				}
			}
			
			// show media info dialog
			final JDialog dialog = new JDialog(getWindow(evt.getSource()), "MediaInfo", ModalityType.DOCUMENT_MODAL);
			
			Action closeAction = new AbstractAction("OK") {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					dialog.setVisible(false);
					dialog.dispose();
				}
			};
			
			JComponent c = (JComponent) dialog.getContentPane();
			c.setLayout(new MigLayout("fill", "[align center]", "[fill][pref!]"));
			c.add(tabbedPane, "grow, wrap");
			c.add(new JButton(closeAction), "wmin 80px, hmin 25px");
			
			dialog.pack();
			dialog.setLocationRelativeTo(EpisodeBindingDialog.this);
			
			dialog.setVisible(true);
		}
		
	};
	
	protected final Action selectFileAction = new AbstractAction("Select File", ResourceManager.getIcon("action.load")) {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			JFileChooser chooser = new JFileChooser();
			chooser.setSelectedFile(getMediaFile());
			
			// collect media file extensions (video, audio and subtitle files)
			List<String> extensions = new ArrayList<String>();
			Collections.addAll(extensions, VIDEO_FILES.extensions());
			Collections.addAll(extensions, AUDIO_FILES.extensions());
			Collections.addAll(extensions, SUBTITLE_FILES.extensions());
			
			chooser.setFileFilter(new FileNameExtensionFilter("Media files", extensions.toArray(new String[0])));
			chooser.setMultiSelectionEnabled(false);
			
			if (chooser.showOpenDialog(getWindow(evt.getSource())) == JFileChooser.APPROVE_OPTION) {
				// update text field
				mediaFileTextField.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		}
	};
	

	private static class Evaluator extends SwingWorker<String, Void> {
		
		private final String expression;
		private final Object bindingBean;
		

		private Evaluator(String expression, Object bindingBean) {
			this.expression = expression;
			this.bindingBean = bindingBean;
		}
		

		public String getExpression() {
			return expression;
		}
		

		@Override
		protected String doInBackground() throws Exception {
			ExpressionFormat format = new ExpressionFormat(expression) {
				
				@Override
				protected Object[] compile(String expression, Compilable engine) throws ScriptException {
					// simple expression format, everything as one expression
					return new Object[] { engine.compile(expression) };
				}
			};
			
			// evaluate expression with given bindings
			String value = format.format(bindingBean);
			
			// check for script exceptions
			if (format.caughtScriptException() != null) {
				throw format.caughtScriptException();
			}
			
			return value;
		}
		

		@Override
		public String toString() {
			try {
				return get(0, TimeUnit.SECONDS);
			} catch (Exception e) {
				return null;
			}
		}
	}
	

	private static class BindingTableModel extends AbstractTableModel {
		
		private final List<Evaluator> model = new ArrayList<Evaluator>();
		
		private final ExecutorService executor = Executors.newFixedThreadPool(2, new DefaultThreadFactory("Evaluator", Thread.MIN_PRIORITY));
		

		public void setModel(Collection<String> expressions, Object bindingBean) {
			// cancel old workers and clear model
			clear();
			
			for (String expression : expressions) {
				Evaluator evaluator = new Evaluator(expression, bindingBean) {
					
					@Override
					protected void done() {
						// update cell when computation is complete
						fireTableCellUpdated(this);
					}
				};
				
				// enqueue for background execution
				executor.execute(evaluator);
				
				model.add(evaluator);
			}
			
			// update view
			fireTableDataChanged();
		}
		

		public void clear() {
			for (Evaluator evaluator : model) {
				evaluator.cancel(true);
			}
			
			model.clear();
			
			// update view
			fireTableDataChanged();
		}
		

		public void fireTableCellUpdated(Evaluator element) {
			int index = model.indexOf(element);
			
			if (index >= 0) {
				fireTableCellUpdated(index, 1);
			}
		}
		

		@Override
		public String getColumnName(int column) {
			switch (column) {
				case 0:
					return "Expression";
				case 1:
					return "Value";
				default:
					return null;
			}
		}
		

		@Override
		public int getColumnCount() {
			return 2;
		}
		

		@Override
		public int getRowCount() {
			return model.size();
		}
		

		@Override
		public Class<?> getColumnClass(int column) {
			switch (column) {
				case 0:
					return String.class;
				case 1:
					return Future.class;
				default:
					return null;
			}
		}
		

		@Override
		public Object getValueAt(int row, int column) {
			switch (column) {
				case 0:
					return model.get(row).getExpression();
				case 1:
					return model.get(row);
				default:
					return null;
			}
		}
	}
	

	private static class ParameterTableModel extends AbstractTableModel {
		
		private final List<Entry<?, ?>> data;
		

		public ParameterTableModel(Map<?, ?> data) {
			this.data = new ArrayList<Entry<?, ?>>(data.entrySet());
		}
		

		@Override
		public int getRowCount() {
			return data.size();
		}
		

		@Override
		public int getColumnCount() {
			return 2;
		}
		

		@Override
		public String getColumnName(int column) {
			switch (column) {
				case 0:
					return "Parameter";
				case 1:
					return "Value";
				default:
					return null;
			}
		}
		

		@Override
		public Object getValueAt(int row, int column) {
			switch (column) {
				case 0:
					return data.get(row).getKey();
				case 1:
					return data.get(row).getValue();
				default:
					return null;
			}
		}
	}
	
}
