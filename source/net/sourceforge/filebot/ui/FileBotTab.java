
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.ui.LoadingOverlayPane;
import net.sourceforge.tuned.ui.ProgressIndicator;


public class FileBotTab<T extends JComponent> extends JPanel {
	
	private final FileBotTabComponent tabComponent = new FileBotTabComponent();
	
	private final T component;
	
	private Icon icon;
	
	private boolean loading = false;
	
	
	public FileBotTab(T component) {
		super(new BorderLayout());
		
		this.component = component;
		tabComponent.getCloseButton().addActionListener(closeAction);
		
		ProgressIndicator progress = new ProgressIndicator(new DefaultBoundedRangeModel(4, 0, 0, 10));
		progress.setPaintBackground(true);
		progress.setPaintText(false);
		progress.setBackground(new Color(255, 255, 255, 70));
		
		LoadingOverlayPane pane = new LoadingOverlayPane(this.component, progress);
		
		add(pane, BorderLayout.CENTER);
	}
	

	public void addTo(JTabbedPane tabbedPane) {
		tabbedPane.addTab(this.getTitle(), this);
		tabbedPane.setTabComponentAt(tabbedPane.indexOfComponent(this), tabComponent);
	}
	

	public void close() {
		if (!isClosed()) {
			getTabbedPane().remove(this);
		}
	}
	

	public boolean isClosed() {
		JTabbedPane tabbedPane = getTabbedPane();
		
		if (tabbedPane == null)
			return true;
		
		return getTabbedPane().indexOfComponent(this) < 0;
	}
	

	private JTabbedPane getTabbedPane() {
		return (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, this);
	}
	

	public T getComponent() {
		return component;
	}
	

	public FileBotTabComponent getTabComponent() {
		return tabComponent;
	}
	

	public void setTitle(String title) {
		tabComponent.setText(title);
	}
	

	public String getTitle() {
		return tabComponent.getText();
	}
	

	public void setIcon(Icon icon) {
		this.icon = icon;
		
		if (!loading) {
			tabComponent.setIcon(icon);
		}
	}
	

	public Icon getIcon() {
		return icon;
	}
	

	public void setLoading(boolean loading) {
		this.loading = loading;
		
		if (loading) {
			tabComponent.setIcon(ResourceManager.getIcon("tab.loading"));
		} else {
			tabComponent.setIcon(icon);
		}
	}
	
	private final ActionListener closeAction = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			close();
		}
		
	};
	
}
