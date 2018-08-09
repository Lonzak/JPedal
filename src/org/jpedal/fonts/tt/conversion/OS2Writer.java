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
 * OS2Writer.java
 * ---------------
 */
package org.jpedal.fonts.tt.conversion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.StandardFonts;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.tt.FontFile2;
import org.jpedal.fonts.tt.Hhea;
import org.jpedal.fonts.tt.OS2;
import org.jpedal.fonts.tt.TTGlyphs;

public class OS2Writer extends OS2 implements FontTableWriter {

	private static final long serialVersionUID = -9180063907293716888L;

	int glyphCount = 0;

	int xAvgCharWidth = 0, minCharCode, maxCharCode;
	float[] bounds;
	double scaling = 1;
	PdfFont originalFont;
	PdfJavaGlyphs glyphs;

	/**
	 * used to turn Ps into OTF
	 */
	public OS2Writer(PdfFont originalFont, PdfJavaGlyphs glyphs, int xAvgCharWidth, int minCharCode, int maxCharCode, float[] bounds,
			double emSquareSize) {

		this.originalFont = originalFont;
		this.glyphs = glyphs;
		this.glyphCount = glyphs.getGlyphCount();

		this.minCharCode = minCharCode;
		this.maxCharCode = maxCharCode;

		this.bounds = bounds;

		this.xAvgCharWidth = xAvgCharWidth;

		// Set scaling for incorrect em square size
		if (emSquareSize != 1000) {
			this.scaling = 1d / (emSquareSize / 1000);
		}
	}

