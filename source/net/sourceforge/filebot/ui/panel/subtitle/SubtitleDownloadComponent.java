
package net.sourceforge.filebot.ui.panel.subtitle;


import static net.sourceforge.filebot.FileBotUtilities.*;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

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
import net.sourceforge.filebot.subtitle.SubtitleElement;
import net.sourceforge.filebot.subtitle.SubtitleFormat;
import net.sourceforge.filebot.subtitle.SubtitleReader;
import net.sourceforge.filebot.ui.panel.subtitle.SubtitlePackage.Download.Phase;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.tuned.ByteBufferInputStream;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.ui.ListView;


public class SubtitleDownloadComponent extends JComponent {
	
	private EventList<SubtitlePackage> packages = new BasicEventList<SubtitlePackage>();
	
	private EventList<MemoryFile> files = new BasicEventList<MemoryFile>();
	
	private SubtitlePackageCellRenderer renderer = new SubtitlePackageCellRenderer();
	
	private JTextField filterEditor = new JTextField();
	

	public SubtitleDownloadComponent() {
		JList packageList = new JList(createPackageListModel());
		packageList.setFixedCellHeight(32);
		packageList.setCellRenderer(renderer);
		
		// better selection behaviour
		EventSelectionModel<SubtitlePackage> packageSelection = new EventSelectionModel<SubtitlePackage>(packages);
		packageSelection.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
		packageList.setSelectionModel(packageSelection);
		
		// context menu and fetch on double click
		packageList.addMouseListener(packageListMouseHandler);
		
		// file list view
		JList fileList = new ListView(createFileListModel()) {
			
			@Override
			protected Icon convertValueToIcon(Object value) {
				if (SUBTITLE_FILES.accept(value.toString()))
					return ResourceManager.getIcon("file.subtitle");
				
				return ResourceManager.getIcon("file.unknown");
			}
		};
		
		fileList.setDragEnabled(true);
		fileList.setTransferHandler(new DefaultTransferHandler(null, new MemoryFileListExportHandler()));
		fileList.addMouseListener(fileListMouseHandler);
		
		JButton clearButton = new JButton(clearFilterAction);
		clearButton.setOpaque(false);
		
		JButton exportButton = new JButton(exportToFolderAction);
		exportButton.setOpaque(false);
		
		setLayout(new MigLayout("fill, nogrid", "[fill]", "[pref!][fill]"));
		
		add(new JLabel("Filter:"), "gap indent:push");
		add(filterEditor, "wmin 120px, gap rel");
		add(clearButton, "w 24px!, h 24px!");
		add(new JScrollPane(packageList), "newline");
		
		JScrollPane scrollPane = new JScrollPane(fileList);
		scrollPane.setViewportBorder(new LineBorder(fileList.getBackground()));
		add(scrollPane, "newline");
		add(exportButton, "w pref!, h pref!");
	}
	

	protected ListModel createPackageListModel() {
		// allow filtering by language name and subtitle name
		MatcherEditor<SubtitlePackage> matcherEditor = new TextComponentMatcherEditor<SubtitlePackage>(filterEditor, new TextFilterator<SubtitlePackage>() {
			
			@Override
			public void getFilterStrings(List<String> list, SubtitlePackage element) {
				list.add(element.getLanguage().getName());
				list.add(element.getName());
			}
		});
		
		// source list
		EventList<SubtitlePackage> source = getPackageModel();
		
		// filter list
		source = new FilterList<SubtitlePackage>(source, matcherEditor);
		
		// listen to changes (e.g. download progress)
		source = new ObservableElementList<SubtitlePackage>(source, GlazedLists.beanConnector(SubtitlePackage.class));
		
		// as list model
		return new EventListModel<SubtitlePackage>(source);
	}
	

	protected ListModel createFileListModel() {
		// as list model
		return new EventListModel<MemoryFile>(getFileModel());
	}
	

	public void reset() {
		// cancel and reset download workers
		for (SubtitlePackage subtitle : packages) {
			subtitle.reset();
		}
		
		files.clear();
	}
	

	public EventList<SubtitlePackage> getPackageModel() {
		return packages;
	}
	

	public EventList<MemoryFile> getFileModel() {
		return files;
	}
	

	public void setLanguageVisible(boolean visible) {
		renderer.getLanguageLabel().setVisible(visible);
	}
	

	private void fetchAll(Object[] selection) {
		for (Object value : selection) {
			fetch((SubtitlePackage) value);
		}
	}
	

