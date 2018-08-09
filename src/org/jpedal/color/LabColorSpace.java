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
 * LabColorSpace.java
 * ---------------
 */
package org.jpedal.color;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

/**
 * handle LabColorSpace
 */
public class LabColorSpace extends GenericColorSpace {

	private static final long serialVersionUID = 9204166205367322647L;
	private int r, g, b;
	private float lastL = -1, lastA = 65536, lastBstar;

	/** holds values we have already calculated to speed-up */
	private Map cache = new HashMap();

	private static final float C1 = 108f / 841f;

	private static final float C2 = 4f / 29f;

	private static final float C3 = 6f / 29f;

	private static final float C4 = 100f / 255f;

	private static final float C5 = 128f;

	public LabColorSpace(float[] whitepoint, float[] blackpoint, float[] range) {

		this.value = ColorSpaces.Lab;
		setCIEValues(whitepoint, blackpoint, range, null, null);
	}

	/**
	 * convert Index to RGB
	 */
	@Override
	public byte[] convertIndexToRGB(byte[] index) {

		this.isConverted = true;

		int size = index.length;
		float cl, ca, cb;

		for (int i = 0; i < size; i = i + 3) { // convert all values to rgb

			cl = (index[i] & 255) * C4;

			ca = (index[i + 1] & 255) - C5;

			cb = (index[i + 2] & 255) - C5;

			convertToRGB(cl, ca, cb);

			index[i] = (byte) this.r;
			index[i + 1] = (byte) this.g;
			index[i + 2] = (byte) this.b;

		}

		return index;
	}

	/**
	 * <p>
	 * Convert DCT encoded image bytestream to sRGB
	 * </p>
	 * <p>
	 * It uses the internal Java classes and the Adobe icm to convert CMYK and YCbCr-Alpha - the data is still DCT encoded.
	 * </p>
	 * <p>
	 * The Sun class JPEGDecodeParam.java is worth examining because it contains lots of interesting comments
	 * </p>
	 * <p>
	 * I tried just using the new IOImage.read() but on type 3 images, all my clipping code stopped working so I am still using 1.3
	 * </p>
	 */
	@Override
	public BufferedImage JPEGToRGBImage(byte[] data, int w, int h, float[] decodeArray, int pX, int pY, boolean arrayInverted, PdfObject XObject) {

		BufferedImage image;
		// ByteArrayInputStream in = null;

		try {

			Raster ras = this.images.readRasterFromJPeg(data);

			ras = cleanupRaster(ras, pX, pY, 3);

			final int width = ras.getWidth();
			final int height = ras.getHeight();
			final int imgSize = width * height;
			byte[] iData = ((DataBufferByte) ras.getDataBuffer()).getData();

			for (int i = 0; i < imgSize * 3; i = i + 3) { // convert all values to rgb

				float cl = (iData[i] & 255) * C4;
				float ca = (iData[i + 1] & 255) - C5;
				float cb = (iData[i + 2] & 255) - C5;

				convertToRGB(cl, ca, cb);

				iData[i] = (byte) this.r;
				iData[i + 1] = (byte) this.g;
				iData[i + 2] = (byte) this.b;

			}

			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			DataBuffer db = new DataBufferByte(iData, iData.length);
			int[] bands = { 0, 1, 2 };
			Raster raster = Raster.createInterleavedRaster(db, width, height, width * 3, 3, bands, null);

			image.setData(raster);

		}
		catch (Exception ee) {
			image = null;

			if (LogWriter.isOutput()) LogWriter.writeLog("Couldn't read JPEG, not even raster: " + ee);
		}

		return image;
	}

	/**
	 * convert LAB stream to RGB and return as an image
	 */
	@Override
	public BufferedImage dataToRGB(byte[] data, int width, int height) {

		BufferedImage image;
		final int imgSize = width * height;

		try {

			for (int i = 0; i < imgSize * 3; i = i + 3) { // convert all values to rgb

				float cl = (data[i] & 255) * C4;
				float ca = (data[i + 1] & 255) - C5;
				float cb = (data[i + 2] & 255) - C5;

				convertToRGB(cl, ca, cb);

				data[i] = (byte) this.r;
				data[i + 1] = (byte) this.g;
				data[i + 2] = (byte) this.b;

			}

			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Raster raster = ColorSpaceConvertor.createInterleavedRaster(data, width, height);
			image.setData(raster);

		}
		catch (Exception ee) {
			image = null;

			if (LogWriter.isOutput()) LogWriter.writeLog("Couldn't read JPEG, not even raster: " + ee);
		}

		return image;
	}

	/** convert numbers to rgb values */
	private void convertToRGB(float l, float a, float bstar) {

		// make sure in range
		if (l < 0) l = 0;
		else
			if (l > 100) l = 100;

		if (a < this.R[0]) a = this.R[0];
		else
			if (a > this.R[1]) a = this.R[1];

		if (bstar < this.R[2]) bstar = this.R[2];
		else
			if (bstar > this.R[3]) bstar = this.R[3];

		/** don't bother if already calculated */
		if ((this.lastL == l) && (this.lastA == a) && (this.lastBstar == bstar)) {}
		else {

			// set indices
			int indexL = (int) l;
			int indexA = (int) (a - this.R[0]);
			int indexB = (int) (bstar - this.R[2]);

			Integer key = (indexL << 16) + (indexA << 8) + indexB;

			Object value = this.cache.get(key);
			/** used cache value or recalulate */
			if (value != null) {

				int raw = (Integer) value;
				this.r = ((raw >> 16) & 255);
				this.g = ((raw >> 8) & 255);
				this.b = ((raw) & 255);

			}
			else {

				double val1 = (l + 16d) / 116d;
				double[] vals = new double[3];

				vals[0] = val1 + (a / 500d);
				vals[1] = val1;
				vals[2] = val1 - (bstar / 200d);

				float[] out = new float[3];
				for (int j = 0; j < 3; j++) {
					if (vals[j] >= C3) out[j] = (float) (this.W[j] * vals[j] * vals[j] * vals[j]);
					else out[j] = (float) (this.W[j] * C1 * (vals[j] - C2));

					// avoid negative numbers
					// seem to give very odd results
					if (out[j] < 0) out[j] = 0;
				}

				// convert to rgb
				out = this.cs.toRGB(out);

				// put values into array
				this.r = (int) (out[0] * 255);
				this.g = (int) (out[1] * 255);
				this.b = (int) (out[2] * 255);

				// check in range
				if (this.r < 0) this.r = 0;
				if (this.g < 0) this.g = 0;
				if (this.b < 0) this.b = 0;

				if (this.r > 255) this.r = 255;
				if (this.g > 255) this.g = 255;
				if (this.b > 255) this.b = 255;

				int raw = (this.r << 16) + (this.g << 8) + this.b;

				// store values in cache
				this.cache.put(key, raw);

			}

			this.lastL = l;
			this.lastA = a;
			this.lastBstar = bstar;

		}
	}

	/**
	 * set color (in terms of rgb)
	 */
	@Override
	final public void setColor(String[] number_values, int items) {

		float[] colValues = new float[items];

		for (int ii = 0; ii < items; ii++)
			colValues[ii] = Float.parseFloat(number_values[ii]);

		setColor(colValues, items);
	}

	/** set color */
	@Override
	final public void setColor(float[] operand, int length) {

		// get raw values
		float l = operand[0];
		float a = operand[1];
		float Bstar = operand[2];

		convertToRGB(l, a, Bstar);

		this.currentColor = new PdfColor(this.r, this.g, this.b);
	}
}
