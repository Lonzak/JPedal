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
 * SeparationColorSpace.java
 * ---------------
 */

package org.jpedal.color;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.jpedal.exception.PdfException;
import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.io.JAIHelper;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

/**
 * handle Separation ColorSpace and some DeviceN functions
 */
public class SeparationColorSpace extends GenericColorSpace {

	private static final long serialVersionUID = 8546233863470224093L;

	protected GenericColorSpace altCS;

	final static int Black = 1009857357;
	final static int PANTONE_BLACK = 573970506;
	final private static int Cyan = 323563838;
	final private static int Magenta = 895186280;
	final public static int Yellow = 1010591868;

	protected ColorMapping colorMapper;

	// avoid rereading colorspaces
	protected Map cachedValues = new HashMap();

	private float[] domain;

	/* if we use CMYK */
	protected int cmykMapping = NOCMYK;

	protected boolean isProcess = false;

	final static protected int NOCMYK = -1;
	final static protected int MYK = 1;
	final static protected int CMY = 2;
	final static protected int CMK = 4;
	final static protected int CY = 5;
	final static protected int MY = 6;
	final static protected int CM = 8;

	final static protected int CMYK = 7; // use don 6 values where CMYK is first 4

	final static protected int CMYB = 9;

	public SeparationColorSpace() {}

	public SeparationColorSpace(PdfObjectReader currentPdfFile, PdfObject colorSpace) {

		this.value = ColorSpaces.Separation;

		processColorToken(currentPdfFile, colorSpace);
	}

	protected void processColorToken(PdfObjectReader currentPdfFile, PdfObject colorSpace) {

		PdfObject indexed = colorSpace.getDictionary(PdfDictionary.Indexed);
		PdfObject functionObj = colorSpace.getDictionary(PdfDictionary.tintTransform);

		this.domain = null;

		if (colorSpace.getDictionary(PdfDictionary.Process) != null) this.isProcess = true;

		// name of color if separation or Components if device and component count
		byte[] name = null;
		byte[][] components = null;
		if (this.value == ColorSpaces.Separation) {
			name = colorSpace.getStringValueAsByte(PdfDictionary.Name);

			if (name != null) components = new byte[][] { name };
			this.componentCount = 1;
		}
		else {
			components = colorSpace.getStringArray(PdfDictionary.Components);
			this.componentCount = components.length;
		}

		// test values

		this.cmykMapping = NOCMYK;

		int[] values = new int[this.componentCount];
		if (components != null) {
			for (int ii = 0; ii < this.componentCount; ii++) {
				values[ii] = PdfDictionary.generateChecksum(1, components[ii].length - 1, components[ii]);
			}
		}

		switch (this.componentCount) {

			case 1:
				if (components != null && (values[0] == Black || values[0] == PANTONE_BLACK)) {
					this.cmykMapping = Black;
				}

				break;

			case 2:

				if (values[0] == Cyan) {
					if (values[1] == Yellow) this.cmykMapping = CY;
					else
						if (values[1] == Magenta) this.cmykMapping = CM;
				}
				else
					if (values[0] == Magenta && values[1] == Yellow) this.cmykMapping = MY;

				break;

			case 3:

				if (values[0] == Magenta && values[1] == Yellow && values[2] == Black) this.cmykMapping = MYK;
				else
					if (values[0] == Cyan && values[1] == Magenta && values[2] == Yellow) this.cmykMapping = CMY;
					else
						if (values[0] == Cyan && values[1] == Magenta && values[2] == Black) this.cmykMapping = CMK;
				break;

			case 4:

				if (values[0] == Cyan && values[1] == Magenta && values[2] == Yellow && values[3] == Black) this.cmykMapping = CMYB;
				break;

			case 5:

				if (values[0] == Cyan && values[1] == Magenta && values[2] == Yellow && values[3] == Black) this.cmykMapping = CMYK;
				break;

			case 6:

				if (values[0] == Cyan && values[1] == Magenta && values[2] == Yellow && values[3] == Black) this.cmykMapping = CMYK;
				break;
		}

		// hard-code myk and cmy
		if (this.cmykMapping != NOCMYK) {

			this.altCS = new DeviceCMYKColorSpace();

		}
		else {

			/**
			 * work out colorspace (can also be direct ie /Pattern)
			 */
			colorSpace = colorSpace.getDictionary(PdfDictionary.AlternateSpace);

			// System.out.println("Set uncached AlCS "+colorSpace.getObjectRefAsString()+" "+this);
			this.altCS = ColorspaceFactory.getColorSpaceInstance(currentPdfFile, colorSpace);

			// use alternate as preference if CMYK
			if (this.altCS.getID() == ColorSpaces.ICC && colorSpace.getParameterConstant(PdfDictionary.Alternate) == ColorSpaces.DeviceCMYK) this.altCS = new DeviceCMYKColorSpace();

		}

		if (name != null) {
			int len = name.length, jj = 0, topHex, bottomHex;
			byte[] tempName = new byte[len];
			for (int i = 0; i < len; i++) {
				if (name[i] == '#') {
					// roll on past #
					i++;

					topHex = name[i];

					// convert to number
					if (topHex >= 'A' && topHex <= 'F') topHex = topHex - 55;
					else
						if (topHex >= 'a' && topHex <= 'f') topHex = topHex - 87;
						else
							if (topHex >= '0' && topHex <= '9') topHex = topHex - 48;

					i++;

					while (name[i] == 32 || name[i] == 10 || name[i] == 13)
						i++;

					bottomHex = name[i];

					if (bottomHex >= 'A' && bottomHex <= 'F') bottomHex = bottomHex - 55;
					else
						if (bottomHex >= 'a' && bottomHex <= 'f') bottomHex = bottomHex - 87;
						else
							if (bottomHex >= '0' && bottomHex <= '9') bottomHex = bottomHex - 48;

					tempName[jj] = (byte) (bottomHex + (topHex << 4));
				}
				else {
					tempName[jj] = name[i];
				}

				jj++;
			}

			// resize
			if (jj != len) {
				name = new byte[jj];
				System.arraycopy(tempName, 0, name, 0, jj);

			}

			this.pantoneName = new String(name);
		}

		/**
		 * setup transformation
		 **/
		if (functionObj == null) colorSpace.getDictionary(PdfDictionary.tintTransform);

		if (functionObj == null && indexed != null) functionObj = indexed.getDictionary(PdfDictionary.tintTransform);

		this.colorMapper = new ColorMapping(currentPdfFile, functionObj);
		this.domain = functionObj.getFloatArray(PdfDictionary.Domain);
	}

