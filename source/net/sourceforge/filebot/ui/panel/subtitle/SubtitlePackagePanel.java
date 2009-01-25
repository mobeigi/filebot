
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventListener;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.EventListModel;


public class SubtitlePackagePanel extends JComponent {
	
	private final EventList<SubtitlePackage> model = new BasicEventList<SubtitlePackage>();
	
	
	public SubtitlePackagePanel() {
		setLayout(new BorderLayout());
		add(new JScrollPane(createList()), BorderLayout.CENTER);
	}
	

	public EventList<SubtitlePackage> getModel() {
		return model;
	}
	

	protected JList createList() {
		ObservableElementList<SubtitlePackage> observableList = new ObservableElementList<SubtitlePackage>(model, new SubtitlePackageConnector());
		
		JList list = new JList(new EventListModel<SubtitlePackage>(observableList));
		
		return list;
	}
	
	
	private static class SubtitlePackageConnector implements ObservableElementList.Connector<SubtitlePackage> {
		
		/**
		 * The list which contains the elements being observed via this
		 * {@link ObservableElementList.Connector}.
		 */
		private ObservableElementList<SubtitlePackage> list = null;
		
		
		public EventListener installListener(SubtitlePackage element) {
			PropertyChangeListener listener = new SubtitlePackageListener(element);
			element.getDownloadTask().addPropertyChangeListener(listener);
			
			return listener;
		}
		

		public void uninstallListener(SubtitlePackage element, EventListener listener) {
			element.getDownloadTask().removePropertyChangeListener((PropertyChangeListener) listener);
		}
		

		public void setObservableElementList(ObservableElementList<SubtitlePackage> list) {
			this.list = list;
		}
		
		
		protected class SubtitlePackageListener implements PropertyChangeListener {
			
			private final SubtitlePackage subtitlePackage;
			
			
			public SubtitlePackageListener(SubtitlePackage subtitlePackage) {
				this.subtitlePackage = subtitlePackage;
			}
			

			public void propertyChange(PropertyChangeEvent evt) {
				list.elementChanged(subtitlePackage);
			}
		}
		
	}
	
}
