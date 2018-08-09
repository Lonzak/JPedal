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
 * MKObject.java
 * ---------------
 */
package org.jpedal.objects.raw;

import org.jpedal.utils.StringUtils;

public class MKObject extends FormObject {

	// unknown CMAP as String
	// String unknownValue=null;

	private float[] BC, BG = null;

	protected String AC, CA, RC;

	protected byte[] rawAC, rawCA, rawRC;

	private int TP = -1;

	int R = 0;

	// boolean ImageMask=false;

	// int FormType=0, Height=1, Width=1;

	private PdfObject I = null;

	/** creates a copy of this MKObject but in a new Object so that changes wont affect this MkObject */
	@Override
	public PdfObject duplicate() {

		MKObject copy = new MKObject();

		// System.out.println(source.getMKInt(PdfDictionary.TP)+" "+TP);

		int sourceTP = this.getInt(PdfDictionary.TP);
		if (sourceTP != -1) copy.setIntNumber(PdfDictionary.TP, sourceTP);

		int sourceR = this.getInt(PdfDictionary.R);
		copy.setIntNumber(PdfDictionary.R, sourceR);

		// make sure also added to getTextStreamValueAsByte
		int[] textStreams = new int[] { PdfDictionary.AC, PdfDictionary.CA, PdfDictionary.RC };

		for (int textStream : textStreams) {
			byte[] bytes = this.getTextStreamValueAsByte(textStream);
			if (bytes != null) copy.setTextStreamValue(textStream, bytes);
		}

		// make sure also added to getTextStreamValueAsByte
		int[] floatStreams = new int[] { PdfDictionary.BC, PdfDictionary.BG };

		for (int floatStream : floatStreams) {
			float[] floats = this.getFloatArray(floatStream);
			if (floats != null) copy.setFloatArray(floatStream, floats);
		}

		if (this.I != null) copy.I = this.I.duplicate();

		return copy;
	}

	public MKObject(String ref) {
		super(ref);
	}

	public MKObject(int ref, int gen) {
		super(ref, gen);
	}

	public MKObject(int type) {
		super(type);
	}

	public MKObject() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean getBoolean(int id) {

		switch (id) {

		// case PdfDictionary.ImageMask:
		// return ImageMask;

			default:
				return super.getBoolean(id);
		}
	}

	@Override
	public void setBoolean(int id, boolean value) {

		switch (id) {

		// case PdfDictionary.ImageMask:
		// ImageMask=value;
		// break;

			default:
				super.setBoolean(id, value);
		}
	}

	@Override
	public PdfObject getDictionary(int id) {

		switch (id) {

			case PdfDictionary.I:
				return this.I;
				//
				// case PdfDictionary.XObject:
				// return XObject;

			default:
				return super.getDictionary(id);
		}
	}

	@Override
	public void setIntNumber(int id, int value) {

		switch (id) {

			case PdfDictionary.R:
				this.R = value;
				break;

			case PdfDictionary.TP:
				this.TP = value;
				break;

			default:
				super.setIntNumber(id, value);
		}
	}

	@Override
	public int getInt(int id) {

		switch (id) {

			case PdfDictionary.R:
				return this.R;

			case PdfDictionary.TP:
				return this.TP;

			default:
				return super.getInt(id);
		}
	}

	@Override
	public void setDictionary(int id, PdfObject value) {

		value.setID(id);
		switch (id) {

			case PdfDictionary.I:
				this.I = value;
				break;
			//
			// case PdfDictionary.XObject:
			// XObject=value;
			// break;

			default:
				super.setDictionary(id, value);
		}
	}

	// public void setStream(){
	//
	// hasStream=true;
	// }

	@Override
	public PdfArrayIterator getMixedArray(int id) {

		switch (id) {

		// case PdfDictionary.Differences:
		// return new PdfArrayIterator(Differences);

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
	public int[] getIntArray(int id) {

		switch (id) {

			default:
				return super.getIntArray(id);
		}
	}

	@Override
	public void setIntArray(int id, int[] value) {

		switch (id) {

			default:
				super.setIntArray(id, value);
		}
	}

	@Override
	public void setMixedArray(int id, byte[][] value) {

		switch (id) {

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

			case PdfDictionary.BC:
				return this.BC;

			case PdfDictionary.BG:
				return this.BG;

			default:
				return super.getFloatArray(id);

		}
	}

	@Override
	public void setFloatArray(int id, float[] value) {

		switch (id) {

			case PdfDictionary.BC:
				this.BC = value;
				break;

			case PdfDictionary.BG:
				this.BG = value;
				break;

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
	public byte[] getTextStreamValueAsByte(int id) {

		switch (id) {

			case PdfDictionary.AC:
				return this.rawAC;

			case PdfDictionary.CA:
				return this.rawCA;

			case PdfDictionary.RC:
				return this.rawRC;

			default:
				return super.getTextStreamValueAsByte(id);

		}
	}

	@Override
	public String getName(int id) {

		switch (id) {

		// case PdfDictionary.BaseFont:
		//
		// //setup first time
		// if(BaseFont==null && rawBaseFont!=null)
		// BaseFont=new String(rawBaseFont);
		//
		// return BaseFont;

			default:
				return super.getName(id);

		}
	}

	@Override
	public void setTextStreamValue(int id, byte[] value) {

		switch (id) {

			case PdfDictionary.AC:
				this.rawAC = value;
				break;

			case PdfDictionary.CA:
				this.rawCA = value;
				break;

			case PdfDictionary.RC:
				this.rawRC = value;
				break;

			default:
				super.setTextStreamValue(id, value);

		}
	}

	@Override
	public String getTextStreamValue(int id) {

		switch (id) {

			case PdfDictionary.AC:

				// setup first time
				if (this.AC == null && this.rawAC != null) this.AC = StringUtils.getTextString(this.rawAC, false);

				return this.AC;

			case PdfDictionary.CA:

				// setup first time
				if (this.CA == null && this.rawCA != null) this.CA = StringUtils.getTextString(this.rawCA, false);
				return this.CA;

			case PdfDictionary.RC:

				// setup first time
				if (this.RC == null && this.rawRC != null) this.RC = StringUtils.getTextString(this.rawRC, false);

				return this.RC;

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
					else return new String(data);
				}
				else return null;

			default:
				throw new RuntimeException("Value not defined in getName(int,mode) in " + this);
		}
	}

	@Override
	public byte[][] getKeyArray(int id) {

		switch (id) {

			default:
				return super.getKeyArray(id);
		}
	}

	@Override
	public void setKeyArray(int id, byte[][] value) {

		switch (id) {

			default:
				super.setKeyArray(id, value);
		}
	}

	@Override
	public boolean decompressStreamWhenRead() {
		return true;
	}

	@Override
	public int getObjectType() {
		return PdfDictionary.MK;
	}
}