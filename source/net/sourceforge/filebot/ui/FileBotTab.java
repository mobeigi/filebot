
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import net.sourceforge.tuned.ui.LoadingOverlayPane;


public class FileBotTab<T extends JComponent> extends JComponent {
	
	private final FileBotTabComponent tabComponent = new FileBotTabComponent();
	
	private final T component;
	
	private final LoadingOverlayPane loadingOverlayPane;
	
	
	public FileBotTab(T component) {
		
		setLayout(new BorderLayout());
		this.component = component;
		tabComponent.getCloseButton().addActionListener(closeAction);
		
		loadingOverlayPane = new LoadingOverlayPane(component, this);
		add(loadingOverlayPane, BorderLayout.CENTER);
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
		tabComponent.setIcon(icon);
	}
	

	public Icon getIcon() {
		return tabComponent.getIcon();
	}
	

	public void setLoading(boolean loading) {
		tabComponent.setLoading(loading);
		loadingOverlayPane.setOverlayVisible(loading);
	}
	
	private final ActionListener closeAction = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			close();
		}
		
	};
	
}