	private void fetch(final SubtitlePackage subtitle) {
		if (subtitle.getDownload().isStarted()) {
			// download has been started already
			return;
		}
		
		// listen to download
		subtitle.addPropertyChangeListener(new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() == Phase.DONE) {
					try {
						files.addAll(subtitle.getDownload().get());
					} catch (CancellationException e) {
						// ignore cancellation
					} catch (Exception e) {
						Logger.getLogger("ui").log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
						
						// reset download
						subtitle.reset();
					}
					
					// listener no longer required
					subtitle.removePropertyChangeListener(this);
				}
			}
		});
		
		// enqueue worker
		subtitle.getDownload().start();
	}
	

	private final Action clearFilterAction = new AbstractAction(null, ResourceManager.getIcon("edit.clear")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			filterEditor.setText("");
		}
	};
	
	private final Action exportToFolderAction = new AbstractAction("Export") {
		
		@Override
		public void actionPerformed(ActionEvent evt) {
			JComponent source = (JComponent) evt.getSource();
			
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			
			if (fileChooser.showSaveDialog(source) == JFileChooser.APPROVE_OPTION) {
				File folder = fileChooser.getSelectedFile();
				
				for (MemoryFile file : files) {
					try {
						FileChannel fileChannel = new FileOutputStream(new File(folder, file.getName())).getChannel();
						
						try {
							fileChannel.write(file.getData());
						} finally {
							fileChannel.close();
						}
					} catch (IOException e) {
						Logger.getLogger("ui").log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
		}
	};
	
	private final MouseListener packageListMouseHandler = new MouseAdapter() {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			// fetch on double click
			if (SwingUtilities.isLeftMouseButton(e) && (e.getClickCount() == 2)) {
				JList list = (JList) e.getSource();
				
				fetchAll(list.getSelectedValues());
			}
		}
		

		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}
		

		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}
		

		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				JList list = (JList) e.getSource();
				
				int index = list.locationToIndex(e.getPoint());
				
				if (index >= 0 && !list.isSelectedIndex(index)) {
					// auto-select clicked element
					list.setSelectedIndex(index);
				}
				
				final Object[] selection = list.getSelectedValues();
				
				Action downloadAction = new AbstractAction("Download", ResourceManager.getIcon("package.fetch")) {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						fetchAll(selection);
					}
				};
				
				downloadAction.setEnabled(isPending(selection));
				
				JPopupMenu contextMenu = new JPopupMenu();
				contextMenu.add(downloadAction);
				
				contextMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
		

		private boolean isPending(Object[] selection) {
			for (Object value : selection) {
				SubtitlePackage subtitle = (SubtitlePackage) value;
				
				if (!subtitle.getDownload().isStarted()) {
					// pending download found
					return true;
				}
			}
			
			return false;
		}
	};
	
	private final MouseListener fileListMouseHandler = new MouseAdapter() {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			// open on double click
			if (SwingUtilities.isLeftMouseButton(e) && (e.getClickCount() == 2)) {
				JList list = (JList) e.getSource();
				
				// open selection
				open(list.getSelectedValues());
			}
		}
		

		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}
		

		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}
		

		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				JList list = (JList) e.getSource();
				
				int index = list.locationToIndex(e.getPoint());
				
				if (index >= 0 && !list.isSelectedIndex(index)) {
					// auto-select clicked element
					list.setSelectedIndex(index);
				}
				
				final Object[] selection = list.getSelectedValues();
				
				JPopupMenu contextMenu = new JPopupMenu();
				
				// Open
				contextMenu.add(new AbstractAction("Open") {
					
					@Override
					public void actionPerformed(ActionEvent evt) {
						open(selection);
					}
				});
				
				// Save as ...
				contextMenu.add(new AbstractAction("Save as ...") {
					
					@Override
					public void actionPerformed(ActionEvent evt) {
						//						save()
					}
				});
				
				contextMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
		

		private void open(Object[] selection) {
			for (Object object : selection) {
				try {
					open((MemoryFile) object);
				} catch (IOException e) {
					Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage());
				}
			}
		}
		

		private void open(MemoryFile file) throws IOException {
			Deque<SubtitleFormat> priorityList = new ArrayDeque<SubtitleFormat>(4);
			
			// gather all formats, put likely formats first
			for (SubtitleFormat format : SubtitleFormat.values()) {
				if (format.filter().accept(file.getName())) {
					priorityList.addFirst(format);
				} else {
					priorityList.addLast(format);
				}
			}
			
			// decode subtitle file with the first reader that seems to work
			for (SubtitleFormat format : priorityList) {
				InputStream data = new ByteBufferInputStream(file.getData());
				SubtitleReader reader = format.newReader(new InputStreamReader(data, "UTF-8"));
				
				try {
					if (reader.hasNext()) {
						// correct format
						List<SubtitleElement> list = new ArrayList<SubtitleElement>(500);
						
						// read subtitle file
						while (reader.hasNext()) {
							list.add(reader.next());
						}
						
						SubtitleViewer viewer = new SubtitleViewer(file.getName());
						viewer.getTitleLabel().setText("Subtitle Viewer");
						viewer.getInfoLabel().setText(file.getPath());
						
						viewer.setData(list);
						viewer.setVisible(true);
						
						// done
						return;
					}
				} finally {
					reader.close();
				}
			}
			
			throw new IOException("Cannot read subtitle format");
		}
		
	};
	
}
