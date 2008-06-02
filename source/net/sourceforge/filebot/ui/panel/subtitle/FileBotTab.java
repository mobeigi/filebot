
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotTabComponent;


public class FileBotTab<T extends JComponent> extends JPanel {
	
	private final FileBotTabComponent tabComponent = new FileBotTabComponent();
	
	private final T component;
	
	private ImageIcon icon;
	
	private boolean loading = false;
	
	
	public FileBotTab(T component) {
		super(new BorderLayout());
		
		this.component = component;
		tabComponent.getCloseButton().addActionListener(closeAction);
		
		add(component, BorderLayout.CENTER);
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
	

	public void setIcon(ImageIcon icon) {
		this.icon = icon;
		
		if (!loading) {
			tabComponent.setIcon(icon);
		}
	}
	

	public ImageIcon getIcon() {
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
