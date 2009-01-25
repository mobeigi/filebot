
package net.sourceforge.filebot.ui.panel.subtitle;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.FilteredImageSource;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;

import net.sourceforge.tuned.ui.ColorTintImageFilter;
import net.sourceforge.tuned.ui.IconViewCellRenderer;
import net.sourceforge.tuned.ui.TunedUtilities;


public class SubtitleCellRenderer extends IconViewCellRenderer {
	
	//TODO rename info to e.g. language
	private final JLabel info1 = new JLabel();
	private final JLabel info2 = new JLabel();
	
	private Color infoForegroundDeselected = new Color(0x808080);
	
	// TODO gscheid machn
	private Icon icon;
	
	
	public SubtitleCellRenderer() {
		info1.setBorder(null);
		info2.setBorder(null);
		
		info1.setHorizontalTextPosition(SwingConstants.LEFT);
		
		getContentPane().add(info1);
	}
	

	@Override
	public void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.configureListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		
		SubtitlePackage subtitle = (SubtitlePackage) value;
		
		setText(subtitle.getName());
		
		info1.setText(subtitle.getLanguage().getName());
		
		icon = subtitle.getLanguage().getIcon();
		
		info1.setIcon(icon);
		
		Icon icon = subtitle.getArchiveIcon();
		
		if (isSelected) {
			setIcon(new ImageIcon(createImage(new FilteredImageSource(TunedUtilities.getImage(icon).getSource(), new ColorTintImageFilter(list.getSelectionBackground(), 0.5f)))));
			
			info1.setForeground(list.getSelectionForeground());
			info2.setForeground(list.getSelectionForeground());
		} else {
			setIcon(icon);
			
			info1.setForeground(infoForegroundDeselected);
			info2.setForeground(infoForegroundDeselected);
		}
	}
	

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		//TODO gscheid machn	
		g.translate(36, 43);
		//		icon.paintIcon(this, g, 0, 0);
		
	}
	
}
