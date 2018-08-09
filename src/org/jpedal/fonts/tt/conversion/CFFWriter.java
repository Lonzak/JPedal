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
 * CFFWriter.java
 * ---------------
 */
package org.jpedal.fonts.tt.conversion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.fonts.StandardFonts;
import org.jpedal.fonts.Type1;
import org.jpedal.fonts.Type1C;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.utils.LogWriter;

public class CFFWriter extends Type1 implements FontTableWriter {

	private static final long serialVersionUID = -4172447980016197351L;

	final private static boolean debugTopDictOffsets = false;

	private String name;
	private byte[][] subrs;
	final private String[] glyphNames;
	private byte[][] charstrings;
	private int[] charstringXDisplacement, charstringYDisplacement;
	private byte[] header, nameIndex, topDictIndex, globalSubrIndex, encodings, charsets, charStringsIndex, privateDict, localSubrIndex, stringIndex;
	final private ArrayList<String> strings = new ArrayList<String>();
	private int[] widthX, widthY, lsbX, lsbY;
	private int defaultWidthX, nominalWidthX;
	private ArrayList<CharstringElement> currentCharString;
	private int currentCharStringID;
	private float[] bbox = new float[4];

	// Values for processing flex sections
	private boolean inFlex = false;
	private CharstringElement currentFlexCommand;
	private boolean firstArgsAdded = false;

	// Values for dealing with incorrect em square
	private double emSquareSize = 1000;
	private double scale = 1;
	private boolean inSeac = false;

	public CFFWriter(PdfJavaGlyphs glyphs, String name) {

		this.glyphs = glyphs;
		this.name = name;

		// Fetch charstrings and subrs
		Map charStringSegments = glyphs.getCharStrings();

		// Count subrs and chars
		Object[] keys = charStringSegments.keySet().toArray();
		Arrays.sort(keys);
		int maxSubrNum = 0, maxSubrLen = 0, charCount = 0;
		for (int i = 0; i < charStringSegments.size(); i++) {
			String key = (String) keys[i];
			if (key.startsWith("subrs")) {
				int num = Integer.parseInt(key.replaceAll("[^0-9]", ""));
				if (num > maxSubrNum) {
					maxSubrNum = num;
				}
				int len = ((byte[]) charStringSegments.get(key)).length;
				if (len > maxSubrLen) {
					maxSubrLen = len;
				}
			}
			else {
				charCount++;
			}
		}

		// Move to array
		this.subrs = new byte[maxSubrNum + 1][];
		this.glyphNames = new String[charCount];
		this.charstrings = new byte[charCount][];
		this.charstringXDisplacement = new int[charCount];
		this.charstringYDisplacement = new int[charCount];
		charCount = 0;
		for (int i = 0; i < charStringSegments.size(); i++) {
			String key = (String) keys[i];
			Object obj = charStringSegments.get(key);
			byte[] cs = ((byte[]) obj);
			if (key.startsWith("subrs")) {
				int num = Integer.parseInt(key.replaceAll("[^0-9]", ""));
				this.subrs[num] = cs;
			}
			else {
				this.glyphNames[charCount] = key;
				this.charstrings[charCount] = cs;
				charCount++;
			}
		}

		convertCharstrings();
	}

