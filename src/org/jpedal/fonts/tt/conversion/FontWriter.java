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
 * FontWriter.java
 * ---------------
 */
package org.jpedal.fonts.tt.conversion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;

import org.jpedal.fonts.tt.FontFile2;
import org.jpedal.utils.Sorts;
import org.jpedal.utils.StringUtils;

public class FontWriter extends FontFile2 {

	private static final long serialVersionUID = -5399219427547126730L;

	String name;

	int glyphCount;

	int headCheckSumPos = -1;

	Map IDtoTable = new HashMap();

	private static boolean compressWoff = true;
	// remove the commas in generated font names in order to support html display
	// private final static boolean removeCommas = true;

	static {
		if (System.getProperty("org.jpedal.pdf2html.compressWoff") != null
				&& System.getProperty("org.jpedal.pdf2html.compressWoff").toLowerCase().equals("false")) compressWoff = false;
	}

	// list responsible for holding TTF Table Header information.
	ArrayList<TTFDirectory> ttfList = new ArrayList<TTFDirectory>();

	// old table order source kept in case of revert
	// /**
	// * the tsbles need to be in 1 order and
	// * the tags in the header in another
	// */
	// static final String[] TTFTableOrder=new String[]{"OS/2",
	// "cmap",
	// "glyf",
	// "head",
	// "hhea",
	// "hmtx",
	// "loca",
	// "maxp",
	// "name",
	// "post",
	// };

	/**
	 * the tsbles need to be in 1 order and the tags in the header in another
	 */
	static final String[] TTFTableOrder = new String[] { "OS/2", "cmap", "cvt ", "fpgm", "glyf", "head", "hhea", "hmtx", "loca", "maxp", "name",
			"post", "prep" };

	private HashMap<Integer, byte[]> tableStore = new HashMap<Integer, byte[]>();

	public FontWriter(byte[] data) {
		super(data);
	}

	public FontWriter() {}

	/**
	 * Retrieves a positive int from a specified number of bytes in supplied array
	 * 
	 * @param d
	 *            Byte array to fetch from
	 * @param startPoint
	 *            Location of start of number
	 * @param offsetSize
	 *            Number of bytes occupied
	 * @return Number found
	 */
	static int getUintFromByteArray(byte[] d, int startPoint, int offsetSize) {
		int shift = (offsetSize - 1) * 8;
		int result = 0;
		int offset = 0;
		while (shift >= 0) {
			int part = d[startPoint + offset];
			if (part < 0) part += 256;
			result = result | part << shift;
			offset++;
			shift = shift - 8;
		}

		return result;
	}

	/**
	 * Retrieves a binary number of arbitrary length from the supplied int array, treating it as bytes
	 * 
	 * @param d
	 *            int array to fetch from
	 * @param startPoint
	 *            Location of start of number
	 * @param offsetSize
	 *            Number of bytes occupied
	 * @return Number found
	 */
	static int getUintFromIntArray(int[] d, int startPoint, int offsetSize) {
		int shift = (offsetSize - 1) * 8;
		int result = 0;
		int offset = 0;
		while (shift >= 0) {
			result = result | (d[startPoint + offset] & 0xFF) << shift;
			offset++;
			shift = shift - 8;
		}

		return result;
	}

	/**
	 * Creates a Type 1c number as a byte array.
	 * 
	 * @param num
	 *            Number to encode
	 * @return byte array of number in type 1c format
	 */
	static byte[] set1cNumber(int num) {
		byte[] result;

		if (num >= -107 && num <= 107) {
			result = new byte[] { (byte) (num + 139) };

		}
		else
			if (num >= 108 && num <= 1131) {
				num -= 108;
				result = new byte[] { (byte) (247 + (num / 256)), (byte) (num & 0xFF) };

			}
			else
				if (num >= -1131 && num <= -108) {
					num += 108;
					result = new byte[] { (byte) (251 + (num / -256)), (byte) (-num & 0xFF) };

				}
				else
					if (num >= -32768 && num <= 32767) {
						result = new byte[] { 28, (byte) ((num / 256) & 0xFF), (byte) (num & 0xFF) };

					}
					else {
						result = new byte[] { 29, (byte) ((((num / 256) / 256) / 256) & 0xFF), (byte) (((num / 256) / 256) & 0xFF),
								(byte) ((num / 256) & 0xFF), (byte) (num & 0xFF) };
					}

		return result;
	}

