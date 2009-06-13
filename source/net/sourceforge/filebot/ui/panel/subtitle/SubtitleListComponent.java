
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;


public class SubtitleListComponent extends JComponent {
	
	private EventList<SubtitlePackage> model = new BasicEventList<SubtitlePackage>();
	
	private EventSelectionModel<SubtitlePackage> selectionModel = new EventSelectionModel<SubtitlePackage>(model);
	
	private SubtitleListCellRenderer renderer = new SubtitleListCellRenderer();
	
	private JTextField filterEditor = new JTextField();
	

	public SubtitleListComponent() {
		JList list = new JList(createListModel(model));
		list.setFixedCellHeight(32);
		list.setCellRenderer(renderer);
		list.addMouseListener(mouseListener);
		
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
	

	protected ListModel createListModel(EventList<SubtitlePackage> source) {
		// allow filtering by language name and subtitle name
		MatcherEditor<SubtitlePackage> matcherEditor = new TextComponentMatcherEditor<SubtitlePackage>(filterEditor, new TextFilterator<SubtitlePackage>() {
			
			@Override
			public void getFilterStrings(List<String> list, SubtitlePackage element) {
				list.add(element.getLanguage().getName());
				list.add(element.getName());
			}
		});
		
		// filter list
		source = new FilterList<SubtitlePackage>(source, matcherEditor);
		
		// listen to changes (e.g. download progress)
		source = new ObservableElementList<SubtitlePackage>(source, GlazedLists.beanConnector(SubtitlePackage.class));
		
		// as list model
		return new EventListModel<SubtitlePackage>(source);
	}
	

	public EventList<SubtitlePackage> getModel() {
		return model;
	}
	

	public void setLanguageVisible(boolean visible) {
		renderer.getLanguageLabel().setVisible(visible);
	}
	

	private final MouseAdapter mouseListener = new MouseAdapter() {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e) && (e.getClickCount() == 2)) {
				JList list = (JList) e.getSource();
				
				for (Object value : list.getSelectedValues()) {
					final SubtitlePackage subtitle = (SubtitlePackage) value;
					
					subtitle.getDownload().execute();
				}
			}
		}
	};
	
	private final Action clearFilterAction = new AbstractAction(null, ResourceManager.getIcon("edit.clear")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			filterEditor.setText("");
		}
	};
	
}
