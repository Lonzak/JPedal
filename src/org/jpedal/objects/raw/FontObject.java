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
 * FontObject.java
 * ---------------
 */
package org.jpedal.objects.raw;

import java.util.Arrays;

import org.jpedal.fonts.StandardFonts;
import org.jpedal.utils.LogWriter;

public class FontObject extends PdfObject {

	// unknown CMAP as String
	String unknownValue = null;

	// mapped onto Type1
	final private static int MMType1 = 1230852645;

	// mapped onto Type1
	final private static int Type1C = 1077224796;

	final private static int ZaDb = 707859506;

	final private static int ZapfDingbats = 1889256112;

	final private static int Symbol = 1026712197;

	private PdfObject CharProcs = null, CIDSet = null, CIDSystemInfo = null, CIDToGIDMap = null, DescendantFonts = null, FontDescriptor = null,
			FontFile, FontFile2, FontFile3, ToUnicode;

	int BaseEncoding = PdfDictionary.Unknown;

	int CIDToGIDMapAsConstant = PdfDictionary.Unknown;

	int FirstChar = 1, LastChar = 255, Flags = 0, MissingWidth = 0, DW = -1, StemV = 0, Supplement = 0;

	float Ascent = 0, Descent = 0;

	float[] Widths = null, FontBBox = null;

	double[] FontMatrix = null;

	byte[][] Differences = null;

	private byte[] rawBaseFont = null, rawCharSet = null, rawCMapName = null, rawFontName = null, rawFontStretch = null, rawOrdering = null,
			rawRegistry = null, rawW = null, rawW2 = null;

	private String BaseFont = null, CharSet = null, CMapName = null, FontName = null, FontStretch = null, Ordering = null, Registry = null, W = null,
			W2 = null;

	public FontObject(String ref) {
		super(ref);
	}

	public FontObject(int ref, int gen) {
		super(ref, gen);
	}

	public FontObject(int type) {
		super(type);
	}

	@Override
	public PdfObject getDictionary(int id) {

		switch (id) {

			case PdfDictionary.CharProcs:
				return this.CharProcs;

			case PdfDictionary.CIDSet:
				return this.CIDSet;

			case PdfDictionary.CIDSystemInfo:
				return this.CIDSystemInfo;

			case PdfDictionary.CIDToGIDMap:
				return this.CIDToGIDMap;

			case PdfDictionary.DescendantFonts:
				return this.DescendantFonts;

			case PdfDictionary.Encoding:
				return this.Encoding;

			case PdfDictionary.FontDescriptor:
				return this.FontDescriptor;

			case PdfDictionary.FontFile:
				return this.FontFile;

			case PdfDictionary.FontFile2:
				return this.FontFile2;

			case PdfDictionary.FontFile3:
				return this.FontFile3;

			case PdfDictionary.ToUnicode:
				return this.ToUnicode;

			default:
				return super.getDictionary(id);
		}
	}

	@Override
	public void setIntNumber(int id, int value) {

		switch (id) {

			case PdfDictionary.Ascent:
				this.Ascent = value;
				break;

			case PdfDictionary.Descent:
				this.Descent = value;
				break;

			case PdfDictionary.DW:
				this.DW = value;
				break;

			case PdfDictionary.FirstChar:
				this.FirstChar = value;
				break;

			case PdfDictionary.Flags:
				this.Flags = value;
				break;

			case PdfDictionary.LastChar:
				this.LastChar = value;
				break;

			case PdfDictionary.MissingWidth:
				this.MissingWidth = value;
				break;

			case PdfDictionary.StemV:
				this.StemV = value;
				break;

			case PdfDictionary.Supplement:
				this.Supplement = value;
				break;

			default:
				super.setIntNumber(id, value);
		}
	}

	@Override
	public int getInt(int id) {

		switch (id) {

			case PdfDictionary.DW:
				return this.DW;

			case PdfDictionary.FirstChar:
				return this.FirstChar;

			case PdfDictionary.Flags:
				return this.Flags;

			case PdfDictionary.LastChar:
				return this.LastChar;

			case PdfDictionary.MissingWidth:
				return this.MissingWidth;

			case PdfDictionary.StemV:
				return this.StemV;

			case PdfDictionary.Supplement:
				return this.Supplement;

			default:
				return super.getInt(id);
		}
	}

