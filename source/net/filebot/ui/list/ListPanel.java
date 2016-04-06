package net.filebot.ui.list;

import static java.awt.Font.*;
import static java.util.stream.Collectors.*;
import static javax.swing.BorderFactory.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.datatransfer.Transferable;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.IntStream;

import javax.script.ScriptException;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.google.common.eventbus.Subscribe;

import net.filebot.ResourceManager;
import net.filebot.format.ExpressionFormat;
import net.filebot.ui.FileBotList;
import net.filebot.ui.FileBotListExportHandler;
import net.filebot.ui.transfer.LoadAction;
import net.filebot.ui.transfer.SaveAction;
import net.filebot.ui.transfer.TransferablePolicy;
import net.filebot.ui.transfer.TransferablePolicy.TransferAction;
import net.filebot.util.ui.DefaultFancyListCellRenderer;
import net.filebot.util.ui.LazyDocumentListener;
import net.filebot.util.ui.PrototypeCellValueUpdater;
import net.miginfocom.swing.MigLayout;

public class ListPanel extends JComponent {

	public static final String DEFAULT_SEQUENCE_FORMAT = "Sequence - {i.pad(2)}";
	public static final String DEFAULT_FILE_FORMAT = "{fn}";
	public static final String DEFAULT_EPISODE_FORMAT = "{n} - {s00e00} - [{airdate.format(/dd MMM YYYY/)}] - {t}";