	/**
	 * Creates a real type 1c as a byte array.
	 * 
	 * @param num
	 *            Number to encode
	 * @return byte array of number in type 1c real format
	 */
	static byte[] set1cRealNumber(double num) {
		String n = Double.toString(num);

		// Reduce length of number
		final int maxLength = 10;
		if (n.length() > maxLength) {
			if (n.contains("E")) {
				String[] parts = n.split("E");
				n = n.substring(0, maxLength - (parts[1].length() + 1)) + "E" + parts[1];
			}
			else {
				n = n.substring(0, maxLength);
			}
		}

		// Append f to end
		n = n + "f";

		// Find length in bytes
		int len = n.length();
		if ((len % 2) == 1) {
			len++;
		}
		len /= 2;
		byte[] result = new byte[1 + len];

		result[0] = 30;

		// Add nibbles
		for (int i = 0; i < len; i++) {
			int charLoc = i * 2;
			char a = n.charAt(charLoc);
			char b;
			if (charLoc + 1 < n.length()) {
				b = n.charAt(charLoc + 1);
			}
			else {
				b = a;
			}

			byte aByte = getNibble(a);
			byte bByte = getNibble(b);

			result[i + 1] = (byte) (((aByte & 0xF) << 4) | (bByte & 0xF));
		}

		return result;
	}

	/**
	 * Get the nibble which represents a character in a type 1c real number
	 * 
	 * @param c
	 *            Character to represent
	 * @return Representation as nibble in bottom 4 bits
	 */
	private static byte getNibble(char c) {
		switch (c) {
			case '.':
				return 0xa;
			case 'E':
				return 0xb;
			case '-':
				return 0xe;
			case 'f':
				return 0xf;
			default:
				return (byte) Integer.parseInt("" + c);
		}
	}

	/**
	 * Creates a Charstring Type 2 number as a byte array.
	 * 
	 * @param num
	 *            Number to encode
	 * @return byte array of number in charstring type 2 format
	 */
	static byte[] setCharstringType2Number(int num) {
		byte[] result;

		if (num >= -107 && num <= 107) {
			result = new byte[] { (byte) (num + 139) };

		}
		else
			if (num >= 108 && num <= 1131) {
				num -= 108;
				result = new byte[] { (byte) (247 + (num / 256)), (byte) (num & 0xFF) };

			}
			else
				if (num >= -1131 && num <= -108) {
					num += 108;
					result = new byte[] { (byte) (251 + (num / -256)), (byte) (-num & 0xFF) };

				}
				else {
					if (num >= 0) {
						result = new byte[] { (byte) 255, (byte) ((num / 256) & 0xFF), (byte) (num & 0xFF), 0, 0 };
					}
					else {
						int add = num + 32768;
						result = new byte[] { (byte) 255, (byte) (0x80 | ((add / 256) & 0x7F)), (byte) (add & 0xFF), 0, 0 };
					}
				}

		return result;
	}

	/**
	 * Encode a number as a byte array of arbitrary length.
	 * 
	 * @param num
	 *            The number to encode
	 * @param byteCount
	 *            The number of bytes to use
	 * @return The number expressed in the required number of bytes
	 */
	public static byte[] setUintAsBytes(int num, int byteCount) {
		byte[] result = new byte[byteCount];
		for (int i = byteCount; i > 0; i--) {
			int part = num;
			for (int j = 1; j < i; j++) {
				part = (part >> 8);
			}
			result[byteCount - i] = (byte) part;
		}

		return result;
	}

	static int createChecksum(byte[] table) {

		int checksumValue = 0;

		FontFile2 checksum = new FontFile2(table, true);

		int longCount = ((table.length + 3) >> 2);

		for (int j = 0; j < longCount; j++) {
			checksumValue = checksumValue + checksum.getNextUint32();
		}

		return checksumValue;
	}

