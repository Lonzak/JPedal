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
 * ColorSpaceConvertor.java
 * ---------------
 */
package org.jpedal.io;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.ImageObserver;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;

import org.jpedal.color.ColorSpaces;
import org.jpedal.color.DeviceCMYKColorSpace;
import org.jpedal.color.GenericColorSpace;
import org.jpedal.utils.LogWriter;

/**
 * set of static methods to save/load objects to convert images between different colorspaces -
 * 
 * Several methods are very similar and I should recode my code to use a common method for the RGB conversion
 * 
 * LogWriter is JPedal logging class
 * 
 */
public class ColorSpaceConvertor {

	/** Flag to trigger raster printing */
	public static boolean isUsingARGB = false;

	/**
	 * slightly contrived but very effective way to convert to RGB
	 */
	public static BufferedImage convertFromICCCMYK(int width, int height, byte[] data) {

		BufferedImage image = null;
		try {

			/** make sure data big enough and pad out if not */
			int size = width * height * 4;
			if (data.length < size) {
				byte[] newData = new byte[size];
				System.arraycopy(data, 0, newData, 0, data.length);
				data = newData;
			}

			if (1 == 2) { // left for when they add hardware acceleration

				int[] bands = { 0, 1, 2, 3 };
				/** turn it into a BufferedImage so we can filter */
				DataBuffer db = new DataBufferByte(data, data.length);

				/** create RGB colorspace and model */
				ColorSpace rgbCS = GenericColorSpace.getColorSpaceInstance();

				/** define the conversion. hints can be replaced with null */
				ColorConvertOp CSToRGB = new ColorConvertOp(DeviceCMYKColorSpace.getColorSpaceInstance(), rgbCS, ColorSpaces.hints);

				/** create RGB colorspace and model */
				ComponentColorModel rgbModel = new ComponentColorModel(rgbCS, new int[] { 8, 8, 8 }, false, false, Transparency.OPAQUE,
						DataBuffer.TYPE_BYTE);

				image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

				WritableRaster rgbRaster = rgbModel.createCompatibleWritableRaster(width, height);
				CSToRGB.filter(Raster.createInterleavedRaster(db, width, height, width * 4, 4, bands, null), rgbRaster);
				image.setData(rgbRaster);
			}
			else {
				return profileConvertCMYKImageToRGB(data, width, height);
			}

		}
		catch (Exception ee) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Exception  " + ee + " converting from ICC colorspace");
			ee.printStackTrace();
		}

