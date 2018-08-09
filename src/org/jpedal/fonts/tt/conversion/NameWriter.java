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
 * NameWriter.java
 * ---------------
 */
package org.jpedal.fonts.tt.conversion;

import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.jpedal.fonts.PdfFont;
import org.jpedal.fonts.glyph.PdfJavaGlyphs;
import org.jpedal.fonts.tt.Name;

public class NameWriter extends Name implements FontTableWriter {

	private static final long serialVersionUID = -962145337573257284L;
	String[] strings = new String[7];

	/**
	 * used to turn Ps into OTF
	 */
	public NameWriter(PdfFont currentFontData, PdfJavaGlyphs glyphs, String name) {

		name = name.replaceAll("[.,<>*#]", "-");
		// copyright
		if (currentFontData.getCopyright() != null) this.strings[0] = currentFontData.getCopyright();
		else this.strings[0] = "No copyright information found.";

		// familyName
		this.strings[1] = name;

		// subName
		switch (glyphs.style) {
			case Font.PLAIN:
				this.strings[2] = "Roman";
				break;
			case Font.BOLD:
				this.strings[2] = "Bold";
				break;
			case Font.ITALIC:
				this.strings[2] = "Italic";
				break;
			default:
				this.strings[2] = "Roman";
				break;
		}

		// uid
		this.strings[3] = "JPedal PDF2HTML " + name + ' ' + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

		// fullName
		this.strings[4] = name;

		// version
		this.strings[5] = "Version 1.0";

		// psName
		this.strings[6] = name;
	}

	@Override
	public byte[] writeTable() throws IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		bos.write(FontWriter.setNextUint16(0)); // format
		bos.write(FontWriter.setNextUint16(14)); // record count
		bos.write(FontWriter.setNextUint16(174)); // Offset to strings

		int offset = 0;
		for (int i = 0; i < 7; i++) {
			bos.write(FontWriter.setNextUint16(1)); // mac
			bos.write(FontWriter.setNextUint16(0)); // Roman
			bos.write(FontWriter.setNextUint16(0)); // English
			bos.write(FontWriter.setNextUint16(i)); // String name id
			bos.write(FontWriter.setNextUint16(this.strings[i].length()));
			bos.write(FontWriter.setNextUint16(offset));

			offset += this.strings[i].length();
		}

		for (int i = 0; i < 7; i++) {
			bos.write(FontWriter.setNextUint16(3)); // windows
			bos.write(FontWriter.setNextUint16(1)); // UCS-2
			bos.write(FontWriter.setNextUint16(1033)); // US English
			bos.write(FontWriter.setNextUint16(i)); // String name id
			bos.write(FontWriter.setNextUint16(this.strings[i].length() * 2));
			bos.write(FontWriter.setNextUint16(offset));

			offset += this.strings[i].length() * 2;
		}

		for (int i = 0; i < 7; i++) {
			byte[] s = this.strings[i].getBytes("US-ASCII"); // Identical to MacRoman for the fist 7 bytes - doesn't use the 8th, so it can be
																// substituted safely
			for (byte value : s) {
				bos.write(FontWriter.setNextUint8(value));
			}
		}

		for (int i = 0; i < 7; i++) {
			byte[] s = this.strings[i].getBytes("UTF-16BE");
			for (byte value : s) {
				bos.write(FontWriter.setNextUint8(value));
			}
		}

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
