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
 * T1Glyphs.java
 * ---------------
 */
package org.jpedal.fonts.glyph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.fonts.StandardFonts;
import org.jpedal.fonts.Type1;
import org.jpedal.fonts.objects.FontData;
import org.jpedal.gui.ShowGUIMessage;
import org.jpedal.objects.GraphicsState;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.utils.LogWriter;

public class T1Glyphs extends PdfJavaGlyphs {

	private static final long serialVersionUID = 2482425584824639998L;

	private static String nybChars = "0123456789.ee -";

	// flag to show if actually 1c
	public boolean is1C = false;

	private String[] charForGlyphIndex;

	/** holds mappings for drawing the glpyhs */
	protected Map charStrings = new HashMap();
	protected Map glyphNumbers = new HashMap();

	/** holds the numbers */
	int max = 100;

	double[] operandsRead = new double[this.max];

	/** pointer on stack */
	int operandReached = 0;

	float[] pt;

	// co-ords for closing glyphs
	private double xs = -1, ys = -1, x = 0, y = 0;

	/** tracks points read in t1 flex */
	private int ptCount = 0;

	/** op to be used next */
	int currentOp = 0;

	/** used to count up hints */
	private int hintCount = 0;

	/**
	 * I byte ops in CFF DIct table * private static String[] raw1ByteValues = { "version", "Notice", "FullName", "FamilyName", "Weight", "FontBBox",
	 * "BlueValues", "OtherBlues", "FamilyBlues", "FamilyOtherBlues", "StdHW", "StdVW", "escape", "UniqueID", "XUID", "charset", "Encoding",
	 * "CharStrings", "Private", "Subrs", "defaultWidthX", "nominalWidthX", "-Reserved-", "-Reserved-", "-Reserved-", "-Reserved-", "-Reserved-",
	 * "-Reserved-", "intint", "longint", "BCD", "-Reserved-" };/
	 **/

	/**
	 * 2 byte ops in CFF DIct table * private static String[] raw2ByteValues = { "Copyright", "isFixedPitch", "ItalicAngle", "UnderlinePosition",
	 * "UnderlineThickness", "PaintType", "CharstringType", "FontMatrix", "StrokeWidth", "BlueScale", "BlueShift", "BlueFuzz", "StemSnapH",
	 * "StemSnapV", "ForceBold", "-Reserved-", "-Reserved-", "LanguageGroup", "ExpansionFactor", "initialRandomSeed", "SyntheticBase", "PostScript",
	 * "BaseFontName", "BaseFontBlend", "-Reserved-", "-Reserved-", "-Reserved-", "-Reserved-", "-Reserved-", "-Reserved-", "ROS", "CIDFontVersion",
	 * "CIDFontRevision", "CIDFontType", "CIDCount", "UIDBase", "FDArray", "FDSelect", "FontName" };/
	 **/

	/** used by t1 font renderer to ensure hsbw or sbw executed first */
	private boolean allowAll = false;

	private double h;
	private boolean isCID;

	// Used by font code - base width to add offset to
	private int[] nominalWidthX = { 0 }, defaultWidthX = { 0 };
	private boolean defaultWidthsPassed = false;
	private int[] fdSelect = null;

	public T1Glyphs(boolean isCID) {
		this.isCID = isCID;
	}

	/** used by PS2OTF conversion */
	public T1Glyphs(boolean isCID, boolean is1C) {

		this.charForGlyphIndex = new String[65536];
	}

	public T1Glyphs() {
	}

	/**
	 * Mode is PdfDecoder.SUBSTITUTE_* CONSTANT. RuntimeException will be thrown on invalid value
	 * 
	 * @return name of font NAME will be LOWERCASE to avoid issues of capitalisation when used for lookup - if no name, will default to null
	 */
	public static String[] readFontNames(FontData fontData, int mode) {

		String[] fontNames = new String[1];
		fontNames[0] = null;

		BufferedReader br = new BufferedReader(new StringReader(new String(fontData.getBytes(0, fontData.length()))));

		String line = null;

		while (true) {

			try {
				line = br.readLine();
			}
			catch (IOException e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}

			if (line == null) break;

			if (line.startsWith("/FontName")) {
				int nameStart = line.indexOf('/', 9);
				if (nameStart != -1) {
					int nameEnd = line.indexOf(' ', nameStart);
					if (nameEnd != -1) {
						String name = line.substring(nameStart + 1, nameEnd);
						fontNames[0] = name.toLowerCase();
						break;
					}
				}
			}
		}

		if (br != null) {
			try {
				br.close();
			}
			catch (Exception e) {
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " closing stream");
			}
		}

		if (fontData != null) fontData.close();

