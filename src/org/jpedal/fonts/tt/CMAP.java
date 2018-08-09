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
 * CMAP.java
 * ---------------
 */
package org.jpedal.fonts.tt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.fonts.StandardFonts;
import org.jpedal.utils.LogWriter;

public class CMAP extends Table {

	private static final long serialVersionUID = 649534827911949122L;

	protected int[][] glyphIndexToChar;

	private int[] glyphToIndex;

	// flag 6 and use if not able to map elsewhere
	private boolean hasSix = false;

	// flag 4
	private boolean hasFormat4 = false;
	private boolean hasFormat6 = false;

	private int lastFormat4Found = -1;

	// used by format 6
	private int firstCode = -1;
	private int entryCount = -1;

	/** used to 'guess' wrongly encoded fonts */
	private int winScore = 0, macScore = 0;

	// used by format 4
	private int segCount = 0;

	/** which type of mapping to use */
	private int fontMapping = 0;

	// used by format 4
	protected int[] endCode;
	protected int[] startCode;
	protected int[] idDelta;
	protected int[] idRangeOffset;
	protected int[] glyphIdArray;
	private int[] f6glyphIdArray;
	private int[] offset;

	// used by Format 12
	int nGroups;
	private int[] startCharCode;
	private int[] endCharCode;
	private int[] startGlyphCode;

	/** CMap format used -1 shows not set */
	protected int[] CMAPformats, CMAPlength, CMAPlang, CMAPsegCount, CMAPsearchRange, CMAPentrySelector, CMAPrangeShift, CMAPreserved;

	private boolean maybeWinEncoded = false;

	/** Platform-specific ID list */
	// private static String[] PlatformSpecificID={"Roman","Japanese","Traditional Chinese","Korean",
	// "Arabic","Hebrew","Greek","Russian",
	// "RSymbol","Devanagari","Gurmukhi","Gujarati",
	// "Oriya","Bengali","Tamil","Telugu",
	// "Kannada","Malayalam","Sinhalese","Burmese",
	// "Khmer","Thai","Laotian","Georgian",
	// "Armenian","Simplified Chinese","Tibetan","Mongolian",
	// "Geez","Slavic","Vietnamese","Sindhi","(Uninterpreted)"};
	//
	/** Platform-specific ID list */
	// private static String[] PlatformIDName={"Unicode","Macintosh","Reserved","Microsoft"};

	/** shows which encoding used */
	protected int[] platformID;

	private static Map exceptions;

	/** set up differences from Mac Roman */
	static {

		exceptions = new HashMap();

		String[] values = { "notequal", "173", "infinity", "176", "lessequal", "178", "greaterequal", "179", "partialdiff", "182", "summation",
				"183", "product", "184", "pi", "185", "integral", "186", "Omega", "189", "radical", "195", "approxequal", "197", "Delta", "198",
				"lozenge", "215", "Euro", "219", "apple", "240" };
		for (int i = 0; i < values.length; i = i + 2)
			exceptions.put(values[i], values[i + 1]);
	}

	/** which CMAP to use to decode the font */
	private int formatToUse;

	/** encoding to use resolving tt font - should be MAC but not always */
	private int encodingToUse = StandardFonts.MAC;

	private static boolean WINchecked;

	protected int id, numberSubtables;

	protected int[] CMAPsubtables, platformSpecificID;

