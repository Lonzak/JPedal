/**
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.jpedal.org
 *
 * (C) Copyright 2007, IDRsolutions and Contributors.
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

  * CMAPWriter.java
  * ---------------
  * (C) Copyright 2007, by IDRsolutions and Contributors.
  *
  *
  * --------------------------
 */
package org.jpedal.fonts.tt.conversion;

import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.tt.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class OS2Writer extends OS2 implements FontTableWriter{

    int glyphCount=0;

    int xAvgCharWidth=0,minCharCode, maxCharCode;
    float[] bounds;
    double scaling=1;



    /**
     * used to turn Ps into OTF
     */
    public OS2Writer(PdfJavaGlyphs glyphs,int xAvgCharWidth,int minCharCode, int maxCharCode, float[] bounds, double emSquareSize) {

        glyphCount=glyphs.getGlyphCount();

        this.minCharCode = minCharCode;
        this.maxCharCode = maxCharCode;

        this.bounds = bounds;

        this.xAvgCharWidth=xAvgCharWidth;

        //Set scaling for incorrect em square size
        if (emSquareSize != 1000) {
            scaling = 1d/(emSquareSize/1000);
        }
    }


    public byte[] writeTable() throws IOException {

        ByteArrayOutputStream bos=new ByteArrayOutputStream();

        bos.write(TTFontWriter.setNextInt16(4)); //USHORT	version	0x0004
        bos.write(TTFontWriter.setNextInt16(xAvgCharWidth)); //SHORT	xAvgCharWidth

        bos.write(TTFontWriter.setNextInt16(400)); //USHORT	usWeightClass  (400 is normal)
        bos.write(TTFontWriter.setNextInt16(100)); //USHORT	usWidthClass
        bos.write(TTFontWriter.setNextInt16(0)); //USHORT	fsType
        bos.write(TTFontWriter.setNextInt16(102)); //SHORT	ySubscriptXSize
        bos.write(TTFontWriter.setNextInt16(102)); //SHORT	ySubscriptYSize
        bos.write(TTFontWriter.setNextInt16(0)); //SHORT	ySubscriptXOffset
        bos.write(TTFontWriter.setNextInt16(0)); //SHORT	ySubscriptYOffset
        bos.write(TTFontWriter.setNextInt16(102)); //SHORT	ySuperscriptXSize
        bos.write(TTFontWriter.setNextInt16(102)); //SHORT	ySuperscriptYSize
        bos.write(TTFontWriter.setNextInt16(0)); //SHORT	ySuperscriptXOffset
        bos.write(TTFontWriter.setNextInt16(0)); //SHORT	ySuperscriptYOffset
        bos.write(TTFontWriter.setNextInt16(102)); //SHORT	yStrikeoutSize
        bos.write(TTFontWriter.setNextInt16(102)); //SHORT	yStrikeoutPosition
        bos.write(TTFontWriter.setNextInt16(0)); //SHORT	sFamilyClass
        bos.write(new byte[]{0,0,0,0,0,0,0,0,0,0}); //BYTE	panose[10]  (see http://www.panose.com/hardware/pan2.asp)
        bos.write(TTFontWriter.setNextUint32(0)); //ULONG	ulUnicodeRange1	Bits 0-31
        bos.write(TTFontWriter.setNextUint32(0)); //ULONG	ulUnicodeRange2	Bits 32-63
        bos.write(TTFontWriter.setNextUint32(0)); //ULONG	ulUnicodeRange3	Bits 64-95
        bos.write(TTFontWriter.setNextUint32(0)); //ULONG	ulUnicodeRange4	Bits 96-127
        bos.write(new byte[4]);                   //CHAR	achVendID[4] //see http://www.microsoft.com/typography/links/links.aspx?type=vendor&part=1
        bos.write(TTFontWriter.setNextInt16(64)); //USHORT	fsSelection  (currently hard-coded to regular)
        bos.write(TTFontWriter.setNextInt16(minCharCode)); //USHORT	usFirstCharIndex
        bos.write(TTFontWriter.setNextInt16(maxCharCode)); //USHORT	usLastCharIndex
        bos.write(TTFontWriter.setNextInt16(1000)); //SHORT	sTypoAscender
        bos.write(TTFontWriter.setNextInt16(0)); //SHORT	sTypoDescender
        bos.write(TTFontWriter.setNextInt16(0)); //SHORT	sTypoLineGap
        bos.write(TTFontWriter.setNextInt16((int)(bounds[3]*scaling))); //USHORT	usWinAscent
        bos.write(TTFontWriter.setNextInt16(-(int)(bounds[1]*scaling))); //USHORT	usWinDescent
        bos.write(TTFontWriter.setNextUint32(0)); //ULONG	ulCodePageRange1	Bits 0-31
        bos.write(TTFontWriter.setNextUint32(0)); //ULONG	ulCodePageRange2	Bits 32-63
        bos.write(TTFontWriter.setNextInt16(0)); //SHORT	sxHeight
        bos.write(TTFontWriter.setNextInt16(0)); //SHORT	sCapHeight
        bos.write(TTFontWriter.setNextInt16(0)); //USHORT	usDefaultChar
        bos.write(TTFontWriter.setNextInt16(0)); //USHORT	usBreakChar
        bos.write(TTFontWriter.setNextInt16(0)); //USHORT	usMaxContext

        bos.flush();
        bos.close();

        return bos.toByteArray();
    }

    public int getIntValue(int key) {
        return 0;
    }
}
