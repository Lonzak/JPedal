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
 * DeviceGrayColorSpace.java
 * ---------------
 */
package org.jpedal.color;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.jpedal.objects.raw.MaskObject;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

/**
 * handle GrayColorSpace
 */
public class DeviceGrayColorSpace extends GenericColorSpace {

	private static final long serialVersionUID = -8160089076145994695L;

	public DeviceGrayColorSpace() {
		this.value = ColorSpaces.DeviceGray;
		this.cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
	}

	/**
	 * set color (in terms of rgb)
	 */
	@Override
	final public void setColor(String[] number_values, int opCount) {

		float[] colValues = new float[1];
		colValues[0] = Float.parseFloat(number_values[0]);

		setColor(colValues, 1);
	}

	/** set color from grayscale values */
	@Override
	final public void setColor(float[] operand, int length) {

		int val;
		float tmp = operand[0];

		// handle float or int
		if (tmp <= 1) val = (int) (255 * tmp);
		else val = (int) (tmp);

		// allow for bum values
		if (val < 0) val = 0;

		this.currentColor = new PdfColor(val, val, val);
	}

	/**
	 * convert Index to RGB
	 */
	@Override
	public byte[] convertIndexToRGB(byte[] index) {

		this.isConverted = true;

		int count = index.length;
		byte[] newIndex = new byte[count * 3];

		for (int i = 0; i < count; i++) {
			byte value = index[i];
			for (int j = 0; j < 3; j++)
				newIndex[(i * 3) + j] = value;

		}

		return newIndex;
	}

	/**
	 * convert data stream to srgb image
	 */
	@Override
	public BufferedImage JPEGToRGBImage(byte[] data, int ww, int hh, float[] decodeArray, int pX, int pY, boolean arrayInverted, PdfObject XObject) {

		// not appropriate for MaskObject case so use super version
		if (XObject != null && XObject instanceof MaskObject) {
			return super.JPEGToRGBImage(data, ww, hh, decodeArray, pX, pY, arrayInverted, XObject);
		}

		BufferedImage image;
		ByteArrayInputStream in = null;

		ImageReader iir = null;
		ImageInputStream iin = null;

		try {

			// read the image data
			in = new ByteArrayInputStream(data);

			// suggestion from Carol
			Iterator iterator = ImageIO.getImageReadersByFormatName("JPEG");

			while (iterator.hasNext()) {
				Object o = iterator.next();
				iir = (ImageReader) o;
				if (iir.canReadRaster()) break;
			}

			ImageIO.setUseCache(false);
			iin = ImageIO.createImageInputStream((in));
			iir.setInput(iin, true);
			Raster ras = iir.readRaster(0, null);

			ras = cleanupRaster(ras, pX, pY, 1); // note uses 1 not count

			int w = ras.getWidth(), h = ras.getHeight();

			DataBufferByte rgb = (DataBufferByte) ras.getDataBuffer();
			byte[] rawData = rgb.getData();

			int byteLength = rawData.length;
			byte[] rgbData = new byte[byteLength * 3];
			int ptr = 0;
			for (int ii = 0; ii < byteLength; ii++) {

				if (arrayInverted) { // flip if needed
					rawData[ii] = (byte) (rawData[ii] ^ 255);
				}

				rgbData[ptr] = rawData[ii];
				ptr++;
				rgbData[ptr] = rawData[ii];
				ptr++;
				rgbData[ptr] = rawData[ii];
				ptr++;

			}

			final int[] bands = { 0, 1, 2 };
			image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Raster raster = Raster.createInterleavedRaster(new DataBufferByte(rgbData, rgbData.length), w, h, w * 3, 3, bands, null);

			image.setData(raster);

		}
		catch (Exception ee) {
			image = null;

			if (LogWriter.isOutput()) LogWriter.writeLog("Couldn't read JPEG, not even raster: " + ee);
		}

		try {
			in.close();
			iir.dispose();
			iin.close();
		}
		catch (Exception ee) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Problem closing  " + ee);
		}

		return image;
	}

}
