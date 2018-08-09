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
 * CFFFixer.java
 * ---------------
 */
package org.jpedal.fonts.tt.conversion;

public class CFFFixer {

	private byte[] data, original;
	private int charstringOffset = -1;

	private int privateOffset = -1;
	private int privateOffsetLocation = -1;
	private int privateOffsetLength = -1;

	private int fdArrayOffset = -1;
	private int fdArrayOffsetLocation = -1;
	private int fdArrayOffsetLength = -1;

	public CFFFixer(byte[] cff) {
		super();

		this.data = cff;
		this.original = new byte[cff.length];
		System.arraycopy(cff, 0, this.original, 0, cff.length);

		// Check for any sequence of 12 0 and fix if found
		if (scanForDotsection()) fixData();
	}

	/**
	 * Tests for a sequence of 12 0, which might signify dotsection is being used.
	 * 
	 * @return Whether dotsection might be being used.
	 */
	private boolean scanForDotsection() {
		boolean result = false;
		for (int i = 0; i < this.data.length - 1; i++) {
			if (this.data[i] == 12 && this.data[i + 1] == 0) result = true;
		}

		return result;
	}

	/**
	 * Go through the data and remove bad commands.
	 */
	private void fixData() {
		try {

			// Find top dict index
			int nameIndexStart = this.data[2];
			int nameIndexCount = FontWriter.getUintFromByteArray(this.data, nameIndexStart, 2);
			int nameIndexOffsize = this.data[nameIndexStart + 2];
			int nameIndexEndOffsetLocation = nameIndexStart + 3 + (nameIndexOffsize * nameIndexCount);
			int nameIndexEndOffset = FontWriter.getUintFromByteArray(this.data, nameIndexEndOffsetLocation, nameIndexOffsize);

			// Find top dict
			int dictIndexStart = nameIndexEndOffsetLocation + nameIndexEndOffset + (nameIndexOffsize - 1);
			int topDictIndexCount = FontWriter.getUintFromByteArray(this.data, dictIndexStart, 2);
			if (topDictIndexCount != 1) {
				this.data = this.original;
				return;
			}
			int topDictIndexOffsize = this.data[dictIndexStart + 2];
			int topDictIndexEndOffsetLocation = dictIndexStart + 3 + (topDictIndexOffsize * topDictIndexCount);
			int topDictIndexEndOffset = FontWriter.getUintFromByteArray(this.data, topDictIndexEndOffsetLocation, topDictIndexOffsize);

			// Decode top dict to find some key values
			int topDictDataStart = topDictIndexEndOffsetLocation + topDictIndexOffsize;
			byte[] topDict = new byte[topDictIndexEndOffset - 1];
			System.arraycopy(this.data, topDictDataStart, topDict, 0, topDictIndexEndOffset - 1);
			decodeTopDict(topDict);

			// Check charstrings found
			if (this.charstringOffset == -1) {
				this.data = this.original;
				return;
			}

			// Get data about charstrings
			int charstringsCount = FontWriter.getUintFromByteArray(this.data, this.charstringOffset, 2);
			int charstringsOffsize = this.data[this.charstringOffset + 2];
			int charstringsOffsetsStart = this.charstringOffset + 3;
			int[] charstringOffsets = new int[charstringsCount + 1];
			for (int i = 0; i < charstringsCount + 1; i++)
				charstringOffsets[i] = FontWriter.getUintFromByteArray(this.data, charstringsOffsetsStart + (charstringsOffsize * i),
						charstringsOffsize);
			int charstringDataStart = charstringsOffsetsStart + ((charstringsCount + 1) * charstringsOffsize);

			// Go through charstrings removing bad commands and tracking what changes need to be made to offsets
			int bytesRemoved = 0;
			for (int i = 0; i < charstringsCount; i++) {

				// Cast Charstring to int array so highest byte isn't negative
				int start = charstringDataStart + charstringOffsets[i] - 1;
				int end = charstringDataStart + charstringOffsets[i + 1] - 1;
				int[] c = new int[end - start];
				for (int j = 0; j < c.length; j++) {
					c[j] = this.data[start + j];

					if (c[j] < 0) c[j] += 256;
				}

				// Store pointer and lengths for moving onto next command/number
				int pointer = 0;
				int lastNumLength;
				int lastOpLength;

				while (pointer < c.length) {
					int chunk = c[pointer];

					if (chunk >= 32 && chunk <= 246) { // Number
						lastNumLength = 1;
						lastOpLength = 0;
					}
					else
						if (chunk >= 247 && chunk <= 254) { // Number
							lastNumLength = 2;
							lastOpLength = 0;
						}
						else
							if (chunk == 255) {
								lastNumLength = 5;
								lastOpLength = 0;
							}
							else
								if (chunk == 28) { // Number
									lastNumLength = 3;
									lastOpLength = 0;
								}
								else
									if (chunk == 12) { // Two byte ID's
										if (c[pointer + 1] == 0) { // dotsection command- remove
											System.arraycopy(this.data, start + pointer + 2, this.data, start + pointer, this.data.length
													- (start + pointer + 2));
											System.arraycopy(c, pointer + 2, c, pointer, c.length - (pointer + 2));
											for (int j = i + 1; j < charstringOffsets.length; j++)
												charstringOffsets[j] -= 2;
											bytesRemoved += 2;
											lastOpLength = 0;
										}
										else {
											lastOpLength = 2;
										}
										lastNumLength = 0;
									}
									else {
										lastOpLength = 1; // One byte ID's - we're not interested in these
										lastNumLength = 0;
									}

					pointer += lastOpLength + lastNumLength;
				}

			}

			// Check if anything was fixed and return if not
			if (bytesRemoved == 0) {
				this.data = this.original;
				return;
			}

			// Update the charstring offsets
			for (int i = 0; i < charstringOffsets.length; i++) {
				byte[] newNumber = FontWriter.setUintAsBytes(charstringOffsets[i], charstringsOffsize);
				System.arraycopy(newNumber, 0, this.data, charstringsOffsetsStart + (i * charstringsOffsize), charstringsOffsize);
			}

			// Update the Font DICT offset (if there is one)
			if (this.fdArrayOffset != -1 && this.fdArrayOffsetLength != -1 && this.fdArrayOffsetLocation != -1) {
				byte[] newFdArrayOffset = FontWriter.set1cNumber(this.fdArrayOffset - bytesRemoved);
				newFdArrayOffset = pad1cNumber(this.fdArrayOffsetLength, newFdArrayOffset, this.fdArrayOffset - bytesRemoved);
				System.arraycopy(newFdArrayOffset, 0, this.data, topDictDataStart + this.fdArrayOffsetLocation, this.fdArrayOffsetLength);
			}

			// Update the Private DICT offset (if there is one)
			if (this.privateOffset != -1 && this.privateOffsetLength != -1 && this.privateOffsetLocation != -1) {
				byte[] newPrivateOffset = FontWriter.set1cNumber(this.privateOffset - bytesRemoved);
				newPrivateOffset = pad1cNumber(this.privateOffsetLength, newPrivateOffset, this.privateOffset - bytesRemoved);
				System.arraycopy(newPrivateOffset, 0, this.data, topDictDataStart + this.privateOffsetLocation, this.privateOffsetLength);
			}

			// Create new array and copy data in
			byte[] oldData = this.data;
			this.data = new byte[this.data.length - bytesRemoved];
			System.arraycopy(oldData, 0, this.data, 0, this.data.length);

		}
		catch (Exception e) {
			this.data = this.original;
		}
	}

