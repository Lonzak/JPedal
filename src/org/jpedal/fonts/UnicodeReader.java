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
 * UnicodeReader.java
 * ---------------
 */
package org.jpedal.fonts;

import org.jpedal.utils.LogWriter;

public class UnicodeReader {

	private final static int[] powers = { 1, 16, 256, 256 * 16 };

	static final boolean debugUnicode = false;

	int ptr;

	byte[] data;

	boolean hasDoubleBytes = false;

	public UnicodeReader(byte[] data) {

		this.data = data;
	}

	/**
	 * read unicode translation table
	 */
	public String[] readUnicode() {

		if (this.data == null) return null;

		int defType = 0;

		if (debugUnicode) System.out.println(" Raw data============\n" + new String(this.data) + "\n=========================");

		// initialise unicode holder
		String[] unicodeMappings = new String[65536];

		int length = this.data.length;

		boolean inDef = false;

		// get stream of data
		try {

			// read values into lookup table
			while (true) {

				while (this.ptr < length && this.data[this.ptr] == 9)
					this.ptr++;

				if (this.ptr >= length) break;
				else
					if (this.ptr + 4 < length && this.data[this.ptr] == 'e' && this.data[this.ptr + 1] == 'n' && this.data[this.ptr + 2] == 'd'
							&& this.data[this.ptr + 3] == 'b' && this.data[this.ptr + 4] == 'f') {
						defType = 0;
						inDef = false;
					}
					else
						if (inDef) {

							if (debugUnicode) System.out.println("Read line");

							readLineValue(unicodeMappings, defType);
						}

				if (this.ptr >= length) {
					break;
				}
				else
					if (this.data[this.ptr] == 'b' && this.data[this.ptr + 1] == 'e' && this.data[this.ptr + 2] == 'g'
							&& this.data[this.ptr + 3] == 'i' && this.data[this.ptr + 4] == 'n' && this.data[this.ptr + 5] == 'b'
							&& this.data[this.ptr + 6] == 'f' && this.data[this.ptr + 7] == 'c' && this.data[this.ptr + 8] == 'h'
							&& this.data[this.ptr + 9] == 'a' && this.data[this.ptr + 10] == 'r') {

						defType = 1;
						this.ptr = this.ptr + 10;

						inDef = true;

					}
					else
						if (this.data[this.ptr] == 'b' && this.data[this.ptr + 1] == 'e' && this.data[this.ptr + 2] == 'g'
								&& this.data[this.ptr + 3] == 'i' && this.data[this.ptr + 4] == 'n' && this.data[this.ptr + 5] == 'b'
								&& this.data[this.ptr + 6] == 'f' && this.data[this.ptr + 7] == 'r' && this.data[this.ptr + 8] == 'a'
								&& this.data[this.ptr + 9] == 'n' && this.data[this.ptr + 10] == 'g' && this.data[this.ptr + 11] == 'e') {

							defType = 2;
							this.ptr = this.ptr + 11;

							inDef = true;
						}

				this.ptr++;
			}

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception setting up text object " + e);

		}

		return unicodeMappings;
	}

