/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2026, Arnaud Roques
 *
 * Project Info:  https://plantuml.com
 *
 * If you like this project or if you find it useful, you can support us at:
 *
 * https://plantuml.com/patreon (only 1$ per month!)
 * https://plantuml.com/paypal
 *
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 *
 * Original Author:  kolulu23
 *
 *
 */
package net.sourceforge.plantuml.packetdiag;

import net.sourceforge.plantuml.klimt.Fashion;
import net.sourceforge.plantuml.klimt.LineBreakStrategy;
import net.sourceforge.plantuml.klimt.Shadowable;
import net.sourceforge.plantuml.klimt.UClip;
import net.sourceforge.plantuml.klimt.UStroke;
import net.sourceforge.plantuml.klimt.UTranslate;
import net.sourceforge.plantuml.klimt.color.HColors;
import net.sourceforge.plantuml.klimt.creole.CreoleMode;
import net.sourceforge.plantuml.klimt.creole.Display;
import net.sourceforge.plantuml.klimt.drawing.UGraphic;
import net.sourceforge.plantuml.klimt.drawing.UGraphicStencil;
import net.sourceforge.plantuml.klimt.font.StringBounder;
import net.sourceforge.plantuml.klimt.geom.HorizontalAlignment;
import net.sourceforge.plantuml.klimt.geom.XDimension2D;
import net.sourceforge.plantuml.klimt.shape.AbstractTextBlock;
import net.sourceforge.plantuml.klimt.shape.TextBlock;
import net.sourceforge.plantuml.klimt.shape.TextBlockUtils;
import net.sourceforge.plantuml.klimt.shape.ULine;
import net.sourceforge.plantuml.klimt.shape.URectangle;
import net.sourceforge.plantuml.style.ISkinParam;
import net.sourceforge.plantuml.style.SName;
import net.sourceforge.plantuml.style.Style;
import net.sourceforge.plantuml.style.StyleSignatureBasic;

public class PacketBlock {

	private static final UStroke openerStroke = new UStroke(5, 5, 1);

	/**
	 * Desired block width unit, this is not the final drawing dimension as margins, paddings and scaling are not added
	 */
	private final int width;

	/**
	 * Desired block height unit
	 */
	private int height;

	/**
	 * Full label text
	 */
	private final String label;

	private final ISkinParam skinParam;

	private boolean leftOpen = false;

	private boolean rightOpen = false;

	public PacketBlock(int width, int height, String label, ISkinParam skinParam) {
		this.width = width;
		this.height = height;
		this.label = label;
		this.skinParam = skinParam;
	}

	public PacketBlock(int width, int height, String label, ISkinParam skinParam, boolean leftOpen, boolean rightOpen) {
		this.width = width;
		this.height = height;
		this.label = label;
		this.skinParam = skinParam;
		this.leftOpen = leftOpen;
		this.rightOpen = rightOpen;
	}

	public void openLeft(boolean leftOpen) {
		this.leftOpen = leftOpen;
	}