	/**
	 * Decode the Top DICT for some required values
	 * 
	 * @param dict
	 *            the dictionary
	 */
	private void decodeTopDict(byte[] dict) {

		// Cast to int array so highest byte isn't negative
		int[] d = new int[dict.length];
		for (int i = 0; i < dict.length; i++)
			d[i] = dict[i] < 0 ? dict[i] + 256 : dict[i];

		int pointer = 0;
		int lastNumLength = 0;
		int lastNum = 0;
		int lastOpLength;

		// Run through array picking out only the bits of data we need
		while (pointer < d.length) {
			int chunk = d[pointer];

			if (chunk >= 32 && chunk <= 246) { // Number
				lastNum = chunk - 139;
				lastNumLength = 1;
				lastOpLength = 0;
			}
			else
				if (chunk >= 247 && chunk <= 250) { // Number
					lastNum = ((chunk - 247) * 256) + d[pointer + 1] + 108;
					lastNumLength = 2;
					lastOpLength = 0;
				}
				else
					if (chunk >= 251 && chunk <= 254) { // Number
						lastNum = -((chunk - 251) * 256) - d[pointer + 1] - 108;
						lastNumLength = 2;
						lastOpLength = 0;
					}
					else
						if (chunk == 28) { // Number
							lastNum = FontWriter.getUintFromIntArray(d, pointer + 1, 2);
							lastNumLength = 3;
							lastOpLength = 0;
						}
						else
							if (chunk == 29) { // Number
								lastNum = FontWriter.getUintFromIntArray(d, pointer + 1, 4);
								lastNumLength = 5;
								lastOpLength = 0;
							}
							else
								if (chunk == 30) { // Number
									lastNumLength = 1;
									while (!(((d[pointer + lastNumLength] & 0xF) == 0xF) || ((d[pointer + lastNumLength] & 0x0F) == 0x0F)))
										lastNumLength++;
									lastNumLength++;
									lastOpLength = 0;
								}
								else
									if (chunk == 12) { // Two byte ID's - we're usually not interested in these
										if (d[pointer + 1] == 36) { // Font DICT - we'll probably need to change this offset so store location and
																	// length and value
											this.fdArrayOffsetLocation = pointer - lastNumLength;
											this.fdArrayOffsetLength = lastNumLength;
											this.fdArrayOffset = lastNum;
										}
										lastOpLength = 2;
										lastNumLength = 0;
									}
									else
										if (chunk == 17) { // Charstrings - we need this so we can change the strings
											this.charstringOffset = lastNum;
											lastOpLength = 1;
											lastNumLength = 0;
										}
										else
											if (chunk == 18) { // Private dict - we'll probably need to change this offset so store location and
																// length and value
												this.privateOffsetLocation = pointer - lastNumLength;
												this.privateOffsetLength = lastNumLength;
												this.privateOffset = lastNum;
												lastOpLength = 1;
												lastNumLength = 0;
											}
											else {
												lastOpLength = 1; // Other one byte command - not interested
												lastNumLength = 0;
											}

			pointer += lastOpLength + lastNumLength;
		}
	}

