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
 * DeviceCMYKColorSpace.java
 * ---------------
 */
package org.jpedal.color;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.jpedal.exception.PdfException;
import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

/**
 * handle DeviceCMYKColorSpace
 */
public class DeviceCMYKColorSpace extends GenericColorSpace {

	private static final long serialVersionUID = 4054062852632000027L;

	private float lastC = -1, lastM = -1, lastY = -1, lastK = -1;

	public static ColorSpace CMYK = null;

	/**
	 * ensure next setColor will not match with old color as value may be out of sync
	 */
	@Override
	public void clearCache() {
		this.lastC = -1;
	}

	/**
	 * initialise CMYK profile
	 */
	private void initColorspace() {

		/**
		 * load the cmyk profile - I am using the Adobe version from the web. There are lots out there.
		 */

		InputStream stream = null;

		try {

			String profile = System.getProperty("org.jpedal.CMYKprofile");

			if (profile == null) stream = this.getClass().getResourceAsStream("/org/jpedal/res/cmm/cmyk.icm");
			else {
				try {
					stream = new FileInputStream(profile);
				}
				catch (Exception ee) {
					throw new PdfException("PdfException attempting to use user profile " + profile + " Message=" + ee);
				}
			}

			ICC_Profile p = ICC_Profile.getInstance(stream);
			CMYK = new ICC_ColorSpace(p);

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e);

			throw new RuntimeException("Problem setting CMYK Colorspace with message " + e + " Possible cause file cmyk.icm corrupted");
		}
		finally {
			if (stream != null) {
				try {
					stream.close();
				}
				catch (IOException e) {
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e);
				}
			}
		}
	}

	/** setup colorspaces */
	public DeviceCMYKColorSpace() {

		this.componentCount = 4;

		if (CMYK == null) initColorspace();

		this.cs = CMYK;

		this.value = ColorSpaces.DeviceCMYK;
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
	 * convert CMYK to RGB as defined by Adobe (p354 Section 6.2.4 in Adobe 1.3 spec 2nd edition) and set value
	 */
	@Override
	final public void setColor(float[] operand, int length) {

		boolean newVersion = true;

		// default of black
		this.c = 1;
		this.y = 1;
		this.m = 1;
		this.k = 1;

		if (length > 3) {
			// get values
			this.c = operand[0];
			// the cyan
			this.m = operand[1];
			// the magenta
			this.y = operand[2];
			// the yellow
			this.k = operand[3];
		}
		else {
			// get values
			if (length > 0) this.c = operand[0];
			// the cyan
			if (length > 1) this.m = operand[1];
			// the magenta
			if (length > 2) this.y = operand[2];
			// the yellow
			if (length > 3) this.k = operand[3];

		}

		float r, g, b;
		if ((this.lastC == this.c) && (this.lastM == this.m) && (this.lastY == this.y) && (this.lastK == this.k)) {}
		else {

			// store values
			this.rawValues = new float[4];
			this.rawValues[0] = this.c;
			this.rawValues[1] = this.m;
			this.rawValues[2] = this.y;
			this.rawValues[3] = this.k;

			if (!newVersion) {
				// convert the colours the old way
				r = (this.c + this.k);
				if (r > 1) r = 1;
				g = (this.m + this.k);
				if (g > 1) g = 1;
				b = (this.y + this.k);
				if (b > 1) b = 1;

				// set the colour
				this.currentColor = new PdfColor((int) (255 * (1 - r)), (int) (255 * (1 - g)), (int) (255 * (1 - b)));

			}
			else
				if ((this.c == 0) && (this.y == 0) && (this.m == 0) && (this.k == 0)) {
					this.currentColor = new PdfColor(1.0f, 1.0f, 1.0f);

				}
				else {
					if (this.c > .99) this.c = 1.0f;
					else
						if (this.c < 0.01) this.c = 0.0f;
					if (this.m > .99) this.m = 1.0f;
					else
						if (this.m < 0.01) this.m = 0.0f;
					if (this.y > .99) this.y = 1.0f;
					else
						if (this.y < 0.01) this.y = 0.0f;
					if (this.k > .99) this.k = 1.0f;
					else
						if (this.k < 0.01) this.k = 0.0f;

					// we store values to speedup operation
					float[] rgb = null;

					if (rgb == null) {
						float[] cmykValues = { this.c, this.m, this.y, this.k };
						rgb = CMYK.toRGB(cmykValues);

						// check rounding
						for (int jj = 0; jj < 3; jj++) {
							if (rgb[jj] > .99) rgb[jj] = 1.0f;
							else
								if (rgb[jj] < 0.01) rgb[jj] = 0.0f;
						}
					}
					this.currentColor = new PdfColor(rgb[0], rgb[1], rgb[2]);

				}
			this.lastC = this.c;
			this.lastM = this.m;
			this.lastY = this.y;
			this.lastK = this.k;
		}
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
	final public BufferedImage JPEGToRGBImage(byte[] data, int w, int h, float[] decodeArray, int pX, int pY, boolean arrayInverted, PdfObject XObject) {
		return nonRGBJPEGToRGBImage(data, w, h, decodeArray, pX, pY);
	}

	/**
	 * convert byte[] datastream JPEG to an image in RGB
	 * 
	 * @throws PdfException
	 */
	@Override
	public BufferedImage JPEG2000ToRGBImage(byte[] data, int w, int h, float[] decodeArray, int pX, int pY) throws PdfException {

		BufferedImage image;

		ByteArrayInputStream in;

		Raster ras;

		try {
			in = new ByteArrayInputStream(data);

			ImageReader iir = ImageIO.getImageReadersByFormatName("JPEG2000").next();
			ImageInputStream iin = ImageIO.createImageInputStream(in);

			iir.setInput(iin, true);

			/**
			 * indexes are a completely different game CMYK has 4 color components (C,M,Y,K) for each pixel indexed has 1 component pointing to value
			 * in lookup table so need a totally different approach
			 */
			byte[] index = this.getIndexedMap();
			if (index != null) {

				// make it RGB
				if (!isIndexConverted()) {
					index = convertIndexToRGB(index);
				}

				// get data for image (its an index so just refers to color numbers
				RenderedImage renderimage = iir.readAsRenderedImage(0, iir.getDefaultReadParam());
				ras = renderimage.getData();

				// and build a standard rgb image
				ColorModel cm = new IndexColorModel(8, index.length / 3, index, 0, false);
				image = new BufferedImage(cm, ras.createCompatibleWritableRaster(), false, null);
				image.setData(ras);

				// downsample to reduce size if huge
				image = cleanupImage(image, pX, pY, image.getType());

			}
			else { // non-indexed routine

				// This works except image is wrong so we read and convert
				image = iir.read(0);
				ras = image.getRaster();

				// apply if set
				if (decodeArray != null) {

					if ((decodeArray.length == 6 && decodeArray[0] == 1f && decodeArray[1] == 0f && decodeArray[2] == 1f && decodeArray[3] == 0f
							&& decodeArray[4] == 1f && decodeArray[5] == 0f)
							|| (decodeArray.length > 2 && decodeArray[0] == 1f && decodeArray[1] == 0)) {

						DataBuffer buf = ras.getDataBuffer();

						int count = buf.getSize();

						for (int ii = 0; ii < count; ii++)
							buf.setElem(ii, 255 - buf.getElem(ii));

					}
					else
						if (decodeArray.length == 6 && decodeArray[0] == 0f && decodeArray[1] == 1f && decodeArray[2] == 0f && decodeArray[3] == 1f
								&& decodeArray[4] == 0f && decodeArray[5] == 1f) {}
						else
							if (decodeArray != null && decodeArray.length > 0) {}
				}

				ras = cleanupRaster(ras, pX, pY, 4);
				w = ras.getWidth();
				h = ras.getHeight();

				// generate the rgb image
				WritableRaster rgbRaster;
				if (image.getType() == 13) { // indexed variant
					rgbRaster = ColorSpaceConvertor.createCompatibleWritableRaaster(image.getColorModel(), w, h);
					CSToRGB = new ColorConvertOp(this.cs, image.getColorModel().getColorSpace(), ColorSpaces.hints);
					image = new BufferedImage(w, h, image.getType());
				}
				else {

					if (CSToRGB == null) initCMYKColorspace();

					rgbRaster = ColorSpaceConvertor.createCompatibleWritableRaaster(rgbModel, w, h);

					CSToRGB = new ColorConvertOp(this.cs, rgbCS, ColorSpaces.hints);
					CSToRGB.filter(ras, rgbRaster);

					image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

				}

				image.setData(rgbRaster);
			}

			iir.dispose();
			iin.close();
			in.close();

			// image=cleanupImage(image,pX,pY);
			// image= ColorSpaceConvertor.convertToRGB(image);

		}
		catch (Exception ee) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Problem reading JPEG 2000: " + ee);

			ee.printStackTrace();
			throw new PdfException("Exception " + ee
					+ " with JPEG2000 image - please ensure imageio.jar (see http://www.idrsolutions.com/additional-jars/) on classpath");
		}
		catch (Error ee2) {
			ee2.printStackTrace();

			if (LogWriter.isOutput()) LogWriter.writeLog("Problem reading JPEG 2000 with error " + ee2);

			throw new PdfException(
					"Error with JPEG2000 image - please ensure imageio.jar (see http://www.idrsolutions.com/additional-jars/) on classpath");
		}

		return image;
	}

	/**
	 * convert Index to RGB
	 */
	@Override
	final public byte[] convertIndexToRGB(byte[] index) {

		this.isConverted = true;

		return convert4Index(index);
	}

	public static ColorSpace getColorSpaceInstance() {

		ColorSpace CMYK = new DeviceCMYKColorSpace().getColorSpace();

		// optional alternative CMYK
		String CMYKprofile = System.getProperty("org.jpedal.CMYKprofile");

		if (CMYKprofile != null) {

			try {
				CMYK = new ICC_ColorSpace(ICC_Profile.getInstance(new FileInputStream(CMYKprofile)));
			}
			catch (Exception e) {
				throw new RuntimeException("Unable to create CMYK colorspace with  " + CMYKprofile
						+ "\nPlease check Path and file valid or use built-in");
			}
		}

		return CMYK;
	}
}