	/** private method to do the calculation */
	private void setColor(float value) {

		try {

			if (this.cmykMapping == Black) { // special case coded in

				float[] newOp = { 0f, 0f, 0f, value };
				this.altCS.setColor(newOp, 1);

			}
			else {
				// optimisation for white
				// if(altCS.getID()==ColorSpaces.DeviceCMYK && value==1.0){
				// altCS.setColor(new float[]{0,0,0,0},4);
				// }else
				{
					// adjust size if needed
					int elements = 1;

					if (this.domain != null) elements = this.domain.length / 2;

					float[] values = new float[elements];
					for (int j = 0; j < elements; j++)
						values[j] = value;

					float[] operand = this.colorMapper.getOperandFloat(values);

					this.altCS.setColor(operand, operand.length);
				}
			}
		}
		catch (Exception e) {}
	}

	/** set color (translate and set in alt colorspace */
	@Override
	public void setColor(float[] operand, int opCount) {

		setColor(operand[0]);
	}

	/** set color (translate and set in alt colorspace */
	@Override
	public void setColor(String[] operand, int opCount) {

		float[] f = new float[1];
		f[0] = Float.parseFloat(operand[0]);

		setColor(f, 1);
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

			// iir = (ImageReader)ImageIO.getImageReadersByFormatName("JPEG").next();

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

			// special case
			if (this.altCS.getID() == ColorSpaces.DeviceGray) {

				for (int aa = 0; aa < rawData.length; aa++)
					rawData[aa] = (byte) (rawData[aa] ^ 255);
				final int[] bands = { 0 };
				image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
				Raster raster = Raster.createInterleavedRaster(new DataBufferByte(rawData, rawData.length), w, h, w, 1, bands, null);

				image.setData(raster);

			}
			else {
				// convert the image in general case
				image = createImage(w, h, rawData, arrayInverted);
			}
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

	/**
	 * convert data stream to srgb image
	 */
	@Override
	public BufferedImage JPEG2000ToRGBImage(byte[] data, int w, int h, float[] decodeArray, int pX, int pY) throws PdfException {

		BufferedImage image;

		ByteArrayInputStream in;
		ImageReader iir;

		try {
			in = new ByteArrayInputStream(data);

			/** 1.4 code */
			// standard java 1.4 IO

			iir = ImageIO.getImageReadersByFormatName("JPEG2000").next();

		}
		catch (Exception ee) {
			// noinspection UnusedAssignment
			image = null;

			if (LogWriter.isOutput()) LogWriter.writeLog("Problem reading JPEG 2000: " + ee);

			String message = "Exception " + ee
					+ " with JPeg 2000 Image from iir = (ImageReader)ImageIO.getImageReadersByFormatName(\"JPEG2000\").next();";

			if (!JAIHelper.isJAIused()) message = "JPeg 2000 Images and JAI not setup.\nYou need both JAI and imageio.jar on classpath, "
					+ "and the VM parameter -Dorg.jpedal.jai=true switch turned on";

			throw new PdfException(message);
		}

		if (iir == null) return null;

		try {
			// ImageIO.setUseCache(false);
			ImageInputStream iin = ImageIO.createImageInputStream(in);
			try {
				iir.setInput(iin, true); // new MemoryCacheImageInputStream(in));
				image = iir.read(0);
				iir.dispose();
				iin.close();
				in.close();
			}
			catch (Exception e) {

				if (LogWriter.isOutput()) LogWriter.writeLog("Problem reading JPEG 2000: " + e);

				e.printStackTrace();
				return null;
			}

			image = cleanupImage(image, pX, pY, this.value);

			w = image.getWidth();
			h = image.getHeight();

			DataBufferByte rgb = (DataBufferByte) image.getRaster().getDataBuffer();
			byte[] rawData = rgb.getData();

			// convert the image
			image = createImage(w, h, rawData, false);

		}
		catch (Exception ee) {
			image = null;

			if (LogWriter.isOutput()) LogWriter.writeLog("Couldn't read JPEG, not even raster: " + ee);
		}

		try {
			in.close();
			iir.dispose();
			// iin.close();
		}
		catch (Exception ee) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Problem closing  " + ee);
		}

		return image;
	}

