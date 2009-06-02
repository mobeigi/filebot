
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.border.CompoundBorder;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.ui.AbstractFancyListCellRenderer;


public class SubtitleListCellRenderer extends AbstractFancyListCellRenderer {
	
	private final JLabel titleLabel = new JLabel();
	private final JLabel languageLabel = new JLabel();
	
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	

	public SubtitleListCellRenderer() {
		setHighlightingEnabled(false);
		
		progressBar.setStringPainted(true);
		progressBar.setOpaque(false);
		progressBar.setPreferredSize(new Dimension(80, 18));
		
		setLayout(new MigLayout("fill, nogrid, insets 0"));
		
		add(languageLabel, "hidemode 3, w 85px!");
		add(titleLabel, "");
		add(progressBar, "gap indent:push");
		
		setBorder(new CompoundBorder(new DashedSeparator(2, 4, Color.lightGray, Color.white), getBorder()));
	}
	

	@Override
	public void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.configureListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		SubtitlePackage subtitle = (SubtitlePackage) value;
		
		titleLabel.setIcon(ResourceManager.getIcon("status.archive"));
		titleLabel.setText(subtitle.getName());
		
		if (languageLabel.isVisible()) {
			languageLabel.setText(subtitle.getLanguage().getName());
			languageLabel.setIcon(ResourceManager.getFlagIcon(subtitle.getLanguage().getCode()));
		}
		
		//TODO download + progress
		progressBar.setVisible(false);
		progressBar.setString(subtitle.getDownload().getState().toString().toLowerCase());
		
		titleLabel.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
		languageLabel.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
		
		// don't paint border on last element
		setBorderPainted(index < list.getModel().getSize() - 1);
	}
	

	public JLabel getLanguageLabel() {
		return languageLabel;
	}
	

	@Override
	public void validate() {
		// validate children, yet avoid flickering of the mouse cursor
		validateTree();
	}
	
}
