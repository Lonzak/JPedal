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
 * HeadWriter.java
 * ---------------
 */
package org.jpedal.fonts.tt.conversion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jpedal.fonts.tt.Head;

public class HeadWriter extends Head implements FontTableWriter {

	private static final long serialVersionUID = 2762765966941340646L;

	public HeadWriter(float[] fontBounds) {
		this.FontBBox = fontBounds;
	}

	@Override
	public byte[] writeTable() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		bos.write(FontWriter.setNextUint32(65536)); // id
		bos.write(FontWriter.setNextUint32(65536)); // revision

		// slot for checksum
		bos.write(FontWriter.setNextUint32(0));

		bos.write(FontWriter.setNextUint32(1594834165)); // magic number 5F0F3CF5

		bos.write(FontWriter.setNextUint16(this.flags));
		bos.write(FontWriter.setNextUint16(this.unitsPerEm));

		// set value for dates (sample taken from a tt font)
		bos.write(FontWriter.setNextUint64(3405888000L));
		bos.write(FontWriter.setNextUint64(3405888000L));

		// bounds
		for (int i = 0; i < 4; i++)
			bos.write(FontWriter.setNextSignedInt16((short) this.FontBBox[i]));

		// more flags
		bos.write(FontWriter.setNextUint16(0)); // macStyle
		bos.write(FontWriter.setNextUint16(7)); // lowestRecPPEM (value is a guess based on sample TT)
		bos.write(FontWriter.setNextUint16(2)); // fontDirectionHint deprecated (set to 2)

		// finally the bit we want indicating size of chunks in mapx
		bos.write(FontWriter.setNextUint16(this.format));

		bos.write(FontWriter.setNextUint16(0)); // not used but in spec (glyphDataFormat)

		bos.flush();
		bos.close();

		return bos.toByteArray();
	}

	@Override
	public int getIntValue(int key) {
		return 0;
	}
}