	/**
	 * Convert the charstrings from type 1 to type 2.
	 */
	private void convertCharstrings() {

		/**
		 * Convert instructions
		 */
		try {

			this.widthX = new int[this.charstrings.length];
			this.widthY = new int[this.charstrings.length];
			this.lsbX = new int[this.charstrings.length];
			this.lsbY = new int[this.charstrings.length];

			// Perform initial conversion
			byte[][] newCharstrings = new byte[this.charstrings.length][];
			for (int charstringID = 0; charstringID < this.charstrings.length; charstringID++) {
				newCharstrings[charstringID] = convertCharstring(this.charstrings[charstringID], charstringID);
			}

			// Check em square size and reconvert while scaling if necessary
			if (this.bbox[2] - this.bbox[0] > 1100) {

				// Calculate em size and scaling to apply
				this.emSquareSize = (this.bbox[2] - this.bbox[0]);
				this.scale = 1d / (this.emSquareSize / 1000d);

				// Reset displacements & bbox
				this.charstringXDisplacement = new int[this.charstringXDisplacement.length];
				this.charstringYDisplacement = new int[this.charstringYDisplacement.length];
				this.bbox = new float[4];

				// Re-convert charstrings now scale is set
				for (int charstringID = 0; charstringID < this.charstrings.length; charstringID++) {
					newCharstrings[charstringID] = convertCharstring(this.charstrings[charstringID], charstringID);
				}
				this.charstrings = newCharstrings;

			}
			else {
				this.charstrings = newCharstrings;
			}

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		/**
		 * Calculate values for defaultWidthX and nominalWidthX & add widths to start of charstrings
		 */

		// Find counts for each value
		HashMap<Integer, Integer> valueCount = new HashMap<Integer, Integer>();
		for (int i = 0; i < this.charstrings.length; i++) {
			Integer count = valueCount.get(Integer.valueOf(this.widthX[i]));
			if (count == null) {
				count = 1;
			}
			else {
				count = count.intValue() + 1;
			}
			valueCount.put(this.widthX[i], count);
		}

		// Find most common value to use as defaultWidthX
		Object[] values = valueCount.keySet().toArray();
		int maxCount = 0;
		this.defaultWidthX = 0;
		for (Object value : values) {
			int count = valueCount.get(value);
			if (count > maxCount) {
				maxCount = count;
				this.defaultWidthX = (Integer) value;
			}
		}

		// Find average for nominalWidthX
		int total = 0;
		int count = 0;
		for (Object value : values) {
			if ((Integer) value != this.defaultWidthX) {
				count++;
				total += (Integer) value;
			}
		}
		if (count != 0) {
			this.nominalWidthX = total / count;
		}
		else {
			this.nominalWidthX = 0;
		}

		// Blank default widths and update other widths
		for (int i = 0; i < this.widthX.length; i++) {
			if (this.widthX[i] == this.defaultWidthX) {
				this.widthX[i] = Integer.MIN_VALUE;
			}
			else {
				this.widthX[i] = this.widthX[i] - this.nominalWidthX;
			}
		}

		// Append widths to start of charstrings (but not if it's 0 as this signifies default)
		for (int i = 0; i < this.widthX.length; i++) {
			if (this.widthX[i] != Integer.MIN_VALUE) {
				byte[] width = FontWriter.setCharstringType2Number(this.widthX[i]);
				byte[] newCharstring = new byte[width.length + this.charstrings[i].length];
				System.arraycopy(width, 0, newCharstring, 0, width.length);
				System.arraycopy(this.charstrings[i], 0, newCharstring, width.length, this.charstrings[i].length);
				this.charstrings[i] = newCharstring;
			}
		}

		// Check all charstrings end in endchar and append if not
		for (int i = 0; i < this.charstrings.length; i++) {
			if (this.charstrings[i][this.charstrings[i].length - 1] != 14) {
				byte[] newCharstring = new byte[this.charstrings[i].length + 1];
				System.arraycopy(this.charstrings[i], 0, newCharstring, 0, this.charstrings[i].length);
				newCharstring[newCharstring.length - 1] = 14;
				this.charstrings[i] = newCharstring;
			}
		}
	}

	/**
	 * Convert a charstring from type 1 to type 2.
	 * 
	 * @param charstring
	 *            The charstring to convert
	 * @param charstringID
	 *            The number of the charstring to convert
	 * @return The converted charstring
	 */
	private byte[] convertCharstring(byte[] charstring, int charstringID) {

		int[] cs = new int[charstring.length];
		for (int i = 0; i < charstring.length; i++) {
			cs[i] = charstring[i];
			if (cs[i] < 0) {
				cs[i] += 256;
			}
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		this.currentCharString = new ArrayList<CharstringElement>();
		this.currentCharStringID = charstringID;

		// Convert to CharstringElements
		CharstringElement element;
		for (int i = 0; i < cs.length; i += element.getLength()) {
			element = new CharstringElement(cs, i);
		}

		// Rescale commands if necessary
		if (this.emSquareSize != 1000 && !this.inSeac) {
			for (CharstringElement e : this.currentCharString) {
				e.scale();
			}
			this.widthX[charstringID] = (int) (this.scale * this.widthX[charstringID]);
			this.widthY[charstringID] = (int) (this.scale * this.widthY[charstringID]);
			this.lsbX[charstringID] = (int) (this.scale * this.lsbX[charstringID]);
			this.lsbY[charstringID] = (int) (this.scale * this.lsbY[charstringID]);
		}

		// Calculate and store displacement and bbox
		for (CharstringElement e : this.currentCharString) {
			int[] d = e.getDisplacement();
			this.charstringXDisplacement[charstringID] += d[0];
			this.charstringYDisplacement[charstringID] += d[1];
			this.bbox[0] = this.charstringXDisplacement[charstringID] < this.bbox[0] ? this.charstringXDisplacement[charstringID] : this.bbox[0];
			this.bbox[1] = this.charstringYDisplacement[charstringID] < this.bbox[1] ? this.charstringYDisplacement[charstringID] : this.bbox[1];
			this.bbox[2] = this.charstringXDisplacement[charstringID] > this.bbox[2] ? this.charstringXDisplacement[charstringID] : this.bbox[2];
			this.bbox[3] = this.charstringYDisplacement[charstringID] > this.bbox[3] ? this.charstringYDisplacement[charstringID] : this.bbox[3];
		}

		// Print for debug
		// System.out.println("Charstring "+charstringID);
		// for (CharstringElement currentElement : currentCharString) {
		// byte[] e = currentElement.getType2Bytes();
		// for (byte b : e) {
		// String bin = Integer.toBinaryString(b);
		// int addZeros = 8 - bin.length();
		// for (int k=0; k<addZeros; k++)
		// System.out.print("0");
		// if (addZeros < 0)
		// bin = bin.substring(-addZeros);
		// int val = b;
		// if (val < 0)
		// val += 256;
		// System.out.println(bin+"\t"+val);
		// }
		// System.out.println(currentElement);
		// }
		// System.out.println();
		// System.out.println();

		// Convert to type 2
		try {
			for (CharstringElement currentElement : this.currentCharString) {
				bos.write(currentElement.getType2Bytes());
			}
		}
		catch (IOException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		return bos.toByteArray();
	}

	/**
	 * Returns a string ID for a given string. It first checks in the standard strings, and if it isn't there places it in the array of custom
	 * strings.
	 * 
	 * @param text
	 *            String to fetch ID for
	 * @return String ID
	 */
	public int getSIDForString(String text) {
		for (int i = 0; i < Type1C.type1CStdStrings.length; i++) {
			if (text.equals(Type1C.type1CStdStrings[i])) {
				return i;
			}
		}

		for (int i = 0; i < this.strings.size(); i++) {
			if (text.equals(this.strings.get(i))) {
				return 391 + i;
			}
		}

		this.strings.add(text);
		return 390 + this.strings.size();
	}

	/**
	 * Retrieve the final whole table.
	 * 
	 * @return the new CFF table
	 * @throws IOException
	 */
	@Override
	public byte[] writeTable() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		// Set as empty array for top Dict generator
		this.topDictIndex = this.globalSubrIndex = this.stringIndex = this.encodings = this.charsets = this.charStringsIndex = this.privateDict = this.localSubrIndex = new byte[] {};

		/**
		 * Generate values
		 */
		this.header = new byte[] { FontWriter.setNextUint8(1), // major
				FontWriter.setNextUint8(0), // minor
				FontWriter.setNextUint8(4), // headerSize
				FontWriter.setNextUint8(2) }; // offSize
		this.nameIndex = createIndex(new byte[][] { this.name.getBytes() });

		if (debugTopDictOffsets) {
			System.out.println("Generating first top dict...");
		}

		this.topDictIndex = createIndex(new byte[][] { createTopDict() });
		this.globalSubrIndex = createIndex(new byte[][] {});
		// Global Subr INDEX -Probably don't need
		this.encodings = createEncodings();
		this.charsets = createCharsets();
		// FDSelect //CIDFonts only
		this.charStringsIndex = createIndex(this.charstrings); // per-font //Might need to reorder, although .notdef does seem to be first
		// Font DICT INDEX //per-font, CIDFonts only
		this.privateDict = createPrivateDict(); // per-font
		// localSubrIndex = createIndex(subrs); //per-font or per-Private DICT for CIDFonts - Subr's are currently inlined
		// Copyright and Trademark Notices
		this.stringIndex = createIndex(createStrings()); // Generate last as strings are added as required by other sections

		// Regenerate private dict until length is stable
		byte[] lastPrivateDict;
		do {
			lastPrivateDict = new byte[this.privateDict.length];
			System.arraycopy(this.privateDict, 0, lastPrivateDict, 0, this.privateDict.length);
			this.privateDict = createPrivateDict();
		}
		while (!Arrays.equals(this.privateDict, lastPrivateDict));

		// Regenerate top dict index until length is stable
		byte[] lastTopDictIndex;
		do {
			lastTopDictIndex = new byte[this.topDictIndex.length];
			System.arraycopy(this.topDictIndex, 0, lastTopDictIndex, 0, this.topDictIndex.length);
			if (debugTopDictOffsets) {
				System.out.println("Current length is " + lastTopDictIndex.length + ". Testing against new...");
			}
			this.topDictIndex = createIndex(new byte[][] { createTopDict() });
		}
		while (!Arrays.equals(lastTopDictIndex, this.topDictIndex));

		if (debugTopDictOffsets) {
			System.out.println("Length matches, offsets are now correct.");
		}

		/**
		 * Write out
		 */
		bos.write(this.header);
		bos.write(this.nameIndex);
		bos.write(this.topDictIndex);
		bos.write(this.stringIndex);
		bos.write(this.globalSubrIndex);
		bos.write(this.encodings);
		bos.write(this.charsets);
		bos.write(this.charStringsIndex);
		bos.write(this.privateDict);
		// bos.write(localSubrIndex); //Subr's are currently inlined so this is not needed

		return bos.toByteArray();
	}

	/**
	 * Create an index as a byte array
	 * 
	 * @param data
	 *            Array of byte array data chunks
	 * @return Byte array of index
	 * @throws IOException
	 *             if cannot write data
	 */
	private static byte[] createIndex(byte[][] data) throws IOException {

		int count = data.length;

		// Check for empty index
		if (count == 0) {
			return new byte[] { 0, 0 };
		}

		// Generate offsets
		int[] offsets = new int[count + 1];
		offsets[0] = 1;
		for (int i = 1; i < count + 1; i++) {
			byte[] cs = data[i - 1];
			if (cs != null) {
				offsets[i] = offsets[i - 1] + cs.length;
			}
			else {
				offsets[i] = offsets[i - 1];
			}
		}
		// Generate offSize
		int offSize = getOffsizeForMaxVal(offsets[count]);

		int len = 3 + (offSize * offsets.length) + offsets[count];
		ByteArrayOutputStream bos = new ByteArrayOutputStream(len);

		// Write out
		bos.write(FontWriter.setNextUint16(count)); // count
		bos.write(FontWriter.setNextUint8(offSize)); // offSize
		for (int offset : offsets) {
			bos.write(FontWriter.setUintAsBytes(offset, offSize)); // offsets
		}
		for (byte[] item : data) {
			if (item != null) {
				bos.write(item); // data
			}
		}

		return bos.toByteArray();
	}

	/**
	 * Create the Top Dict.
	 * 
	 * @return a byte array representing the Top DICT
	 * @throws IOException
	 *             is ByteArrayOutputStream breaks
	 */
	private byte[] createTopDict() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		// Version 0
		bos.write(FontWriter.set1cNumber(getSIDForString("1")));
		bos.write((byte) 0);

		// Notice 1
		if (this.copyright != null) {
			bos.write(FontWriter.set1cNumber(getSIDForString(this.copyright)));
			bos.write((byte) 1);
		}

		// FontBBox 5
		bos.write(FontWriter.set1cNumber((int) this.bbox[0]));
		bos.write(FontWriter.set1cNumber((int) this.bbox[1]));
		bos.write(FontWriter.set1cNumber((int) this.bbox[2]));
		bos.write(FontWriter.set1cNumber((int) this.bbox[3]));
		bos.write((byte) 5);

		// //FontMatrix
		// //Commented out as mac doesn't support FontMatrix :(
		// //Reduce font size if em square is incorrect
		// if (emSquareSize != 1000) {
		// bos.write(FontWriter.set1cRealNumber(1d/emSquareSize));
		// bos.write(FontWriter.set1cNumber(0));
		// bos.write(FontWriter.set1cNumber(0));
		// bos.write(FontWriter.set1cRealNumber(1d/emSquareSize));
		// bos.write(FontWriter.set1cNumber(0));
		// bos.write(FontWriter.set1cNumber(0));
		// bos.write(new byte[]{12,7});
		// }

		// encoding 16
		int loc = this.header.length + this.nameIndex.length + this.topDictIndex.length + this.stringIndex.length + this.globalSubrIndex.length;
		if (this.encodings.length != 0) {
			bos.write(FontWriter.set1cNumber(loc));
			if (debugTopDictOffsets) {
				System.out.println("Encoding offset: " + loc);
			}
			bos.write((byte) 16);
		}

		// charset 15
		loc += this.encodings.length;
		bos.write(FontWriter.set1cNumber(loc));
		if (debugTopDictOffsets) {
			System.out.println("Charset offset: " + loc);
		}
		bos.write((byte) 15);

		// charstrings 17
		loc += this.charsets.length;
		bos.write(FontWriter.set1cNumber(loc));
		if (debugTopDictOffsets) {
			System.out.println("Charstrings offset: " + loc);
		}
		bos.write((byte) 17);

		// private 18
		loc += this.charStringsIndex.length;
		bos.write(FontWriter.set1cNumber(this.privateDict.length));
		bos.write(FontWriter.set1cNumber(loc));
		if (debugTopDictOffsets) {
			System.out.println("Private offset: " + loc);
		}
		bos.write((byte) 18);

		return bos.toByteArray();
	}

	/**
	 * Create the Strings ready to place into an index
	 * 
	 * @return The strings as an array of byte arrays
	 */
	private byte[][] createStrings() {
		byte[][] result = new byte[this.strings.size()][];

		for (int i = 0; i < this.strings.size(); i++) {
			result[i] = this.strings.get(i).getBytes();
		}

		return result;
	}

	/**
	 * Create charsets table.
	 * 
	 * @return byte array representing the Charsets
	 */
	private byte[] createCharsets() {

		// Make sure .notdef removed
		String[] names = null;
		for (int i = 0; i < this.glyphNames.length; i++) {
			if (".notdef".equals(this.glyphNames[i])) {
				names = new String[this.glyphNames.length - 1];
				System.arraycopy(this.glyphNames, 0, names, 0, i);
				System.arraycopy(this.glyphNames, i + 1, names, i, names.length - i);
			}
		}

		if (names == null) {
			names = this.glyphNames;
		}

		// Create array for result
		byte[] result = new byte[(names.length * 2) + 1];

		// Leave first byte blank for format 0, then fill rest of array with 2-byte SIDs
		for (int i = 0; i < names.length; i++) {
			byte[] sid = FontWriter.setUintAsBytes(getSIDForString(names[i]), 2);

			result[1 + (i * 2)] = sid[0];
			result[2 + (i * 2)] = sid[1];
		}

		return result;
	}

	/**
	 * Create Encodings table
	 * 
	 * @return byte array representing the Encodings
	 */
	private static byte[] createEncodings() {
		return new byte[0];
	}

	/**
	 * Create the Private dictionary
	 * 
	 * @return byte array representing the Private dict
	 * @throws IOException
	 *             if ByteOutputStream breaks
	 */
	private byte[] createPrivateDict() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		// //Subrs 19 Subr's are currently inlined so this isn't needed
		// bos.write(FontWriter.set1cNumber(privateDict.length));
		// bos.write((byte)19);

		// defaultWidthX 20
		bos.write(FontWriter.set1cNumber(this.defaultWidthX));
		bos.write((byte) 20);

		// nominalWidthX 21
		bos.write(FontWriter.set1cNumber(this.nominalWidthX));
		bos.write((byte) 21);

		// bos.write(FontWriter.set1cNumber(-24));
		// bos.write(FontWriter.set1cNumber(24));
		// bos.write(FontWriter.set1cNumber(670));
		// bos.write(FontWriter.set1cNumber(17));
		// bos.write(FontWriter.set1cNumber(-189));
		// bos.write(FontWriter.set1cNumber(13));
		// bos.write((byte)6); //BlueValues
		//
		// bos.write(FontWriter.set1cNumber(326));
		// bos.write(FontWriter.set1cNumber(5));
		// bos.write(FontWriter.set1cNumber(-512));
		// bos.write(FontWriter.set1cNumber(2));
		// bos.write((byte)7); //OtherBlues
		//
		// bos.write(FontWriter.set1cNumber(1));
		// bos.write(new byte[]{12,10}); //BlueShift
		//
		// bos.write(FontWriter.set1cNumber(63));
		// bos.write((byte)10); //StdHW
		//
		// bos.write(FontWriter.set1cNumber(11));
		// bos.write(FontWriter.set1cNumber(44));
		// bos.write(FontWriter.set1cNumber(2));
		// bos.write(FontWriter.set1cNumber(2));
		// bos.write(FontWriter.set1cNumber(4));
		// bos.write(FontWriter.set1cNumber(5));
		// bos.write(FontWriter.set1cNumber(42));
		// bos.write(FontWriter.set1cNumber(9));
		// bos.write(FontWriter.set1cNumber(18));
		// bos.write(FontWriter.set1cNumber(14));
		// bos.write(FontWriter.set1cNumber(123));
		// bos.write(FontWriter.set1cNumber(4));
		// bos.write(new byte[]{12,12}); //StemSnapH
		//
		// bos.write(FontWriter.set1cNumber(74));
		// bos.write((byte)11); //StdVW
		//
		// bos.write(FontWriter.set1cNumber(70));
		// bos.write(FontWriter.set1cNumber(4));
		// bos.write(FontWriter.set1cNumber(6));
		// bos.write(FontWriter.set1cNumber(12));
		// bos.write(new byte[]{12,13}); //StemSnapV

		return bos.toByteArray();
	}