	private RSyntaxTextArea editor = createEditor();
	private SpinnerNumberModel fromSpinnerModel = new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1);
	private SpinnerNumberModel toSpinnerModel = new SpinnerNumberModel(20, 0, Integer.MAX_VALUE, 1);

	private FileBotList<ListItem> list = new FileBotList<ListItem>();

	public ListPanel() {
		list.setTitle("Title");

		// need a fixed cell size for high performance scrolling
		list.getListComponent().setFixedCellHeight(28);
		list.getListComponent().getModel().addListDataListener(new PrototypeCellValueUpdater(list.getListComponent(), ""));

		list.getRemoveAction().setEnabled(true);

		list.setTransferablePolicy(new FileListTransferablePolicy(list::setTitle, this::setFormatTemplate, this::createItemSequence));

		FileBotListExportHandler<ListItem> exportHandler = new FileBotListExportHandler<ListItem>(list, (item, out) -> out.println(item.getFormattedValue()));
		list.setExportHandler(exportHandler);
		list.getTransferHandler().setClipboardHandler(exportHandler);

		list.getListComponent().setCellRenderer(new DefaultFancyListCellRenderer() {

			@Override
			protected void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.configureListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				ListItem item = (ListItem) value;
				String text = item.getFormattedValue(); // format just-in-time

				if (text.isEmpty()) {
					if (item.getFormat() != null && item.getFormat().caughtScriptException() != null) {
						setText(item.getFormat().caughtScriptException().getMessage());
					} else {
						setText("Expression yields no results for value " + item.getObject());
					}
					setIcon(ResourceManager.getIcon("status.warning"));
				} else {
					setText(text);
					setIcon(null);
				}
			}
		});

		JSpinner fromSpinner = new JSpinner(fromSpinnerModel);
		JSpinner toSpinner = new JSpinner(toSpinnerModel);

		fromSpinner.setEditor(new NumberEditor(fromSpinner, "#"));
		toSpinner.setEditor(new NumberEditor(toSpinner, "#"));

		RTextScrollPane editorScrollPane = new RTextScrollPane(editor, false);
		editorScrollPane.setLineNumbersEnabled(false);
		editorScrollPane.setFoldIndicatorEnabled(false);
		editorScrollPane.setIconRowHeaderEnabled(false);

		editorScrollPane.setVerticalScrollBarPolicy(RTextScrollPane.VERTICAL_SCROLLBAR_NEVER);
		editorScrollPane.setHorizontalScrollBarPolicy(RTextScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		editorScrollPane.setBackground(editor.getBackground());
		editorScrollPane.setViewportBorder(createEmptyBorder(2, 2, 2, 2));
		editorScrollPane.setOpaque(true);
		editorScrollPane.setBorder(new JTextField().getBorder());

		setLayout(new MigLayout("nogrid, fill, insets dialog", "align center", "[pref!, center][fill]"));

		JLabel patternLabel = new JLabel("Pattern:");
		add(patternLabel, "gapbefore indent");
		add(editorScrollPane, "gap related, growx, wmin 2cm, h pref!, sizegroupy editor");
		add(new JLabel("From:"), "gap 5mm");
		add(fromSpinner, "gap related, wmax 15mm, sizegroup spinner, sizegroupy editor");
		add(new JLabel("To:"), "gap 5mm");
		add(toSpinner, "gap related, wmax 15mm, sizegroup spinner, sizegroupy editor");
		add(newButton("Sequence", ResourceManager.getIcon("action.export"), evt -> createItemSequence()), "gap 7mm, gapafter indent, wrap paragraph");

		add(list, "grow");

		// panel with buttons that will be added inside the list component
		JPanel buttonPanel = new JPanel(new MigLayout("insets 1.2mm, nogrid, fill", "align center"));
		buttonPanel.add(new JButton(new LoadAction(list::getTransferablePolicy)));
		buttonPanel.add(new JButton(new SaveAction(list.getExportHandler())), "gap related");

		list.add(buttonPanel, BorderLayout.SOUTH);

		// initialize with default values
		SwingUtilities.invokeLater(() -> {
			if (list.getModel().isEmpty()) {
				createItemSequence();
			}
		});
	}

	private RSyntaxTextArea createEditor() {
		RSyntaxTextArea editor = new RSyntaxTextArea(new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_GROOVY) {
			@Override
			public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
				super.insertString(offs, str.replaceAll("\\R", ""), a); // FORCE SINGLE LINE
			}
		}, null, 1, 80);

		editor.setAntiAliasingEnabled(true);
		editor.setAnimateBracketMatching(false);
		editor.setAutoIndentEnabled(false);
		editor.setClearWhitespaceLinesEnabled(false);
		editor.setBracketMatchingEnabled(true);
		editor.setCloseCurlyBraces(false);
		editor.setCodeFoldingEnabled(false);
		editor.setHyperlinksEnabled(false);
		editor.setUseFocusableTips(false);
		editor.setHighlightCurrentLine(false);
		editor.setLineWrap(false);

		editor.setFont(new Font(MONOSPACED, PLAIN, 14));

		// update format on change
		editor.getDocument().addDocumentListener(new LazyDocumentListener(20) {

			private Color valid = editor.getForeground();
			private Color invalid = Color.red;

			@Override
			public void update(DocumentEvent evt) {
				try {
					String expression = editor.getText().trim();
					setFormat(expression.isEmpty() ? null : new ExpressionFormat(expression));
					editor.setForeground(valid);
				} catch (ScriptException e) {
					editor.setForeground(invalid);
				}
			}
		});

		return editor;
	}

	private ExpressionFormat format;
	private String template;

	public ListItem createItem(Object object, int i, int from, int to, List<?> context) {
		return new ListItem(new IndexedBindingBean(object, i, from, to, context), format);
	}

	public void setFormat(ExpressionFormat format) {
		this.format = format;

		// update items
		for (ListIterator<ListItem> itr = list.getModel().listIterator(); itr.hasNext();) {
			itr.set(new ListItem(itr.next().getBindings(), format));
		}
	}

	public void createItemSequence(List<?> objects) {
		List<ListItem> items = IntStream.range(1, objects.size()).mapToObj(i -> createItem(objects.get(i), i, 0, objects.size(), objects)).collect(toList());

		list.getListComponent().clearSelection();
		list.getModel().clear();
		list.getModel().addAll(items);
	}

	public void createItemSequence() {
		int from = fromSpinnerModel.getNumber().intValue();
		int to = toSpinnerModel.getNumber().intValue();

		List<Integer> context = IntStream.rangeClosed(from, to).boxed().collect(toList());
		List<ListItem> items = context.stream().map(it -> createItem(it, it.intValue(), from, to, context)).collect(toList());

		setFormatTemplate(DEFAULT_SEQUENCE_FORMAT);
		list.setTitle("Sequence");
		list.getListComponent().clearSelection();
		list.getModel().clear();
		list.getModel().addAll(items);
	}

	public void setFormatTemplate(String format) {
		if (template != format) {
			template = format;
			editor.setText(format);
		}
	}

	@Subscribe
	public void handle(Transferable transferable) throws Exception {
		TransferablePolicy handler = list.getTransferablePolicy();

		if (handler != null && handler.accept(transferable)) {
			handler.handleTransferable(transferable, TransferAction.PUT);
		}
	}

}
