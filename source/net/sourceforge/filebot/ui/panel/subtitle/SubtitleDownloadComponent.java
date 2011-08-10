
package net.sourceforge.filebot.ui.panel.subtitle;


import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.ui.NotificationLogging.*;
import static net.sourceforge.filebot.ui.panel.subtitle.SubtitleUtilities.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.EventListModel;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.panel.subtitle.SubtitlePackage.Download.Phase;
import net.sourceforge.filebot.ui.transfer.DefaultTransferHandler;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.ui.ListView;
import net.sourceforge.tuned.ui.TunedUtilities;


class SubtitleDownloadComponent extends JComponent {
	
	private EventList<SubtitlePackage> packages = new BasicEventList<SubtitlePackage>();
	
	private EventList<MemoryFile> files = new BasicEventList<MemoryFile>();
	
	private SubtitlePackageCellRenderer renderer = new SubtitlePackageCellRenderer();
	
	private JTextField filterEditor = new JTextField();
	

	public SubtitleDownloadComponent() {
		final JList packageList = new JList(createPackageListModel());
		packageList.setFixedCellHeight(32);
		packageList.setCellRenderer(renderer);
		
		// better selection behaviour
		EventSelectionModel<SubtitlePackage> packageSelection = new EventSelectionModel<SubtitlePackage>(packages);
		packageSelection.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
		packageList.setSelectionModel(packageSelection);
		
		// context menu and fetch on double click
		packageList.addMouseListener(packageListMouseHandler);
		
		// file list view
		final JList fileList = new ListView(createFileListModel()) {
			
			@Override
			protected String convertValueToText(Object value) {
				MemoryFile file = (MemoryFile) value;
				return file.getName();
			}
			

			@Override
			protected Icon convertValueToIcon(Object value) {
				if (SUBTITLE_FILES.accept(value.toString()))
					return ResourceManager.getIcon("file.subtitle");
				
				return ResourceManager.getIcon("file.unknown");
			}
		};
		
		// better selection behaviour
		EventSelectionModel<MemoryFile> fileSelection = new EventSelectionModel<MemoryFile>(files);
		fileSelection.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
		fileList.setSelectionModel(fileSelection);
		
		// install dnd and clipboard export handler
		MemoryFileListExportHandler memoryFileExportHandler = new MemoryFileListExportHandler();
		fileList.setTransferHandler(new DefaultTransferHandler(null, memoryFileExportHandler, memoryFileExportHandler));
		
		fileList.setDragEnabled(true);
		fileList.addMouseListener(fileListMouseHandler);
		
		JButton clearButton = new JButton(clearFilterAction);
		clearButton.setOpaque(false);
		
		setLayout(new MigLayout("nogrid, fill", "[fill]", "[pref!][fill]"));
		
		add(new JLabel("Filter:"), "gap indent:push");
		add(filterEditor, "wmin 120px, gap rel");
		add(clearButton, "w 24px!, h 24px!");
		add(new JScrollPane(packageList), "newline, hmin 80px");
		
		JScrollPane scrollPane = new JScrollPane(fileList);
		scrollPane.setViewportBorder(new LineBorder(fileList.getBackground()));
		add(scrollPane, "newline, hmin max(80px, 30%)");
		
		// install fetch action
		TunedUtilities.installAction(packageList, KeyStroke.getKeyStroke("ENTER"), new AbstractAction("Fetch") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				fetch(packageList.getSelectedValues());
			}
		});
		
		// install open action
		TunedUtilities.installAction(fileList, KeyStroke.getKeyStroke("ENTER"), new AbstractAction("Open") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				open(fileList.getSelectedValues());
			}
		});
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
		// source list
		EventList<MemoryFile> source = getFileModel();
		
		// sort by name
		source = new SortedList<MemoryFile>(source, new Comparator<MemoryFile>() {
			
			@Override
			public int compare(MemoryFile m1, MemoryFile m2) {
				return m1.getName().compareToIgnoreCase(m2.getName());
			}
		});
		
		// as list model
		return new EventListModel<MemoryFile>(source);
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
	

	private void fetch(Object[] selection) {
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
						UILogger.log(Level.WARNING, ExceptionUtilities.getRootCauseMessage(e), e);
						
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
	

	private void open(Object[] selection) {
		try {
			for (Object object : selection) {
				MemoryFile file = (MemoryFile) object;
				
				// only open subtitle files
				if (SUBTITLE_FILES.accept(file.getName())) {
					open(file);
				}
			}
		} catch (Exception e) {
			UILogger.log(Level.WARNING, e.getMessage(), e);
		}
	}
	

	private void open(MemoryFile file) throws IOException {
		SubtitleViewer viewer = new SubtitleViewer(file.getName());
		viewer.getTitleLabel().setText("Subtitle Viewer");
		viewer.getInfoLabel().setText(file.getPath());
		
		viewer.setData(decode(file));
		viewer.setVisible(true);
	}
	

	private void save(Object[] selection) {
		try {
			if (selection.length == 1) {
				// single file
				MemoryFile file = (MemoryFile) selection[0];
				
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setSelectedFile(new File(validateFileName(file.getName())));
				
				if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
					write(file.getData(), fileChooser.getSelectedFile());
				}
			} else {
				// multiple files
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
					File folder = fileChooser.getSelectedFile();
					
					for (Object object : selection) {
						MemoryFile file = (MemoryFile) object;
						write(file.getData(), new File(folder, validateFileName(file.getName())));
					}
				}
			}
		} catch (IOException e) {
			UILogger.log(Level.WARNING, e.getMessage(), e);
		}
	}
	

	private final Action clearFilterAction = new AbstractAction(null, ResourceManager.getIcon("edit.clear")) {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			filterEditor.setText("");
		}
	};
	
	private final MouseListener packageListMouseHandler = new MouseAdapter() {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			// fetch on double click
			if (SwingUtilities.isLeftMouseButton(e) && (e.getClickCount() == 2)) {
				JList list = (JList) e.getSource();
				
				fetch(list.getSelectedValues());
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
				
				if (selection.length > 0) {
					JPopupMenu contextMenu = new JPopupMenu();
					
					JMenuItem item = contextMenu.add(new AbstractAction("Download", ResourceManager.getIcon("package.fetch")) {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							fetch(selection);
						}
					});
					
					// disable menu item if all selected elements have been fetched already
					item.setEnabled(isPending(selection));
					
					contextMenu.show(e.getComponent(), e.getX(), e.getY());
				}
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
				
				if (selection.length > 0) {
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
							save(selection);
						}
					});
					
					contextMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}
		
	};
	
}