	public CMAP(FontFile2 currentFontFile, int startPointer, Glyf currentGlyf) {

		final boolean debug = false;

		if (debug) System.out.println("CMAP " + this);

		// LogWriter.writeMethod("{readCMAPTable}", 0);

		// read 'cmap' table
		if (startPointer == 0) {
			if (LogWriter.isOutput()) LogWriter.writeLog("No CMAP table found");
		}
		else {

			this.id = currentFontFile.getNextUint16();// id
			this.numberSubtables = currentFontFile.getNextUint16();

			// read the subtables
			this.CMAPsubtables = new int[this.numberSubtables];
			this.platformID = new int[this.numberSubtables];
			this.platformSpecificID = new int[this.numberSubtables];
			this.CMAPformats = new int[this.numberSubtables];
			this.CMAPsearchRange = new int[this.numberSubtables];
			this.CMAPentrySelector = new int[this.numberSubtables];
			this.CMAPrangeShift = new int[this.numberSubtables];
			this.CMAPreserved = new int[this.numberSubtables];
			this.CMAPsegCount = new int[this.numberSubtables];
			this.CMAPlength = new int[this.numberSubtables];
			this.CMAPlang = new int[this.numberSubtables];
			this.glyphIndexToChar = new int[this.numberSubtables][256];

			this.glyphToIndex = new int[256];

			for (int i = 0; i < this.numberSubtables; i++) {

				this.platformID[i] = currentFontFile.getNextUint16();
				this.platformSpecificID[i] = currentFontFile.getNextUint16();
				this.CMAPsubtables[i] = currentFontFile.getNextUint32();

				if (debug) System.out.println("IDs platformID=" + this.platformID[i] + " platformSpecificID=" + this.platformSpecificID[i]
						+ " CMAPsubtables=" + this.CMAPsubtables[i]);
				// System.out.println(PlatformID[platformID[i]]+" "+PlatformSpecificID[platformSpecificID[i]]+CMAPsubtables[i]);

			}

			// now read each subtable
			for (int j = 0; j < this.numberSubtables; j++) {
				currentFontFile.selectTable(FontFile2.CMAP);
				currentFontFile.skip(this.CMAPsubtables[j]);

				// assume 16 bit format to start
				this.CMAPformats[j] = currentFontFile.getNextUint16();
				this.CMAPlength[j] = currentFontFile.getNextUint16();
				this.CMAPlang[j] = currentFontFile.getNextUint16();// lang

				if (debug) System.out.println(j + " type=" + this.CMAPformats[j] + " length=" + this.CMAPlength[j] + " lang=" + this.CMAPlang[j]);
				// flag if present
				if (this.CMAPformats[j] == 6) this.hasSix = true;

				if (this.CMAPformats[j] == 0 && this.CMAPlength[j] == 262) {

					StandardFonts.checkLoaded(StandardFonts.WIN);
					StandardFonts.checkLoaded(StandardFonts.MAC);

					// populate table and get valid glyphs
					// Map uniqueFontMappings=StandardFonts.getUniqueMappings(); //win or mac only values we check for
					// int total=0;

					boolean isValidOnMac, isValidOnWin;

					for (int glyphNum = 0; glyphNum < 256; glyphNum++) {

						int index = currentFontFile.getNextUint8();
						this.glyphIndexToChar[j][glyphNum] = index;
						this.glyphToIndex[index] = glyphNum;

						/** count to try and guess if wrongly encoded */
						if (index > 0) {// &&(currentGlyf.isPresent(index))){

							isValidOnMac = StandardFonts.isValidMacEncoding(glyphNum);
							isValidOnWin = StandardFonts.isValidWinEncoding(glyphNum);

							// if any different flag it up
							if (isValidOnMac != isValidOnWin) this.maybeWinEncoded = true;

							// System.out.println(Integer.toOctalString(index)+" "+glyphNum+" "+StandardFonts.isValidMacEncoding(glyphNum)+" "+StandardFonts.isValidWinEncoding(glyphNum));
							/**/
							if (isValidOnMac) {
								this.macScore++;
							}// else
								// System.out.println(glyphNum+" not MAC");

							if (isValidOnWin) {
								this.winScore++;
							}// else
								// System.out.println(glyphNum+" now WIN");

							// cumulative WIN or MAC only values found
							// Object uniqueness=uniqueFontMappings.get(new Integer(glyphNum));
							// if(uniqueness!=null){// will give a +1 (mac only) or -1 (win only) to total
							// total=total+((Integer) uniqueness).intValue();
							// //System.out.println(glyphNum+" >>"+((Integer) uniqueness).intValue());
							// }
						}
					}

					// switch to win only if scored several win only values
					// System.out.println("total="+total+" macScore="+macScore+" winScore="+winScore);
					// if(total<-2){
					// macScore=98;
					// winScore=99;
					// }

					// System.out.println("mac="+macScore+" win="+winScore);

				}
				else
					if (this.CMAPformats[j] == 4) {

						// read values
						this.CMAPsegCount[j] = currentFontFile.getNextUint16();
						this.segCount = this.CMAPsegCount[j] / 2;
						this.CMAPsearchRange[j] = currentFontFile.getNextUint16(); // searchrange
						this.CMAPentrySelector[j] = currentFontFile.getNextUint16();// entrySelector
						this.CMAPrangeShift[j] = currentFontFile.getNextUint16();// rangeShift

						// check current format 4 is greater than previous or vice versa and act accordingly
						// because some font files have more than one format 4 subtables with different length
						if (this.hasFormat4) {
							if (this.CMAPlength[this.lastFormat4Found] > this.CMAPlength[j]) {
								this.CMAPlength[j] = this.CMAPlength[this.lastFormat4Found];
								this.CMAPsegCount[j] = this.CMAPsegCount[this.lastFormat4Found];
								this.CMAPsearchRange[j] = this.CMAPsearchRange[this.lastFormat4Found]; // searchrange
								this.CMAPentrySelector[j] = this.CMAPentrySelector[this.lastFormat4Found];// entrySelector
								this.CMAPrangeShift[j] = this.CMAPrangeShift[this.lastFormat4Found];// rangeShift
								continue;
							}
							else
								if (this.CMAPlength[this.lastFormat4Found] < this.CMAPlength[j]) {
									this.CMAPlength[this.lastFormat4Found] = this.CMAPlength[j];
									this.CMAPsegCount[this.lastFormat4Found] = this.CMAPsegCount[j];
									this.CMAPsearchRange[this.lastFormat4Found] = this.CMAPsearchRange[j]; // searchrange
									this.CMAPentrySelector[this.lastFormat4Found] = this.CMAPentrySelector[j];// entrySelector
									this.CMAPrangeShift[this.lastFormat4Found] = this.CMAPrangeShift[j];// rangeShift
								}
						}

						this.lastFormat4Found = j;
						this.hasFormat4 = true;

						// read tables and initialise size of arrays
						this.endCode = new int[this.segCount];
						for (int i = 0; i < this.segCount; i++)
							this.endCode[i] = currentFontFile.getNextUint16();

						this.CMAPreserved[j] = currentFontFile.getNextUint16(); // reserved (should be zero)

						this.startCode = new int[this.segCount];
						for (int i = 0; i < this.segCount; i++)
							this.startCode[i] = currentFontFile.getNextUint16();

						this.idDelta = new int[this.segCount];
						for (int i = 0; i < this.segCount; i++)
							this.idDelta[i] = currentFontFile.getNextUint16();

						this.idRangeOffset = new int[this.segCount];
						for (int i = 0; i < this.segCount; i++)
							this.idRangeOffset[i] = currentFontFile.getNextUint16();

						/** create offsets */
						this.offset = new int[this.segCount];
						int diff, cumulative = 0;

						for (int i = 0; i < this.segCount; i++) {

							// System.out.println("seg="+i+" cumulative="+cumulative+
							// " idDelta[i]="+idDelta[i]+" startCode[i]="+startCode[i]+
							// " endCode[i]="+endCode[i]);
							//
							if (this.idDelta[i] == 0) {// && startCode[i]!=endCode[i]){
								this.offset[i] = cumulative;
								diff = 1 + this.endCode[i] - this.startCode[i];

								// fixes bug in mapping theSansOffice tff font
								if (this.startCode[i] == this.endCode[i] && this.idRangeOffset[i] == 0) diff = 0;

								cumulative = cumulative + diff;
							}
						}

						// glyphIdArray at end
						int count = (this.CMAPlength[j] - 16 - (this.segCount * 8)) / 2;

						this.glyphIdArray = new int[count];
						for (int i = 0; i < count; i++) {
							this.glyphIdArray[i] = currentFontFile.getNextUint16();
						}

					}
					else
						if (this.CMAPformats[j] == 6) {
							this.hasFormat6 = true;
							this.firstCode = currentFontFile.getNextUint16();
							this.entryCount = currentFontFile.getNextUint16();

							this.f6glyphIdArray = new int[this.firstCode + this.entryCount];
							for (int jj = 0; jj < this.entryCount; jj++)
								this.f6glyphIdArray[jj + this.firstCode] = currentFontFile.getNextUint16();

						}
						else
							if (this.CMAPformats[j] == 12) {

								currentFontFile.getNextUint16(); // length //not what it says in spec but what I found in file
								currentFontFile.getNextUint32(); // lang

								this.nGroups = currentFontFile.getNextUint32();

								this.startCharCode = new int[this.nGroups];
								this.endCharCode = new int[this.nGroups];
								this.startGlyphCode = new int[this.nGroups];

								for (int ii = 0; ii < this.nGroups; ii++) {

									this.startCharCode[ii] = currentFontFile.getNextUint32();
									this.endCharCode[ii] = currentFontFile.getNextUint32();
									this.startGlyphCode[ii] = currentFontFile.getNextUint32();
								}

							}
							else {
								// System.out.println("Unsupported Format "+CMAPformats[j]);
								// reset to avoid setting
								this.CMAPformats[j] = -1;

							}

				// System.out.println(" <> "+platformID[j]);
				// System.out.println(CMAPformats[j]+" "+platformID[j]+" "+platformSpecificID[j]+" "+PlatformIDName[platformID[j]]);

			}
		}

		/** validate format zero encoding */
		// if(formatFour!=-1)
		// validateMacEncoding(formatZero,formatFour);
	}

