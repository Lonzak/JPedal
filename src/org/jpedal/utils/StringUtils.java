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
 * StringUtils.java
 * ---------------
 */
package org.jpedal.utils;

import java.io.UnsupportedEncodingException;

import org.jpedal.PdfDecoder;
import org.jpedal.fonts.StandardFonts;
import org.jpedal.io.TextTokens;
import org.jpedal.parser.DecoderOptions;

public class StringUtils {

	private static final int aInt = 97;
	private static final int zeroInt = 48;
	private static final int nineInt = 57;
	private static final int openSquareBracketInt = 91;
	private static final int closeSquareBracketInt = 93;
	private static final int openCurlyBracket = 40;
	private static final int closeCurlyBracket = 41;
	private static final int backSlashInt = 92;
	private static final int forwardSlashInt = 47;
	private static final int hashInt = 35;
	private static final int divideInt = 247;
	private static final int fullStopInt = 46;
	private static final int spaceInt = 32;
	private static final int percentInt = 37;
	private static final int minusInt = 45;
	private static final int underScoreInt = 95;
	private static final int backSlachInt = 92;
	private static final int nInt = 110;
	private static final int newLineInt = 10;
	private static final int plusInt = 43;
	private static final int pInt = 112;
	private static final int colonInt = 58;
	private static final int equalsInt = 61;
	private static final int cInt = 99;
	private static final int qInt = 113;

	private static String enc;

	static {
		enc = System.getProperty("file.encoding");

		if (enc.equals("UTF-8") || enc.equals("MacRoman") || enc.equals("Cp1252")) {
			// fine carry on
		}
		else
			if (DecoderOptions.isRunningOnMac) enc = "MacRoman";
			else
				if (DecoderOptions.isRunningOnWindows) enc = "Cp1252";
				else enc = "UTF-8";
	}

	/**
	 * quick code to make text lower case
	 */
	public static String toLowerCase(String str) {

		int len = str.length();
		char c;
		char[] chars = str.toCharArray();

		// strip out any odd codes
		boolean isChanged = false;
		for (int jj = 0; jj < len; jj++) {
			c = chars[jj];

			// ensure lower case and flip if not
			if (c > 64 && c < 91) {
				c = (char) (c + 32);
				chars[jj] = c;
				isChanged = true;
			}
		}

		if (isChanged) return String.copyValueOf(chars, 0, len);
		else return str;
	}

	public static String toUpperCase(String str) {

		int len = str.length();
		char c;
		char[] chars = str.toCharArray();

		// strip out any odd codes
		boolean isChanged = false;
		for (int jj = 0; jj < len; jj++) {
			c = chars[jj];

			// ensure UPPER case and flip if not
			if (c > 96 && c < 123) {
				c = (char) (c - 32);
				chars[jj] = c;
				isChanged = true;
			}
		}

		if (isChanged) return String.copyValueOf(chars, 0, len);
		else return str;
	}

	static final public String handleEscapeChars(String value) {
		// deal with escape characters
		int escapeChar = value.indexOf(backSlachInt);

		while (escapeChar != -1) {
			char c = value.charAt(escapeChar + 1);
			if (c == nInt) {
				c = newLineInt;
			}
			else {}

			value = value.substring(0, escapeChar) + c + value.substring(escapeChar + 2, value.length());

			escapeChar = value.indexOf(backSlachInt);
		}
		return value;
	}

	/**
	 * turn any hex values (ie #e4) into chars
	 * 
	 * @param value
	 */
	static final public String convertHexChars(String value) {

		// avoid null
		if (value == null) return value;

		// find char
		int escapeChar = value.indexOf(hashInt);

		if (escapeChar == -1) return value;

		// process
		StringBuilder newString = new StringBuilder();
		int length = value.length();
		// newString.setLength(length);

		char c;

		for (int ii = 0; ii < length; ii++) {
			c = value.charAt(ii);

			if (c == hashInt) {
				ii++;
				int end = ii + 2;
				if (end > length) end = length;
				String key = value.substring(ii, end);

				c = (char) Integer.parseInt(key, 16);

				ii++;

				if (c != spaceInt) newString.append(c);
			}
			else newString.append(c);

		}

		return newString.toString();
	}

	/**
	 * check to see if the string contains anything other than '-' '0-9' '.' if so then its not a number.
	 */
	public static boolean isNumber(String textString) {
		byte[] data = StringUtils.toBytes(textString);
		int strLength = data.length;
		boolean isNumber = true;

		// assume true and disprove
		for (int j = 0; j < strLength; j++) {
			if ((data[j] >= zeroInt && data[j] <= nineInt) || data[j] == fullStopInt || (j == 0 && data[j] == minusInt)) { // assume and disprove
			}
			else {
				isNumber = false;
				// exit loop
				j = strLength;
			}
		}

		return isNumber;
	}

	/** removes the specified index from the array and reduces the array by 1 in length */
	public static String[] remove(String[] fields, int i) {
		if (i < 0 || i > fields.length) return fields;

		String[] retArray = new String[fields.length - 1];
		int r = 0;
		for (int f = 0; f < fields.length; f++) {
			if (f == i) continue;
			retArray[r++] = fields[f];
		}
		return retArray;
	}

