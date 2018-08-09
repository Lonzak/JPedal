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
 * EncryptionObject.java
 * ---------------
 */
package org.jpedal.objects.raw;

import org.jpedal.utils.LogWriter;

public class EncryptionObject extends PdfObject {

	// unknown CMAP as String
	// String unknownValue=null;

	// private float[] Matrix;

	boolean EncryptMetadata = true;

	int V = 1; // default value

	int R = -1, P = -1;

	byte[] rawPerms, rawU, rawUE, rawO, rawOE, rawCFM, rawEFF, rawStrF, rawStmF;
	String U, UE, O, OE, EFF, CFM, Perms, StrF = null, StmF = null;

	private PdfObject CF = null;
	private byte[][] Recipients = null;

	public EncryptionObject(String ref) {
		super(ref);
	}

	public EncryptionObject(int ref, int gen) {
		super(ref, gen);
	}

	public EncryptionObject(int type) {
		super(type);
	}

	@Override
	public boolean getBoolean(int id) {

		switch (id) {

			case PdfDictionary.EncryptMetadata:
				return this.EncryptMetadata;

			default:
				return super.getBoolean(id);
		}
	}

	@Override
	public void setBoolean(int id, boolean value) {

		switch (id) {

			case PdfDictionary.EncryptMetadata:
				this.EncryptMetadata = value;
				break;

			default:
				super.setBoolean(id, value);
		}
	}

	@Override
	public PdfObject getDictionary(int id) {

		switch (id) {

			case PdfDictionary.CF:
				return this.CF;

				// case PdfDictionary.XObject:
				// return XObject;

			default:
				return super.getDictionary(id);
		}
	}

	@Override
	public void setIntNumber(int id, int value) {

		switch (id) {

			case PdfDictionary.P:
				this.P = value;
				break;

			case PdfDictionary.R:
				this.R = value;
				break;

			case PdfDictionary.V:
				this.V = value;
				break;

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

			case PdfDictionary.P:
				return this.P;

			case PdfDictionary.R:
				return this.R;

			case PdfDictionary.V:
				return this.V;

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

			case PdfDictionary.CF:
				this.CF = value;
				break;

			// case PdfDictionary.XObject:
			// XObject=value;
			// break;

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

			case PdfDictionary.CFM:
				this.rawCFM = value;
				break;

			case PdfDictionary.EFF:
				this.rawEFF = value;
				break;

			case PdfDictionary.StmF:
				this.rawStmF = value;
				break;

			case PdfDictionary.StrF:
				this.rawStrF = value;
				break;

			default:
				super.setName(id, value);

		}
	}

	@Override
	public void setTextStreamValue(int id, byte[] value) {

		switch (id) {

			case PdfDictionary.O:
				this.rawO = value;
				break;

			case PdfDictionary.OE:
				this.rawOE = value;
				break;

			case PdfDictionary.Perms:
				this.rawPerms = value;
				break;

			case PdfDictionary.U:
				this.rawU = value;
				break;

			case PdfDictionary.UE:
				this.rawUE = value;
				break;

			default:
				super.setTextStreamValue(id, value);

		}
	}

	@Override
	public String getName(int id) {

		switch (id) {

			case PdfDictionary.CFM:

				// setup first time
				if (this.CFM == null && this.rawCFM != null) this.CFM = new String(this.rawCFM);

				return this.CFM;

			case PdfDictionary.EFF:

				// setup first time
				if (this.EFF == null && this.rawEFF != null) this.EFF = new String(this.rawEFF);

				return this.EFF;

			case PdfDictionary.StmF:

				// setup first time
				if (this.StmF == null && this.rawStmF != null) this.StmF = new String(this.rawStmF);

				return this.StmF;

			case PdfDictionary.StrF:

				// setup first time
				if (this.StrF == null && this.rawStrF != null) this.StrF = new String(this.rawStrF);

				return this.StrF;

			default:
				return super.getName(id);

		}
	}

	@Override
	public String getTextStreamValue(int id) {

		switch (id) {

			case PdfDictionary.O:

				// setup first time
				if (this.O == null && this.rawO != null) this.O = new String(this.rawO);

				return this.O;

			case PdfDictionary.OE:

				// setup first time
				if (this.OE == null && this.rawOE != null) this.OE = new String(this.rawOE);

				return this.OE;

			case PdfDictionary.U:

				// setup first time
				if (this.U == null && this.rawU != null) this.U = new String(this.rawU);

				return this.U;

			case PdfDictionary.UE:

				// setup first time
				if (this.UE == null && this.rawUE != null) this.UE = new String(this.rawUE);

				return this.UE;

			default:
				return super.getTextStreamValue(id);

		}
	}

	@Override
	public byte[] getTextStreamValueAsByte(int id) {

		switch (id) {

			case PdfDictionary.O:

				return this.rawO;

			case PdfDictionary.OE:

				return this.rawOE;

			case PdfDictionary.Perms:

				return this.rawPerms;

			case PdfDictionary.U:

				// setup first time
				if (this.U == null && this.rawU != null) this.U = new String(this.rawU);

				return this.rawU;

			case PdfDictionary.UE:

				// setup first time
				if (this.UE == null && this.rawUE != null) this.UE = new String(this.rawUE);

				return this.rawUE;

			default:
				return super.getTextStreamValueAsByte(id);

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
				throw new RuntimeException("Value not defined in getStringValue(int,mode) in " + this);
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
	public byte[][] getStringArray(int id) {

		switch (id) {

			case PdfDictionary.Recipients:
				return deepCopy(this.Recipients);

			default:
				return super.getStringArray(id);
		}
	}

	@Override
	public void setStringArray(int id, byte[][] value) {

		switch (id) {

			case PdfDictionary.Recipients:
				this.Recipients = value;
				break;

			default:
				super.setStringArray(id, value);
		}
	}

	@Override
	public boolean decompressStreamWhenRead() {
		return false;
	}

	@Override
	public int getObjectType() {
		return PdfDictionary.Encrypt;
	}
}