	/**
	 * return a short
	 */
	final static public byte[] setUFWord(int rawValue) {

		short value = (short) rawValue;

		byte[] returnValue = new byte[2];

		for (int i = 0; i < 2; i++) {
			returnValue[i] = (byte) ((value >> (8 * (1 - i))) & 255);
		}

		return returnValue;
	}

	/**
	 * return a short
	 */
	final static public byte[] setFWord(int rawValue) {

		short value = (short) rawValue;

		byte[] returnValue = new byte[2];

		for (int i = 0; i < 2; i++) {
			returnValue[i] = (byte) ((value >> (8 * (1 - i))) & 255);
		}

		return returnValue;
	}

	/**
	 * turn int back into byte[2]
	 **/
	final static public byte[] setNextUint16(int value) {

		byte[] returnValue = new byte[2];

		for (int i = 0; i < 2; i++) {
			returnValue[i] = (byte) ((value >> (8 * (1 - i))) & 255);
		}
		return returnValue;
	}

	/**
	 * turn int back into byte[2]
	 **/
	final static public byte[] setNextInt16(int value) {

		byte[] returnValue = new byte[2];

		for (int i = 0; i < 2; i++) {
			returnValue[i] = (byte) ((value >> (8 * (1 - i))) & 255);
		}
		return returnValue;
	}

	/**
	 * turn int back into byte[2]
	 **/
	final static public byte[] setNextSignedInt16(short value) {

		byte[] returnValue = new byte[2];

		for (int i = 0; i < 2; i++) {
			returnValue[i] = (byte) ((value >> (8 * (1 - i))) & 255);
		}
		return returnValue;
	}

	/**
	 * turn int back into byte
	 **/
	final static public byte setNextUint8(int value) {
		return (byte) (value & 255);
	}

	/**
	 * turn int back into byte[4]
	 **/
	final static public byte[] setNextUint32(int value) {

		byte[] returnValue = new byte[4];

		for (int i = 0; i < 4; i++) {
			returnValue[i] = (byte) ((value >> (8 * (3 - i))) & 255);
		}

		return returnValue;
	}

	/**
	 * turn int back into byte[8]
	 **/
	final static public byte[] setNextUint64(int value) {

		byte[] returnValue = new byte[8];

		for (int i = 0; i < 8; i++) {
			returnValue[i] = (byte) ((value >> (8 * (7 - i))) & 255);
		}

		return returnValue;
	}

	final static public byte[] setNextUint64(long value) {

		byte[] returnValue = new byte[8];

		for (int i = 0; i < 8; i++) {
			returnValue[i] = (byte) ((value >> (8 * (7 - i))) & 255);
		}

		return returnValue;
	}

	/** read the table tableLength */
	final public byte[] writeFontToStream() throws IOException {

		readTables();

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		// version
		if (this.type == OPENTYPE) { // OTF with glyf data start OTTO otherwise 1.0
			if (this.subType == PS) bos.write(setNextUint32(1330926671));
			else bos.write(setNextUint32(65536));
		}
		else
			if (this.type == TTC) bos.write(setNextUint32(1953784678));
			else bos.write(setNextUint32(65536));

		if (this.type == TTC) {

			System.out.println("TTC write not implemented");

			// getNextUint32(); //version
			// fontCount=getNextUint32();
			//
			// //location of tables
			// tables=new int[tableCount][fontCount];
			// tableLength=new int[tableCount][fontCount];
			//
			// int[] fontOffsets=new int[fontCount];
			//
			// for(int currentFont=0;currentFont<fontCount;currentFont++){
			//
			// currentFontID=currentFont;
			//
			// int fontStart=getNextUint32();
			// fontOffsets[currentFont]=fontStart;
			// }
			//
			// for(int currentFont=0;currentFont<fontCount;currentFont++){
			//
			// currentFontID=currentFont; //choose this font
			//
			// this.pointer = fontOffsets[currentFont];
			//
			// scalerType=getNextUint32();
			//
			// readTablesForFont();
			// }
			//
			// //back to default
			// currentFontID=0;

		}
		else { // otf or ttf

			// location of tables
			// tables=new int[tableCount][1];
			// tableLength=new int[tableCount][1];

			writeTablesForFont(bos);
		}

		bos.flush();
		bos.close();
		byte[] fileInBytes = bos.toByteArray();

		/**
		 * set HEAD checksum
		 */
		byte[] headCheckSum = setNextUint32((int) (Long.parseLong("B1B0AFBA", 16) - createChecksum(fileInBytes)));
		System.arraycopy(headCheckSum, 0, fileInBytes, 0 + this.headCheckSumPos, 4);

		// Code for writing out whole font
		// if (name.contains("NAAAJN"))
		// new BinaryTool(fileInBytes,"C:/Users/sam/desktop/"+name.replace('+', '-')+".otf.iab");

		return fileInBytes;
	}

