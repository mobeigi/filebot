package net.sourceforge.filebot.ui.rename;

import static net.sourceforge.filebot.similarity.EpisodeMetrics.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.similarity.MetricCascade;
import net.sourceforge.filebot.similarity.MetricMin;
import net.sourceforge.filebot.similarity.SimilarityMetric;
import net.sourceforge.filebot.ui.rename.RenameModel.FormattedFuture;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.tuned.FileUtilities;
import net.sourceforge.tuned.ui.DefaultFancyListCellRenderer;
import net.sourceforge.tuned.ui.GradientStyle;

class RenameListCellRenderer extends DefaultFancyListCellRenderer {

	private RenameModel renameModel;

	private TypeRenderer typeRenderer = new TypeRenderer();

	private Color noMatchGradientBeginColor = new Color(0xB7B7B7);
	private Color noMatchGradientEndColor = new Color(0x9A9A9A);

	private Color warningGradientBeginColor = Color.RED;
	private Color warningGradientEndColor = new Color(0xDC143C);

	private Color pathRainbowBeginColor = new Color(0xCC3300);
	private Color pathRainbowEndColor = new Color(0x008080);

	public RenameListCellRenderer(RenameModel renameModel) {
		super(new Insets(4, 7, 4, 7));

		this.renameModel = renameModel;
		setHighlightingEnabled(false);

		setLayout(new MigLayout("insets 0, fill", "align left", "align center"));
		this.add(typeRenderer, "gap rel:push, hidemode 3");
	}

	@Override
	public void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		super.configureListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		// reset decoration / highlighting
		setOpaque(false);
		setIcon(null);
		typeRenderer.setVisible(false);
		typeRenderer.setAlpha(1.0f);

		// render unmatched values differently
		if (!renameModel.hasComplement(index)) {
			if (isSelected) {
				setGradientColors(noMatchGradientBeginColor, noMatchGradientEndColor);
			} else {
				setForeground(noMatchGradientBeginColor);
				typeRenderer.setAlpha(0.5f);
			}
		}

		if (renameModel.preserveExtension() && index < renameModel.size() && renameModel.getMatch(index).getCandidate() != null) {
			typeRenderer.setText(getType(renameModel.getMatch(index).getCandidate()));
			typeRenderer.setVisible(true);
		}

