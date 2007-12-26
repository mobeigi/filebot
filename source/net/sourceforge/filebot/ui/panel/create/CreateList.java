
package net.sourceforge.filebot.ui.panel.create;


import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.transfer.SaveAction;


public class CreateList extends FileBotList {
	
	private SaveAction saveAction = new SaveAction(this);
	
	
	public CreateList() {
		super(false, true, true);
		
		setTitle("List");
		
		Box buttons = Box.createHorizontalBox();
		buttons.setBorder(new EmptyBorder(5, 5, 5, 5));
		buttons.add(Box.createGlue());
		buttons.add(new JButton(saveAction));
		buttons.add(Box.createGlue());
		
		add(buttons, BorderLayout.SOUTH);
	}
}