	// empty base copy replaced by version from TT or PS
	void readTables() {
	}

	boolean debug = false;

	private void writeTablesForFont(ByteArrayOutputStream bos) throws IOException {

		// boolean hasValidOffsets=false;

		int[] keys = new int[this.numTables];
		int[] offset = new int[this.numTables];
		int id;
		String tag;
		int[] checksum = new int[this.numTables];
		int[] tableSize = new int[this.numTables];

		/**
		 * write out header
		 */
		bos.write(setNextUint16(this.numTables));
		bos.write(setNextUint16(this.searchRange));
		bos.write(setNextUint16(this.entrySelector));
		bos.write(setNextUint16(this.rangeShift));

		/**
		 * calc checksums
		 */
		for (int l = 0; l < this.numTables; l++) {

			tag = this.tableList.get(l);

			// read table value (including for head which is done differently at end)
			id = getTableID(tag);

			if (this.debug) System.out.println("writing out " + tag + " id=" + id);

			if (id != -1) {
				byte[] tableBytes = this.getTableBytes(id);
				this.tableStore.put(id, tableBytes);

				// head is set to zero here and replaced later
				if (id != HEAD) checksum[l] = createChecksum(tableBytes);

				tableSize[l] = tableBytes.length;

				// needed below to work out order in file
				keys[l] = l;
				offset[l] = this.tables[id][this.currentFontID];
				// if(offset[l]>0){ //all set to zero in TTF so ignore sort
				// hasValidOffsets=true;
				// }
			}
		}

		if (this.subType == PS) {// not used in Marks new code - one for sam to consider?
			keys = Sorts.quicksort(offset, keys);
		}

		int currentOffset = alignOnWordBoundary(bos.size() + (16 * this.numTables));
		int[] fileOffset = new int[this.numTables];

		int i;
		/**
		 * calc filePointer
		 */
		for (int l = 0; l < this.numTables; l++) {

			i = keys[l];

			fileOffset[i] = currentOffset;

			offset[i] = currentOffset;

			currentOffset = alignOnWordBoundary(currentOffset + tableSize[i]);

		}

		// write out TTF font data
		if (this.subType == TTF) {

			/**
			 * write out pointers
			 */
			for (int j = 0; j < this.numTables; j++) {

				tag = TTFTableOrder[j];

				bos.write(StringUtils.toBytes(tag));

				// read table value
				id = getTableID(tag);
				int l = (Integer) (this.IDtoTable.get(tag));

				if (id != -1) {

					// flag pos to put in correct value later
					if (id == HEAD) {
						this.headCheckSumPos = bos.size();
					}

					bos.write(setNextUint32(checksum[l]));// checksum
					bos.write(setNextUint32(fileOffset[l])); // table pos
					bos.write(setNextUint32(tableSize[l])); // table length

					this.ttfList.add(new TTFDirectory(tag, fileOffset[l], checksum[l], tableSize[l]));

				}
			}

		}
		else { // old version for PS

			/**
			 * write out pointers
			 */
			for (int j = 0; j < this.numTables; j++) {

				int l = j;// keys[j];
				tag = this.tableList.get(l);

				bos.write(StringUtils.toBytes(tag));

				// read table value
				id = getTableID(tag);

				if (id != -1) {
					// byte[] table=this.getTableBytes(id);

					// flag pos to put in correct value later
					if (id == HEAD) {
						this.headCheckSumPos = bos.size();
					}

					bos.write(setNextUint32(checksum[l]));// checksum
					bos.write(setNextUint32(fileOffset[l])); // table pos
					bos.write(setNextUint32(tableSize[l])); // table length

					this.ttfList.add(new TTFDirectory(tag, fileOffset[l], checksum[l], tableSize[l]));

				}
			}
		}

		/**
		 * write out actual tables in order
		 */
		int sortedKey;
		byte[] bytes;
		for (int l = 0; l < this.numTables; l++) {

			sortedKey = keys[l];
			tag = this.tableList.get(sortedKey);
			id = getTableID(tag);
			// bytes=this.getTableBytes(id);
			bytes = this.tableStore.get(id);

			// fill in any gaps (pad out to multiple of 4 byte blocks)
			while ((bos.size() & 3) != 0) {
				bos.write((byte) 0);
			}

			bos.write(bytes);
		}
	}

