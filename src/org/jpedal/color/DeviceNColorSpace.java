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
 * DeviceNColorSpace.java
 * ---------------
 */
package org.jpedal.color;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

/**
 * handle Device ColorSpace
 */
public class DeviceNColorSpace extends SeparationColorSpace {

	private static final long serialVersionUID = -1372268945371555187L;

	public DeviceNColorSpace() {}

	private Map cache = new HashMap();

	public DeviceNColorSpace(PdfObjectReader currentPdfFile, PdfObject colorSpace) {

		this.value = ColorSpaces.DeviceN;

		processColorToken(currentPdfFile, colorSpace);
	}

	/** set color (translate and set in alt colorspace) */
	@Override
	public void setColor(String[] operand, int opCount) {

		float[] values = new float[opCount];
		for (int j = 0; j < opCount; j++)
			values[j] = Float.parseFloat(operand[j]);

		setColor(values, opCount);
	}

	/** set color (translate and set in alt colorspace */
	@Override
	public void setColor(float[] raw, int opCount) {

		int[] lookup = new int[3];

		int opNumbers = raw.length;
		if (opNumbers > 3) opNumbers = 3;

		for (int i = 0; i < opNumbers; i++) {
			lookup[i] = (int) (raw[i] * 255);
		}

		boolean isCached = false;

		if (this.cmykMapping == Black && opCount == 1) { // special case coded in

			float[] newOp = { 0f, 0f, 0f, raw[0] };
			this.altCS.setColor(newOp, newOp.length);

		}
		else
			if (opCount < 4 && this.cache.get((lookup[0] << 16) + (lookup[1] << 8) + lookup[2]) != null) {

				isCached = true;

				Object val = this.cache.get((lookup[0] << 16) + (lookup[1] << 8) + lookup[2]);
				int rawValue = (Integer) val;
				int r = ((rawValue >> 16) & 255);
				int g = ((rawValue >> 8) & 255);
				int b = ((rawValue) & 255);

				this.altCS.currentColor = new PdfColor(r, g, b);

			}
			else
				if (this.cmykMapping == CMYB && opCount == 4) { // special case coded in

					float[] newOp = { raw[0], raw[1], raw[2], raw[3] };
					this.altCS.setColor(newOp, newOp.length);
				}
				else
					if (this.cmykMapping == CMYK && opCount == 6) { // special case coded in

						float[] newOp = { raw[5], raw[4], raw[3], raw[2] };
						this.altCS.setColor(newOp, newOp.length);

					}
					else
						if (this.cmykMapping == MYK && opCount == 3) { // special case coded in

							float[] newOp = { 0.0f, raw[0], raw[1], raw[2] };
							this.altCS.setColor(newOp, newOp.length);

						}
						else
							if (this.cmykMapping == CMY && opCount == 3) { // special case coded in

								float[] newOp = { raw[0], raw[1], raw[2], 0.0f };
								this.altCS.setColor(newOp, newOp.length);

							}
							else
								if (this.cmykMapping == CMK && opCount == 3) { // special case coded in

									float[] newOp = { raw[0], raw[1], 0f, raw[2] };
									this.altCS.setColor(newOp, newOp.length);

								}
								else
									if (this.cmykMapping == CY && opCount == 2) { // special case coded in

										float[] newOp = { raw[0], 0, raw[1], 0 };
										this.altCS.setColor(newOp, newOp.length);

									}
									else
										if (this.cmykMapping == CM && opCount == 2) { // special case coded in

											float[] newOp = { raw[0], raw[1], 0, 0 };
											this.altCS.setColor(newOp, newOp.length);
										}
										else
											if (this.cmykMapping == MY && opCount == 2) { // special case coded in

												float[] newOp = { 0, raw[0], raw[1], 0 };
												this.altCS.setColor(newOp, newOp.length);

											}
											else {

												float[] operand = this.colorMapper.getOperandFloat(raw);
												this.altCS.setColor(operand, operand.length);

											}

		if (!isCached) { // not used except as flag

			this.altCS.getColor().getRGB();
			int rawValue = this.altCS.getColor().getRGB();

			// store values in cache
			this.cache.put((lookup[0] << 16) + (lookup[1] << 8) + lookup[2], rawValue);

		}
	}

	/**
	 * convert separation stream to RGB and return as an image
	 */
	@Override
	public BufferedImage dataToRGB(byte[] data, int w, int h) {

		BufferedImage image;

		try {

			// convert data
			image = createImage(w, h, data);

		}
		catch (Exception ee) {
			image = null;

			if (LogWriter.isOutput()) LogWriter.writeLog("Couldn't convert DeviceN colorspace data: " + ee);
		}

		return image;
	}

	/**
	 * convert data stream to srgb image
	 */
	@Override
	public BufferedImage JPEGToRGBImage(byte[] data, int ww, int hh, float[] decodeArray, int pX, int pY, boolean arrayInverted, PdfObject XObject) {

		BufferedImage image;
		ByteArrayInputStream in = null;

		ImageReader iir = null;
		ImageInputStream iin = null;

		try {

			// read the image data
			in = new ByteArrayInputStream(data);

			try {

				// suggestion from Carol
				Iterator iterator = ImageIO.getImageReadersByFormatName("JPEG");

				while (iterator.hasNext()) {
					Object o = iterator.next();
					iir = (ImageReader) o;
					if (iir.canReadRaster()) break;
				}

			}
			catch (Exception e) {

				if (LogWriter.isOutput()) LogWriter.writeLog("Unable to find JAI jars on classpath");
				return null;
			}

			ImageIO.setUseCache(false);
			iin = ImageIO.createImageInputStream((in));
			iir.setInput(iin, true);
			Raster ras = iir.readRaster(0, null);

			ras = cleanupRaster(ras, pX, pY, this.componentCount);
			int w = ras.getWidth();
			int h = ras.getHeight();

			DataBufferByte rgb = (DataBufferByte) ras.getDataBuffer();

			// convert the image
			image = createImage(w, h, rgb.getData());

		}
		catch (Exception ee) {
			image = null;

			if (LogWriter.isOutput()) LogWriter.writeLog("Couldn't read JPEG, not even raster: " + ee);

			ee.printStackTrace();
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

	/**
	 * turn raw data into an image
	 */
	private BufferedImage createImage(int w, int h, byte[] rawData) {

		BufferedImage image;

		byte[] rgb = new byte[w * h * 3];

		int bytesCount = rawData.length;

		// convert data to RGB format
		int byteCount = rawData.length / this.componentCount;

		float[] values = new float[this.componentCount];

		int j = 0, j2 = 0;

		for (int i = 0; i < byteCount; i++) {

			if (j >= bytesCount) break;

			for (int comp = 0; comp < this.componentCount; comp++) {
				values[comp] = ((rawData[j] & 255) / 255f);
				j++;
			}

			setColor(values, this.componentCount);

			// set values
			int foreground = this.altCS.currentColor.getRGB();

			rgb[j2] = (byte) ((foreground >> 16) & 0xFF);
			rgb[j2 + 1] = (byte) ((foreground >> 8) & 0xFF);
			rgb[j2 + 2] = (byte) ((foreground) & 0xFF);

			j2 = j2 + 3;

		}

		// create the RGB image
		int[] bands = { 0, 1, 2 };
		image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		DataBuffer dataBuf = new DataBufferByte(rgb, rgb.length);
		Raster raster = Raster.createInterleavedRaster(dataBuf, w, h, w * 3, 3, bands, null);
		image.setData(raster);

		return image;
	}
}