		return fontNames;
	}

	/**
	 * @param factory
	 * @param debug
	 * @param lastKey
	 * @param isFlex
	 * @param routine
	 */
	private boolean processFlex(GlyphFactory factory, boolean debug, int lastKey, boolean isFlex, int routine) {

		// if in flex feature see if we have all values - exit if not
		if ((isFlex) && (this.ptCount == 14) && (routine == 0)) {
			isFlex = false;
			for (int i = 0; i < 12; i = i + 6) {
				factory.curveTo(this.pt[i], this.pt[i + 1], this.pt[i + 2], this.pt[i + 3], this.pt[i + 4], this.pt[i + 5]);
				if (debug) System.out.println("t1 flex " + this.pt[i] + ' ' + this.pt[i + 1] + ' ' + this.pt[i + 2] + ' ' + this.pt[i + 3] + ' '
						+ this.pt[i + 4] + ' ' + this.pt[i + 5]);
			}
		}
		else
			if ((!isFlex) && (routine >= 0) && (routine <= 2)) { // determine if flex feature and enable
				isFlex = true;
				this.ptCount = 0;
				this.pt = new float[16];
				if (debug) System.out.println("flex on " + lastKey + ' ' + routine);
			}
		return isFlex;
	}

	/**
	 * @param factory
	 * @param rawInt
	 * @param debug
	 * @param dicEnd
	 */
	private void endchar(GlyphFactory factory, int rawInt, boolean debug, int dicEnd) {

		if (debug) System.out.println("Endchar");
		if (this.operandReached == 5) { // allow for width and 4 chars
			this.operandReached--;
			this.currentOp++;
		}
		if (this.operandReached == 4) {
			endchar(factory, rawInt);
		}
		else factory.closePath();
	}

	/**
	 * @param debug
	 * @param p
	 * @param lastKey
	 */
	private int mask(boolean debug, int p, int lastKey) {
		if (debug) System.out.println("hintmask/cntrmask " + lastKey);

		// if((lastKey==18)||(lastKey==1)||(lastKey==8))
		this.hintCount += this.operandReached / 2;

		if (debug) System.out.println("hintCount=" + this.hintCount);

		int count = this.hintCount;
		while (count > 0) {
			p++;
			count = count - 8;
		}
		return p;
	}

	/**
	 * @param debug
	 */
	private double sbw(boolean debug) {

		double yy;

		double val = this.operandsRead[this.operandReached - 2];
		this.y = val;

		val = this.operandsRead[this.operandReached - 1];
		this.x = val;

		this.xs = this.x;
		this.ys = this.y;
		this.allowAll = true;
		yy = this.y;

		this.h = this.operandsRead[this.operandReached - 3];

		if (debug) System.out.println("sbw xs,ys set to " + this.x + ' ' + this.y);
		return yy;
	}

	/**
	 * @param factory
	 * @param debug
	 * @param isFirst
	 */
	private void hmoveto(GlyphFactory factory, boolean debug, boolean isFirst) {
		if ((isFirst) && (this.operandReached == 2)) this.currentOp++;

		double val = this.operandsRead[this.currentOp];
		this.x = this.x + val;
		factory.moveTo((float) this.x, (float) this.y);

		// if((xs==-1)|(!pointDrawn)){
		this.xs = this.x;
		this.ys = this.y;

		if (debug) System.out.println("reset xs,ys to " + this.x + ' ' + this.y);

		// }

		if (debug) System.out.println("hmoveto " + this.x + ' ' + this.y);
	}

	/**
	 * @param factory
	 * @param debug
	 * @param isFirst
	 */
	private void rmoveto(GlyphFactory factory, boolean debug, boolean isFirst) {
		if ((isFirst) && (this.operandReached == 3)) this.currentOp++;

		if (debug) System.out.println(this.currentOp + " " + this.operandReached + ' ' + isFirst + " x,y=(" + this.x + ' ' + this.y + ") xs,ys=("
				+ this.xs + ' ' + this.ys + ") rmoveto " + this.operandsRead[this.currentOp] + ' ' + this.operandsRead[this.currentOp + 1]);

		double val = this.operandsRead[this.currentOp + 1];
		this.y = this.y + val;
		val = this.operandsRead[this.currentOp];
		this.x = this.x + val;

		factory.moveTo((float) this.x, (float) this.y);
		// if(xs==-1){
		this.xs = this.x;
		this.ys = this.y;

		if (debug) System.out.println("xs,ys=(" + this.xs + ' ' + this.ys + ") x=" + this.x + " y=" + this.y);
		// }
	}

	/**
	 * @param factory
	 * @param debug
	 * @param key
	 * 
	 */
	private void vhhvcurveto(GlyphFactory factory, boolean debug, int key) {
		boolean isHor = (key == 31);
		while (this.operandReached >= 4) {
			this.operandReached -= 4;
			if (isHor) this.x += this.operandsRead[this.currentOp];
			else this.y += this.operandsRead[this.currentOp];
			this.pt[0] = (float) this.x;
			this.pt[1] = (float) this.y;
			this.x += this.operandsRead[this.currentOp + 1];
			this.y += this.operandsRead[this.currentOp + 2];
			this.pt[2] = (float) this.x;
			this.pt[3] = (float) this.y;
			if (isHor) {
				this.y += this.operandsRead[this.currentOp + 3];
				if (this.operandReached == 1) this.x += this.operandsRead[this.currentOp + 4];
			}
			else {
				this.x += this.operandsRead[this.currentOp + 3];
				if (this.operandReached == 1) this.y += this.operandsRead[this.currentOp + 4];
			}
			this.pt[4] = (float) this.x;
			this.pt[5] = (float) this.y;
			factory.curveTo(this.pt[0], this.pt[1], this.pt[2], this.pt[3], this.pt[4], this.pt[5]);

			if (debug) System.out.println(this.currentOp + "vh/hvCurveto " + this.operandsRead[this.currentOp] + ' '
					+ this.operandsRead[this.currentOp + 1] + ' ' + this.operandsRead[this.currentOp + 2] + ' '
					+ this.operandsRead[this.currentOp + 3] + ' ' + this.operandsRead[this.currentOp + 4] + ' '
					+ this.operandsRead[this.currentOp + 5]);

			this.currentOp += 4;

			isHor = !isHor;
		}
	}

	/**
	 * @param factory
	 * @param debug
	 * @param key
	 */
	private void vvhhcurveto(GlyphFactory factory, boolean debug, int key) {

		boolean isVV = (key == 26);
		if ((this.operandReached & 1) == 1) {
			if (isVV) this.x += this.operandsRead[0];
			else this.y += this.operandsRead[0];
			this.currentOp++;
		}

		// note odd co-ord order
		while (this.currentOp < this.operandReached) {
			if (isVV) this.y += this.operandsRead[this.currentOp];
			else this.x += this.operandsRead[this.currentOp];
			this.pt[0] = (float) this.x;
			this.pt[1] = (float) this.y;
			this.x += this.operandsRead[this.currentOp + 1];
			this.y += this.operandsRead[this.currentOp + 2];
			this.pt[2] = (float) this.x;
			this.pt[3] = (float) this.y;
			if (isVV) this.y += this.operandsRead[this.currentOp + 3];
			else this.x += this.operandsRead[this.currentOp + 3];
			this.pt[4] = (float) this.x;
			this.pt[5] = (float) this.y;
			this.currentOp += 4;
			factory.curveTo(this.pt[0], this.pt[1], this.pt[2], this.pt[3], this.pt[4], this.pt[5]);

			if (debug) System.out.println("vv/hhCurveto " + this.operandsRead[0] + ' ' + this.operandsRead[1] + ' ' + this.operandsRead[2] + ' '
					+ this.operandsRead[3] + ' ' + this.operandsRead[4] + ' ' + this.operandsRead[5]);

		}
	}

	/**
	 * @param factory
	 * @param debug
	 */
	private void rlinecurve(GlyphFactory factory, boolean debug) {
		// lines
		int lineCount = (this.operandReached - 6) / 2;
		while (lineCount > 0) {
			this.x += this.operandsRead[this.currentOp];
			this.y += this.operandsRead[this.currentOp + 1];
			factory.lineTo((float) this.x, (float) this.y);

			if (debug) System.out.println("rlineCurve " + this.operandsRead[0] + ' ' + this.operandsRead[1]);

			this.currentOp += 2;
			lineCount--;
		}
		// curves
		float[] coords = new float[6];
		this.x += this.operandsRead[this.currentOp];
		this.y += this.operandsRead[this.currentOp + 1];
		coords[0] = (float) this.x;
		coords[1] = (float) this.y;

		this.x += this.operandsRead[this.currentOp + 2];
		this.y += this.operandsRead[this.currentOp + 3];
		coords[2] = (float) this.x;
		coords[3] = (float) this.y;

		this.x += this.operandsRead[this.currentOp + 4];
		this.y += this.operandsRead[this.currentOp + 5];
		coords[4] = (float) this.x;
		coords[5] = (float) this.y;

		factory.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);

		if (debug) System.out.println("rlineCurve " + this.operandsRead[0] + ' ' + this.operandsRead[1] + ' ' + this.operandsRead[2] + ' '
				+ this.operandsRead[3] + ' ' + this.operandsRead[4] + ' ' + this.operandsRead[5]);
		this.currentOp += 6;
	}

	/**
	 * @param factory
	 * @param debug
	 */
	private void closepath(GlyphFactory factory, boolean debug) {
		if (this.xs != -1) factory.lineTo((float) this.xs, (float) this.ys);

		if (debug) System.out.println("close to xs=" + this.xs + " ys=" + this.ys + ' ' + this.x + ',' + this.y);

		this.xs = -1; // flag as unset
	}

	/**
	 * @param factory
	 * @param debug
	 */
	private void hsbw(GlyphFactory factory, String glyphName, boolean debug) {
		this.x = this.x + this.operandsRead[0];
		factory.moveTo((float) this.x, 0);
		if (debug) System.out.println("hsbw " + this.x + " xs,ys=" + this.xs + ' ' + this.ys);

		// <start-pro>
		/**
		 * //<end-pro>
		 * 
		 * //<start-std> if (baseFontName != null && //Check right call dynamicVectorRenderer != null && //Check right call
		 * (dynamicVectorRenderer.getType()==org.jpedal.render.output.html.HTMLDisplay.CREATE_HTML ||
		 * dynamicVectorRenderer.getType()==org.jpedal.render.output.html.HTMLDisplay.CREATE_JAVAFX ||
		 * dynamicVectorRenderer.getType()==org.jpedal.render.output.html.HTMLDisplay.CREATE_SVG)) { //Just to be safe
		 * 
		 * dynamicVectorRenderer.saveAdvanceWidth(baseFontName,glyphName,(int)ys);
		 * 
		 * } //<end-std> /
		 **/

		this.allowAll = true;
	}

	/**
	 * @param debug
	 */
	private void pop(boolean debug) {

		// for(int ii=1;ii<count-1;ii++){
		// operandsRead[ii-1]=operandsRead[ii];
		// }
		if (this.operandReached > 0) this.operandReached--;
		if (debug) System.out.println("POP");

		if (debug) {
			for (int i = 0; i < 6; i++)
				System.out.println(i + " == " + this.operandsRead[i] + ' ' + this.operandReached);
		}
	}

	/**
	 * @param debug
	 */
	private void setcurrentpoint(boolean debug) {
		// x=operandsRead[0];
		// y=operandsRead[1];
		// factory.moveTo((float)operandsRead[0],(float)operandsRead[1]);
		if (debug) System.out.println("setCurrentPoint " + this.operandsRead[0] + ' ' + this.operandsRead[1]);
	}

	/**
	 * @param debug
	 */
	private void div(boolean debug) {
		if (debug) {
			for (int i = 0; i < 6; i++)
				System.out.println(i + " " + this.currentOp + ' ' + this.operandsRead[i] + ' ' + this.operandReached);
		}
		double value = this.operandsRead[this.operandReached - 2] / this.operandsRead[this.operandReached - 1];

		// operandReached--;
		if (this.operandReached > 0) this.operandReached--;
		this.operandsRead[this.operandReached - 1] = value;

		if (debug) {
			for (int i = 0; i < 6; i++)
				System.out.println("after====" + i + " == " + this.operandsRead[i] + ' ' + this.operandReached);
		}
		if (debug) System.out.println("DIV");
	}

	/**
	 * @param factory
	 * @param debug
	 * @param isFirst
	 */
	private void vmoveto(GlyphFactory factory, boolean debug, boolean isFirst) {
		if ((isFirst) && (this.operandReached == 2)) this.currentOp++;
		this.y = this.y + this.operandsRead[this.currentOp];
		factory.moveTo((float) this.x, (float) this.y);

		// if((xs==-1)){
		this.xs = this.x;
		this.ys = this.y;

		if (debug) System.out.println("Set xs,ys= " + this.xs + ' ' + this.ys);
		// }

		if (debug) System.out.println("vmoveto " + this.operandsRead[0] + ' ' + this.operandsRead[1] + " currentOp" + this.currentOp + " y=" + this.y
				+ ' ' + isFirst);
	}

	/**
	 * @param factory
	 * @param debug
	 */
	private void rlineto(GlyphFactory factory, boolean debug) {
		int lineCount = this.operandReached / 2;
		while (lineCount > 0) {
			this.x += this.operandsRead[this.currentOp];
			this.y += this.operandsRead[this.currentOp + 1];
			factory.lineTo((float) this.x, (float) this.y);
			this.currentOp += 2;
			lineCount--;

			if (debug) System.out.println("x,y= (" + this.x + ' ' + this.y + ") rlineto " + this.operandsRead[0] + ' ' + this.operandsRead[1]);

		}
	}

	/**
	 * @param factory
	 * @param debug
	 * @param key
	 */
	private void hvlineto(GlyphFactory factory, boolean debug, int key) {
		boolean isHor = (key == 6);
		int start = 0;
		while (start < this.operandReached) {
			if (isHor) this.x += this.operandsRead[start];
			else this.y += this.operandsRead[start];
			factory.lineTo((float) this.x, (float) this.y);

			if (debug) System.out.println("h/vlineto " + this.operandsRead[0] + ' ' + this.operandsRead[1]);

			start++;
			isHor = !isHor;
		}
	}

	/**
	 * @param factory
	 * @param debug
	 */
	private void rrcurveto(GlyphFactory factory, boolean debug) {

		int curveCount = (this.operandReached) / 6;

		if (debug && curveCount > 1) System.out.println("**********currentOp=" + this.currentOp + " curves=" + curveCount);

		while (curveCount > 0) {
			float[] coords = new float[6];
			this.x += this.operandsRead[this.currentOp];
			this.y += this.operandsRead[this.currentOp + 1];
			coords[0] = (float) this.x;
			coords[1] = (float) this.y;

			this.x += this.operandsRead[this.currentOp + 2];
			this.y += this.operandsRead[this.currentOp + 3];
			coords[2] = (float) this.x;
			coords[3] = (float) this.y;

			this.x += this.operandsRead[this.currentOp + 4];
			this.y += this.operandsRead[this.currentOp + 5];
			coords[4] = (float) this.x;
			coords[5] = (float) this.y;

			factory.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);

			// if(debug)
			// System.out.println("now="+x+" "+y);
			if (debug) System.out.println("rrcurveto " + this.operandsRead[this.currentOp] + ' ' + this.operandsRead[this.currentOp + 1] + ' '
					+ this.operandsRead[this.currentOp + 2] + ' ' + this.operandsRead[this.currentOp + 3] + ' '
					+ this.operandsRead[this.currentOp + 4] + ' ' + this.operandsRead[this.currentOp + 5]);
			this.currentOp += 6;
			curveCount--;
		}
	}

	/**
	 * @param factory
	 * @param rawInt
	 */
	private void endchar(GlyphFactory factory, int rawInt) {
		StandardFonts.checkLoaded(StandardFonts.STD);
		float adx = (float) (this.x + this.operandsRead[this.currentOp]);
		float ady = (float) (this.y + this.operandsRead[this.currentOp + 1]);
		String bchar = StandardFonts.getUnicodeChar(StandardFonts.STD, (int) this.operandsRead[this.currentOp + 2]);
		String achar = StandardFonts.getUnicodeChar(StandardFonts.STD, (int) this.operandsRead[this.currentOp + 3]);

		this.x = 0;
		this.y = 0;
		decodeGlyph(null, factory, bchar, rawInt, "", 0, true);
		factory.closePath();
		factory.moveTo(adx, ady);
		this.x = adx;
		this.y = ady;
		decodeGlyph(null, factory, achar, rawInt, "", 0, true);

		if (this.xs == -1) {
			this.xs = this.x;
			this.ys = this.y;

			System.out.println("ENDCHAR Set xs,ys= " + this.xs + ' ' + this.ys);
		}
	}

	/**
	 * @param factory
	 * @param debug
	 */
	private void rcurveline(GlyphFactory factory, boolean debug) {
		// curves
		int curveCount = (this.operandReached - 2) / 6;
		while (curveCount > 0) {
			float[] coords = new float[6];
			this.x += this.operandsRead[this.currentOp];
			this.y += this.operandsRead[this.currentOp + 1];
			coords[0] = (float) this.x;
			coords[1] = (float) this.y;

			this.x += this.operandsRead[this.currentOp + 2];
			this.y += this.operandsRead[this.currentOp + 3];
			coords[2] = (float) this.x;
			coords[3] = (float) this.y;

			this.x += this.operandsRead[this.currentOp + 4];
			this.y += this.operandsRead[this.currentOp + 5];
			coords[4] = (float) this.x;
			coords[5] = (float) this.y;

			factory.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);

			if (debug) System.out.println("rCurveline " + this.operandsRead[0] + ' ' + this.operandsRead[1] + ' ' + this.operandsRead[2] + ' '
					+ this.operandsRead[3] + ' ' + this.operandsRead[4] + ' ' + this.operandsRead[5]);
			this.currentOp += 6;
			curveCount--;
		}

		// line
		this.x += this.operandsRead[this.currentOp];
		this.y += this.operandsRead[this.currentOp + 1];
		factory.lineTo((float) this.x, (float) this.y);
		this.currentOp += 2;

		if (debug) System.out.println("rCurveline " + this.operandsRead[0] + ' ' + this.operandsRead[1]);
	}

	/**
	 * @param factory
	 * @param rawInt
	 * @param currentOp
	 */
	private void seac(GlyphFactory factory, int rawInt, int currentOp) {

		StandardFonts.checkLoaded(StandardFonts.STD);
		float adx = (float) (this.operandsRead[currentOp + 1]);
		float ady = (float) (this.operandsRead[currentOp + 2]);
		String bchar = StandardFonts.getUnicodeChar(StandardFonts.STD, (int) this.operandsRead[currentOp + 3]);
		String achar = StandardFonts.getUnicodeChar(StandardFonts.STD, (int) this.operandsRead[currentOp + 4]);

		double preX = this.x;
		// x=0;
		this.y = 0;
		decodeGlyph(null, factory, bchar, rawInt, "", 0, true);

		factory.closePath();
		factory.moveTo(0, 0);
		this.x = adx + preX;
		this.y = ady;
		decodeGlyph(null, factory, achar, rawInt, "", 0, true);
	}

	/**
	 * @param factory
	 * @param debug
	 */
	private void flex1(GlyphFactory factory, boolean debug) {
		double dx = 0, dy = 0, x1 = this.x, y1 = this.y;

		/* workout dx/dy/horizontal and reset flag */
		for (int count = 0; count < 10; count = count + 2) {
			dx += this.operandsRead[count];
			dy += this.operandsRead[count + 1];
		}
		boolean isHorizontal = (Math.abs(dx) > Math.abs(dy));

		for (int points = 0; points < 6; points = points + 2) {// first curve
			this.x += this.operandsRead[points];
			this.y += this.operandsRead[points + 1];
			this.pt[points] = (float) this.x;
			this.pt[points + 1] = (float) this.y;
		}
		factory.curveTo(this.pt[0], this.pt[1], this.pt[2], this.pt[3], this.pt[4], this.pt[5]);
		if (debug) System.out.println("flex1 first curve " + this.operandsRead[0] + ' ' + this.operandsRead[1] + ' ' + this.operandsRead[2] + ' '
				+ this.operandsRead[3] + ' ' + this.operandsRead[4] + ' ' + this.operandsRead[5]);

		for (int points = 0; points < 4; points = points + 2) {// second curve
			this.x += this.operandsRead[points + 6];
			this.y += this.operandsRead[points + 7];
			this.pt[points] = (float) this.x;
			this.pt[points + 1] = (float) this.y;
		}

		if (isHorizontal) { // last point
			this.x += this.operandsRead[10];
			this.y = y1;
		}
		else {
			this.x = x1;
			this.y += this.operandsRead[10];
		}
		this.pt[4] = (float) this.x;
		this.pt[5] = (float) this.y;
		factory.curveTo(this.pt[0], this.pt[1], this.pt[2], this.pt[3], this.pt[4], this.pt[5]);
		if (debug) System.out.println("flex1 second curve " + this.operandsRead[0] + ' ' + this.operandsRead[1] + ' ' + this.operandsRead[2] + ' '
				+ this.operandsRead[3] + ' ' + this.operandsRead[4] + ' ' + this.operandsRead[5]);
	}

	/**
	 * @param factory
	 * @param debug
	 */
	private void flex(GlyphFactory factory, boolean debug) {
		for (int curves = 0; curves < 12; curves = curves + 6) {
			for (int points = 0; points < 6; points = points + 2) {
				this.x += this.operandsRead[curves + points];
				this.y += this.operandsRead[curves + points + 1];
				this.pt[points] = (float) this.x;
				this.pt[points + 1] = (float) this.y;
			}
			factory.curveTo(this.pt[0], this.pt[1], this.pt[2], this.pt[3], this.pt[4], this.pt[5]);

			if (debug) System.out.println("flex " + this.pt[0] + ' ' + this.pt[1] + ' ' + this.pt[2] + ' ' + this.pt[3] + ' ' + this.pt[4] + ' '
					+ this.pt[5]);

		}
	}

	/**
	 * @param factory
	 * @param debug
	 */
	private void hflex(GlyphFactory factory, boolean debug) {
		// first curve
		this.x += this.operandsRead[0];
		this.pt[0] = (float) this.x;
		this.pt[1] = (float) this.y;
		this.x += this.operandsRead[1];
		this.y += this.operandsRead[2];
		this.pt[2] = (float) this.x;
		this.pt[3] = (float) this.y;
		this.x += this.operandsRead[3];
		this.pt[4] = (float) this.x;
		this.pt[5] = (float) this.y;
		factory.curveTo(this.pt[0], this.pt[1], this.pt[2], this.pt[3], this.pt[4], this.pt[5]);

		if (debug) System.out.println("hflex first curve " + this.operandsRead[0] + ' ' + this.operandsRead[1] + ' ' + this.operandsRead[2] + ' '
				+ this.operandsRead[3] + ' ' + this.operandsRead[4] + ' ' + this.operandsRead[5]);

		// second curve
		this.x += this.operandsRead[4];
		this.pt[0] = (float) this.x;
		this.pt[1] = (float) this.y;
		this.x += this.operandsRead[5];
		this.pt[2] = (float) this.x;
		this.pt[3] = (float) this.y;
		this.x += this.operandsRead[6];
		this.pt[4] = (float) this.x;
		this.pt[5] = (float) this.y;
		factory.curveTo(this.pt[0], this.pt[1], this.pt[2], this.pt[3], this.pt[4], this.pt[5]);

		if (debug) System.out.println("hflex second curve " + this.operandsRead[0] + ' ' + this.operandsRead[1] + ' ' + this.operandsRead[2] + ' '
				+ this.operandsRead[3] + ' ' + this.operandsRead[4] + ' ' + this.operandsRead[5]);
	}

	/**
	 * @param factory
	 * @param debug
	 */
	private void hflex1(GlyphFactory factory, boolean debug) {
		// first curve
		this.x += this.operandsRead[0];
		this.y += this.operandsRead[1];
		this.pt[0] = (float) this.x;
		this.pt[1] = (float) this.y;
		this.x += this.operandsRead[2];
		this.y += this.operandsRead[3];
		this.pt[2] = (float) this.x;
		this.pt[3] = (float) this.y;
		this.x += this.operandsRead[4];
		this.pt[4] = (float) this.x;
		this.pt[5] = (float) this.y;
		factory.curveTo(this.pt[0], this.pt[1], this.pt[2], this.pt[3], this.pt[4], this.pt[5]);

		if (debug) System.out.println("36 first curve " + this.operandsRead[0] + ' ' + this.operandsRead[1] + ' ' + this.operandsRead[2] + ' '
				+ this.operandsRead[3] + ' ' + this.operandsRead[4] + ' ' + this.operandsRead[5]);

		// second curve
		this.x += this.operandsRead[5];
		this.pt[0] = (float) this.x;
		this.pt[1] = (float) this.y;
		this.x += this.operandsRead[6];
		this.y += this.operandsRead[7];
		this.pt[2] = (float) this.x;
		this.pt[3] = (float) this.y;
		this.x += this.operandsRead[8];
		this.pt[4] = (float) this.x;
		this.pt[5] = (float) this.y;
		factory.curveTo(this.pt[0], this.pt[1], this.pt[2], this.pt[3], this.pt[4], this.pt[5]);

		if (debug) System.out.println("36 second curve " + this.operandsRead[0] + ' ' + this.operandsRead[1] + ' ' + this.operandsRead[2] + ' '
				+ this.operandsRead[3] + ' ' + this.operandsRead[4] + ' ' + this.operandsRead[5]);
	}

	/**
	 * used by non type3 font
	 */
	@Override
	public PdfGlyph getEmbeddedGlyph(GlyphFactory factory, String glyph, float[][] Trm, int rawInt, String displayValue, float currentWidth,
			String key) {

		/** flush cache if needed */
		if ((this.lastTrm[0][0] != Trm[0][0]) | (this.lastTrm[1][0] != Trm[1][0]) | (this.lastTrm[0][1] != Trm[0][1])
				| (this.lastTrm[1][1] != Trm[1][1])) {
			this.lastTrm = Trm;
			flush();
		}

		// either calculate the glyph to draw or reuse if alreasy drawn
		PdfGlyph transformedGlyph2 = getEmbeddedCachedShape(rawInt);

		if (transformedGlyph2 == null) {

			/** create new stack for glyph */
			this.operandsRead = new double[this.max];
			this.operandReached = 0;

			this.x = -factory.getLSB();

			this.y = 0;
			decodeGlyph(key, factory, glyph, rawInt, displayValue, currentWidth, false);

			// generate Glyph
			transformedGlyph2 = factory.getGlyph(false);

			if (transformedGlyph2 != null) transformedGlyph2.setID(rawInt);

			// save so we can reuse if it occurs again in this TJ command
			setEmbeddedCachedShape(rawInt, transformedGlyph2);
		}

		return transformedGlyph2;
	}

	/** Utility method used during processing of type1C files */
	@Override
	final public int getNumber(byte[] fontDataAsArray, int pos, double[] values, int valuePointer, boolean debug) {

		int b0, i;
		double x = 0;

		b0 = fontDataAsArray[pos] & 0xFF;

		if ((b0 < 28) | (b0 == 31)) { // error!
			System.err.println("!!!!Incorrect type1C operand");
		}
		else
			if (b0 == 28) { // 2 byte number in range -32768
							// +32767
				x = (fontDataAsArray[pos + 1] << 8) + (fontDataAsArray[pos + 2] & 0xff);
				pos += 3;
			}
			else
				if (b0 == 255) {

					if (this.is1C) {
						x = ((fontDataAsArray[pos + 1] & 0xFF) << 8) + (fontDataAsArray[pos + 2] & 0xFF);
						x += (((fontDataAsArray[pos + 3] & 0xFF) << 8) + (fontDataAsArray[pos + 4] & 0xFF)) / 65536.0;
						if (fontDataAsArray[pos + 1] < 0) {
							x -= 65536;
						}

						if (debug) {
							System.out.println("x=" + x);

							for (int j = 0; j < 5; j++) {
								System.out.println(j + " " + fontDataAsArray[pos + j] + ' ' + (fontDataAsArray[pos + j] & 0xff) + ' '
										+ (fontDataAsArray[pos + j] & 0x7f));
							}
						}
					}
					else {
						// x=((content[pos + 1]& 127) << 24) + (content[pos + 2]<<16)+(content[pos + 3] << 8) + content[pos + 4];
						x = ((fontDataAsArray[pos + 1] & 0xFF) << 24) + ((fontDataAsArray[pos + 2] & 0xFF) << 16)
								+ ((fontDataAsArray[pos + 3] & 0xFF) << 8) + (fontDataAsArray[pos + 4] & 0xFF);

					}

					pos += 5;
				}
				else
					if (b0 == 29) { // 4 byte signed number
						x = ((fontDataAsArray[pos + 1] & 0xFF) << 24) + ((fontDataAsArray[pos + 2] & 0xFF) << 16)
								+ ((fontDataAsArray[pos + 3] & 0xFF) << 8) + (fontDataAsArray[pos + 4] & 0xFF);
						pos += 5;
					}
					else
						if (b0 == 30) { // BCD values

							char buf[] = new char[65];
							pos += 1;
							i = 0;
							while (i < 64) {
								int b = fontDataAsArray[pos++] & 0xFF;

								int nyb0 = (b >> 4) & 0x0f;
								int nyb1 = b & 0x0f;

								if (nyb0 == 0xf) break;
								buf[i++] = nybChars.charAt(nyb0);
								if (i == 64) break;
								if (nyb0 == 0xc) buf[i++] = '-';
								if (i == 64) break;
								if (nyb1 == 0xf) break;
								buf[i++] = nybChars.charAt(nyb1);
								if (i == 64) break;
								if (nyb1 == 0xc) buf[i++] = '-';
							}
							x = Double.valueOf(new String(buf, 0, i));

						}
						else
							if (b0 < 247) { // -107 +107
								x = b0 - 139;
								pos++;
							}
							else
								if (b0 < 251) { // 2 bytes +108 +1131
									x = ((b0 - 247) << 8) + (fontDataAsArray[pos + 1] & 0xff) + 108;
									pos += 2;
								}
								else { // -1131 -108
									x = -((b0 - 251) << 8) - (fontDataAsArray[pos + 1] & 0xff) - 108;
									pos += 2;
								}

		// assign number
		values[valuePointer] = x;

		// if(debug)
		// System.out.println("Number ="+x);
		return pos;
	}

	/** Utility method used during processing of type1C files */
	@Override
	final public int getNumber(FontData fontDataAsObject, int pos, double[] values, int valuePointer, boolean debug) {

		int b0, i;
		double x = 0;

		b0 = fontDataAsObject.getByte(pos) & 0xFF;

		if ((b0 < 28) | (b0 == 31)) { // error!
			System.err.println("!!!!Incorrect type1C operand");
		}
		else
			if (b0 == 28) { // 2 byte number in range -32768
							// +32767
				x = (fontDataAsObject.getByte(pos + 1) << 8) + (fontDataAsObject.getByte(pos + 2) & 0xff);
				pos += 3;
			}
			else
				if (b0 == 255) {

					if (this.is1C) {
						int top = ((fontDataAsObject.getByte(pos + 1) & 0xFF) << 8) + (fontDataAsObject.getByte(pos + 2) & 0xFF);
						if (top > 32768) top = 65536 - top;
						double numb = top;
						double dec = ((fontDataAsObject.getByte(pos + 3) & 0xFF) << 8) + (fontDataAsObject.getByte(pos + 4) & 0xFF);
						x = numb + (dec / 65536);
						if (fontDataAsObject.getByte(pos + 1) < 0) {
							if (debug) System.out.println("Negative " + x);
							x = -x;

						}

						if (debug) {
							System.out.println("x=" + x);

							for (int j = 0; j < 5; j++) {
								System.out.println(j + " " + fontDataAsObject.getByte(pos + j) + ' ' + (fontDataAsObject.getByte(pos + j) & 0xff)
										+ ' ' + (fontDataAsObject.getByte(pos + j) & 0x7f));
							}
						}
					}
					else {
						// x=((content[pos + 1]& 127) << 24) + (content[pos + 2]<<16)+(content[pos + 3] << 8) + content[pos + 4];
						x = ((fontDataAsObject.getByte(pos + 1) & 0xFF) << 24) + ((fontDataAsObject.getByte(pos + 2) & 0xFF) << 16)
								+ ((fontDataAsObject.getByte(pos + 3) & 0xFF) << 8) + (fontDataAsObject.getByte(pos + 4) & 0xFF);

					}

					pos += 5;
				}
				else
					if (b0 == 29) { // 4 byte signed number
						x = ((fontDataAsObject.getByte(pos + 1) & 0xFF) << 24) + ((fontDataAsObject.getByte(pos + 2) & 0xFF) << 16)
								+ ((fontDataAsObject.getByte(pos + 3) & 0xFF) << 8) + (fontDataAsObject.getByte(pos + 4) & 0xFF);
						pos += 5;
					}
					else
						if (b0 == 30) { // BCD values

							char buf[] = new char[65];
							pos += 1;
							i = 0;
							while (i < 64) {
								int b = fontDataAsObject.getByte(pos++) & 0xFF;

								int nyb0 = (b >> 4) & 0x0f;
								int nyb1 = b & 0x0f;

								if (nyb0 == 0xf) break;
								buf[i++] = nybChars.charAt(nyb0);
								if (i == 64) break;
								if (nyb0 == 0xc) buf[i++] = '-';
								if (i == 64) break;
								if (nyb1 == 0xf) break;
								buf[i++] = nybChars.charAt(nyb1);
								if (i == 64) break;
								if (nyb1 == 0xc) buf[i++] = '-';
							}
							x = Double.valueOf(new String(buf, 0, i));

						}
						else
							if (b0 < 247) { // -107 +107
								x = b0 - 139;
								pos++;
							}
							else
								if (b0 < 251) { // 2 bytes +108 +1131
									x = ((b0 - 247) << 8) + (fontDataAsObject.getByte(pos + 1) & 0xff) + 108;
									pos += 2;
								}
								else { // -1131 -108
									x = -((b0 - 251) << 8) - (fontDataAsObject.getByte(pos + 1) & 0xff) - 108;
									pos += 2;
								}

		// assign number
		values[valuePointer] = x;

		// if(debug)
		// System.out.println("Number ="+x);
		return pos;
	}

	/*
	 * creates glyph from type1c font commands
	 */
	protected void decodeGlyph(String embKey, GlyphFactory factory, String glyph, int rawInt, String displayValue, float currentWidth,
			boolean isRecursive) {

		byte[] glyphStream;

		boolean debug = false;// rawInt==1445;//rawInt==40 || rawInt==105 || rawInt==109;

		// System.out.println(glyph+" "+baseFontName+" "+rawInt+" "+currentWidth);

		this.allowAll = false; // used by T1 to make sure sbw of hsbw

		/**
		 * if((this.baseFontName.indexOf("YYSITY+Aybabtu-Regular")!=-1)){ debug=rawInt==0; if(displayValue.equals("aaaaaa")) debug=true; //else //
		 * debug=false; // System.out.println(isCID+" "+glyph+" "+baseFontName+" "+currentWidth+" "+displayValue+" "+rawInt); }
		 * 
		 * /** get the stream of commands for the glyph
		 */
		if (this.isCID) {
			glyphStream = (byte[]) this.charStrings.get(String.valueOf(rawInt));
		}
		else {
			if (glyph == null) glyph = displayValue;// getMappedChar(rawInt,false);

			if (glyph == null) {
				glyph = embKey;

				if (glyph == null) glyph = ".notdef";
			}

			/**
			 * get the bytestream of commands and reset global values
			 */
			glyphStream = (byte[]) this.charStrings.get(glyph);

			if (glyphStream == null) {

				if (embKey != null) glyphStream = (byte[]) this.charStrings.get(embKey);
				if (glyphStream == null) glyphStream = (byte[]) this.charStrings.get(".notdef");
			}
		}

		/**
		 * if valid stream then decode
		 */
		if (glyphStream != null) {

			boolean isFirst = true; // flag to pick up extra possible first value
			boolean isNumber;
			this.ptCount = 0;
			int commandCount = -1; // command number
			int p = 0, lastNumberStart = 0, nextVal, key = 0, lastKey, dicEnd = glyphStream.length, lastVal = 0;
			this.currentOp = 0;
			this.hintCount = 0;
			double ymin = 999999, ymax = 0, yy = 1000;
			boolean isFlex = false; // flag to show its a flex command in t1
			this.pt = new float[6];
			int potentialWidth = 0;

			this.h = 100000;
			/** set length for 1C */
			if (this.is1C) {
				this.operandsRead = new double[this.max];
				this.operandReached = 0;
				this.allowAll = true;

			}

			if ((debug)) {
				System.out.println("******************" + ' ' + displayValue + ' ' + glyph);
				for (int j = 0; j < dicEnd; j++)
					System.out.println(j + " " + (glyphStream[j] & 0xff));

				System.out.println("=====xs=" + this.xs + " ys=" + this.ys);
			}

			/**
			 * work through the commands decoding and extracting numbers (operands are FIRST)
			 */

			while (p < dicEnd) {

				// get next byte value from stream
				nextVal = glyphStream[p] & 0xFF;

				// if(debug)
				// System.out.println("p="+p+">>>"+nextVal+" operandReached="+operandReached+" currentOp="+currentOp+" x="+x+" y="+y);

				if (nextVal > 31 || nextVal == 28) { // if its a number get it and update pointer p

					// track location
					lastNumberStart = p;

					// isNumber=true;
					p = getNumber(glyphStream, p, this.operandsRead, this.operandReached, debug);
					lastVal = (int) this.operandsRead[this.operandReached];// nextVal;
					this.operandReached++;

					// Pick up if first item is a number as this may be the width offset - currently only used for HTML
					if (lastNumberStart == 0) {
						if (this.nominalWidthX.length == 1) {
							potentialWidth = this.nominalWidthX[0] + lastVal;
						}
						else {
							int glyphNo = (Integer) this.glyphNumbers.get("" + rawInt) - 1;
							if (glyphNo < this.fdSelect.length) {
								potentialWidth = this.nominalWidthX[this.fdSelect[glyphNo]] + lastVal;
							}
						}
					}

					if (nextVal == 28 && debug) System.out.println("Shortint " + lastVal);

				}
				else { // operator
					// <start-pro>
					/**
					 * //<end-pro>
					 * 
					 * //<start-std> //This tests whether the previously saved potential width is an argument of the first operator or //an actual
					 * width. If it's an actual width, it's saved. boolean hasOddArgs=false; if (nextVal == 22 || //hmoveto nextVal == 4 || //vmoveto
					 * nextVal == 10 || //callsubr nextVal == 29 || //callgsubr (nextVal == 12 && ( glyphStream[p+1] == 9 || //abs glyphStream[p+1] ==
					 * 14 || //neg glyphStream[p+1] == 26 || //sqrt glyphStream[p+1] == 18 || //drop glyphStream[p+1] == 27 || //dup glyphStream[p+1]
					 * == 21 || //get glyphStream[p+1] == 5))) { //not hasOddArgs = true; }
					 * 
					 * if (commandCount == -1 && //Only on first command baseFontName != null && //Check right call dynamicVectorRenderer != null &&
					 * //Check right call (dynamicVectorRenderer.getType()== org.jpedal.render.output.html.HTMLDisplay.CREATE_HTML ||
					 * dynamicVectorRenderer.getType()== org.jpedal.render.output.html.HTMLDisplay.CREATE_JAVAFX || dynamicVectorRenderer.getType()==
					 * org.jpedal.render.output.html.HTMLDisplay.CREATE_SVG)) { //Just to be safe
					 * 
					 * if (((!hasOddArgs && (operandReached % 2 == 1)) || (hasOddArgs && (operandReached % 2 == 0)))) { //Make sure not an argument
					 * 
					 * if ("notdef".equals(glyph)) { dynamicVectorRenderer.saveAdvanceWidth(baseFontName, String.valueOf(rawInt),potentialWidth); }
					 * else { dynamicVectorRenderer.saveAdvanceWidth(baseFontName,glyph,potentialWidth); }
					 * 
					 * } else { //Store the default widths. if (!defaultWidthsPassed) { for (int i=0; i<defaultWidthX.length; i++) {
					 * dynamicVectorRenderer.saveAdvanceWidth(baseFontName, "JPedalDefaultWidth"+i, defaultWidthX[i]); } defaultWidthsPassed = true; }
					 * } } //<end-std> /
					 **/

					commandCount++;
					isNumber = false;
					lastKey = key;
					key = nextVal;
					p++;
					this.currentOp = 0;

					if (key == 12) { // handle escaped keys (ie 2 byte ops)
						key = glyphStream[p] & 0xFF;
						p++;

						if (key == 7) { // sbw
							yy = sbw(debug);
							this.operandReached = 0; // move to first operator
						}
						else
							if (this.allowAll) { // other 2 byte operands
								if (key == 16) { // other subroutine
									isFlex = processFlex(factory, debug, lastKey, isFlex, lastVal);
									this.operandReached = 0; // move to first operator
								}
								else
									if (key == 33) { // setcurrentpoint
										setcurrentpoint(debug);
										this.operandReached = 0; // move to first operator
									}
									else
										if (key == 34) { // hflex
											hflex(factory, debug);
											this.operandReached = 0; // move to first operator
										}
										else
											if (key == 35) { // fle
												flex(factory, debug);
												this.operandReached = 0; // move to first operator
											}
											else
												if (key == 36) { // hflex1
													hflex1(factory, debug);
													this.operandReached = 0; // move to first operator
												}
												else
													if (key == 37) { // flex1
														flex1(factory, debug);
														this.operandReached = 0; // move to first operator
													}
													else
														if (key == 6) { // seac
															seac(factory, rawInt, this.currentOp);
															this.operandReached = 0; // move to first operator
														}
														else
															if (key == 12) { // div functionn
																div(debug);
															}
															else
																if (key == 17) { // POP function
																	pop(debug);
																}
																else
																	if (key == 0) { // dotsection
																		this.operandReached = 0; // move to first operator
																		if (debug) System.out.println("Dot section");
																	}
																	else
																		if (debug) {
																			this.operandReached = 0; // move to first operator
																			System.out.println("1 Not implemented " + p + " id=" + key + " op="
																					+ Type1.T1C[key]);
																		}
																		else this.operandReached = 0; // move to first operator
							}
					}
					else
						if (key == 13) { // hsbw (T1 only)
							hsbw(factory, glyph, debug);
							this.operandReached = 0; // move to first operator
						}
						else
							if (this.allowAll) { // other one byte ops
								if (key == 0) { // reserved
								}
								else
									if ((key == 1) | (key == 3) | (key == 18) | (key == 23)) { // hstem vstem hstemhm vstemhm
										this.hintCount += this.operandReached / 2;
										this.operandReached = 0; // move to first operator
										if (debug) System.out.println("One of hstem vstem hstemhm vstemhm " + key + ' ' + this.xs + ' ' + this.ys);
									}
									else
										if (key == 4) { // vmoveto
											if (isFlex) {
												double val = this.operandsRead[this.currentOp];
												this.y = this.y + val;
												this.pt[this.ptCount] = (float) this.x;
												this.ptCount++;
												this.pt[this.ptCount] = (float) this.y;
												this.ptCount++;
												if (debug) System.out.println("flex value " + this.x + ' ' + this.y);
											}
											else vmoveto(factory, debug, isFirst);
											this.operandReached = 0; // move to first operator
										}
										else
											if ((key == 5)) {// rlineto
												rlineto(factory, debug);
												this.operandReached = 0; // move to first operator
											}
											else
												if ((key == 6) | (key == 7)) {// hlineto or vlineto
													hvlineto(factory, debug, key);
													this.operandReached = 0; // move to first operator
												}
												else
													if (key == 8) {// rrcurveto
														rrcurveto(factory, debug);
														this.operandReached = 0; // move to first operator
													}
													else
														if (key == 9) { // closepath (T1 only)
															closepath(factory, debug);
															this.operandReached = 0; // move to first operator
														}
														else
															if (key == 10 || (key == 29)) { // callsubr and callgsubr

																if (debug) System.out.println(key + " -------------- last Value=" + lastVal + ' '
																		+ this.allowAll + " commandCount=" + commandCount + " operandReached="
																		+ this.operandReached + ' ' + isFirst);

																if (1 == 2 && this.is1C && isFirst) {
																	this.xs = -1;
																	this.ys = -1;

																	if (debug) System.out.println("reset xs,ys");
																}
																if (!this.is1C && key == 10 && (lastVal >= 0) && (lastVal <= 2) && lastKey != 11
																		&& this.operandReached > 5) {// last key stops spurious match in multiple
																										// sub-routines
																	isFlex = processFlex(factory, debug, lastKey, isFlex, lastVal);
																	this.operandReached = 0; // move to first operator

																}
																else {

																	// factor in bias
																	if (key == 10) lastVal = lastVal + this.localBias;
																	else lastVal = lastVal + this.globalBias;

																	byte[] newStream;
																	if (key == 10) { // local subroutine

																		newStream = (byte[]) this.charStrings.get("subrs" + (lastVal));

																		if (debug) System.out.println("=================callsubr " + lastVal);
																	}
																	else { // global subroutine
																		if (debug) System.out.println("=================callgsubr " + lastVal);

																		newStream = (byte[]) this.charStrings.get("global" + (lastVal));
																	}

																	if (newStream != null) {

																		if (debug) System.out.println("Subroutine=============" + lastVal + " op="
																				+ this.currentOp + ' ' + this.operandReached);

																		int newLength = newStream.length;
																		int oldLength = glyphStream.length;
																		int totalLength = newLength + oldLength - 2;

																		dicEnd = dicEnd + newLength - 2;
																		// workout length of new stream
																		byte[] combinedStream = new byte[totalLength];

																		System.arraycopy(glyphStream, 0, combinedStream, 0, lastNumberStart);
																		System.arraycopy(newStream, 0, combinedStream, lastNumberStart, newLength);
																		System.arraycopy(glyphStream, p, combinedStream, lastNumberStart + newLength,
																				oldLength - p);

																		glyphStream = combinedStream;

																		p = lastNumberStart;

																		if (this.operandReached > 0) this.operandReached--;

																		/**
																		 * if(debug){ //System.out.println("P now="+p);
																		 * 
																		 * System.out.println("Merged-------------"); for(int
																		 * p2=0;p2<glyphStream.length;p2++){ if(p2==lastNumberStart ||
																		 * p==(lastNumberStart+newLength)) System.out.println("----");
																		 * System.out.println(p2+" "+(glyphStream[p2] & 0xff)); } } /
																		 **/

																	}
																	else
																		if (debug) System.out.println("No data found for sub-routine "
																				+ this.charStrings);

																}
																// operandReached=0; //move to first operator
															}
															else
																if (key == 11) { // return
																	if (debug) System.out.println("return=============" + p);
																	// operandReached=0; //move to first operator
																}
																else
																	if ((key == 14)) { // endchar
																		endchar(factory, rawInt, debug, dicEnd);
																		this.operandReached = 0; // move to first operator
																		p = dicEnd + 1;
																	}
																	else
																		if (key == 16) { // blend
																			if (debug) System.out.println("Blend");
																			this.operandReached = 0; // move to first operator
																		}
																		else
																			if ((key == 19) | (key == 20)) { // hintmask //cntrmask
																				p = mask(debug, p, lastKey);
																				this.operandReached = 0; // move to first operator
																			}
																			else
																				if (key == 21) {// rmoveto
																					if (isFlex) {
																						if (debug) System.out.println(this.currentOp + " "
																								+ this.ptCount + ' ' + this.pt.length);
																						double val = this.operandsRead[this.currentOp + 1];
																						this.y = this.y + val;
																						val = this.operandsRead[this.currentOp];
																						this.x = this.x + val;
																						this.pt[this.ptCount] = (float) this.x;
																						this.ptCount++;

																						this.pt[this.ptCount] = (float) this.y;
																						this.ptCount++;
																						if (debug) System.out.println("flex value "
																								+ this.pt[this.ptCount - 2] + ' '
																								+ this.pt[this.ptCount - 1] + " count="
																								+ this.ptCount);

																					}
																					else rmoveto(factory, debug, isFirst);
																					this.operandReached = 0; // move to first operator
																				}
																				else
																					if (key == 22) { // hmoveto
																						if (isFlex) {
																							double val = this.operandsRead[this.currentOp];
																							this.x = this.x + val;
																							this.pt[this.ptCount] = (float) this.x;
																							this.ptCount++;
																							this.pt[this.ptCount] = (float) this.y;
																							this.ptCount++;
																							if (debug) System.out.println("flex value " + this.x
																									+ ' ' + this.y);
																						}
																						else hmoveto(factory, debug, isFirst);
																						this.operandReached = 0; // move to first operator
																					}
																					else
																						if (key == 24) { // rcurveline
																							rcurveline(factory, debug);
																							this.operandReached = 0; // move to first operator
																						}
																						else
																							if (key == 25) { // rlinecurve
																								rlinecurve(factory, debug);
																								this.operandReached = 0; // move to first operator
																							}
																							else
																								if ((key == 26) | (key == 27)) { // vvcurve hhcurveto
																									vvhhcurveto(factory, debug, key);
																									this.operandReached = 0; // move to first operator
																								}
																								else
																									if ((key == 30) | (key == 31)) { // vhcurveto/hvcurveto
																										vhhvcurveto(factory, debug, key);
																										this.operandReached = 0; // move to first
																																	// operator
																									}
																									else
																										if (debug) { // unknown command
																											this.operandReached = 0; // move to first
																																		// operator

																											System.out.println("Unsupported command "
																													+ p + ">>>>>" + this.hintCount
																													+ ">>>>>>key=" + key + ' '
																													+ Type1.T1CcharCodes1Byte[key]
																													+ " <1<<" + this.operandsRead);
																											for (int j = 0; j < dicEnd; j++)
																												System.out.println(j + " "
																														+ (glyphStream[j] & 0xff));

																										}

								/**/
								if (debug && !isNumber) {
									BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
									Graphics2D g2 = img.createGraphics();
									g2.setColor(Color.red);
									// AffineTransform af=new AffineTransform();

									for (int ii = 0; ii < 7; ii++) {
										g2.drawLine(ii * 100, 0, ii * 100, 1000);
										g2.drawLine(0, ii * 100, 1000, ii * 100);
									}
									g2.setColor(Color.black);
									PdfGlyph transformedGlyph2 = factory.getGlyph(true);

									if (transformedGlyph2.getShape() != null) {
										transformedGlyph2.render(GraphicsState.STROKE, g2, 1f, false);

										// if(p>128)
										ShowGUIMessage.showGUIMessage(p + " x " + " x,y=" + this.x + ' ' + this.y, img, p + " x ");
									}
								}
							}

					if (ymin > this.y) ymin = this.y;

					if (ymax < this.y) ymax = this.y;

					if (key != 19 && key != 29 && key != 10) isFirst = false;
				}
			}

			if (yy > this.h) ymin = yy - this.h;

			// System.out.println("raw ---ymin="+ymin+" yy="+yy+" ymax="+ymax+" "+glyph+" "+rawInt);

			if ((ymax) < yy) {
				ymin = 0;

			}
			else
				if ((yy == ymax)) {// added for M2003W.pdf font display
					// if(ymin<0)
					// ymin=270;
				}
				else {

					float dy = (float) (ymax - (yy - ymin));

					// System.out.println(dy+" "+ymin+" "+yy+" "+ymax+" "+(yy-ymax));
					if ((dy < 0)) {

						if (yy - ymax <= dy) ymin = dy;
						else ymin = ymin - dy;
						// }
					}
					else ymin = 0;

					if (ymin < 0) ymin = 0;
					// if(glyph.indexOf("325")!=-1){
					// ymin=0;
					// System.out.println(glyph+" "+ymin+" "+ymax+" "+dy);
					// }
					// if((ymin<0)&&((ymax-(yy-ymin)<0))){
					// if(ymin>yy)
					// ymin=yy-ymin;
					// else
					// if(ymax>yy)
					// ymin=yy-ymax;
					// debug=true;
					// }
				}

			/** set values to adjust glyph vertically */

			factory.setYMin((float) (ymin), (float) ymax);

			/**/
			if ((debug) & (!isRecursive)) {
				BufferedImage img = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = img.createGraphics();
				g2.setColor(Color.red);
				AffineTransform af = new AffineTransform();
				af.scale(0.25, 0.25);
				// af.translate(0,30);
				g2.transform(af);

				PdfGlyph transformedGlyph2 = factory.getGlyph(true);

				g2.setColor(Color.green);
				for (int j = 0; j < 7; j++)
					g2.drawLine(0, j * 50, 1000, j * 50);
				transformedGlyph2.render(GraphicsState.STROKE, g2, 1f, false);
				// if(p>160)
				// ShowGUIMessage.showGUIMessage("Completed "+p+" x "+ii+" x,y="+x+" "+y,img,p+" x "+ii);
				// System.out.println(ii+"/"+p+" start="+xs+" "+ys);
			}

		}
	}

	/** add a charString value */
	@Override
	public void setCharString(String glyph, byte[] stream, int glyphNo) {
		this.charStrings.put(glyph, stream);
		this.glyphNumbers.put(glyph, glyphNo);
	}

	/** index for each char */
	@Override
	public void setIndexForCharString(int index, String charName) {
		if (this.charForGlyphIndex == null) this.charForGlyphIndex = new String[65536];

		if (index < this.charForGlyphIndex.length) this.charForGlyphIndex[index] = charName;
	}

	/** index for each char */
	@Override
	public String getIndexForCharString(int index) {
		return this.charForGlyphIndex[index];
	}

	@Override
	public boolean is1C() {
		return this.is1C;
	}

	/**
	 * Used by HTML code
	 * 
	 * @param defaultWidthX
	 * @param nominalWidthX
	 */
	public void setWidthValues(int[] defaultWidthX, int[] nominalWidthX) {
		this.nominalWidthX = nominalWidthX;
		this.defaultWidthX = defaultWidthX;
	}

	@Override
	public void setis1C(boolean is1C) {

		this.is1C = is1C;
	}

	DynamicVectorRenderer dynamicVectorRenderer = null;

	@Override
	public void setRenderer(DynamicVectorRenderer current) {
		this.dynamicVectorRenderer = current;
	}

	/**
	 * Pass in array saying which Font DICT to use for each glyph.
	 * 
	 * @param fdSelect
	 */
	public void setFDSelect(int[] fdSelect) {
		this.fdSelect = fdSelect;
	}

	/**
	 * @return charstrings and subrs - used by PS to OTF converter
	 */
	@Override
	public Map getCharStrings() {
		return this.charStrings;
	}
}