	private static int alignOnWordBoundary(int currentOffset) {
		// make sure on 4 byte boundary
		int packing = currentOffset & 3;
		if (packing != 0) {
			currentOffset = currentOffset + (4 - packing);
		}

		return currentOffset;
	}

	/*
	 * Method does the ttf to woff conversion ######################################################################################### ## please read
	 * carefully the woff specification prior to any alteration on this method ##
	 * #########################################################################################
	 */
	final public byte[] writeFontToWoffStream() throws IOException {
		byte[] originalSfntData = writeFontToStream();

		for (TTFDirectory t : this.ttfList) {
			if (t.getTag().equals("head")) {
				int headChecksum = AdjustWoffChecksum(originalSfntData, t.getOffset(), t.getLength());
				t.setChecksum(headChecksum);
			}
		}

		int ttfSuffix = 12 + (16 * this.numTables);
		int woffSuffix = 44 + (20 * this.numTables);
		int onlySfntLen = originalSfntData.length - ttfSuffix;
		byte[] onlySfntData = new byte[onlySfntLen];

		System.arraycopy(originalSfntData, ttfSuffix, onlySfntData, 0, originalSfntData.length - ttfSuffix);

		ByteArrayOutputStream dbos = new ByteArrayOutputStream(4);

		int endPos = 0;
		for (TTFDirectory d : this.ttfList) {

			byte[] b = new byte[d.getLength()];
			System.arraycopy(originalSfntData, d.getOffset(), b, 0, d.getLength());
			d.setOffset(endPos + woffSuffix);

			int compLength = d.getLength();

			if (compressWoff) {
				byte[] output = new byte[d.getLength()];
				
				//Lonzak: added correct Deflater end() method
				Deflater compresser=null;
				try {
					compresser = new Deflater(Deflater.BEST_COMPRESSION);
					compresser.setInput(b);
					compresser.finish();
					compLength = compresser.deflate(output);
					if (compLength < d.getLength() && compresser.finished()) {
						dbos.write(output, 0, compLength);
					}
					else {
						dbos.write(b);
						compLength = d.getLength();
					}
				}
				finally {
					if(compresser!=null) {
						compresser.end();
						compresser=null;
					}
				}
			}
			else {
				dbos.write(b);
			}
			d.setCompressLength(compLength);

			// int padding = (d.getLength()%4);
			int padding = (compLength % 4) > 0 ? 4 - (compLength % 4) : 0;
			if (padding > 0) {
				byte[] n = new byte[padding];
				dbos.write(n);
			}
			endPos = endPos + d.getCompressLength() + padding;
			// System.out.println("printing\t"+d.getTag()+" off "+d.getOffset()+" checksum "+d.getChecksum()+" length "+d.getLength()+" compressLen "+d.getCompressLength()+" --pdding-- "+
			// padding +" --- end-- "+endPos);

		}

		ByteArrayOutputStream wbos = new ByteArrayOutputStream();
		wbos.write(setNextUint32(0x774f4646)); // signature

		// below lines write sfnt version into woff data
		if (this.type == OPENTYPE) { // OTF with glyf data start OTTO otherwise 1.0
			if (this.subType == PS) wbos.write(setNextUint32(1330926671));
			else wbos.write(setNextUint32(65536));
		}
		else
			if (this.type == TTC) {
				wbos.write(setNextUint32(1953784678));
			}
			else {
				wbos.write(setNextUint32(65536));
			}
		// end of sfnt version

		wbos.write(setNextUint32(woffSuffix + endPos));// length
		wbos.write(setNextUint16(this.numTables));// number of tables
		wbos.write(setNextUint16(0)); // reserved

		int endPadding = (originalSfntData.length % 4) > 0 ? 4 - (originalSfntData.length % 4) : 0;

		wbos.write(setNextUint32(endPadding > 0 ? originalSfntData.length + endPadding : originalSfntData.length));
		wbos.write(setNextUint16(1)); // major version
		wbos.write(setNextUint16(1)); // minor version
		wbos.write(setNextUint32(0)); // metaOffet
		wbos.write(setNextUint32(0)); // metaLength
		wbos.write(setNextUint32(0)); // metaOrigLength
		wbos.write(setNextUint32(0)); // privOffset
		wbos.write(setNextUint32(0)); // privLength

		for (TTFDirectory t : this.ttfList) {
			wbos.write(t.getTagBytes());
			wbos.write(setNextUint32(t.getOffset()));
			wbos.write(setNextUint32(t.getCompressLength()));
			wbos.write(setNextUint32(t.getLength()));
			wbos.write(setNextUint32(t.getChecksum()));
		}
		wbos.write(dbos.toByteArray());
		wbos.flush();
		wbos.close();
		return wbos.toByteArray();
	}

