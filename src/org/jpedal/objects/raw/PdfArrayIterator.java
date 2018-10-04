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
 * PdfArrayIterator.java
 * ---------------
 */
package org.jpedal.objects.raw;

import java.util.Arrays;

import org.jpedal.fonts.StandardFonts;
import org.jpedal.utils.NumberUtils;
import org.jpedal.utils.StringUtils;

/**
 * allow fast access to data from PDF object
 * 
 */
public class PdfArrayIterator {

	public static final int TYPE_KEY_INTEGER = 1;
	public static final int TYPE_VALUE_INTEGER = 2;

	byte[][] rawData = null;

	// used for Font chars
	boolean hasHexChars = false;

	int tokenCount = 0, currentToken = 0, spaceChar = -1;

	public PdfArrayIterator(byte[][] rawData) {
		this.rawData = rawData;

		if (rawData != null) this.tokenCount = rawData.length;
	}

	public PdfArrayIterator(String colorspaceObject) {
		byte[][] rawData = new byte[1][];
		rawData[0] = StringUtils.toBytes(colorspaceObject);

		this.tokenCount = 1;
	}

	public boolean hasMoreTokens() {
		return this.currentToken < this.tokenCount;
	}

	// return type (ie PdfArrayIterator.TYPE_KEY_INTEGER)
	public int getNextValueType() {

		// allow for non-valid
		if (this.rawData == null || this.rawData[this.currentToken] == null || this.rawData[this.currentToken].length == 0) return PdfDictionary.Unknown;
		else { // look at first char
			int firstByte;

			/**
			 * assume number and exit as soon as disproved
			 */
			int len = this.rawData[this.currentToken].length;
			boolean isNumber = true;
			for (int ii = 0; ii < len; ii++) {
				firstByte = this.rawData[this.currentToken][ii];

				if (firstByte >= 47 && firstByte < 58) { // is between 0 and 9
					// / or number
				}
				else {
					ii = len;
					isNumber = false;
				}
			}

			if (isNumber) {
				if (this.rawData[this.currentToken][0] != '/') return TYPE_KEY_INTEGER;
				else return TYPE_VALUE_INTEGER;
			}
			else return PdfDictionary.Unknown;
		}
	}

	/**
	 * should only be used with Font Object
	 */
	public String getNextValueAsFontChar(int pointer, boolean containsHexNumbers, boolean allNumbers) {

		String value;

		if (this.currentToken < this.tokenCount) {

			// allow for non-valid
			if (this.rawData == null || this.rawData[this.currentToken] == null || this.rawData[this.currentToken].length == 0) throw new RuntimeException(
					"NullValue exception with PdfArrayIterator");

			// lose / at start
			int length = this.rawData[this.currentToken].length - 1;

			byte[] raw = new byte[length];

			System.arraycopy(this.rawData[this.currentToken], 1, raw, 0, length);

			// ////////////////////////////////////////////////////
			// /getNextValueAsFontChar
			// ensure its a glyph and not a number
			value = new String(raw);
			value = StandardFonts.convertNumberToGlyph(value, containsHexNumbers, allNumbers);

			char c = value.charAt(0);
			if (c == 'B' || c == 'c' || c == 'C' || c == 'G') {
				int i = 1, l = value.length();
				while (!this.hasHexChars && i < l)
					this.hasHexChars = Character.isLetter(value.charAt(i++));
			}
			// ////////////////////////////////////////////////////

			// see if space
			if (raw.length == 5 && raw[0] == 's' && raw[1] == 'p' && raw[2] == 'a' && raw[3] == 'c' && raw[4] == 'e') this.spaceChar = pointer;

			this.currentToken++;
		}
		else throw new RuntimeException("Out of range exception with PdfArrayIterator");

		return value;
	}

	public float getNextValueAsFloat() {

		if (this.currentToken < this.tokenCount) {

			// allow for non-valid
			if (this.rawData == null || this.rawData[this.currentToken] == null || this.rawData[this.currentToken].length == 0) throw new RuntimeException(
					"NullValue exception with PdfArrayIterator");

			byte[] raw = this.rawData[this.currentToken];

			this.currentToken++;

			// return null as 0
			if (raw[0] == 'n' && raw[1] == 'u' && raw[2] == 'l' && raw[2] == 'l') return 0;
			else return NumberUtils.parseFloat(0, raw.length, raw);

		}
		else throw new RuntimeException("Out of range exception with PdfArrayIterator");
	}

	public int getNextValueAsInteger() {
		return getNextValueAsInteger(true);
	}

	public int getNextValueAsInteger(boolean rollOn) {

		if (this.currentToken < this.tokenCount) {

			// allow for non-valid
			if (this.rawData == null || this.rawData[this.currentToken] == null || this.rawData[this.currentToken].length == 0) throw new RuntimeException(
					"NullValue exception with PdfArrayIterator");

			byte[] raw = this.rawData[this.currentToken];

			if (rollOn) this.currentToken++;

			// skip / which can be on some values
			int i = 0;
			if (raw[0] == '/') i++;
			return NumberUtils.parseInt(i, raw.length, raw);

		}
		else throw new RuntimeException("Out of range exception with PdfArrayIterator");
	}

