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
 * DecodeParmsObject.java
 * ---------------
 */
package org.jpedal.objects.raw;

import org.jpedal.utils.LogWriter;

public class DecodeParmsObject extends PdfObject {

	boolean EncodedByteAlign = false, EndOfBlock = true, EndOfLine = false, BlackIs1 = false, Uncompressed = false;

	PdfObject JBIG2Globals;

	int Blend = -1, Colors = -1, ColorTransform = 1, Columns = -1, DamagedRowsBeforeError = 0, EarlyChange = 1, K = 0, Predictor = 1, QFactor = -1,
			Rows = -1;

	public DecodeParmsObject(String ref) {
		super(ref);
	}

	public DecodeParmsObject(int ref, int gen) {
		super(ref, gen);
	}

	public DecodeParmsObject(int type) {
		super(type);
	}

	@Override
	public boolean getBoolean(int id) {

		switch (id) {

			case PdfDictionary.BlackIs1:
				return this.BlackIs1;

			case PdfDictionary.EncodedByteAlign:
				return this.EncodedByteAlign;

			case PdfDictionary.EndOfBlock:
				return this.EndOfBlock;

			case PdfDictionary.EndOfLine:
				return this.EndOfLine;

			case PdfDictionary.Uncompressed:
				return this.Uncompressed;

			default:
				return super.getBoolean(id);
		}
	}

	@Override
	public void setBoolean(int id, boolean value) {

		switch (id) {

			case PdfDictionary.BlackIs1:
				this.BlackIs1 = value;
				break;

			case PdfDictionary.EncodedByteAlign:
				this.EncodedByteAlign = value;
				break;

			case PdfDictionary.EndOfBlock:
				this.EndOfBlock = value;
				break;

			case PdfDictionary.EndOfLine:
				this.EndOfLine = value;
				break;

			case PdfDictionary.Uncompressed:
				this.Uncompressed = value;
				break;

			default:
				super.setBoolean(id, value);
		}
	}

	@Override
	public PdfObject getDictionary(int id) {

		switch (id) {

			case PdfDictionary.JBIG2Globals:
				return this.JBIG2Globals;

			default:
				return super.getDictionary(id);
		}
	}

	@Override
	public void setIntNumber(int id, int value) {

		switch (id) {

			case PdfDictionary.Blend:
				this.Blend = value;
				break;

			case PdfDictionary.Colors:
				this.Colors = value;
				break;

			case PdfDictionary.ColorTransform:
				this.ColorTransform = value;
				break;

			case PdfDictionary.Columns:
				this.Columns = value;
				break;

			case PdfDictionary.DamagedRowsBeforeError:
				this.DamagedRowsBeforeError = value;
				break;

			case PdfDictionary.EarlyChange:
				this.EarlyChange = value;
				break;

			case PdfDictionary.K:
				this.K = value;
				break;

			case PdfDictionary.Predictor:
				this.Predictor = value;
				break;

			case PdfDictionary.QFactor:
				this.QFactor = value;
				break;

			case PdfDictionary.Rows:
				this.Rows = value;
				break;

			default:
				super.setIntNumber(id, value);
		}
	}

	@Override
	public int getInt(int id) {

		switch (id) {

			case PdfDictionary.Blend:
				return this.Blend;

			case PdfDictionary.Colors:
				return this.Colors;

			case PdfDictionary.ColorTransform:
				return this.ColorTransform;

			case PdfDictionary.Columns:
				return this.Columns;

			case PdfDictionary.DamagedRowsBeforeError:
				return this.DamagedRowsBeforeError;

			case PdfDictionary.EarlyChange:
				return this.EarlyChange;

			case PdfDictionary.K:
				return this.K;

			case PdfDictionary.Predictor:
				return this.Predictor;

			case PdfDictionary.QFactor:
				return this.QFactor;

			case PdfDictionary.Rows:
				return this.Rows;

			default:
				return super.getInt(id);
		}
	}

	@Override
	public void setDictionary(int id, PdfObject value) {

		value.setID(id);

		switch (id) {

			case PdfDictionary.JBIG2Globals:
				this.JBIG2Globals = value;
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

			// case StandardFonts.CIDTYPE0:
			// PDFvalue =StandardFonts.CIDTYPE0;
			// break;

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

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
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

		int def;

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
	public boolean decompressStreamWhenRead() {
		return true;
	}

}