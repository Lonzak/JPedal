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
 * CCITT1D.java
 * ---------------
 */
package org.jpedal.io.filter.ccitt;

import java.util.BitSet;

import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;

/**
 * implement 1D CCITT decoding
 */
public class CCITT1D implements CCITTDecoder {

	/**
	 * values set in PdfObject
	 */
	boolean BlackIs1 = false, isByteAligned = false;
	int columns = 1728;

	byte[] data;

	int bitReached;

	private final static int EOL = -1;

	private static final boolean debug = false;

	boolean isWhite = true;
	private boolean isTerminating = false;

	private boolean isEndOfLine = false;
	private boolean EOS = false;

	private int cRTC = 0;
	int width = 0;
	int height = 0;
	private int line = 0;

	BitSet out;
	private BitSet inputBits;
	int outPtr = 0;

	private int bytesNeeded = 0;
	int scanlineStride;
	private int inputBitCount = 0;

	// ---------- BLACK --------------

	final static private int[][][] b = new int[][][] { { { 3, 2, 0 }, { 2, 3, 0 } },
			{ { 2, 1, 0 }, { 3, 4, 0 } },

			{ // b4
			{ 3, 5, 0 }, { 2, 6, 0 } },

			// b5
			{ { 3, 7, 0 } },
			// b6
			{ { 5, 8, 0 }, { 4, 9, 0 } },
			// b7
			{ { 4, 10, 0 }, { 5, 11, 0 }, { 7, 12, 0 } },
			// b8
			{ { 4, 13, 0 }, { 7, 14, 0 } },

			// b9
			{ { 24, 15, 0 } },

			// b10
			{ { 55, 0, 0 }, { 23, 16, 0 }, { 24, 17, 0 }, { 8, 18, 0 }, { 15, 64, 1 } },

			// b11
			{ { 103, 19, 0 }, { 104, 20, 0 }, { 108, 21, 0 }, { 55, 22, 0 }, { 40, 23, 0 }, { 23, 24, 0 }, { 24, 25, 0 }, { 8, 1792, 1 },
					{ 12, 1856, 1 }, { 13, 1920, 1 } },

			// b12
			{ { 202, 26, 0 }, { 203, 27, 0 }, { 204, 28, 0 }, { 205, 29, 0 }, { 104, 30, 0 }, { 105, 31, 0 }, { 106, 32, 0 }, { 107, 33, 0 },
					{ 210, 34, 0 }, { 211, 35, 0 }, { 212, 36, 0 }, { 213, 37, 0 }, { 214, 38, 0 }, { 215, 39, 0 }, { 108, 40, 0 }, { 109, 41, 0 },
					{ 218, 42, 0 }, { 219, 43, 0 }, { 84, 44, 0 }, { 85, 45, 0 }, { 86, 46, 0 }, { 87, 47, 0 }, { 100, 48, 0 }, { 101, 49, 0 },
					{ 82, 50, 0 }, { 83, 51, 0 }, { 36, 52, 0 }, { 55, 53, 0 }, { 56, 54, 0 }, { 39, 55, 0 }, { 40, 56, 0 }, { 88, 57, 0 },
					{ 89, 58, 0 }, { 43, 59, 0 }, { 44, 60, 0 }, { 90, 61, 0 }, { 102, 62, 0 }, { 103, 63, 0 }, { 200, 128, 1 }, { 201, 192, 1 },
					{ 91, 256, 1 }, { 51, 320, 1 }, { 52, 384, 1 }, { 53, 448, 1 }, { 1, EOL, 1 }, { 18, 1984, 1 }, { 19, 2048, 1 }, { 20, 2112, 1 },
					{ 21, 2176, 1 }, { 22, 2240, 1 }, { 23, 2304, 1 }, { 28, 2368, 1 }, { 29, 2432, 1 }, { 30, 2496, 1 }, { 31, 2560, 1 } },

			// b13
			{ { 108, 512, 1 }, { 109, 576, 1 }, { 74, 640, 1 }, { 75, 704, 1 }, { 76, 768, 1 }, { 77, 832, 1 }, { 114, 896, 1 }, { 115, 960, 1 },
					{ 116, 1024, 1 }, { 117, 1088, 1 }, { 118, 1152, 1 }, { 119, 1216, 1 }, { 82, 1280, 1 }, { 83, 1344, 1 }, { 84, 1408, 1 },
					{ 85, 1472, 1 }, { 90, 1536, 1 }, { 91, 1600, 1 }, { 100, 1664, 1 }, { 101, 1728, 1 } } };

