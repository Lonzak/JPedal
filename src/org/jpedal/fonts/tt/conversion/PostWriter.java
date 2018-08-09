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
 * PostWriter.java
 * ---------------
 */
package org.jpedal.fonts.tt.conversion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jpedal.fonts.tt.Post;

public class PostWriter extends Post implements FontTableWriter {

	private static final long serialVersionUID = -4361139825244723099L;

	/**
	 * used to turn Ps into OTF
	 */
	public PostWriter() {
	}

	@Override
	public byte[] writeTable() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		bos.write(FontWriter.setNextUint32(196608)); // Format - should be a fixed 3, but this should provide the same result
		bos.write(FontWriter.setNextUint32(0)); // Italic angle
		bos.write(FontWriter.setNextInt16(-125)); // Underline position
		bos.write(FontWriter.setNextInt16(54)); // Underline thickness
		bos.write(FontWriter.setNextUint32(0)); // isFixedPitch
		bos.write(FontWriter.setNextUint32(0)); // Min memory usage when downloading
		bos.write(FontWriter.setNextUint32(0)); // Max memory usage when downloading
		bos.write(FontWriter.setNextUint32(0)); // Min memory usage when downloading as Type 1
		bos.write(FontWriter.setNextUint32(0)); // Max memory usage when downloading as Type 1

		bos.flush();
		bos.close();

		return bos.toByteArray();
	}

	@Override
	public int getIntValue(int key) {

		int value = 0;

		switch (key) {
			default:
				break;
		}

		return value;
	}
}