	/**
	 * Calculate the offSize required to encode a value
	 * 
	 * @param i
	 *            Max value to encode
	 * @return Number of bytes required
	 */
	private static byte getOffsizeForMaxVal(int i) {
		byte result = 1;
		while (i > 256) {
			result++;
			i = i / 256;
		}

		return result;
	}

	@Override
	public int getIntValue(int i) {
		return -1;
	}

	/**
	 * Return a list of the names of the glyphs in this font.
	 * 
	 * @return List of glyph names
	 */
	public String[] getGlyphList() {
		return this.glyphNames;
	}

	/**
	 * Return the widths of all of the glyphs of this font.
	 * 
	 * @return List of glyph widths
	 */
	public int[] getWidths() {
		int[] widths = new int[this.widthX.length];

		for (int i = 0; i < this.widthX.length; i++) {
			if (this.widthX[i] == Integer.MIN_VALUE) {
				widths[i] = this.defaultWidthX;
			}
			else {
				widths[i] = this.widthX[i] + this.nominalWidthX;
			}
		}

		return widths;
	}

	/**
	 * Return the left side bearings of all of the glyphs of this font.
	 * 
	 * @return List of left side bearings.
	 */
	public int[] getBearings() {
		return this.lsbX;
	}

	/**
	 * Return a bounding box calculated from the outlines.
	 * 
	 * @return the calculated bbox
	 */
	public float[] getBBox() {
		return this.bbox;
	}

