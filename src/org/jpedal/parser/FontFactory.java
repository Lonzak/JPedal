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
 * FontFactory.java
 * ---------------
 */
package org.jpedal.parser;

import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.StandardFonts;
import org.jpedal.io.PdfObjectReader;

public class FontFactory {

	public static PdfFont createFont(int fontType, PdfObjectReader currentPdfFile, String subFont, boolean isPrinting) {

		switch (fontType) {
			case StandardFonts.TYPE1:
				return new org.jpedal.fonts.Type1C(currentPdfFile, subFont);

			case StandardFonts.TRUETYPE:
				return new org.jpedal.fonts.TrueType(currentPdfFile, subFont);

			case StandardFonts.TYPE3:
				return new org.jpedal.fonts.Type3(currentPdfFile, isPrinting);

			case StandardFonts.CIDTYPE0:
				return new org.jpedal.fonts.CIDFontType0(currentPdfFile, subFont);

			case StandardFonts.CIDTYPE2:
				return new org.jpedal.fonts.CIDFontType2(currentPdfFile, subFont);

			default:

				// LogWriter.writeLog("Font type " + subtype + " not supported");
				return new PdfFont(currentPdfFile);
		}
	}
}