	public void openRight(boolean rightOpen) {
		this.rightOpen = rightOpen;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public double getDrawWidth(double scale) {
		return width * scale;
	}

	public double getDrawHeight(double scale) {
		return height * scale;
	}

	public String getLabel() {
		return label;
	}

	Style getStyle() {
		return StyleSignatureBasic.of(SName.root, SName.element, SName.packetdiagDiagram, SName.rectangle)
						.getMergedStyle(skinParam.getCurrentStyleBuilder());
	}

	Fashion getFashion() {
		return getStyle().getSymbolContext(skinParam.getIHtmlColorSet());
	}

	private TextBlock getLabelTextBlock(String label, LineBreakStrategy lineBreakStrategy) {
		return Display.getWithNewlines(skinParam.getPragma(), label).create8(
						getStyle().getFontConfiguration(skinParam.getIHtmlColorSet()),
						HorizontalAlignment.CENTER,
						skinParam,
						CreoleMode.SIMPLE_LINE,
						lineBreakStrategy
		);
	}

	private TextBlock clipLabel(final TextBlock labelBlock, final double maxWidth, final double maxHeight) {
		return new AbstractTextBlock() {
			@Override
			public XDimension2D calculateDimension(StringBounder stringBounder) {
				XDimension2D orig = labelBlock.calculateDimension(stringBounder);
				double width = Math.min(orig.getWidth(), maxWidth);
				double height = Math.min(orig.getHeight(), maxHeight);
				return new XDimension2D(width, height);
			}

			@Override
			public void drawU(UGraphic ug) {
				XDimension2D origin = labelBlock.calculateDimension(ug.getStringBounder());
				double clipWidth = Math.min(origin.getWidth(), maxWidth);
				double clipHeight = Math.min(origin.getHeight(), maxHeight);
				ug = ug.apply(new UClip(0, 0, clipWidth, clipHeight));
				labelBlock.drawU(ug);
			}
		};
	}

	private TextBlock getLabelTextBlockClipped(StringBounder stringBounder, double reqWidth, double clipWidth, double clipHeight) {
		final LineBreakStrategy wrapStrategy = new LineBreakStrategy(Integer.toString((int) reqWidth));
		TextBlock labelTB = getLabelTextBlock(label, wrapStrategy);
		return clipLabel(labelTB, clipWidth, clipHeight);
	}

	TextBlock getShapeTextBlock(StringBounder stringBounder, double bitWidth, double bitHeight) {
		final double reqWidth = getDrawWidth(bitWidth);
		final double reqHeight = getDrawHeight(bitHeight);
		final Fashion fashion = getFashion();
		final TextBlock labelTB = getLabelTextBlockClipped(stringBounder, reqWidth, reqWidth, reqHeight);
		final XDimension2D labelDim = labelTB.calculateDimension(stringBounder);
		final double labelMarginTop = (reqHeight - labelDim.getHeight()) / 2;
		// mergeTB would add label height and margins to the shape so here this offset makes sure we actually get drawHeight
		final TextBlock stereo = TextBlockUtils.empty(reqWidth, reqHeight - labelDim.getHeight());

		// Basically it's URectangle#asSmall, but without horizontal margin
		return new AbstractTextBlock() {
			@Override
			public XDimension2D calculateDimension(StringBounder stringBounder) {
				final XDimension2D dimLabel = labelTB.calculateDimension(stringBounder);
				final XDimension2D dimStereo = stereo.calculateDimension(stringBounder);
				XDimension2D dim = dimStereo.mergeTB(dimLabel);
				return new XDimension2D(dim.getWidth(), dim.getHeight());
			}

			@Override
			public void drawU(UGraphic ug) {
				final XDimension2D dim = calculateDimension(ug.getStringBounder());
				ug = UGraphicStencil.create(ug, dim);
				// Draw filled rectangle with no border
				final Fashion fillFashion = fashion.withForeColor(HColors.none());
				UGraphic ugFill = fillFashion.apply(ug);
				final URectangle rect = URectangle.build(dim.getWidth(), dim.getHeight());
				final Shadowable shape = rect.rounded(fashion.getRoundCorner());
				shape.setDeltaShadow(fashion.getDeltaShadow());
				ugFill.draw(shape);

				// Draw borders with appropriate strokes
				final double width = dim.getWidth();
				final double height = dim.getHeight();
				final UStroke solidStroke = fashion.getStroke();
				final UStroke dashedStroke = openerStroke;
				// Apply foreground color for lines
				final UGraphic ugLines = fashion.applyColors(ug);

				// Top side (always solid)
				ugLines.apply(solidStroke).draw(ULine.hline(width));
				// Bottom side (always solid)
				ugLines.apply(solidStroke).apply(UTranslate.dy(height)).draw(ULine.hline(width));
				// Left side (dashed if leftOpen)
				ugLines.apply(leftOpen ? dashedStroke : solidStroke).draw(ULine.vline(height));
				// Right side (dashed if rightOpen)
				ugLines.apply(rightOpen ? dashedStroke : solidStroke).apply(UTranslate.dx(width)).draw(ULine.vline(height));

				final TextBlock tb = TextBlockUtils.mergeTB(labelTB, stereo, HorizontalAlignment.CENTER);
				tb.drawU(ug.apply(new UTranslate(0D, labelMarginTop)));
			}
		};
	}
}