	@Override
	public byte[] writeTable() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		if (this.originalFont.getFontType() == StandardFonts.TRUETYPE) {
			TTGlyphs ttGlyphs = (TTGlyphs) this.glyphs;
			Hhea hhea = (Hhea) this.glyphs.getTable(FontFile2.HHEA);
			int ascender = hhea.getIntValue(Hhea.ASCENDER);
			int descender = hhea.getIntValue(Hhea.DESCENDER);

			bos.write(FontWriter.setNextInt16(3)); // USHORT version 0x0004
			bos.write(FontWriter.setNextInt16(this.xAvgCharWidth)); // SHORT xAvgCharWidth
			bos.write(FontWriter.setNextInt16(400)); // USHORT usWeightClass (400 is normal)
			bos.write(FontWriter.setNextInt16(5)); // USHORT usWidthClass
			bos.write(FontWriter.setNextInt16(0)); // USHORT fsType
			// ascender is multiplied by 0.3 to support average sub and superscirpt
			bos.write(FontWriter.setNextInt16((int) (ascender * 0.3))); // SHORT ySubscriptXSize
			bos.write(FontWriter.setNextInt16((int) (ascender * 0.3))); // SHORT ySubscriptYSize
			bos.write(FontWriter.setNextInt16(0)); // SHORT ySubscriptXOffset
			bos.write(FontWriter.setNextInt16(0)); // SHORT ySubscriptYOffset
			// ascender is multiplied by 0.3 to support average sub and superscirpt
			bos.write(FontWriter.setNextInt16((int) (ascender * 0.3))); // SHORT ySuperscriptXSize
			bos.write(FontWriter.setNextInt16((int) (ascender * 0.3))); // SHORT ySuperscriptYSize
			bos.write(FontWriter.setNextInt16(0)); // SHORT ySuperscriptXOffset
			bos.write(FontWriter.setNextInt16(0)); // SHORT ySuperscriptYOffset
			// ascender is multiplied by 0.3 to support average strikeoutsize
			bos.write(FontWriter.setNextInt16((int) (ascender * 0.3))); // SHORT yStrikeoutSize
			bos.write(FontWriter.setNextInt16((int) (ascender * 0.3))); // SHORT yStrikeoutPosition
			bos.write(FontWriter.setNextInt16(0)); // SHORT sFamilyClass
			bos.write(new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }); // BYTE panose[10] (see http://www.panose.com/hardware/pan2.asp)
			bos.write(FontWriter.setNextUint32(0)); // ULONG ulUnicodeRange1 Bits 0-31
			bos.write(FontWriter.setNextUint32(0)); // ULONG ulUnicodeRange2 Bits 32-63
			bos.write(FontWriter.setNextUint32(0)); // ULONG ulUnicodeRange3 Bits 64-95
			bos.write(FontWriter.setNextUint32(0)); // ULONG ulUnicodeRange4 Bits 96-127
			bos.write(new byte[4]); // CHAR achVendID[4] //see http://www.microsoft.com/typography/links/links.aspx?type=vendor&part=1
			bos.write(FontWriter.setNextInt16(64)); // USHORT fsSelection (currently hard-coded to regular)
			bos.write(FontWriter.setNextInt16(this.minCharCode)); // USHORT usFirstCharIndex
			bos.write(FontWriter.setNextInt16(this.maxCharCode)); // USHORT usLastCharIndex
			bos.write(FontWriter.setNextInt16(ascender + descender)); // SHORT sTypoAscender
			bos.write(FontWriter.setNextInt16(0)); // SHORT sTypoDescender
			bos.write(FontWriter.setNextInt16(0)); // SHORT sTypoLineGap
			bos.write(FontWriter.setNextInt16(ascender)); // USHORT usWinAscent
			descender = Math.abs(descender);
			bos.write(FontWriter.setNextInt16(descender)); // USHORT usWinDescent
			bos.write(FontWriter.setNextUint32(0)); // ULONG ulCodePageRange1 Bits 0-31
			bos.write(FontWriter.setNextUint32(0)); // ULONG ulCodePageRange2 Bits 32-63
			bos.write(FontWriter.setNextInt16(0)); // SHORT sxHeight
			bos.write(FontWriter.setNextInt16(0)); // SHORT sCapHeight
			bos.write(FontWriter.setNextInt16(0)); // USHORT usDefaultChar
			bos.write(FontWriter.setNextInt16(0)); // USHORT usBreakChar
			bos.write(FontWriter.setNextInt16(0)); // USHORT usMaxContext
		}
		else {
			bos.write(FontWriter.setNextInt16(3)); // USHORT version 0x0004
			bos.write(FontWriter.setNextInt16(this.xAvgCharWidth)); // SHORT xAvgCharWidth

			bos.write(FontWriter.setNextInt16(400)); // USHORT usWeightClass (400 is normal)
			bos.write(FontWriter.setNextInt16(5)); // USHORT usWidthClass
			bos.write(FontWriter.setNextInt16(0)); // USHORT fsType
			bos.write(FontWriter.setNextInt16(102)); // SHORT ySubscriptXSize
			bos.write(FontWriter.setNextInt16(102)); // SHORT ySubscriptYSize
			bos.write(FontWriter.setNextInt16(0)); // SHORT ySubscriptXOffset
			bos.write(FontWriter.setNextInt16(0)); // SHORT ySubscriptYOffset
			bos.write(FontWriter.setNextInt16(102)); // SHORT ySuperscriptXSize
			bos.write(FontWriter.setNextInt16(102)); // SHORT ySuperscriptYSize
			bos.write(FontWriter.setNextInt16(0)); // SHORT ySuperscriptXOffset
			bos.write(FontWriter.setNextInt16(0)); // SHORT ySuperscriptYOffset
			bos.write(FontWriter.setNextInt16(102)); // SHORT yStrikeoutSize
			bos.write(FontWriter.setNextInt16(102)); // SHORT yStrikeoutPosition
			bos.write(FontWriter.setNextInt16(0)); // SHORT sFamilyClass
			bos.write(new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }); // BYTE panose[10] (see http://www.panose.com/hardware/pan2.asp)
			bos.write(FontWriter.setNextUint32(0)); // ULONG ulUnicodeRange1 Bits 0-31
			bos.write(FontWriter.setNextUint32(0)); // ULONG ulUnicodeRange2 Bits 32-63
			bos.write(FontWriter.setNextUint32(0)); // ULONG ulUnicodeRange3 Bits 64-95
			bos.write(FontWriter.setNextUint32(0)); // ULONG ulUnicodeRange4 Bits 96-127
			bos.write(new byte[4]); // CHAR achVendID[4] //see http://www.microsoft.com/typography/links/links.aspx?type=vendor&part=1
			bos.write(FontWriter.setNextInt16(64)); // USHORT fsSelection (currently hard-coded to regular)
			bos.write(FontWriter.setNextInt16(this.minCharCode)); // USHORT usFirstCharIndex
			bos.write(FontWriter.setNextInt16(this.maxCharCode)); // USHORT usLastCharIndex
			bos.write(FontWriter.setNextInt16(1000)); // SHORT sTypoAscender
			bos.write(FontWriter.setNextInt16(0)); // SHORT sTypoDescender
			bos.write(FontWriter.setNextInt16(0)); // SHORT sTypoLineGap
			bos.write(FontWriter.setNextInt16((int) (this.bounds[3] * this.scaling))); // USHORT usWinAscent
			bos.write(FontWriter.setNextInt16(-(int) (this.bounds[1] * this.scaling))); // USHORT usWinDescent
			bos.write(FontWriter.setNextUint32(0)); // ULONG ulCodePageRange1 Bits 0-31
			bos.write(FontWriter.setNextUint32(0)); // ULONG ulCodePageRange2 Bits 32-63
			bos.write(FontWriter.setNextInt16(0)); // SHORT sxHeight
			bos.write(FontWriter.setNextInt16(0)); // SHORT sCapHeight
			bos.write(FontWriter.setNextInt16(0)); // USHORT usDefaultChar
			bos.write(FontWriter.setNextInt16(0)); // USHORT usBreakChar
			bos.write(FontWriter.setNextInt16(0)); // USHORT usMaxContext
		}
		bos.flush();
		bos.close();

		return bos.toByteArray();
	}

	@Override
	public int getIntValue(int key) {
		return 0;
	}
}
