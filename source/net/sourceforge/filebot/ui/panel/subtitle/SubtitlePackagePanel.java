
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventListener;

import javax.swing.JComponent;
import javax.swing.JList;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.tuned.DownloadTask;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.EventListModel;


public class SubtitlePackagePanel extends JComponent {
	
	private final EventList<SubtitlePackage> model = new BasicEventList<SubtitlePackage>();
	
	
	public SubtitlePackagePanel() {
		setLayout(new BorderLayout());
		add(createList(), BorderLayout.CENTER);
		model.add(new SubtitlePackage(new SubtitleDescriptor() {
			
			@Override
			public DownloadTask createDownloadTask() {
				return null;
			}
			

			@Override
			public String getArchiveType() {
				return ArchiveType.ZIP.getExtension();
			}
			

			@Override
			public String getLanguageName() {
				return "english";
			}
			

			@Override
			public String getName() {
				return "Firefly 1x01 The Train Job.srt";
			}
			
		}));
	}
	

	public EventList<SubtitlePackage> getModel() {
		return model;
	}
	

	protected JComponent createList() {
		ObservableElementList<SubtitlePackage> observableList = new ObservableElementList<SubtitlePackage>(model, new SubtitlePackageConnector());
		
		JList list = new JList(new EventListModel<SubtitlePackage>(observableList));
		
		list.setCellRenderer(new SubtitleCellRenderer());
		list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		list.setVisibleRowCount(-1);
		
		return list;
	}
	
	
	private static class SubtitlePackageConnector implements ObservableElementList.Connector<SubtitlePackage> {
		
		/**
		 * The list which contains the elements being observed via this
		 * {@link ObservableElementList.Connector}.
		 */
		private ObservableElementList<SubtitlePackage> list = null;
		
		
		public EventListener installListener(final SubtitlePackage element) {
			PropertyChangeListener listener = new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					list.elementChanged(element);
				}
			};
			
			return listener;
		}
		

		public void uninstallListener(SubtitlePackage element, EventListener listener) {
			element.getDownloadTask().removePropertyChangeListener((PropertyChangeListener) listener);
		}
		

		@SuppressWarnings("unchecked")
		@Override
		public void setObservableElementList(ObservableElementList<? extends SubtitlePackage> list) {
			this.list = (ObservableElementList<SubtitlePackage>) list;
		}
		
	}
	
}
