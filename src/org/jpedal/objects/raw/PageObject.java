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
 * PageObject.java
 * ---------------
 */
package org.jpedal.objects.raw;

import java.util.Arrays;

public class PageObject extends PdfObject {

	private byte[][] Annots, Contents, Kids, OpenAction;

	PdfObject AA, AcroForm, Group, OCProperties, O, OpenActionDict, PO, Properties, PV, Metadata, Outlines, Pages, MarkInfo, Names, StructTreeRoot;

	private int StructParents = -1, pageMode = -1;

	public PageObject(String ref) {
		super(ref);
	}

	public PageObject(int ref, int gen) {
		super(ref, gen);
	}

	public PageObject(int type) {
		super(type);
	}

	@Override
	public int getObjectType() {
		return PdfDictionary.Page;
	}

	@Override
	public boolean getBoolean(int id) {

		switch (id) {

		// case PdfDictionary.EncodedByteAlign:
		// return EncodedByteAlign;

			default:
				return super.getBoolean(id);
		}
	}

	@Override
	public void setBoolean(int id, boolean value) {

		switch (id) {

		// case PdfDictionary.EncodedByteAlign:
		// EncodedByteAlign=value;
		// break;

			default:
				super.setBoolean(id, value);
		}
	}

	@Override
	public PdfObject getDictionary(int id) {

		switch (id) {

			case PdfDictionary.AA:
				return this.AA;

			case PdfDictionary.AcroForm:
				return this.AcroForm;

			case PdfDictionary.Group:
				return this.Group;

			case PdfDictionary.MarkInfo:
				return this.MarkInfo;

			case PdfDictionary.Metadata:
				return this.Metadata;

			case PdfDictionary.O:
				return this.O;

			case PdfDictionary.OpenAction:
				return this.OpenActionDict;

			case PdfDictionary.OCProperties:
				return this.OCProperties;

			case PdfDictionary.Outlines:
				return this.Outlines;

			case PdfDictionary.Pages:
				return this.Pages;

			case PdfDictionary.PO:
				return this.PO;

			case PdfDictionary.Properties:
				return this.Properties;

			case PdfDictionary.PV:
				return this.PV;

			case PdfDictionary.Names:
				return this.Names;

			case PdfDictionary.StructTreeRoot:
				return this.StructTreeRoot;

			default:
				return super.getDictionary(id);
		}
	}

	@Override
	public void setIntNumber(int id, int value) {

		switch (id) {

			case PdfDictionary.StructParents:
				this.StructParents = value;
				break;

			default:
				super.setIntNumber(id, value);
		}
	}

	@Override
	public int getInt(int id) {

		switch (id) {

			case PdfDictionary.StructParents:
				return this.StructParents;

			default:
				return super.getInt(id);
		}
	}

	@Override
	public void setDictionary(int id, PdfObject value) {

		value.setID(id);

		switch (id) {

			case PdfDictionary.AA:
				this.AA = value;
				break;

			case PdfDictionary.AcroForm:
				this.AcroForm = value;
				break;

			case PdfDictionary.Group:
				this.Group = value;
				break;

			case PdfDictionary.OCProperties:
				this.OCProperties = value;
				break;

			case PdfDictionary.MarkInfo:
				this.MarkInfo = value;
				break;

			case PdfDictionary.Metadata:
				this.Metadata = value;
				break;

			case PdfDictionary.O:
				this.O = value;
				break;

			case PdfDictionary.OpenAction:
				this.OpenActionDict = value;
				break;

			case PdfDictionary.Outlines:
				this.Outlines = value;
				break;

			case PdfDictionary.Pages:
				this.Pages = value;
				break;

			case PdfDictionary.PO:
				this.PO = value;
				break;

			case PdfDictionary.Properties:
				this.Properties = value;
				break;

			case PdfDictionary.PV:
				this.PV = value;
				break;

			case PdfDictionary.Names:
				this.Names = value;
				break;

			case PdfDictionary.StructTreeRoot:
				this.StructTreeRoot = value;
				break;

			default:
				super.setDictionary(id, value);
		}
	}

