
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.Color;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;
import net.sourceforge.filebot.ui.panel.rename.similarity.MultiSimilarityMetric;
import net.sourceforge.filebot.ui.panel.rename.similarity.SimilarityMetric;
import net.sourceforge.tuned.ui.notification.SeparatorBorder;


class SimilarityPanel extends Box {
	
	private JPanel grid = new JPanel(new GridLayout(0, 2, 25, 1));
	
	private JList nameList;
	
	private JList fileList;
	
	private UpdateMetricsListener updateMetricsListener = new UpdateMetricsListener();
	
	private NumberFormat numberFormat = NumberFormat.getNumberInstance();
	
	private List<MetricUpdater> updaterList = new ArrayList<MetricUpdater>();
	
	private Border labelMarginBorder = BorderFactory.createEmptyBorder(0, 3, 0, 0);
	
	private Border separatorBorder = new SeparatorBorder(1, Color.decode("#ACA899"), SeparatorBorder.Position.TOP);
	
	
	public SimilarityPanel(JList nameList, JList fileList) {
		super(BoxLayout.PAGE_AXIS);
		
		this.nameList = nameList;
		this.fileList = fileList;
		
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
		
		Box subBox = Box.createVerticalBox();
		
		add(subBox);
		add(Box.createVerticalStrut(15));
		
		subBox.add(grid);
		
		subBox.setBorder(BorderFactory.createTitledBorder("Similarity"));
		
		Border pane = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
		Border margin = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		
		grid.setBorder(BorderFactory.createCompoundBorder(pane, margin));
		grid.setBackground(Color.WHITE);
		grid.setOpaque(true);
	}
	

	public void setMetrics(MultiSimilarityMetric metrics) {
		grid.removeAll();
		updaterList.clear();
		
		for (SimilarityMetric metric : metrics) {
			JLabel name = new JLabel(metric.getName());
			name.setToolTipText(metric.getDescription());
			
			JLabel value = new JLabel();
			
			name.setBorder(labelMarginBorder);
			value.setBorder(labelMarginBorder);
			
			MetricUpdater updater = new MetricUpdater(value, metric);
			updaterList.add(updater);
			
			grid.add(name);
			grid.add(value);
		}
		
		JLabel name = new JLabel(metrics.getName());
		
		JLabel value = new JLabel();
		
		MetricUpdater updater = new MetricUpdater(value, metrics);
		updaterList.add(updater);
		
		Border border = BorderFactory.createCompoundBorder(separatorBorder, labelMarginBorder);
		name.setBorder(border);
		value.setBorder(border);
		
		grid.add(name);
		grid.add(value);
	}
	

	public void hook() {
		updateMetrics();
		nameList.addListSelectionListener(updateMetricsListener);
		fileList.addListSelectionListener(updateMetricsListener);
	}
	

	public void unhook() {
		nameList.removeListSelectionListener(updateMetricsListener);
		fileList.removeListSelectionListener(updateMetricsListener);
	}
	
	private ListEntry<?> lastListEntryA = null;
	
	private ListEntry<?> lastListEntryB = null;
	
	
	public void updateMetrics() {
		ListEntry<?> a = (ListEntry<?>) nameList.getSelectedValue();
		ListEntry<?> b = (ListEntry<?>) fileList.getSelectedValue();
		
		if ((a == lastListEntryA) && (b == lastListEntryB))
			return;
		
		lastListEntryA = a;
		lastListEntryB = b;
		
		boolean reset = ((a == null) || (b == null));
		
		for (MetricUpdater updater : updaterList) {
			if (!reset)
				updater.update(a, b);
			else
				updater.reset();
		}
	}
	
	
	private class UpdateMetricsListener implements ListSelectionListener {
		
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting())
				return;
			
			updateMetrics();
		}
	}
	

	private class MetricUpdater {
		
		private JLabel value;
		
		private SimilarityMetric metric;
		
		
		public MetricUpdater(JLabel value, SimilarityMetric metric) {
			this.value = value;
			this.metric = metric;
			
			reset();
		}
		

		public void update(ListEntry<?> a, ListEntry<?> b) {
			value.setText(numberFormat.format(metric.getSimilarity(a, b)));
		}
		

		public void reset() {
			value.setText(numberFormat.format(0));
		}
	}
	
}