	/**
	 * convert separation stream to RGB and return as an image
	 */
	@Override
	public BufferedImage dataToRGB(byte[] data, int w, int h) {

		BufferedImage image;

		try {

			// convert data
			image = createImage(w, h, data, false);

		}
		catch (Exception ee) {
			image = null;

			if (LogWriter.isOutput()) LogWriter.writeLog("Couldn't convert Separation colorspace data: " + ee);
		}

		return image;
	}

	/**
	 * turn raw data into an image
	 */
	private BufferedImage createImage(int w, int h, byte[] rgb, boolean arrayInverted) {

		BufferedImage image;

		int pixelCount = 3 * w * h;
		byte[] imageData = new byte[pixelCount];

		// convert data to RGB format
		int pixelReached = 0;

		// cache table for speed
		float[][] lookuptable = new float[3][256];
		for (int i = 0; i < 256; i++)
			lookuptable[0][i] = -1;

		for (byte aRgb : rgb) {

			int value = (aRgb & 255);

			if (lookuptable[0][value] == -1) {
				if (arrayInverted) setColor(1f - (value / 255f));
				else setColor(value / 255f);

				lookuptable[0][value] = ((Color) this.getColor()).getRed();
				lookuptable[1][value] = ((Color) this.getColor()).getGreen();
				lookuptable[2][value] = ((Color) this.getColor()).getBlue();

			}

			for (int comp = 0; comp < 3; comp++) {
				imageData[pixelReached] = (byte) lookuptable[comp][value];
				pixelReached++;
			}
		}

		// create the RGB image
		image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Raster raster = ColorSpaceConvertor.createInterleavedRaster(imageData, w, h);
		image.setData(raster);

		return image;
	}

	/**
	 * create rgb index for color conversion
	 */
	@Override
	public byte[] convertIndexToRGB(byte[] data) {

		this.isConverted = true;

		byte[] newdata = new byte[3 * 256]; // converting to RGB so size known

		try {

			int outputReached = 0;
			float[] opValues;
			Color currentCol;
			float[] operand;
			int byteCount = data.length;
			float[] values = new float[this.componentCount];

			// scan each byte and convert
			for (int i = 0; i < byteCount; i = i + this.componentCount) {

				// turn into rgb and store
				if (this.componentCount == 1 && this.value == ColorSpaces.Separation && this.colorMapper == null) { // separation (fix bug with 1
																													// component DeviceN with second
																													// check)
					opValues = new float[1];
					opValues[1] = (data[i] & 255);
					setColor(opValues, 1);
					currentCol = (Color) this.getColor();
				}
				else { // convert deviceN

					for (int j = 0; j < this.componentCount; j++)
						values[j] = (data[i + j] & 255) / 255f;

					operand = this.colorMapper.getOperandFloat(values);

					this.altCS.setColor(operand, operand.length);
					currentCol = (Color) this.altCS.getColor();

				}

				newdata[outputReached] = (byte) currentCol.getRed();
				outputReached++;
				newdata[outputReached] = (byte) currentCol.getGreen();
				outputReached++;
				newdata[outputReached] = (byte) currentCol.getBlue();
				outputReached++;

			}

		}
		catch (Exception ee) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Exception  " + ee + " converting colorspace");

		}

		return newdata;
	}

	/**
	 * get color
	 */
	@Override
	public PdfPaint getColor() {
		return this.altCS.getColor();
	}

	/**
	 * clone graphicsState
	 */
	@Override
	final public Object clone() {

		this.setColorStatus();

		Object o;
		try {
			o = super.clone();
		}
		catch (Exception e) {
			throw new RuntimeException("Unable to clone object");
		}

		return o;
	}

	private void setColorStatus() {

		int foreground = this.altCS.currentColor.getRGB();

		this.r = ((foreground >> 16) & 0xFF);
		this.g = ((foreground >> 8) & 0xFF);
		this.b = ((foreground) & 0xFF);
	}

	@Override
	public void restoreColorStatus() {

		this.altCS.currentColor = new PdfColor(this.r, this.g, this.b);

		// values may now be wrong in cache code so force reset
		this.altCS.clearCache();
	}

	/**
	 * get alt colorspace for separation colorspace
	 */
	public GenericColorSpace getAltColorSpace() {
		return this.altCS;
	}

}
