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
 * CCITT.java
 * ---------------
 */
package org.jpedal.io.filter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.util.Map;

import org.jpedal.io.JAIHelper;
import org.jpedal.io.filter.ccitt.CCITT2D;
import org.jpedal.io.filter.ccitt.CCITTMix;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;

/**
 * CCITT
 */
public class CCITT extends BaseFilter implements PdfFilter {

	private final int width;
	private final int height;

	public CCITT(PdfObject decodeParms, int width, int height) {

		super(decodeParms);

		this.width = width;
		this.height = height;

		// check JAI loaded on first call
		JAIHelper.confirmJAIOnClasspath();
	}

	@Override
	public byte[] decode(byte[] data) throws Exception {

		/**
		 * if NOT byte aligned try new tiff decoder using JAI first fixes several bugs in old code -note it has some new bugs missing in the old code
		 * :-(
		 */

		// flag set to ensure that only new code gets execued
		data = decodeCCITT(data);

		return data;
	}

	@Override
	public void decode(BufferedInputStream bis, BufferedOutputStream streamCache, String cacheName, Map cachedObjects) throws Exception {

		int size = bis.available();
		byte[] data = new byte[size];
		bis.read(data);
		data = decodeCCITT(data);

		streamCache.write(data);
	}

	private byte[] decodeCCITT(byte[] rawData) throws Exception {

		byte[] data;

		int K = this.decodeParms.getInt(PdfDictionary.K);

		// new CCITT decoder - encodes runs of black or white pixels
		// always assumes white to start
		org.jpedal.io.filter.ccitt.CCITTDecoder ccitt = null;

		if (K == 0) {

			// Pure 1D decoding, group3
			ccitt = new org.jpedal.io.filter.ccitt.CCITT1D(rawData, this.width, this.height, this.decodeParms);

			// K<0 case
			// Pure 2D, group 4
		}
		else
			if (K < 0) {

				ccitt = new CCITT2D(rawData, this.width, this.height, this.decodeParms);

			}
			else
				if (K > 0) {
					// Mixed 1/2 D encoding we can use either for maximum compression
					// A 1D line can be followed by up to K-1 2D lines

					ccitt = new CCITTMix(rawData, this.width, this.height, this.decodeParms);
				}

		data = ccitt.decode();

		return data;
	}
}
