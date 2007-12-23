
package net.sourceforge.filebot.ui.panel.list;


import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.sal.LoadAction;
import net.sourceforge.filebot.ui.sal.SaveAction;


public class FileList extends FileBotList {
	
	private SaveAction saveAction = new SaveAction(this);
	
	private LoadAction loadAction = new LoadAction(this);
	
	
	public FileList() {
		super(true, true, true);
		
		setTransferablePolicy(new FileListTransferablePolicy(this));
		
		setTitle("Folder");
		
		Box buttons = Box.createHorizontalBox();
		buttons.setBorder(new EmptyBorder(5, 5, 5, 5));
		buttons.add(Box.createHorizontalGlue());
		
		buttons.add(new JButton(loadAction));
		buttons.add(Box.createHorizontalStrut(5));
		buttons.add(new JButton(saveAction));
		buttons.add(Box.createHorizontalGlue());
		
		add(buttons, BorderLayout.SOUTH);
	}
	
}