	@Override
	public int setConstant(int pdfKeyType, int keyStart, int keyLength, byte[] raw) {

		int PDFvalue = PdfDictionary.Unknown;

		int id = 0, x = 0, next;

		// convert token to unique key which we can lookup

		for (int i2 = keyLength - 1; i2 > -1; i2--) {

			next = raw[keyStart + i2];

			next = next - 48;

			id = id + ((next) << x);

			x = x + 8;
		}

		switch (id) {

			case PdfDictionary.Page:
				return super.setConstant(pdfKeyType, PdfDictionary.Page);

			case PdfDictionary.Pages:
				return super.setConstant(pdfKeyType, PdfDictionary.Pages);

			case PdfDictionary.PageMode:
				this.pageMode = id;
				break;

			default:

				PDFvalue = super.setConstant(pdfKeyType, id);

				if (PDFvalue == -1) {
					if (debug) {

						byte[] bytes = new byte[keyLength];

						System.arraycopy(raw, keyStart, bytes, 0, keyLength);
						System.out.println("key=" + new String(bytes) + ' ' + id + " not implemented in setConstant in " + this);

						System.out.println("final public static int " + new String(bytes) + '=' + id + ';');

					}
				}

				break;

		}

		// System.out.println(pdfKeyType+"="+PDFvalue);
		switch (pdfKeyType) {

		// case PdfDictionary.BaseEncoding:
		// BaseEncoding=PDFvalue;
		// break;

		}

		return PDFvalue;
	}

	@Override
	public int getParameterConstant(int key) {

		switch (key) {

			case PdfDictionary.PageMode:
				return this.pageMode;

				// case PdfDictionary.BaseEncoding:
				//
				// //special cases first
				// if(key==PdfDictionary.BaseEncoding && Encoding!=null && Encoding.isZapfDingbats)
				// return StandardFonts.ZAPF;
				// else if(key==PdfDictionary.BaseEncoding && Encoding!=null && Encoding.isSymbol)
				// return StandardFonts.SYMBOL;
				// else
				// return BaseEncoding;

				// check general values
			default:
				return super.getParameterConstant(key);
		}
	}

	public void setStream() {

		this.hasStream = true;
	}

	@Override
	public PdfArrayIterator getMixedArray(int id) {

		switch (id) {

			case PdfDictionary.OpenAction:
				return new PdfArrayIterator(this.OpenAction);

			default:
				return super.getMixedArray(id);
		}
	}

	@Override
	public double[] getDoubleArray(int id) {

		switch (id) {
			default:
				return super.getDoubleArray(id);

		}
	}

	@Override
	public void setDoubleArray(int id, double[] value) {

		switch (id) {

		// case PdfDictionary.FontMatrix:
		// FontMatrix=value;
		// break;

			default:
				super.setDoubleArray(id, value);
		}
	}

	@Override
	public byte[][] getKeyArray(int id) {

		switch (id) {

			case PdfDictionary.Annots:
				return deepCopy(this.Annots);

			case PdfDictionary.Contents:
				return deepCopy(this.Contents);

			case PdfDictionary.Kids:
				return deepCopy(this.Kids);

			default:
				return super.getKeyArray(id);
		}
	}

	@Override
	public void setKeyArray(int id, byte[][] value) {

		switch (id) {

			case PdfDictionary.Annots:
				this.Annots = value;
				break;

			case PdfDictionary.Kids:
				this.Kids = value;
				break;

			case PdfDictionary.Contents:
				this.Contents = value;
				break;

			default:
				super.setKeyArray(id, value);
		}
	}

	@Override
	public void setMixedArray(int id, byte[][] value) {

		switch (id) {

			case PdfDictionary.OpenAction:
				this.OpenAction = value;
				break;
			// case PdfDictionary.Differences:
			// Differences=value;
			// break;

			default:
				super.setMixedArray(id, value);
		}
	}

	@Override
	public float[] getFloatArray(int id) {

		switch (id) {
			default:
				return super.getFloatArray(id);

		}
	}

	@Override
	public void setFloatArray(int id, float[] value) {

		switch (id) {

		// case PdfDictionary.FontBBox:
		// FontBBox=value;
		// break;

			default:
				super.setFloatArray(id, value);
		}
	}

