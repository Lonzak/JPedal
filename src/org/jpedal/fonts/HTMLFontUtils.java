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
 * HTMLFontUtils.java
 * ---------------
 */
package org.jpedal.fonts;

import java.io.IOException;
import java.util.HashMap;

import org.jpedal.fonts.tt.conversion.PS2OTFFontWriter;
import org.jpedal.fonts.tt.conversion.TTFontWriter;

public class HTMLFontUtils {

	public static byte[] convertTTForHTML(PdfFont pdfFont, String fontName, byte[] rawFontData) throws IOException {

		// create new version which works on IPad
		return new TTFontWriter(rawFontData).writeFontToStream(); // use 1.0
	}

	public static byte[] convertPSForHTML(PdfFont pdfFont, String fontName, byte[] rawFontData, String fileType, HashMap<String, Integer> widths)
			throws Exception {
		return new PS2OTFFontWriter(pdfFont, rawFontData, fileType, widths).writeFontToStream(); // use OTTO as start
	}

	public static byte[] convertPSForHTMLOTF(PdfFont pdfFont, String fontName, byte[] rawFontData, String fileType, HashMap<String, Integer> widths)
			throws Exception {
		return new PS2OTFFontWriter(pdfFont, rawFontData, fileType, widths).writeFontToStream();
	}

	public static byte[] convertPSForHTMLWOFF(PdfFont pdfFont, String fontName, byte[] rawFontData, String fileType, HashMap<String, Integer> widths)
			throws Exception {
		return new PS2OTFFontWriter(pdfFont, rawFontData, fileType, widths).writeFontToWoffStream();
	}

}