	/*
	 * Method adjust the header table checksum and checkSumAdjustment field in head
	 * @return calculated head checksum
	 */
	private int AdjustWoffChecksum(byte[] tableBytes, int headOffset, int headLength) {

		if (tableBytes.length > headOffset && headLength >= 4) {
			ByteBuffer data = ByteBuffer.wrap(tableBytes);

			byte[] a = new byte[4];
			for (int z = 0; z < 4; z++) {
				a[z] = 0;
			}

			System.arraycopy(a, 0, tableBytes, (headOffset + 8), 4);
			int totalChecksum = (0xB1B0AFBA - createChecksum(a));

			byte[] b = new byte[headLength];
			System.arraycopy(data.array(), headOffset, b, 0, headLength);

			int headChecksum = createChecksum(b);

			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(totalChecksum);
			byte[] c = bb.array();

			System.arraycopy(c, 0, tableBytes, (headOffset + 8), 4);
			return headChecksum;

		}
		else {
			return 0;
		}
	}

	/*
	 * This class added in order to access the variable amount in woff Font Conversion
	 */
	private class TTFDirectory {
		private int offset2, checksum2, length2, compressLength;

		private String tag2;

		public TTFDirectory(String tag, int offset, int checksum, int length) {
			this.tag2 = tag;
			this.offset2 = offset;
			this.checksum2 = checksum;
			this.length2 = length;
		}

		public String getTag() {
			return this.tag2;
		}

		public byte[] getTagBytes() {
			byte[] b = new byte[4];
			for (int z = 0; z < 4; z++) {
				b[z] = (byte) this.getTag().charAt(z);
			}
			return b;
		}

		public int getOffset() {
			return this.offset2;
		}

		public int getChecksum() {
			return this.checksum2;
		}

		public int getLength() {
			return this.length2;
		}

		public int getCompressLength() {
			return this.compressLength;
		}

		public void setOffset(int offset) {
			this.offset2 = offset;
		}

		public void setChecksum(int checksum) {
			this.checksum2 = checksum;
		}

		public void setLength(int length) {
			this.length2 = length;
		}

		public void setTag(String tag) {
			this.tag2 = tag;
		}

		public void setCompressLength(int compressLength) {
			this.compressLength = compressLength;
		}
	}

}