	public CMAP() {}

	/** convert raw glyph number to Character code */
	public int convertIndexToCharacterCode(String glyph, int index, boolean remapFont, boolean isSubsetted, String[] diffTable) {

		int index2 = -1, rawIndex = index;
		int format = this.CMAPformats[this.formatToUse];

		final boolean debugMapping = false;// (index==223);

		if (debugMapping) System.out.println(glyph + " fontMapping=" + this.fontMapping + " index=" + index + " encodingToUse=" + this.encodingToUse
				+ ' ' + this.encodingToUse + " WIN=" + StandardFonts.WIN + " MAC=" + StandardFonts.MAC);

		/** convert index if needed */
		if ((this.fontMapping == 1 || (!remapFont && this.fontMapping == 4)) && (glyph != null) && (!"notdef".equals(glyph))) {

			index = StandardFonts.getAdobeMap(glyph);

			if (debugMapping) System.out.println("convert index");

		}
		else
			if (this.fontMapping == 2) {

				StandardFonts.checkLoaded(this.encodingToUse);

				if (this.encodingToUse == StandardFonts.MAC) {

					Object exception = null;
					if (glyph != null) exception = exceptions.get(glyph);

					if (exception == null) {
						if (glyph != null && !isSubsetted) index = StandardFonts.lookupCharacterIndex(glyph, this.encodingToUse);
					}
					else
						if (diffTable == null || diffTable[index] == null) { // not if a diff
							try {
								index = Integer.parseInt(((String) exception));
							}
							catch (Exception e) {
								// tell user and log
								if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
							}
						}
					// win indexed just incase
					if (glyph != null) {
						if (!WINchecked) {
							StandardFonts.checkLoaded(StandardFonts.WIN);
							WINchecked = true;
						}
						index2 = StandardFonts.lookupCharacterIndex(glyph, StandardFonts.WIN);
					}
				}
				else
					if (glyph != null) index = StandardFonts.lookupCharacterIndex(glyph, this.encodingToUse);
			}

		int value = -1;

		// remap if flag set
		if (remapFont && format > 0 && format != 6) index = index + 0xf000;

		// if no cmap use identity
		if (format == 0) {

			// hack
			if (index > 255) index = 0;

			value = this.glyphIndexToChar[this.formatToUse][index];
			if (value == 0 && index2 != -1) value = this.glyphIndexToChar[this.formatToUse][index2];

			// exception found in Itext
			if (rawIndex == 128 && this.endCode != null && "Euro".equals(glyph)) value = getFormat4Value(8364, debugMapping, value);

		}
		else
			if (format == 4) {

				value = getFormat4Value(index, debugMapping, value);

				// hack for odd value in customer file
				if (value == -1) {

					if (index > 0xf000) value = getFormat4Value(index - 0xf000, debugMapping, value);
					else value = getFormat4Value(index + 0xf000, debugMapping, value);

				}
			}
			else
				if (format == 12) {
					value = getFormat12Value(index, debugMapping, value);
				}

		// second attempt if no value found
		if (value == -1 && this.hasSix) {
			index = rawIndex;
			format = 6;
		}

		if (format == 6) {
			
			if(!remapFont){
                index=StandardFonts.lookupCharacterIndex(glyph,encodingToUse);
            }
			
			if (index >= this.f6glyphIdArray.length) value = 0;
			else value = this.f6glyphIdArray[index];
		}

		// System.out.println(value+" format="+format);

		if (debugMapping) System.out.println("returns " + value + ' ' + this);

		return value;
	}