	@Override
	public void setName(int id, byte[] value) {

		switch (id) {

		// case PdfDictionary.CMapName:
		// rawCMapName=value;
		// break;

			default:
				super.setName(id, value);

		}
	}

	@Override
	public void setTextStreamValue(int id, byte[] value) {

		switch (id) {

		// case PdfDictionary.CharSet:
		// rawCharSet=value;
		// break;

			default:
				super.setTextStreamValue(id, value);

		}
	}

	@Override
	public String getName(int id) {

		switch (id) {

			case PdfDictionary.BaseFont:

				// setup first time
				// if(BaseFont==null && rawBaseFont!=null)
				// BaseFont=new String(rawBaseFont);
				//
				// return BaseFont;

			default:
				return super.getName(id);

		}
	}

	@Override
	public String getTextStreamValue(int id) {

		switch (id) {

		// case PdfDictionary.CharSet:
		//
		// //setup first time
		// if(CharSet==null && rawCharSet!=null)
		// CharSet=new String(rawCharSet);
		//
		// return CharSet;

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

		// case PdfDictionary.BaseFont:
		// data=rawBaseFont;
		// break;

		}

		// convert
		switch (mode) {
		// case PdfStrings.STANDARD:
		//
		// //setup first time
		// if(data!=null)
		// return new String(data);
		// else
		// return null;

			default:
				throw new RuntimeException("Value not defined in getName(int,mode)");
		}
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PageObject [");
		if (Annots != null) {
			builder.append("Annots=");
			builder.append(Arrays.toString(Annots));
			builder.append(", ");
		}
		if (Contents != null) {
			builder.append("Contents=");
			builder.append(Arrays.toString(Contents));
			builder.append(", ");
		}
		if (Kids != null) {
			builder.append("Kids=");
			for(int i=0; i<Kids.length;i++) {
				builder.append(new String(Kids[i])+", ");
			}
			builder.append(", ");
		}
		if (OpenAction != null) {
			builder.append("OpenAction=");
			builder.append(Arrays.toString(OpenAction));
			builder.append(", ");
		}
		if (AA != null) {
			builder.append("AA=");
			builder.append(AA);
			builder.append(", ");
		}
		if (AcroForm != null) {
			builder.append("AcroForm=");
			builder.append(AcroForm);
			builder.append(", ");
		}
		if (Group != null) {
			builder.append("Group=");
			builder.append(Group);
			builder.append(", ");
		}
		if (OCProperties != null) {
			builder.append("OCProperties=");
			builder.append(OCProperties);
			builder.append(", ");
		}
		if (O != null) {
			builder.append("O=");
			builder.append(O);
			builder.append(", ");
		}
		if (OpenActionDict != null) {
			builder.append("OpenActionDict=");
			builder.append(OpenActionDict);
			builder.append(", ");
		}
		if (PO != null) {
			builder.append("PO=");
			builder.append(PO);
			builder.append(", ");
		}
		if (Properties != null) {
			builder.append("Properties=");
			builder.append(Properties);
			builder.append(", ");
		}
		if (PV != null) {
			builder.append("PV=");
			builder.append(PV);
			builder.append(", ");
		}
		if (Metadata != null) {
			builder.append("Metadata=");
			builder.append(Metadata);
			builder.append(", ");
		}
		if (Outlines != null) {
			builder.append("Outlines=");
			builder.append(Outlines);
			builder.append(", ");
		}
		if (Pages != null) {
			builder.append("Pages=");
			builder.append(Pages);
			builder.append(", ");
		}
		if (MarkInfo != null) {
			builder.append("MarkInfo=");
			builder.append(MarkInfo);
			builder.append(", ");
		}
		if (Names != null) {
			builder.append("Names=");
			builder.append(Names);
			builder.append(", ");
		}
		if (StructTreeRoot != null) {
			builder.append("StructTreeRoot=");
			builder.append(StructTreeRoot);
			builder.append(", ");
		}
		builder.append("StructParents=");
		builder.append(StructParents);
		builder.append(", pageMode=");
		builder.append(pageMode);
		builder.append("]");
		return builder.toString();
	}
}