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
 * ObjectDecoder.java
 * ---------------
 */
package org.jpedal.io;

import java.io.Serializable;

import org.jpedal.color.ColorSpaces;
import org.jpedal.constants.PDFflags;
import org.jpedal.exception.PdfSecurityException;
import org.jpedal.objects.raw.ColorSpaceObject;
import org.jpedal.objects.raw.FormObject;
import org.jpedal.objects.raw.NamesObject;
import org.jpedal.objects.raw.ObjectFactory;
import org.jpedal.objects.raw.PdfArrayIterator;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.NumberUtils;
import org.jpedal.utils.StringUtils;

public class ObjectDecoder implements Serializable {

	private static final long serialVersionUID = 7141499991569886872L;

	PdfFileReader objectReader = null;

	DecryptionFactory decryption = null;

	// string representation of key only used in debugging
	private Object PDFkey;

	final static byte[] endPattern = { 101, 110, 100, 111, 98, 106 }; // pattern endobj

	static final boolean debugFastCode = false; // objRef.equals("68 0 R")

	private int pdfKeyType, PDFkeyInt;

	/** used in debuggin output */
	static String padding = "";

	boolean isInlineImage = false;

	private int endPt = -1;

	public ObjectDecoder(PdfFileReader pdfFileReader) {
		init(pdfFileReader);
	}

	private ObjectDecoder() {
	}

	private void init(PdfFileReader objectReader) {
		this.objectReader = objectReader;

		this.decryption = objectReader.getDecryptionObject();
	}

	/**
	 * read a dictionary object
	 */
	public int readDictionaryAsObject(PdfObject pdfObject, int i, byte[] raw) {

		if (this.endPt == -1) this.endPt = raw.length;

		// used to debug issues by printing out details for obj
		// (set to non-final above)
		// debugFastCode =pdfObject.getObjectRefAsString().equals("5 0 R");

		if (debugFastCode) padding = padding + "   ";

		final int length = raw.length;

		// show details in debug mode
		if (debugFastCode) ObjectUtils.showData(pdfObject, i, length, raw, padding);

		/**
		 * main loop for read all values from Object data and store in PDF object
		 */
		i = readObjectDataValues(pdfObject, i, raw, length);

		/**
		 * look for stream afterwards
		 */
		if (!pdfObject.ignoreStream() && pdfObject.getGeneralType(-1) != PdfDictionary.ID) readStreamData(pdfObject, i, raw, length);

		/**
		 * we need full names for Forms
		 */
		if (pdfObject.getObjectType() == PdfDictionary.Form) setFieldNames(pdfObject);

		/**
		 * reset indent in debugging
		 */
		if (debugFastCode) {
			int len = padding.length();

			if (len > 3) padding = padding.substring(0, len - 3);
		}

		return i;
	}

	/**
	 * get the values from the data stream and store in PdfObject
	 * 
	 * @param pdfObject
	 * @param i
	 * @param raw
	 * @param length
	 */
	private int readObjectDataValues(PdfObject pdfObject, int i, byte[] raw, int length) {

		int level = 0;
		// allow for no << at start
		if (this.isInlineImage) level = 1;

		while (true) {

			if (i < length && raw[i] == 37) // allow for comment and ignore
			i = stripComment(length, i, raw);

			/**
			 * exit conditions
			 */
			if ((i >= length || (this.endPt != -1 && i >= this.endPt))
					|| (raw[i] == 101 && raw[i + 1] == 110 && raw[i + 2] == 100 && raw[i + 3] == 111)
					|| (raw[i] == 's' && raw[i + 1] == 't' && raw[i + 2] == 'r' && raw[i + 3] == 'e' && raw[i + 4] == 'a' && raw[i + 5] == 'm')) break;

			/**
			 * process value
			 */
			if (raw[i] == 60 && raw[i + 1] == 60) {
				i++;
				level++;
			}
			else
				if (raw[i] == 62 && i + 1 != length && raw[i + 1] == 62 && raw[i - 1] != 62) {
					i++;
					level--;

					if (level == 0) break;
				}
				else
					if (raw[i] == 47 && (raw[i + 1] == 47 || raw[i + 1] == 32)) { // allow for oddity of //DeviceGray and / /DeviceGray in colorspace
						i++;
					}
					else
						if (raw[i] == 47) { // everything from /

							i++; // skip /

							int keyStart = i;
							int keyLength = findDictionaryEnd(i, raw, length);
							i = i + keyLength;

							if (i == length) break;

							// if BDC see if string
							boolean isStringPair = false;
							if (pdfObject.getID() == PdfDictionary.BDC) isStringPair = isStringPair(i, raw, isStringPair);

							int type = pdfObject.getObjectType();

							if (debugFastCode) System.out.println("type=" + type + ' ' + ' ' + pdfObject.getID() + " chars=" + (char) raw[i - 1]
									+ (char) raw[i] + ' ' + pdfObject + " i=" + i + ' ' + isStringPair);

							// see if map of objects
							boolean isMap = isMapObject(pdfObject, i, raw, length, keyStart, keyLength, isStringPair, type);

							if (raw[i] == 47 || raw[i] == 40 || raw[i] == 91) // move back cursor
							i--;

							// check for unknown value and ignore
							if (this.pdfKeyType == -1) i = ObjectUtils.handleUnknownType(i, raw, length);

							/**
							 * now read value
							 */
							if (this.PDFkeyInt == -1 || this.pdfKeyType == -1) {
								if (debugFastCode) System.out.println(padding + pdfObject.getObjectRefAsString()
										+ " =================Not implemented=" + this.PDFkey + " pdfKeyType=" + this.pdfKeyType);
							}
							else {
								i = setValue(pdfObject, i, raw, length, isMap);
							}

							// special case if Dest defined as names object and indirect
						}
						else
							if (raw[i] == '[' && level == 0 && pdfObject.getObjectType() == PdfDictionary.Outlines) {

								ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, raw.length, PdfDictionary.VALUE_IS_MIXED_ARRAY,
										null, PdfDictionary.Names);
								objDecoder.readArray(false, raw, pdfObject, PdfDictionary.Dest);

							}

			i++;

		}