	@Override
	public void setFloatNumber(int id, float value) {

		switch (id) {

			case PdfDictionary.Ascent:
				this.Ascent = value;
				break;

			case PdfDictionary.Descent:
				this.Descent = value;
				break;

			default:
				super.setFloatNumber(id, value);
		}
	}

	@Override
	public float getFloatNumber(int id) {

		switch (id) {

			case PdfDictionary.Ascent:
				return this.Ascent;

			case PdfDictionary.Descent:
				return this.Descent;

			default:
				return super.getFloatNumber(id);
		}
	}

	@Override
	public void setDictionary(int id, PdfObject value) {

		value.setID(id);

		// flag embedded data
		if (id == PdfDictionary.FontFile || id == PdfDictionary.FontFile2 || id == PdfDictionary.FontFile3) this.hasStream = true;

		switch (id) {

			case PdfDictionary.CharProcs:
				this.CharProcs = value;
				break;

			case PdfDictionary.CIDSet:
				this.CIDSet = value;
				break;

			case PdfDictionary.CIDSystemInfo:
				this.CIDSystemInfo = value;
				break;

			case PdfDictionary.CIDToGIDMap:
				this.CIDToGIDMap = value;
				break;

			case PdfDictionary.DescendantFonts:
				this.DescendantFonts = value;
				break;

			case PdfDictionary.Encoding:
				this.Encoding = value;
				break;

			case PdfDictionary.FontDescriptor:
				this.FontDescriptor = value;
				break;

			case PdfDictionary.FontFile:
				this.FontFile = value;
				break;

			case PdfDictionary.FontFile2:
				this.FontFile2 = value;
				break;

			case PdfDictionary.FontFile3:
				this.FontFile3 = value;
				break;

			case PdfDictionary.ToUnicode:
				this.ToUnicode = value;
				break;

			default:
				super.setDictionary(id, value);
		}
	}

