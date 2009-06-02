
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;


public class SubtitleListComponent extends JComponent {
	
	private EventList<SubtitlePackage> model = new BasicEventList<SubtitlePackage>();
	
	private SubtitleListCellRenderer renderer = new SubtitleListCellRenderer();
	
	private JTextField filterEditor = new JTextField();
	

	public SubtitleListComponent() {
		// allow filtering by language name and subtitle name
		TextComponentMatcherEditor<SubtitlePackage> matcherEditor = new TextComponentMatcherEditor<SubtitlePackage>(filterEditor, new TextFilterator<SubtitlePackage>() {
			
			@Override
			public void getFilterStrings(List<String> list, SubtitlePackage element) {
				list.add(element.getLanguage().getName());
				list.add(element.getName());
			}
		});
		
		JList list = new JList(new EventListModel<SubtitlePackage>(new FilterList<SubtitlePackage>(model, matcherEditor)));
		list.setCellRenderer(renderer);
		list.setFixedCellHeight(35);
		
		EventSelectionModel<SubtitlePackage> selectionModel = new EventSelectionModel<SubtitlePackage>(model);
		selectionModel.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
		list.setSelectionModel(selectionModel);
		
		JButton clearButton = new JButton(clearFilterAction);
		clearButton.setOpaque(false);
		
		setLayout(new MigLayout("fill, nogrid", "[fill]", "[pref!][fill]"));
		
		add(new JLabel("Filter:"), "gap indent:push");
		add(filterEditor, "wmin 120px, gap rel");
		add(clearButton, "w 24px!, h 24px!");
		add(new JScrollPane(list), "newline");
	}
	

	public EventList<SubtitlePackage> getModel() {
		return model;
	}
	

	public void setLanguageVisible(boolean visible) {
		renderer.getLanguageLabel().setVisible(visible);
	}
	

	private final Action clearFilterAction = new AbstractAction(null, ResourceManager.getIcon("edit.clear")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			filterEditor.setText("");
		}
	};
	
}
