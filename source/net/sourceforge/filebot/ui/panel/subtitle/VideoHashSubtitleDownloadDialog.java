
package net.sourceforge.filebot.ui.panel.subtitle;


import static javax.swing.BorderFactory.*;
import static javax.swing.JOptionPane.*;
import static net.sourceforge.filebot.ui.NotificationLogging.*;
import static net.sourceforge.filebot.ui.panel.subtitle.SubtitleUtilities.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.filebot.web.VideoHashSubtitleService;
import net.sourceforge.tuned.FileUtilities;
import net.sourceforge.tuned.ui.AbstractBean;
import net.sourceforge.tuned.ui.EmptySelectionModel;
import net.sourceforge.tuned.ui.LinkButton;
import net.sourceforge.tuned.ui.RoundBorder;


class VideoHashSubtitleDownloadDialog extends JDialog {
	
	private final JPanel servicePanel = new JPanel(new MigLayout());
	private final List<VideoHashSubtitleServiceBean> services = new ArrayList<VideoHashSubtitleServiceBean>();
	
	private final JTable subtitleMappingTable = createTable();
	
	private ExecutorService downloadService;
	

	public VideoHashSubtitleDownloadDialog(Window owner) {
		super(owner, "Download Subtitles", ModalityType.MODELESS);
		
		JComponent content = (JComponent) getContentPane();
		content.setLayout(new MigLayout("fill, insets dialog, nogrid", "", "[fill][pref!]"));
		
		servicePanel.setBorder(new RoundBorder());
		servicePanel.setOpaque(false);
		servicePanel.setBackground(new Color(0xFAFAD2)); // LightGoldenRodYellow
		
		content.add(new JScrollPane(subtitleMappingTable), "grow, wrap");
		content.add(servicePanel, "gap after indent*2");
		
		content.add(new JButton(downloadAction), "tag ok");
		content.add(new JButton(finishAction), "tag cancel");
	}
	

	protected JTable createTable() {
		JTable table = new JTable(new SubtitleMappingTableModel());
		table.setDefaultRenderer(SubtitleMapping.class, new SubtitleMappingOptionRenderer());
		
		table.setRowHeight(24);
		table.setIntercellSpacing(new Dimension(5, 5));
		
		table.setBackground(Color.white);
		table.setAutoCreateRowSorter(true);
		table.setFillsViewportHeight(true);
		
		JComboBox editor = new SimpleComboBox();
		editor.setRenderer(new SubtitleOptionRenderer());
		
		// disable selection
		table.setSelectionModel(new EmptySelectionModel());
		editor.setFocusable(false);
		
		table.setDefaultEditor(SubtitleMapping.class, new DefaultCellEditor(editor) {
			
			@Override
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
				JComboBox editor = (JComboBox) super.getTableCellEditorComponent(table, value, isSelected, row, column);
				
				SubtitleMapping mapping = (SubtitleMapping) value;
				editor.setModel(new DefaultComboBoxModel(mapping.getOptions()));
				editor.setSelectedItem(mapping.getSelectedOption());
				
				return editor;
			}
		});
		
