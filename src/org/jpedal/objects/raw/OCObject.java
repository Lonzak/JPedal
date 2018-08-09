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
 * OCObject.java
 * ---------------
 */
package org.jpedal.objects.raw;

import org.jpedal.utils.LogWriter;

public class OCObject extends PdfObject {

	// unknown CMAP as String
	// String unknownValue=null;

	// private float[] Matrix;

	// boolean ImageMask=false;

	float max, min;

	int Event = -1;

	private byte[] rawBaseState, rawListMode;
	String BaseState, ListMode;

	private PdfObject D = null, Layer = null, Usage = null, Zoom = null;

	private Object[] Order;
	private byte[][] AS, Category, Locked, ON, OFF, OCGs, Configs, RBGroups;

	public OCObject(String ref) {
		super(ref);
	}

	public OCObject(int ref, int gen) {
		super(ref, gen);
	}

	public OCObject(int type) {
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

			case PdfDictionary.D:
				return this.D;

			case PdfDictionary.Layer:
				return this.Layer;

			case PdfDictionary.Usage:
				return this.Usage;

			case PdfDictionary.Zoom:
				return this.Zoom;

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
	public void setFloatNumber(int id, float value) {

		switch (id) {

			case PdfDictionary.max:
				this.max = value;
				break;

			case PdfDictionary.min:
				this.min = value;
				break;

			default:

				super.setFloatNumber(id, value);
		}
	}

	@Override
	public float getFloatNumber(int id) {

		switch (id) {

			case PdfDictionary.max:
				return this.max;

			case PdfDictionary.min:
				return this.min;

			default:

				return super.getFloatNumber(id);
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

			case PdfDictionary.D:
				this.D = value;
				break;

			case PdfDictionary.Layer:
				this.Layer = value;
				break;

			case PdfDictionary.Usage:
				this.Usage = value;
				break;

			case PdfDictionary.Zoom:
				this.Zoom = value;
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
					//
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

			case PdfDictionary.Event:
				this.Event = PDFvalue;
				break;

			default:
				super.setConstant(pdfKeyType, id);

		}

		return PDFvalue;
	}

	@Override
	public int getParameterConstant(int key) {

		// System.out.println("Get constant for "+key +" "+this);
		switch (key) {

			case PdfDictionary.Event:
				return this.Event;

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

			case PdfDictionary.ListMode:
				this.rawListMode = value;
				break;

			default:
				super.setName(id, value);

		}
	}

	@Override
	public void setTextStreamValue(int id, byte[] value) {

		switch (id) {

			default:
				super.setTextStreamValue(id, value);

		}
	}

	// return as constnt we can check
	@Override
	public int getNameAsConstant(int id) {

		byte[] raw;

		switch (id) {

			case PdfDictionary.BaseState:
				raw = this.rawBaseState;
				break;

			case PdfDictionary.ListMode:
				raw = this.rawListMode;
				break;

			default:
				return super.getNameAsConstant(id);

		}

		if (raw == null) return super.getNameAsConstant(id);
		else return PdfDictionary.generateChecksum(0, raw.length, raw);
	}

	@Override
	public String getName(int id) {

		switch (id) {

			case PdfDictionary.BaseState:

				// setup first time
				if (this.BaseState == null && this.rawBaseState != null) this.BaseState = new String(this.rawBaseState);

				return this.BaseState;

			case PdfDictionary.ListMode:

				// setup first time
				if (this.ListMode == null && this.rawListMode != null) this.ListMode = new String(this.rawListMode);

				return this.ListMode;

			default:
				return super.getName(id);

		}
	}

	@Override
	public String getTextStreamValue(int id) {

		switch (id) {

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

			case PdfDictionary.AS:
				return this.AS;

			case PdfDictionary.Category:
				return this.Category;

			case PdfDictionary.Configs:
				return this.Configs;

			case PdfDictionary.Locked:
				return this.Locked;

			case PdfDictionary.OCGs:
				return this.OCGs;

			case PdfDictionary.OFF:
				return this.OFF;

			case PdfDictionary.ON:
				return this.ON;

			case PdfDictionary.RBGroups:
				return this.RBGroups;

			default:
				return super.getKeyArray(id);
		}
	}

	@Override
	public void setObjectArray(int id, Object[] objectValues) {

		switch (id) {

			case PdfDictionary.Order:
				this.Order = objectValues;
				break;

			default:
				super.setObjectArray(id, objectValues);
				break;
		}
	}

	@Override
	public Object[] getObjectArray(int id) {

		switch (id) {

			case PdfDictionary.Order:
				return deepCopy(this.Order);

			default:
				return super.getObjectArray(id);
		}
	}

	protected static Object[] deepCopy(Object[] input) {

		if (input == null) return null;

		int count = input.length;

		Object[] deepCopy = new Object[count];

		for (int aa = 0; aa < count; aa++) {

			if (input[aa] instanceof byte[]) {
				byte[] byteVal = (byte[]) input[aa];
				int byteCount = byteVal.length;

				byte[] newValue = new byte[byteCount];
				deepCopy[aa] = newValue;

				System.arraycopy(byteVal, 0, newValue, 0, byteCount);
			}
			else {
				deepCopy[aa] = deepCopy((Object[]) input[aa]);
			}
		}

		return deepCopy;
	}

	@Override
	public void setKeyArray(int id, byte[][] value) {

		switch (id) {

			case PdfDictionary.AS:
				this.AS = value;
				break;

			case PdfDictionary.Category:
				this.Category = value;
				break;

			case PdfDictionary.Configs:
				this.Configs = value;
				break;

			case PdfDictionary.Locked:
				this.Locked = value;
				break;

			case PdfDictionary.OCGs:
				this.OCGs = value;
				break;

			case PdfDictionary.OFF:
				this.OFF = value;
				break;

			case PdfDictionary.ON:
				this.ON = value;
				break;

			case PdfDictionary.RBGroups:
				this.RBGroups = value;
				break;

			default:
				super.setKeyArray(id, value);
		}
	}

	@Override
	public boolean decompressStreamWhenRead() {
		return false;
	}

	@Override
	public int getObjectType() {
		return PdfDictionary.OCProperties;
	}
}