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
 * ArrayDecoder.java
 * ---------------
 */
package org.jpedal.io;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.exception.PdfSecurityException;
import org.jpedal.objects.raw.OCObject;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.NumberUtils;
import org.jpedal.utils.StringUtils;

/**
 * parse PDF array data from PDF
 */
public class ArrayDecoder extends ObjectDecoder {

	private static final long serialVersionUID = 8797304649862134871L;
	// now create array and read values
	private float[] floatValues = null;
	private int[] intValues = null;
	private double[] doubleValues = null;
	private byte[][] mixedValues = null;
	private byte[][] keyValues = null;
	private byte[][] stringValues = null;
	private boolean[] booleanValues = null;
	private Object[] objectValues = null;

	private int i;
	private int endPoint;
	private int type;

	private int keyReached = -1;

	private Object[] objectValuesArray = null;

	public ArrayDecoder(PdfFileReader pdfFileReader, int i, int endPoint, int type) {
		super(pdfFileReader);

		this.i = i;
		this.endPoint = endPoint;
		this.type = type;
	}

	public ArrayDecoder(PdfFileReader pdfFileReader, int i, int endPoint, int type, Object[] objectValuesArray, int keyReached) {
		super(pdfFileReader);

		this.i = i;
		this.endPoint = endPoint;
		this.type = type;
		this.objectValuesArray = objectValuesArray;
		this.keyReached = keyReached;
	}