	/**
	 * lookup tables similar to format 4 see https://developer.apple.com/fonts/TTRefMan/RM06/Chap6cmap.html
	 */
	private int getFormat12Value(int index, boolean debugMapping, int value) {

		/**
		 * cycle through tables and then add offset to Glyph start
		 */
		for (int i = 0; i < this.nGroups; i++) {

			if (debugMapping) System.out.println("table=" + i + " start=" + this.startCharCode[i] + ' ' + index + " end=" + this.endCharCode[i]
					+ " glypgStartCode[i]=" + this.startGlyphCode[i]);

			if (this.endCharCode[i] >= index && this.startCharCode[i] <= index) {

				value = this.startGlyphCode[i] + index - this.startCharCode[i];
				i = this.nGroups; // exit loop
			}
		}

		return value;
	}

	private int getFormat4Value(int index, boolean debugMapping, int value) {

		for (int i = 0; i < this.segCount; i++) {

			if (debugMapping) System.out.println("Segtable=" + i + " start=" + this.startCode[i] + ' ' + index + " end=" + this.endCode[i]
					+ " idRangeOffset[i]=" + this.idRangeOffset[i] + " offset[i]=" + this.offset[i] + " idRangeOffset[i]=" + this.idRangeOffset[i]
					+ " idDelta[i]=" + this.idDelta[i]);

			if (this.endCode[i] >= index && this.startCode[i] <= index) {

				int idx;
				if (this.idRangeOffset[i] == 0) {

					if (debugMapping) System.out.println("xxx=" + (this.idDelta[i] + index));

					value = (this.idDelta[i] + index) % 65536;

					i = this.segCount;
				}
				else {

					idx = this.offset[i] + (index - this.startCode[i]);
					value = this.glyphIdArray[idx];

					if (debugMapping) System.out.println("value=" + value + " idx=" + idx + " glyphIdArrays=" + this.glyphIdArray[0] + ' '
							+ this.glyphIdArray[1] + ' ' + this.glyphIdArray[2] + " offset[i]=" + this.offset[i] + " index=" + index + " startCode["
							+ i + "]=" + this.startCode[i] + " i=" + i);

					i = this.segCount;

				}
			}
		}

		return value;
	}

