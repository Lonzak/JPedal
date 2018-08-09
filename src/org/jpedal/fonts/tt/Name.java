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
 * Name.java
 * ---------------
 */
package org.jpedal.fonts.tt;

import java.util.HashMap;
import java.util.Map;

/**
 * font strings partially implemented - full spec is at http://www.microsoft.com/OpenType/OTSpec/name.htm
 */
public class Name extends Table {

	private static final long serialVersionUID = -6534023316796643941L;

	/** max number of glyphs */
	private int encoding = 0;

	private Map strings = new HashMap();

	public static final Integer COPYRIGHT_NOTICE = 0;
	public static final Integer FONT_FAMILY_NAME = 1;
	public static final Integer FONT_SUBFAMILY_NAME = 2;
	public static final Integer UNIQUE_FONT_IDENTIFIER = 3;
	public static final Integer FULL_FONT_NAME = 4;
	public static final Integer VERSION_STRING = 5;
	public static final Integer POSTSCRIPT_NAME = 6;
	public static final Integer TRADEMARK = 7;
	public static final Integer MANUFACTURER = 8;
	public static final Integer DESIGNER = 9;
	public static final Integer DESCRIPTION = 10;
	public static final Integer VENDOR_URL = 11;
	public static final Integer DESIGNER_URL = 12;
	public static final Integer LICENSE = 13;
	public static final Integer LICENSE_URL = 14;
	public static final Integer PREFERRED_FAMILY = 16;
	public static final Integer PREFERRED_SUBFAMILY = 17;
	public static final Integer COMPATIBLE = 18;
	public static final Integer SAMPLE_TEXT = 19;

	/** list of Integer values in same order as keys */
	public static final String[] stringNames = { "COPYRIGHT_NOTICE", "FONT_FAMILY_NAME", "FONT_SUBFAMILY_NAME", "UNIQUE_FONT_IDENTIFIER",
			"FULL_FONT_NAME", "VERSION_STRING", "POSTSCRIPT_NAME", "TRADEMARK", "MANUFACTURER", "DESIGNER", "DESCRIPTION", "VENDOR_URL",
			"DESIGNER_URL", "LICENSE", "LICENSE_URL", "PREFERRED_FAMILY", "PREFERRED_SUBFAMILY", "COMPATIBLE", "SAMPLE_TEXT" };

	public Name(FontFile2 currentFontFile) {

		// LogWriter.writeMethod("{readMapxTable}", 0);

		// move to start and check exists
		int startPointer = currentFontFile.selectTable(FontFile2.NAME);

		// read 'head' table
		if (startPointer != 0) {

			// read global details
			currentFontFile.getNextUint16();// format
			int count = currentFontFile.getNextUint16();
			int offset = currentFontFile.getNextUint16();

			/**
			 * read strings
			 */
			for (int i = 0; i < count; i++) {

				// get table values
				int platformID = currentFontFile.getNextUint16();
				int platformSpecificID = currentFontFile.getNextUint16();
				int langID = currentFontFile.getNextUint16();
				int nameID = currentFontFile.getNextUint16();
				int length = currentFontFile.getNextUint16();
				int offset2 = currentFontFile.getNextUint16();

				// only these 2 variations at present
				if ((platformID == 1 && platformSpecificID == 0 && langID == 0) || (platformID == 3 && platformSpecificID == 0 && langID == 1033)
						|| (platformID == 3 && platformSpecificID == 1 && langID == 1033)) {

					// read actual string for location, altering/restoring pointers
					int oldP = currentFontFile.getPointer();

					currentFontFile.setPointer(startPointer + offset + offset2);

					int nextChar;

					// allow for 2 bytes in char
					if (platformID == 0 || platformID == 3) length = length / 2;

					StringBuilder s = new StringBuilder();
					s.setLength(length);

					for (int ii = 0; ii < length; ii++) {
						if (platformID == 0 || platformID == 3) nextChar = currentFontFile.getNextUint16();
						else nextChar = currentFontFile.getNextUint8();

						s.setCharAt(ii, (char) nextChar);
					}

					String str = s.toString();

					if (str != null) this.strings.put(nameID, str);

					currentFontFile.setPointer(oldP);
				}
			}
		}
	}

	public Name() {}

	/**
	 * get Map of Strings - key is Integer key and String value is in
	 */
	public Map getStrings() {
		return this.strings;
	}

	public int getEncoding() {
		return this.encoding;
	}

	/**
	 * return a String value in font - keys also defined as Integers in NAME class
	 * 
	 * not all encodings or ID 20 handled at present
	 */
	public String getString(Integer id) {
		return (String) this.strings.get(id);
	}
}