	/**
	 * Return the size of the em square calculated from the outlines.
	 * 
	 * @return the calculated em square size
	 */
	public double getEmSquareSize() {
		return this.emSquareSize;
	}

	private class CharstringElement {
		private boolean isCommand = true;
		private String commandName;
		private int numberValue;
		private int length = 1;
		private ArrayList<CharstringElement> args = new ArrayList<CharstringElement>();
		final private boolean isResult;
		private CharstringElement parent;

		/**
		 * Constructor used for generating an integer parameter.
		 * 
		 * @param number
		 *            The number this element should represent
		 */
		public CharstringElement(int number) {
			this.isResult = false;
			this.isCommand = false;
			this.numberValue = number;
		}

		/**
		 * Constructor used for generating placeholder result elements.
		 * 
		 * @param parent
		 *            The element this is a result of
		 */
		public CharstringElement(CharstringElement parent) {
			this.isResult = true;
			this.isCommand = false;
			this.parent = parent;
			CFFWriter.this.currentCharString.add(this);
		}

		/**
		 * Normal constructor used when converting from Type 1 stream.
		 * 
		 * @param charstring
		 *            byte array to copy from
		 * @param pos
		 *            starting position for this element
		 */
		public CharstringElement(int[] charstring, int pos) {
			this.isResult = false;
			CFFWriter.this.currentCharString.add(this);

			int b = charstring[pos];

			if (b >= 32 && b <= 246) { // Single byte number

				this.numberValue = b - 139;
				this.isCommand = false;

			}
			else
				if ((b >= 247 && b <= 250) || (b >= 251 && b <= 254)) { // Two byte number

					if (b < 251) {
						this.numberValue = ((b - 247) * 256) + charstring[pos + 1] + 108;
					}
					else {
						this.numberValue = -((b - 251) * 256) - charstring[pos + 1] - 108;
					}

					this.isCommand = false;
					this.length = 2;

				}
				else {

					boolean mergePrevious = false;

					switch (b) {
						case 1: // hstem
							this.commandName = "hstem";
							claimArguments(2, true, true);
							break;
						case 3: // vstem
							this.commandName = "vstem";
							claimArguments(2, true, true);
							break;
						case 4: // vmoveto
							this.commandName = "vmoveto";
							claimArguments(1, true, true);

							// If in a flex section channel arg and 0 into current flex command
							if (CFFWriter.this.inFlex) {
								// If second pair found add to the first and store back
								if (CFFWriter.this.currentFlexCommand.args.size() == 2 && !CFFWriter.this.firstArgsAdded) {
									int arg0 = CFFWriter.this.currentFlexCommand.args.get(0).numberValue;
									int arg1 = this.args.get(0).numberValue + CFFWriter.this.currentFlexCommand.args.get(1).numberValue;
									CFFWriter.this.currentFlexCommand.args.clear();
									CFFWriter.this.currentFlexCommand.args.add(new CharstringElement(arg0));
									CFFWriter.this.currentFlexCommand.args.add(new CharstringElement(arg1));
									CFFWriter.this.firstArgsAdded = true;
								}
								else {
									CFFWriter.this.currentFlexCommand.args.add(new CharstringElement(0));
									CFFWriter.this.currentFlexCommand.args.add(this.args.get(0));
								}
								this.commandName = "";
							}
							break;
						case 5: // rlineto
							this.commandName = "rlineto";
							claimArguments(2, true, true);
							mergePrevious = true;
							break;
						case 6: // hlineto
							this.commandName = "hlineto";
							claimArguments(1, true, true);
							break;
						case 7: // vlineto
							this.commandName = "vlineto";
							claimArguments(1, true, true);
							break;
						case 8: // rrcurveto
							this.commandName = "rrcurveto";
							claimArguments(6, true, true);
							mergePrevious = true;
							break;
						case 9: // closepath
							this.commandName = "closepath";
							claimArguments(0, false, true);
							break;
						case 10: // callsubr
							this.commandName = "callsubr";
							claimArguments(1, false, false);

							int subrNumber = this.args.get(0).numberValue;

							// Handle starting a section of flex code
							if (!CFFWriter.this.inFlex && subrNumber == 1) {
								// Repurpose this as flex command and set flag for processing following commands
								this.args.clear();
								this.commandName = "flex";
								CFFWriter.this.currentFlexCommand = this;
								CFFWriter.this.inFlex = true;
							}

							// Handle subr calls during flex sections
							if (CFFWriter.this.inFlex && subrNumber >= 0 && subrNumber <= 2) {

								// Handle endind flex section
								if (subrNumber == 0) {
									claimArguments(3, false, false);
									if (this.args.size() >= 4) {
										CFFWriter.this.currentFlexCommand.args.add(this.args.get(3));
									}
									else {
										CFFWriter.this.currentFlexCommand.args.add(new CharstringElement(0));
									}
									CFFWriter.this.inFlex = false;
									CFFWriter.this.firstArgsAdded = false;
								}

								// Handle other cases
							}
							else {

								byte[] rawSubr = CFFWriter.this.subrs[subrNumber];

								// Deal with top byte being negative
								int[] subr = new int[rawSubr.length];
								for (int i = 0; i < rawSubr.length; i++) {
									subr[i] = rawSubr[i];
									if (subr[i] < 0) {
										subr[i] += 256;
									}
								}

								// Convert to CharstringElements
								CharstringElement element;
								for (int i = 0; i < subr.length; i += element.length) {
									element = new CharstringElement(subr, i);
								}
							}
							break;
						case 11: // return
							this.commandName = "return";
							break;
						case 12: // 2 byte command
							this.length = 2;
							switch (charstring[pos + 1]) {
								case 0: // dotsection
									this.commandName = "dotsection";
									claimArguments(0, false, true);
									break;
								case 1: // vstem3
									this.commandName = "vstem3";
									claimArguments(6, true, true);
									break;
								case 2: // hstem3
									this.commandName = "hstem3";
									claimArguments(6, true, true);
									break;
								case 6: // seac
									this.commandName = "seac";
									claimArguments(5, true, true);
									break;
								case 7: // sbw
									this.commandName = "sbw";
									claimArguments(4, true, true);
									CFFWriter.this.lsbX[CFFWriter.this.currentCharStringID] = this.args.get(0).evaluate();
									CFFWriter.this.lsbY[CFFWriter.this.currentCharStringID] = this.args.get(1).evaluate();
									CFFWriter.this.widthX[CFFWriter.this.currentCharStringID] = this.args.get(2).evaluate();
									CFFWriter.this.widthY[CFFWriter.this.currentCharStringID] = this.args.get(3).evaluate();

									// repurpose as rmoveto
									if (CFFWriter.this.lsbX[CFFWriter.this.currentCharStringID] != 0) {
										this.commandName = "rmoveto";
										this.args.clear();
										this.args.add(new CharstringElement(CFFWriter.this.lsbX[CFFWriter.this.currentCharStringID]));
										this.args.add(new CharstringElement(CFFWriter.this.lsbY[CFFWriter.this.currentCharStringID]));
									}
									break;
								case 12: // div
									this.commandName = "div";
									claimArguments(2, false, false);
									new CharstringElement(this);
									break;
								case 16: // callothersubr
									this.commandName = "callothersubr";
									claimArguments(2, false, false);
									if (this.args.size() > 1) {
										int count = this.args.get(1).numberValue;
										boolean foundEnough = claimArguments(count, false, false);

										if (!foundEnough) {
											CFFWriter.this.currentCharString.remove(this);
											return;
										}

										// Place arguments back on stack
										for (int i = 0; i < count; i++) {
											new CharstringElement(this.args.get((1 + count) - i).numberValue);
										}
									}
									break;
								case 17: // pop
									this.commandName = "pop";
									new CharstringElement(this);
									break;
								case 33: // setcurrentpoint
									this.commandName = "setcurrentpoint";
									claimArguments(2, true, true);
									break;
								default:
							}
							break;
						case 13: // hsbw
							this.commandName = "hsbw";
							claimArguments(2, true, true);
							CFFWriter.this.lsbX[CFFWriter.this.currentCharStringID] = this.args.get(0).evaluate();
							CFFWriter.this.widthX[CFFWriter.this.currentCharStringID] = this.args.get(1).evaluate();

							// repurpose as rmoveto
							if (CFFWriter.this.lsbX[CFFWriter.this.currentCharStringID] != 0) {
								this.commandName = "rmoveto";
								this.args.set(1, new CharstringElement(0));
							}
							break;
						case 14: // endchar
							this.commandName = "endchar";
							claimArguments(0, false, true);
							break;
						case 21: // rmoveto
							this.commandName = "rmoveto";
							claimArguments(2, true, true);

							// If in a flex section channel args into current flex command
							if (CFFWriter.this.inFlex) {
								// If second pair found add to the first and store back
								if (CFFWriter.this.currentFlexCommand.args.size() == 2 && !CFFWriter.this.firstArgsAdded) {
									int arg0 = this.args.get(0).numberValue + CFFWriter.this.currentFlexCommand.args.get(0).numberValue;
									int arg1 = this.args.get(1).numberValue + CFFWriter.this.currentFlexCommand.args.get(1).numberValue;
									CFFWriter.this.currentFlexCommand.args.clear();
									CFFWriter.this.currentFlexCommand.args.add(new CharstringElement(arg0));
									CFFWriter.this.currentFlexCommand.args.add(new CharstringElement(arg1));
									CFFWriter.this.firstArgsAdded = true;
								}
								else {
									CFFWriter.this.currentFlexCommand.args.add(this.args.get(0));
									CFFWriter.this.currentFlexCommand.args.add(this.args.get(1));
								}
								this.commandName = "";
							}
							break;
						case 22: // hmoveto
							this.commandName = "hmoveto";
							claimArguments(1, true, true);

							// If in a flex section channel arg and 0 into current flex command
							if (CFFWriter.this.inFlex) {
								// If second pair found add to the first and store back
								if (CFFWriter.this.currentFlexCommand.args.size() == 2 && !CFFWriter.this.firstArgsAdded) {
									int arg0 = this.args.get(0).numberValue + CFFWriter.this.currentFlexCommand.args.get(0).numberValue;
									int arg1 = CFFWriter.this.currentFlexCommand.args.get(1).numberValue;
									CFFWriter.this.currentFlexCommand.args.clear();
									CFFWriter.this.currentFlexCommand.args.add(new CharstringElement(arg0));
									CFFWriter.this.currentFlexCommand.args.add(new CharstringElement(arg1));
									CFFWriter.this.firstArgsAdded = true;
								}
								else {
									CFFWriter.this.currentFlexCommand.args.add(this.args.get(0));
									CFFWriter.this.currentFlexCommand.args.add(new CharstringElement(0));
								}
								this.commandName = "";
							}
							break;
						case 30: // vhcurveto
							this.commandName = "vhcurveto";
							claimArguments(4, true, true);
							break;
						case 31: // hvcurveto
							this.commandName = "hvcurveto";
							claimArguments(4, true, true);
							break;
						case 255: // 5 byte number
							this.length = 5;
							this.isCommand = false;
							this.numberValue = (charstring[pos + 4] & 0xFF) + ((charstring[pos + 3] & 0xFF) << 8)
									+ ((charstring[pos + 2] & 0xFF) << 16) + ((charstring[pos + 1] & 0xFF) << 24);

							break;
						default:
					}

					if (mergePrevious) {
						CharstringElement previous = CFFWriter.this.currentCharString.get(CFFWriter.this.currentCharString.indexOf(this) - 1);
						if (this.commandName.equals(previous.commandName) && previous.args.size() <= (39 - this.args.size())) {
							CFFWriter.this.currentCharString.remove(previous);
							for (CharstringElement e : this.args) {
								previous.args.add(e);
							}
							this.args = previous.args;
						}
					}
				}
		}