	private void readLineValue(String[] unicodeMappings, int type) {

		int entryCount = type + 1;

		int dataLen = this.data.length;

		int raw;
		if (debugUnicode) System.out.println("in definition  " + type + " entryCount=" + entryCount);

		// read 2 values
		int[] value = new int[2000];
		boolean isMultipleValues = false;

		for (int vals = 0; vals < entryCount; vals++) {

			if (!isMultipleValues) {
				while (this.ptr < this.data.length && this.data[this.ptr] != '<') { // read up to

					if (vals == 2 && entryCount == 3 && this.data[this.ptr] == '[') { // mutiple values inside []

						type = 4;

						int ii = this.ptr;
						while (this.data[ii] != ']') {
							if (this.data[ii] == '<') entryCount++;

							ii++;
						}

						// needs to be 1 less to make it work
						entryCount--;

						// vals=entryCount;
						// break;
					}

					this.ptr++;
				}

				this.ptr++; // skip past
			}

			// find end
			int count = 0, charsFound = 0;

			while (this.ptr < dataLen && this.data[this.ptr] != '>') {

				if (this.data[this.ptr] != 10 && this.data[this.ptr] != 13 && this.data[this.ptr] != 32) charsFound++;

				this.ptr++;
				count++;

				// allow for multiple values
				if (charsFound == 5) {

					count = 4;
					this.ptr--;

					entryCount++;
					isMultipleValues = true;
					break;
				}
			}

			int pos = 0;

			for (int jj = 0; jj < count; jj++) {
				// convert to number
				while (true) {
					raw = this.data[this.ptr - 1 - jj];

					if (raw != 10 && raw != 13 && raw != 32) break;

					jj++;
				}

				if (raw >= 'A' && raw <= 'F') {
					raw = raw - 55;
				}
				else
					if (raw >= 'a' && raw <= 'f') {
						raw = raw - 87;
					}
					else
						if (raw >= '0' && raw <= '9') {
							raw = raw - 48;
						}
						else throw new RuntimeException("Unexpected number " + (char) raw);

				value[vals] = value[vals] + (raw * powers[pos]);

				if (pos == 3 && debugUnicode) System.out.println("read value (" + vals + ")=" + value[vals] + " (" + vals + ')' + " Hex="
						+ Integer.toHexString(value[vals]) + " char=" + (char) value[vals]);

				pos++;
			}
		}

		// roll to end end so works
		while (this.ptr < dataLen
				&& (this.data[this.ptr] == 62 || this.data[this.ptr] == 32 || this.data[this.ptr] == 10 || this.data[this.ptr] == 13 || this.data[this.ptr] == ']'))
			this.ptr++;

		this.ptr--;

		if (debugUnicode) System.out.println("fill entryCount=" + entryCount + " defType=" + type + " ");

		// put into array
		fillValues(unicodeMappings, entryCount, value, type);
	}

	private void fillValues(String[] unicodeMappings, int entryCount, int[] value, int type) {

		int intValue;

		switch (type) {

			case 1:

				if (entryCount == 2) {
					if (value[type] > 0) {
						unicodeMappings[value[0]] = String.valueOf((char) value[type]);
						if (value[0] > 255) this.hasDoubleBytes = true;
					}
					if (debugUnicode) System.out.println("2=" + unicodeMappings[value[0]]);

				}
				else {

					char str[] = new char[entryCount - 1];

					for (int aa = 0; aa < entryCount - 1; aa++)
						str[aa] = (char) value[type + aa];

					unicodeMappings[value[0]] = new String(str);
					if (value[0] > 255) this.hasDoubleBytes = true;

					if (debugUnicode) System.out.println("3=" + unicodeMappings[value[0]]);

				}

				break;

			case 4:

				// ptr++;

				int j = 2;
				for (int i = value[0]; i < value[1] + 1; i++) {

					if (entryCount > 1 && value[0] == value[1]) { // allow for <02> <02> [<0066006C>]

						unicodeMappings[i] = String.valueOf((char) (value[2]));
						if (i > 255) this.hasDoubleBytes = true;

						for (int jj = 1; jj < entryCount; jj++) {
							unicodeMappings[i] = unicodeMappings[i] + String.valueOf((char) (value[2 + jj]));
							if (i > 255) this.hasDoubleBytes = true;
						}
						// System.out.println("val="+value[0]+" "+unicodeMappings[i]+" value[0]="+value[0]+" value[1]="+value[1]+" value[2]="+value[2]+" value[3]="+value[3]);

					}
					else {
						// read next value

						intValue = value[j];
						j++;
						if (intValue > 0) { // ignore 0 to fix issue in Dalim files
							unicodeMappings[i] = String.valueOf((char) (intValue));
							if (i > 255) this.hasDoubleBytes = true;

							if (debugUnicode) System.out.println(i + "=" + unicodeMappings[i] + " (4)");

						}

						type = 0;
					}
				}

				break;

			default:

				for (int i = value[0]; i < value[1] + 1; i++) {
					intValue = value[type] + i - value[0];
					if (intValue > 0) { // ignore 0 to fix issue in Dalim files
						unicodeMappings[i] = String.valueOf((char) (intValue));
						if (i > 255) this.hasDoubleBytes = true;
					}
				}

				break;
		}
	}

	public boolean hasDoubleByteValues() {
		return this.hasDoubleBytes;
	}

}