	/**
	 * public static void main(String[] args){ //add characters here to get int UNIVERSAL equivalents. char[] chrs = new char[]{'(',')'}; for (int i =
	 * 0; i < chrs.length; i++) { System.out.println(chrs[i]+" ="+((int)chrs[i])); } }/
	 **/

	/**
	 * replaces all spaces ' ' with underscores '_' to allow the whole name to be used in HTML
	 * 
	 */
	public static String makeHTMLNameSafe(String name) {

		if (name == null || name.length() == 0) return name;

		char[] chrs = name.toCharArray();

		// replace any dodgy chars
		if (name.indexOf(percentInt) != -1 || name.indexOf(spaceInt) != -1 || name.indexOf(fullStopInt) != -1 || name.indexOf(plusInt) != -1
				|| name.indexOf(colonInt) != -1 || name.indexOf(equalsInt) != -1 || name.indexOf(forwardSlashInt) != -1
				|| name.indexOf(backSlashInt) != -1) {
			// NOTE: if you add any more please check with main method above for int values and DONT use char
			// strings as they are not cross platform. search for 'UNIVERSAL equivalents' to find main method.
			for (int i = 0; i < chrs.length; i++) {
				switch (chrs[i]) {

					case spaceInt:
						chrs[i] = underScoreInt;
						break;

					case fullStopInt:
						chrs[i] = minusInt;
						break;

					// replace & with safe char as images break if in path ?? ANY IDEA WHAT THIS LINE IS??
					case percentInt:
						chrs[i] = underScoreInt;
						break;

					case plusInt:
						chrs[i] = pInt;
						break;

					case colonInt:
						chrs[i] = cInt;
						break;

					case equalsInt:
						chrs[i] = qInt;
						break;

					case forwardSlashInt:
						chrs[i] = underScoreInt;
						break;

					case backSlashInt:
						chrs[i] = underScoreInt;
						break;
				}
			}
		}

		char[] testchrs = new char[] { openSquareBracketInt, closeSquareBracketInt, hashInt, divideInt, openCurlyBracket, closeCurlyBracket };
		int count = 0;
		for (char chr1 : chrs) {
			for (char testchr : testchrs) {
				if (chr1 == testchr) count++;
			}
		}

		if (count > 0) {
			int c = 0;
			char[] tmp = new char[chrs.length - count];
			MAINLOOP: for (char chr : chrs) {
				for (char testchr : testchrs) {
					if (chr == testchr) continue MAINLOOP;
				}
				tmp[c++] = chr;
			}
			chrs = tmp;

		}

		if (chrs[0] >= zeroInt && chrs[0] <= nineInt) {
			char[] tmp = new char[chrs.length + 1];
			System.arraycopy(chrs, 0, tmp, 1, chrs.length);
			tmp[0] = aInt;
			chrs = tmp;
		}

		name = new String(chrs);

		return name;
	}

	/**
	 * read a text String held in fieldName in string
	 */
	public static String getTextString(byte[] rawText, boolean keepReturns) {

		String returnText = "";

		// make sure encoding loaded
		StandardFonts.checkLoaded(StandardFonts.PDF);

		char[] chars = null;
		if (rawText != null) chars = new char[rawText.length * 2];
		int ii = 0;
		char nextChar;

		TextTokens rawChars = new TextTokens(rawText);

		// test to see if unicode
		if (rawChars.isUnicode()) {
			// its unicode
			while (rawChars.hasMoreTokens()) {
				nextChar = rawChars.nextUnicodeToken();
				if (nextChar == 9) {
					chars[ii] = 32;
					ii++;
				}
				else
					if (nextChar > 31 || (keepReturns && (nextChar == 10 || nextChar == 13))) {
						chars[ii] = nextChar;
						ii++;
					}
			}

		}
		else {
			// pdfDoc encoding

			while (rawChars.hasMoreTokens()) {
				nextChar = rawChars.nextToken();

				String c = null;
				if (nextChar == 9) {
					c = " ";
				}
				else
					if (keepReturns && (nextChar == 10 || nextChar == 13)) {
						c = String.valueOf(nextChar);
					}
					else
						if (nextChar > 31 && nextChar < 253) {
							c = StandardFonts.getEncodedChar(StandardFonts.PDF, nextChar);
						}

				if (c != null) {
					int len = c.length();

					// resize if needed
					if (ii + len >= chars.length) {
						char[] tmp = new char[len + ii + 10];
						System.arraycopy(chars, 0, tmp, 0, chars.length);
						chars = tmp;
					}

					// add values
					for (int i = 0; i < len; i++) {
						chars[ii] = c.charAt(i);
						ii++;
					}
				}
			}
		}

		if (chars != null) {
			returnText = String.copyValueOf(chars, 0, ii);
			// fix for certain PDFS using the HEX digits of a UTF-16 encoded String to store the string
			returnText = decodeUTF16String(returnText);
		}

		return returnText;
	}