	public int readArray(boolean ignoreRecursion, byte[] raw, PdfObject pdfObject, int PDFkeyInt) {

		// roll on
		if (this.type == PdfDictionary.VALUE_IS_KEY_ARRAY && raw[this.i] == 60) {
			// i--;
		}
		else
			if (raw[this.i] != 91 && raw[this.i] != '<') this.i++;

		// ignore empty
		if (raw[this.i] == '[' && raw[this.i + 1] == ']') return this.i + 1;

		Map isRef = new HashMap();

		boolean isHexString = false;

		boolean alwaysRead = (PDFkeyInt == PdfDictionary.Kids || PDFkeyInt == PdfDictionary.Annots);

		final boolean debugArray = debugFastCode;// || type==PdfDictionary.VALUE_IS_OBJECT_ARRAY;

		if (debugArray) System.out.println(padding + "Reading array type=" + PdfDictionary.showArrayType(this.type) + " into " + pdfObject + ' '
				+ (char) raw[this.i] + ' ' + (char) raw[this.i + 1] + ' ' + (char) raw[this.i + 2] + ' ' + (char) raw[this.i + 3] + ' '
				+ (char) raw[this.i + 4]);

		int currentElement = 0, elementCount = 0, keyStart;

		// move cursor to start of text
		while (raw[this.i] == 10 || raw[this.i] == 13 || raw[this.i] == 32)
			this.i++;

		// allow for comment
		if (raw[this.i] == 37) skipComment(raw);

		keyStart = this.i;

		// work out if direct or read ref ( [values] or ref to [values])
		int j2 = this.i;
		byte[] arrayData = raw;

		// may need to add method to PdfObject is others as well as Mask
		boolean isIndirect = raw[this.i] != 91 && raw[this.i] != '(' && (PDFkeyInt != PdfDictionary.Mask && PDFkeyInt != PdfDictionary.TR &&
		// pdfObject.getObjectType()!=PdfDictionary.ColorSpace &&
				raw[0] != 0); // 0 never occurs but we set as flag if called from gotoDest/DefaultActionHandler

		// allow for /Contents null
		if (raw[this.i] == 'n' && raw[this.i + 1] == 'u' && raw[this.i + 2] == 'l' && raw[this.i + 2] == 'l') {
			isIndirect = false;
			elementCount = 1;
		}

		// check indirect and not [/DeviceN[/Cyan/Magenta/Yellow/Black]/DeviceCMYK 36 0 R]
		if (isIndirect) isIndirect = handleIndirect(this.endPoint, raw, debugArray);

		if (debugArray && isIndirect) System.out.println(padding + "Indirect ref");

		boolean isSingleKey = false, isSingleDirectValue = false; // flag to show points to Single value (ie /FlateDecode)
		boolean isSingleNull = true;
		int endPtr = -1;

		if ((raw[this.i] == 47 || raw[this.i] == '(' || raw[this.i] == '<' || (raw[this.i] == '<' && raw[this.i + 1] == 'f' && raw[this.i + 2] == 'e')
				&& raw[this.i + 3] == 'f' && raw[this.i + 4] == 'f')
				&& this.type != PdfDictionary.VALUE_IS_STRING_ARRAY && PDFkeyInt != PdfDictionary.TR) { // single value ie /Filter /FlateDecode or
																										// (text)

			elementCount = 1;
			isSingleKey = true;

			if (debugArray) System.out.println(padding + "Direct single value with /");
		}
		else {

			int endI = -1;// allow for jumping back to single value (ie /Contents 12 0 R )

			if (isIndirect) {

				if (debugArray) System.out.println(padding + "------reading data----");

				// allow for indirect to 1 item
				int startI = this.i;

				if (debugArray) System.out.print(padding + "Indirect object ref=");

				// move cursor to end of ref
				while (raw[this.i] != 10 && raw[this.i] != 13 && raw[this.i] != 32 && raw[this.i] != 47 && raw[this.i] != 60 && raw[this.i] != 62) {
					this.i++;
				}

				// actual value or first part of ref
				int ref = NumberUtils.parseInt(keyStart, this.i, raw);

				// move cursor to start of generation
				while (raw[this.i] == 10 || raw[this.i] == 13 || raw[this.i] == 32 || raw[this.i] == 47 || raw[this.i] == 60)
					this.i++;

				// get generation number
				keyStart = this.i;
				// move cursor to end of reference
				while (raw[this.i] != 10 && raw[this.i] != 13 && raw[this.i] != 32 && raw[this.i] != 47 && raw[this.i] != 60 && raw[this.i] != 62)
					this.i++;

				int generation = NumberUtils.parseInt(keyStart, this.i, raw);

				if (debugFastCode) System.out.print(padding + " ref=" + ref + " generation=" + generation + '\n');

				// check R at end of reference and abort if wrong
				// move cursor to start of R
				while (raw[this.i] == 10 || raw[this.i] == 13 || raw[this.i] == 32 || raw[this.i] == 47 || raw[this.i] == 60)
					this.i++;

				if (raw[this.i] != 82) // we are expecting R to end ref
				throw new RuntimeException(padding + "4. Unexpected value " + (char) raw[this.i]
						+ " in file - please send to IDRsolutions for analysis");

				if (ignoreRecursion && !alwaysRead) {

					if (debugArray) System.out.println(padding + "Ignore sublevels");
					return this.i;
				}

				// read the Dictionary data
				arrayData = this.objectReader.readObjectAsByteArray(pdfObject, this.objectReader.isCompressed(ref, generation), ref, generation);

				// allow for data in Linear object not yet loaded
				if (arrayData == null) {
					pdfObject.setFullyResolved(false);

					if (debugFastCode) System.out.println(padding + "Data not yet loaded");

					if (LogWriter.isOutput()) LogWriter.writeLog("[Linearized] " + pdfObject.getObjectRefAsString() + " not yet available (14)");

					return raw.length;
				}

				// lose obj at start and roll onto [
				j2 = 0;
				while (arrayData[j2] != 91) {

					// allow for % comment
					if (arrayData[j2] == '%') {
						while (true) {
							j2++;
							if (arrayData[j2] == 13 || arrayData[j2] == 10) break;
						}
						while (arrayData[j2] == 13 || arrayData[j2] == 10)
							j2++;

						// roll back as [ may be next char
						j2--;
					}

					// allow for null
					if (arrayData[j2] == 'n' && arrayData[j2 + 1] == 'u' && arrayData[j2 + 2] == 'l' && arrayData[j2 + 3] == 'l') break;

					if (arrayData[j2] == 47) { // allow for value of type 32 0 obj /FlateDecode endob
						j2--;
						isSingleDirectValue = true;
						break;
					}
					if ((arrayData[j2] == '<' && arrayData[j2 + 1] == '<')
							|| ((j2 + 4 < arrayData.length) && arrayData[j2 + 3] == '<' && arrayData[j2 + 4] == '<')) { // also check ahead to pick up
																														// [<<
						endI = this.i;

						j2 = startI;
						arrayData = raw;

						if (debugArray) System.out.println(padding + "Single value, not indirect");

						break;
					}

					j2++;
				}
			}

			if (j2 < 0) // avoid exception
			j2 = 0;

			// skip [ and any spaces allow for [[ in recursion
			boolean startFound = false;

			while (arrayData[j2] == 10 || arrayData[j2] == 13 || arrayData[j2] == 32 || (arrayData[j2] == 91 && !startFound)) {// (type!=PdfDictionary.VALUE_IS_OBJECT_ARRAY
																																// ||
																																// objectValuesArray==null)))

				if (arrayData[j2] == 91) startFound = true;

				j2++;
			}

			// count number of elements
			endPtr = j2;
			boolean charIsSpace, lastCharIsSpace = true, isRecursive;
			int arrayEnd = arrayData.length;
			if (debugArray) System.out.println(padding + "----counting elements----arrayData[endPtr]=" + arrayData[endPtr] + " type=" + this.type);

			while (endPtr < arrayEnd && arrayData[endPtr] != 93) {

				isRecursive = false;

				// allow for embedded objects
				while (true) {

					if (arrayData[endPtr] == '<' && arrayData[endPtr + 1] == '<') {
						int levels = 1;

						elementCount++;

						if (debugArray) System.out.println(padding + "Direct value elementCount=" + elementCount);

						while (levels > 0) {
							endPtr++;

							if (arrayData[endPtr] == '<' && arrayData[endPtr + 1] == '<') {
								endPtr++;
								levels++;
							}
							else
								if (arrayData[endPtr] == '>' && arrayData[endPtr - 1] == '>') {
									endPtr++;
									levels--;
								}
						}

						if (this.type == PdfDictionary.VALUE_IS_KEY_ARRAY) endPtr--;

					}
					else break;
				}

				// allow for null (not Mixed!)
				if (this.type != PdfDictionary.VALUE_IS_MIXED_ARRAY && arrayData[endPtr] == 'n' && arrayData[endPtr + 1] == 'u'
						&& arrayData[endPtr + 2] == 'l' && arrayData[endPtr + 3] == 'l') {

					// get next legit value and make sure not only value if layer or Order
					// to handle bum null values in Layers on some files
					byte nextChar = 93;
					if (PDFkeyInt == PdfDictionary.Layer || PDFkeyInt == PdfDictionary.Order) {
						for (int aa = endPtr + 3; aa < arrayData.length; aa++) {
							if (arrayData[aa] == 10 || arrayData[aa] == 13 || arrayData[aa] == 32 || arrayData[aa] == 9) {}
							else {
								nextChar = arrayData[aa];
								aa = arrayData.length;
							}
						}
					}

					if (nextChar == 93) {
						isSingleNull = true;
						elementCount = 1;
						break;
					}
					else { // ignore null value
						isSingleNull = false;
						// elementCount++;
						endPtr = endPtr + 4;
						lastCharIsSpace = true;

						if (debugArray) System.out.println("ignore null");

						continue;
					}
				}

				if (isSingleDirectValue && (arrayData[endPtr] == 32 || arrayData[endPtr] == 13 || arrayData[endPtr] == 10)) break;

				if (endI != -1 && endPtr > endI) break;

				if (this.type == PdfDictionary.VALUE_IS_KEY_ARRAY) {

					if (arrayData[endPtr] == 'R'
							|| ((PDFkeyInt == PdfDictionary.TR || PDFkeyInt == PdfDictionary.Category) && arrayData[endPtr] == '/')) elementCount++;

				}
				else {

					// handle (string)
					if (arrayData[endPtr] == '(') {
						elementCount++;

						if (debugArray) System.out.println(padding + "string");
						while (true) {
							if (arrayData[endPtr] == ')' && !ObjectUtils.isEscaped(arrayData, endPtr)) break;

							endPtr++;

							lastCharIsSpace = true; // needs to be space for code to work eve if no actual space
						}
					}
					else
						if (arrayData[endPtr] == '<') {
							elementCount++;

							if (debugArray) System.out.println(padding + "direct");
							while (true) {
								if (arrayData[endPtr] == '>') break;

								endPtr++;

								lastCharIsSpace = true; // needs to be space for code to work eve if no actual space
							}
						}
						else
							if (arrayData[endPtr] == 91) { // handle recursion

								elementCount++;

								if (debugArray) System.out.println(padding + "recursion");
								int level = 1;

								while (true) {

									endPtr++;

									if (endPtr == arrayData.length) break;

									if (arrayData[endPtr] == 93) level--;
									else
										if (arrayData[endPtr] == 91) level++;

									if (level == 0) break;
								}

								isRecursive = true;
								lastCharIsSpace = true; // needs to be space for code to work eve if no actual space

							}
							else {

								charIsSpace = arrayData[endPtr] == 10 || arrayData[endPtr] == 13 || arrayData[endPtr] == 32
										|| arrayData[endPtr] == 47;

								if (lastCharIsSpace && !charIsSpace) {
									if ((this.type == PdfDictionary.VALUE_IS_MIXED_ARRAY || this.type == PdfDictionary.VALUE_IS_OBJECT_ARRAY)
											&& arrayData[endPtr] == 'R' && arrayData[endPtr - 1] != '/') { // adjust so returns correct count /R and
																											// on 12 0 R
										elementCount--;

										isRef.put(elementCount - 1, "x");

										if (debugArray) System.out.println(padding + "aref " + (char) arrayData[endPtr]);
									}
									else elementCount++;

								}
								lastCharIsSpace = charIsSpace;
							}
				}

				// allow for empty array [ ]
				if (!isRecursive && endPtr < arrayEnd && arrayData[endPtr] == 93 && this.type != PdfDictionary.VALUE_IS_KEY_ARRAY) {

					// get first char
					int ptr = endPtr - 1;
					while (arrayData[ptr] == 13 || arrayData[ptr] == 10 || arrayData[ptr] == 32)
						ptr--;

					if (arrayData[ptr] == '[') // if empty reset
					elementCount = 0;
					break;
				}

				endPtr++;
			}

			if (debugArray) System.out.println(padding + "Number of elements=" + elementCount + " rawCount=");

			if (elementCount == 0 && debugArray) System.out.println(padding + "zero elements found!!!!!!");

		}

		if (ignoreRecursion && !alwaysRead) return endPtr;

		// setup the correct array to size
		initObjectArray(elementCount);

		/**
		 * read all values and convert
		 */
		// if(isSingleNull && arrayData[j2]=='n' && arrayData[j2+1]=='u' &&
		if (arrayData[j2] == 'n' && arrayData[j2 + 1] == 'u' && arrayData[j2 + 2] == 'l' && arrayData[j2 + 3] == 'l' && isSingleNull
				&& (this.type != PdfDictionary.VALUE_IS_OBJECT_ARRAY || elementCount == 1)) {

			j2 = j2 + 3;

			if (this.type == PdfDictionary.VALUE_IS_MIXED_ARRAY) this.mixedValues[currentElement] = null;
			else
				if (this.type == PdfDictionary.VALUE_IS_KEY_ARRAY) this.keyValues[currentElement] = null;
				else
					if (this.type == PdfDictionary.VALUE_IS_STRING_ARRAY) this.stringValues[currentElement] = null;
					else
						if (this.type == PdfDictionary.VALUE_IS_OBJECT_ARRAY) this.objectValues[currentElement] = null;

		}
		else j2 = setValue(ignoreRecursion, raw, pdfObject, PDFkeyInt, isRef, isHexString, debugArray, currentElement, elementCount, j2, arrayData,
				isSingleKey, endPtr);

		// put cursor in correct place (already there if ref)
		if (!isIndirect) this.i = j2;

		// set value in PdfObject
		if (this.type == PdfDictionary.VALUE_IS_FLOAT_ARRAY) pdfObject.setFloatArray(PDFkeyInt, this.floatValues);
		else
			if (this.type == PdfDictionary.VALUE_IS_INT_ARRAY) pdfObject.setIntArray(PDFkeyInt, this.intValues);
			else
				if (this.type == PdfDictionary.VALUE_IS_BOOLEAN_ARRAY) pdfObject.setBooleanArray(PDFkeyInt, this.booleanValues);
				else
					if (this.type == PdfDictionary.VALUE_IS_DOUBLE_ARRAY) pdfObject.setDoubleArray(PDFkeyInt, this.doubleValues);
					else
						if (this.type == PdfDictionary.VALUE_IS_MIXED_ARRAY) pdfObject.setMixedArray(PDFkeyInt, this.mixedValues);
						else
							if (this.type == PdfDictionary.VALUE_IS_KEY_ARRAY) setKeyArrayValue(pdfObject, PDFkeyInt, elementCount);
							else
								if (this.type == PdfDictionary.VALUE_IS_STRING_ARRAY) pdfObject.setStringArray(PDFkeyInt, this.stringValues);
								else
									if (this.type == PdfDictionary.VALUE_IS_OBJECT_ARRAY) setObjectArrayValue(pdfObject, PDFkeyInt,
											this.objectValuesArray, this.keyReached, debugArray);

		if (debugArray) showValues();

		// roll back so loop works if no spaces
		if (this.i < raw.length && (raw[this.i] == 47 || raw[this.i] == 62 || (raw[this.i] >= '0' && raw[this.i] <= '9'))) this.i--;

		return this.i;
	}