		if (value instanceof File) {
			// display file extension
			File file = (File) value;

			if (renameModel.preserveExtension()) {
				setText(FileUtilities.getName(file));
			} else {
				setText(isSelected || !renameModel.hasComplement(index) ? formatPath(file) : colorizePath(file.getAbsoluteFile(), true));
			}
		} else if (value instanceof FormattedFuture) {
			// display progress icon
			FormattedFuture formattedFuture = (FormattedFuture) value;
			float matchProbablity = renameModel.hasComplement(index) ? getMatchProbablity(formattedFuture.getMatch()) : 1;

			if (formattedFuture.isDone() && !formattedFuture.isCancelled()) {
				if (!renameModel.preserveExtension() && renameModel.hasComplement(index)) {
					// absolute path mode
					File file = renameModel.getMatch(index).getCandidate();
					File path = resolveAbsolutePath(file.getParentFile(), formattedFuture.toString(), null);
					setText(isSelected || matchProbablity < 1 ? formatPath(path) : colorizePath(path, true));

					String ext = getExtension(path);
					typeRenderer.setText(ext != null ? ext.toLowerCase() : "MISSING EXTENSION");
					if (file.isDirectory()) {
						typeRenderer.setText("Folder");
					}
					typeRenderer.setVisible(true);
				} else {
					// relative name mode
					File path = new File(formattedFuture.toString());
					setText(isSelected || matchProbablity < 1 || !renameModel.hasComplement(index) ? formatPath(path) : colorizePath(path, !renameModel.preserveExtension()));
				}
			} else {
				setText(formattedFuture.preview()); // default text
			}

			switch (formattedFuture.getState()) {
			case PENDING:
				setIcon(ResourceManager.getIcon("worker.pending"));
				break;
			case STARTED:
				setIcon(ResourceManager.getIcon("worker.started"));
				break;
			}

			if (renameModel.hasComplement(index)) {
				setOpaque(true); // enable paint background
				setBackground(derive(warningGradientBeginColor, (1 - matchProbablity) * 0.5f)); // alpha indicates match probability

				if (matchProbablity < 1) {
					if (isSelected) {
						setGradientColors(warningGradientBeginColor, warningGradientEndColor);
						setIcon(ResourceManager.getIcon("status.warning"));

						if (formattedFuture.isComplexFormat()) {
							typeRenderer.setVisible(true);
							typeRenderer.setAlpha(1.0f);
							typeRenderer.setText(formattedFuture.getMatch().getValue().toString());
						}
					}
				}

				// check if files already exist
				FormattedFuture pathFuture = (FormattedFuture) value;
				if (pathFuture.isDone() && !pathFuture.isCancelled()) {
					File from = renameModel.getMatch(index).getCandidate();
					File to = resolveAbsolutePath(from.getParentFile(), pathFuture.toString(), renameModel.preserveExtension() ? getExtension(from) : null);
					if (from.getAbsolutePath().equals(to.getAbsolutePath())) {
						setIcon(ResourceManager.getIcon("dialog.continue"));
					} else if (to.exists() && !to.equals(from)) {
						setIcon(ResourceManager.getIcon("dialog.cancel")); // take into account that on Windows equals/exists is case-insensitive which we have to work around
					}
				}
			}
		}
	}

	protected String formatPath(File file) {
		return normalizePathSeparators(file.getPath());
	}

	protected String colorizePath(File file, boolean hasExtension) {
		List<File> path = listPath(file);
		StringBuilder html = new StringBuilder(512);
		html.append("<html><nobr>");

		// colorize parent path
		for (int i = 0; i < path.size() - 1; i++) {
			float f = (path.size() <= 2) ? 1 : (float) i / (path.size() - 2);
			Color c = interpolateHSB(pathRainbowBeginColor, pathRainbowEndColor, f);
			html.append(String.format("<span style='color:rgb(%1$d, %2$d, %3$d)'>%4$s</span><span style='color:rgb(%1$d, %2$d, %3$d)'>/</span>", c.getRed(), c.getGreen(), c.getBlue(), escapeHTML(FileUtilities.getName(path.get(i)))));
		}

		// only colorize extension
		if (hasExtension) {
			html.append(escapeHTML(FileUtilities.getName(file)));
			String extension = FileUtilities.getExtension(file);
			if (extension != null) {
				html.append(String.format("<span style='color:#607080'>.%s</span>", escapeHTML(extension))); // highlight extension
			}
		} else {
			html.append(file.getName());
		}

		return html.append("</nobr></html>").toString();
	}

	protected File resolveAbsolutePath(File targetDir, String path, String extension) {
		File f = new File(extension == null || extension.isEmpty() ? path : String.format("%s.%s", path, extension));
		if (!f.isAbsolute()) {
			f = new File(targetDir, f.getPath()); // resolve path against target folder
		}

		return f.getAbsoluteFile();
	}

	protected float getMatchProbablity(Match<Object, File> match) {
		if (match.getValue() instanceof Episode) {
			float f = verificationMetric().getSimilarity(match.getValue(), match.getCandidate());
			return (f + 1) / 2; // normalize -1..1 to 0..1
		}

		SimilarityMetric fsm = new MetricCascade(new MetricMin(FileSize, 0), FileName, EpisodeIdentifier);
		float f = fsm.getSimilarity(match.getValue(), match.getCandidate());
		if (f != 0) {
			return (Math.max(f, 0)); // normalize -1..1 and boost by 0.25 (because file <-> file matches are not necessarily about Episodes)
		}

		return 1; // assume match is OK
	}

	protected String getType(File file) {
		if (file.isDirectory()) {
			return "Folder";
		}

		String extension = FileUtilities.getExtension(file);
		if (extension != null) {
			return extension.toLowerCase();
		}

		// some file with no extension
		return "File";
	}

	private static class TypeRenderer extends DefaultListCellRenderer {

		private final Insets margin = new Insets(0, 10, 0, 0);
		private final Insets padding = new Insets(0, 6, 0, 5);
		private final int arc = 10;

		private Color gradientBeginColor = new Color(0xFFCC00);
		private Color gradientEndColor = new Color(0xFF9900);

		private float alpha = 1.0f;

		public TypeRenderer() {
			setOpaque(false);
			setForeground(new Color(0x141414));

			setBorder(new CompoundBorder(new EmptyBorder(margin), new EmptyBorder(padding)));
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g;

			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			RoundRectangle2D shape = new RoundRectangle2D.Float(margin.left, margin.top, getWidth() - (margin.left + margin.right), getHeight(), arc, arc);

			g2d.setComposite(AlphaComposite.SrcOver.derive(alpha));

			g2d.setPaint(GradientStyle.TOP_TO_BOTTOM.getGradientPaint(shape, gradientBeginColor, gradientEndColor));
			g2d.fill(shape);

			g2d.setFont(getFont());
			g2d.setPaint(getForeground());

			Rectangle2D textBounds = g2d.getFontMetrics().getStringBounds(getText(), g2d);
			g2d.drawString(getText(), (float) (shape.getCenterX() - textBounds.getX() - (textBounds.getWidth() / 2f)), (float) (shape.getCenterY() - textBounds.getY() - (textBounds.getHeight() / 2)));
		}

		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
	}

}
