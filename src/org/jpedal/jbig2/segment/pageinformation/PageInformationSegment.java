/**
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.jpedal.org
 * (C) Copyright 1997-2008, IDRsolutions and Contributors.
 * Main Developer: Simon Barnett
 *
 * 	This file is part of JPedal
 *
 * Copyright (c) 2008, IDRsolutions
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the IDRsolutions nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY IDRsolutions ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL IDRsolutions BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Other JBIG2 image decoding implementations include
 * jbig2dec (http://jbig2dec.sourceforge.net/)
 * xpdf (http://www.foolabs.com/xpdf/)
 * 
 * The final draft JBIG2 specification can be found at http://www.jpeg.org/public/fcd14492.pdf
 * 
 * All three of the above resources were used in the writing of this software, with methodologies,
 * processes and inspiration taken from all three.
 *
 * ---------------
 * PageInformationSegment.java
 * ---------------
 */
package org.jpedal.jbig2.segment.pageinformation;

import java.io.IOException;

import org.jpedal.jbig2.JBIG2Exception;
import org.jpedal.jbig2.decoders.JBIG2StreamDecoder;
import org.jpedal.jbig2.image.JBIG2Bitmap;
import org.jpedal.jbig2.segment.Segment;
import org.jpedal.jbig2.util.BinaryOperation;

public class PageInformationSegment extends Segment {

	private int pageBitmapHeight, pageBitmapWidth;
	private int yResolution, xResolution;

	PageInformationFlags pageInformationFlags = new PageInformationFlags();
	private int pageStriping;

	private JBIG2Bitmap pageBitmap;

	public PageInformationSegment(JBIG2StreamDecoder streamDecoder) {
		super(streamDecoder);
	}

	public PageInformationFlags getPageInformationFlags() {
		return this.pageInformationFlags;
	}

	public JBIG2Bitmap getPageBitmap() {
		return this.pageBitmap;
	}

	@Override
	public void readSegment() throws IOException, JBIG2Exception {

		if (JBIG2StreamDecoder.debug) System.out.println("==== Reading Page Information Dictionary ====");

		short[] buff = new short[4];
		this.decoder.readByte(buff);
		this.pageBitmapWidth = BinaryOperation.getInt32(buff);

		buff = new short[4];
		this.decoder.readByte(buff);
		this.pageBitmapHeight = BinaryOperation.getInt32(buff);

		if (JBIG2StreamDecoder.debug) System.out.println("Bitmap size = " + this.pageBitmapWidth + 'x' + this.pageBitmapHeight);

		buff = new short[4];
		this.decoder.readByte(buff);
		this.xResolution = BinaryOperation.getInt32(buff);

		buff = new short[4];
		this.decoder.readByte(buff);
		this.yResolution = BinaryOperation.getInt32(buff);

		if (JBIG2StreamDecoder.debug) System.out.println("Resolution = " + this.xResolution + 'x' + this.yResolution);

		/** extract page information flags */
		short pageInformationFlagsField = this.decoder.readByte();

		this.pageInformationFlags.setFlags(pageInformationFlagsField);

		if (JBIG2StreamDecoder.debug) System.out.println("symbolDictionaryFlags = " + pageInformationFlagsField);

		buff = new short[2];
		this.decoder.readByte(buff);
		this.pageStriping = BinaryOperation.getInt16(buff);

		if (JBIG2StreamDecoder.debug) System.out.println("Page Striping = " + this.pageStriping);

		int defPix = this.pageInformationFlags.getFlagValue(PageInformationFlags.DEFAULT_PIXEL_VALUE);

		int height;

		if (this.pageBitmapHeight == -1) {
			height = this.pageStriping & 0x7fff;
		}
		else {
			height = this.pageBitmapHeight;
		}

		this.pageBitmap = new JBIG2Bitmap(this.pageBitmapWidth, height, this.arithmeticDecoder, this.huffmanDecoder, this.mmrDecoder);
		this.pageBitmap.clear(defPix);
	}

	public int getPageBitmapHeight() {
		return this.pageBitmapHeight;
	}
}
