
package net.filebot.ui;

import javax.swing.Icon;
import javax.swing.JComponent;

import net.filebot.ui.analyze.AnalyzePanelBuilder;
import net.filebot.ui.episodelist.EpisodeListPanelBuilder;
import net.filebot.ui.list.ListPanelBuilder;
import net.filebot.ui.rename.RenamePanelBuilder;
import net.filebot.ui.sfv.SfvPanelBuilder;
import net.filebot.ui.subtitle.SubtitlePanelBuilder;

public interface PanelBuilder {

	public String getName();

	public Icon getIcon();

	public JComponent create();

	public static PanelBuilder[] defaultSequence() {
		return new PanelBuilder[] { new RenamePanelBuilder(), new EpisodeListPanelBuilder(), new SubtitlePanelBuilder(), new SfvPanelBuilder(), new AnalyzePanelBuilder(), new ListPanelBuilder() };
	}

}
