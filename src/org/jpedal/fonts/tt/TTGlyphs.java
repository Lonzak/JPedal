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
 * TTGlyphs.java
 * ---------------
 */
package org.jpedal.fonts.tt;

import java.util.Map;

import org.jpedal.PdfDecoder;
import org.jpedal.fonts.FontMappings;
import org.jpedal.fonts.StandardFonts;
import org.jpedal.fonts.glyph.GlyphFactory;
import org.jpedal.fonts.glyph.PdfGlyph;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.objects.FontData;
import org.jpedal.utils.LogWriter;

public class TTGlyphs extends PdfJavaGlyphs {

	private static final long serialVersionUID = -8151179255804850800L;

	protected boolean hasGIDtoCID;

	protected int[] CIDToGIDMap;

	float[] FontBBox = new float[] { 0f, 0f, 1000f, 1000f };

	private String[] diffTable = null;

	boolean isCorrupted = false;

	private CMAP currentCMAP;
	private Post currentPost;
	private Glyf currentGlyf;
	private Hmtx currentHmtx;
	private Hhea currentHhea;

	private FontFile2 fontTable;

	// private Head currentHead;
	// private Name currentName;
	// private Maxp currentMapx;
	private Loca currentLoca;

	// private Hhea currentHhea;

	private CFF currentCFF;

	// int glyphCount=0;

	// assume TT and set to OTF further down
	int type = StandardFonts.TRUETYPE;

	private int unitsPerEm;

	private boolean hasCFF;

	private boolean isCID;

	/**
	 * used by non type3 font
	 */
	@Override
	public PdfGlyph getEmbeddedGlyph(GlyphFactory factory, String glyph, float[][] Trm, int rawInt, String displayValue, float currentWidth,
			String key) {

		int id = rawInt;
		if (this.hasGIDtoCID) rawInt = this.CIDToGIDMap[rawInt];

		/** flush cache if needed */
		if ((this.lastTrm[0][0] != Trm[0][0]) | (this.lastTrm[1][0] != Trm[1][0]) | (this.lastTrm[0][1] != Trm[0][1])
				| (this.lastTrm[1][1] != Trm[1][1])) {
			this.lastTrm = Trm;
			flush();
		}

		// either calculate the glyph to draw or reuse if alreasy drawn
		PdfGlyph transformedGlyph2 = getEmbeddedCachedShape(id);

		if (transformedGlyph2 == null) {

			// use CMAP to get actual glyph ID
			int idx = rawInt;

			if ((!this.isCID || !isIdentity()) && this.currentCMAP != null) idx = this.currentCMAP.convertIndexToCharacterCode(glyph, rawInt,
					this.remapFont, this.isSubsetted, this.diffTable);

			// if no value use post to lookup
			if (idx < 1) idx = this.currentPost.convertGlyphToCharacterCode(glyph);

			// shape to draw onto
			try {
				if (this.hasCFF) {

					transformedGlyph2 = this.currentCFF.getCFFGlyph(factory, glyph, Trm, idx, displayValue, currentWidth, key);

					// set raw width to use for scaling
					if (transformedGlyph2 != null) transformedGlyph2.setWidth(getUnscaledWidth(glyph, rawInt, displayValue, false));

					if (transformedGlyph2 != null) transformedGlyph2.setID(rawInt);

				}
				else transformedGlyph2 = getTTGlyph(idx, glyph, rawInt, displayValue, currentWidth);
			}
			catch (Exception e) {
				// noinspection UnusedAssignment
				transformedGlyph2 = null;

				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());

			}

			// save so we can reuse if it occurs again in this TJ command
			setEmbeddedCachedShape(id, transformedGlyph2);
		}

