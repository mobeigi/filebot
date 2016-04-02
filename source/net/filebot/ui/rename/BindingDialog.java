package net.filebot.ui.rename;

import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.MediaTypes.*;
import static net.filebot.UserFiles.*;
import static net.filebot.media.XattrMetaInfo.*;
import static net.filebot.util.RegularExpressions.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import net.filebot.ResourceManager;
import net.filebot.format.ExpressionFormat;
import net.filebot.format.MediaBindingBean;
import net.filebot.mediainfo.MediaInfo;
import net.filebot.mediainfo.MediaInfo.StreamKind;
import net.filebot.util.DefaultThreadFactory;
import net.filebot.util.FileUtilities.ExtensionFileFilter;
import net.filebot.util.ui.LazyDocumentListener;
import net.miginfocom.swing.MigLayout;

class BindingDialog extends JDialog {

	private final JTextField infoTextField = new JTextField();
	private final JTextField mediaFileTextField = new JTextField();

	private final Format infoObjectFormat;
	private final BindingTableModel bindingModel = new BindingTableModel();

	private boolean submit = false;

	public BindingDialog(Window owner, String title, Format infoObjectFormat, boolean editable) {
		super(owner, title, ModalityType.DOCUMENT_MODAL);
		this.infoObjectFormat = infoObjectFormat;

		JComponent root = (JComponent) getContentPane();
		root.setLayout(new MigLayout("nogrid, fill, insets dialog"));

		// decorative tabbed pane
		JTabbedPane inputContainer = new JTabbedPane();
		inputContainer.setFocusable(false);

		JPanel inputPanel = new JPanel(new MigLayout("nogrid, fill"));
		inputPanel.setOpaque(false);

		inputPanel.add(new JLabel("Name:"), "wrap 2px");
		inputPanel.add(infoTextField, "hmin 20px, growx, wrap paragraph");

		inputPanel.add(new JLabel("Media File:"), "wrap 2px");
		inputPanel.add(mediaFileTextField, "hmin 20px, growx");
		inputPanel.add(createImageButton(mediaInfoAction), "gap rel, w 28px!, h 28px!");
		inputPanel.add(createImageButton(selectFileAction), "gap rel, w 28px!, h 28px!, wrap paragraph");
		inputContainer.add("Bindings", inputPanel);
		root.add(inputContainer, "growx, wrap paragraph");

		root.add(new JLabel("Preview:"), "gap 5px, wrap 2px");
		root.add(new JScrollPane(createBindingTable(bindingModel)), "growx, wrap paragraph:push");

		if (editable) {
			root.add(new JButton(approveAction), "tag apply");
			root.add(new JButton(cancelAction), "tag cancel");
		} else {
			root.add(new JButton(okAction), "tag apply");
		}

		// update preview on change
		DocumentListener changeListener = new LazyDocumentListener(1000) {

			@Override
			public void update(DocumentEvent evt) {
				// ignore lazy events that come in after the window has been closed
				if (bindingModel.executor.isShutdown())
					return;

				bindingModel.setModel(getSampleExpressions(), new MediaBindingBean(getInfoObject(), getMediaFile()));
			}
		};

		// update example bindings on change
		infoTextField.getDocument().addDocumentListener(changeListener);
		mediaFileTextField.getDocument().addDocumentListener(changeListener);

		// disabled by default
		infoTextField.setEnabled(false);
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
				finish(false);
			}
		});

		mediaFileTextField.setEditable(editable);
		infoTextField.setEditable(editable);
		selectFileAction.setEnabled(editable);

		// initialize window properties
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setSize(420, 520);
	}

	private JTable createBindingTable(TableModel model) {
		JTable table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		table.setAutoCreateColumnsFromModel(true);
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

	private List<String> getSampleExpressions() {
		String expressions = ResourceBundle.getBundle(getClass().getName()).getString("expressions");
		return COMMA.splitAsStream(expressions).collect(toList());
	}

	public boolean submit() {
		return submit;
	}

	private void finish(boolean submit) {
		this.submit = submit;

		// cancel background evaluators
		bindingModel.executor.shutdownNow();

		setVisible(false);
		dispose();
	}

	public void setInfoObject(Object info) {
		infoTextField.putClientProperty("model", info);
		infoTextField.setText(info == null ? "" : infoObjectFormat.format(info));
	}

	public void setMediaFile(File mediaFile) {
		mediaFileTextField.setText(mediaFile == null ? "" : mediaFile.getAbsolutePath());
	}

	public Object getInfoObject() {
		return infoTextField.getClientProperty("model");
	}

	public File getMediaFile() {
		File file = new File(mediaFileTextField.getText());

		// allow only absolute paths
		return file.isAbsolute() ? file : null;
	}

	protected final Action hideAction = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			finish(true);
		}
	};

	protected final Action approveAction = new AbstractAction("Use Bindings", ResourceManager.getIcon("dialog.continue")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			// check episode and media file
			if (getInfoObject() == null) {
				// illegal episode string
				log.warning(format("Failed to parse episode: '%s'", infoTextField.getText()));
			} else if (getMediaFile() == null && !mediaFileTextField.getText().isEmpty()) {
				// illegal file path
				log.warning(format("Invalid media file: '%s'", mediaFileTextField.getText()));
			} else {
				// everything seems to be in order
				finish(true);
			}
		}
	};

	protected final Action okAction = new AbstractAction("OK") {

		@Override
		public void actionPerformed(ActionEvent evt) {
			finish(true);
		}
	};

	protected final Action cancelAction = new AbstractAction("Cancel", ResourceManager.getIcon("dialog.cancel")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			finish(false);
		}
	};

	protected final Action mediaInfoAction = new AbstractAction("Open MediaInfo", ResourceManager.getIcon("action.properties")) {

		private Map<StreamKind, List<Map<String, String>>> getMediaInfo(File file) {
			try {
				return MediaInfo.snapshot(file);
			} catch (Exception e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				return null;
			}
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			Map<StreamKind, List<Map<String, String>>> mediaInfo = getMediaInfo(getMediaFile());

			// check if we could get some info
			if (mediaInfo == null)
				return;

			// create table tab for each stream
			JTabbedPane tabbedPane = new JTabbedPane();

			ResourceBundle bundle = ResourceBundle.getBundle(BindingDialog.class.getName());
			RowFilter<Object, Object> excludeRowFilter = RowFilter.notFilter(RowFilter.regexFilter(bundle.getString("parameter.exclude")));

			for (StreamKind streamKind : mediaInfo.keySet()) {
				for (Map<String, String> parameters : mediaInfo.get(streamKind)) {
					JPanel panel = new JPanel(new MigLayout("fill"));
					panel.setOpaque(false);

					JTable table = new JTable(new ParameterTableModel(parameters));
					table.setAutoCreateRowSorter(true);
					table.setAutoCreateColumnsFromModel(true);
					table.setFillsViewportHeight(true);

					table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
					table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

					table.setBackground(Color.white);
					table.setGridColor(new Color(0xEEEEEE));
					table.setRowHeight(25);

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
			dialog.setLocationRelativeTo(BindingDialog.this);

			dialog.setVisible(true);
		}

	};

	protected final Action selectFileAction = new AbstractAction("Select Media File", ResourceManager.getIcon("action.load")) {

		@Override
		public void actionPerformed(ActionEvent evt) {
			ExtensionFileFilter mediaFiles = combineFilter(VIDEO_FILES, AUDIO_FILES, SUBTITLE_FILES);
			List<File> file = showLoadDialogSelectFiles(false, false, getMediaFile(), mediaFiles, (String) getValue(NAME), evt);

			if (file.size() > 0) {
				// update text field
				mediaFileTextField.setText(file.get(0).getAbsolutePath());

				// set info object from xattr if possible
				Object object = xattr.getMetaInfo(file.get(0));
				if (object != null && infoObjectFormat.format(object) != null) {
					setInfoObject(object);
				}
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
				protected Object[] compile(String expression) throws ScriptException {
					// simple expression format, everything as one expression
					return new Object[] { compileScriptlet(expression) };
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
