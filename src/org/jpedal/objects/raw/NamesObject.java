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
 * NamesObject.java
 * ---------------
 */
package org.jpedal.objects.raw;

import org.jpedal.utils.LogWriter;

public class NamesObject extends PdfObject {

	// unknown CMAP as String
	// String unknownValue=null;

	byte[] rawJS;

	String JSString;

	// private float[] Matrix;

	// boolean ImageMask=false;

	// int FormType=0, Height=1, Width=1;

	private PdfObject Dests = null, JavaScript = null, JS = null;

	private byte[][] Kids, Names = null;

	public NamesObject(String ref) {
		super(ref);
	}

	public NamesObject(int ref, int gen) {
		super(ref, gen);
	}

	public NamesObject(int type) {
		super(type);
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

			case PdfDictionary.Dests:
				return this.Dests;

			case PdfDictionary.JavaScript:
				return this.JavaScript;

			case PdfDictionary.JS:
				return this.JS;

			default:
				return super.getDictionary(id);
		}
	}

	@Override
	public void setIntNumber(int id, int value) {

		switch (id) {

		// case PdfDictionary.FormType:
		// FormType=value;
		// break;
		//
		// case PdfDictionary.Height:
		// Height=value;
		// break;
		//
		// case PdfDictionary.Width:
		// Width=value;
		// break;

			default:
				super.setIntNumber(id, value);
		}
	}

	@Override
	public int getInt(int id) {

		switch (id) {

		// case PdfDictionary.FormType:
		// return FormType;
		//
		// case PdfDictionary.Height:
		// return Height;
		//
		// case PdfDictionary.Width:
		// return Width;

			default:
				return super.getInt(id);
		}
	}

	@Override
	public void setDictionary(int id, PdfObject value) {

		value.setID(id);

		switch (id) {

			case PdfDictionary.Dests:
				this.Dests = value;
				break;

			case PdfDictionary.JavaScript:
				this.JavaScript = value;
				break;

			case PdfDictionary.JS:
				this.JS = value;
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

				// System.out.println((char)next);
				next = next - 48;

				id = id + ((next) << x);

				x = x + 8;
			}

			/**
			 * not standard
			 */
			switch (id) {

			// case StandardFonts.CIDTYPE0:
			// PDFvalue =StandardFonts.CIDTYPE0;
			// break;

				default:

					// if(pdfKeyType==PdfDictionary.Encoding){
					// PDFvalue=PdfCIDEncodings.getConstant(id);
					//
					// if(PDFvalue==PdfDictionary.Unknown){
					//
					// byte[] bytes=new byte[keyLength];
					//
					// System.arraycopy(raw,keyStart,bytes,0,keyLength);
					//
					// unknownValue=new String(bytes);
					// }
					//
					// if(debug && PDFvalue==PdfDictionary.Unknown){
					// System.out.println("Value not in PdfCIDEncodings");
					//
					// byte[] bytes=new byte[keyLength];
					//
					// System.arraycopy(raw,keyStart,bytes,0,keyLength);
					// System.out.println("Add to CIDEncodings and as String");
					// System.out.println("key="+new String(bytes)+" "+id+" not implemented in setConstant in PdfFont Object");
					//
					// System.out.println("final public static int CMAP_"+new String(bytes)+"="+id+";");
					// }
					// }else
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

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		// System.out.println(pdfKeyType+"="+PDFvalue);
		switch (pdfKeyType) {

			default:
				super.setConstant(pdfKeyType, id);

		}

		return PDFvalue;
	}

	@Override
	public int getParameterConstant(int key) {

		// System.out.println("Get constant for "+key +" "+this);
		switch (key) {

		// case PdfDictionary.BaseEncoding:
		//
		// //special cases first
		// if(key==PdfDictionary.BaseEncoding && Encoding!=null && Encoding.isZapfDingbats)
		// return StandardFonts.ZAPF;
		// else if(key==PdfDictionary.BaseEncoding && Encoding!=null && Encoding.isSymbol)
		// return StandardFonts.SYMBOL;
		// else
		// return BaseEncoding;
			default:
				return super.getParameterConstant(key);

		}
	}

	// public void setStream(){
	//
	// hasStream=true;
	// }

	@Override
	public PdfArrayIterator getMixedArray(int id) {

		switch (id) {

			case PdfDictionary.Names:
				return new PdfArrayIterator(this.Names);

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

			case PdfDictionary.Names:
				this.Names = value;
				break;

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

		// case PdfDictionary.Matrix:
		// Matrix=value;
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
		//
			case PdfDictionary.JS:
				this.rawJS = value;
				break;

			default:
				super.setTextStreamValue(id, value);

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
	public String getTextStreamValue(int id) {

		switch (id) {

		// case PdfDictionary.CharSet:
		//
		// //setup first time
		// if(CharSet==null && rawCharSet!=null)
		// CharSet=new String(rawCharSet);
		//
		// return CharSet;

			case PdfDictionary.JS:

				// setup first time
				if (this.JSString == null && this.rawJS != null) {
					// JSString= PdfReader.getTextString(rawJS);
					this.JSString = new String(this.rawJS);
				}
				return this.JSString;

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

			case PdfDictionary.Kids:
				return deepCopy(this.Kids);

			default:
				return super.getKeyArray(id);
		}
	}

	@Override
	public void setKeyArray(int id, byte[][] value) {

		switch (id) {

			case PdfDictionary.Kids:
				this.Kids = value;
				break;

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
		return PdfDictionary.Names;
	}
}