	// ---------- WHITE --------------

	final static private int[][][] w = new int[][][] {
			{ { 7, 2, 0 }, { 8, 3, 0 }, { 11, 4, 0 }, { 12, 5, 0 }, { 14, 6, 0 }, { 15, 7, 0 } },
			// b5
			{ { 19, 8, 0 }, { 20, 9, 0 }, { 7, 10, 0 }, { 8, 11, 0 }, { 27, 64, 1 }, { 18, 128, 1 } },
			// b6
			{ { 7, 1, 0 }, { 8, 12, 0 }, { 3, 13, 0 }, { 52, 14, 0 }, { 53, 15, 0 }, { 42, 16, 0 }, { 43, 17, 0 }, { 23, 192, 1 }, { 24, 1664, 1 } },
			// w7
			{ { 39, 18, 0 }, { 12, 19, 0 }, { 8, 20, 0 }, { 23, 21, 0 }, { 3, 22, 0 }, { 4, 23, 0 }, { 40, 24, 0 }, { 43, 25, 0 }, { 19, 26, 0 },
					{ 36, 27, 0 }, { 24, 28, 0 }, { 55, 256, 1 } },

			// w8
			{ { 53, 0, 0 }, { 2, 29, 0 }, { 3, 30, 0 }, { 26, 31, 0 }, { 27, 32, 0 }, { 18, 33, 0 }, { 19, 34, 0 }, { 20, 35, 0 }, { 21, 36, 0 },
					{ 22, 37, 0 }, { 23, 38, 0 }, { 40, 39, 0 }, { 41, 40, 0 }, { 42, 41, 0 }, { 43, 42, 0 }, { 44, 43, 0 }, { 45, 44, 0 },
					{ 4, 45, 0 }, { 5, 46, 0 }, { 10, 47, 0 }, { 11, 48, 0 }, { 82, 49, 0 }, { 83, 50, 0 }, { 84, 51, 0 }, { 85, 52, 0 },
					{ 36, 53, 0 }, { 37, 54, 0 }, { 88, 55, 0 }, { 89, 56, 0 }, { 90, 57, 0 }, { 91, 58, 0 }, { 74, 59, 0 }, { 75, 60, 0 },
					{ 50, 61, 0 }, { 51, 62, 0 }, { 52, 63, 0 }, { 54, 320, 1 }, { 55, 384, 1 }, { 100, 448, 1 }, { 101, 512, 1 }, { 104, 576, 1 },
					{ 103, 640, 1 } },

			// w9
			{ { 204, 704, 1 }, { 205, 768, 1 }, { 210, 832, 1 }, { 211, 896, 1 }, { 212, 960, 1 }, { 213, 1024, 1 }, { 214, 1088, 1 },
					{ 215, 1152, 1 }, { 216, 1216, 1 }, { 217, 1280, 1 }, { 218, 1344, 1 }, { 219, 1408, 1 }, { 152, 1472, 1 }, { 153, 1536, 1 },
					{ 154, 1600, 1 }, { 155, 1728, 1 } },

			// w10
			{},

			// w11
			{ { 8, 1792, 1 }, { 12, 1856, 1 }, { 13, 1920, 1 } },

			// w12
			{ { 1, EOL, 1 }, { 18, 1984, 1 }, { 19, 2048, 1 }, { 20, 2112, 1 }, { 21, 2176, 1 }, { 22, 2240, 1 }, { 23, 2304, 1 }, { 28, 2368, 1 },
					{ 29, 2432, 1 }, { 30, 2496, 1 }, { 31, 2560, 1 } } };