	private int setValue(boolean ignoreRecursion, byte[] raw, PdfObject pdfObject, int PDFkeyInt, Map ref, boolean hexString, boolean debugArray,
			int currentElement, int elementCount, int j2, byte[] arrayData, boolean singleKey, int endPtr) {

		int keyStart;// /read values

		while (arrayData[j2] != 93) {

			if (endPtr > -1 && j2 >= endPtr) break;

			// move cursor to start of text
			while (arrayData[j2] == 10 || arrayData[j2] == 13 || arrayData[j2] == 32 || arrayData[j2] == 47)
				j2++;

			keyStart = j2;

			if (debugArray) System.out.print("j2=" + j2 + " value=" + (char) arrayData[j2]);

			boolean isKey = arrayData[j2 - 1] == '/';
			boolean isRecursiveValue = false; // flag to show if processed in top part so ignore second part

			// move cursor to end of text
			if (this.type == PdfDictionary.VALUE_IS_KEY_ARRAY
					|| ((this.type == PdfDictionary.VALUE_IS_MIXED_ARRAY || this.type == PdfDictionary.VALUE_IS_OBJECT_ARRAY) && (ref
							.containsKey(currentElement) || (PDFkeyInt == PdfDictionary.Order && arrayData[j2] >= '0' && arrayData[j2] <= '9') || (arrayData[j2] == '<' && arrayData[j2 + 1] == '<')))) {

				if (debugArray) System.out.println("ref currentElement=" + currentElement);

				while (arrayData[j2] != 'R' && arrayData[j2] != ']') {

					// allow for embedded object
					if (arrayData[j2] == '<' && arrayData[j2 + 1] == '<') {
						int levels = 1;

						if (debugArray) System.out.println(padding + "Reading Direct value");

						while (levels > 0) {
							j2++;

							if (arrayData[j2] == '<' && arrayData[j2 + 1] == '<') {
								j2++;
								levels++;
							}
							else
								if (arrayData[j2] == '>' && arrayData[j2 + 1] == '>') {
									j2++;
									levels--;
								}
						}
						break;
					}

					if (isKey && PDFkeyInt == PdfDictionary.TR && arrayData[j2 + 1] == ' ') break;

					j2++;
				}
				j2++;

			}
			else {

				// handle (string)
				if (arrayData[j2] == '(') {

					keyStart = j2 + 1;
					while (true) {
						if (arrayData[j2] == ')' && !ObjectUtils.isEscaped(arrayData, j2)) break;

						j2++;
					}

					hexString = false;

				}
				else
					if (arrayData[j2] == '[' && this.type == PdfDictionary.VALUE_IS_MIXED_ARRAY && PDFkeyInt == PdfDictionary.Names) { // [59 0 R /XYZ
																																		// null 711
																																		// null ]

						keyStart = j2;
						while (true) {
							if (arrayData[j2] == ']') break;

							j2++;

						}

						// include end bracket
						j2++;

					}
					else
						if (arrayData[j2] == '<') {

							hexString = true;
							keyStart = j2 + 1;
							while (true) {
								if (arrayData[j2] == '>') break;

								if (arrayData[j2] == '/') hexString = false;

								j2++;

							}

						}
						else
							if (arrayData[j2] == 91 && this.type == PdfDictionary.VALUE_IS_OBJECT_ARRAY) {

								// find end
								int j3 = j2 + 1;
								int level = 1;

								while (true) {

									j3++;

									if (j3 == arrayData.length) break;

									if (arrayData[j3] == 93) level--;
									else
										if (arrayData[j3] == 91) level++;

									if (level == 0) break;
								}
								j3++;

								if (debugArray) padding = padding + "   ";

								ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, j2, j3, this.type, this.objectValues, currentElement);
								j2 = objDecoder.readArray(ignoreRecursion, arrayData, pdfObject, PDFkeyInt);

								if (debugArray) {
									int len = padding.length();

									if (len > 3) padding = padding.substring(0, len - 3);
								}

								if (arrayData[j2] != '[') j2++;

								isRecursiveValue = true;

								while (j2 < arrayData.length && arrayData[j2] == ']')
									j2++;

							}
							else
								if (!isKey && elementCount - currentElement == 1 && this.type == PdfDictionary.VALUE_IS_MIXED_ARRAY) { // if last
																																		// value just
																																		// read to end
																																		// in case 1 0
																																		// R

									while (arrayData[j2] != 93 && arrayData[j2] != 47) {

										if (arrayData[j2] == 62 && arrayData[j2 + 1] == 62) break;

										j2++;
									}
								}
								else
									if (this.type == PdfDictionary.VALUE_IS_OBJECT_ARRAY && arrayData[j2] == 'n' && arrayData[j2 + 1] == 'u'
											&& arrayData[j2 + 2] == 'l' && arrayData[j2 + 3] == 'l') {
										j2 = j2 + 4;
										this.objectValues[currentElement] = null;
										currentElement++;
										continue;

									}
									else {
										while (arrayData[j2] != 10 && arrayData[j2] != 13 && arrayData[j2] != 32 && arrayData[j2] != 93
												&& arrayData[j2] != 47) {
											if (arrayData[j2] == 62 && arrayData[j2 + 1] == 62) break;

											j2++;

											if (j2 == arrayData.length) break;
										}
									}
			}

			// actual value or first part of ref
			if (this.type == PdfDictionary.VALUE_IS_FLOAT_ARRAY) this.floatValues[currentElement] = NumberUtils.parseFloat(keyStart, j2, arrayData);
			else
				if (this.type == PdfDictionary.VALUE_IS_INT_ARRAY) this.intValues[currentElement] = NumberUtils.parseInt(keyStart, j2, arrayData);
				else
					if (this.type == PdfDictionary.VALUE_IS_BOOLEAN_ARRAY) {
						if (raw[keyStart] == 't' && raw[keyStart + 1] == 'r' && raw[keyStart + 2] == 'u' && raw[keyStart + 3] == 'e') this.booleanValues[currentElement] = true; // (false
																																													// id
																																													// default
																																													// if
																																													// not
																																													// set)
					}
					else
						if (this.type == PdfDictionary.VALUE_IS_DOUBLE_ARRAY) this.doubleValues[currentElement] = NumberUtils.parseFloat(keyStart,
								j2, arrayData);
						else
							if (!isRecursiveValue) j2 = setObjectArrayValue(pdfObject, PDFkeyInt, hexString, debugArray, currentElement,
									elementCount, j2, arrayData, singleKey, keyStart);

			currentElement++;

			if (debugArray) System.out.println(padding + "roll onto ==================================>" + currentElement + '/' + elementCount);
			if (currentElement == elementCount) break;
		}
		return j2;
	}

	private int setObjectArrayValue(PdfObject pdfObject, int PDFkeyInt, boolean hexString, boolean debugArray, int currentElement, int elementCount,
			int j2, byte[] arrayData, boolean singleKey, int keyStart) {

		// include / so we can differentiate /9 and 9
		if (keyStart > 0 && arrayData[keyStart - 1] == 47) keyStart--;

		// lose any spurious [
		if (keyStart > 0 && arrayData[keyStart] == '[' && PDFkeyInt != PdfDictionary.Names) keyStart++;

		// lose any nulls
		if (PDFkeyInt == PdfDictionary.Order || PDFkeyInt == PdfDictionary.Layer) {

			while (arrayData[keyStart] == 'n' && arrayData[keyStart + 1] == 'u' && arrayData[keyStart + 2] == 'l' && arrayData[keyStart + 3] == 'l') {
				keyStart = keyStart + 4;

				// lose any spurious chars at start
				while (keyStart >= 0
						&& (arrayData[keyStart] == ' ' || arrayData[keyStart] == 10 || arrayData[keyStart] == 13 || arrayData[keyStart] == 9))
					keyStart++;
			}

		}

		// lose any spurious chars at start
		while (keyStart >= 0 && (arrayData[keyStart] == ' ' || arrayData[keyStart] == 10 || arrayData[keyStart] == 13 || arrayData[keyStart] == 9))
			keyStart++;

		byte[] newValues = ObjectUtils.readEscapedValue(j2, arrayData, keyStart, PDFkeyInt == PdfDictionary.ID);

		if (debugArray) System.out.println(padding + "<1.Element -----" + currentElement + '/' + elementCount + "( j2=" + j2 + " ) value="
				+ new String(newValues) + '<');

		if (j2 == arrayData.length) {
			// ignore
		}
		else
			if (arrayData[j2] == '>') {
				j2++;
				// roll past ) and decrypt if needed
			}
			else
				if (arrayData[j2] == ')') {
					j2++;

					try {
						if (!pdfObject.isInCompressedStream() && this.decryption != null) newValues = this.decryption.decrypt(newValues,
								pdfObject.getObjectRefAsString(), false, null, false, false);
					}
					catch (PdfSecurityException e) {
						// tell user and log
						if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
					}

					// convert Strings in Order now
					if (PDFkeyInt == PdfDictionary.Order) newValues = StringUtils.toBytes(StringUtils.getTextString(newValues, false));
				}

		// update pointer if needed
		if (singleKey) this.i = j2;

		if (this.type == PdfDictionary.VALUE_IS_MIXED_ARRAY) {
			this.mixedValues[currentElement] = newValues;
		}
		else
			if (this.type == PdfDictionary.VALUE_IS_KEY_ARRAY) {
				this.keyValues[currentElement] = ObjectUtils.convertReturnsToSpaces(newValues);
			}
			else
				if (this.type == PdfDictionary.VALUE_IS_STRING_ARRAY) {
					if (hexString) {
						// convert to byte values
						String nextValue;
						String str = new String(newValues);
						byte[] IDbytes = new byte[newValues.length / 2];
						for (int ii = 0; ii < newValues.length; ii = ii + 2) {

							if (ii + 2 > newValues.length) continue;

							/*
							 * String array is a series of byte values. If the byte values has a \n in the middle we should ignore it.
							 * (customer-June2011/payam.pdf)
							 */
							if (str.charAt(ii) == '\n') {
								ii++;
							}

							nextValue = str.substring(ii, ii + 2);
							IDbytes[ii / 2] = (byte) Integer.parseInt(nextValue, 16);

						}
						newValues = IDbytes;
					}

					this.stringValues[currentElement] = newValues;

				}
				else
					if (this.type == PdfDictionary.VALUE_IS_OBJECT_ARRAY) {
						this.objectValues[currentElement] = (newValues);

						if (debugArray) System.out.println(padding + "objectValues[" + currentElement + "]=" + Arrays.toString(this.objectValues)
								+ ' ');
					}
		return j2;
	}

	private void initObjectArray(int elementCount) {
		if (this.type == PdfDictionary.VALUE_IS_FLOAT_ARRAY) this.floatValues = new float[elementCount];
		else
			if (this.type == PdfDictionary.VALUE_IS_INT_ARRAY) this.intValues = new int[elementCount];
			else
				if (this.type == PdfDictionary.VALUE_IS_BOOLEAN_ARRAY) this.booleanValues = new boolean[elementCount];
				else
					if (this.type == PdfDictionary.VALUE_IS_DOUBLE_ARRAY) this.doubleValues = new double[elementCount];
					else
						if (this.type == PdfDictionary.VALUE_IS_MIXED_ARRAY) this.mixedValues = new byte[elementCount][];
						else
							if (this.type == PdfDictionary.VALUE_IS_KEY_ARRAY) this.keyValues = new byte[elementCount][];
							else
								if (this.type == PdfDictionary.VALUE_IS_STRING_ARRAY) this.stringValues = new byte[elementCount][];
								else
									if (this.type == PdfDictionary.VALUE_IS_OBJECT_ARRAY) this.objectValues = new Object[elementCount];
	}

	private boolean handleIndirect(int endPoint, byte[] raw, boolean debugArray) {

		boolean indirect = true;

		// find next value and make sure not /
		int aa = this.i, length = raw.length;

		while (raw[aa] != 93) {
			aa++;

			// allow for ref (ie 7 0 R)
			if (aa >= endPoint || aa >= length) break;

			if (raw[aa] == 'R' && (raw[aa - 1] == 32 || raw[aa - 1] == 10 || raw[aa - 1] == 13)) break;
			else
				if (raw[aa] == '>' && raw[aa - 1] == '>') {
					indirect = false;
					if (debugArray) System.out.println(padding + "1. rejected as indirect ref");

					break;
				}
				else
					if (raw[aa] == 47) {
						indirect = false;
						if (debugArray) System.out.println(padding + "2. rejected as indirect ref - starts with /");

						break;
					}
		}
		return indirect;
	}

	private void skipComment(byte[] raw) {
		while (raw[this.i] != 10 && raw[this.i] != 13) {
			this.i++;
		}

		// move cursor to start of text
		while (raw[this.i] == 10 || raw[this.i] == 13 || raw[this.i] == 32)
			this.i++;
	}

	private void setKeyArrayValue(PdfObject pdfObject, int PDFkeyInt, int elementCount) {

		if (this.type == PdfDictionary.VALUE_IS_KEY_ARRAY && elementCount == 1 && PDFkeyInt == PdfDictionary.Annots) {// allow for indirect on Annots

			byte[] objData = this.keyValues[0];

			// allow for null
			if (objData != null) {

				int size = objData.length;
				if (objData[size - 1] == 'R') {

					PdfObject obj = new PdfObject(new String(objData));
					byte[] newData = this.objectReader.readObjectData(obj);

					if (newData != null) {

						int jj = 0, newLen = newData.length;
						boolean hasArray = false;
						while (jj < newLen) {
							jj++;

							if (jj == newData.length) break;

							if (newData[jj] == '[') {
								hasArray = true;
								break;
							}
							else
								if (newData[jj - 1] == '<' && newData[jj] == '<') {
									hasArray = false;
									break;
								}
						}

						if (hasArray) {
							ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, jj, newLen, PdfDictionary.VALUE_IS_KEY_ARRAY);
							objDecoder.readArray(false, newData, pdfObject, PDFkeyInt);
						}
						else pdfObject.setKeyArray(PDFkeyInt, this.keyValues);
					}
				}
			}
		}
		else pdfObject.setKeyArray(PDFkeyInt, this.keyValues);
	}

	private void setObjectArrayValue(PdfObject pdfObject, int PDFkeyInt, Object[] objectValuesArray, int keyReached, boolean debugArray) {
		// allow for indirect order
		if (PDFkeyInt == PdfDictionary.Order && this.objectValues != null && this.objectValues.length == 1 && this.objectValues[0] instanceof byte[]) {

			byte[] objData = (byte[]) this.objectValues[0];
			int size = objData.length;
			if (objData[size - 1] == 'R') {

				PdfObject obj = new OCObject(new String(objData));
				byte[] newData = this.objectReader.readObjectData(obj);

				int jj = 0, newLen = newData.length;
				boolean hasArray = false;
				while (jj < newLen) {
					jj++;

					if (jj == newData.length) break;

					if (newData[jj] == '[') {
						hasArray = true;
						break;
					}
				}

				if (hasArray) {
					ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, jj, newLen, PdfDictionary.VALUE_IS_OBJECT_ARRAY);
					objDecoder.readArray(false, newData, pdfObject, PDFkeyInt);
				}
				this.objectValues = null;

			}
		}

		if (objectValuesArray != null) {
			objectValuesArray[keyReached] = this.objectValues;

			if (debugArray) System.out.println(padding + "set Object objectValuesArray[" + keyReached + "]=" + Arrays.toString(this.objectValues));

		}
		else
			if (this.objectValues != null) {
				pdfObject.setObjectArray(PDFkeyInt, this.objectValues);

				if (debugArray) System.out.println(padding + PDFkeyInt + " set Object value=" + Arrays.toString(this.objectValues));
			}
	}

	/**
	 * used for debugging
	 */
	private void showValues() {

		String values = "[";

		if (this.type == PdfDictionary.VALUE_IS_FLOAT_ARRAY) {
			for (float floatValue : this.floatValues) {
				values = values + floatValue + ' ';
			}

		}
		else
			if (this.type == PdfDictionary.VALUE_IS_DOUBLE_ARRAY) {
				for (double doubleValue : this.doubleValues) {
					values = values + doubleValue + ' ';
				}

			}
			else
				if (this.type == PdfDictionary.VALUE_IS_INT_ARRAY) {
					for (int intValue : this.intValues) {
						values = values + intValue + ' ';
					}

				}
				else
					if (this.type == PdfDictionary.VALUE_IS_BOOLEAN_ARRAY) {
						for (boolean booleanValue : this.booleanValues) {
							values = values + booleanValue + ' ';
						}

					}
					else
						if (this.type == PdfDictionary.VALUE_IS_MIXED_ARRAY) {
							for (byte[] mixedValue : this.mixedValues) {
								if (mixedValue == null) values = values + "null ";
								else values = values + new String(mixedValue) + ' ';
							}

						}
						else
							if (this.type == PdfDictionary.VALUE_IS_KEY_ARRAY) {
								for (byte[] keyValue : this.keyValues) {
									if (keyValue == null) values = values + "null ";
									else values = values + new String(keyValue) + ' ';
								}
							}
							else
								if (this.type == PdfDictionary.VALUE_IS_STRING_ARRAY) {
									for (byte[] stringValue : this.stringValues) {
										if (stringValue == null) values = values + "null ";
										else values = values + new String(stringValue) + ' ';
									}
								}
								else
									if (this.type == PdfDictionary.VALUE_IS_OBJECT_ARRAY) {
										values = ObjectUtils.showMixedValuesAsString(this.objectValues, "");
									}

		values = values + " ]";

		System.out.println(padding + "values=" + values);
	}

}