	/**
	 * Decodes a String that is in the UTF16 format<br />
	 * <b>Example Input:</b> FEFF0041007500730074007200690061<br />
	 * <b>Example Output:</b> Austria
	 * 
	 * @param source
	 *            The UTF16 String
	 */
	public static String decodeUTF16String(String source) {
		String copy = source.toUpperCase();
		if (copy.length() >= 4 && copy.length() % 2 == 0 && copy.startsWith("FEFF") && copy.matches("[0-9A-F]+")) {
			copy = copy.substring(4);
			byte content[] = new byte[copy.length() / 2];
			for (int i = 0; i < copy.length(); i += 2) {
				content[i / 2] = (byte) Integer.parseInt(copy.substring(i, i + 2), 16);
			}

			StringBuilder newString = new StringBuilder(content.length / 2);
			for (int i = 0; i < content.length; i++) {
				if (content[i] != 0) {
					newString.append((char) content[i]);
				}
			}

			return newString.toString();
		}
		else {
			return source;
		}
	}

	public static String replaceAllManual(String string, int find, String replace) {
		int index = string.indexOf(find);
		while (index != -1) {
			string = string.substring(0, index) + replace + string.substring(index + 1);
			index = string.indexOf(find);
		}
		return string;
	}

	public static String correctSpecialChars(String string) {
		for (int i = 0; i < string.length(); i++) {
			switch (string.charAt(i)) {
				case 225:
					string = replaceAllManual(string, 225, "&aacute;");
					break;
				case 224:
					string = replaceAllManual(string, 224, "&agrave;");
					break;
				case 226:
					string = replaceAllManual(string, 226, "&acirc;");
					break;
				case 229:
					string = replaceAllManual(string, 229, "&aring;");
					break;
				case 227:
					string = replaceAllManual(string, 227, "&atilde;");
					break;
				case 228:
					string = replaceAllManual(string, 228, "&auml;");
					break;
				case 230:
					string = replaceAllManual(string, 230, "&aelig;");
					break;
				case 231:
					string = replaceAllManual(string, 231, "&ccedil;");
					break;
				case 233:
					string = replaceAllManual(string, 233, "&eacute;");
					break;
				case 232:
					string = replaceAllManual(string, 232, "&egrave;");
					break;
				case 234:
					string = replaceAllManual(string, 234, "&ecirc;");
					break;
				case 235:
					string = replaceAllManual(string, 235, "&euml;");
					break;
				case 237:
					string = replaceAllManual(string, 237, "&iacute;");
					break;
				case 236:
					string = replaceAllManual(string, 236, "&igrave;");
					break;
				case 238:
					string = replaceAllManual(string, 238, "&icirc;");
					break;
				case 239:
					string = replaceAllManual(string, 239, "&iuml;");
					break;
				case 241:
					string = replaceAllManual(string, 241, "&ntilde;");
					break;
				case 243:
					string = replaceAllManual(string, 243, "&oacute;");
					break;
				case 242:
					string = replaceAllManual(string, 242, "&ograve;");
					break;
				case 244:
					string = replaceAllManual(string, 244, "&ocirc;");
					break;
				case 248:
					string = replaceAllManual(string, 248, "&oslash;");
					break;
				case 245:
					string = replaceAllManual(string, 245, "&otilde;");
					break;
				case 246:
					string = replaceAllManual(string, 246, "&ouml;");
					break;
				case 223:
					string = replaceAllManual(string, 223, "&szlig;");
					break;
				case 250:
					string = replaceAllManual(string, 250, "&uacute;");
					break;
				case 249:
					string = replaceAllManual(string, 249, "&ugrave;");
					break;
				case 251:
					string = replaceAllManual(string, 251, "&ucirc;");
					break;
				case 252:
					string = replaceAllManual(string, 252, "&uuml;");
					break;
				case 255:
					string = replaceAllManual(string, 255, "&yuml;");
					break;
				case 8217:
					string = replaceAllManual(string, 8217, "&#39;");
					break;
			// to find other codes check out http://www.interfacebus.com/html_escape_codes.html
			}
		}

		return string;
	}

	public static byte[] toBytes(String value) {

		byte[] data = null;

		try {
			data = value.getBytes(enc);

		}
		catch (UnsupportedEncodingException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
		return data;
	}

	/**
	 * Replaces illegal characters that aren't allowed in code
	 * 
	 * @param S
	 *            String to have characters replaced in
	 * @return A safe String that can be used as a Java or Javascript variable or function
	 */
	public static String makeMethodSafe(String S) {
		String name = makeHTMLNameSafe(S);
		name = name.replace("-", "_");
		return name;
	}

	/**
	 * Replaces all illegal characters as defined by ses the standard UNICODE Consortium character repertoire. This means it strips out characters
	 * between: 0 to 31 inclusive and 127 to 159 inclusive.
	 * 
	 * @param S
	 */
	public static String stripIllegalCharacters(String S) {
		String newString = "";
		for (int i = 0; i < S.length(); i++) {
			char ch = S.charAt(i);
			if ((ch < 32 && ch >= 0) || (ch > 126 && ch < 160)) {
				continue;
			}
			newString += ch;
		}
		return newString;
	}
}
