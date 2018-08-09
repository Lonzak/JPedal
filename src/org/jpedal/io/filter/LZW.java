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
 * LZW.java
 * ---------------
 */
package org.jpedal.io.filter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.sun.LZWDecoder;
import org.jpedal.sun.LZWDecoder2;
import org.jpedal.sun.TIFFLZWDecoder;

/**
 * LZW
 */
public class LZW extends BaseFilter implements PdfFilter {

	// default values
	private int predictor = 1;
	private int EarlyChange = 1;
	private int colors = 1;
	private int bitsPerComponent = 8;
	private int rows;
	private int columns;

	public LZW(PdfObject decodeParms, int width, int height) {

		super(decodeParms);

		this.rows = height;
		this.columns = width;

		if (decodeParms != null) {

			int newBitsPerComponent = decodeParms.getInt(PdfDictionary.BitsPerComponent);
			if (newBitsPerComponent != -1) this.bitsPerComponent = newBitsPerComponent;

			int newColors = decodeParms.getInt(PdfDictionary.Colors);
			if (newColors != -1) this.colors = newColors;

			int columnsSet = decodeParms.getInt(PdfDictionary.Columns);
			if (columnsSet != -1) this.columns = columnsSet;

			this.EarlyChange = decodeParms.getInt(PdfDictionary.EarlyChange);

			this.predictor = decodeParms.getInt(PdfDictionary.Predictor);

			int rowsSet = decodeParms.getInt(PdfDictionary.Rows);
			if (rowsSet != -1) this.rows = rowsSet;

		}
	}

	@Override
	public byte[] decode(byte[] data) throws Exception {

		if (this.rows * this.columns == 1) {

			if (data != null) {
				int bitsPerComponent1 = 8;
				byte[] processed_data = new byte[bitsPerComponent1 * this.rows * ((this.columns + 7) >> 3)]; // will be resized if needed
				// 9allow for not a full 8
				// bits

				TIFFLZWDecoder lzw_decode = new TIFFLZWDecoder(this.columns, this.predictor, bitsPerComponent1);

				lzw_decode.decode(data, processed_data, this.rows);

				return applyPredictor(this.predictor, processed_data, this.colors, bitsPerComponent1, this.columns);
			}
		}
		else { // version for no parameters

			if (data != null) {
				ByteArrayOutputStream processed = new ByteArrayOutputStream();
				LZWDecoder lzw = new LZWDecoder();
				lzw.decode(data, processed, this.EarlyChange == 1);
				processed.close();
				data = processed.toByteArray();
			}

			data = applyPredictor(this.predictor, data, this.colors, this.bitsPerComponent, this.columns);

		}

		return data;
	}

	@Override
	public void decode(BufferedInputStream bis, BufferedOutputStream streamCache, String cacheName, Map cachedObjects) throws Exception {

		if (this.rows * this.columns == 1) {

		}
		else { // version for no parameters

			/**
			 * decompress cached object
			 */
			if (bis != null) {

				LZWDecoder2 lzw2 = new LZWDecoder2();
				lzw2.decode(null, streamCache, bis);

			}

			if (this.predictor != 1 && this.predictor != 10) {
				streamCache.flush();
				streamCache.close();
				if (cacheName != null) setupCachedObjectForDecoding(cacheName);
			}

			applyPredictor(this.predictor, null, this.colors, this.bitsPerComponent, this.columns);

		}
	}
}
