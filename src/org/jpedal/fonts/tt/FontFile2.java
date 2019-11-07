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
 * FontFile2.java
 * ---------------
 */
package org.jpedal.fonts.tt;

import java.io.Serializable;
import java.util.ArrayList;

import org.jpedal.fonts.objects.FontData;

/**
 * @author markee
 * 
 *         To change the template for this generated type comment go to Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FontFile2 implements Serializable {

	private static final long serialVersionUID = -3097990864237320960L;

	public final static int HEAD = 0;
	public final static int MAXP = 1;
	public final static int CMAP = 2;
	public final static int LOCA = 3;
	public final static int GLYF = 4;
	public final static int HHEA = 5;
	public final static int HMTX = 6;
	public final static int NAME = 7;
	public final static int POST = 8;
	public final static int CVT = 9;
	public final static int FPGM = 10;
	public final static int HDMX = 11;
	public final static int KERN = 12;
	public final static int OS2 = 13;
	public final static int PREP = 14;
	public final static int DSIG = 15;

	public final static int CFF = 16;
	public final static int GSUB = 17;
	public final static int BASE = 18;
	public final static int EBDT = 19;
	public final static int EBLC = 20;
	public final static int GASP = 21;
	public final static int VHEA = 22;
	public final static int VMTX = 23;
	public final static int GDEF = 24;
	public final static int JSTF = 25;
	public final static int LTSH = 26;
	public final static int PCLT = 27;
	public final static int VDMX = 28;
	public final static int BSLN = 29;
	public final static int MORT = 30;
	public final static int FDSC = 31;
	public final static int FFTM = 32;
	public final static int GPOS = 33;
	public final static int FEAT = 34;
	public final static int JUST = 35;
	public final static int PROP = 36;
	public final static int LCCL = 37;
	public final static int Zapf = 38;

	protected int tableCount = 39;

	// location of tables
	protected int checksums[][];
	protected int tables[][];
	protected int tableLength[][];

	/** holds embedded font */
	private FontData fontDataAsObject = null;

	private byte[] fontDataAsArray = null;

	private boolean useArray = true;

	protected ArrayList<String> tableList = new ArrayList<String>(32);

	/** current location in fontDataArray */
	private int pointer = 0;

	public static final int OPENTYPE = 1;
	public static final int TRUETYPE = 2;
	public static final int TTC = 3;

	/** subtypes used in conversion */
	public static final int PS = 10;
	public static final int TTF = 11;
	protected int subType = PS;

	protected int type = TRUETYPE;

	// if several fonts, selects which font
	public int currentFontID = 0;
	private int fontCount = 1;

	// defaults are for OTF write
	protected int numTables = 11, searchRange = 128, entrySelector = 3, rangeShift = 48;

	public FontFile2(FontData data) {

		this.useArray = false;

		this.fontDataAsObject = data;

		readHeader();
	}

	public FontFile2(byte[] data) {

		this.useArray = true;

		this.fontDataAsArray = data;

		readHeader();
	}

	public FontFile2(byte[] data, boolean ignoreHeaders) {

		this.useArray = true;

		this.fontDataAsArray = data;

		if (!ignoreHeaders) readHeader();
	}

	public FontFile2() {}

	/**
	 * set selected font as a number in TTC ie if 4 fonts, use 0,1,2,3 if less than fontCount. Otherwise does nothing
	 */
	public void setSelectedFontIndex(int currentFontID) {

		if (currentFontID < this.fontCount) this.currentFontID = currentFontID;
	}

	/** read the table offsets */
	final private void readHeader() {

		/** code to read the data at start of file */
		// scalertype
		int scalerType = getNextUint32();

		if (scalerType == 1330926671) // starts OTTF
		this.type = OPENTYPE;
		else
			if (scalerType == 1953784678) // ttc
			this.type = TTC;

		if (this.type == TTC) {

			getNextUint32(); // version
			this.fontCount = getNextUint32();

			// location of tables
			this.checksums = new int[this.tableCount][this.fontCount];
			this.tables = new int[this.tableCount][this.fontCount];
			this.tableLength = new int[this.tableCount][this.fontCount];

			int[] fontOffsets = new int[this.fontCount];

			for (int currentFont = 0; currentFont < this.fontCount; currentFont++) {

				this.currentFontID = currentFont;

				int fontStart = getNextUint32();
				fontOffsets[currentFont] = fontStart;
			}

			for (int currentFont = 0; currentFont < this.fontCount; currentFont++) {

				this.currentFontID = currentFont; // choose this font

				this.pointer = fontOffsets[currentFont];

				getNextUint32(); // scalerType

				readTablesForFont();
			}

			// back to default
			this.currentFontID = 0;

		}
		else { // otf or ttf

			// location of tables
			this.checksums = new int[this.tableCount][1];
			this.tables = new int[this.tableCount][1];
			this.tableLength = new int[this.tableCount][1];

			readTablesForFont();
		}
	}

	private void readTablesForFont() {

		this.numTables = getNextUint16(); // tables in the file
		this.searchRange = getNextUint16(); // searchRange
		this.entrySelector = getNextUint16(); // entrySelector
		this.rangeShift = getNextUint16(); // rangeShift

		String tag;
		int checksum, offset, length, id;

		for (int l = 0; l < this.numTables; l++) {
			// read table
			tag = getNextUint32AsTag();
			checksum = getNextUint32(); // checksum
			offset = getNextUint32();
			length = getNextUint32();

			this.tableList.add(tag);

			id = getTableID(tag);

			if (id != -1) {
				this.checksums[id][this.currentFontID] = checksum;
				this.tables[id][this.currentFontID] = offset;
				this.tableLength[id][this.currentFontID] = length;
			}
		}
	}

	protected static int getTableID(String tag) {

		int id = -1;

		if (tag.equals("maxp")) id = MAXP;
		else
			if (tag.equals("head")) id = HEAD;
			else
				if (tag.equals("cmap")) id = CMAP;
				else
					if (tag.equals("loca")) {
						id = LOCA;
					}
					else
						if (tag.equals("glyf")) {
							id = GLYF;
						}
						else
							if (tag.equals("hhea")) {
								id = HHEA;
							}
							else
								if (tag.equals("hmtx")) {
									id = HMTX;
								}
								else
									if (tag.equals("name")) {
										id = NAME;
									}
									else
										if (tag.equals("post")) {
											id = POST;
										}
										else
											if (tag.equals("cvt ")) {
												id = CVT;
											}
											else
												if (tag.equals("fpgm")) {
													id = FPGM;
												}
												else
													if (tag.equals("hdmx")) {
														id = HDMX;
													}
													else
														if (tag.equals("kern")) {
															id = KERN;
														}
														else
															if (tag.equals("OS/2")) {
																id = OS2;
															}
															else
																if (tag.equals("prep")) {
																	id = PREP;
																}
																else
																	if (tag.equals("DSIG")) {
																		id = DSIG;
																	}
																	else
																		if (tag.equals("BASE")) {
																			id = BASE;
																		}
																		else
																			if (tag.equals("CFF ")) {
																				id = CFF;
																			}
																			else
																				if (tag.equals("GSUB")) {
																					id = GSUB;
																				}
																				else
																					if (tag.equals("EBDT")) {
																						id = EBDT;
																					}
																					else
																						if (tag.equals("EBLC")) {
																							id = EBLC;
																						}
																						else
																							if (tag.equals("gasp")) {
																								id = GASP;
																							}
																							else
																								if (tag.equals("vhea")) {
																									id = VHEA;
																								}
																								else
																									if (tag.equals("vmtx")) {
																										id = VMTX;
																									}
																									else
																										if (tag.equals("GDEF")) {
																											id = GDEF;
																										}
																										else
																											if (tag.equals("JSTF")) {
																												id = JSTF;
																											}
																											else
																												if (tag.equals("LTSH")) {
																													id = LTSH;
																												}
																												else
																													if (tag.equals("PCLT")) {
																														id = PCLT;
																													}
																													else
																														if (tag.equals("VDMX")) {
																															id = VDMX;
																														}
																														else
																															if (tag.equals("mort")) {
																																id = MORT;
																															}
																															else
																																if (tag.equals("bsln")) {
																																	id = BSLN;
																																}
																																else
																																	if (tag.equals("fdsc")) {
																																		id = FDSC;
																																	}
																																	else
																																		if (tag.equals("FFTM")) {
																																			id = FFTM;
																																		}
																																		else
																																			if (tag.equals("GPOS")) {
																																				id = GPOS;
																																			}
																																			else
																																				if (tag.equals("feat")) {
																																					id = FEAT;
																																				}
																																				else
																																					if (tag.equals("just")) {
																																						id = JUST;
																																					}
																																					else
																																						if (tag.equals("prop")) {
																																							id = PROP;
																																						}
																																						else
																																							if (tag.equals("LCCL")) {
																																								id = LCCL;
																																							}
																																							else
																																								if (tag.equals("Zapf")) {
																																									id = Zapf;
																																								}
																																								else {
																																									// System.out.println("No tag for "+tag);
																																								}
		return id;
	}

	/**
	 * choose a table and move to start. Return 0 if not present
	 */
	public int selectTable(int tableID) {
		this.pointer = this.tables[tableID][this.currentFontID];

		return this.pointer;
	}

	/** get table size */
	public int getTableSize(int tableID) {
		return this.tableLength[tableID][this.currentFontID];
	}

	/** get table size */
	public int getTableStart(int tableID) {
		return this.tables[tableID][this.currentFontID];
	}

	/** return a uint32 */
	final public int getNextUint32() {

		int returnValue = 0, nextValue;

		for (int i = 0; i < 4; i++) {

			if (this.useArray) {
				if (this.pointer < this.fontDataAsArray.length) nextValue = this.fontDataAsArray[this.pointer] & 255;
				else nextValue = 0;
			}
			else nextValue = this.fontDataAsObject.getByte(this.pointer) & 255;

			returnValue = returnValue + ((nextValue << (8 * (3 - i))));

			this.pointer++;
		}

		return returnValue;
	}

	/** return a uint64 */
	final public int getNextUint64() {

		int returnValue = 0, nextValue;

		for (int i = 0; i < 8; i++) {

			if (this.useArray) nextValue = this.fontDataAsArray[this.pointer];
			else nextValue = this.fontDataAsObject.getByte(this.pointer);

			if (nextValue < 0) nextValue = 256 + nextValue;

			returnValue = returnValue + (nextValue << (8 * (7 - i)));

			this.pointer++;
		}

		return returnValue;
	}

	/** set pointer to location in font file */
	final public void setPointer(int p) {
		this.pointer = p;
	}

	/** get length of table */
	final public int getOffset(int tableID) {
		return this.tableLength[tableID][this.currentFontID];
	}

	/** get start of table */
	final public int getTable(int tableID) {
		return this.tables[tableID][this.currentFontID];
	}

	/** get pointer to location in font file */
	final public int getPointer() {
		return this.pointer;
	}

	/** return a uint32 */
	final public String getNextUint32AsTag() {

		StringBuilder returnValue = new StringBuilder();

		char c;

		for (int i = 0; i < 4; i++) {

			if (this.useArray) c = (char) this.fontDataAsArray[this.pointer];
			else c = (char) this.fontDataAsObject.getByte(this.pointer);

			returnValue.append(c);

			this.pointer++;
		}

		return returnValue.toString();
	}

	/** return a uint16 */
	final public int getNextUint16() {

		int returnValue = 0, nextValue;

		for (int i = 0; i < 2; i++) {

			if (this.useArray) nextValue = this.fontDataAsArray[this.pointer] & 255;
			else nextValue = this.fontDataAsObject.getByte(this.pointer) & 255;

			returnValue = returnValue + (nextValue << (8 * (1 - i)));

			this.pointer++;
		}

		return returnValue;
	}

	/** return a short */
	final public short getShort() {

		int returnValue = 0, nextValue;

		for (int i = 0; i < 2; i++) {

			if (this.useArray) nextValue = this.fontDataAsArray[this.pointer];
			else nextValue = this.fontDataAsObject.getByte(this.pointer);

			returnValue = returnValue + (nextValue << (8 * (1 - i)));

			this.pointer++;
		}

		return (short) returnValue;
	}

	/** return a uint8 */
	final public int getNextUint8() {

		int nextValue;

		if (this.useArray) nextValue = this.fontDataAsArray[this.pointer] & 255;
		else nextValue = this.fontDataAsObject.getByte(this.pointer) & 255;

		// if(returnValue<0)
		// returnValue=256+returnValue;

		this.pointer++;

		return nextValue;
	}

	/** return a uint8 */
	final public int getNextint8() {

		int nextValue;

		if (this.useArray) nextValue = this.fontDataAsArray[this.pointer];
		else nextValue = this.fontDataAsObject.getByte(this.pointer);

		// if(returnValue<0)
		// returnValue=256+returnValue;

		this.pointer++;

		return nextValue;
	}

	/**
	 * move forward a certain amount relative
	 */
	public void skip(int i) {
		this.pointer = this.pointer + i;
	}

	/**
	 * return a short
	 */
	public short getFWord() {
		int returnValue = 0, nextValue;

		for (int i = 0; i < 2; i++) {

			if (this.useArray) nextValue = this.fontDataAsArray[this.pointer] & 255;
			else nextValue = this.fontDataAsObject.getByte(this.pointer) & 255;

			returnValue = returnValue + (nextValue << (8 * (1 - i)));

			this.pointer++;
		}

		return (short) returnValue;
	}

	public short getNextInt16() {
		int returnValue = 0, nextValue;

		for (int i = 0; i < 2; i++) {

			if (this.useArray) nextValue = this.fontDataAsArray[this.pointer] & 255;
			else nextValue = this.fontDataAsObject.getByte(this.pointer) & 255;

			returnValue = returnValue + (nextValue << (8 * (1 - i)));

			this.pointer++;
		}

		return (short) returnValue;
	}

	public short getNextSignedInt16() {
		int returnValue = 0, nextValue;

		for (int i = 0; i < 2; i++) {

			if (this.useArray) nextValue = this.fontDataAsArray[this.pointer] & 255;
			else nextValue = this.fontDataAsObject.getByte(this.pointer) & 255;

			returnValue = returnValue + (nextValue << (8 * (1 - i)));
			this.pointer++;
		}

		return (short) (returnValue);
	}

	public short readUFWord() {
		int returnValue = 0, nextValue;

		for (int i = 0; i < 2; i++) {

			if (this.useArray) nextValue = this.fontDataAsArray[this.pointer] & 255;
			else nextValue = this.fontDataAsObject.getByte(this.pointer) & 255;

			returnValue = returnValue + (nextValue << (8 * (1 - i)));

			this.pointer++;
		}

		return (short) returnValue;
	}

	/**
	 * get 16 bit signed fixed point
	 */
	public float getFixed() {

		int number, dec;

		if (this.useArray) number = ((this.fontDataAsArray[this.pointer] & 0xff) * 256) + (this.fontDataAsArray[this.pointer + 1] & 0xff);
		else number = ((this.fontDataAsObject.getByte(this.pointer) & 0xff) * 256) + (this.fontDataAsObject.getByte(this.pointer + 1) & 0xff);

		if (number > 32768) number = number - 65536;

		this.pointer = this.pointer + 2;

		if (this.useArray) dec = ((this.fontDataAsArray[this.pointer] & 0xff) * 256) + (this.fontDataAsArray[this.pointer + 1] & 0xff);
		else dec = ((this.fontDataAsObject.getByte(this.pointer) & 0xff) * 256) + (this.fontDataAsObject.getByte(this.pointer + 1) & 0xff);

		this.pointer = this.pointer + 2;

		return (number + (dec / 65536f));
	}

	/**
	 * get a pascal string
	 */
	public String getString() {
		int length;

		// catch bug in odd file
		if (this.useArray && this.pointer == this.fontDataAsArray.length) return "";

		if (this.useArray) length = this.fontDataAsArray[this.pointer] & 255;
		else length = this.fontDataAsObject.getByte(this.pointer) & 255;

		char[] chars = new char[length];
		// StringBuilder value=new StringBuilder();
		// value.setLength(length);

		this.pointer++;

		for (int i = 0; i < length; i++) {
			int nextChar;

			if (this.useArray) nextChar = this.fontDataAsArray[this.pointer] & 255;
			else nextChar = this.fontDataAsObject.getByte(this.pointer) & 255;

			this.pointer++;

			// value.append((char)nextChar);
			// value.setCharAt(i,(char)nextChar);
			chars[i] = (char) nextChar;
			// allow for error
			if (this.useArray && this.pointer >= this.fontDataAsArray.length) i = length;

		}
		return String.copyValueOf(chars);
		// return value.toString();
	}

	/**
	 * get a pascal string
	 */
	public byte[] getStringBytes() {
		int length;

		// catch bug in odd file
		if (this.useArray && this.pointer == this.fontDataAsArray.length) return new byte[1];

		if (this.useArray) length = this.fontDataAsArray[this.pointer] & 255;
		else length = this.fontDataAsObject.getByte(this.pointer) & 255;

		byte[] value = new byte[length];

		this.pointer++;

		for (int i = 0; i < length; i++) {
			byte nextChar;

			if (this.useArray) nextChar = this.fontDataAsArray[this.pointer];
			else nextChar = this.fontDataAsObject.getByte(this.pointer);

			this.pointer++;

			// value.append((char)nextChar);
			value[i] = nextChar;

			// allow for error
			if (this.useArray && this.pointer >= this.fontDataAsArray.length) i = length;

		}
		return value;
	}

	public float getF2Dot14() {

		int firstValue;

		if (this.useArray) firstValue = ((this.fontDataAsArray[this.pointer] & 0xff) << 8) + (this.fontDataAsArray[this.pointer + 1] & 0xff);
		else firstValue = ((this.fontDataAsObject.getByte(this.pointer) & 0xff) << 8) + (this.fontDataAsObject.getByte(this.pointer + 1) & 0xff);

		this.pointer = this.pointer + 2;

		if (firstValue == 49152) {
			return -1.0f;
		}
		else
			if (firstValue == 16384) {
				return 1.0f;
			}
			else {
				return (firstValue - (2 * (firstValue & 32768))) / 16384f;
			}
	}

	public byte[] readBytes(int startPointer, int length) {

		if (this.useArray) {
			byte[] block = new byte[length];
			System.arraycopy(this.fontDataAsArray, startPointer, block, 0, length);
			return block;
		}
		else return this.fontDataAsObject.getBytes(startPointer, length);
	}

	public byte[] getTableBytes(int tableID) {

		int startPointer = this.tables[tableID][this.currentFontID];
		int length = this.tableLength[tableID][this.currentFontID];

		if (this.useArray) {
			byte[] block = new byte[length];
			System.arraycopy(this.fontDataAsArray, startPointer, block, 0, length);
			return block;
		}
		else return this.fontDataAsObject.getBytes(startPointer, length);
	}

	/**
	 * return fonttype
	 */
	public int getType() {
		return this.type;
	}

	// number of fonts - 1 for Open/True, can be more for TTC
	public int getFontCount() {
		return this.fontCount;
	}

	/**
	 * used to test if table too short so need to stop reading
	 * 
	 */
	public boolean hasValuesLeft() {

		int size;
		if (this.useArray) size = this.fontDataAsArray.length;
		else size = this.fontDataAsObject.length();

		return this.pointer < size;
	}

	/**
	 * used to see how many bytes left to avoid exception if figure wrong
	 * 
	 */
	public int getBytesLeft() {

		int size;
		if (this.useArray) size = this.fontDataAsArray.length;
		else size = this.fontDataAsObject.length();

		return size - this.pointer;
	}

}