	public CCITT1D(byte[] rawData, int width, int height, PdfObject DecodeParms) {

		this.data = rawData;
		this.bitReached = 0;

		this.columns = width; // default if not set (in theory should be the same if set)

		// and any values from PDFobject
		if (DecodeParms != null) {

			this.BlackIs1 = DecodeParms.getBoolean(PdfDictionary.BlackIs1);

			int columnsSet = DecodeParms.getInt(PdfDictionary.Columns);
			if (columnsSet != -1) this.columns = columnsSet;

			int rowsSet = DecodeParms.getInt(PdfDictionary.Rows);
			if (rowsSet > 0) // allow for value set to 0 which is impossible! (see abacus/aba_Dossier)
			height = rowsSet;

			this.isByteAligned = DecodeParms.getBoolean(PdfDictionary.EncodedByteAlign);

			if (debug) System.out.println("BlackIs1=" + this.BlackIs1 + "\ncolumnsSet=" + columnsSet + "\nisByteAligned=" + this.isByteAligned
					+ "\nrowsSet=" + rowsSet);
		}

		// and other values which might use defaults set from PdfObject
		this.width = this.columns;
		this.height = height;

		this.scanlineStride = (this.columns + 7) >> 3;

		this.bytesNeeded = (height * this.scanlineStride);

		this.out = new BitSet(this.bytesNeeded << 3);

		// number of bits in raw compressed data
		this.inputBitCount = this.data.length << 3;

		// raw data bits to read for codewords
		this.inputBits = fromByteArray(this.data, this.inputBitCount);
	}

	CCITT1D() {}

	@Override
	public byte[] decode() {

		// Commented out as breaks dotloop file
		// moveToEOLMarker();

		// read all the tokens and find sub values until End Of Stream
		decode1DRun();

		// put output bits together into byte[] we return
		// put it all together
		byte[] buffer = createOutputFromBitset();

		// by default blackIs1 is false so black pixels do not need to be set
		// invert image if needed -
		// ultimately will be quicker to add into decode
		// k==0 seems to be reverse of others
		if (this.BlackIs1) {
			for (int i = 0; i < buffer.length; i++)
				buffer[i] = (byte) (255 - buffer[i]); // invert all bytes
		}

		return buffer;
	}

	byte[] createOutputFromBitset() {

		byte[] output = new byte[this.bytesNeeded];

		// assemble all tokens into a decompressed output data block
		int bytePtr = 0, bitPtr = 7, mask;
		byte entry = 0;

		for (int j = 0; j < this.outPtr; j++) {

			if (this.out.get(j)) {
				mask = 1 << bitPtr;

				entry |= mask;
				bitPtr--;
			}
			else {
				bitPtr--;
			}

			if (((j + 1) % (this.width)) == 0 && j != 0) {
				bitPtr = -1;
			}

			if (bitPtr < 0 && bytePtr < output.length) {
				output[bytePtr] = entry;
				bytePtr++;
				bitPtr = 7;
				entry = 0;
			}
		}

		return output;
	}

	/**
	 * work through a 1D block reading code words and creating uncompressed data
	 */
	private void decode1DRun() {

		while (!this.EOS) { // repeat until we hit the end of the data

			// set flags for codeword
			if (this.isTerminating) {

				this.isTerminating = false;
				this.isWhite = !this.isWhite;

				if (this.isEndOfLine) {
					this.isEndOfLine = false;
					this.isWhite = true;
				}
			}

			// remember state as getPixelCount will alter
			boolean pixelIsWhite = this.isWhite;

			// decode the next codeword and get how many pixels written out
			int pixelCount = getCodeWord();

			// set bits
			if (pixelCount > 0) {
				if (pixelIsWhite) {
					this.out.set(this.outPtr, (this.outPtr + pixelCount), true);
				}
				this.outPtr = this.outPtr + pixelCount;
			}
		}
	}