		/**
		 * Evaluate the numerical value of this element. This is used for hsbw and sbw where the value is being funneled into a data structure rather
		 * than remaining in the converted charstring.
		 * 
		 * @return The numerical value of the element.
		 */
		private int evaluate() {
			if (this.isResult) {
				return this.parent.evaluate();
			}
			else
				if (this.isCommand) {
					if ("div".equals(this.commandName)) {
						return (this.args.get(1).evaluate() / this.args.get(0).evaluate());
					}
					else {}
				}

			return this.numberValue;
		}

		/**
		 * @return The number of bytes used in the original stream for just this element (not it's arguments).
		 */
		public int getLength() {
			return this.length;
		}

		/**
		 * Get the displacement created by this CharstringElement.
		 * 
		 * @return An int array pair of values for the horizontal and vertical displacement values.
		 */
		public int[] getDisplacement() {

			if (!this.isCommand) {
				return new int[] { 0, 0 };
			}

			if ("hstem".equals(this.commandName)) {}
			else
				if ("vstem".equals(this.commandName)) {}
				else
					if ("vmoveto".equals(this.commandName)) {
						return new int[] { 0, this.args.get(0).evaluate() };
					}
					else
						if ("rlineto".equals(this.commandName)) {
							int dx = 0;
							int dy = 0;
							for (int i = 0; i < this.args.size() / 2; i++) {
								dx += this.args.get(i * 2).evaluate();
								dy += this.args.get(1 + (i * 2)).evaluate();
							}
							return new int[] { dx, dy };
						}
						else
							if ("hlineto".equals(this.commandName)) {
								return new int[] { this.args.get(0).evaluate(), 0 };
							}
							else
								if ("vlineto".equals(this.commandName)) {
									return new int[] { 0, this.args.get(0).evaluate() };
								}
								else
									if ("rrcurveto".equals(this.commandName)) {
										int dx = 0;
										int dy = 0;
										for (int i = 0; i < this.args.size() / 2; i++) {
											dx += this.args.get(i * 2).evaluate();
											dy += this.args.get(1 + (i * 2)).evaluate();
										}
										return new int[] { dx, dy };
									}
									else
										if ("closepath".equals(this.commandName)) {}
										else
											if ("callsubr".equals(this.commandName)) {}
											else
												if ("return".equals(this.commandName)) {}
												else
													if ("dotsection".equals(this.commandName)) {}
													else
														if ("vstem3".equals(this.commandName)) {}
														else
															if ("hstem3".equals(this.commandName)) {}
															else
																if ("seac".equals(this.commandName)) {
																	// Hopefully won't have to implement this...
																}
																else
																	if ("sbw".equals(this.commandName)) {}
																	else
																		if ("div".equals(this.commandName)) {}
																		else
																			if ("callothersubr".equals(this.commandName)) {}
																			else
																				if ("pop".equals(this.commandName)) {}
																				else
																					if ("setcurrentpoint".equals(this.commandName)) {}
																					else
																						if ("hsbw".equals(this.commandName)) {}
																						else
																							if ("endchar".equals(this.commandName)) {}
																							else
																								if ("rmoveto".equals(this.commandName)) {
																									return new int[] { this.args.get(0).evaluate(),
																											this.args.get(1).evaluate() };
																								}
																								else
																									if ("hmoveto".equals(this.commandName)) {
																										return new int[] {
																												this.args.get(0).evaluate(), 0 };
																									}
																									else
																										if ("vhcurveto".equals(this.commandName)) {
																											return new int[] {
																													this.args.get(1).evaluate()
																															+ this.args.get(3)
																																	.evaluate(),
																													this.args.get(0).evaluate()
																															+ this.args.get(2)
																																	.evaluate() };
																										}
																										else
																											if ("hvcurveto".equals(this.commandName)) {
																												return new int[] {
																														this.args.get(0).evaluate()
																																+ this.args.get(1)
																																		.evaluate(),
																														this.args.get(2).evaluate()
																																+ this.args.get(3)
																																		.evaluate() };
																											}
																											else
																												if ("flex".equals(this.commandName)) {
																													int dx = 0;
																													int dy = 0;
																													for (int i = 0; i < 6; i++) {
																														dx += this.args.get(i * 2)
																																.evaluate();
																														dy += this.args.get(
																																1 + (i * 2))
																																.evaluate();
																													}
																													return new int[] { dx, dy };
																												}
																												else
																													if (this.commandName.length() == 0) {
																														return new int[] { 0, 0 };
																													}
																													else {}

			return new int[] { 0, 0 };
		}

