/*
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.idrsolutions.com
 * Help section for developers at http://www.idrsolutions.com/java-pdf-library-support/
 *
 * (C) Copyright 1997-2013, IDRsolutions and Contributors.
 *
 * 	This file is part of JPedal
 *
     This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


 *
 * ---------------
 * TextBlock.java
 * ---------------
 */

package org.jpedal.render.output;

import java.awt.Rectangle;

import org.jpedal.parser.PdfStreamDecoder;

public class TextBlock {
	private float rotatedXCoord, rawXCoord, rotatedYCoord, rawYCoord, yAdjust;
	private float lastXCoord;
	private float stringWidth;

	// Maximum screen limit
	private Rectangle cropBox;

	// Font metrics
	private int fontSize;
	private String font = "", realFont = "";
	private String weight = "normal";
	private String style = "normal";
	private float spaceWidth;

	private int fontAdjust = 0;

	private String text;
	private int textColRGB;

	// matrix for this block
	private float[][] matrix;

	private float[][] Trm;
	private float lastYUsed;

	private float lastX = 0; // used in odd rotated pages like siggietest.pdf to pick up breaks

	/** max number of spaces we can add into a text string */
	private int maxSpacesAllowed = 3;

	boolean convertSpacesTonbsp;

	int altFontSize;
	int fontCondition = -1;

	int pageRotation;

	public TextBlock() {

		this.text = "";
		this.textColRGB = -1;
		this.stringWidth = 0;
	}

	public int getAltFontSize() {
		return this.altFontSize;
	}

	public TextBlock(String glyf, int fontSize, FontMapper fontMapper, float[][] matrix, float x, float y, float charWidth, int color,
			float spaceWidth, Rectangle cropBox, float[][] trm, int altFontSize, int fontCondition, float rotX, float rotY, int pageRotation) {
		this.text = glyf;
		this.matrix = matrix;

		this.lastXCoord = rotX + charWidth;

		this.rawXCoord = x;
		this.rotatedXCoord = rotX;

		this.rawYCoord = y;
		this.rotatedYCoord = rotY;

		this.stringWidth = charWidth;

		this.fontSize = fontSize;
		this.font = fontMapper.getFont(false);
		this.realFont = fontMapper.getFont(true);
		this.weight = fontMapper.getWeight();
		this.style = fontMapper.getStyle();
		this.fontAdjust = fontMapper.getFontSizeAdjustment();
		this.textColRGB = color;
		this.spaceWidth = spaceWidth;

		this.cropBox = cropBox;

		this.Trm = trm;

		this.altFontSize = altFontSize;
		this.fontCondition = fontCondition;

		this.pageRotation = pageRotation;

		// track value so we can see when line changes in corner case siggietest.pdf
		this.lastX = trm[2][0];

		// If the text is beyond the page boundaries empty this block is empty.
		if (!cropBox.contains(x + charWidth, y)) {
			this.text = "";
		}
	}

	public boolean isEmpty() {
		return this.text.length() == 0;
	}

	public float getX() {
		return this.rawXCoord;
	}

	public float getY() {
		return this.rawYCoord + this.yAdjust;
	}

	public void adjustY(float y) {
		this.yAdjust = y;
	}

	public float getWidth() {
		return this.stringWidth;
	}

	public int getFontSize() {
		if (this.matrix != null && this.matrix[0][0] * this.matrix[0][1] != 0) {

			float x1 = this.matrix[0][0];
			if (x1 < 0) x1 = -x1;

			float x2 = this.matrix[0][1];
			if (x2 < 0) x2 = -x2;

			float sum1 = x1 + this.fontSize * x2;
			float sum2 = this.fontSize * x1 + x2;

			float result = sum1 > sum2 ? sum1 : sum2;

			return (int) Math.ceil((Math.abs(result) / this.fontSize));
		}
		return this.fontSize;
	}

	public String getFont() {
		return this.realFont;
	}

	public String getWeight() {
		return this.weight;
	}

	public int getColor() {
		return this.textColRGB;
	}

