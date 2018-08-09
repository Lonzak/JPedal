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
 * CalRGBColorSpace.java
 * ---------------
 */
package org.jpedal.color;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

import org.jpedal.utils.LogWriter;

/**
 * handle CalRGBColorSpace in PDF
 */
public class CalRGBColorSpace extends GenericColorSpace {

	private static final long serialVersionUID = 4569336292751894930L;

	private int r, g, b;

	private static final double[][] xyzrgb = { { 3.240449, -1.537136, -0.498531 }, { -0.969265, 1.876011, 0.041556 },
			{ 0.055643, -0.204026, 1.057229 } };

	/** cache for values to stop recalculation */
	private float lastC = -255, lastI = -255, lastE = -255;

	public CalRGBColorSpace(float[] whitepoint, float[] blackpoint, float[] matrix, float[] gamma) {

		this.cs = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);

		setCIEValues(whitepoint, blackpoint, null, matrix, gamma);
		this.value = ColorSpaces.CalRGB;
	}

	/**
	 * convert to RGB and return as an image
	 */
	@Override
	final public BufferedImage dataToRGB(byte[] data, int width, int height) {

		BufferedImage image;
		DataBuffer db = new DataBufferByte(data, data.length);
		int size = width * height;

		try {

			for (int i = 0; i < size * 3; i = i + 3) { // convert all values to rgb

				float cl = data[i] & 255;
				float ca = data[i + 1] & 255;
				float cb = data[i + 2] & 255;

				convertToRGB(cl, ca, cb);

				db.setElem(i, this.r);
				db.setElem(i + 1, this.g);
				db.setElem(i + 2, this.b);

			}

			int[] bands = { 0, 1, 2 };
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
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
	 * set CalRGB color (in terms of rgb)
	 */
	@Override
	final public void setColor(String[] number_values, int items) {

		float[] colValues = new float[items];

		for (int ii = 0; ii < items; ii++)
			colValues[ii] = Float.parseFloat(number_values[ii]);

		setColor(colValues, items);
	}

	/**
	 * reset any defaults if reused
	 */
	@Override
	final public void reset() {

		this.lastC = -255;
		this.lastI = -255;
		this.lastE = -255;

		this.r = 0;
		this.g = 0;
		this.b = 0;

		this.currentColor = new PdfColor(0, 0, 0);
	}

	/**
	 * set CalRGB color (in terms of rgb)
	 */
	@Override
	final public void setColor(float[] number_values, int items) {

		// get values (and allow for mapped from separation where only 1 value
		float[] A = { 1.0f, 1.0f, 1.0f };

		// allow for use as alt colorspace which only has one value
		if (items == 3) {

			for (int i = 0; i < items; i++) {
				A[i] = number_values[i];
				if (A[i] > 1) return;
			}

			convertToRGB(A[0], A[1], A[2]);

			this.currentColor = new PdfColor(this.r, this.g, this.b);

			// allow for indexed value where 1 values points to 3 in table
		}
		else
			if (items == 1 && this.getIndexedMap() != null) {

				int ptr = (int) (number_values[0] * 3);
				byte[] cmap = getIndexedMap();

				A[0] = ((cmap[ptr] & 255)) / 255f;
				A[1] = ((cmap[ptr + 1] & 255)) / 255f;
				A[2] = ((cmap[ptr + 2] & 255)) / 255f;

				convertToRGB(A[0], A[1], A[2]);

				this.currentColor = new PdfColor(this.r, this.g, this.b);

			}
	}

	final private void convertToRGB(float C, float I, float E) {// ], boolean convert) {

		if ((this.lastC == C) && (this.lastI == I) && (this.lastE == E)) {}
		else {

			// thanks Leonard for the formula
			double ag = Math.pow(C, this.G[0]);
			double bg = Math.pow(I, this.G[1]);
			double cg = Math.pow(E, this.G[2]);

			double X = this.Ma[0] * ag + this.Ma[3] * bg + this.Ma[6] * cg;
			double Y = this.Ma[1] * ag + this.Ma[4] * bg + this.Ma[7] * cg;
			double Z = this.Ma[2] * ag + this.Ma[5] * bg + this.Ma[8] * cg;

			// convert XYZ to RGB, including gamut mapping and gamma correction
			double rawR = xyzrgb[0][0] * X + xyzrgb[0][1] * Y + xyzrgb[0][2] * Z;
			double rawG = xyzrgb[1][0] * X + xyzrgb[1][1] * Y + xyzrgb[1][2] * Z;
			double rawB = xyzrgb[2][0] * X + xyzrgb[2][1] * Y + xyzrgb[2][2] * Z;

			// compute the white point adjustment
			double kr = 1 / ((xyzrgb[0][0] * this.W[0]) + (xyzrgb[0][1] * this.W[1]) + (xyzrgb[0][2] * this.W[2]));
			double kg = 1 / ((xyzrgb[1][0] * this.W[0]) + (xyzrgb[1][1] * this.W[1]) + (xyzrgb[1][2] * this.W[2]));
			double kb = 1 / ((xyzrgb[2][0] * this.W[0]) + (xyzrgb[2][1] * this.W[1]) + (xyzrgb[2][2] * this.W[2]));

			// compute final values based on
			this.r = (int) (255 * Math.pow(clip(rawR * kr), 0.5));
			this.g = (int) (255 * Math.pow(clip(rawG * kg), 0.5));
			this.b = (int) (255 * Math.pow(clip(rawB * kb), 0.5));

			// calcuate using Tristimulus values
			// r = (int) (C * 255);
			// g = (int) (I * 255);
			// b = (int) (E * 255);

			// cache for later
			this.lastC = C;
			this.lastI = I;
			this.lastE = E;
		}
	}

	private static double clip(double v) {
		if (v > 1) return 1d;
		else return v;
	}

}