		/**
		 * Scale this element according to a precalculated scale value. Works recursively.
		 */
		public void scale() {

			// If result, ignore
			if (this.isResult) {
				return;
			}

			// If number, scale it
			if (!this.isCommand) {
				this.numberValue = (int) (this.numberValue * CFFWriter.this.scale);
				return;
			}

			// Check how to handle args if command
			boolean scaleAll = false;
			if ("hstem".equals(this.commandName)) {
				scaleAll = true;
			}
			else
				if ("vstem".equals(this.commandName)) {
					scaleAll = true;
				}
				else
					if ("vmoveto".equals(this.commandName)) {
						scaleAll = true;
					}
					else
						if ("rlineto".equals(this.commandName)) {
							scaleAll = true;
						}
						else
							if ("hlineto".equals(this.commandName)) {
								scaleAll = true;
							}
							else
								if ("vlineto".equals(this.commandName)) {
									scaleAll = true;
								}
								else
									if ("rrcurveto".equals(this.commandName)) {
										scaleAll = true;
									}
									else
										if ("closepath".equals(this.commandName)) {}
										else
											if ("callsubr".equals(this.commandName)) {}
											else
												if ("return".equals(this.commandName)) {}
												else
													if ("dotsection".equals(this.commandName)) {}
													else
														if ("vstem3".equals(this.commandName)) {
															scaleAll = true;
														}
														else
															if ("hstem3".equals(this.commandName)) {
																scaleAll = true;
															}
															else
																if ("seac".equals(this.commandName)) {
																	for (int i = 0; i < 3; i++) {
																		this.args.get(i).scale();
																	}
																}
																else
																	if ("sbw".equals(this.commandName)) {}
																	else
																		if ("div".equals(this.commandName)) {
																			scaleAll = true;
																		}
																		else
																			if ("callothersubr".equals(this.commandName)) {}
																			else
																				if ("pop".equals(this.commandName)) {}
																				else
																					if ("setcurrentpoint".equals(this.commandName)) {
																						scaleAll = true;
																					}
																					else
																						if ("hsbw".equals(this.commandName)) {}
																						else
																							if ("endchar".equals(this.commandName)) {}
																							else
																								if ("rmoveto".equals(this.commandName)) {
																									scaleAll = true;
																								}
																								else
																									if ("hmoveto".equals(this.commandName)) {
																										scaleAll = true;
																									}
																									else
																										if ("vhcurveto".equals(this.commandName)) {
																											scaleAll = true;
																										}
																										else
																											if ("hvcurveto".equals(this.commandName)) {
																												scaleAll = true;
																											}
																											else
																												if ("flex".equals(this.commandName)) {
																													scaleAll = true;
																												}
																												else
																													if (this.commandName.length() == 0) {}
																													else {}

			if (scaleAll) {
				for (CharstringElement e : this.args) {
					e.scale();
				}
			}
		}