	/**
	 * Compare a trm with the current trm ignoring the translation.
	 */
	private boolean compareTrm(float[][] newTrm) {
		if (this.matrix == null) {
			return false;
		}

		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 2; x++) {
				if (newTrm[x][y] != this.matrix[x][y]) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * @return The angle this text is rotated
	 */
	public float getRotationAngle() {
		float sum = Math.abs(this.matrix[0][0]) + Math.abs(this.matrix[0][1]);
		float angle = (float) Math.acos(this.matrix[0][0] / sum);

		if (angle != 0 && this.matrix[1][0] < 0 && this.matrix[0][1] > 0) angle = -angle;

		return angle;
	}

	/**
	 * make sure it is between 0 and 360
	 * 
	 */
	public int getRotationAngleInDegrees() {
		int angle = (int) (getRotationAngle() * 180 / Math.PI);

		while (angle < 0)
			angle = angle + 360;

		return angle;
	}

	/**
	 * @param s
	 *            - add string to the end of text
	 */
	private void concat(String s) {
		this.text += s;
	}

	public String getOutputString(boolean isOutput) {
		String result = this.text;

		if (isOutput && OutputDisplay.Helper != null) {
			result = OutputDisplay.Helper.tidyText(result);

			if (this.convertSpacesTonbsp) result = result.replaceAll(" ", "&nbsp;");
		}

		return result;
	}

	public boolean isSameFont(int otherfontSize, FontMapper otherMapper, float[][] newTrm, int color) {

		// allow for close match on text on large text
		float ratio;
		if (this.fontSize < 18 || otherfontSize < 18) ratio = 0;
		else
			if (this.fontSize < otherfontSize) ratio = (float) this.fontSize / (float) otherfontSize;
			else ratio = (float) otherfontSize / (float) this.fontSize;

		return ((compareTrm(newTrm) && this.fontSize == otherfontSize) || ratio > 0.8f) && compareFontMapper(otherMapper) && this.textColRGB == color;
	}

	private boolean compareFontMapper(FontMapper mapper) {
		return mapper.getFont(false).equals(this.font) && mapper.getWeight().equals(this.weight);
	}

	/**
	 * Append a glyf to the current string.
	 * 
	 * @param glyf
	 * @param charWidth
	 * @param x
	 * @param y
	 * @return false if glyf can be appended
	 */
	public boolean appendText(String glyf, float charWidth, float x, float y, boolean groupTJGlyphs, boolean checkGaps, float realX, float realY) {

		// Discard if glyf is to be drawn beyond screen boundary
		if (!this.cropBox.contains(realX, realY)) {
			return false;
		}

		float lineYChange = this.rotatedYCoord - y;

		// ignore tiny changes
		if (Math.abs(lineYChange) < 0.01f) {
			lineYChange = 0f;
			this.rotatedYCoord = y;
		}

		float lineXChange = x - this.lastXCoord;

		/**
		 * Text with 180 rotation gets printed out in reverse so has to be accounted for by subtracting the charWidth from the lineXChange
		 */
		if (this.getRotationAngleInDegrees() == 180) {
			lineXChange = lineXChange - charWidth;
		}

		// ignoreable space (ie "Certifi cat" of Lohnausweis.pdf)
		if (!groupTJGlyphs && glyf.equals(" ") && lineYChange == 0 && lineXChange < (-charWidth)) {
			return true;
		}

		if (!groupTJGlyphs && glyf.equals(" ")) {
			return false;
		}

		// Account for occasions where there is a long line of repeating ..........
		// if(text.endsWith(".") && glyf.equals(".")) {
		// return false;
		// }

		// Allow for slight variation
		if (lineXChange < 0 && lineXChange > -charWidth) {
			lineXChange = 0;
		}

		// ignore test in case of rotated text (see siggietest.pdf) by looking for change on X
		if (this.Trm[0][0] == 0 && this.Trm[1][1] == 0 && this.Trm[0][1] > 0 && this.Trm[1][0] < 0) {

			{ // usual rotated case
				// avoid big gap as probably cursor moving

				float estimatedCharDiff = (this.lastYUsed - y) / this.fontSize;
				if (estimatedCharDiff > 1.5) {
					return false;
				}

				else
					if (this.Trm[0][0] == 0 && this.Trm[1][1] == 0 && (this.lastX == x)) {// Zombies.pdf fix
					// return false;
					}
					else
						if ((lineYChange != 0 || lineXChange < -1.5f)) {// costena space fix
							return false;
						}
			}

		}
		else {

			// note duplication above if we ever change
			if ((lineYChange != 0 || lineXChange < -1.5f)) {
				return false;
			}
		}

		String spaces = "";

		int spaceCount = (int) (lineXChange / this.spaceWidth);
		float percentageOfSpaceSize = lineXChange % this.spaceWidth;
		percentageOfSpaceSize = percentageOfSpaceSize / this.spaceWidth;

		// Close enough to be a space
		if (percentageOfSpaceSize > PdfStreamDecoder.currentThreshold) {
			spaceCount++;
		}

		if (checkGaps || spaceCount > this.maxSpacesAllowed) { // allow us to keep text blocks in TJ together but avoid big spaces as hard to space
																// correctly
			if (spaceCount > 2 || (spaceCount > 0 && this.matrix[0][0] == 0 && this.matrix[1][1] == 0)) // avoid this case // cropping/4.pdf
			return false;
		}

		while (spaceCount-- > 0) {
			spaces += " ";
		}

		// stick the glyf on the end
		spaces += glyf;

		concat(spaces);

		/**
		 * Store current width of the text string
		 */

		if (this.Trm[0][0] == 0 && this.Trm[1][1] == 0 && this.Trm[0][1] > 0 && this.Trm[1][0] < 0 && this.pageRotation == 0) {

			this.stringWidth = this.rotatedYCoord - y + charWidth;
		}
		else {
			this.stringWidth = x - this.rotatedXCoord + charWidth; // for code to work x and xcoords have to be different values.
			this.lastXCoord = x + charWidth;
		}

		this.lastYUsed = y;

		return true;
	}

	public boolean hasSameFont(TextBlock otherBlock) {
		return this.font.equals(otherBlock.font) && this.weight.equals(otherBlock.weight) && getFontSize() == otherBlock.getFontSize();
	}

	public String getStyle() {
		return this.style;
	}

	@Override
	public String toString() {
		String result;
		result = "text[" + this.text + "]\tfontSize[" + this.fontSize + "]\tspaceWidth[" + this.spaceWidth + "]\tcoord[" + this.rawXCoord + ", "
				+ this.rawYCoord + ']';
		return result;
	}

	/**
	 * For over large fonts that can't be mapped to a suitable equivalent.
	 * 
	 */
	public int getFontAdjustment() {
		return this.fontAdjust;
	}

	/*
	 * @return true if glfy is on ignore list
	 */
	public static boolean ignoreGlyf(String glfy) {
		return glfy.codePointAt(0) == 65533;
	}

	public void convertSpacesTonbsp(boolean convertSpacesTonbsp) {
		this.convertSpacesTonbsp = convertSpacesTonbsp;
	}

	public int getFontCondition() {
		return this.fontCondition;
	}
}