	@Override
	public int setConstant(int pdfKeyType, int keyStart, int keyLength, byte[] raw) {

		int PDFvalue = PdfDictionary.Unknown;

		int id = 0, x = 0, next;

		try {

			// convert token to unique key which we can lookup

			for (int i2 = keyLength - 1; i2 > -1; i2--) {

				next = raw[keyStart + i2];

				next = next - 48;

				id = id + ((next) << x);

				x = x + 8;
			}

			switch (id) {

				case StandardFonts.CIDTYPE0:
					PDFvalue = StandardFonts.CIDTYPE0;
					break;

				case PdfDictionary.CIDFontType0C:
					PDFvalue = PdfDictionary.CIDFontType0C;
					break;

				case StandardFonts.CIDTYPE2:
					PDFvalue = StandardFonts.CIDTYPE2;
					break;

				case PdfDictionary.CMap:
					PDFvalue = PdfDictionary.CMap;
					break;

				case PdfDictionary.Encoding:
					PDFvalue = PdfDictionary.Encoding;
					break;

				case PdfDictionary.Identity_H:
					PDFvalue = PdfDictionary.Identity_H;
					break;

				case PdfDictionary.Identity_V:
					PDFvalue = PdfDictionary.Identity_V;
					break;

				case PdfDictionary.MacExpertEncoding:
					PDFvalue = StandardFonts.MACEXPERT;
					break;

				case PdfDictionary.MacRomanEncoding:
					PDFvalue = StandardFonts.MAC;
					break;

				case PdfDictionary.PDFDocEncoding:
					PDFvalue = StandardFonts.PDF;
					break;

				case MMType1:
					PDFvalue = StandardFonts.TYPE1;
					break;

				case PdfDictionary.StandardEncoding:
					PDFvalue = StandardFonts.STD;
					break;

				case StandardFonts.TYPE0:
					PDFvalue = StandardFonts.TYPE0;
					break;

				case StandardFonts.TYPE1:
					PDFvalue = StandardFonts.TYPE1;
					break;

				case Type1C:
					PDFvalue = StandardFonts.TYPE1;
					break;

				case StandardFonts.TYPE3:
					PDFvalue = StandardFonts.TYPE3;
					break;

				case StandardFonts.TRUETYPE:
					PDFvalue = StandardFonts.TRUETYPE;
					break;

				case PdfDictionary.WinAnsiEncoding:
					PDFvalue = StandardFonts.WIN;
					break;

				default:

					if (pdfKeyType == PdfDictionary.Encoding) {
						PDFvalue = CIDEncodings.getConstant(id);

						if (PDFvalue == PdfDictionary.Unknown) {

							byte[] bytes = new byte[keyLength];

							System.arraycopy(raw, keyStart, bytes, 0, keyLength);

							this.unknownValue = new String(bytes);
						}

						if (debug && PDFvalue == PdfDictionary.Unknown) {
							System.out.println("Value not in PdfCIDEncodings");

							byte[] bytes = new byte[keyLength];

							System.arraycopy(raw, keyStart, bytes, 0, keyLength);
							System.out.println("Add to CIDEncodings and as String");
							System.out.println("key=" + new String(bytes) + ' ' + id + " not implemented in setConstant in PdfFont Object");

							System.out.println("final public static int CMAP_" + new String(bytes) + '=' + id + ';');

						}
					}
					else PDFvalue = super.setConstant(pdfKeyType, id);

					if (PDFvalue == -1) {

						if (debug) {

							byte[] bytes = new byte[keyLength];

							System.arraycopy(raw, keyStart, bytes, 0, keyLength);
							System.out.println("key=" + new String(bytes) + ' ' + id + " not implemented in setConstant in PdfFont Object");

							System.out.println("final public static int " + new String(bytes) + '=' + id + ';');

						}

					}

					break;

			}

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		// System.out.println(pdfKeyType+"="+PDFvalue);
		switch (pdfKeyType) {

			case PdfDictionary.BaseEncoding:
				this.BaseEncoding = PDFvalue;
				break;

			case PdfDictionary.CIDToGIDMap:
				this.CIDToGIDMapAsConstant = PDFvalue;
				break;

			case PdfDictionary.Encoding:
				this.generalType = PDFvalue;
				break;

			// case PdfDictionary.Subtype:
			// subtype=PDFvalue;
			// //System.out.println("value set to "+subtype);
			// break;
			case PdfDictionary.ToUnicode:
				this.generalType = PDFvalue;
				break;
			default:
				super.setConstant(pdfKeyType, PDFvalue);
		}

		return PDFvalue;
	}

	@Override
	public int getParameterConstant(int key) {

		int def;

		switch (key) {

			case PdfDictionary.BaseEncoding:

				// special cases first
				if (key == PdfDictionary.BaseEncoding && this.Encoding != null && this.Encoding.isZapfDingbats) return StandardFonts.ZAPF;
				else
					if (key == PdfDictionary.BaseEncoding && this.Encoding != null && this.Encoding.isSymbol) return StandardFonts.SYMBOL;
					else return this.BaseEncoding;

			case PdfDictionary.CIDToGIDMap:
				return this.CIDToGIDMapAsConstant;
		}

		// check general values
		def = super.getParameterConstant(key);

		return def;
	}

	public void setStream() {

		this.hasStream = true;
	}

	@Override
	public PdfArrayIterator getMixedArray(int id) {

		switch (id) {

			case PdfDictionary.Differences:
				return new PdfArrayIterator(this.Differences);

			default:
				return super.getMixedArray(id);

		}
	}

	@Override
	public byte[][] getByteArray(int id) {

		switch (id) {

			case PdfDictionary.Differences:
				return this.Differences;

			default:
				return super.getByteArray(id);

		}
	}

	@Override
	public double[] getDoubleArray(int id) {

		switch (id) {

			case PdfDictionary.FontMatrix:
				return deepCopy(this.FontMatrix);

			default:
				return super.getDoubleArray(id);
		}
	}

	@Override
	public void setDoubleArray(int id, double[] value) {

		switch (id) {

			case PdfDictionary.FontMatrix:
				this.FontMatrix = value;
				break;

			default:
				super.setDoubleArray(id, value);
		}
	}

	@Override
	public void setMixedArray(int id, byte[][] value) {

		switch (id) {

			case PdfDictionary.Differences:
				this.Differences = value;
				break;

			default:
				super.setMixedArray(id, value);

		}
	}

	@Override
	public float[] getFloatArray(int id) {

		switch (id) {

			case PdfDictionary.FontBBox:
				return deepCopy(this.FontBBox);

			case PdfDictionary.Widths:
				return deepCopy(this.Widths);

			default:
				return super.getFloatArray(id);
		}
	}

	@Override
	public void setFloatArray(int id, float[] value) {

		switch (id) {

			case PdfDictionary.FontBBox:
				this.FontBBox = value;
				break;

			case PdfDictionary.Widths:
				this.Widths = value;
				break;

			default:
				super.setFloatArray(id, value);
		}
	}

	@Override
	public void setName(int id, byte[] value) {

		switch (id) {

			case PdfDictionary.BaseFont:
				this.rawBaseFont = value;

				// track if font called ZapfDingbats and flag
				int checksum = PdfDictionary.generateChecksum(0, value.length, value);

				this.isZapfDingbats = (checksum == ZapfDingbats || checksum == ZaDb);
				this.isSymbol = (checksum == Symbol);

				// store in both as we can't guarantee creation order
				if (this.Encoding != null) {
					this.Encoding.isZapfDingbats = this.isZapfDingbats;
					this.Encoding.isSymbol = this.isSymbol;
				}

				break;

			case PdfDictionary.CMapName:
				this.rawCMapName = value;
				break;

			case PdfDictionary.FontName:
				this.rawFontName = value;
				break;

			case PdfDictionary.FontStretch:
				this.rawFontStretch = value;
				break;

			default:
				super.setName(id, value);

		}
	}

	@Override
	public void setTextStreamValue(int id, byte[] value) {

		switch (id) {

			case PdfDictionary.CharSet:
				this.rawCharSet = value;
				break;

			case PdfDictionary.Ordering:
				this.rawOrdering = value;
				break;

			case PdfDictionary.Registry:
				this.rawRegistry = value;
				break;

			case PdfDictionary.W:
				this.rawW = value;
				break;

			case PdfDictionary.W2:
				this.rawW2 = value;
				break;

			default:
				super.setTextStreamValue(id, value);

		}
	}

	@Override
	public String getName(int id) {

		switch (id) {

			case PdfDictionary.BaseFont:

				// setup first time
				if (this.BaseFont == null && this.rawBaseFont != null) this.BaseFont = new String(this.rawBaseFont);

				return this.BaseFont;

			case PdfDictionary.CMapName:

				// setup first time
				if (this.CMapName == null && this.rawCMapName != null) this.CMapName = new String(this.rawCMapName);

				return this.CMapName;

			case PdfDictionary.FontName:

				// setup first time
				if (this.FontName == null && this.rawFontName != null) this.FontName = new String(this.rawFontName);

				return this.FontName;

			case PdfDictionary.FontStretch:
				// setup first time
				if (this.FontStretch == null && this.rawFontStretch != null) this.FontStretch = new String(this.rawFontStretch);

				return this.FontStretch;

			case PdfDictionary.W:

				// setup first time
				if (this.W == null && this.rawW != null) this.W = new String(this.rawW);

				return this.W;

			case PdfDictionary.W2:

				// setup first time
				if (this.W2 == null && this.rawW2 != null) this.W2 = new String(this.rawW2);

				return this.W2;

			default:
				return super.getName(id);

		}
	}

	@Override
	public String getTextStreamValue(int id) {

		switch (id) {

			case PdfDictionary.CharSet:

				// setup first time
				if (this.CharSet == null && this.rawCharSet != null) this.CharSet = new String(this.rawCharSet);

				return this.CharSet;

			case PdfDictionary.Ordering:

				// setup first time
				if (this.Ordering == null && this.rawOrdering != null) this.Ordering = new String(this.rawOrdering);

				return this.Ordering;

			case PdfDictionary.Registry:

				// setup first time
				if (this.Registry == null && this.rawRegistry != null) this.Registry = new String(this.rawRegistry);

				return this.Registry;

			case PdfDictionary.W:

				// setup first time
				if (this.W == null && this.rawW != null) this.W = new String(this.rawW);

				return this.W;

			case PdfDictionary.W2:

				// setup first time
				if (this.W2 == null && this.rawW2 != null) this.W2 = new String(this.rawW2);

				return this.W2;

			default:
				return super.getTextStreamValue(id);

		}
	}

	/**
	 * unless you need special fucntions, use getStringValue(int id) which is faster
	 */
	@Override
	public String getStringValue(int id, int mode) {

		byte[] data = null;

		// get data
		switch (id) {

			case PdfDictionary.BaseFont:
				data = this.rawBaseFont;
				break;

			case PdfDictionary.CMapName:
				data = this.rawCMapName;
				break;

			case PdfDictionary.FontName:
				data = this.rawFontName;
				break;

			case PdfDictionary.FontStretch:
				data = this.rawFontStretch;
				break;
		}

		// convert
		switch (mode) {
			case PdfDictionary.STANDARD:

				// setup first time
				if (data != null) return new String(data);
				else return null;

			case PdfDictionary.LOWERCASE:

				// setup first time
				if (data != null) return new String(data);
				else return null;

			case PdfDictionary.REMOVEPOSTSCRIPTPREFIX:

				// setup first time
				if (data != null) {
					int len = data.length;
					if (len > 6 && data[6] == '+') { // lose ABCDEF+ if present
						int length = len - 7;
						byte[] newData = new byte[length];
						System.arraycopy(data, 7, newData, 0, length);
						return new String(newData);
					}
					else
						if (len > 7 && data[len - 7] == '+') { // lose +ABCDEF if present
							int length = len - 7;
							byte[] newData = new byte[length];
							System.arraycopy(data, 0, newData, 0, length);
							return new String(newData);
						}
						else return new String(data);
				}
				else return null;

			default:
				throw new RuntimeException("Value not defined in getName(int,mode)");
		}
	}

	@Override
	public int getObjectType() {
		return PdfDictionary.Font;
	}

	@Override
	public boolean decompressStreamWhenRead() {
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FontObject [");
		builder.append("ID=");
		builder.append(super.getID());
		builder.append(", ");
		if (super.getName(PdfDictionary.Name) != null) {
			builder.append("FontName=");
			builder.append(super.getName(PdfDictionary.Name));
			builder.append(", ");
		}
		if (this.rawFontName != null) {
			builder.append("rawFontName=");
			builder.append(new String(this.rawFontName));
			builder.append(", ");
		}
		if (this.rawBaseFont != null) {
			builder.append("rawBaseFont=");
			builder.append(new String(this.rawBaseFont));
			builder.append(", ");
		}
		if (this.unknownValue != null) {
			builder.append("unknownValue=");
			builder.append(this.unknownValue);
			builder.append(", ");
		}
		if (this.CharProcs != null) {
			builder.append("CharProcs=");
			builder.append(this.CharProcs);
			builder.append(", ");
		}
		if (this.CIDSet != null) {
			builder.append("CIDSet=");
			builder.append(this.CIDSet);
			builder.append(", ");
		}
		if (this.CIDSystemInfo != null) {
			builder.append("CIDSystemInfo=");
			builder.append(this.CIDSystemInfo);
			builder.append(", ");
		}
		if (this.CIDToGIDMap != null) {
			builder.append("CIDToGIDMap=");
			builder.append(this.CIDToGIDMap);
			builder.append(", ");
		}
		if (this.DescendantFonts != null) {
			builder.append("DescendantFonts=");
			builder.append(this.DescendantFonts);
			builder.append(", ");
		}
		if (this.FontDescriptor != null) {
			builder.append("FontDescriptor=");
			builder.append(this.FontDescriptor);
			builder.append(", ");
		}
		if (this.FontFile != null) {
			builder.append("FontFile=");
			builder.append(this.FontFile);
			builder.append(", ");
		}
		if (this.FontFile2 != null) {
			builder.append("FontFile2=");
			builder.append(this.FontFile2);
			builder.append(", ");
		}
		if (this.FontFile3 != null) {
			builder.append("FontFile3=");
			builder.append(this.FontFile3);
			builder.append(", ");
		}
		if (this.ToUnicode != null) {
			builder.append("ToUnicode=");
			builder.append(this.ToUnicode);
			builder.append(", ");
		}
		builder.append("BaseEncoding=");
		builder.append(this.BaseEncoding);
		builder.append(", CIDToGIDMapAsConstant=");
		builder.append(this.CIDToGIDMapAsConstant);
		builder.append(", FirstChar=");
		builder.append(this.FirstChar);
		builder.append(", LastChar=");
		builder.append(this.LastChar);
		builder.append(", Flags=");
		builder.append(this.Flags);
		builder.append(", MissingWidth=");
		builder.append(this.MissingWidth);
		builder.append(", DW=");
		builder.append(this.DW);
		builder.append(", StemV=");
		builder.append(this.StemV);
		builder.append(", Supplement=");
		builder.append(this.Supplement);
		builder.append(", Ascent=");
		builder.append(this.Ascent);
		builder.append(", Descent=");
		builder.append(this.Descent);
		builder.append(", ");
		if (this.Widths != null) {
			builder.append("Widths=");
			builder.append(Arrays.toString(this.Widths));
			builder.append(", ");
		}
		if (this.FontBBox != null) {
			builder.append("FontBBox=");
			builder.append(Arrays.toString(this.FontBBox));
			builder.append(", ");
		}
		if (this.FontMatrix != null) {
			builder.append("FontMatrix=");
			builder.append(Arrays.toString(this.FontMatrix));
			builder.append(", ");
		}
		if (this.Differences != null) {
			builder.append("Differences=[");
			
			for (int i = 0; i < this.Differences.length; i++) {
				builder.append(new String(this.Differences[i]));
				builder.append(", ");
			}
			builder.append("]");
		}
		if (this.rawCharSet != null) {
			builder.append("rawCharSet=");
			builder.append(new String(this.rawCharSet));
			builder.append(", ");
		}
		if (this.rawCMapName != null) {
			builder.append("rawCMapName=");
			builder.append(new String(this.rawCMapName));
			builder.append(", ");
		}
		if (this.rawFontStretch != null) {
			builder.append("rawFontStretch=");
			builder.append(new String(this.rawFontStretch));
			builder.append(", ");
		}
		if (this.rawOrdering != null) {
			builder.append("rawOrdering=");
			builder.append(new String(this.rawOrdering));
			builder.append(", ");
		}
		if (this.rawRegistry != null) {
			builder.append("rawRegistry=");
			builder.append(new String(this.rawRegistry));
			builder.append(", ");
		}
		if (this.rawW != null) {
			builder.append("rawW=");
			builder.append(new String(this.rawW));
			builder.append(", ");
		}
		if (this.rawW2 != null) {
			builder.append("rawW2=");
			builder.append(new String(this.rawW2));
			builder.append(", ");
		}
		if (this.BaseFont != null) {
			builder.append("BaseFont=");
			builder.append(this.BaseFont);
			builder.append(", ");
		}
		if (this.CharSet != null) {
			builder.append("CharSet=");
			builder.append(this.CharSet);
			builder.append(", ");
		}
		if (this.CMapName != null) {
			builder.append("CMapName=");
			builder.append(this.CMapName);
			builder.append(", ");
		}
		if (this.FontName != null) {
			builder.append("FontName=");
			builder.append(this.FontName);
			builder.append(", ");
		}
		if (this.FontStretch != null) {
			builder.append("FontStretch=");
			builder.append(this.FontStretch);
			builder.append(", ");
		}
		if (this.Ordering != null) {
			builder.append("Ordering=");
			builder.append(this.Ordering);
			builder.append(", ");
		}
		if (this.Registry != null) {
			builder.append("Registry=");
			builder.append(this.Registry);
			builder.append(", ");
		}
		if (this.W != null) {
			builder.append("W=");
			builder.append(this.W);
			builder.append(", ");
		}
		if (this.W2 != null) {
			builder.append("W2=");
			builder.append(this.W2);
		}
		builder.append("]");
		return builder.toString();
	}
	
	
}