		return transformedGlyph2;
	}

	/*
	 * creates glyph from truetype font commands
	 */
	public PdfGlyph getTTGlyph(int idx, String glyph, int rawInt, String displayValue, float currentWidth) {

		if (this.isCorrupted) idx = rawInt;

		PdfGlyph currentGlyph = null;
		/**
		 * if(rawInt>glyphCount){ LogWriter.writeLog("Font index out of bounds using defaul t"+glyphCount); rawInt=0;
		 * 
		 * }
		 */

		try {
			// final boolean debug=(rawInt==2465);
			final boolean debug = false;

			if (idx != -1) {
				// move the pointer to the commands
				int p = this.currentGlyf.getCharString(idx);

				if (p != -1) {
					currentGlyph = new TTGlyph(glyph, debug, this.currentGlyf, this.fontTable, this.currentHmtx, idx, (this.unitsPerEm / 1000f),
							this.baseFontName);

					if (debug) System.out.println(">>" + p + ' ' + rawInt + ' ' + displayValue + ' ' + this.baseFontName);
				}
			}

		}
		catch (Exception ee) {
			ee.printStackTrace(System.out);

		}

		// if(glyph.equals("fl"))

		return currentGlyph;
	}

	@Override
	public void setDiffValues(String[] diffTable) {

		this.diffTable = diffTable;
	}

	@Override
	public void setEncodingToUse(boolean hasEncoding, int fontEncoding, boolean isSubstituted, boolean isCIDFont) {

		if (this.currentCMAP != null) {
			if (this.isCorrupted) this.currentCMAP.setEncodingToUse(hasEncoding, fontEncoding, true, isCIDFont);
			else this.currentCMAP.setEncodingToUse(hasEncoding, fontEncoding, isSubstituted, isCIDFont);
		}
	}

	@Override
	public int getConvertedGlyph(int idx) {

		if (this.currentCMAP == null) return idx;
		else return this.currentCMAP.convertIndexToCharacterCode(null, idx, false, false, this.diffTable);
	}

	/**
	 * @return charstrings and subrs - used by PS to OTF converter
	 */
	@Override
	public Map getCharStrings() {

		if (this.currentCMAP != null) {
			return this.currentCMAP.buildCharStringTable();
		}
		else {
			return this.currentGlyf.buildCharStringTable(StandardFonts.MAC);
		}
	}

	/*
	 * creates glyph from truetype font commands
	 */
	@Override
	public float getTTWidth(String glyph, int rawInt, String displayValue, boolean TTstreamisCID) {

		// use CMAP if not CID
		int idx = rawInt;

		float width = 0;

		try {
			if ((!TTstreamisCID)) idx = this.currentCMAP.convertIndexToCharacterCode(glyph, rawInt, this.remapFont, this.isSubsetted, this.diffTable);

			// if no value use post to lookup
			if (idx < 1) idx = this.currentPost.convertGlyphToCharacterCode(glyph);

			// if(idx!=-1)
			width = this.currentHmtx.getWidth(idx);

		}
		catch (Exception e) {

		}

		return width;
	}

	/*
	 * creates glyph from truetype font commands
	 */
	private float getUnscaledWidth(String glyph, int rawInt, String displayValue, boolean TTstreamisCID) {

		// use CMAP if not CID
		int idx = rawInt;

		float width = 0;

		try {
			if ((!TTstreamisCID)) idx = this.currentCMAP.convertIndexToCharacterCode(glyph, rawInt, this.remapFont, this.isSubsetted, this.diffTable);

			// if no value use post to lookup
			if (idx < 1) idx = this.currentPost.convertGlyphToCharacterCode(glyph);

			// if(idx!=-1)
			width = this.currentHmtx.getUnscaledWidth(idx);

		}
		catch (Exception e) {

		}

		return width;
	}

	@Override
	public void setGIDtoCID(int[] cidToGIDMap) {

		this.hasGIDtoCID = true;
		this.CIDToGIDMap = cidToGIDMap;
	}

	/**
	 * @return name of font or all fonts if TTC NAME will be LOWERCASE to avoid issues of capitalisation when used for lookup - if no name, will default to null
	 * 
	 * Mode is PdfDecoder.SUBSTITUTE_* CONSTANT. RuntimeException will be thrown on invalid value
	 */
	public static String[] readFontNames(FontData fontData, int mode) {

		/** setup read the table locations */
		FontFile2 currentFontFile = new FontFile2(fontData);

		// get type
		// int fontType=currentFontFile.getType();

		int fontCount = currentFontFile.getFontCount();

		String[] fontNames = new String[fontCount];

		/** read tables for names */
		for (int i = 0; i < fontCount; i++) {

			currentFontFile.setSelectedFontIndex(i);

			Name currentName = new Name(currentFontFile);

			String name;

			if (mode == PdfDecoder.SUBSTITUTE_FONT_USING_POSTSCRIPT_NAME) {
				name = currentName.getString(Name.POSTSCRIPT_NAME);
			}
			else
				if (mode == PdfDecoder.SUBSTITUTE_FONT_USING_FAMILY_NAME) {
					name = currentName.getString(Name.FONT_FAMILY_NAME);
				}
				else
					if (mode == PdfDecoder.SUBSTITUTE_FONT_USING_FULL_FONT_NAME) {
						name = currentName.getString(Name.FULL_FONT_NAME);
					}
					else { // tell user if invalid
						throw new RuntimeException("Unsupported mode " + mode + ". Unable to resolve font names");
					}

			if (name == null) {
				fontNames[i] = null;
			}
			else {
				fontNames[i] = name.toLowerCase();
			}
		}

		if (fontData != null) fontData.close();

		return fontNames;
	}

	/**
	 * Add font details to Map so we can access later
	 */
	public static void addStringValues(FontData fontData, Map fontDetails) {

		/** setup read the table locations */
		FontFile2 currentFontFile = new FontFile2(fontData);

		// get type
		// int fontType=currentFontFile.getType();

		int fontCount = currentFontFile.getFontCount();

		/** read tables for names */
		for (int i = 0; i < fontCount; i++) {

			currentFontFile.setSelectedFontIndex(i);

			Name currentName = new Name(currentFontFile);

			Map stringValues = currentName.getStrings();

			if (stringValues != null) {
				for (Object o : stringValues.keySet()) {
					Integer currentKey = (Integer) o;

					int keyInt = currentKey.intValue();
					if (keyInt < Name.stringNames.length) fontDetails.put(Name.stringNames[currentKey.intValue()], stringValues.get(currentKey));
				}
			}
		}

		if (fontData != null) fontData.close();
	}

	@Override
	public int readEmbeddedFont(boolean TTstreamisCID, byte[] fontDataAsArray, FontData fontData) {

		FontFile2 currentFontFile;

		this.isCID = TTstreamisCID;

		/** setup read the table locations */
		if (fontDataAsArray != null) currentFontFile = new FontFile2(fontDataAsArray);
		else currentFontFile = new FontFile2(fontData);

		// select font if TTC
		// does nothing if TT
		if (FontMappings.fontSubstitutionFontID == null) {
			currentFontFile.setPointer(0);
		}
		else {
			Integer fontID = FontMappings.fontSubstitutionFontID.get(this.fontName.toLowerCase());

			if (fontID != null) currentFontFile.setPointer(fontID.intValue());
			else currentFontFile.setPointer(0);
		}

		/** read tables */
		Head currentHead = new Head(currentFontFile);

		this.currentPost = new Post(currentFontFile);

		// currentName=new Name(currentFontFile);

		Maxp currentMaxp = new Maxp(currentFontFile);
		this.glyphCount = currentMaxp.getGlyphCount();
		this.currentLoca = new Loca(currentFontFile, this.glyphCount, currentHead.getFormat());

		this.isCorrupted = this.currentLoca.isCorrupted();

		this.currentGlyf = new Glyf(currentFontFile, this.glyphCount, this.currentLoca.getIndices());

		this.currentCFF = new CFF(currentFontFile, this.isCID);

		this.hasCFF = this.currentCFF.hasCFFData();
		if (this.hasCFF) this.type = StandardFonts.OPENTYPE;

		// currentCvt=new Cvt(currentFontFile);

		this.currentHhea = new Hhea(currentFontFile);

		this.FontBBox = currentHead.getFontBBox();

		this.currentHmtx = new Hmtx(currentFontFile, this.glyphCount, this.currentHhea.getNumberOfHMetrics(), (int) this.FontBBox[3]);

		// not all files have CMAPs
		// if(!TTstreamisCID){
		int startPointer = currentFontFile.selectTable(FontFile2.CMAP);

		if (startPointer != 0) this.currentCMAP = new CMAP(currentFontFile, startPointer, this.currentGlyf);

		// }

		this.unitsPerEm = currentHead.getUnitsPerEm();

		this.fontTable = new FontFile2(this.currentGlyf.getTableData(), true);

		if (fontData != null) fontData.close();

		return this.type;
	}

	@Override
	public float[] getFontBoundingBox() {
		return this.FontBBox;
	}

	@Override
	public int getType() {
		return this.type;
	}

	// flag if Loca broken so we need to try and Substitute
	@Override
	public boolean isCorrupted() {
		return this.isCorrupted;
	}

	@Override
	public void setCorrupted(boolean corrupt) {
		this.isCorrupted = corrupt;
	}

	@Override
	public Table getTable(int type) {

		Table table;
		switch (type) {
			case FontFile2.LOCA:
				table = this.currentLoca;
				break;

			case FontFile2.CMAP:
				table = this.currentCMAP;
				break;

			case FontFile2.HHEA:
				table = this.currentHhea;
				break;

			case FontFile2.HMTX:
				table = this.currentHmtx;
				break;

			default:
				throw new RuntimeException("table not yet added to getTable)");
		}

		return table;
	}
}
