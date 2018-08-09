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
 * JAITiffHelper.java
 * ---------------
 */
package org.jpedal.io;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import org.jpedal.utils.LogWriter;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;

public class JAITiffHelper {

	private ImageDecoder dec;

	private int pageCount = 0;

	/**
	 * setup access to Tif file and also read page count
	 */
	public JAITiffHelper(String file) {

		try {
			// Get file info
			File imgFile = new File(file);
			FileSeekableStream s = new FileSeekableStream(imgFile);
			this.dec = ImageCodec.createImageDecoder("tiff", s, null);
			this.pageCount = this.dec.getNumPages();

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	public int getTiffPageCount() {
		return this.pageCount;
	}

	public BufferedImage getImage(int tiffImageToLoad) {

		BufferedImage img = null;

		try {

			RenderedImage op = new javax.media.jai.NullOpImage(this.dec.decodeAsRenderedImage(tiffImageToLoad), null, null,
					javax.media.jai.OpImage.OP_IO_BOUND);

			img = (javax.media.jai.JAI.create("affine", op, null, new javax.media.jai.InterpolationBicubic(1))).getAsBufferedImage();

			/** change to grey as default */
			img = ColorSpaceConvertor.convertColorspace(img, BufferedImage.TYPE_BYTE_GRAY);

		}
		catch (IOException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		return img;
	}
}