	/**
	 * work out correct CMAP table to use.
	 */
	public void setEncodingToUse(boolean hasEncoding, int fontEncoding, boolean isSubstituted, boolean isCID) {

		final boolean encodingDebug = false;

		if (encodingDebug) System.out.println(this + "hasEncoding=" + hasEncoding + " fontEncoding=" + fontEncoding + " isSubstituted="
				+ isSubstituted + " isCID=" + isCID + "  macScore=" + this.macScore);

		this.formatToUse = -1;

		int count = this.platformID.length;

		// this code changes encoding to WIN if that appears to be encoding used in spite of being MAC
		if (!isSubstituted && this.macScore < 207) {

			// System.out.println(macScore+" winScore="+winScore+" count="+count+" "+hasEncoding+"  "+fontEncoding+"  "+StandardFonts.WIN);

			if (this.glyphToIndex != null && this.macScore > 90 && !this.maybeWinEncoded) {
				// System.out.println("Its Mac");

			}
			else
				if (this.glyphToIndex != null && this.macScore > 205 && this.glyphToIndex[138] != 0 && this.glyphToIndex[228] == 0) {
					// System.out.println("Its Mac");
				}
				else {

					if (count > 0 && this.winScore > this.macScore) this.encodingToUse = StandardFonts.WIN;

					if (this.macScore > 80 && hasEncoding && fontEncoding == StandardFonts.WIN && this.winScore >= this.macScore) this.encodingToUse = StandardFonts.WIN;
				}
		}

		// System.out.println(isSubstituted+" "+encodingToUse+" WIN="+StandardFonts.WIN+" MAC="+StandardFonts.MAC);

		if (encodingDebug) System.out.println("macScore=" + this.macScore + " winScore=" + this.winScore + " count=" + count + " isSubstituted="
				+ isSubstituted);

		/** case 1 */
		for (int i = 0; i < count; i++) {
			if (encodingDebug) System.out.println("Maps=" + this.platformID[i] + ' ' + this.CMAPformats[i]);
			if ((this.platformID[i] == 3) && (this.CMAPformats[i] == 1 || this.CMAPformats[i] == 0)) {
				this.formatToUse = i;
				this.fontMapping = 1;
				i = count;
				// StandardFonts.loadAdobeMap();

				if (encodingDebug) System.out.println("case1");

			}
		}

		/** case 2 */
		boolean wasCase2 = false;
		if (this.formatToUse == -1 && this.CMAPformats[0] != 12 && ((this.macScore > 0 && this.winScore > 0) || this.CMAPformats.length == 1)
				&& !isCID && (!isSubstituted || (this.CMAPformats.length == 1 && this.CMAPformats[0] == 0))) {

			for (int i = 0; i < count; i++) {
				if (this.platformID[i] == 1 && this.CMAPformats[i] == 0) {
					this.formatToUse = i;
					if (hasEncoding || fontEncoding == StandardFonts.WIN) this.fontMapping = 2;
					else this.fontMapping = 3;

					i = count;

					wasCase2 = true;
				}
			}

			if (encodingDebug) System.out.println("case2 fontMapping=" + this.fontMapping + ' ' + this.glyphIndexToChar[this.formatToUse][175] + ' '
					+ this.glyphIndexToChar[this.formatToUse][223]);
		}

		/** case 3 - no MAC cmap in other ranges and substituting font */
		boolean wasCase3 = false;
		if (this.formatToUse == -1) {
			for (int i = 0; i < count; i++) {
				// if((platformID[i]==1)&&(CMAPformats[i]==6)){ Altered 20050921 to fix problem with Doucsign page
				if ((this.CMAPformats[i] == 6)) {
					this.formatToUse = i;
					if ((!hasEncoding) && (fontEncoding == StandardFonts.WIN)) {
						this.fontMapping = 2;
						StandardFonts.checkLoaded(StandardFonts.MAC);
					}
					else this.fontMapping = 6;

					wasCase3 = true;
					i = count;
				}
			}

			if (encodingDebug) System.out.println("case3");
		}

		/** case 4 - no simple maps or prefer to last 1 */
		/** last check uses fl glyph and sticks to case 1 if found */
		if ((this.formatToUse == -1) || wasCase3
				|| (wasCase2 && !(this.glyphIndexToChar[this.formatToUse][223] != 0 && getFormat4Value(223, false, 0) == 0))) {// ||
																																// glyphIndexToChar[formatToUse][223]==0){//&&((!isSubstituted)|(isCID))){
			// if((formatToUse==-1)){
			for (int i = 0; i < count; i++) {
				if ((this.CMAPformats[i] == 4)) {
					this.formatToUse = i;
					this.fontMapping = 4;

					i = count;

				}
			}

			if (encodingDebug) System.out.println("case4 fontMapping=" + this.fontMapping + " formatToUse=" + this.formatToUse);
		}

		/** case 5 - type12 */
		if (this.formatToUse == -1) {
			for (int i = 0; i < count; i++) {
				if ((this.CMAPformats[i] == 12)) {
					this.formatToUse = i;
					if ((!hasEncoding) && (fontEncoding == StandardFonts.WIN)) {
						this.fontMapping = 2;
						StandardFonts.checkLoaded(StandardFonts.MAC);
					}
					else this.fontMapping = 12;

					i = count;
				}
			}

			if (encodingDebug) System.out.println("case3");
		}

		// System.out.println(formatToUse+" " +fontMapping);

		if (fontEncoding == StandardFonts.ZAPF) {
			this.fontMapping = 2;

			if (encodingDebug) System.out.println("Zapf");
		}

		// further tests
		if (this.encodingToUse == StandardFonts.WIN && this.macScore == this.winScore && this.glyphIndexToChar[this.formatToUse][146] == 0
				&& this.glyphIndexToChar[this.formatToUse][213] != 0) { // quoteright
			this.encodingToUse = StandardFonts.MAC;
		}

		// System.out.println("enc="+encodingToUse+" "+glyphIndexToChar[formatToUse][138]+" "+glyphIndexToChar[formatToUse][228]+" mac="+macScore+" win="+winScore);

		if (this.encodingToUse == StandardFonts.WIN && this.macScore == this.winScore && this.glyphIndexToChar[this.formatToUse][228] == 0
				&& this.glyphIndexToChar[this.formatToUse][138] != 0) { // adieresis
			this.encodingToUse = StandardFonts.MAC;
		}
	}