	public float[] getNextValueAsFloatArray() {

		if (this.currentToken < this.tokenCount) {

			// allow for non-valid
			if (this.rawData == null || this.rawData[this.currentToken] == null || this.rawData[this.currentToken].length == 0) throw new RuntimeException(
					"NullValue exception with PdfArrayIterator");

			byte[] raw = this.rawData[this.currentToken];

			this.currentToken++;

			// return null as 0
			if (raw[0] == 'n' && raw[1] == 'u' && raw[2] == 'l' && raw[2] == 'l') return new float[1];
			else {

				int length = raw.length;
				int elementCount = 1, elementReached = 0;
				/**
				 * first work out number of elements by counting spaces to end
				 */
				for (int ii = 1; ii < length; ii++) {
					if ((raw[ii] == ' ' || raw[ii] == 10 || raw[ii] == 13) && (raw[ii - 1] != ' ' && raw[ii - 1] == 10 && raw[ii - 1] == 13)) elementCount++;
				}

				/**
				 * now create and populate
				 */
				float[] values = new float[elementCount];
				int start;
				for (int ii = 0; ii < length; ii++) {
					while (ii < length && (raw[ii] == ' ' || raw[ii] == 10 || raw[ii] == 13)) {
						ii++;
					}
					start = ii;
					while (ii < length && (raw[ii] != ' ' && raw[ii] != 10 && raw[ii] != 13)) {
						ii++;
					}
					values[elementReached] = NumberUtils.parseFloat(start, ii, raw);
					elementReached++;

				}

				return values;
			}

		}
		else throw new RuntimeException("Out of range exception with PdfArrayIterator");
	}

	public int getNextValueAsConstant(boolean moveToNextAfter) {

		if (this.currentToken < this.tokenCount) {

			// allow for non-valid
			if (this.rawData == null || this.rawData[this.currentToken] == null || this.rawData[this.currentToken].length == 0) throw new RuntimeException(
					"NullValue exception with PdfArrayIterator");

			byte[] raw = this.rawData[this.currentToken];

			if (moveToNextAfter) this.currentToken++;

			return PdfDictionary.getIntKey(1, raw.length - 1, raw);

		}
		else throw new RuntimeException("Out of range exception with PdfArrayIterator");
	}

	public int getSpaceChar() {
		return this.spaceChar;
	}

	public boolean hasHexChars() {
		return this.hasHexChars;
	}

	public int getTokenCount() {
		return this.tokenCount;
	}

	/** returns the next value as a string, and if <b>rollon</b> is true moves the count onto the next token */
	public String getNextValueAsString(boolean rollon) {

		String value = "";

		if (this.currentToken < this.tokenCount) {

			// allow for non-valid
			if (this.rawData == null) throw new RuntimeException("Null Value exception with PdfArrayIterator rawData=" + this.rawData);
			// else if(rawData[currentToken]==null)
			// throw new RuntimeException("Null Value exception with PdfArrayIterator rawData="+rawData);

			byte[] raw = this.rawData[this.currentToken];
			if (raw != null) value = new String(raw);

			if (rollon) this.currentToken++;
		}
		else throw new RuntimeException("Out of range exception with PdfArrayIterator");

		return value;
	}

	/** returns the next value as a byte[], and if <b>rollon</b> is true moves the count onto the next token */
	public byte[] getNextValueAsByte(boolean rollon) {

		byte[] value = null;

		if (this.currentToken < this.tokenCount) {

			// allow for non-valid
			if (this.rawData == null) throw new RuntimeException("Null Value exception with PdfArrayIterator rawData=" + this.rawData);
			// else if(rawData[currentToken]==null)
			// throw new RuntimeException("Null Value exception with PdfArrayIterator rawData="+rawData);

			byte[] raw = this.rawData[this.currentToken];
			if (raw != null) {
				int length = raw.length;
				value = new byte[length];
				System.arraycopy(raw, 0, value, 0, length);
			}

			if (rollon) this.currentToken++;
		}
		else throw new RuntimeException("Out of range exception with PdfArrayIterator");

		return value;
	}

	public int getNextValueAsKey() {

		if (this.currentToken < this.tokenCount) {

			// allow for non-valid
			if (this.rawData == null || this.rawData[this.currentToken] == null || this.rawData[this.currentToken].length == 0) throw new RuntimeException(
					"NullValue exception with PdfArrayIterator");

			byte[] raw = this.rawData[this.currentToken];
			this.currentToken++;

			// System.out.println("String="+new String(raw));

			return PdfDictionary.getIntKey(0, raw.length, raw);

		}
		else throw new RuntimeException("Out of range exception with PdfArrayIterator");
	}

	public boolean isNextValueRef() {

		boolean isRef;

		if (this.currentToken < this.tokenCount) {

			// allow for non-valid
			if (this.rawData == null || this.rawData[this.currentToken] == null || this.rawData[this.currentToken].length == 0) return false;

			byte[] raw = this.rawData[this.currentToken];
			isRef = raw[raw.length - 1] == 'R';

		}
		else throw new RuntimeException("Out of range exception with PdfArrayIterator");

		return isRef;
	}

	public void resetToStart() {

		if (this.rawData != null) this.tokenCount = this.rawData.length;

		this.currentToken = 0;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PdfArrayIterator [");
		if (this.rawData != null) {
			builder.append("rawData=");
			for (int i = 0; i < this.rawData.length; i++) {
				builder.append(new String(this.rawData[i]));
				builder.append(", ");
			}
			builder.append("], ");
		}
		builder.append("hasHexChars=");
		builder.append(this.hasHexChars);
		builder.append(", tokenCount=");
		builder.append(this.tokenCount);
		builder.append(", currentToken=");
		builder.append(this.currentToken);
		builder.append(", spaceChar=");
		builder.append(this.spaceChar);
		builder.append("]");
		return builder.toString();
	}
}