		/**
		 * Return the type 2 bytes required to match the effect of the instruction and it's arguments.
		 * 
		 * @return the type 2 bytes required to match the effect of the instruction and it's arguments
		 */
		public byte[] getType2Bytes() {

			if (!this.isCommand) {

				if (this.isResult) {
					return new byte[] {};
				}

				return FontWriter.setCharstringType2Number(this.numberValue);
			}

			boolean noChange = false;
			byte[] commandNumber = new byte[] {};

			if ("hstem".equals(this.commandName)) {
				noChange = true;
				commandNumber = new byte[] { 1 };
			}
			else
				if ("vstem".equals(this.commandName)) {
					noChange = true;
					commandNumber = new byte[] { 3 };
				}
				else
					if ("vmoveto".equals(this.commandName)) {
						noChange = true;
						commandNumber = new byte[] { 4 };
					}
					else
						if ("rlineto".equals(this.commandName)) {
							noChange = true;
							commandNumber = new byte[] { 5 };
						}
						else
							if ("hlineto".equals(this.commandName)) {
								noChange = true;
								commandNumber = new byte[] { 6 };
							}
							else
								if ("vlineto".equals(this.commandName)) {
									noChange = true;
									commandNumber = new byte[] { 7 };
								}
								else
									if ("rrcurveto".equals(this.commandName)) {
										noChange = true;
										commandNumber = new byte[] { 8 };
									}
									else
										if ("closepath".equals(this.commandName)) {
											// Remove moveto automatically closes paths in Type 2
											return new byte[] {};
										}
										else
											if ("callsubr".equals(this.commandName)) {
												return new byte[] {};

											}
											else
												if ("return".equals(this.commandName)) {
													// noChange=true;
													// commandNumber=new byte[]{11};
													// Unsupported othersubrs
													return new byte[] {};
												}
												else
													if ("dotsection".equals(this.commandName)) {
														// Deprecated - remove
														return new byte[] {};
													}
													else
														if ("vstem3".equals(this.commandName)) {

														}
														else
															if ("hstem3".equals(this.commandName)) {

															}
															else
																if ("seac".equals(this.commandName)) { // Create accented character by merging
																										// specified charstrings

																	// Get args
																	// int asb = args.get(0).numberValue;
																	int adx = this.args.get(1).numberValue;
																	int ady = this.args.get(2).numberValue;
																	int bchar = this.args.get(3).numberValue;
																	int achar = this.args.get(4).numberValue;

																	// Look up character code for specified location in standard encoding
																	int aCharUnicode = StandardFonts.getEncodedChar(StandardFonts.STD, achar).charAt(
																			0);
																	int bCharUnicode = StandardFonts.getEncodedChar(StandardFonts.STD, bchar).charAt(
																			0);
																	int accentIndex = -1;
																	int baseIndex = -1;

																	// Run through glyph names comparing character codes to those for the accent and
																	// base to find glyph indices
																	for (int i = 0; i < CFFWriter.this.glyphNames.length; i++) {
																		int adobePos = StandardFonts.getAdobeMap(CFFWriter.this.glyphNames[i]);
																		if (adobePos >= 0 && adobePos < 512) {

																			if (adobePos == aCharUnicode) {
																				accentIndex = i;
																			}
																			if (adobePos == bCharUnicode) {
																				baseIndex = i;
																			}
																		}
																	}

																	// Check both glyphs found
																	if (accentIndex == -1 || baseIndex == -1) {
																		return new byte[] {};
																	}

																	// Merge glyphs
																	try {
																		ByteArrayOutputStream bos = new ByteArrayOutputStream();

																		int charstringStore = CFFWriter.this.currentCharStringID;

																		// Fetch base charstring, convert, and remove endchar command
																		CFFWriter.this.charstringXDisplacement[baseIndex] = 0;
																		CFFWriter.this.charstringYDisplacement[baseIndex] = 0;
																		CFFWriter.this.inSeac = true;
																		byte[] rawBaseCharstring = convertCharstring(
																				CFFWriter.this.charstrings[baseIndex], baseIndex);
																		CFFWriter.this.inSeac = false;
																		CFFWriter.this.currentCharStringID = charstringStore;
																		byte[] baseCharstring = new byte[rawBaseCharstring.length - 1];
																		System.arraycopy(rawBaseCharstring, 0, baseCharstring, 0,
																				baseCharstring.length);
																		bos.write(baseCharstring);

																		// Move to the origin plus the offset
																		bos.write(FontWriter
																				.setCharstringType2Number(-(CFFWriter.this.charstringXDisplacement[baseIndex])
																						+ adx));
																		bos.write(FontWriter
																				.setCharstringType2Number(-(CFFWriter.this.charstringYDisplacement[baseIndex])
																						+ ady));
																		bos.write((byte) 21);

																		// Fetch accent charstring and convert
																		CFFWriter.this.charstringXDisplacement[accentIndex] = 0;
																		CFFWriter.this.charstringYDisplacement[accentIndex] = 0;
																		byte[] accentCharstring = convertCharstring(
																				CFFWriter.this.charstrings[accentIndex], accentIndex);
																		CFFWriter.this.currentCharStringID = charstringStore;
																		bos.write(accentCharstring);

																		return bos.toByteArray();
																	}
																	catch (IOException e) {
																		// tell user and log
																		if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
																	}

																}
																else
																	if ("sbw".equals(this.commandName)) {
																		// Might need to moveto arg coordinates?
																		return new byte[] {};

																	}
																	else
																		if ("div".equals(this.commandName)) {
																			noChange = true;
																			commandNumber = new byte[] { 12, 12 };

																		}
																		else
																			if ("callothersubr".equals(this.commandName)) {

																			}
																			else
																				if ("pop".equals(this.commandName)) {

																				}
																				else
																					if ("setcurrentpoint".equals(this.commandName)) {

																					}
																					else
																						if ("hsbw".equals(this.commandName)) {
																							// Might need to moveto arg coordinates?
																							return new byte[] {};

																						}
																						else
																							if ("endchar".equals(this.commandName)) {
																								noChange = true;
																								commandNumber = new byte[] { 14 };
																							}
																							else
																								if ("rmoveto".equals(this.commandName)) {
																									noChange = true;
																									commandNumber = new byte[] { 21 };
																								}
																								else
																									if ("hmoveto".equals(this.commandName)) {
																										noChange = true;
																										commandNumber = new byte[] { 22 };
																									}
																									else
																										if ("vhcurveto".equals(this.commandName)) {
																											noChange = true;
																											commandNumber = new byte[] { 30 };
																										}
																										else
																											if ("hvcurveto".equals(this.commandName)) {
																												noChange = true;
																												commandNumber = new byte[] { 31 };
																											}
																											else
																												if ("flex".equals(this.commandName)) {
																													noChange = true;
																													commandNumber = new byte[] { 12,
																															35 };
																												}
																												else
																													if (this.commandName.length() == 0) {
																														return new byte[] {};
																													}
																													else {}

			if (noChange) {
				// No change - return args and command
				ByteArrayOutputStream bos = getStreamWithArgs();
				try {
					bos.write(commandNumber);
				}
				catch (IOException e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
				return bos.toByteArray();
			}

			return new byte[] {};
		}

		private ByteArrayOutputStream getStreamWithArgs() {
			ByteArrayOutputStream result = new ByteArrayOutputStream();

			try {
				for (CharstringElement arg : this.args) {
					result.write(arg.getType2Bytes());
				}
			}
			catch (IOException e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}

			return result;
		}

		/**
		 * Return a representation of the element as a string.
		 * 
		 * @return Element as string
		 */
		@Override
		public String toString() {
			if (this.isCommand) {
				return this.commandName + this.args.toString();
			}

			if (this.isResult) {
				return "result of " + this.parent;
			}

			return String.valueOf(this.numberValue);
		}

		private void printStack() {
			System.out.println("Stack bottom");
			for (CharstringElement e : CFFWriter.this.currentCharString) {
				if (!e.isCommand) {
					System.out.println(e);
				}
			}
			System.out.println("Stack top");
		}

		/**
		 * Removes arguments from the stack (in other words, numbers and results from the instruction stream) and places them in this element's
		 * argument list.
		 * 
		 * @param count
		 *            The number of arguments to take
		 * @param takeFromBottom
		 *            Where to take the arguments from
		 * @param clearStack
		 *            Whether to clear the stack after
		 * @return whether enough arguments were found
		 */
		private boolean claimArguments(int count, boolean takeFromBottom, boolean clearStack) {

			if (count > 0) {
				int currentIndex = CFFWriter.this.currentCharString.indexOf(this);
				if (currentIndex == -1) {
					throw new RuntimeException("Not in list!");
				}

				int argsFound = 0;
				boolean failed = false;
				while (argsFound < count && !failed) {

					boolean found = false;
					if (takeFromBottom) {
						int pos = 0;
						while (!found && pos <= currentIndex) {
							CharstringElement e = CFFWriter.this.currentCharString.get(pos);
							if (!e.isCommand) {
								argsFound++;
								this.args.add(e);
								CFFWriter.this.currentCharString.remove(e);
								found = true;
							}
							pos++;
						}
					}
					else {
						int pos = currentIndex;
						while (!found && pos >= 0) {
							CharstringElement e = CFFWriter.this.currentCharString.get(pos);
							if (!e.isCommand) {
								argsFound++;
								this.args.add(e);
								CFFWriter.this.currentCharString.remove(e);
								found = true;
								currentIndex--;
							}
							pos--;
						}
					}
					if (!found) {
						failed = true;
					}
				}

				if (argsFound < count) {
					// System.out.println("Not enough arguments! ("+argsFound+" of "+count+") "+ (currentCharStringID > charstrings.length ?
					// "subr "+(currentCharStringID-charstrings.length) : "charstring "+currentCharStringID));
					// throw new RuntimeException("Not enough arguments!");
					return false;
				}
			}

			if (clearStack) {
				for (int i = 0; i < CFFWriter.this.currentCharString.size(); i++) {
					CharstringElement e = CFFWriter.this.currentCharString.get(i);
					if (!e.isCommand) {
						CFFWriter.this.currentCharString.remove(e);
					}
				}
			}

			return true;

		}
	}
}