	/** turn type 0 table into a list of glyph */
	public Map buildCharStringTable() {

		Map glyfValues = new HashMap();
		// for(int i : glyphToIndex){

		// if(i>0){
		// glyfValues.put(glyphToIndex[i],i);
		// //System.out.println("i=" + i + " " + StandardFonts.getUnicodeChar(encodingToUse, i));
		// }
		// }
		if (this.hasFormat4) {
			ArrayList<Integer> list4 = new ArrayList<Integer>();
			for (int z = 0; z < this.segCount; z++) {
				int total = this.endCode[z] - this.startCode[z] + 1;
				for (int q = 0; q < total; q++) {
					list4.add(this.startCode[z] + q);
				}
			}
			for (Integer i : list4) {
				glyfValues.put(i, getFormat4Value(i, false, 0));
			}
		}
		else
			if (this.hasFormat6) {
				for (int z = 0; z < this.entryCount; z++) {
					// System.out.println(firstCode+z+" ==> "+f6glyphIdArray[firstCode+z]);
					glyfValues.put(this.firstCode + z, this.f6glyphIdArray[this.firstCode + z]);
				}
			}
			else {
				for (int z = 0; z < this.glyphToIndex.length; z++) {
					if (this.glyphToIndex[z] > 0) {
						glyfValues.put(this.glyphToIndex[z], z);
					}
				}
			}

		return glyfValues;
	}

	/**
	 * returns hasFormat4 Method added in order to skip unicode lookup in cmapwriter
	 * 
	 * @return hasFormat4
	 * */
	public boolean hasFormat4() {
		return this.hasFormat4;
	}

}