	/**
	 * Pads a type 1c number to take up the required number of bytes.
	 * 
	 * @param byteCount
	 *            The number of bytes required.
	 * @param currentRepresentation
	 *            The current representation of the number.
	 * @param number
	 *            The number to encode.
	 * @return The representation using the required number of bytes.
	 */
	private static byte[] pad1cNumber(int byteCount, byte[] currentRepresentation, int number) {

		// Check if already the same
		if (byteCount == currentRepresentation.length) return currentRepresentation;

		if (byteCount < currentRepresentation.length) throw new IllegalArgumentException("Trying to pad a number to a smaller size.");

		if (byteCount != 2 && byteCount != 3 && byteCount != 5) throw new IllegalArgumentException("Padding to an incorect number of bytes.");

		byte[] result;

		if (byteCount == 2) // must be padding from 1 so just append a zero beforehand
		result = new byte[] { (byte) 139, currentRepresentation[0] };

		else
			if (byteCount == 3) // Generate as 2 byte number
			result = new byte[] { 28, (byte) ((number >> 8) & 0xFF), (byte) (number & 0xFF) };

			else // byteCount must be 5 - Generate as 4 byte number
			result = new byte[] { 29, (byte) ((number >> 24) & 0xFF), (byte) ((number >> 16) & 0xFF), (byte) ((number >> 8) & 0xFF),
					(byte) (number & 0xFF) };

		return result;
	}

	/**
	 * @return the fixed font
	 */
	public byte[] getBytes() {
		return this.data;
	}

}
