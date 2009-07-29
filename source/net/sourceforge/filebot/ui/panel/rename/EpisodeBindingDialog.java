
package net.sourceforge.filebot.ui.panel.rename;


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
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.format.EpisodeBindingBean;
import net.sourceforge.filebot.format.ExpressionFormat;
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
		
		// create image button from action
		JButton selectFileButton = new JButton(selectFileAction);
		selectFileButton.setHideActionText(true);
		selectFileButton.setOpaque(false);
		
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
		inputPanel.add(selectFileButton, "gap rel, w 26px!, h 24px!, wrap paragraph");
		
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
		
		episodeTextField.getDocument().addDocumentListener(changeListener);
		mediaFileTextField.getDocument().addDocumentListener(changeListener);
		
		// finish dialog and close window manually
		addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				finish(Option.CANCEL);
			}
		});
		
		// initialize window properties
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setLocation(getPreferredLocation(this));
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
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				
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
		episodeTextField.setText(episode == null ? "" : EpisodeFormat.getInstance().format(episode));
	}
	

	public void setMediaFile(File mediaFile) {
		mediaFileTextField.setText(mediaFile == null ? "" : mediaFile.getAbsolutePath());
	}
	

	public Episode getEpisode() {
		try {
			return EpisodeFormat.getInstance().parseObject(episodeTextField.getText());
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
				Logger.getLogger("ui").warning(String.format("Failed to parse episode: '%s'", episodeTextField.getText()));
			} else if (getMediaFile() == null && !mediaFileTextField.getText().isEmpty()) {
				// illegal file path
				Logger.getLogger("ui").warning(String.format("Invalid media file: '%s'", mediaFileTextField.getText()));
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
	
	protected final Action selectFileAction = new AbstractAction("Select File", ResourceManager.getIcon("action.load")) {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			JFileChooser chooser = new JFileChooser();
			chooser.setSelectedFile(getMediaFile());
			
			// collect media file extensions (video, audio and subtitle files)
			List<String> extensions = new ArrayList<String>();
			extensions.addAll(MediaTypes.getExtensionList("video"));
			extensions.addAll(MediaTypes.getExtensionList("audio"));
			extensions.addAll(MediaTypes.getExtensionList("subtitle"));
			
			chooser.setFileFilter(new FileNameExtensionFilter("Media files", extensions.toArray(new String[0])));
			chooser.setMultiSelectionEnabled(false);
			
			if (chooser.showOpenDialog(getWindow(evt.getSource())) == JFileChooser.APPROVE_OPTION) {
				File selectedFile = chooser.getSelectedFile();
				
				if (selectedFile.isFile()) {
					try {
						// display media info
						MediaInfoPane.showMessageDialog(getWindow(evt.getSource()), selectedFile);
					} catch (LinkageError e) {
						Logger.getLogger("ui").log(Level.SEVERE, "Unable to load native library 'mediainfo'", e);
					}
					
					// update text field
					mediaFileTextField.setText(selectedFile.getAbsolutePath());
				}
			}
		}
	};
	

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
		
	}
	
}