		return table;
	}
	

	public void setVideoFiles(File[] videoFiles) {
		subtitleMappingTable.setModel(new SubtitleMappingTableModel(videoFiles));
	}
	

	public void addSubtitleService(final VideoHashSubtitleService service) {
		final VideoHashSubtitleServiceBean serviceBean = new VideoHashSubtitleServiceBean(service);
		final LinkButton component = new LinkButton(serviceBean.getName(), ResourceManager.getIcon("database.go"), serviceBean.getLink());
		
		serviceBean.addPropertyChangeListener(new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (serviceBean.getState() == StateValue.STARTED) {
					component.setIcon(ResourceManager.getIcon("database.go"));
				} else {
					component.setIcon(ResourceManager.getIcon(serviceBean.getError() == null ? "database.ok" : "database.error"));
				}
				
				component.setToolTipText(serviceBean.getError() == null ? null : serviceBean.getError().getMessage());
			}
		});
		
		services.add(serviceBean);
		servicePanel.add(component);
	}
	

	public void startQuery(String languageName) {
		final SubtitleMappingTableModel mappingModel = (SubtitleMappingTableModel) subtitleMappingTable.getModel();
		
		// query services concurrently
		for (VideoHashSubtitleServiceBean service : services) {
			QueryTask task = new QueryTask(service, mappingModel.getVideoFiles(), languageName) {
				
				@Override
				protected void done() {
					try {
						Map<File, List<SubtitleDescriptorBean>> subtitles = get();
						
						// update subtitle options
						for (SubtitleMapping subtitleMapping : mappingModel) {
							List<SubtitleDescriptorBean> options = subtitles.get(subtitleMapping.getVideoFile());
							
							if (options != null && options.size() > 0) {
								subtitleMapping.addOptions(options);
							}
						}
						
						// make subtitle column visible
						mappingModel.setOptionColumnVisible(true);
					} catch (Exception e) {
						Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
					}
				}
			};
			
			// start background worker
			task.execute();
		}
	}
	

	private Boolean showConfirmReplaceDialog(List<?> files) {
		JList existingFilesComponent = new JList(files.toArray()) {
			
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				// adjust component size
				return new Dimension(80, 50);
			}
		};
		
		Object[] message = new Object[] { "Replace existing subtitle files?", new JScrollPane(existingFilesComponent) };
		Object[] options = new Object[] { "Replace All", "Skip All", "Cancel" };
		JOptionPane optionPane = new JOptionPane(message, WARNING_MESSAGE, YES_NO_CANCEL_OPTION, null, options);
		
		// display option dialog
		optionPane.createDialog(VideoHashSubtitleDownloadDialog.this, "Replace").setVisible(true);
		
		// replace all
		if (options[0] == optionPane.getValue())
			return true;
		
		// skip all
		if (options[1] == optionPane.getValue())
			return false;
		
		// cancel
		return null;
	}
	

	private final Action downloadAction = new AbstractAction("Download", ResourceManager.getIcon("dialog.continue")) {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			// disable any active cell editor
			if (subtitleMappingTable.getCellEditor() != null) {
				subtitleMappingTable.getCellEditor().cancelCellEditing();
			}
			
			// don't allow restart of download as long as there are still unfinished download tasks
			if (downloadService != null && !downloadService.isTerminated()) {
				return;
			}
			
			final SubtitleMappingTableModel mappingModel = (SubtitleMappingTableModel) subtitleMappingTable.getModel();
			
			// collect the subtitles that will be fetched
			List<DownloadTask> downloadQueue = new ArrayList<DownloadTask>();
			
			for (SubtitleMapping mapping : mappingModel) {
				SubtitleDescriptorBean subtitleBean = mapping.getSelectedOption();
				
				if (subtitleBean != null && subtitleBean.getState() == null) {
					downloadQueue.add(new DownloadTask(subtitleBean, mapping.getSubtitleFile()));
				}
			}
			
			// collect downloads that will override a file
			List<DownloadTask> confirmReplaceDownloadQueue = new ArrayList<DownloadTask>();
			List<String> existingFiles = new ArrayList<String>();
			
			for (DownloadTask download : downloadQueue) {
				if (download.getDestination().exists()) {
					confirmReplaceDownloadQueue.add(download);
					existingFiles.add(download.getDestination().getName());
				}
			}
			
			// confirm replace
			if (confirmReplaceDownloadQueue.size() > 0) {
				Boolean option = showConfirmReplaceDialog(existingFiles);
				
				// abort the operation altogether
				if (option == null) {
					return;
				}
				
				// don't replace any files
				if (option == false) {
					downloadQueue.removeAll(confirmReplaceDownloadQueue);
				}
			}
			
			// start download
			if (downloadQueue.size() > 0) {
				downloadService = Executors.newFixedThreadPool(2);
				
				for (DownloadTask downloadTask : downloadQueue) {
					downloadTask.getSubtitleBean().setState(StateValue.PENDING);
					downloadService.execute(downloadTask);
				}
				
				// terminate after all downloads have been completed
				downloadService.shutdown();
			}
		}
	};
	
	private final Action finishAction = new AbstractAction("Close", ResourceManager.getIcon("dialog.cancel")) {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			if (downloadService != null) {
				downloadService.shutdownNow();
			}
			
			setVisible(false);
			dispose();
		}
	};
	

	private static class SubtitleMappingOptionRenderer extends DefaultTableCellRenderer {
		
		private final JComboBox optionComboBox = new SimpleComboBox();
		

		public SubtitleMappingOptionRenderer() {
			optionComboBox.setRenderer(new SubtitleOptionRenderer());
		}
		

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			SubtitleMapping mapping = (SubtitleMapping) value;
			SubtitleDescriptorBean subtitleBean = mapping.getSelectedOption();
			
			// render combobox for subtitle options
			if (mapping.isEditable()) {
				optionComboBox.setModel(new DefaultComboBoxModel(new Object[] { subtitleBean }));
				return optionComboBox;
			}
			
			// render status label
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setForeground(table.getForeground());
			
			if (subtitleBean == null) {
				// no subtitles found
				setText("No subtitles found");
				setIcon(null);
				setForeground(Color.gray);
			} else if (subtitleBean.getState() == StateValue.PENDING) {
				// download in the queue
				setText(subtitleBean.getText());
				setIcon(ResourceManager.getIcon("worker.pending"));
			} else if (subtitleBean.getState() == StateValue.STARTED) {
				// download in progress
				setText(subtitleBean.getText());
				setIcon(ResourceManager.getIcon("action.fetch"));
			} else {
				// download complete
				setText(mapping.getSubtitleFile().getName());
				setIcon(ResourceManager.getIcon("status.ok"));
			}
			
			return this;
		}
	}
	

	private static class SubtitleOptionRenderer extends DefaultListCellRenderer {
		
		private final Border padding = createEmptyBorder(3, 3, 3, 3);
		

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
			setBorder(padding);
			
			SubtitleDescriptorBean subtitleBean = (SubtitleDescriptorBean) value;
			setText(subtitleBean.getText());
			setIcon(subtitleBean.getError() == null ? subtitleBean.getIcon() : ResourceManager.getIcon("status.warning"));
			
			return this;
		}
	}
	

	private static class SubtitleMappingTableModel extends AbstractTableModel implements Iterable<SubtitleMapping> {
		
		private final SubtitleMapping[] data;
		
		private boolean optionColumnVisible = false;
		

		public SubtitleMappingTableModel(File... videoFiles) {
			data = new SubtitleMapping[videoFiles.length];
			
			for (int i = 0; i < videoFiles.length; i++) {
				data[i] = new SubtitleMapping(videoFiles[i]);
				data[i].addPropertyChangeListener(new SubtitleMappingListener(i));
			}
		}
		

		public List<File> getVideoFiles() {
			return new AbstractList<File>() {
				
				@Override
				public File get(int index) {
					return data[index].getVideoFile();
				}
				

				@Override
				public int size() {
					return data.length;
				}
			};
		}
		

		@Override
		public Iterator<SubtitleMapping> iterator() {
			return Arrays.asList(data).iterator();
		}
		

		public void setOptionColumnVisible(boolean optionColumnVisible) {
			if (this.optionColumnVisible == optionColumnVisible)
				return;
			
			this.optionColumnVisible = optionColumnVisible;
			
			// update columns
			fireTableStructureChanged();
		}
		

		@Override
		public int getColumnCount() {
			return optionColumnVisible ? 2 : 1;
		}
		

		@Override
		public String getColumnName(int column) {
			switch (column) {
				case 0:
					return "Video";
				case 1:
					return "Subtitle";
			}
			
			return null;
		}
		

		@Override
		public int getRowCount() {
			return data.length;
		}
		

		@Override
		public Object getValueAt(int row, int column) {
			switch (column) {
				case 0:
					return data[row].getVideoFile().getName();
				case 1:
					return data[row];
			}
			
			return null;
		}
		

		@Override
		public void setValueAt(Object value, int row, int column) {
			data[row].setSelectedOption((SubtitleDescriptorBean) value);
		}
		

		@Override
		public boolean isCellEditable(int row, int column) {
			return column == 1 && data[row].isEditable();
		}
		

		@Override
		public Class<?> getColumnClass(int column) {
			switch (column) {
				case 0:
					return String.class;
				case 1:
					return SubtitleMapping.class;
			}
			
			return null;
		}
		

		private class SubtitleMappingListener implements PropertyChangeListener {
			
			private final int index;
			

			public SubtitleMappingListener(int index) {
				this.index = index;
			}
			

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				// update state and subtitle options
				fireTableRowsUpdated(index, index);
			}
		}
	}
	

	private static class SubtitleMapping extends AbstractBean {
		
		private final File videoFile;
		
		private SubtitleDescriptorBean selectedOption;
		private List<SubtitleDescriptorBean> options = new ArrayList<SubtitleDescriptorBean>();
		

		public SubtitleMapping(File videoFile) {
			this.videoFile = videoFile;
		}
		

		public File getVideoFile() {
			return videoFile;
		}
		

		public File getSubtitleFile() {
			if (selectedOption == null)
				throw new IllegalStateException("Selected option must not be null");
			
			return new File(videoFile.getParentFile(), FileUtilities.getName(videoFile) + '.' + selectedOption.getType());
		}
		

		public boolean isEditable() {
			return selectedOption != null && (selectedOption.getState() == null || selectedOption.getError() != null);
		}
		

		public SubtitleDescriptorBean getSelectedOption() {
			return selectedOption;
		}
		

		public void setSelectedOption(SubtitleDescriptorBean selectedOption) {
			if (this.selectedOption != null) {
				this.selectedOption.removePropertyChangeListener(selectedOptionListener);
			}
			
			this.selectedOption = selectedOption;
			this.selectedOption.addPropertyChangeListener(selectedOptionListener);
			
			firePropertyChange("selectedOption", null, this.selectedOption);
		}
		

		public SubtitleDescriptorBean[] getOptions() {
			return options.toArray(new SubtitleDescriptorBean[0]);
		}
		

		public void addOptions(List<SubtitleDescriptorBean> options) {
			this.options.addAll(options);
			
			if (selectedOption == null && options.size() > 0) {
				setSelectedOption(options.get(0));
			}
		}
		

		private final PropertyChangeListener selectedOptionListener = new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				firePropertyChange("selectedOption", null, selectedOption);
			}
		};
	}
	

	private static class SubtitleDescriptorBean extends AbstractBean {
		
		private final SubtitleDescriptor subtitle;
		private final VideoHashSubtitleServiceBean source;
		
		private StateValue state;
		private Exception error;
		

		public SubtitleDescriptorBean(SubtitleDescriptor subtitle, VideoHashSubtitleServiceBean source) {
			this.subtitle = subtitle;
			this.source = source;
		}
		

		public String getText() {
			return subtitle.getName() + '.' + subtitle.getType();
		}
		

		public Icon getIcon() {
			return source.getIcon();
		}
		

		public String getType() {
			return subtitle.getType();
		}
		

		public ByteBuffer fetch() throws Exception {
			setState(StateValue.STARTED);
			
			try {
				return subtitle.fetch();
			} catch (Exception e) {
				// remember exception
				error = e;
				
				// rethrow exception
				throw e;
			} finally {
				setState(StateValue.DONE);
			}
		}
		

		public Exception getError() {
			return error;
		}
		

		public StateValue getState() {
			return state;
		}
		

		public void setState(StateValue state) {
			this.state = state;
			firePropertyChange("state", null, this.state);
		}
		

		@Override
		public String toString() {
			return getText();
		}
	}
	

	private static class QueryTask extends SwingWorker<Map<File, List<SubtitleDescriptorBean>>, Void> {
		
		private final VideoHashSubtitleServiceBean service;
		
		private final File[] videoFiles;
		private final String languageName;
		

		public QueryTask(VideoHashSubtitleServiceBean service, Collection<File> videoFiles, String languageName) {
			this.service = service;
			this.videoFiles = videoFiles.toArray(new File[0]);
			this.languageName = languageName;
		}
		

		@Override
		protected Map<File, List<SubtitleDescriptorBean>> doInBackground() throws Exception {
			Map<File, List<SubtitleDescriptorBean>> subtitleSet = new HashMap<File, List<SubtitleDescriptorBean>>();
			
			for (final Entry<File, List<SubtitleDescriptor>> result : service.getSubtitleList(videoFiles, languageName).entrySet()) {
				List<SubtitleDescriptorBean> subtitles = new ArrayList<SubtitleDescriptorBean>();
				
				// associate subtitles with services
				for (SubtitleDescriptor subtitleDescriptor : result.getValue()) {
					subtitles.add(new SubtitleDescriptorBean(subtitleDescriptor, service));
				}
				
				subtitleSet.put(result.getKey(), subtitles);
			}
			
			return subtitleSet;
		}
	}
	

	private static class DownloadTask extends SwingWorker<File, Void> {
		
		private final SubtitleDescriptorBean subtitle;
		private final File destination;
		

		public DownloadTask(SubtitleDescriptorBean subtitle, File destination) {
			this.subtitle = subtitle;
			this.destination = destination;
		}
		

		public SubtitleDescriptorBean getSubtitleBean() {
			return subtitle;
		}
		

		public File getDestination() {
			return destination;
		}
		

		@Override
		protected File doInBackground() {
			try {
				// fetch subtitle
				ByteBuffer data = subtitle.fetch();
				
				if (isCancelled())
					return null;
				
				// save to file
				write(data, destination);
				
				return destination;
			} catch (Exception e) {
				UILogger.log(Level.WARNING, e.getMessage(), e);
			}
			
			return null;
		}
	}
	

	private static class VideoHashSubtitleServiceBean extends AbstractBean {
		
		private final VideoHashSubtitleService service;
		
		private StateValue state;
		private Throwable error;
		

		public VideoHashSubtitleServiceBean(VideoHashSubtitleService service) {
			this.service = service;
			this.state = StateValue.PENDING;
		}
		

		public String getName() {
			return service.getName();
		}
		

		public Icon getIcon() {
			return service.getIcon();
		}
		

		public URI getLink() {
			return service.getLink();
		}
		

		public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] files, String languageName) throws Exception {
			setState(StateValue.STARTED);
			
			try {
				return service.getSubtitleList(files, languageName);
			} catch (Exception e) {
				// remember error
				error = e;
				
				// rethrow error
				throw e;
			} finally {
				setState(StateValue.DONE);
			}
		}
		

		private void setState(StateValue state) {
			this.state = state;
			firePropertyChange("state", null, this.state);
		}
		

		public StateValue getState() {
			return state;
		}
		

		public Throwable getError() {
			return error;
		}
		
	}
	
}