	private int getCodeWord() {

		int pixelCount = 0, itemFound = -1, maskAdj = 0;

		/**
		 * look for valid value starting at 2 bits
		 * 
		 * starting with 2 bits (4 for white) look at next pixels until we find a valid value for next codeword
		 */
		int startBitLength = 2, endBit = 14, code = 0, bits = 0;
		if (this.isWhite) { // no values for 2,3 in this case
			startBitLength = 4;
			endBit = 13;
		}

		// loop to find the valid keys by checking next bit value against tables
		for (int bitLength = startBitLength; bitLength < endBit; bitLength++) {
			code = get1DBits(bitLength, true) & 255; // next n bits as possible key
			itemFound = checkTables(code, bitLength, this.isWhite); // see if it is a key

			// if it is we exit
			if (itemFound != -1) {
				bits = bitLength;
				bitLength = endBit;
			}
			else
				if (bitLength == 8) {
					maskAdj++;
					this.bitReached++;
				}
		}

		// we have a match so process the codeword and move on pointer
		if (itemFound != -1) {
			// update count of bits
			this.bitReached = this.bitReached + bits - maskAdj;

			pixelCount = processCodeWord(itemFound, code, bits);
		}
		else
			if (this.bitReached > this.inputBitCount) {
				this.EOS = true;
			}

		return pixelCount;
	}

	private int processCodeWord(int itemFound, int code, int bits) {

		int pixelCount;
		boolean isT;

		// values in the table
		if (this.isWhite) {
			pixelCount = w[bits - 4][itemFound][1];
			isT = w[bits - 4][itemFound][2] == 0;
		}
		else {
			pixelCount = b[bits - 2][itemFound][1];
			isT = b[bits - 2][itemFound][2] == 0;
		}

		if (isT) this.isTerminating = true;

		if (pixelCount == -1) {
			if (this.line != 0) {
				pixelCount = this.width - this.line;
			}

			this.line = 0;
			this.isWhite = true;
			this.isTerminating = false;
		}

		if (pixelCount != -1) {
			this.line = this.line + pixelCount;

			if (this.line == this.width) {
				if (isT) {
					this.line = 0;
					this.isEndOfLine = true;
				}

			}
			else
				if (this.line > this.width) {
					this.line = 0;
					this.isEndOfLine = true;
				}
		}

		if (bits == 12 && code == 1) {
			this.cRTC++;
			if (this.cRTC == 6) {
				this.EOS = true;
			}
		}
		else {
			this.cRTC = 0;
		}

		if (this.cRTC != 6 && this.isEndOfLine && this.isByteAligned) {

			// get bits over 8 and align to byte boundary
			int iPart = (this.bitReached) % 8;
			int iDrop = 8 - (iPart);
			if (iPart > 0) {
				this.bitReached = this.bitReached + iDrop;
			}
		}

		return pixelCount;
	}

	// 2D version
	int get1DBits(int bitsToGet) {
		return get1DBits(bitsToGet, false);
	}

	private int get1DBits(int bitsToGet, boolean is1D) {

		int tmp = 0;

		int maskAdj = 0;
		if (is1D && bitsToGet > 8) maskAdj++;

		int mask;
		for (int y = 0; y < bitsToGet; y++) {
			if (this.inputBits.get(y + this.bitReached)) {
				mask = 1 << (bitsToGet - y - 1 - maskAdj);

				tmp |= mask;
			}
		}

		return tmp;
	}

	private static int checkTables(int possCode, int bitLength, boolean isWhite) {

		int itemFound = -1;
		int[][] table;

		if (isWhite) {
			table = w[bitLength - 4];
		}
		else {
			table = b[bitLength - 2];
		}

		int size = table.length;

		for (int z = 0; z < size; z++) {
			if (possCode == table[z][0]) {
				itemFound = z;
				z = size;

			}
		}
		return itemFound;
	}

	private static BitSet fromByteArray(byte[] bytes, int bitsNeeded) {

		int bitSetPtr = 0, value;
		byte tmp;

		BitSet bits = new BitSet(bitsNeeded);
		for (byte aByte : bytes) {
			tmp = aByte;
			for (int z = 7; z >= 0; z--) {

				value = (tmp & (1 << z));

				if (value >= 1) bits.set(bitSetPtr, true);

				bitSetPtr++;
			}
		}

		return bits;
	}

}
