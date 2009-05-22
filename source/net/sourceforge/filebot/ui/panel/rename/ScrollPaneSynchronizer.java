
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoundedRangeModel;
import javax.swing.Timer;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;


class ScrollPaneSynchronizer {
	
	private final RenameList[] components;
	
	
	public ScrollPaneSynchronizer(RenameList... components) {
		this.components = components;
		
		// share vertical and horizontal scrollbar model
		BoundedRangeModel horizontalScrollBarModel = components[0].getListScrollPane().getHorizontalScrollBar().getModel();
		BoundedRangeModel verticalScrollBarModel = components[0].getListScrollPane().getVerticalScrollBar().getModel();
		
		// recalculate common size on change
		ListEventListener<Object> resizeListener = new ListEventListener<Object>() {
			
			private final Timer timer = new Timer(50, new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					syncSize();
					
					// run only once
					timer.stop();
				}
			});
			
			
			@Override
			public void listChanged(ListEvent<Object> evt) {
				// sync size when there are no more events coming in
				timer.restart();
			}
		};
		
		// apply to all components
		for (RenameList<?> component : components) {
			component.getListScrollPane().getHorizontalScrollBar().setModel(horizontalScrollBarModel);
			component.getListScrollPane().getVerticalScrollBar().setModel(verticalScrollBarModel);
			
			component.getModel().addListEventListener(resizeListener);
		}
	}
	

	public void syncSize() {
		Dimension max = new Dimension();
		
		for (RenameList component : components) {
			// reset preferred size
			component.getListComponent().setPreferredSize(null);
			
			// calculate preferred size based on data and renderer
			Dimension preferred = component.getListComponent().getPreferredSize();
			
			// update maximum size
			if (preferred.width > max.width)
				max.width = preferred.width;
			if (preferred.height > max.height)
				max.height = preferred.height;
		}
		
		for (RenameList component : components) {
			// set fixed preferred size
			component.getListComponent().setPreferredSize(max);
			
			// update scrollbars
			component.getListScrollPane().revalidate();
		}
	}
	
}