		return i;
	}

	private boolean isMapObject(PdfObject pdfObject, int i, byte[] raw, int length, int keyStart, int keyLength, boolean stringPair, int type) {

		boolean isMap;// ensure all go into 'pool'
		if (type == PdfDictionary.MCID
				&& (pdfObject.getID() == PdfDictionary.RoleMap || (pdfObject.getID() == PdfDictionary.BDC && stringPair) || (pdfObject.getID() == PdfDictionary.A && raw[i - 2] == '/'))) {

			this.pdfKeyType = PdfDictionary.VALUE_IS_NAME;

			// used in debug and this case
			this.PDFkey = PdfDictionary.getKey(keyStart, keyLength, raw);
			this.PDFkeyInt = PdfDictionary.MCID;
			isMap = true;

		}
		else {
			isMap = false;
			this.PDFkey = null;
			getKeyType(pdfObject, i, raw, length, keyLength, keyStart, type);
		}
		return isMap;
	}

	private static int findDictionaryEnd(int jj, byte[] raw, int length) {

		int keyLength = 0;
		while (true) { // get key up to space or [ or / or ( or < or carriage return

			if (raw[jj] == 32 || raw[jj] == 13 || raw[jj] == 9 || raw[jj] == 10 || raw[jj] == 91 || raw[jj] == 47 || raw[jj] == 40 || raw[jj] == 60
					|| raw[jj] == 62) break;

			jj++;
			keyLength++;

			if (jj == length) break;
		}
		return keyLength;
	}

	private void getKeyType(PdfObject pdfObject, int i, byte[] raw, int length, int keyLength, int keyStart, int type) {
		/**
		 * get Dictionary key and type of value it takes
		 */
		if (debugFastCode) // used in debug
		this.PDFkey = PdfDictionary.getKey(keyStart, keyLength, raw);

		this.PDFkeyInt = PdfDictionary.getIntKey(keyStart, keyLength, raw);

		// correct mapping
		if (this.PDFkeyInt == PdfDictionary.Indexed && (type == PdfDictionary.MK || type == PdfDictionary.Form || type == PdfDictionary.Linearized)) this.PDFkeyInt = PdfDictionary.I;

		if (this.isInlineImage) this.PDFkeyInt = PdfObjectFactory.getInlineID(this.PDFkeyInt);

		int id = pdfObject.getID();

		if (type == PdfDictionary.Resources
				&& (this.PDFkeyInt == PdfDictionary.ColorSpace || this.PDFkeyInt == PdfDictionary.ExtGState
						|| this.PDFkeyInt == PdfDictionary.Shading || this.PDFkeyInt == PdfDictionary.XObject || this.PDFkeyInt == PdfDictionary.Font || this.PDFkeyInt == PdfDictionary.Pattern)) {
			this.pdfKeyType = PdfDictionary.VALUE_IS_DICTIONARY_PAIRS;
			// }else if (type==PdfDictionary.Form && id== PdfDictionary.AA && PDFkeyInt== PdfDictionary.K){
			// pdfKeyType= PdfDictionary.VALUE_IS_UNREAD_DICTIONARY;
		}
		else
			if (type == PdfDictionary.Outlines && this.PDFkeyInt == PdfDictionary.D) {
				this.PDFkeyInt = PdfDictionary.Dest;
				this.pdfKeyType = PdfDictionary.VALUE_IS_MIXED_ARRAY;
			}
			else
				if ((type == PdfDictionary.Form || type == PdfDictionary.MK) && this.PDFkeyInt == PdfDictionary.D) {
					if (id == PdfDictionary.AP || id == PdfDictionary.AA) {
						this.pdfKeyType = PdfDictionary.VALUE_IS_VARIOUS;
					}
					else
						if (id == PdfDictionary.Win) {
							this.pdfKeyType = PdfDictionary.VALUE_IS_TEXTSTREAM;
						}
						else {
							this.PDFkeyInt = PdfDictionary.Dest;
							this.pdfKeyType = PdfDictionary.VALUE_IS_MIXED_ARRAY;
						}
				}
				else
					if ((type == PdfDictionary.Form || type == PdfDictionary.MK) && (id == PdfDictionary.AP || id == PdfDictionary.AA)
							&& this.PDFkeyInt == PdfDictionary.A) {
						this.pdfKeyType = PdfDictionary.VALUE_IS_VARIOUS;
					}
					else
						if (this.PDFkeyInt == PdfDictionary.Order && type == PdfDictionary.OCProperties) {
							this.pdfKeyType = PdfDictionary.VALUE_IS_OBJECT_ARRAY;
						}
						else
							if (this.PDFkeyInt == PdfDictionary.Name && type == PdfDictionary.OCProperties) {
								this.pdfKeyType = PdfDictionary.VALUE_IS_TEXTSTREAM;
							}
							else
								if ((type == PdfDictionary.ColorSpace || type == PdfDictionary.Function) && this.PDFkeyInt == PdfDictionary.N) {
									this.pdfKeyType = PdfDictionary.VALUE_IS_FLOAT;
								}
								else
									if (this.PDFkeyInt == PdfDictionary.Gamma && type == PdfDictionary.ColorSpace
											&& pdfObject.getParameterConstant(PdfDictionary.ColorSpace) == ColorSpaces.CalGray) { // its a number not
																																	// an array
										this.pdfKeyType = PdfDictionary.VALUE_IS_FLOAT;
									}
									else
										if (id == PdfDictionary.Win && pdfObject.getObjectType() == PdfDictionary.Form
												&& (this.PDFkeyInt == PdfDictionary.P || this.PDFkeyInt == PdfDictionary.O)) {
											this.pdfKeyType = PdfDictionary.VALUE_IS_TEXTSTREAM;
										}
										else
											if (this.isInlineImage && this.PDFkeyInt == PdfDictionary.ColorSpace) {
												this.pdfKeyType = PdfDictionary.VALUE_IS_DICTIONARY;
											}
											else this.pdfKeyType = PdfDictionary.getKeyType(this.PDFkeyInt, type);

		// handle array of Function in Shading by using keyArray
		if (id == PdfDictionary.Shading && this.PDFkeyInt == PdfDictionary.Function) {

			// get next non number/char value
			int ptr = i;
			while ((raw[ptr] >= 48 && raw[ptr] < 58) || raw[ptr] == 32) {
				ptr++;
			}

			if (raw[ptr] == '[') {
				this.pdfKeyType = PdfDictionary.VALUE_IS_KEY_ARRAY;
			}
		}

		// allow for other values in D,N,R definitions
		/**
		 * if((pdfKeyType==-1 && id== PdfDictionary.ClassMap) || ((pdfKeyType==-1 || (keyLength<3 && (id==PdfDictionary.N || id==PdfDictionary.D ||
		 * id==PdfDictionary.R))) && pdfObject.getParentID()==PdfDictionary.AP && pdfObject.getObjectType()==PdfDictionary.Form &&
		 * (id==PdfDictionary.D || id==PdfDictionary.N || id==PdfDictionary.R)) ){
		 * 
		 * pdfKeyType = getPairedValues(pdfObject, i, raw, pdfKeyType, length, keyLength, keyStart);
		 * 
		 * if(debugFastCode) System.out.println("Returns "+pdfKeyType+" i="+i); }/
		 **/

		/**/
		// allow for other values in D,N,R definitions
		if (this.pdfKeyType == -1 && id == PdfDictionary.ClassMap) {
			this.pdfKeyType = getPairedValues(pdfObject, i, raw, this.pdfKeyType, length, keyLength, keyStart);
		}
		else
			// allow for other values in D,N,R definitions as key pairs
			if (((((id == PdfDictionary.N || id == PdfDictionary.D || id == PdfDictionary.R))) && pdfObject.getParentID() == PdfDictionary.AP
					&& pdfObject.getObjectType() == PdfDictionary.Form && raw[i] != '[')) {

				// get next non number/char value
				int ptr = i;
				while ((raw[ptr] >= 48 && raw[ptr] < 58) || raw[ptr] == 32) {
					ptr++;
				}

				// decide if pair
				if (raw[keyStart] == 'L' && raw[keyStart + 1] == 'e' && raw[keyStart + 2] == 'n' && raw[keyStart + 3] == 'g'
						&& raw[keyStart + 4] == 't' && raw[keyStart + 5] == 'h') {}
				else
					if (raw[keyStart] == 'O' && raw[keyStart + 1] == 'n') {}
					else
						if (raw[keyStart] == 'O' && raw[keyStart + 1] == 'f' && raw[keyStart + 2] == 'f') {}
						else
							if (raw[ptr] == 'R') {
								this.pdfKeyType = getPairedValues(pdfObject, i, raw, this.pdfKeyType, length, keyLength, keyStart);

								if (debugFastCode) System.out.println("new Returns " + this.pdfKeyType + " i=" + i);
							}
			}

		/**/

		// DecodeParms can be an array as well as a dictionary so check next char and alter if so
		if (this.PDFkeyInt == PdfDictionary.DecodeParms) this.pdfKeyType = setTypeForDecodeParams(i, raw, length, this.pdfKeyType);

		if (debugFastCode && this.pdfKeyType == -1 && pdfObject.getObjectType() != PdfDictionary.Page) {
			System.out.println(id + " " + type);
			System.out.println(padding + this.PDFkey + " NO type setting for " + PdfDictionary.getKey(keyStart, keyLength, raw) + " id=" + i);
		}
	}

	private int setValue(PdfObject pdfObject, int i, byte[] raw, int length, boolean map) {

		// if we only need top level do not read whole tree
		boolean ignoreRecursion = pdfObject.ignoreRecursion();

		if (debugFastCode) System.out.println(padding + pdfObject.getObjectRefAsString() + " =================Reading value for key=" + this.PDFkey
				+ " (" + this.PDFkeyInt + ") type=" + PdfDictionary.showAsConstant(this.pdfKeyType) + " ignorRecursion=" + ignoreRecursion + ' '
				+ pdfObject);

		// resolve now in this case as we need to ensure all parts present
		if (this.pdfKeyType == PdfDictionary.VALUE_IS_UNREAD_DICTIONARY && pdfObject.isDataExternal()) this.pdfKeyType = PdfDictionary.VALUE_IS_DICTIONARY;

		switch (this.pdfKeyType) {

		// read text stream (this is text) and also special case of [] in W in CID Fonts
			case PdfDictionary.VALUE_IS_TEXTSTREAM: {
				i = setTextStreamValue(pdfObject, i, raw, ignoreRecursion);
				break;

			}
			case PdfDictionary.VALUE_IS_NAMETREE: {
				i = setNameTreeValue(pdfObject, i, raw, length, ignoreRecursion);
				break;

				// readDictionary keys << /A 12 0 R /B 13 0 R >>
			}
			case PdfDictionary.VALUE_IS_DICTIONARY_PAIRS: {
				i = setDictionaryValue(pdfObject, i, raw, length, ignoreRecursion);
				break;

				// Strings
			}
			case PdfDictionary.VALUE_IS_STRING_ARRAY: {
				ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, this.endPt, PdfDictionary.VALUE_IS_STRING_ARRAY);
				i = objDecoder.readArray(ignoreRecursion, raw, pdfObject, this.PDFkeyInt);
				break;

				// read Object Refs in [] (may be indirect ref)
			}
			case PdfDictionary.VALUE_IS_BOOLEAN_ARRAY: {
				ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, this.endPt, PdfDictionary.VALUE_IS_BOOLEAN_ARRAY);
				i = objDecoder.readArray(false, raw, pdfObject, this.PDFkeyInt);
				break;

				// read Object Refs in [] (may be indirect ref)
			}
			case PdfDictionary.VALUE_IS_KEY_ARRAY: {
				ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, this.endPt, PdfDictionary.VALUE_IS_KEY_ARRAY);
				i = objDecoder.readArray(ignoreRecursion, raw, pdfObject, this.PDFkeyInt);
				break;

				// read numbers in [] (may be indirect ref)
			}
			case PdfDictionary.VALUE_IS_MIXED_ARRAY: {
				ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, this.endPt, PdfDictionary.VALUE_IS_MIXED_ARRAY);
				i = objDecoder.readArray(ignoreRecursion, raw, pdfObject, this.PDFkeyInt);
				break;

				// read numbers in [] (may be indirect ref) same as Mixed but allow for recursion and store as objects
			}
			case PdfDictionary.VALUE_IS_OBJECT_ARRAY: {
				ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, this.endPt, PdfDictionary.VALUE_IS_OBJECT_ARRAY);
				i = objDecoder.readArray(false, raw, pdfObject, this.PDFkeyInt);
				break;

				// read numbers in [] (may be indirect ref)
			}
			case PdfDictionary.VALUE_IS_DOUBLE_ARRAY: {
				ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, this.endPt, PdfDictionary.VALUE_IS_DOUBLE_ARRAY);
				i = objDecoder.readArray(false, raw, pdfObject, this.PDFkeyInt);
				break;

				// read numbers in [] (may be indirect ref)
			}
			case PdfDictionary.VALUE_IS_INT_ARRAY: {
				ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, this.endPt, PdfDictionary.VALUE_IS_INT_ARRAY);
				i = objDecoder.readArray(false, raw, pdfObject, this.PDFkeyInt);
				break;

				// read numbers in [] (may be indirect ref)
			}
			case PdfDictionary.VALUE_IS_FLOAT_ARRAY: {
				ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, this.endPt, PdfDictionary.VALUE_IS_FLOAT_ARRAY);
				i = objDecoder.readArray(false, raw, pdfObject, this.PDFkeyInt);
				break;

				// read String (may be indirect ref)
			}
			case PdfDictionary.VALUE_IS_NAME: {
				i = setNameStringValue(pdfObject, i, raw, map);
				break;

				// read true or false
			}
			case PdfDictionary.VALUE_IS_BOOLEAN: {
				i = setBooleanValue(pdfObject, i, raw, this.PDFkeyInt);
				break;

				// read known set of values
			}
			case PdfDictionary.VALUE_IS_STRING_CONSTANT: {
				i = setStringConstantValue(pdfObject, i, raw);
				break;

				// read known set of values
			}
			case PdfDictionary.VALUE_IS_STRING_KEY: {
				i = setStringKeyValue(pdfObject, i, raw);
				break;

				// read number (may be indirect ref)
			}
			case PdfDictionary.VALUE_IS_INT: {

				// roll on
				i++;

				// move cursor to start of text
				while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47)
					i++;

				i = setNumberValue(pdfObject, i, raw, this.PDFkeyInt);
				break;

				// read float number (may be indirect ref)
			}
			case PdfDictionary.VALUE_IS_FLOAT: {
				i = setFloatValue(pdfObject, i, raw, length);
				break;

				// read known Dictionary object which may be direct or indirect
			}
			case PdfDictionary.VALUE_IS_UNREAD_DICTIONARY: {
				i = setUnreadDictionaryValue(pdfObject, i, raw);
				break;

			}
			case PdfDictionary.VALUE_IS_VARIOUS: {
				if (raw.length - 5 > 0 && raw[i + 1] == 'n' && raw[i + 2] == 'u' && raw[i + 3] == 'l' && raw[i + 4] == 'l') { // ignore null value and
																																// skip (ie /N null)
					i = i + 5;
				}
				else {
					i = setVariousValue(pdfObject, i, raw, length, this.PDFkeyInt, map, ignoreRecursion);
				}
				break;

			}
			case PdfDictionary.VALUE_IS_DICTIONARY: {
				i = setDictionaryValue(pdfObject, i, raw, ignoreRecursion);
				break;
			}
		}
		return i;
	}

	private static boolean isStringPair(int i, byte[] raw, boolean stringPair) {

		int len = raw.length;
		for (int aa = i; aa < len; aa++) {
			if (raw[aa] == '(') {
				aa = len;
				stringPair = true;
			}
			else
				if (raw[aa] == '/' || raw[aa] == '>' || raw[aa] == '<' || raw[aa] == '[' || raw[aa] == 'R') {
					aa = len;
				}
				else
					if (raw[aa] == 'M' && raw[aa + 1] == 'C' && raw[aa + 2] == 'I' && raw[aa + 3] == 'D') {
						aa = len;
					}
		}
		return stringPair;
	}

	private static int stripComment(int length, int i, byte[] raw) {

		while (i < length && raw[i] != 10 && raw[i] != 13)
			i++;

		// move cursor to start of text
		while (i < length && (raw[i] == 9 || raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 60))
			i++;

		return i;
	}

	private int setVariousValue(PdfObject pdfObject, int i, byte[] raw, int length, int PDFkeyInt, boolean map, boolean ignoreRecursion) {

		int keyStart;
		if (raw[i] != '<') i++;

		if (debugFastCode) System.out.println(padding + "Various value (first char=" + (char) raw[i] + (char) raw[i + 1] + " )");

		if (raw[i] == '/') {
			i = setNameStringValue(pdfObject, i, raw, map);
		}
		else
			if (raw[i] == 'f' && raw[i + 1] == 'a' && raw[i + 2] == 'l' && raw[i + 3] == 's' && raw[i + 4] == 'e') {
				pdfObject.setBoolean(PDFkeyInt, false);
				i = i + 4;
			}
			else
				if (raw[i] == 't' && raw[i + 1] == 'r' && raw[i + 2] == 'u' && raw[i + 3] == 'e') {
					pdfObject.setBoolean(PDFkeyInt, true);
					i = i + 3;
				}
				else
					if (raw[i] == '(' || (raw[i] == '<' && raw[i - 1] != '<' && raw[i + 1] != '<')) {
						i = readTextStream(pdfObject, i, raw, PDFkeyInt, ignoreRecursion);
					}
					else
						if (raw[i] == '[') {

							if (PDFkeyInt == PdfDictionary.XFA) {
								ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, this.endPt, PdfDictionary.VALUE_IS_MIXED_ARRAY);
								i = objDecoder.readArray(ignoreRecursion, raw, pdfObject, PDFkeyInt);
							}
							else
								if (PDFkeyInt == PdfDictionary.K) {
									ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, this.endPt, PdfDictionary.VALUE_IS_STRING_ARRAY);
									i = objDecoder.readArray(ignoreRecursion, raw, pdfObject, PDFkeyInt);
								}
								else
									if (PDFkeyInt == PdfDictionary.C) {
										ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, this.endPt,
												PdfDictionary.VALUE_IS_FLOAT_ARRAY);
										i = objDecoder.readArray(ignoreRecursion, raw, pdfObject, PDFkeyInt);
									}
									else {
										ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, i, this.endPt,
												PdfDictionary.VALUE_IS_STRING_ARRAY);
										i = objDecoder.readArray(ignoreRecursion, raw, pdfObject, PDFkeyInt);
									}
						}
						else
							if ((raw[i] == '<' && raw[i + 1] == '<')) {
								i = readDictionary(pdfObject, i, raw, PDFkeyInt, ignoreRecursion);
							}
							else {

								if (debugFastCode) System.out.println(padding + "general case " + i);

								// see if number or ref
								int jj = i;
								int j = i + 1;
								byte[] data = raw;
								int typeFound = 0;
								boolean isNumber = true, isRef = false, isString = false;

								String objRef = pdfObject.getObjectRefAsString();

								while (true) {

									if (data[j] == 'R' && !isString) {

										isRef = true;
										int end = j;
										j = i;
										i = end;

										int ref, generation;

										// allow for [ref] at top level (may be followed by gap
										while (data[j] == 91 || data[j] == 32 || data[j] == 13 || data[j] == 10)
											j++;

										// get object ref
										keyStart = j;
										int refStart = j;
										// move cursor to end of reference
										while (data[j] != 10 && data[j] != 13 && data[j] != 32 && data[j] != 47 && data[j] != 60 && data[j] != 62)
											j++;

										ref = NumberUtils.parseInt(keyStart, j, data);

										// move cursor to start of generation or next value
										while (data[j] == 10 || data[j] == 13 || data[j] == 32)
											// || data[j]==47 || data[j]==60)
											j++;

										/**
										 * get generation number
										 */
										keyStart = j;
										// move cursor to end of reference
										while (data[j] != 10 && data[j] != 13 && data[j] != 32 && data[j] != 47 && data[j] != 60 && data[j] != 62)
											j++;

										generation = NumberUtils.parseInt(keyStart, j, data);

										/**
										 * check R at end of reference and abort if wrong
										 */
										// move cursor to start of R
										while (data[j] == 10 || data[j] == 13 || data[j] == 32 || data[j] == 47 || data[j] == 60)
											j++;

										if (data[j] != 82) // we are expecting R to end ref
										throw new RuntimeException("ref=" + ref + " gen=" + ref + " 1. Unexpected value " + data[j]
												+ " in file - please send to IDRsolutions for analysis char=" + (char) data[j]);

										objRef = new String(data, refStart, 1 + j - refStart);

										// read the Dictionary data
										// boolean setting=debugFastCode;
										byte[] newData = this.objectReader.readObjectAsByteArray(pdfObject,
												this.objectReader.isCompressed(ref, generation), ref, generation);

										// find first valid char to see if String
										int firstChar = 0;
										int newLength = newData.length - 3;
										for (int aa = 3; aa < newLength; aa++) { // skip past 13 0 obj bit at start if present
											if (newData[aa - 2] == 'o' && newData[aa - 1] == 'b' && newData[aa] == 'j') {
												firstChar = aa + 1;
												// roll on past and spaces
												while (firstChar < newLength
														&& (newData[firstChar] == 10 || newData[firstChar] == 13 || newData[firstChar] == 32 || newData[firstChar] == 9))
													firstChar++;

												aa = newLength; // exit loop
											}
											else
												if (newData[aa] > 47 && newData[aa] < 58) {// number
												}
												else
													if (newData[aa] == 'o' || newData[aa] == 'b' || newData[aa] == 'j' || newData[aa] == 'R'
															|| newData[aa] == 32 || newData[aa] == 10 || newData[aa] == 13) { // allowed char
													}
													else { // not expected so reset and quit
														aa = newLength;
														firstChar = 0;
													}
										}

										// stop string with R failing in loop
										isString = newData[firstChar] == '(';

										if (pdfObject.getID() == PdfDictionary.AA && newData[0] == '<' && newData[1] == '<') {

											// create and store stub
											PdfObject valueObj = ObjectFactory.createObject(PDFkeyInt, objRef, pdfObject.getObjectType(),
													pdfObject.getID());
											valueObj.setID(PDFkeyInt);

											pdfObject.setDictionary(PDFkeyInt, valueObj);
											valueObj.setStatus(PdfObject.UNDECODED_DIRECT);
											valueObj.setUnresolvedData(newData, PdfObject.UNDECODED_DIRECT);

											if (debugFastCode) System.out.println(padding + "obj ");

											isNumber = false;
											typeFound = 4;

											i = j;

											break;

										}
										else
											if ((pdfObject.getID() == -1) && newData[0] == '<' && newData[1] == '<') {
												isNumber = false;
												typeFound = 0;

												i = j;

												if (debugFastCode) System.out.println(padding + "Dict starting << ");

												break;

												// allow for indirect on OpenAction
											}
											else
												if (PDFkeyInt == PdfDictionary.OpenAction && data[i] == 'R') {

													// get the object data and pass in
													int jj2 = 0;
													while (newData[jj2] != '[') {
														jj2++;

														if (newData[jj2] == '<' && newData[jj2 + 1] != '<') break;
													}

													ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, jj2, this.endPt,
															PdfDictionary.VALUE_IS_MIXED_ARRAY);
													objDecoder.readArray(ignoreRecursion, newData, pdfObject, PDFkeyInt);
													i = j;
													break;
												}
												else {

													data = newData;

													// allow for data in Linear object not yet loaded
													if (data == null) {
														pdfObject.setFullyResolved(false);

														if (debugFastCode) System.out.println(padding + "Data not yet loaded");

														if (LogWriter.isOutput()) LogWriter.writeLog("[Linearized] " + objRef
																+ " not yet available (4)");

														i = length;
														break;
													}

													jj = 3;

													if (data.length <= 3) {
														jj = 0;
													}
													else {
														while (true) {
															if (data[jj - 2] == 'o' && data[jj - 1] == 'b' && data[jj] == 'j') break;

															jj++;

															if (jj == data.length) {
																jj = 0;
																break;
															}
														}
													}

													if (data[jj] != '(') // do not roll on if text string
													jj++;

													while (data[jj] == 10 || data[jj] == 13 || data[jj] == 32)
														// || data[j]==47 || data[j]==60)
														jj++;

													j = jj;

													if (debugFastCode) System.out.println(j + " >>" + new String(data) + "<<next=" + (char) data[j]);
												}
									}
									else
										if (data[j] == '[' || data[j] == '(') {
											// typeFound=0;
											break;
										}
										else
											if (data[j] == '<') {
												typeFound = 0;
												break;

											}
											else
												if (data[j] == '>' || data[j] == '/') {
													typeFound = 1;
													break;
												}
												else
													if (data[j] == 32 || data[j] == 10 || data[j] == 13) {}
													else
														if ((data[j] >= '0' && data[j] <= '9') || data[j] == '.') { // assume and disprove
														}
														else {
															isNumber = false;
														}
									j++;
									if (j == data.length) break;
								}

								// check if name by counting /
								int count = 0;
								for (int aa = jj + 1; aa < data.length; aa++) {
									if (data[aa] == '/') count++;
								}

								// lose spurious spaces
								while (data[jj] == 10 || data[jj] == 13 || data[jj] == 32)
									// || data[j]==47 || data[j]==60)
									jj++;

								if (typeFound == 4) {// direct ref done above
								}
								else
									if (count == 0 && data[jj] == '/') {

										if (debugFastCode) System.out.println(padding + "NameString ");

										jj = setNameStringValue(pdfObject, jj, data, map);
									}
									else
										if (data[jj] == '(') {

											if (debugFastCode) System.out.println(padding + "Textstream ");

											jj = readTextStream(pdfObject, jj, data, PDFkeyInt, ignoreRecursion);
										}
										else
											if (data[jj] == '[') {

												if (debugFastCode) System.out.println(padding + "Array ");

												ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, jj, this.endPt,
														PdfDictionary.VALUE_IS_STRING_ARRAY);
												jj = objDecoder.readArray(ignoreRecursion, data, pdfObject, PDFkeyInt);
												/**/
											}
											else
												if (typeFound == 0) {
													if (debugFastCode) System.out.println("Dictionary " + (char) +data[jj] + (char) data[jj + 1]);

													try {
														jj = readDictionaryFromRefOrDirect(-1, pdfObject, objRef, jj, data, PDFkeyInt);

													}
													catch (Exception e) {
														// tell user and log
														if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
													}

												}
												else
													if (isNumber) {

														if (debugFastCode) System.out.println("Number");

														jj = setNumberValue(pdfObject, jj, data, PDFkeyInt);

													}
													else
														if (typeFound == 1) {

															if (debugFastCode) System.out.println("Name");

															jj = setNameStringValue(pdfObject, jj, data, map);

														}
														else
															if (debugFastCode) System.out.println(padding + "Not read");

								if (!isRef) i = jj;
							}
		return i;
	}

	private static int setTypeForDecodeParams(int i, byte[] raw, int length, int pdfKeyType) {
		int ii = i;

		// roll onto first valid char
		while (ii < length && (raw[ii] == 32 || raw[ii] == 9 || raw[ii] == 13 || raw[ii] == 10))
			ii++;

		// see if might be object arrays
		if (raw[ii] != '<') {

			// roll onto first valid char
			while (ii < length && (raw[ii] == 32 || raw[ii] == 9 || raw[ii] == 13 || raw[ii] == 10 || raw[ii] == 91))
				ii++;

			if (raw[ii] == '<') pdfKeyType = PdfDictionary.VALUE_IS_OBJECT_ARRAY;

		}
		return pdfKeyType;
	}

	private int setNameTreeValue(PdfObject pdfObject, int i, byte[] raw, int length, boolean ignoreRecursion) {

		boolean isRef = false;

		// move to start
		while (raw[i] != '[') { // can be number as well

			if (raw[i] == '(') { // allow for W (7)
				isRef = false;
				break;
			}

			// allow for number as in refer 9 0 R
			if (raw[i] >= '0' && raw[i] <= '9') {
				isRef = true;
				break;
			}

			i++;
		}

		// allow for direct or indirect
		byte[] data = raw;

		int start = i, j = i;

		int count = 0;

		// read ref data and slot in
		if (isRef) {
			// number
			int keyStart2 = i;
			while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62)
				i++;

			int number = NumberUtils.parseInt(keyStart2, i, raw);

			// generation
			while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
				i++;

			keyStart2 = i;
			// move cursor to end of reference
			while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62)
				i++;
			int generation = NumberUtils.parseInt(keyStart2, i, raw);

			// move cursor to start of R
			while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
				i++;

			if (raw[i] != 82) // we are expecting R to end ref
			throw new RuntimeException("3. Unexpected value in file " + raw[i] + " - please send to IDRsolutions for analysis");

			if (!ignoreRecursion) {

				// read the Dictionary data
				data = this.objectReader.readObjectAsByteArray(pdfObject, this.objectReader.isCompressed(number, generation), number, generation);

				// allow for data in Linear object not yet loaded
				if (data == null) {
					pdfObject.setFullyResolved(false);

					if (debugFastCode) System.out.println(padding + "Data not yet loaded");

					if (LogWriter.isOutput()) LogWriter.writeLog("[Linearized] " + pdfObject.getObjectRefAsString() + " not yet available (1)");

					i = length;
					return i;
				}

				// lose obj at start
				j = 3;
				while (data[j - 1] != 106 && data[j - 2] != 98 && data[j - 3] != 111)
					j++;

				// skip any spaces after
				while (data[j] == 10 || data[j] == 13 || data[j] == 32)
					// || data[j]==47 || data[j]==60)
					j++;

				// reset pointer
				start = j;

			}
		}

		// move to end
		while (j < data.length) {

			if (data[j] == '[' || data[j] == '(') count++;
			else
				if (data[j] == ']' || data[j] == ')') count--;

			if (count == 0) break;

			j++;
		}

		if (!ignoreRecursion) {
			int stringLength = j - start + 1;
			byte[] newString = new byte[stringLength];

			System.arraycopy(data, start, newString, 0, stringLength);
			if (pdfObject.getObjectType() != PdfDictionary.Encrypt && this.decryption != null) {
				try {
					newString = this.decryption.decrypt(newString, pdfObject.getObjectRefAsString(), false, null, false, false);
				}
				catch (PdfSecurityException e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}

			pdfObject.setTextStreamValue(this.PDFkeyInt, newString);

			if (debugFastCode) System.out.println(padding + "name=" + new String(newString) + " set in " + pdfObject);
		}

		// roll on
		if (!isRef) i = j;
		return i;
	}

	private int setDictionaryValue(PdfObject pdfObject, int i, byte[] raw, boolean ignoreRecursion) {
		/**
		 * workout actual end as not always returned right
		 */
		int end = i;
		int nextC = i;

		// ignore any gaps
		while (raw[nextC] == 10 || raw[nextC] == 32 || raw[nextC] == 9)
			nextC++;

		// allow for null object
		if (raw[nextC] == 'n' && raw[nextC + 1] == 'u' && raw[nextC + 2] == 'l' && raw[nextC + 3] == 'l') {
			i = nextC + 4;
			return i;
		}

		if (raw[i] != '<' && raw[i + 1] != '<') end = end + 2;

		boolean inDictionary = true;
		boolean isKey = raw[end - 1] == '/';

		while (inDictionary) {

			if (raw[end] == '<' && raw[end + 1] == '<') {
				int level2 = 1;
				end++;
				while (level2 > 0) {

					if (raw[end] == '<' && raw[end + 1] == '<') {
						level2++;
						end = end + 2;
					}
					else
						if (raw[end - 1] == '>' && raw[end] == '>') {
							level2--;
							if (level2 > 0) end = end + 2;
						}
						else end++;
				}

				inDictionary = false;

			}
			else
				if (raw[end] == 'R') {
					inDictionary = false;
				}
				else
					if (isKey && (raw[end] == ' ' || raw[end] == 13 || raw[end] == 10 || raw[end] == 9)) {
						inDictionary = false;
					}
					else
						if (raw[end] == '/') {
							inDictionary = false;
							end--;
						}
						else
							if (raw[end] == '>' && raw[end + 1] == '>') {
								inDictionary = false;
								end--;
							}
							else end++;
		}

		// boolean save=debugFastCode;
		readDictionary(pdfObject, i, raw, this.PDFkeyInt, ignoreRecursion);

		// use correct value
		return end;
	}

	private int setFloatValue(PdfObject pdfObject, int i, byte[] raw, int length) {

		// roll on
		i++;

		while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47)
			i++;

		int keyStart = i;

		// move cursor to end of text
		while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62) {
			i++;
		}

		// actual value or first part of ref
		float number = NumberUtils.parseFloat(keyStart, i, raw);

		// roll onto next nonspace char and see if number
		int jj = i;
		while (jj < length && (raw[jj] == 32 || raw[jj] == 13 || raw[jj] == 10))
			jj++;

		// check its not a ref (assumes it XX 0 R)
		if (raw[jj] >= 48 && raw[jj] <= 57) { // if next char is number 0-9 its a ref

			// move cursor to start of generation
			while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
				i++;

			/**
			 * get generation number
			 */
			keyStart = i;
			// move cursor to end of reference
			while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62)
				i++;

			int generation = NumberUtils.parseInt(keyStart, i, raw);

			// move cursor to start of R
			while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
				i++;

			if (raw[i] != 82) { // we are expecting R to end ref
				throw new RuntimeException("3. Unexpected value in file - please send to IDRsolutions for analysis");
			}

			// read the Dictionary data
			byte[] data = this.objectReader.readObjectAsByteArray(pdfObject, this.objectReader.isCompressed((int) number, generation), (int) number,
					generation);

			// allow for data in Linear object not yet loaded
			if (data == null) {
				pdfObject.setFullyResolved(false);

				if (debugFastCode) System.out.println(padding + "Data not yet loaded");

				if (LogWriter.isOutput()) LogWriter.writeLog("[Linearized] " + pdfObject.getObjectRefAsString() + " not yet available (3)");

				i = length;
				return i;
			}

			// lose obj at start
			int j = 3;
			while (data[j - 1] != 106 && data[j - 2] != 98 && data[j - 3] != 111)
				j++;

			// skip any spaces after
			while (data[j] == 10 || data[j] == 13 || data[j] == 32)
				// || data[j]==47 || data[j]==60)
				j++;

			int count = j;

			// skip any spaces at end
			while (data[count] != 10 && data[count] != 13 && data[count] != 32) {// || data[j]==47 || data[j]==60)
				count++;
			}

			number = NumberUtils.parseFloat(j, count, data);

		}

		// store value
		pdfObject.setFloatNumber(this.PDFkeyInt, number);

		if (debugFastCode) System.out.println(padding + "set key in numberValue=" + number);// +" in "+pdfObject);

		i--;// move back so loop works
		return i;
	}

	private int setStringKeyValue(PdfObject pdfObject, int i, byte[] raw) {
		int keyStart;
		int keyLength;
		i++;

		// move cursor to start of text
		while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47)
			i++;

		keyStart = i;
		keyLength = 1;

		boolean isNull = false;

		// move cursor to end of text (allow for null)
		while (raw[i] != 'R' && !isNull) {

			// allow for null for Parent
			if (this.PDFkeyInt == PdfDictionary.Parent && raw[i] == 'n' && raw[i + 1] == 'u' && raw[i + 2] == 'l' && raw[i + 3] == 'l') isNull = true;

			i++;
			keyLength++;
		}

		i--;// move back so loop works

		if (!isNull) {

			// set value
			byte[] stringBytes = new byte[keyLength];
			System.arraycopy(raw, keyStart, stringBytes, 0, keyLength);

			// store value
			pdfObject.setStringKey(this.PDFkeyInt, stringBytes);

			if (debugFastCode) System.out.println(padding + "Set constant in " + pdfObject + " to " + new String(stringBytes));
		}
		return i;
	}

	private int setDictionaryValue(PdfObject pdfObject, int i, byte[] raw, int length, boolean ignoreRecursion) {

		if (debugFastCode) System.out.println(padding + ">>>Reading Dictionary Pairs i=" + i + ' ' + (char) raw[i] + (char) raw[i + 1]
				+ (char) raw[i + 2] + (char) raw[i + 3] + (char) raw[i + 4] + (char) raw[i + 5] + (char) raw[i + 6]);

		// move cursor to start of text
		while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47)
			i++;

		// set data which will be switched below if ref
		byte[] data = raw;
		int j = i;

		// get next key to see if indirect
		boolean isRef = data[j] != '<';

		if (isRef) {

			// number
			int keyStart2 = i;
			while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62)
				i++;

			int number = NumberUtils.parseInt(keyStart2, i, raw);

			// generation
			while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
				i++;

			keyStart2 = i;
			// move cursor to end of reference
			while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62)
				i++;
			int generation = NumberUtils.parseInt(keyStart2, i, raw);

			// move cursor to start of R
			while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
				i++;

			if (raw[i] != 82) // we are expecting R to end ref
			throw new RuntimeException("3. Unexpected value in file " + raw[i] + " - please send to IDRsolutions for analysis");

			if (!ignoreRecursion) {

				// read the Dictionary data
				data = this.objectReader.readObjectAsByteArray(pdfObject, this.objectReader.isCompressed(number, generation), number, generation);

				// allow for data in Linear object not yet loaded
				if (data == null) {
					pdfObject.setFullyResolved(false);

					if (debugFastCode) System.out.println(padding + "Data not yet loaded");

					if (LogWriter.isOutput()) LogWriter.writeLog("[Linearized] " + pdfObject.getObjectRefAsString() + " not yet available (2)");

					i = length;
					return i;
				}

				if (data[0] == '<' && data[1] == '<') {
					j = 0;
				}
				else {
					// lose obj at start
					j = 3;

					while (data[j - 1] != 106 && data[j - 2] != 98 && data[j - 3] != 111) {

						if (data[j] == '/') { // trap for odd case
							j = 0;
							break;
						}

						j++;

						if (j == data.length) { // some missing obj so catch these
							j = 0;
							break;
						}
					}

					// skip any spaces after
					while (data[j] == 10 || data[j] == 13 || data[j] == 32)
						// || data[j]==47 || data[j]==60)
						j++;
				}

			}
		}

		// allow for empty object (ie /Pattern <<>> )
		int endJ = j;
		while (data[endJ] == '<' || data[endJ] == ' ' || data[endJ] == 13 || data[endJ] == 10)
			endJ++;

		if (data[endJ] == '>') { // empty object
			j = endJ + 1;
		}
		else {

			PdfObject valueObj = ObjectFactory.createObject(this.PDFkeyInt, pdfObject.getObjectRefAsString(), pdfObject.getObjectType(),
					pdfObject.getID());
			valueObj.setID(this.PDFkeyInt);

			/**
			 * read pairs (stream in data starting at j)
			 */
			if (ignoreRecursion) // just skip to end
			j = readKeyPairs(this.PDFkeyInt, data, j, -2, null);
			else {
				// count values first
				int count = readKeyPairs(this.PDFkeyInt, data, j, -1, null);

				// now set values
				j = readKeyPairs(this.PDFkeyInt, data, j, count, valueObj);

				// store value
				pdfObject.setDictionary(this.PDFkeyInt, valueObj);

				if (debugFastCode) System.out.println(padding + "Set Dictionary " + count + " pairs type in " + pdfObject + " to " + valueObj);
			}
		}

		// update pointer if direct so at end (if ref already in right place)
		if (!isRef) {
			i = j;

			if (debugFastCode) System.out.println(i + ">>>>" + data[i - 2] + ' ' + data[i - 1] + " >" + data[i] + "< " + data[i + 1] + ' '
					+ data[i + 2]);
		}
		return i;
	}

	private int setStringConstantValue(PdfObject pdfObject, int i, byte[] raw) {

		i++;

		// move cursor to start of text
		while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47)
			i++;

		int keyStart = i;
		int keyLength = 0;

		// move cursor to end of text
		while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62) {
			i++;
			keyLength++;
		}

		i--;// move back so loop works

		// store value
		pdfObject.setConstant(this.PDFkeyInt, keyStart, keyLength, raw);

		if (debugFastCode) System.out.println(padding + "Set constant in " + pdfObject + " to "
				+ pdfObject.setConstant(this.PDFkeyInt, keyStart, keyLength, raw));

		return i;
	}

	private int setBooleanValue(PdfObject pdfObject, int i, byte[] raw, int PDFkeyInt) {
		int keyStart;

		i++;

		// move cursor to start of text
		while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47) {
			// System.out.println("skip="+raw[i]);
			i++;
		}

		keyStart = i;

		// move cursor to end of text
		while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62) {
			// System.out.println("key="+raw[i]+" "+(char)raw[i]);
			i++;
		}

		i--;// move back so loop works

		// store value
		if (raw[keyStart] == 't' && raw[keyStart + 1] == 'r' && raw[keyStart + 2] == 'u' && raw[keyStart + 3] == 'e') {
			pdfObject.setBoolean(PDFkeyInt, true);

			if (debugFastCode) System.out.println(padding + "Set Boolean true " + this.PDFkey + " in " + pdfObject);

		}
		else
			if (raw[keyStart] == 'f' && raw[keyStart + 1] == 'a' && raw[keyStart + 2] == 'l' && raw[keyStart + 3] == 's' && raw[keyStart + 4] == 'e') {
				pdfObject.setBoolean(PDFkeyInt, false);

				if (debugFastCode) System.out.println(padding + "Set Boolean false " + this.PDFkey + " in " + pdfObject);

			}
			else throw new RuntimeException("Unexpected value for Boolean value for" + PDFkeyInt + '=' + this.PDFkey);
		return i;
	}

	private int setTextStreamValue(PdfObject pdfObject, int i, byte[] raw, boolean ignoreRecursion) {

		if (raw[i + 1] == 40 && raw[i + 2] == 41) { // allow for empty stream
			i = i + 3;
			pdfObject.setTextStreamValue(this.PDFkeyInt, new byte[1]);

			if (raw[i] == '/') i--;
		}
		else i = readTextStream(pdfObject, i, raw, this.PDFkeyInt, ignoreRecursion);

		return i;
	}

	private void setFieldNames(PdfObject pdfObject) {

		String fieldName = pdfObject.getTextStreamValue(PdfDictionary.T);

		if (fieldName != null) {

			// at this point newString is the raw byte value (99% of the time this is the
			// string but it can be encode in some other ways (like a set of hex values)
			// so we need to use PdfReader.getTextString(newString, false) rather than new String(newString)
			// 6 0 obj <</T <FEFF0066006F0072006D0031005B0030005D>
			//
			// Most of the time you can forget about this because getTextStream() handles it for you
			//
			// Except here where we are manipulating the bytes directly...
			String parent = pdfObject.getStringKey(PdfDictionary.Parent);

			// if no name, or parent has one recursively scan tree for one in Parent
			boolean isMultiple = false;

			while (parent != null) {

				FormObject parentObj = new FormObject(parent, false);
				this.objectReader.readObject(parentObj);

				String newName = parentObj.getTextStreamValue(PdfDictionary.T);
				if (fieldName == null && newName != null) fieldName = newName;
				else
					if (newName != null) {
						// we pass in kids data so stop name.name
						if (!fieldName.equals(newName) || !parent.equals(pdfObject.getObjectRefAsString())) {
							fieldName = newName + '.' + fieldName;
							isMultiple = true;
						}
					}
				if (newName == null) break;

				parent = parentObj.getParentRef();
			}

			// set the field name to be the Fully Qualified Name
			if (isMultiple) pdfObject.setTextStreamValue(PdfDictionary.T, StringUtils.toBytes(fieldName));
		}
	}

	private void readStreamData(PdfObject pdfObject, int i, byte[] raw, int length) {

		for (int xx = i; xx < length - 5; xx++) {

			// avoid reading on subobject ie << /DecodeParams << >> >>
			if (raw[xx] == '>' && raw[xx + 1] == '>') break;

			if (raw[xx] == 's' && raw[xx + 1] == 't' && raw[xx + 2] == 'r' && raw[xx + 3] == 'e' && raw[xx + 4] == 'a' && raw[xx + 5] == 'm') {

				if (debugFastCode) System.out.println(padding + "1. Stream found afterwards");

				if (!pdfObject.isCached()) readStreamIntoObject(pdfObject, xx, raw);

				xx = length;
			}
		}
	}

	private static int getPairedValues(PdfObject pdfObject, int i, byte[] raw, int pdfKeyType, int length, int keyLength, int keyStart) {

		boolean isPair = false;

		int jj = i;

		while (jj < length) {

			// ignore any spaces
			while (jj < length && (raw[jj] == 32 || raw[jj] == 10 || raw[jj] == 13 || raw[jj] == 10))
				jj++;

			// number (possibly reference)
			if (jj < length && raw[jj] >= '0' && raw[jj] <= '9') {

				// rest of ref
				while (jj < length && raw[jj] >= '0' && raw[jj] <= '9')
					jj++;

				// ignore any spaces
				while (jj < length && (raw[jj] == 32 || raw[jj] == 10 || raw[jj] == 13 || raw[jj] == 10))
					jj++;

				// generation and spaces
				while (jj < length && ((raw[jj] >= '0' && raw[jj] <= '9') || (raw[jj] == 32 || raw[jj] == 10 || raw[jj] == 13 || raw[jj] == 10)))
					jj++;

				// not a ref
				if (jj >= length || raw[jj] != 'R') break;

				// roll past R
				jj++;
			}

			// ignore any spaces
			while (jj < length && (raw[jj] == 32 || raw[jj] == 10 || raw[jj] == 13 || raw[jj] == 10))
				jj++;

			// must be next key or end
			if (raw[jj] == '>' && raw[jj + 1] == '>') {
				isPair = true;
				break;
			}
			else
				if (raw[jj] != '/') break;

			jj++;

			// ignore any spaces
			while (jj < length && (raw[jj] != 32 && raw[jj] != 10 && raw[jj] != 13 && raw[jj] != 10))
				jj++;

		}

		if (isPair) {
			pdfObject.setCurrentKey(PdfDictionary.getKey(keyStart, keyLength, raw));
			return PdfDictionary.VALUE_IS_UNREAD_DICTIONARY;
		}
		else return pdfKeyType;
	}

	private int setUnreadDictionaryValue(PdfObject pdfObject, int i, byte[] raw) {

		if (raw[i] != '<') // roll on
		i++;

		while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 9)
			// move cursor to start of text
			i++;

		int start = i, keyStart, keyLength;

		// create and store stub
		PdfObject valueObj = ObjectFactory.createObject(this.PDFkeyInt, pdfObject.getObjectRefAsString(), pdfObject.getObjectType(),
				pdfObject.getID());
		valueObj.setID(this.PDFkeyInt);

		if (raw[i] == 'n' && raw[i + 1] == 'u' && raw[i + 2] == 'l' && raw[i + 3] == 'l') { // allow for null
		}
		else pdfObject.setDictionary(this.PDFkeyInt, valueObj);

		int status = PdfObject.UNDECODED_DIRECT; // assume not object and reset below if wrong

		// some objects can have a common value (ie /ToUnicode /Identity-H
		if (raw[i] == 47) { // not worth caching

			// move cursor to start of text
			while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
				i++;

			keyStart = i;
			keyLength = 0;

			// move cursor to end of text
			while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62) {
				i++;
				keyLength++;
			}

			i--;// move back so loop works

			// store value
			int constant = valueObj.setConstant(this.PDFkeyInt, keyStart, keyLength, raw);

			if (constant == PdfDictionary.Unknown || this.isInlineImage) {

				byte[] newStr = new byte[keyLength];
				System.arraycopy(raw, keyStart, newStr, 0, keyLength);

				String s = new String(newStr);
				valueObj.setGeneralStringValue(s);

			}

			status = PdfObject.DECODED;

		}
		else
			// allow for empty object
			if (raw[i] == 'e' && raw[i + 1] == 'n' && raw[i + 2] == 'd' && raw[i + 3] == 'o' && raw[i + 4] == 'b') {}
			else { // we need to ref from ref elsewhere which may be indirect [ref], hence loop

				// roll onto first valid char
				while ((raw[i] == 91 && this.PDFkeyInt != PdfDictionary.ColorSpace) || raw[i] == 32 || raw[i] == 13 || raw[i] == 10) {
					i++;
				}

				// roll on and ignore
				if (raw[i] == '<' && raw[i + 1] == '<') {

					i = i + 2;
					int reflevel = 1;

					while (reflevel > 0) {
						if (raw[i] == '<' && raw[i + 1] == '<') {
							i = i + 2;
							reflevel++;
						}
						else
							if (raw[i] == '>' && i + 1 == raw.length) {
								reflevel = 0;
							}
							else
								if (raw[i] == '>' && raw[i + 1] == '>') {
									i = i + 2;
									reflevel--;
								}
								else i++;
					}
				}
				else
					if (raw[i] == '[') {

						i++;
						int reflevel = 1;

						while (reflevel > 0) {

							if (raw[i] == '(') { // allow for [[ in stream ie [/Indexed /DeviceRGB 255 (abc[[z

								i++;
								while (raw[i] != ')' || ObjectUtils.isEscaped(raw, i))
									i++;

							}
							else
								if (raw[i] == '[') {
									reflevel++;
								}
								else
									if (raw[i] == ']') {
										reflevel--;
									}

							i++;
						}
						i--;
					}
					else
						if (raw[i] == 'n' && raw[i + 1] == 'u' && raw[i + 2] == 'l' && raw[i + 3] == 'l') { // allow for null
							i = i + 4;
						}
						else { // must be a ref

							// assume not object and reset below if wrong
							status = PdfObject.UNDECODED_REF;

							while (raw[i] != 'R' || raw[i - 1] == 'e') { // second condition to stop spurious match on DeviceRGB
								i++;

								if (i == raw.length) break;
							}
							i++;

							if (i >= raw.length) i = raw.length - 1;
						}
			}

		valueObj.setStatus(status);
		if (status != PdfObject.DECODED) {

			int StrLength = i - start;
			byte[] unresolvedData = new byte[StrLength];
			System.arraycopy(raw, start, unresolvedData, 0, StrLength);

			// check for returns in data if ends with R and correct to space
			if (unresolvedData[StrLength - 1] == 82) {

				for (int jj = 0; jj < StrLength; jj++) {

					if (unresolvedData[jj] == 10 || unresolvedData[jj] == 13) unresolvedData[jj] = 32;

				}
			}
			valueObj.setUnresolvedData(unresolvedData, this.PDFkeyInt);

		}

		if (raw[i] == '/' || raw[i] == '>') // move back so loop works
		i--;
		return i;
	}

	int readDictionary(PdfObject pdfObject, int i, byte[] raw, int PDFkeyInt, boolean ignoreRecursion) {

		int keyLength, keyStart;

		String objectRef = pdfObject.getObjectRefAsString();

		// roll on
		if (raw[i] != '<') i++;

		// move cursor to start of text
		while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32)
			i++;

		// some objects can have a common value (ie /ToUnicode /Identity-H
		if (raw[i] == 47) {

			if (debugFastCode) System.out.println(padding + "Indirect");

			// move cursor to start of text
			while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
				i++;

			keyStart = i;
			keyLength = 0;

			// move cursor to end of text
			while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62) {
				i++;
				keyLength++;
			}

			i--;// move back so loop works

			if (!ignoreRecursion) {

				PdfObject valueObj = ObjectFactory.createObject(PDFkeyInt, objectRef, pdfObject.getObjectType(), pdfObject.getID());
				valueObj.setID(PDFkeyInt);

				// store value
				int constant = valueObj.setConstant(PDFkeyInt, keyStart, keyLength, raw);

				if (constant == PdfDictionary.Unknown || this.isInlineImage) {

					byte[] newStr = new byte[keyLength];
					System.arraycopy(raw, keyStart, newStr, 0, keyLength);

					String s = new String(newStr);
					valueObj.setGeneralStringValue(s);

					if (debugFastCode) System.out.println(padding + "Set Dictionary as String=" + s + "  in " + pdfObject + " to " + valueObj);

				}
				else
					if (debugFastCode) System.out.println(padding + "Set Dictionary as constant=" + constant + "  in " + pdfObject + " to "
							+ valueObj);

				// store value
				pdfObject.setDictionary(PDFkeyInt, valueObj);

				if (pdfObject.isDataExternal()) {
					valueObj.isDataExternal(true);
					if (!this.resolveFully(valueObj)) pdfObject.setFullyResolved(false);
				}
			}

		}
		else
			// allow for empty object
			if (raw[i] == 'e' && raw[i + 1] == 'n' && raw[i + 2] == 'd' && raw[i + 3] == 'o' && raw[i + 4] == 'b') {
				// return i;

				if (debugFastCode) System.out.println(padding + "Empty object" + new String(raw) + "<<");

			}
			else
				if (raw[i] == '(' && PDFkeyInt == PdfDictionary.JS) { // ie <</S/JavaScript/JS( for JS
					i++;
					int start = i;
					// find end
					while (i < raw.length) {
						i++;
						if (raw[i] == ')' && !ObjectUtils.isEscaped(raw, i)) break;
					}
					byte[] data = ObjectUtils.readEscapedValue(i, raw, start, false);

					NamesObject JS = new NamesObject(objectRef);
					JS.setDecodedStream(data);
					pdfObject.setDictionary(PdfDictionary.JS, JS);

				}
				else { // we need to ref from ref elsewhere which may be indirect [ref], hence loop

					if (debugFastCode) System.out.println(padding + "1.About to read ref orDirect i=" + i + " char=" + (char) raw[i]
							+ " ignoreRecursion=" + ignoreRecursion);

					if (ignoreRecursion) {

						// roll onto first valid char
						while (raw[i] == 91 || raw[i] == 32 || raw[i] == 13 || raw[i] == 10) {

							// if(raw[i]==91) //track incase /Mask [19 19]
							// possibleArrayStart=i;

							i++;
						}

						// roll on and ignore
						if (raw[i] == '<' && raw[i + 1] == '<') {

							i = i + 2;
							int reflevel = 1;

							while (reflevel > 0) {
								if (raw[i] == '<' && raw[i + 1] == '<') {
									i = i + 2;
									reflevel++;
								}
								else
									if (raw[i] == '>' && raw[i + 1] == '>') {
										i = i + 2;
										reflevel--;
									}
									else i++;
							}
							i--;

						}
						else { // must be a ref
								// while(raw[i]!='R')
								// i++;
								// i++;
								// System.out.println("read ref");
							i = readDictionaryFromRefOrDirect(PDFkeyInt, pdfObject, objectRef, i, raw, PDFkeyInt);
						}

						if (i < raw.length && raw[i] == '/') // move back so loop works
						i--;

					}
					else {
						i = readDictionaryFromRefOrDirect(PDFkeyInt, pdfObject, objectRef, i, raw, PDFkeyInt);
					}
				}
		return i;
	}

	private int readTextStream(PdfObject pdfObject, int i, byte[] raw, int PDFkeyInt, boolean ignoreRecursion) {

		if (PDFkeyInt == PdfDictionary.W || PDFkeyInt == PdfDictionary.W2) {

			// we need to roll on as W2 is 2 chars and W is 1
			if (PDFkeyInt == PdfDictionary.W2) i++;

			boolean isRef = false;

			if (debugFastCode) System.out.println(padding + "Reading W or W2");

			// move to start
			while (raw[i] != '[') { // can be number as well

				// System.out.println((char) raw[i]);
				if (raw[i] == '(') { // allow for W (7)
					isRef = false;
					break;
				}

				// allow for number as in refer 9 0 R
				if (raw[i] >= '0' && raw[i] <= '9') {
					isRef = true;
					break;
				}

				i++;
			}

			// allow for direct or indirect
			byte[] data = raw;

			int start = i, j = i;

			int count = 0;

			// read ref data and slot in
			if (isRef) {
				// number
				int keyStart2 = i;
				while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62) {

					i++;

				}
				int number = NumberUtils.parseInt(keyStart2, i, raw);

				// generation
				while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
					i++;

				keyStart2 = i;
				// move cursor to end of reference
				while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62)
					i++;
				int generation = NumberUtils.parseInt(keyStart2, i, raw);

				// move cursor to start of R
				while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
					i++;

				if (raw[i] != 82) // we are expecting R to end ref
				throw new RuntimeException("3. Unexpected value in file " + raw[i] + " - please send to IDRsolutions for analysis");

				if (!ignoreRecursion) {

					// read the Dictionary data
					data = this.objectReader.readObjectAsByteArray(pdfObject, this.objectReader.isCompressed(number, generation), number, generation);

					// allow for data in Linear object not yet loaded
					if (data == null) {
						pdfObject.setFullyResolved(false);

						if (debugFastCode) System.out.println(padding + "Data not yet loaded");

						if (LogWriter.isOutput()) LogWriter.writeLog("[Linearized] " + pdfObject.getObjectRefAsString() + " not yet available (6)");

						return raw.length;
					}

					// lose obj at start
					j = 3;
					while (data[j - 1] != 106 && data[j - 2] != 98 && data[j - 3] != 111) {
						j++;

						// catch for error
						if (j == data.length) {
							j = 0;
							break;
						}
					}

					// skip any spaces after
					while (data[j] == 10 || data[j] == 13 || data[j] == 32)
						// || data[j]==47 || data[j]==60)
						j++;

					// reset pointer
					start = j;

				}
			}

			// move to end
			while (j < data.length) {

				if (data[j] == '[' || data[j] == '(') count++;
				else
					if (data[j] == ']' || data[j] == ')') count--;

				if (count == 0) break;

				j++;
			}

			if (!ignoreRecursion) {
				int stringLength = j - start + 1;
				byte[] newString = new byte[stringLength];

				System.arraycopy(data, start, newString, 0, stringLength);

				/**
				 * clean up so matches old string so old code works
				 */
				if (PDFkeyInt != PdfDictionary.JS) { // keep returns in code
					for (int aa = 0; aa < stringLength; aa++) {
						if (newString[aa] == 10 || newString[aa] == 13) newString[aa] = 32;
					}
				}

				pdfObject.setTextStreamValue(PDFkeyInt, newString);

				if (debugFastCode) {
					if (PDFkeyInt == 39) System.out.println(padding + pdfObject + " W=" + new String(newString));
					else System.out.println(padding + pdfObject + " W2=" + new String(newString));
				}
			}

			// roll on
			if (!isRef) i = j;
		}
		else {

			byte[] data;
			try {
				if (raw[i] != '<' && raw[i] != '(') i++;

				while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32)
					i++;

				// allow for no actual value but another key
				if (raw[i] == 47) {
					pdfObject.setTextStreamValue(PDFkeyInt, new byte[1]);
					i--;
					return i;
				}

				if (debugFastCode) {
					System.out.println(padding + "i=" + i + " Reading Text from String=" + new String(raw) + '<');

					System.out.println("-->>");
					for (int zz = i; zz < raw.length; zz++) {
						System.out.print((char) raw[zz]);
					}
					System.out.println("<<--");
				}

				// System.out.println("raw["+i+"]="+(char)raw[i]);
				// get next key to see if indirect
				boolean isRef = raw[i] != '<' && raw[i] != '(';

				int j = i;
				data = raw;
				if (isRef) {

					// number
					int keyStart2 = i;
					while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62) {

						i++;
					}

					int number = NumberUtils.parseInt(keyStart2, i, raw);

					// generation
					while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
						i++;

					keyStart2 = i;
					// move cursor to end of reference
					while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62)
						i++;
					int generation = NumberUtils.parseInt(keyStart2, i, raw);

					// move cursor to start of R
					while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
						i++;

					if (raw[i] != 82) // we are expecting R to end ref
					return raw.length;
					// throw new RuntimeException(i+" 3. Unexpected value in file " + (char) raw[i - 2]+ (char) raw[i-1] + (char) raw[i] + (char)
					// raw[i+1] + (char)
					// raw[i+2]+(char)raw[i]+" - please send to IDRsolutions for analysis "+pdfObject.getObjectRefAsString()+" "+pdfObject);

					if (!ignoreRecursion) {

						// read the Dictionary data
						data = this.objectReader.readObjectAsByteArray(pdfObject, this.objectReader.isCompressed(number, generation), number,
								generation);

						// System.out.println("data read is>>>>>>>>>>>>>>>>>>>\n");
						// for(int ab=0;ab<data.length;ab++)
						// System.out.print((char)data[ab]);
						// System.out.println("\n<<<<<<<<<<<<<<<<<<<\n");

						// allow for data in Linear object not yet loaded
						if (data == null) {
							pdfObject.setFullyResolved(false);

							if (debugFastCode) System.out.println(padding + "Data not yet loaded");

							if (LogWriter.isOutput()) LogWriter.writeLog("[Linearized] " + pdfObject.getObjectRefAsString()
									+ " not yet available (7)");

							return raw.length;
						}

						// lose obj at start
						if (data[0] == '(') {
							j = 0;
						}
						else {
							j = 3;
							while (data[j - 1] != 106 && data[j - 2] != 98 && data[j - 3] != 111)
								j++;

							// skip any spaces after
							while (data[j] == 10 || data[j] == 13 || data[j] == 32)
								// || data[j]==47 || data[j]==60)
								j++;
						}

					}
				}
				// ///////////////
				int start = 0;
				if (!isRef || !ignoreRecursion) {
					// move to start
					while (data[j] != '(' && data[j] != '<') {
						j++;

					}

					byte startChar = data[j];

					start = j;

					// move to end (allow for ((text in brackets))
					int bracketCount = 1;
					while (j < data.length) {
						// System.out.println(i+"="+raw[j]+" "+(char)raw[j]);
						j++;

						if (startChar == '(' && (data[j] == ')' || data[j] == '(') && !ObjectUtils.isEscaped(data, j)) {
							// allow for non-escaped brackets
							if (data[j] == '(') bracketCount++;
							else
								if (data[j] == ')') bracketCount--;

							if (bracketCount == 0) break;
						}

						if (startChar == '<' && (data[j] == '>' || data[j] == 0)) break;
					}

				}

				if (!ignoreRecursion) {

					byte[] newString;

					if (data[start] == '<') {
						start++;

						int byteCount = (j - start) >> 1;
						newString = new byte[byteCount];

						int byteReached = 0, topHex, bottomHex;
						while (true) {

							if (start == j) break;

							while (data[start] == 32 || data[start] == 10 || data[start] == 13)
								start++;

							topHex = data[start];

							// convert to number
							if (topHex >= 'A' && topHex <= 'F') {
								topHex = topHex - 55;
							}
							else
								if (topHex >= 'a' && topHex <= 'f') {
									topHex = topHex - 87;
								}
								else
									if (topHex >= '0' && topHex <= '9') {
										topHex = topHex - 48;
									}
									else {

										if (LogWriter.isOutput()) LogWriter.writeLog("Unexpected number " + (char) data[start]);

									}

							start++;

							while (data[start] == 32 || data[start] == 10 || data[start] == 13)
								start++;

							bottomHex = data[start];

							if (bottomHex >= 'A' && bottomHex <= 'F') {
								bottomHex = bottomHex - 55;
							}
							else
								if (bottomHex >= 'a' && bottomHex <= 'f') {
									bottomHex = bottomHex - 87;
								}
								else
									if (bottomHex >= '0' && bottomHex <= '9') {
										bottomHex = bottomHex - 48;
									}
									else {

										if (LogWriter.isOutput()) LogWriter.writeLog("Unexpected number " + (char) data[start]);

										return i;
									}

							start++;

							// calc total
							int finalValue = bottomHex + (topHex << 4);

							newString[byteReached] = (byte) finalValue;

							byteReached++;

						}

					}
					else {
						// roll past (
						if (data[start] == '(') start++;

						newString = ObjectUtils.readEscapedValue(j, data, start, PDFkeyInt == PdfDictionary.ID);
					}

					if (pdfObject.getObjectType() != PdfDictionary.Encrypt) {// && pdfObject.getObjectType()!=PdfDictionary.Outlines){

						try {
							if (this.decryption != null && !pdfObject.isInCompressedStream()) newString = this.decryption.decryptString(newString,
									pdfObject.getObjectRefAsString());
						}
						catch (PdfSecurityException e) {
							// tell user and log
							if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
						}
					}

					pdfObject.setTextStreamValue(PDFkeyInt, newString);

					if (debugFastCode) System.out.println(padding + "TextStream=" + new String(newString) + " in pdfObject=" + pdfObject);
				}

				if (!isRef) i = j;

			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}
		return i;
	}

	int setNumberValue(PdfObject pdfObject, int i, byte[] raw, int PDFkeyInt) {

		int keyStart = i, rawLength = raw.length;

		// move cursor to end of text
		while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62 && raw[i] != '(' && raw[i] != '.') {
			i++;
		}

		// actual value or first part of ref
		int number = NumberUtils.parseInt(keyStart, i, raw);

		// roll onto next nonspace char and see if number
		int jj = i;
		while (jj < rawLength && (raw[jj] == 32 || raw[jj] == 13 || raw[jj] == 10))
			jj++;

		boolean isRef = false;

		// check its not a ref (assumes it XX 0 R)
		if (raw[jj] >= 48 && raw[jj] <= 57) { // if next char is number 0-9 it may be a ref

			int aa = jj;

			// move cursor to end of number
			while ((raw[aa] != 10 && raw[aa] != 13 && raw[aa] != 32 && raw[aa] != 47 && raw[aa] != 60 && raw[aa] != 62))
				aa++;

			// move cursor to start of text
			while (aa < rawLength && (raw[aa] == 10 || raw[aa] == 13 || raw[aa] == 32 || raw[aa] == 47))
				aa++;

			isRef = aa < rawLength && raw[aa] == 'R';

		}

		if (isRef) {
			// move cursor to start of generation
			while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
				i++;

			/**
			 * get generation number
			 */
			keyStart = i;
			// move cursor to end of reference
			while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62)
				i++;

			int generation = NumberUtils.parseInt(keyStart, i, raw);

			// move cursor to start of R
			while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
				i++;

			if (raw[i] != 82) { // we are expecting R to end ref
				throw new RuntimeException("3. Unexpected value in file - please send to IDRsolutions for analysis");
			}

			// read the Dictionary data
			byte[] data = this.objectReader.readObjectAsByteArray(pdfObject, this.objectReader.isCompressed(number, generation), number, generation);

			// allow for data in Linear object not yet loaded
			if (data == null) {
				pdfObject.setFullyResolved(false);

				if (debugFastCode) System.out.println(padding + "Data not yet loaded");

				if (LogWriter.isOutput()) LogWriter.writeLog("[Linearized] " + pdfObject.getObjectRefAsString() + " not yet available (8)");

				return rawLength;
			}

			// lose obj at start
			int j = 0, len = data.length;

			// allow for example where start <<
			if (len > 1 && data[0] == '<' && data[1] == '<') {}
			else
				if (len < 3) { // fix for short indirect value /K 30 0 R where 30 0R is -1 ie (Customers-Dec2011/Real Estate Tax Bill 2011.pdf)
				}
				else {
					j = 3;
					if (len > 3) { // allow for small values (ie Rotate object pointing to value 0)
						while (data[j - 1] != 106 && data[j - 2] != 98 && data[j - 3] != 111) {
							j++;

							if (j == len) {
								j = 0;
								break;
							}
						}
					}
				}

			// skip any spaces after
			if (len > 1) {// allow for small values (ie Rotate object pointing to value 0)
				while (j < data.length && (data[j] == 9 || data[j] == 10 || data[j] == 13 || data[j] == 32))
					// || data[j]==47 || data[j]==60)
					j++;
			}

			int count = j;

			// skip any spaces at end
			while (count < len && data[count] != 9 && data[count] != 10 && data[count] != 13 && data[count] != 32)
				// || data[j]==47 || data[j]==60)
				count++;

			number = NumberUtils.parseInt(j, count, data);

		}

		// store value
		pdfObject.setIntNumber(PDFkeyInt, number);

		if (debugFastCode) System.out.println(padding + "set key=" + this.PDFkey + " numberValue=" + number);// +" in "+pdfObject);

		i--;// move back so loop works

		return i;
	}

	/**
	 * @param id
	 * @param pdfObject
	 * @param objectRef
	 * @param i
	 * @param raw
	 * @param PDFkeyInt
	 *            - -1 will store in pdfObject directly, not as separate object
	 */
	int readDictionaryFromRefOrDirect(int id, PdfObject pdfObject, String objectRef, int i, byte[] raw, int PDFkeyInt) {

		readDictionaryFromRefOrDirect: while (true) {

			int keyStart;
			int possibleArrayStart = -1;

			// @speed - find end so we can ignore once no longer reading into map as well
			// and skip to end of object
			// allow for [ref] or [<< >>] at top level (may be followed by gap)
			// good example is /PDFdata/baseline_screens/docusign/test3 _ Residential Purchase and Sale Agreement - 6-03.pdf
			while (raw[i] == 91 || raw[i] == 32 || raw[i] == 13 || raw[i] == 10) {

				if (raw[i] == 91) // track incase /Mask [19 19]
				possibleArrayStart = i;

				i++;
			}

			// some items like MAsk can be [19 19] or stream
			// and colorspace is law unto itself
			if (PDFkeyInt == PdfDictionary.ColorSpace || id == PdfDictionary.ColorSpace || pdfObject.getPDFkeyInt() == PdfDictionary.ColorSpace) {
				ColorObjectDecoder colDecoder = new ColorObjectDecoder(this.objectReader);
				return colDecoder.processColorSpace(pdfObject, pdfObject.getObjectRefAsString(), i, raw);
			}
			else
				if (possibleArrayStart != -1
						&& (PDFkeyInt == PdfDictionary.Mask || PDFkeyInt == PdfDictionary.TR || PDFkeyInt == PdfDictionary.OpenAction)) return processArray(
						pdfObject, raw, PDFkeyInt, possibleArrayStart);

			if (raw[i] == '%') { // if %comment roll onto next line
				while (raw[i] != 13 && raw[i] != 10)
					i++;

				// and lose space after
				while (raw[i] == 91 || raw[i] == 32 || raw[i] == 13 || raw[i] == 10)
					i++;
			}

			if (raw[i] == 60) { // [<<data inside brackets>>]

				i = convertDirectDictionaryToObject(pdfObject, objectRef, i, raw, PDFkeyInt);

			}
			else
				if (raw[i] == 47) { // direct value such as /DeviceGray

					i = ObjectUtils.setDirectValue(pdfObject, i, raw, PDFkeyInt);

				}
				else { // ref or [ref]

					int j = i, ref, generation;
					byte[] data = raw;

					while (true) {

						// allow for [ref] at top level (may be followed by gap
						while (data[j] == 91 || data[j] == 32 || data[j] == 13 || data[j] == 10)
							j++;

						// trap nulls as well
						boolean hasNull = false;

						while (true) {

							// trap null arrays ie [null null]
							if (hasNull && data[j] == ']') return j;

							/**
							 * get object ref
							 */
							keyStart = j;
							// move cursor to end of reference
							while (data[j] != 10 && data[j] != 13 && data[j] != 32 && data[j] != 47 && data[j] != 60 && data[j] != 62) {

								// trap null arrays ie [null null] or [null]

								if (data[j] == 'l' && data[j - 1] == 'l' && data[j - 2] == 'u' && data[j - 3] == 'n') hasNull = true;

								if (hasNull && data[j] == ']') return j;

								j++;
							}

							ref = NumberUtils.parseInt(keyStart, j, data);

							// move cursor to start of generation or next value
							while (data[j] == 10 || data[j] == 13 || data[j] == 32)
								// || data[j]==47 || data[j]==60)
								j++;

							// handle nulls
							if (ref != 69560 || data[keyStart] != 'n') break; // not null
							else {
								hasNull = true;
								if (data[j] == '<') { // /DecodeParms [ null << /K -1 /Columns 1778 >> ] ignore null and jump down to enclosed
														// Dictionary
									i = j;
									continue readDictionaryFromRefOrDirect;

								}
							}
						}

						/**
						 * get generation number
						 */
						keyStart = j;
						// move cursor to end of reference
						while (data[j] != 10 && data[j] != 13 && data[j] != 32 && data[j] != 47 && data[j] != 60 && data[j] != 62)
							j++;

						generation = NumberUtils.parseInt(keyStart, j, data);

						/**
						 * check R at end of reference and abort if wrong
						 */
						// move cursor to start of R
						while (data[j] == 10 || data[j] == 13 || data[j] == 32 || data[j] == 47 || data[j] == 60)
							j++;

						if (data[j] != 82) // we are expecting R to end ref
						throw new RuntimeException("ref=" + ref + " gen=" + ref + " 1. Unexpected value " + data[j]
								+ " in file - please send to IDRsolutions for analysis char=" + (char) data[j]);

						// read the Dictionary data
						data = this.objectReader.readObjectAsByteArray(pdfObject, this.objectReader.isCompressed(ref, generation), ref, generation);

						// allow for data in Linear object not yet loaded
						if (data == null) {
							pdfObject.setFullyResolved(false);

							if (LogWriter.isOutput()) LogWriter.writeLog("[Linearized] " + pdfObject.getObjectRefAsString()
									+ " not yet available (11)");

							return raw.length;
						}

						// disregard corrputed data from start of file
						if (data != null && data.length > 4 && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F') data = null;

						if (data == null) break;

						/**
						 * get not indirect and exit if not
						 */
						int j2 = 0;

						// allow for [91 0 r]
						if (data[j2] != '[' && data[0] != '<' && data[1] != '<') {

							while (j2 < 3 || (j2 > 2 && data[j2 - 1] != 106 && data[j2 - 2] != 98 && data[j2 - 3] != 111)) {

								// allow for /None as value
								if (data[j2] == '/') break;
								j2++;
							}

							// skip any spaces
							while (data[j2] != 91 && (data[j2] == 10 || data[j2] == 13 || data[j2] == 32))
								// || data[j]==47 || data[j]==60)
								j2++;
						}

						// if indirect, round we go again
						if (data[j2] != 91) {
							j = 0;
							break;
						}

						j = j2;
					}

					// allow for no data found (ie /PDFdata/baseline_screens/debug/hp_broken_file.pdf)
					if (data != null) {

						/**
						 * get id from stream
						 */
						// skip any spaces
						while (data[j] == 10 || data[j] == 13 || data[j] == 32)
							// || data[j]==47 || data[j]==60)
							j++;

						boolean isMissingValue = j < raw.length && raw[j] == '<';

						if (isMissingValue) { // check not <</Last
							// find first valid char
							int xx = j;
							while (xx < data.length && (raw[xx] == '<' || raw[xx] == 10 || raw[xx] == 13 || raw[xx] == 32))
								xx++;

							if (raw[xx] == '/') isMissingValue = false;
						}

						if (isMissingValue) { // missing value at start for some reason

							/**
							 * get object ref
							 */
							keyStart = j;
							// move cursor to end of reference
							while (data[j] != 10 && data[j] != 13 && data[j] != 32 && data[j] != 47 && data[j] != 60 && data[j] != 62)
								j++;

							ref = NumberUtils.parseInt(keyStart, j, data);

							// move cursor to start of generation
							while (data[j] == 10 || data[j] == 13 || data[j] == 32 || data[j] == 47 || data[j] == 60)
								j++;

							/**
							 * get generation number
							 */
							keyStart = j;
							// move cursor to end of reference
							while (data[j] != 10 && data[j] != 13 && data[j] != 32 && data[j] != 47 && data[j] != 60 && data[j] != 62)
								j++;

							generation = NumberUtils.parseInt(keyStart, j, data);

							// lose obj at start
							while (data[j - 1] != 106 && data[j - 2] != 98 && data[j - 3] != 111) {

								if (data[j] == '<') break;

								j++;
							}
						}

						// skip any spaces
						while (data[j] == 10 || data[j] == 13 || data[j] == 32 || data[j] == 9)
							// || data[j]==47 || data[j]==60)
							j++;

						// move to start of Dict values
						if (data[0] != 60) while (data[j] != 60 && data[j + 1] != 60) {

							// allow for null object
							if (data[j] == 'n' && data[j + 1] == 'u' && data[j + 2] == 'l' && data[j + 3] == 'l') return i;

							// allow for Direct value ie 2 0 obj /WinAnsiEncoding
							if (data[j] == 47) break;

							// allow for textStream (text)
							if (data[j] == '(') {
								j = readTextStream(pdfObject, j, data, PDFkeyInt, true);
								break;
							}

							j++;
						}

						i = handleValue(pdfObject, i, PDFkeyInt, j, ref, generation, data);
					}
				}

			return i;
		}
	}

	private int processArray(PdfObject pdfObject, byte[] raw, int PDFkeyInt, int possibleArrayStart) {
		int i;// find end
		int endPoint = possibleArrayStart;
		while (raw[endPoint] != ']' && endPoint <= raw.length)
			endPoint++;

		// convert data to new Dictionary object and store
		PdfObject valueObj = ObjectFactory.createObject(PDFkeyInt, null, pdfObject.getObjectType(), pdfObject.getID());
		valueObj.setID(PDFkeyInt);
		pdfObject.setDictionary(PDFkeyInt, valueObj);
		valueObj.ignoreRecursion(pdfObject.ignoreRecursion());

		if (valueObj.isDataExternal()) {
			valueObj.isDataExternal(true);
			if (!resolveFully(valueObj)) pdfObject.setFullyResolved(false);
		}

		int type = PdfDictionary.VALUE_IS_INT_ARRAY;
		if (PDFkeyInt == PdfDictionary.TR) type = PdfDictionary.VALUE_IS_KEY_ARRAY;

		ArrayDecoder objDecoder = new ArrayDecoder(this.objectReader, possibleArrayStart, endPoint, type);
		i = objDecoder.readArray(pdfObject.ignoreRecursion(), raw, valueObj, PDFkeyInt);

		// rollon
		return i;
	}

	private int handleValue(PdfObject pdfObject, int i, int PDFkeyInt, int j, int ref, int generation, byte[] data) {

		int keyStart;
		int keyLength;
		int dataLen = data.length;

		if (data[j] == 47) {
			j++; // roll on past /

			keyStart = j;
			keyLength = 0;

			// move cursor to end of text
			while (j < dataLen && data[j] != 10 && data[j] != 13 && data[j] != 32 && data[j] != 47 && data[j] != 60 && data[j] != 62) {
				j++;
				keyLength++;

			}

			i--;// move back so loop works

			if (PDFkeyInt == -1) {
				// store value directly
				pdfObject.setConstant(PDFkeyInt, keyStart, keyLength, data);

				if (debugFastCode) System.out.println(padding + "Set object Constant directly to "
						+ pdfObject.setConstant(PDFkeyInt, keyStart, keyLength, data));
			}
			else {
				// convert data to new Dictionary object
				PdfObject valueObj = ObjectFactory.createObject(PDFkeyInt, null, pdfObject.getObjectType(), pdfObject.getID());
				valueObj.setID(PDFkeyInt);
				// store value
				valueObj.setConstant(PDFkeyInt, keyStart, keyLength, data);
				pdfObject.setDictionary(PDFkeyInt, valueObj);

				if (pdfObject.isDataExternal()) {
					valueObj.isDataExternal(true);
					if (!resolveFully(valueObj)) pdfObject.setFullyResolved(false);
				}
			}
		}
		else {

			// convert data to new Dictionary object
			PdfObject valueObj;
			if (PDFkeyInt == -1) valueObj = pdfObject;
			else {
				valueObj = ObjectFactory.createObject(PDFkeyInt, ref, generation, pdfObject.getObjectType());
				valueObj.setID(PDFkeyInt);
				valueObj.setInCompressedStream(pdfObject.isInCompressedStream());

				if (pdfObject.isDataExternal()) {
					valueObj.isDataExternal(true);

					if (!resolveFully(valueObj)) pdfObject.setFullyResolved(false);
				}

				if (PDFkeyInt != PdfDictionary.Resources) valueObj.ignoreRecursion(pdfObject.ignoreRecursion());
			}

			ObjectDecoder objDecoder = new ObjectDecoder(this.objectReader);
			objDecoder.readDictionaryAsObject(valueObj, j, data);

			// store value
			if (PDFkeyInt != -1) pdfObject.setDictionary(PDFkeyInt, valueObj);
		}

		return i;
	}

	int convertDirectDictionaryToObject(PdfObject pdfObject, String objectRef, int i, byte[] raw, int PDFkeyInt) {

		// convert data to new Dictionary object
		PdfObject valueObj;

		if (PDFkeyInt == -1) {
			valueObj = pdfObject;

			// if only 1 item use that ref not parent and indirect (ie <</Metadata 38 0 R>>)
			int objCount = 0, refStarts = -1, refEnds = -1;
			if (raw[0] == '<') {
				for (int ii = 0; ii < raw.length; ii++) {

					// count keys
					if (raw[ii] == '/') objCount++;
					// find start of ref
					if (objCount == 1) {
						if (refStarts == -1) {
							if (raw[ii] > '0' && raw[ii] < '9') refStarts = ii;
						}
						else {
							if (raw[ii] == 'R') refEnds = ii + 1;
						}
					}
				}

				if (objCount == 1 && refStarts != -1 && refEnds != -1) {
					objectRef = new String(raw, refStarts, refEnds - refStarts);
					valueObj.setRef(objectRef);
				}
			}

		}
		else {
			valueObj = ObjectFactory.createObject(PDFkeyInt, objectRef, pdfObject.getObjectType(), pdfObject.getID());
			// <start-gpL>
			valueObj.setInCompressedStream(pdfObject.isInCompressedStream());
			// <end-gpl>
			valueObj.setID(PDFkeyInt);

			// if it is cached, we need to copy across data so in correct Obj
			// (we read data before we created object so in wrong obj at this point)
			if (pdfObject.isCached()) valueObj.moveCacheValues(pdfObject);

			if (debugFastCode) System.out.println("valueObj=" + valueObj + " pdfObject=" + pdfObject + " PDFkeyInt=" + PDFkeyInt + ' '
					+ pdfObject.getID() + ' ' + pdfObject.getParentID());
		}

		if (debugFastCode) System.out.println(padding + "Reading [<<data>>] to " + valueObj + " into " + pdfObject + " i=" + i);

		ObjectDecoder objDecoder = new ObjectDecoder(this.objectReader);
		i = objDecoder.readDictionaryAsObject(valueObj, i, raw);

		// needed to ensure >>>> works
		if (i < raw.length && raw[i] == '>') i--;

		if (debugFastCode) {
			System.out.println(padding + "data " + valueObj + " into pdfObject=" + pdfObject + " i=" + i);
		}

		// store value (already set above for -1
		if (PDFkeyInt != -1) pdfObject.setDictionary(PDFkeyInt, valueObj);

		// roll on to end
		int count = raw.length;
		while (i < count - 1 && raw[i] == 62 && raw[i + 1] == 62) { //
			i++;
			if (i + 1 < raw.length && raw[i + 1] == 62) // allow for >>>>
			break;
		}
		return i;
	}

	/**
	 * if pairs is -1 returns number of pairs otherwise sets pairs and returns point reached in stream
	 */
	private int readKeyPairs(int id, byte[] data, int j, int pairs, PdfObject pdfObject) {

		final boolean debug = false;

		int start = j, level;

		int numberOfPairs = pairs;

		if (debug) {
			int length = data.length;
			System.out.println("count=" + pairs + "============================================\n");
			for (int aa = j; aa < length; aa++) {
				System.out.print((char) data[aa]);

				if (aa > 5 && data[aa - 5] == 's' && data[aa - 4] == 't' && data[aa - 3] == 'r' && data[aa - 2] == 'e' && data[aa - 1] == 'a'
						&& data[aa] == 'm') aa = length;
			}
			System.out.println("\n============================================");
		}

		// same routine used to count first and then fill with values
		boolean isCountOnly = false, skipToEnd = false;
		byte[][] keys = null, values = null;
		PdfObject[] objs = null;

		if (pairs == -1) {
			isCountOnly = true;
		}
		else
			if (pairs == -2) {
				isCountOnly = true;
				skipToEnd = true;
			}
			else {
				keys = new byte[numberOfPairs][];
				values = new byte[numberOfPairs][];
				objs = new PdfObject[numberOfPairs];

				if (debug) System.out.println("Loading " + numberOfPairs + " pairs");
			}
		pairs = 0;

		while (true) {

			// move cursor to start of text
			while (data[start] == 9 || data[start] == 10 || data[start] == 13 || data[start] == 32 || data[start] == 60)
				start++;

			// allow for comment
			if (data[start] == 37) {
				while (data[start] != 10 && data[start] != 13) {
					// System.out.println(data[start]+" "+(char)data[start]);
					start++;
				}

				// move cursor to start of text
				while (data[start] == 9 || data[start] == 10 || data[start] == 13 || data[start] == 32 || data[start] == 60)
					start++;
			}

			// exit at end
			if (data[start] == 62) break;

			// count token or tell user problem
			if (data[start] == 47) {
				pairs++;
				start++;
			}
			else throw new RuntimeException("Unexpected value " + data[start] + " - not key pair");

			// read token key and save if on second run
			int tokenStart = start;
			while (data[start] != 32 && data[start] != 10 && data[start] != 13 && data[start] != '[' && data[start] != '<' && data[start] != '/')
				start++;

			int tokenLength = start - tokenStart;

			byte[] tokenKey = new byte[tokenLength];
			System.arraycopy(data, tokenStart, tokenKey, 0, tokenLength);

			if (!isCountOnly) // pairs already rolled on so needs to be 1 less
			keys[pairs - 1] = tokenKey;

			// now skip any spaces to key or text
			while (data[start] == 10 || data[start] == 13 || data[start] == 32)
				start++;

			boolean isDirect = data[start] == 60 || data[start] == '[' || data[start] == '/';

			byte[] dictData;

			if (debug) System.out.println("token=" + new String(tokenKey) + " isDirect " + isDirect);

			if (isDirect) {
				// get to start at <<
				while (data[start - 1] != '<' && data[start] != '<' && data[start] != '[' && data[start] != '/')
					start++;

				int streamStart = start;

				// find end
				boolean isObject = true;

				if (data[start] == '<') {
					start = start + 2;
					level = 1;

					while (level > 0) {
						// System.out.print((char)data[start]);
						if (data[start] == '<' && data[start + 1] == '<') {
							start = start + 2;
							level++;
						}
						else
							if (data[start] == '>' && data[start + 1] == '>') {
								start = start + 2;
								level--;
							}
							else start++;
					}

					// System.out.println("\n<---------------"+start);

					// if(data[start]=='>' && data[start+1]=='>')
					// start=start+2;
				}
				else
					if (data[start] == '[') {

						level = 1;
						start++;

						boolean inStream = false;

						while (level > 0) {

							// allow for streams
							if (!inStream && data[start] == '(') inStream = true;
							else
								if (inStream && data[start] == ')' && (data[start - 1] != '\\' || data[start - 2] == '\\')) inStream = false;

							// System.out.println((char)data[start]);

							if (!inStream) {
								if (data[start] == '[') level++;
								else
									if (data[start] == ']') level--;
							}

							start++;
						}

						isObject = false;
					}
					else
						if (data[start] == '/') {
							start++;
							while (data[start] != '/' && data[start] != 10 && data[start] != 13 && data[start] != 32) {
								start++;

								if (start < data.length - 1 && data[start] == '>' && data[start + 1] == '>') break;
							}
						}

				if (!isCountOnly) {
					int len = start - streamStart;
					dictData = new byte[len];
					System.arraycopy(data, streamStart, dictData, 0, len);
					// pairs already rolled on so needs to be 1 less
					values[pairs - 1] = dictData;

					String ref = pdfObject.getObjectRefAsString();

					// @speed - will probably need to change as we add more items

					if (pdfObject.getObjectType() == PdfDictionary.ColorSpace) {

						ColorObjectDecoder colDecoder = new ColorObjectDecoder(this.objectReader);

						// isDirect avoids storing multiple direct objects as will overwrite each other
						if (isObject && !isDirect) {
							colDecoder.handleColorSpaces(pdfObject, 0, dictData);
							objs[pairs - 1] = pdfObject;
						}
						else {
							ColorSpaceObject colObject = new ColorSpaceObject(ref);

							if (isDirect) colObject.setRef(-1, 0);

							colDecoder.handleColorSpaces(colObject, 0, dictData);
							objs[pairs - 1] = colObject;
						}

						// handleColorSpaces(-1, valueObj,ref, 0, dictData,debug, -1,null, paddingString);
					}
					else
						if (isObject) {

							PdfObject valueObj = ObjectFactory.createObject(id, ref, pdfObject.getObjectType(), pdfObject.getID());
							valueObj.setID(id);
							readDictionaryFromRefOrDirect(id, valueObj, ref, 0, dictData, -1);
							objs[pairs - 1] = valueObj;
						}

					// lose >> at end
					// while(start<data.length && data[start-1]!='>' && data[start]!='>')
					// start++;

				}

			}
			else { // its 50 0 R

				int number = 0, generation = 0;
				int refStart = start, keyStart2 = start;

				boolean isNull = false;
				if (data[start] == 'n' && data[start + 1] == 'u' && data[start + 2] == 'l' && data[start + 3] == 'l') {
					start = start + 4;

					isNull = true;
				}
				else {

					// number
					while (data[start] != 10 && data[start] != 13 && data[start] != 32 && data[start] != 47 && data[start] != 60 && data[start] != 62) {
						start++;
					}
					number = NumberUtils.parseInt(keyStart2, start, data);

					// generation
					while (data[start] == 10 || data[start] == 13 || data[start] == 32 || data[start] == 47 || data[start] == 60)
						start++;

					keyStart2 = start;
					// move cursor to end of reference
					while (data[start] != 10 && data[start] != 13 && data[start] != 32 && data[start] != 47 && data[start] != 60 && data[start] != 62)
						start++;

					generation = NumberUtils.parseInt(keyStart2, start, data);

					// move cursor to start of R
					while (data[start] == 10 || data[start] == 13 || data[start] == 32 || data[start] == 47 || data[start] == 60)
						start++;

				}

				{
					if (!isNull && data[start] != 82) { // we are expecting R to end ref
						throw new RuntimeException((char) data[start - 1] + " " + (char) data[start] + " " + (char) data[start + 1]
								+ " 3. Unexpected value in file - please send to IDRsolutions for analysis");
					}
					start++; // roll past

					if (debug) System.out.println("Data in object=" + number + ' ' + generation + " R");

					// read the Dictionary data
					if (!isCountOnly) {

						if (isNull) {
							objs[pairs - 1] = null;
						}
						else
							if (PdfDictionary.getKeyType(id, pdfObject.getObjectType()) == PdfDictionary.VALUE_IS_UNREAD_DICTIONARY) {

								String ref = new String(data, refStart, start - refStart);

								PdfObject valueObj = ObjectFactory.createObject(id, ref, pdfObject.getObjectType(), pdfObject.getID());

								valueObj.setStatus(PdfObject.UNDECODED_REF);
								valueObj.setUnresolvedData(StringUtils.toBytes(ref), id);

								objs[pairs - 1] = valueObj;

							}
							else {

								byte[] rawDictData = this.objectReader.readObjectAsByteArray(pdfObject,
										this.objectReader.isCompressed(number, generation), number, generation);

								// allow for data in Linear object not yet loaded
								if (rawDictData == null) {
									pdfObject.setFullyResolved(false);

									if (LogWriter.isOutput()) LogWriter.writeLog("[Linearized] " + pdfObject.getObjectRefAsString()
											+ " not yet available (12)");

									return data.length;
								}

								if (debug) {
									System.out.println("============================================\n");
									for (int aa = 0; aa < rawDictData.length; aa++) {
										System.out.print((char) rawDictData[aa]);

										if (aa > 5 && rawDictData[aa - 5] == 's' && rawDictData[aa - 4] == 't' && rawDictData[aa - 3] == 'r'
												&& rawDictData[aa - 2] == 'e' && rawDictData[aa - 1] == 'a' && rawDictData[aa] == 'm') aa = rawDictData.length;
									}
									System.out.println("\n============================================");
								}
								// cleanup
								// lose obj at start
								int jj = 0;

								while (jj < 3 || (rawDictData[jj - 1] != 106 && rawDictData[jj - 2] != 98 && rawDictData[jj - 3] != 111)) {

									if (rawDictData[jj] == '/' || rawDictData[jj] == '[' || rawDictData[jj] == '<') break;

									jj++;

									if (jj == rawDictData.length) {
										jj = 0;
										break;
									}
								}

								// skip any spaces after
								while (rawDictData[jj] == 10 || rawDictData[jj] == 13 || rawDictData[jj] == 32)
									// || data[j]==47 || data[j]==60)
									jj++;

								int len = rawDictData.length - jj;
								dictData = new byte[len];
								System.arraycopy(rawDictData, jj, dictData, 0, len);
								// pairs already rolled on so needs to be 1 less
								values[pairs - 1] = dictData;

								String ref = number + " " + generation + " R";// pdfObject.getObjectRefAsString();

								if (pdfObject.getObjectType() == PdfDictionary.Font && id == PdfDictionary.Font) {// last condition for CharProcs
									objs[pairs - 1] = null;
									values[pairs - 1] = StringUtils.toBytes(ref);
								}
								else
									if (pdfObject.getObjectType() == PdfDictionary.XObject) {
										// intel Unimplemented pattern type 0 in file
										PdfObject valueObj = ObjectFactory.createObject(id, ref, PdfDictionary.XObject, PdfDictionary.XObject);
										valueObj.setStatus(PdfObject.UNDECODED_REF);
										valueObj.setUnresolvedData(StringUtils.toBytes(ref), id);

										objs[pairs - 1] = valueObj;
									}
									else {

										// @speed - will probably need to change as we add more items
										PdfObject valueObj = ObjectFactory.createObject(id, ref, pdfObject.getObjectType(), pdfObject.getID());
										valueObj.setID(id);
										if (debug) {
											System.out.println(ref + " ABOUT TO READ OBJ for " + valueObj + ' ' + pdfObject);

											System.out.println("-------------------\n");
											for (int aa = 0; aa < dictData.length; aa++) {
												System.out.print((char) dictData[aa]);

												if (aa > 5 && dictData[aa - 5] == 's' && dictData[aa - 4] == 't' && dictData[aa - 3] == 'r'
														&& dictData[aa - 2] == 'e' && dictData[aa - 1] == 'a' && dictData[aa] == 'm') aa = dictData.length;
											}
											System.out.println("\n-------------------");
										}

										if (valueObj.getObjectType() == PdfDictionary.ColorSpace) {
											ColorObjectDecoder colDecoder = new ColorObjectDecoder(this.objectReader);
											colDecoder.handleColorSpaces(valueObj, 0, dictData);
										}
										else readDictionaryFromRefOrDirect(id, valueObj, ref, 0, dictData, -1);

										objs[pairs - 1] = valueObj;

									}
							}
					}
				}
			}
		}

		if (!isCountOnly) pdfObject.setDictionaryPairs(keys, values, objs);

		if (debug) System.out.println("done=============================================");

		if (skipToEnd || !isCountOnly) return start;
		else return pairs;
	}

	void readStreamIntoObject(PdfObject pdfObject, int j, byte[] data) {

		int count = data.length;

		if (debugFastCode) System.out.println(padding + "Looking for stream");

		byte[] stream = null;

		/**
		 * see if JBIG encoded
		 */
		PdfArrayIterator maskFilters = pdfObject.getMixedArray(PdfDictionary.Filter);

		// get type as need different handling
		boolean isJBigEncoded = false;
		int firstMaskValue = PdfDictionary.Unknown;
		if (maskFilters != null && maskFilters.hasMoreTokens()) {

			firstMaskValue = maskFilters.getNextValueAsConstant(true);

			if (firstMaskValue == PdfFilteredReader.JBIG2Decode) isJBigEncoded = true;

			while (maskFilters.hasMoreTokens() && !isJBigEncoded) {
				firstMaskValue = maskFilters.getNextValueAsConstant(true);
				if (firstMaskValue == PdfFilteredReader.JBIG2Decode) isJBigEncoded = true;
			}
		}

		for (int a = j; a < count; a++) {
			if ((data[a] == 115) && (data[a + 1] == 116) && (data[a + 2] == 114) && (data[a + 3] == 101) && (data[a + 4] == 97)
					&& (data[a + 5] == 109)) {

				// ignore these characters and first return
				a = a + 6;

				while (data[a] == 32)
					a++;

				if (data[a] == 13 && data[a + 1] == 10) // allow for double linefeed
				a = a + 2;
				// see /PDFdata/baseline_screens/customers-june2011/Agency discl. Wabash.pdf
				else
					if (data[a] == 10 && data[a + 1] == 10 && data[a + 2] == 10 && data[a + 3] == -1 && firstMaskValue == PdfFilteredReader.DCTDecode) { // allow
																																							// for
																																							// double
																																							// linefeed
																																							// on
																																							// jpeg
						a = a + 3;
					}
					else
						if (data[a] == 10 && data[a + 1] == 10 && data[a + 2] == -1 && firstMaskValue == PdfFilteredReader.DCTDecode) { // allow for
																																		// double
																																		// linefeed on
																																		// jpeg
							a = a + 2;
						}
						else
							if (data[a] == 10 || data[a] == 13) a++;

				int start = a;

				a--; // move pointer back 1 to allow for zero length stream

				/**
				 * if Length set and valid use it
				 */
				int streamLength = 0;
				int setStreamLength = pdfObject.getInt(PdfDictionary.Length);

				if (debugFastCode) System.out.println(padding + "setStreamLength=" + setStreamLength);

				boolean isValid = false;

				if (setStreamLength != -1) {

					streamLength = setStreamLength;

					// System.out.println("1.streamLength="+streamLength);

					a = start + streamLength;

					if (a < count && data[a] == 13 && (a + 1 < count) && data[a + 1] == 10) a = a + 2;

					// check validity
					if (count > (a + 9) && data[a] == 101 && data[a + 1] == 110 && data[a + 2] == 100 && data[a + 3] == 115 && data[a + 4] == 116
							&& data[a + 5] == 114 && data[a + 6] == 101 && data[a + 7] == 97 && data[a + 8] == 109) {

					}
					else {

						int current = a;
						// check forwards
						if (a < count) {
							while (true) {
								a++;
								if (isValid || a == count) break;

								if (data[a] == 101 && data[a + 1] == 110 && data[a + 2] == 100 && data[a + 3] == 115 && data[a + 4] == 116
										&& data[a + 5] == 114 && data[a + 6] == 101 && data[a + 7] == 97 && data[a + 8] == 109) {

									streamLength = a - start;
									isValid = true;
								}
							}
						}

						if (!isValid) {
							a = current;
							if (a > count) a = count;
							// check backwords
							while (true) {
								a--;
								if (isValid || a < 0) break;

								if (data[a] == 101 && data[a + 1] == 110 && data[a + 2] == 100 && data[a + 3] == 115 && data[a + 4] == 116
										&& data[a + 5] == 114 && data[a + 6] == 101 && data[a + 7] == 97 && data[a + 8] == 109) {
									streamLength = a - start;
									isValid = true;
								}
							}
						}

						if (!isValid) a = current;
					}

					// use correct figure if encrypted
					if (this.decryption != null && this.decryption.getBooleanValue(PDFflags.IS_FILE_ENCRYPTED)) streamLength = setStreamLength;

				}
				else {

					/** workout length and check if length set */
					int end;

					while (true) { // find end

						a++;

						if (a == count) break;
						if (data[a] == 101 && data[a + 1] == 110 && data[a + 2] == 100 && data[a + 3] == 115 && data[a + 4] == 116
								&& data[a + 5] == 114 && data[a + 6] == 101 && data[a + 7] == 97 && data[a + 8] == 109) break;

					}

					end = a - 1;

					if ((end > start)) streamLength = end - start + 1;
				}

				// lose trailing 10s or 13s
				if (streamLength > 1 && !(this.decryption != null && this.decryption.getBooleanValue(PDFflags.IS_FILE_ENCRYPTED))) {// && !isValid){
					int ptr = start + streamLength - 1;

					if (ptr < data.length && ptr > 0
							&& (data[ptr] == 10 || (data[ptr] == 13 && ((pdfObject != null && isJBigEncoded) || (ptr > 0 && data[ptr - 1] == 10))))) {
						streamLength--;
						ptr--;
					}
				}

				/**
				 * read stream into object from memory
				 */
				if (start + streamLength > count) streamLength = count - start;

				// @speed - switch off and investigate
				if (streamLength < 0) return;

				if (streamLength < 0) throw new RuntimeException("Negative stream length " + streamLength + " start=" + start + " count=" + count);
				stream = new byte[streamLength];
				System.arraycopy(data, start, stream, 0, streamLength);

				a = count;
			}

		}

		if (debugFastCode && stream != null) System.out.println(padding + "stream read saved into " + pdfObject);

		if (pdfObject != null) {

			pdfObject.setStream(stream);

			// and decompress now forsome objects
			if (pdfObject.decompressStreamWhenRead()) this.objectReader.readStream(pdfObject, true, true, false,
					pdfObject.getObjectType() == PdfDictionary.Metadata, pdfObject.isCompressedStream(), null);

		}
	}

	private int setNameStringValue(PdfObject pdfObject, int i, byte[] raw, boolean isMap) {

		byte[] stringBytes;

		int keyStart;

		// move cursor to end of last command if needed
		while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != '(')
			i++;

		// move cursor to start of text
		while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32)
			i++;

		// work out if direct (ie /String or read ref 27 0 R
		int j2 = i;
		byte[] arrayData = raw;

		boolean isIndirect = raw[i] != 47 && raw[i] != 40 && raw[i] != 60; // Some /NAME values start (

		boolean startsWithBrace = raw[i] == 40;

		// delete
		// @speed - lose this code once Filters done properly
		/**
		 * just check its not /Filter [/FlateDecode ] or [] or [ /ASCII85Decode /FlateDecode ] by checking next valid char not /
		 */
		boolean isInsideArray = false;
		if (isIndirect) {
			int aa = i + 1;
			while (aa < raw.length && (raw[aa] == 10 || raw[aa] == 13 || raw[aa] == 32))
				aa++;

			if (raw[aa] == 47 || raw[aa] == ']') {
				isIndirect = false;
				i = aa + 1;
				isInsideArray = true;
			}
		}

		if (isIndirect) { // its in another object so we need to fetch

			keyStart = i;

			// move cursor to end of ref
			while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62) {
				i++;
			}

			// actual value or first part of ref
			int ref = NumberUtils.parseInt(keyStart, i, raw);

			// move cursor to start of generation
			while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
				i++;

			// get generation number
			keyStart = i;
			// move cursor to end of reference
			while (raw[i] != 10 && raw[i] != 13 && raw[i] != 32 && raw[i] != 47 && raw[i] != 60 && raw[i] != 62)
				i++;

			int generation = NumberUtils.parseInt(keyStart, i, raw);

			// move cursor to start of R
			while (raw[i] == 10 || raw[i] == 13 || raw[i] == 32 || raw[i] == 47 || raw[i] == 60)
				i++;

			if (raw[i] != 82) { // we are expecting R to end ref
				throw new RuntimeException(padding + "2. Unexpected value in file - please send to IDRsolutions for analysis");
			}

			// read the Dictionary data
			arrayData = this.objectReader.readObjectAsByteArray(pdfObject, this.objectReader.isCompressed(ref, generation), ref, generation);

			// allow for data in Linear object not yet loaded
			if (arrayData == null) {
				pdfObject.setFullyResolved(false);

				if (debugFastCode) System.out.println(padding + "Data not yet loaded");

				if (LogWriter.isOutput()) LogWriter.writeLog("[Linearized] " + pdfObject.getObjectRefAsString() + " not yet available (13)");

				return raw.length;
			}

			// lose obj at start and roll onto /
			if (arrayData[0] == 47) {
				j2 = 0;
			}
			else {
				j2 = 3;

				while (arrayData[j2] != 47) {
					j2++;
				}
			}
		}

		// lose /
		j2++;

		// allow for no value with /Intent//Filter
		if (arrayData[j2] == 47) return j2 - 1;

		int end = j2 + 1;

		if (isInsideArray) { // values inside []

			// move cursor to start of text
			while (arrayData[j2] == 10 || arrayData[j2] == 13 || arrayData[j2] == 32 || arrayData[j2] == 47)
				j2++;

			int slashes = 0;

			// count chars
			byte lastChar = 0;
			while (true) {
				if (arrayData[end] == ']') break;

				if (arrayData[end] == 47 && (lastChar == 32 || lastChar == 10 || lastChar == 13)) // count the / if gap before
				slashes++;

				lastChar = arrayData[end];
				end++;

				if (end == arrayData.length) break;
			}

			// set value and ensure space gap
			int charCount = end - slashes, ptr = 0;
			stringBytes = new byte[charCount - j2];

			byte nextChar, previous = 0;
			for (int ii = j2; ii < charCount; ii++) {
				nextChar = arrayData[ii];
				if (nextChar == 47) {
					if (previous != 32 && previous != 10 && previous != 13) {
						stringBytes[ptr] = 32;
						ptr++;
					}
				}
				else {
					stringBytes[ptr] = nextChar;
					ptr++;
				}

				previous = nextChar;
			}
		}
		else { // its in data stream directly or (string)

			// count chars
			while (true) {

				if (startsWithBrace) {
					if (arrayData[end] == ')') break;
				}
				else
					if (arrayData[end] == 32 || arrayData[end] == 10 || arrayData[end] == 13 || arrayData[end] == 47 || arrayData[end] == 62) break;

				end++;

				if (end == arrayData.length) break;
			}

			// set value
			int charCount = end - j2;
			stringBytes = new byte[charCount];
			System.arraycopy(arrayData, j2, stringBytes, 0, charCount);

		}

		/**
		 * finally set the value
		 */
		if (isMap) {
			pdfObject.setName(this.PDFkey, StringUtils.getTextString(stringBytes, false));
		}
		else pdfObject.setName(this.PDFkeyInt, stringBytes);

		if (debugFastCode) System.out.println(padding + "String set as =" + new String(stringBytes) + "< written to " + pdfObject);

		// put cursor in correct place (already there if ref)
		if (!isIndirect) i = end - 1;

		return i;
	}

	/**
	 * used by linearization to check object fully fully available and return false if not
	 * 
	 * @param pdfObject
	 */
	public synchronized boolean resolveFully(PdfObject pdfObject) {

		boolean fullyResolved = pdfObject != null;

		if (fullyResolved) {

			byte[] raw;
			if (pdfObject.getStatus() == PdfObject.DECODED) raw = StringUtils.toBytes(pdfObject.getObjectRefAsString());
			else raw = pdfObject.getUnresolvedData();

			// flag now done and flush raw data
			pdfObject.setStatus(PdfObject.DECODED);

			// allow for empty object
			if (raw[0] != 'e' && raw[1] != 'n' && raw[2] != 'd' && raw[3] != 'o' && raw[4] != 'b') {

				int j = 0;

				// allow for [ref] at top level (may be followed by gap
				while (raw[j] == 91 || raw[j] == 32 || raw[j] == 13 || raw[j] == 10)
					j++;

				// get object ref
				int keyStart = j;

				// move cursor to end of reference
				while (raw[j] != 10 && raw[j] != 13 && raw[j] != 32 && raw[j] != 47 && raw[j] != 60 && raw[j] != 62)
					j++;

				int ref = NumberUtils.parseInt(keyStart, j, raw);

				// move cursor to start of generation or next value
				while (raw[j] == 10 || raw[j] == 13 || raw[j] == 32)
					// || data[j]==47 || data[j]==60)
					j++;

				/**
				 * get generation number
				 */
				keyStart = j;

				// move cursor to end of reference
				while (raw[j] != 10 && raw[j] != 13 && raw[j] != 32 && raw[j] != 47 && raw[j] != 60 && raw[j] != 62)
					j++;

				int generation = NumberUtils.parseInt(keyStart, j, raw);

				if (raw[raw.length - 1] == 'R') // recursively validate all child objects
				fullyResolved = resolveFullyChildren(pdfObject, fullyResolved, raw, ref, generation);

				if (fullyResolved) {
					pdfObject.ignoreRecursion(false);
					ObjectDecoder objDecoder = new ObjectDecoder(this.objectReader);
					objDecoder.readDictionaryAsObject(pdfObject, j, raw);

					// if(!pdfObject.isFullyResolved())
					// fullyResolved=false;
				}
			}
		}

		return fullyResolved;
	}

	private boolean resolveFullyChildren(PdfObject pdfObject, boolean fullyResolved, byte[] raw, int ref, int generation) {

		pdfObject.setRef(new String(raw));
		pdfObject.isDataExternal(true);

		byte[] pageData = this.objectReader.readObjectAsByteArray(pdfObject, this.objectReader.isCompressed(ref, generation), ref, generation);

		// allow for data in Linear object not yet loaded
		if (pageData == null) {
			pdfObject.setFullyResolved(false);
			fullyResolved = false;
		}
		else {
			pdfObject.setStatus(PdfObject.UNDECODED_DIRECT);
			pdfObject.setUnresolvedData(pageData, PdfDictionary.Linearized);
			pdfObject.isDataExternal(true);

			if (!resolveFully(pdfObject)) pdfObject.setFullyResolved(false);
		}

		return fullyResolved;
	}

	/**
	 * read object setup to contain only ref to data
	 * 
	 * @param pdfObject
	 */
	public void checkResolved(PdfObject pdfObject) {

		if (pdfObject != null && pdfObject.getStatus() != PdfObject.DECODED) {

			byte[] raw = pdfObject.getUnresolvedData();

			// flag now done and flush raw data
			pdfObject.setStatus(PdfObject.DECODED);

			// allow for empty object
			if (raw[0] == 'e' && raw[1] == 'n' && raw[2] == 'd' && raw[3] == 'o' && raw[4] == 'b') {
				// empty object
			}
			else
				if (raw[0] == 'n' && raw[1] == 'u' && raw[2] == 'l' && raw[3] == 'l') {
					// null object
				}
				else { // we need to ref from ref elsewhere which may be indirect [ref], hence loop

					String objectRef = pdfObject.getObjectRefAsString();

					// allow for Color where starts [/ICCBased 2 0 R so we get the ref if present
					if (raw[0] == '[') {

						// scan along to find number
						int ptr = 0, len = raw.length;
						for (int jj = 0; jj < len; jj++) {

							if (raw[jj] >= '0' && raw[jj] <= '9') {
								ptr = jj;
								jj = len;
							}
						}

						// check first non-number is R
						int end = ptr;
						while ((raw[end] >= '0' && raw[end] <= '9') || raw[end] == ' ' || raw[end] == 10 || raw[end] == 13 || raw[end] == 9)
							end++;
						// and store if it is a ref
						if (raw[end] == 'R') pdfObject.setRef(new String(raw, ptr, len - ptr));

					}
					else
						if (raw[raw.length - 1] == 'R') pdfObject.setRef(new String(raw));

					ObjectDecoder objDecoder = new ObjectDecoder(this.objectReader);
					objDecoder.readDictionaryFromRefOrDirect(-1, pdfObject, objectRef, 0, raw, -1);

				}
		}
	}

	/**
	 * set end if not end of data stream
	 */
	public void setEndPt(int dataPointer) {
		this.endPt = dataPointer;
	}
}