		return image;
	}

	/**
	 * convert any BufferedImage to RGB colourspace
	 */
	public static BufferedImage convertToRGB(BufferedImage image) {

		// don't bother if already rgb or ICC
		if ((image.getType() != BufferedImage.TYPE_INT_RGB)) {

			try {
				/**/
				BufferedImage raw_image = image;
				image = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
				// ColorConvertOp xformOp = new ColorConvertOp(ColorSpaces.hints);/**/

				// THIS VERSION IS AT LEAST 5 TIMES SLOWER!!!
				// ColorConvertOp colOp = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), ColorSpaces.hints);
				// image=colOp.filter(image,null);

				// xformOp.filter(raw_image, image);
				new ColorConvertOp(ColorSpaces.hints).filter(raw_image, image);
				// image = raw_image;
			}
			catch (Exception e) {

				e.printStackTrace();

				if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e.toString() + " converting to RGB");
			}
			catch (Error ee) {

				ee.printStackTrace();
				if (LogWriter.isOutput()) LogWriter.writeLog("Error " + ee.toString() + " converting to RGB");

				image = null;
			}
		}

		return image;
	}

	/**
	 * convert a BufferedImage to RGB colourspace (used when I clip the image)
	 */
	public static BufferedImage convertToARGB(BufferedImage image) {

		// don't bother if already rgb
		if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
			try {
				BufferedImage raw_image = image;
				image = new BufferedImage(raw_image.getWidth(), raw_image.getHeight(), BufferedImage.TYPE_INT_ARGB);
				ColorConvertOp xformOp = new ColorConvertOp(null);
				xformOp.filter(raw_image, image);
			}
			catch (Exception e) {
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " creating argb image");
			}
		}

		isUsingARGB = true;

		return image;
	}

	/**
	 * save raw CMYK data by converting to RGB using algorithm method - pdfsages supplied the C source and I have converted - This works very well on
	 * most colours but not dark shades which are all rolled into black
	 * 
	 * This is what xpdf seems to use - <b>Note</b> we store the output data in our input queue to reduce memory usage - we have seen raw 2000 * 2000
	 * images and having input and output buffers is a LOT of memory - I have kept the doubles in as I just rewrote Leonard's code - I haven't really
	 * looked at optimisation beyond memory issues
	 */
	public static BufferedImage algorithmicConvertCMYKImageToRGB(byte[] buffer, int w, int h) {

		BufferedImage image = null;
		byte[] new_data = new byte[w * h * 3];

		int pixelCount = w * h * 4;

		double lastC = -1, lastM = -1.12, lastY = -1.12, lastK = -1.21;
		double x = 255;

		double c, m, y, aw, ac, am, ay, ar, ag, ab;
		double outRed = 0, outGreen = 0, outBlue = 0;

		int pixelReached = 0;
		for (int i = 0; i < pixelCount; i = i + 4) {

			double inCyan = (buffer[i] & 0xff) / x;
			double inMagenta = (buffer[i + 1] & 0xff) / x;
			double inYellow = (buffer[i + 2] & 0xff) / x;
			double inBlack = (buffer[i + 3] & 0xff) / x;

			if ((lastC == inCyan) && (lastM == inMagenta) && (lastY == inYellow) && (lastK == inBlack)) {
				// use existing values
			}
			else {// work out new
				double k = 1;
				c = clip01(inCyan + inBlack);
				m = clip01(inMagenta + inBlack);
				y = clip01(inYellow + inBlack);
				aw = (k - c) * (k - m) * (k - y);
				ac = c * (k - m) * (k - y);
				am = (k - c) * m * (k - y);
				ay = (k - c) * (k - m) * y;
				ar = (k - c) * m * y;
				ag = c * (k - m) * y;
				ab = c * m * (k - y);
				outRed = x * clip01(aw + 0.9137 * am + 0.9961 * ay + 0.9882 * ar);
				outGreen = x * clip01(aw + 0.6196 * ac + ay + 0.5176 * ag);
				outBlue = x * clip01(aw + 0.7804 * ac + 0.5412 * am + 0.0667 * ar + 0.2118 * ag + 0.4863 * ab);

				lastC = inCyan;
				lastM = inMagenta;
				lastY = inYellow;
				lastK = inBlack;
			}

			new_data[pixelReached++] = (byte) (outRed);
			new_data[pixelReached++] = (byte) (outGreen);
			new_data[pixelReached++] = (byte) (outBlue);

		}

		try {

			image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

			Raster raster = createInterleavedRaster(new_data, w, h);
			image.setData(raster);

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " with 24 bit RGB image");
		}

		return image;
	}

	public static BufferedImage profileConvertCMYKImageToRGB(byte[] buffer, int w, int h) {

		ColorSpace CMYK = DeviceCMYKColorSpace.getColorSpaceInstance();

		BufferedImage image = null;
		byte[] new_data = new byte[w * h * 3];

		int pixelCount = w * h * 4;

		float lastC = -1, lastM = -1, lastY = -1, lastK = -1;
		float C, M, Y, K;

		float[] rgb = new float[3];

		/**
		 * loop through each pixel changing CMYK values to RGB
		 */
		int pixelReached = 0;
		for (int i = 0; i < pixelCount; i = i + 4) {

			C = (buffer[i] & 0xff) / 255f;
			M = (buffer[i + 1] & 0xff) / 255f;
			Y = (buffer[i + 2] & 0xff) / 255f;
			K = (buffer[i + 3] & 0xff) / 255f;

			if (lastC == C && lastM == M && lastY == Y && lastK == K) {
				// use existing values if not changed
			}
			else {// work out new

				rgb = CMYK.toRGB(new float[] { C, M, Y, K });

				lastC = C;
				lastM = M;
				lastY = Y;
				lastK = K;
			}

			new_data[pixelReached++] = (byte) (rgb[0] * 255);
			new_data[pixelReached++] = (byte) (rgb[1] * 255);
			new_data[pixelReached++] = (byte) (rgb[2] * 255);

		}

		/**
		 * turn data into RGB image
		 */
		try {

			image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Raster raster = createInterleavedRaster(new_data, w, h);
			image.setData(raster);

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " with 24 bit RGB image");
		}

		return image;
	}

	/**
	 * convert YCbCr to RGB using formula
	 * 
	 */
	public static BufferedImage algorithmicConvertYCbCrToRGB(byte[] buffer, int w, int h) {

		BufferedImage image = null;
		byte[] new_data = new byte[w * h * 3];

		int pixelCount = w * h * 3;

		if (pixelCount > buffer.length) pixelCount = buffer.length;

		// boolean isAllBlack=true;

		int r = 0, g = 0, b = 0;
		int lastY = -1, lastCb = -1, lastCr = -1;
		int pixelReached = 0;
		float val1;

		for (int i = 0; i < pixelCount; i = i + 3) {

			int Y = ((buffer[i] & 255));
			int Cb = ((buffer[1 + i] & 255));
			int Cr = ((buffer[2 + i] & 255));

			if ((lastY == Y) && (lastCb == Cb) && (lastCr == Cr)) {
				// use existing values
			}
			else {// work out new

				// System.out.println(Y + " " + Cb + ' ' + Cr);

				val1 = 298.082f * Y;

				r = (int) (((val1 + (408.583f * Cr)) / 256f) - 222.921);
				if (r < 0) r = 0;
				if (r > 255) r = 255;

				g = (int) (((val1 - (100.291f * Cb) - (208.120f * Cr)) / 256f) + 135.576f);
				if (g < 0) g = 0;
				if (g > 255) g = 255;

				b = (int) (((val1 + (516.412f * Cb)) / 256f) - 276.836f);
				if (b < 0) b = 0;
				if (b > 255) b = 255;

				// track blanks
				// if(Y==255 && Cr==0 && Cb==0) {
				//
				// }else
				// isAllBlack=false;

				// if (Y == 255 && Cr == Cb && (Cr!=0)) {

				// System.out.println(Y + " " + Cb + " " + Cr + " " + CENTER);

				// r = 255;
				// g = 255;
				// b = 255;

				// }

				// System.out.println(r+" "+g+ ' ' +b);

				lastY = Y;
				lastCb = Cb;
				lastCr = Cr;

			}

			new_data[pixelReached++] = (byte) (r);
			new_data[pixelReached++] = (byte) (g);
			new_data[pixelReached++] = (byte) (b);

		}

		// if(!nonTransparent || isAllBlack){
		//
		// wasRemoved=true;
		// return null;
		// }

		try {

			image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

			Raster raster = createInterleavedRaster(new_data, w, h);
			image.setData(raster);

		}
		catch (Exception e) {
			e.printStackTrace();

			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " with 24 bit RGB image");
		}

		return image;
	}

	public static BufferedImage convertIndexedToFlat(int d, int w, int h, byte[] data, byte[] index, boolean isARGB, boolean isDownsampled) {

		BufferedImage image;
		DataBuffer db;
		int[] bandsRGB = { 0, 1, 2 };
		int[] bandsARGB = { 0, 1, 2, 3 };
		int[] bands;
		int components = 3;

		int indexLength = 0;

		if (index != null) indexLength = index.length;

		if (isARGB) {
			bands = bandsARGB;
			components = 4;
		}
		else bands = bandsRGB;

		int length = (w * h * components);

		// assume true in case of 8 bit and disprove
		// not currently used
		final boolean isGrayscale = false;// d==8; //@change

		byte[] newData = new byte[length];

		int id = 0;
		float ratio = 0f;

		switch (d) {
			case 8:

				int pt = 0;

				for (int ii = 0; ii < data.length - 1; ii++) {

					if (isDownsampled) ratio = (data[ii] & 0xff) / 255f;
					else id = (data[ii] & 0xff) * 3;

					if (pt >= length) break;

					// see - if really grayscale all components the same
					// if(index==null)
					// isGrayscale=isGrayscale && newData[pt]== newData[pt+1] && newData[pt]== newData[pt+2];
					// else
					// isGrayscale=isGrayscale && index[id]== index[id+1] && index[id]== index[id+2];

					if (isDownsampled) {
						if (ratio > 0) {
							newData[pt++] = (byte) ((255 - index[0]) * ratio);
							newData[pt++] = (byte) ((255 - index[1]) * ratio);
							newData[pt++] = (byte) ((255 - index[2]) * ratio);
						}
						else pt = pt + 3;
					}
					else {
						if (id < indexLength) {
							newData[pt++] = index[id];
							newData[pt++] = index[id + 1];
							newData[pt++] = index[id + 2];
						}
					}

					if (isARGB) {
						if (id == 0 && ratio == 0) newData[pt++] = (byte) 255;
						else newData[pt++] = 0;
					}
				}

				break;

			case 4:
				flatten4bpc(w, data, index, isARGB, length, newData);
				break;

			case 2:
				flatten2bpc(w, data, index, isARGB, length, newData);
				break;

			case 1:
				flatten1bpc(w, data, index, isARGB, length, 255, newData);
				break;

		}

		/** create the image */
		if (isARGB) image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		else
			if (isGrayscale) image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
			else image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

		if (isGrayscale) {

			byte[] grayData = new byte[w * h];
			int j = 0;
			for (int i = 0; i < newData.length; i = i + 3) {
				grayData[j] = newData[i];
				j++;
			}

			bands = new int[] { 0 };

			Raster raster = Raster.createInterleavedRaster(new DataBufferByte(grayData, grayData.length), w, h, w, 1, bands, null);

			image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
			image.setData(raster);

		}
		else {
			db = new DataBufferByte(newData, newData.length);
			WritableRaster raster = Raster.createInterleavedRaster(db, w, h, w * components, components, bands, null);
			image.setData(raster);
		}

		return image;
	}

	private static void flatten4bpc(int w, byte[] data, byte[] index, boolean isARGB, int length, byte[] newData) {

		int id1, pt = 0;
		int[] shift = { 4, 0 };
		int widthReached = 0;

		for (byte aData : data) {

			for (int samples = 0; samples < 2; samples++) {

				id1 = ((aData >> shift[samples]) & 15) * 3;

				if (pt >= length) break;

				newData[pt++] = index[id1];
				newData[pt++] = index[id1 + 1];
				newData[pt++] = index[id1 + 2];

				if (isARGB) {
					if (id1 == 0) newData[pt++] = (byte) 0;
					else newData[pt++] = 0;
				}

				// ignore filler bits
				widthReached++;
				if (widthReached == w) {
					widthReached = 0;
					samples = 8;
				}
			}
		}
	}

	public static void flatten1bpc(int w, byte[] data, byte[] index, boolean isARGB, int length, int transparency, byte[] newData) {

		int pt = 0;
		int id;// work through the bytes
		int widthReached = 0;
		for (byte aData : data) {

			for (int bits = 0; bits < 8; bits++) {

				// int id=((data[ii] & (1<<bits)>>bits))*3;
				id = ((aData >> (7 - bits)) & 1) * 3;

				if (pt >= length) break;

				// @itemtoFix
				if (isARGB) {

					// System.out.println(id+" "+index[id]+" "+index[id+1]+" "+index[id+2]);

					// treat white as transparent
					if (1 == 2 && (index[id] & 255) > 250 && (index[id + 1] & 255) > 250 && (index[id + 2] & 255) > 250) {
						pt = pt + 4;
					}
					else
						if (id == 0) {
							newData[pt++] = index[id];
							newData[pt++] = index[id + 1];
							newData[pt++] = index[id + 2];

							newData[pt++] = (byte) transparency;

						}
						else {
							newData[pt++] = index[id];
							newData[pt++] = index[id + 1];
							newData[pt++] = index[id + 2];

							newData[pt++] = 0;
							// System.out.println(id+" "+index[id]+" "+index[id+1]+" "+index[id+2]);
						}

				}
				else {
					newData[pt++] = index[id];
					newData[pt++] = index[id + 1];
					newData[pt++] = index[id + 2];

				}
				// ignore filler bits
				widthReached++;
				if (widthReached == w) {
					widthReached = 0;
					bits = 8;
				}
			}
		}
	}

	/**
	 * convert to RGB or gray. If index is null we assume single component gray
	 * 
	 * @param w
	 * @param data
	 * @param index
	 * @param isARGB
	 * @param length
	 * @param newData
	 */
	public static void flatten2bpc(int w, byte[] data, byte[] index, boolean isARGB, int length, byte[] newData) {

		int id1, pt = 0;

		int[] shift = { 6, 4, 2, 0 };
		int widthReached = 0;

		for (byte aData : data) {

			for (int samples = 0; samples < 4; samples++) {

				if (pt >= length) break;

				if (index == null) {
					id1 = ((aData << shift[3 - samples]) & 192);
					if (id1 == 192) { // top value white needs to be 255 so trap
						id1 = 255;
					}
					newData[pt++] = (byte) (id1);
				}
				else {
					id1 = ((aData >> shift[samples]) & 3) * 3;

					newData[pt++] = index[id1];
					newData[pt++] = index[id1 + 1];
					newData[pt++] = index[id1 + 2];

					if (isARGB) {
						if (id1 == 0) newData[pt++] = (byte) 0;
						else newData[pt++] = 0;
					}
				}

				// ignore filler bits
				widthReached++;
				if (widthReached == w) {
					widthReached = 0;
					samples = 8;
				}
			}
		}
	}

	/**
	 * convert YCC to CMY via formula and the CMYK to sRGB via profiles
	 */
	public static BufferedImage iccConvertCMYKImageToRGB(byte[] buffer, int w, int h) throws IOException {

		/**
		 * set colorspaces and color models using profiles if set
		 */

		ColorSpace CMYK = DeviceCMYKColorSpace.getColorSpaceInstance();

		ColorSpace rgbCS = GenericColorSpace.getColorSpaceInstance();

		ColorModel rgbModel = new ComponentColorModel(rgbCS, new int[] { 8, 8, 8 }, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
		ColorConvertOp CSToRGB = new ColorConvertOp(CMYK, rgbCS, ColorSpaces.hints);

		int pixelCount = w * h * 4;
		int Y, Cb, Cr, CENTER, lastY = -1, lastCb = -1, lastCr = -1, lastCENTER = -1;

		int outputC = 0, outputM = 0, outputY = 0;
		double R, G, B;
		// turn YCC in Buffer to CYM using profile
		for (int i = 0; i < pixelCount; i = i + 4) {

			Y = (buffer[i] & 255);
			Cb = (buffer[i + 1] & 255);
			Cr = (buffer[i + 2] & 255);
			CENTER = (buffer[i + 3] & 255);

			if (Y == lastY && Cb == lastCb && Cr == lastCr && CENTER == lastCENTER) {
				// no change so use last value
			}
			else { // new value

				R = Y + 1.402 * Cr - 179.456;
				if (R < 0d) R = 0d;
				else
					if (R > 255d) R = 255d;

				G = Y - 0.34414 * Cb - 0.71414 * Cr + 135.45984;
				if (G < 0d) G = 0d;
				else
					if (G > 255d) G = 255d;

				B = Y + 1.772 * Cb - 226.816;
				if (B < 0d) B = 0d;
				else
					if (B > 255d) B = 255d;

				outputC = 255 - (int) R;
				outputM = 255 - (int) G;
				outputY = 255 - (int) B;

				// flag so we can just reuse if next value the same
				lastY = Y;
				lastCb = Cb;
				lastCr = Cr;
				lastCENTER = CENTER;
			}

			// put back as CMY
			buffer[i] = (byte) (outputC);
			buffer[i + 1] = (byte) (outputM);
			buffer[i + 2] = (byte) (outputY);

		}

		/**
		 * create CMYK raster from buffer
		 */
		Raster raster = Raster.createInterleavedRaster(new DataBufferByte(buffer, buffer.length), w, h, w * 4, 4, new int[] { 0, 1, 2, 3 }, null);
		WritableRaster rgbRaster = rgbModel.createCompatibleWritableRaster(w, h);

		/**
		 * convert to sRGB with profiles (I think this is done native as its much faster than my pure Java efforts)
		 */
		CSToRGB.filter(raster, rgbRaster);

		// data now sRGB so create image
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		image.setData(rgbRaster);

		return image;
	}

	/**
	 * convert a BufferedImage to RGB colourspace
	 */
	public static BufferedImage convertColorspace(BufferedImage image, int newType) {

		try {
			BufferedImage raw_image = image;
			image = new BufferedImage(raw_image.getWidth(), raw_image.getHeight(), newType);
			ColorConvertOp xformOp = new ColorConvertOp(null);
			xformOp.filter(raw_image, image);
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " converting image");

		}
		return image;
	}

	/** convenience method used to check value within bounds */
	private static double clip01(double value) {

		if (value < 0) value = 0;

		if (value > 1) value = 1;

		return value;
	}

	public static WritableRaster createCompatibleWritableRaaster(ColorModel colorModel, int w, int h) {
		return colorModel.createCompatibleWritableRaster(w, h);
	}

	public static Raster createInterleavedRaster(byte[] data, int w, int h) {

		DataBuffer db = new DataBufferByte(data, data.length);
		int[] bands = { 0, 1, 2 };
		return Raster.createInterleavedRaster(db, w, h, w * 3, 3, bands, null);
	}

	public static void drawImage(Graphics2D g2, BufferedImage tileImg, AffineTransform tileAff, ImageObserver observer) {

		g2.drawImage(tileImg, tileAff, observer);
	}

	public static void flatten4bpc(int w, byte[] data, int newSize, byte[] newData) {

		int origSize = data.length;

		byte rawByte;
		int ptr = 0, currentLine = 0;
		boolean oddValues = ((w & 1) == 1);
		for (int ii = 0; ii < origSize; ii++) {
			rawByte = data[ii];

			currentLine = currentLine + 2;
			newData[ptr] = (byte) (rawByte & 240);
			if (newData[ptr] == -16) // fix for white
			newData[ptr] = (byte) 255;
			ptr++;

			if (oddValues && currentLine > w) { // ignore second value if odd as just packing
				currentLine = 0;
			}
			else {
				newData[ptr] = (byte) ((rawByte & 15) << 4);
				if (newData[ptr] == -16) // fix for white
				newData[ptr] = (byte) 255;
				ptr++;
			}

			if (ptr == newSize) ii = origSize;
		}
	}
}
