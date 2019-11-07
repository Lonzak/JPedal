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
 * GenericColorSpace.java
 * ---------------
 */
package org.jpedal.color;

//standard java
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

import org.jpedal.examples.handlers.DefaultImageHelper;
import org.jpedal.exception.PdfException;
import org.jpedal.external.ImageHelper;
import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.io.JAIHelper;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Strip;
import org.w3c.dom.NodeList;

/**
 * Provides Color functionality and conversion for pdf decoding
 */
public class GenericColorSpace implements Cloneable, Serializable {

	private static final long serialVersionUID = -4102665469115658823L;

	public ImageHelper images = new DefaultImageHelper();

	boolean isConverted = false;

	/** any intent */
	private String intent = null;

	/** actual raw value */
	float[] rawValues;

	Map patterns; // holds new PdfObjects

	/** for Patterns */
	float[][] CTM;

	/** handle shading */
	float[] inputs;

	/** size for indexed colorspaces */
	private int size = 0;

	/** holds cmyk values if present */
	float c = -1;
	float y = -1;
	float m = -1;
	float k = -1;

	/** matrices for calculating CIE XYZ colour */
	float[] W;
	float[] G;
	float[] Ma;
	// private float[] B;
	float[] R;

	/** defines rgb colorspace */
	public static ColorSpace rgbCS;

	public static final String cb = "<color ";

	public static final String ce = "</color>";

	// ID of colorspace (ie DeviceRGB)
	int value = ColorSpaces.DeviceRGB;

	/** conversion Op for translating rasters or images */
	static ColorConvertOp CSToRGB = null;

	ColorSpace cs;

	PdfPaint currentColor = new PdfColor(0, 0, 0);

	/** rgb colormodel */
	static ColorModel rgbModel = null;

	/** currently does nothing but added so we can introduce profile matching */
	public static ICC_Profile ICCProfileForRGB = null;

	/** enables optimisations for PDF output - enabled with jvm flag org.jpedal.fasterPNG */
	public static boolean fasterPNG = false;

	// flag to show problem with colors
	boolean failed = false;

	int alternative = PdfDictionary.Unknown;

	private PdfObject decodeParms = null;

	private boolean hasYCCKimages = false;

	private Object[] cache;

	boolean isPrinting = false;

	public void setPrinting(boolean isPrinting) {

		// local handles
		this.isPrinting = isPrinting;
	}

	/** initialise all the colorspaces when first needed */
	protected static void initCMYKColorspace() throws PdfException {

		try {

			if (ICCProfileForRGB == null) {
				rgbModel = new ComponentColorModel(rgbCS, new int[] { 8, 8, 8 }, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
			}
			else {
				int compCount = rgbCS.getNumComponents();
				int[] values = new int[compCount];
				for (int i = 0; i < compCount; i++)
					values[i] = 8;

				rgbModel = new ComponentColorModel(rgbCS, values, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
			}

			/** create CMYK colorspace using icm profile */
			ICC_Profile p = ICC_Profile.getInstance(GenericColorSpace.class.getResourceAsStream("/org/jpedal/res/cmm/cmyk.icm"));
			ICC_ColorSpace cmykCS = new ICC_ColorSpace(p);

			/** define the conversion. PdfColor.hints can be replaced with null or some hints */
			CSToRGB = new ColorConvertOp(cmykCS, rgbCS, ColorSpaces.hints);
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e.getMessage() + " initialising color components");

			throw new PdfException("[PDF] Unable to create CMYK colorspace. Check cmyk.icm in jar file");

		}
	}

	/**
	 * reset any defaults if reused
	 */
	public void reset() {

		this.currentColor = new PdfColor(0, 0, 0);
	}

	// show if problem and we should default to Alt
	public boolean isInvalid() {
		return this.failed;
	}

	// allow user to replace sRGB colorspace
	static {

		// enable user to disable some checks and used indexed output
		String fasterPNG = System.getProperty("org.jpedal.fasterPNG");
		GenericColorSpace.fasterPNG = fasterPNG != null && fasterPNG.toLowerCase().equals("true");

		String profile = System.getProperty("org.jpedal.RGBprofile");

		if (profile != null) {
			try {
				ICCProfileForRGB = ICC_Profile.getInstance(new FileInputStream(profile));

			}
			catch (Exception e) {
				e.printStackTrace();
				if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Problem " + e.getMessage() + " with ICC data ");

				if (ICCProfileForRGB == null) throw new RuntimeException("Problem wth RGB profile " + profile + ' ' + e.getMessage());
			}
		}

		if (ICCProfileForRGB != null) {
			rgbCS = new ICC_ColorSpace(ICCProfileForRGB);
		}
		else rgbCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
	}

	/**
	 * get size
	 */
	public int getIndexSize() {
		return this.size;
	}

	/**
	 * get color
	 */
	public PdfPaint getColor() {
		return this.currentColor;
	}

	/** return the set Java colorspace */
	public ColorSpace getColorSpace() {
		return this.cs;
	}

	GenericColorSpace() {

		this.cs = rgbCS;
	}

	protected void setAlternateColorSpace(int alt) {
		this.alternative = alt;
	}

	public int getAlternateColorSpace() {
		return this.alternative;
	}

	/** store color setting when we push to stack */
	int r;
	int g;
	int b;

	public void restoreColorStatus() {

		this.currentColor = new PdfColor(this.r, this.g, this.b);
	}

	/**
	 * clone graphicsState
	 */
	@Override
	public Object clone() {
		// this.setColorStatus();

		Object o;
		try {
			o = super.clone();
		}
		catch (Exception e) {
			throw new RuntimeException("Unable to clone object");
		}

		return o;
	}

	/** any indexed colormap */
	byte[] IndexedColorMap = null;

	/** pantone name if present */
	String pantoneName = null;

	/** number of colors */
	int componentCount = 3;

	/** handle to graphics state / only set and used by Pattern */
	GraphicsState gs;

	int pageWidth, pageHeight;

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
	final protected BufferedImage nonRGBJPEGToRGBImage(byte[] data, int w, int h, float[] decodeArray, int pX, int pY) {

		boolean isProcessed = false;

		BufferedImage image = null;
		ByteArrayInputStream in = null;

		ImageReader iir = null;
		ImageInputStream iin = null;

		try {

			if (CSToRGB == null) initCMYKColorspace();

			CSToRGB = new ColorConvertOp(this.cs, rgbCS, ColorSpaces.hints);

			in = new ByteArrayInputStream(data);

			int cmykType = getJPEGTransform(data);

			// suggestion from Carol
			try {
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

			// iir = (ImageReader)ImageIO.getImageReadersByFormatName("JPEG").next();
			ImageIO.setUseCache(false);

			iin = ImageIO.createImageInputStream((in));
			iir.setInput(iin, true); // new MemoryCacheImageInputStream(in));

			Raster ras = iir.readRaster(0, null);

			// invert
			if (decodeArray != null) {

				// decodeArray=Strip.removeArrayDeleminators(decodeArray).trim();

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
							&& decodeArray[4] == 0f && decodeArray[5] == 1f) {
						// }else if(decodeArray.indexOf("0 1 0 1 0 1 0 1")!=-1){//identity
						// }else if(decodeArray.indexOf("0.0 1.0 0.0 1.0 0.0 1.0 0.0 1.0")!=-1){//identity
					}
					else
						if (decodeArray != null && decodeArray.length > 0) {}
			}

			if (this.cs.getNumComponents() == 4) { // if 4 col CMYK of ICC translate

				isProcessed = true;

				try {
					if (cmykType == 2) {

						this.hasYCCKimages = true;
						image = ColorSpaceConvertor.iccConvertCMYKImageToRGB(((DataBufferByte) ras.getDataBuffer()).getData(), w, h);

					}
					else {

						ras = cleanupRaster(ras, pX, pY, 4);
						w = ras.getWidth();
						h = ras.getHeight();

						/** generate the rgb image */
						WritableRaster rgbRaster = rgbModel.createCompatibleWritableRaster(w, h);

						// if(cmykType!=0)
						CSToRGB.filter(ras, rgbRaster);
						image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
						image.setData(rgbRaster);

						// slower in tests
						// image=new BufferedImage(rgbModel,rgbRaster,false,null);

					}
				}
				catch (Exception e) {
					e.printStackTrace();

					if (LogWriter.isOutput()) LogWriter.writeLog("Problem with JPEG conversion");
				}
			}
			else
				if (cmykType != 0) {

					image = iir.read(0);

					image = cleanupImage(image, pX, pY, this.value);

					isProcessed = true;

				}

			// test

			if (!isProcessed) {
				/** 1.3 version or vanilla version */
				WritableRaster rgbRaster;

				if (cmykType == 4) { // CMYK

					ras = cleanupRaster(ras, pX, pY, 4);

					int width = ras.getWidth();
					int height = ras.getHeight();

					rgbRaster = rgbModel.createCompatibleWritableRaster(width, height);
					CSToRGB.filter(ras, rgbRaster);
					image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					image.setData(rgbRaster);

				}
				else { // type 7 - these seem to crash the new 1.4 IO routines as far as I can see

					boolean isYCC = false;
					try {
						IIOMetadata metadata = iir.getImageMetadata(0);
						String metadataFormat = metadata.getNativeMetadataFormatName();
						IIOMetadataNode iioNode = (IIOMetadataNode) metadata.getAsTree(metadataFormat);

						NodeList children = iioNode.getElementsByTagName("app14Adobe");
						if (children.getLength() > 0) {
							isYCC = true;
						}
					}
					catch (Exception ee) {
						if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Unable to read metadata on Jpeg " + ee);
					}

					if (LogWriter.isOutput()) LogWriter.writeLog("COLOR_ID_YCbCr image");

					if (isYCC) { // sample file debug2/pdf4134.pdf suggests we need this change
						image = ImageIO.read(new ByteArrayInputStream(data));
					}
					else {
						// try with iccConvertCMYKImageToRGB(byte[] buffer,int w,int h) and delete if works
						image = ColorSpaceConvertor.algorithmicConvertYCbCrToRGB(((DataBufferByte) ras.getDataBuffer()).getData(), w, h);
					}

					image = cleanupImage(image, pX, pY, this.value);
					image = ColorSpaceConvertor.convertToRGB(image);
				}
			}

		}
		catch (Exception ee) {
			image = null;
			ee.printStackTrace();

			if (LogWriter.isOutput()) LogWriter.writeLog("Couldn't read JPEG, not even raster: " + ee);
		}
		catch (Error err) {
			if (iir != null) iir.dispose();
			if (iin != null) {
				try {
					iin.flush();
				}
				catch (IOException e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}
			}
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

	protected static BufferedImage cleanupImage(BufferedImage image, int pX, int pY, int colorspaceType) {

		try {
			final boolean marksNewCode = true;

			int imageType = image.getType();

			if (getSampling(image.getWidth(), image.getHeight(), pX, pY) <= 1 || imageType == BufferedImage.TYPE_CUSTOM) return image;

			if (marksNewCode && imageType == BufferedImage.TYPE_3BYTE_BGR) {
				return cleanupBGRImage(image, pX, pY);
			}
			else {
				if (imageType == 5) image = ColorSpaceConvertor.convertToRGB(image);

				Raster ras = cleanupRaster(image.getData(), pX, pY, image.getColorModel().getNumColorComponents());

				image = new BufferedImage(ras.getWidth(), ras.getHeight(), image.getType());
				image.setData(ras);

				return image;
			}

		}
		catch (Error err) {
			if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Error in cleanupImage " + err);
		}

		return image;
	}

	private static int getSampling(int w, int h, int pX, int pY) {

		int sampling = 1; // keep as multiple of 2
		int newW = w, newH = h;

		if (pX > 0 && pY > 0) {

			int smallestH = pY << 2; // double so comparison works
			int smallestW = pX << 2;

			// cannot be smaller than page
			while (newW > smallestW && newH > smallestH) {
				sampling = sampling << 1;
				newW = newW >> 1;
				newH = newH >> 1;
			}

			int scaleX = w / pX;
			if (scaleX < 1) scaleX = 1;

			int scaleY = h / pY;
			if (scaleY < 1) scaleY = 1;

			// choose smaller value so at least size of page
			sampling = scaleX;
			if (sampling > scaleY) sampling = scaleY;
		}

		return sampling;
	}

	protected static Raster cleanupRaster(Raster ras, int pX, int pY, int comp) {

		/**
		 * allow user to disable this function and just return raw data
		 */
		String avoidCleanupRaster = System.getProperty("org.jpedal.avoidCleanupRaster");
		if (avoidCleanupRaster != null && avoidCleanupRaster.toLowerCase().contains("true")) {
			return ras;
		}

		byte[] buffer = null;
		int[] intBuffer = null;
		int type;
		DataBuffer data = ras.getDataBuffer();
		if (data instanceof DataBufferInt) type = 1;
		else type = 0;

		if (type == 1) intBuffer = ((DataBufferInt) data).getData();
		else {
			int layerCount = ras.getNumBands();
			if (layerCount == comp) {
				buffer = ((DataBufferByte) data).getData();
			}
			else
				if (layerCount == 1) {
					byte[] rawBuffer = ((DataBufferByte) ras.getDataBuffer()).getData();
					int size = rawBuffer.length;
					int realSize = size * comp;
					int j = 0, i = 0;
					buffer = new byte[realSize];
					while (true) {
						for (int a = 0; a < comp; a++) {
							buffer[j] = rawBuffer[i];
							j++;
						}
						i++;

						if (i >= size) break;
					}
				}
				else {}
		}

		int sampling = 1; // keep as multiple of 2

		int w = ras.getWidth();
		int h = ras.getHeight();

		int newW = w, newH = h;

		if (pX > 0 && pY > 0) {

			int smallestH = pY << 2; // double so comparison works
			int smallestW = pX << 2;

			// cannot be smaller than page
			while (newW > smallestW && newH > smallestH) {
				sampling = sampling << 1;
				newW = newW >> 1;
				newH = newH >> 1;
			}

			int scaleX = w / pX;
			if (scaleX < 1) scaleX = 1;

			int scaleY = h / pY;
			if (scaleY < 1) scaleY = 1;

			// choose smaller value so at least size of page
			sampling = scaleX;
			if (sampling > scaleY) sampling = scaleY;
		}

		// switch to 8 bit and reduce bw image size by averaging
		if (sampling > 1) {

			newW = w / sampling;
			newH = h / sampling;

			int x, y, xx, yy, jj, origLineLength = w;
			try {

				byte[] newData = new byte[newW * newH * comp];

				if (type == 0) origLineLength = w * comp;

				for (y = 0; y < newH; y++) {
					for (x = 0; x < newW; x++) {

						// allow for edges in number of pixels left
						int wCount = sampling, hCount = sampling;
						int wGapLeft = w - x;
						int hGapLeft = h - y;
						if (wCount > wGapLeft) wCount = wGapLeft;
						if (hCount > hGapLeft) hCount = hGapLeft;

						for (jj = 0; jj < comp; jj++) {
							int byteTotal = 0, count = 0;
							// count pixels in sample we will make into a pixel (ie 2x2 is 4 pixels , 4x4 is 16 pixels)
							for (yy = 0; yy < hCount; yy++) {
								for (xx = 0; xx < wCount; xx++) {
									if (type == 0) byteTotal = byteTotal
											+ (buffer[((yy + (y * sampling)) * origLineLength) + (((x * sampling * comp) + (xx * comp) + jj))] & 255);
									else byteTotal = byteTotal
											+ ((intBuffer[((yy + (y * sampling)) * origLineLength) + (x * sampling) + xx] >> (8 * (2 - jj))) & 255);

									count++;
								}
							}

							// set value as white or average of pixels
							if (count > 0) newData[jj + (x * comp) + (newW * y * comp)] = (byte) ((byteTotal) / count);
						}
					}
				}

				int[] bands = new int[comp];
				for (int jj2 = 0; jj2 < comp; jj2++)
					bands[jj2] = jj2;

				ras = Raster.createInterleavedRaster(new DataBufferByte(newData, newData.length), newW, newH, newW * comp, comp, bands, null);

			}
			catch (Exception e) {

				e.printStackTrace();

				if (LogWriter.isOutput()) LogWriter.writeLog("Problem with Image");
			}

		}

		return ras;
	}

	private static BufferedImage cleanupBGRImage(BufferedImage img, int pX, int pY) {

		// hack to fix bug on Linux (also effects Windows 1.5 so disabled)
		if (System.getProperty("java.version").startsWith("1.5")) {
			// if(DecoderOptions.isRunningOnLinux && System.getProperty("java.version").startsWith("1.5")){
			return img;
		}

		Raster ras = img.getData();
		int comp = img.getColorModel().getNumColorComponents();

		byte[] buffer = null;
		int[] intBuffer = null;
		int type;
		DataBuffer data = ras.getDataBuffer();
		if (data instanceof DataBufferInt) type = 1;
		else type = 0;

		if (type == 1) intBuffer = ((DataBufferInt) data).getData();
		else {
			int layerCount = ras.getNumBands();
			if (layerCount == comp) {
				buffer = ((DataBufferByte) data).getData();
			}
			else
				if (layerCount == 1) {
					byte[] rawBuffer = ((DataBufferByte) ras.getDataBuffer()).getData();
					int size = rawBuffer.length;
					int realSize = size * comp;
					int j = 0, i = 0;
					buffer = new byte[realSize];
					while (true) {
						for (int a = 0; a < comp; a++) {
							buffer[j] = rawBuffer[i];
							j++;
						}
						i++;

						if (i >= size) break;
					}
				}
				else {}
		}

		int sampling = 1; // keep as multiple of 2

		int w = ras.getWidth();
		int h = ras.getHeight();

		int newW = w, newH = h;

		if (pX > 0 && pY > 0) {

			int smallestH = pY << 2; // double so comparison works
			int smallestW = pX << 2;

			// cannot be smaller than page
			while (newW > smallestW && newH > smallestH) {
				sampling = sampling << 1;
				newW = newW >> 1;
				newH = newH >> 1;
			}

			int scaleX = w / pX;
			if (scaleX < 1) scaleX = 1;

			int scaleY = h / pY;
			if (scaleY < 1) scaleY = 1;

			// choose smaller value so at least size of page
			sampling = scaleX;
			if (sampling > scaleY) sampling = scaleY;
		}

		// switch to 8 bit and reduce bw image size by averaging
		if (sampling > 1) {

			WritableRaster newRas = ((WritableRaster) ras);

			newW = w / sampling;
			newH = h / sampling;

			int x, y, xx, yy, jj, origLineLength = w;
			try {

				int[] newData = new int[comp];

				if (type == 0) origLineLength = w * comp;

				for (y = 0; y < newH; y++) {
					for (x = 0; x < newW; x++) {

						// allow for edges in number of pixels left
						int wCount = sampling, hCount = sampling;
						int wGapLeft = w - x;
						int hGapLeft = h - y;
						if (wCount > wGapLeft) wCount = wGapLeft;
						if (hCount > hGapLeft) hCount = hGapLeft;

						for (jj = 0; jj < comp; jj++) {
							int byteTotal = 0, count = 0;
							// count pixels in sample we will make into a pixel (ie 2x2 is 4 pixels , 4x4 is 16 pixels)
							for (yy = 0; yy < hCount; yy++) {
								for (xx = 0; xx < wCount; xx++) {
									if (type == 0) byteTotal = byteTotal
											+ (buffer[((yy + (y * sampling)) * origLineLength) + (((x * sampling * comp) + (xx * comp) + jj))] & 255);
									else byteTotal = byteTotal
											+ ((intBuffer[((yy + (y * sampling)) * origLineLength) + (x * sampling) + xx] >> (8 * (2 - jj))) & 255);

									count++;
								}
							}

							// set value as white or average of pixels
							if (count > 0) {
								if (jj == 0) newData[2] = ((byteTotal) / count);
								else
									if (jj == 2) newData[0] = ((byteTotal) / count);
									else newData[jj] = ((byteTotal) / count);

							}

						}

						// write back into ras
						newRas.setPixels(x, y, 1, 1, newData);// changed to setPixels from setPixel for JAVA ME

						// System.out.println(x+"/"+newW+" "+y+"/"+newH+" "+newData[0]+" "+newData[1]);
					}
				}

				// put back data and trim
				img = new BufferedImage(newW, newH, img.getType());
				img.setData(newRas);
				// img=img.getSubimage(0,0,newW,newH); slower so replaced

			}
			catch (Exception e) {

				e.printStackTrace();
				if (LogWriter.isOutput()) LogWriter.writeLog("Problem with Image");
			}

		}

		return img;
	}

	/**
	 * Toms routine to read the image type - you can also use int colorType = decoder.getJPEGDecodeParam().getEncodedColorID();
	 */
	private static int getJPEGTransform(byte[] data) {
		int xform = 0;

		for (int i = 0, imax = data.length - 2; i < imax;) {

			int type = data[i + 1] & 0xff; // want unsigned bytes!
			// out_.println("+"+i+": "+Integer.toHexString(type)/*+", len="+len*/);
			i += 2; // 0xff and type

			if (type == 0x01 || (0xd0 <= type && type <= 0xda)) {

			}
			else
				if (type == 0xda) {
					i = i + ((data[i] & 0xff) << 8) + (data[i + 1] & 0xff);
					while (true) {
						for (; i < imax; i++)
							if ((data[i] & 0xff) == 0xff && data[i + 1] != 0) break;
						int rst = data[i + 1] & 0xff;
						if (0xd0 <= rst && rst <= 0xd7) i += 2;
						else break;
					}

				}
				else {
					/*
					 * if (0xc0 <= type&&type <= 0xcf) { // SOF Nf = data[i+7] & 0xff; // 1, 3=YCbCr, 4=YCCK or CMYK } else
					 */if (type == 0xee) { // Adobe
						if (data[i + 2] == 'A' && data[i + 3] == 'd' && data[i + 4] == 'o' && data[i + 5] == 'b' && data[i + 6] == 'e') {
							xform = data[i + 13] & 0xff;
							break;
						}
					}
					i = i + ((data[i] & 0xff) << 8) + (data[i + 1] & 0xff);
				}
		}

		return xform;
	}

	public void setIndex(byte[] IndexedColorMap, int size) {

		// set the data for an object
		this.IndexedColorMap = IndexedColorMap;
		this.size = size;

		// System.out.println("Set index ="+IndexedColorMap);
	}

	public void setIndex(String CMap, String name, int count) {

		StringBuilder rawValues = new StringBuilder();
		this.size = count;

		// see if hex or octal values and make a lisr
		if (CMap.startsWith("(\\")) {

			// get out the octal values to hex
			StringTokenizer octal_values = new StringTokenizer(CMap, "(\\)");

			while (octal_values.hasMoreTokens()) {
				int next_value = Integer.parseInt(octal_values.nextToken(), 8);
				String hex_value = Integer.toHexString(next_value);
				// pad with 0 if required
				if (hex_value.length() < 2) rawValues.append('0');

				rawValues.append(hex_value);
			}
		}
		else
			if (CMap.startsWith("(")) {

				// should never happen as remapped in ObjectReader

			}
			else {

				// get rest of hex data minus any <>
				if (CMap.startsWith("<")) CMap = CMap.substring(1, CMap.length() - 1).trim();
				rawValues = new StringBuilder(CMap);

			}

		// workout components size
		int total_components = 1;
		if ((name.contains("RGB")) | (name.contains("ICC"))) total_components = 3;
		else
			if (name.contains("CMYK")) total_components = 4;

		this.IndexedColorMap = new byte[(count + 1) * total_components];

		// make sure no spaces in array
		rawValues = Strip.stripAllSpaces(rawValues);

		// put into lookup array
		for (int entries = 0; entries < count + 1; entries++) {
			for (int comp = 0; comp < total_components; comp++) {
				int p = (entries * total_components * 2) + (comp * 2);

				int col_value = Integer.parseInt(rawValues.substring(p, p + 2), 16);
				this.IndexedColorMap[(entries * total_components) + comp] = (byte) col_value;

			}
		}
	}

	/**
	 * lookup a component for index colorspace
	 */
	protected int getIndexedColorComponent(int count) {
		int value = 255;

		if (this.IndexedColorMap != null) {
			value = this.IndexedColorMap[count];

			if (value < 0) value = 256 + value;

		}
		return value;
	}

	/**
	 * return indexed COlorMap
	 */
	public byte[] getIndexedMap() {

		// return IndexedColorMap;
		/**/
		if (this.IndexedColorMap == null) return null;

		int size = this.IndexedColorMap.length;
		byte[] copy = new byte[size];
		System.arraycopy(this.IndexedColorMap, 0, copy, 0, size);

		return copy;
		/**/
	}

	/**
	 * convert color value to sRGB color
	 */
	public void setColor(String[] value, int operandCount) {
	}

	/**
	 * convert color value to sRGB color
	 */
	public void setColor(float[] value, int operandCount) {
	}

	/**
	 * convert byte[] datastream JPEG to an image in RGB
	 */
	public BufferedImage JPEGToRGBImage(byte[] data, int w, int h, float[] decodeArray, int pX, int pY, boolean arrayInverted, PdfObject XObject) {

		BufferedImage image;
		ByteArrayInputStream bis = null;

		try {

			image = this.images.read(data);

			if (image != null && !fasterPNG) {

				if (this.value != ColorSpaces.DeviceGray) // crashes Linux
				image = cleanupImage(image, pX, pY, this.value);

				if (this.value != ColorSpaces.DeviceGray) image = ColorSpaceConvertor.convertToRGB(image);

			}

		}
		catch (Exception ee) {
			image = null;

			if (LogWriter.isOutput()) LogWriter.writeLog("Problem reading JPEG: " + ee);

			ee.printStackTrace();
		}

		if (bis != null) {
			try {
				bis.close();
			}
			catch (IOException e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}

		if (arrayInverted && this.value == ColorSpaces.DeviceGray) {

			DataBufferByte rgb = (DataBufferByte) image.getRaster().getDataBuffer();
			byte[] rawData = rgb.getData();

			for (int aa = 0; aa < rawData.length; aa++) { // flip the bytes
				rawData[aa] = (byte) (rawData[aa] ^ 255);
			}

			image.setData(Raster.createRaster(image.getSampleModel(), new DataBufferByte(rawData, rawData.length), null));
		}

		return image;
	}

	/**
	 * convert byte[] datastream JPEG to an image in RGB
	 * 
	 * @throws PdfException
	 */
	public BufferedImage JPEG2000ToRGBImage(byte[] data, int w, int h, float[] decodeArray, int pX, int pY) throws PdfException {

		BufferedImage image = null;

		ByteArrayInputStream in;
		ImageReader iir;

		try {
			in = new ByteArrayInputStream(data);

			/** 1.4 code */
			// standard java 1.4 IO

			iir = ImageIO.getImageReadersByFormatName("JPEG2000").next();

		}
		catch (Exception ee) {

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

				// does not work correctly if indexed so we manipulate the data
				// if you need to alter check the images on page 42, 53, 71, 80, 89, 98, 107 and 114 are displayed in black
				// infinite/Plumbing_Fixtures_2_V1_replaced.pdf
				byte[] index = getIndexedMap();
				if (index != null && this.value == ColorSpaces.DeviceRGB) {

					data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
					image = ColorSpaceConvertor.convertIndexedToFlat(8, w, h, data, index, false, false);

				}
			}
			catch (Exception e) {

				if (LogWriter.isOutput()) LogWriter.writeLog("Problem reading JPEG 2000: " + e);

				e.printStackTrace();
				image = null;
			}
			finally {
				iir.dispose();
				iin.close();
				in.close();
			}

			if (image != null) {

				image = cleanupImage(image, pX, pY, this.value);

				// ensure white background
				if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
					BufferedImage oldImage = image;
					int newW = image.getWidth();
					int newH = image.getHeight();
					image = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
					Graphics2D g2 = (Graphics2D) image.getGraphics();
					g2.setPaint(Color.WHITE);
					g2.fillRect(0, 0, newW, newH);
					g2.drawImage(oldImage, 0, 0, null);

				}

				image = ColorSpaceConvertor.convertToRGB(image);

			}
		}
		catch (Exception ee) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Problem reading JPEG 2000: " + ee);

			String message = "Exception " + ee + " with JPeg 2000 Image";

			if (!JAIHelper.isJAIused()) message = "JPeg 2000 Images and JAI not setup.\nYou need both JAI and imageio.jar on classpath, "
					+ "and the VM parameter -Dorg.jpedal.jai=true switch turned on";

			throw new PdfException(message);

		}
		catch (Error ee2) {
			ee2.printStackTrace();

			if (LogWriter.isOutput()) LogWriter.writeLog("Problem reading JPEG 2000: " + ee2);

			throw new PdfException("JPeg 2000 Images need both JAI (imageio.jar) on classpath, "
					+ "and the VM parameter -Dorg.jpedal.jai=true switch turned on");

		}

		return image;
	}

	/**
	 * convert color content of data to sRGB data
	 */
	public BufferedImage dataToRGB(byte[] data, int w, int h) {

		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Raster raster = ColorSpaceConvertor.createInterleavedRaster(data, w, h);
		image.setData(raster);

		return image;
	}

	/**
	 * convert image to sRGB image
	 */
	public static BufferedImage BufferedImageToRGBImage(BufferedImage image) {
		return image;
	}

	/** get colorspace ID */
	public int getID() {
		return this.value;
	}

	/**
	 * create a CIE values for conversion to RGB colorspace
	 */
	final public void setCIEValues(float[] W, float[] B, float[] R, float[] Ma, float[] G) {

		/** set to CIEXYZ colorspace */
		this.cs = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);

		// set values
		this.G = G;
		this.Ma = Ma;
		this.W = W;
		// this.B = B;
		this.R = R;
	}

	/**
	 * convert 4 component index to 3
	 */
	final protected byte[] convert4Index(byte[] data) {
		return convertIndex(data, 4);
	}

	/**
	 * convert 4 component index to 3
	 */
	private byte[] convertIndex(byte[] data, int compCount) {

		if (compCount == 4 && this.value == ColorSpaces.DeviceCMYK) {

			int len = data.length;

			byte[] rgb = new byte[len * 3 / 4];
			int j2 = 0;

			for (int ii = 0; ii < len; ii = ii + 4) {

				float[] vals = new float[4];
				for (int j = 0; j < 4; j++)
					vals[j] = (data[ii + j] & 255) / 255f;
				this.setColor(vals, 4);

				int foreground = this.currentColor.getRGB();
				rgb[j2] = (byte) ((foreground >> 16) & 0xFF);
				rgb[j2 + 1] = (byte) ((foreground >> 8) & 0xFF);
				rgb[j2 + 2] = (byte) ((foreground) & 0xFF);

				j2 = j2 + 3;
				if (len - 4 - ii < 4) ii = len;
			}

			return rgb;
		}

		try {

			/** turn it into a BufferedImage so we can convert then extract the data */
			int width = data.length / compCount;
			int height = 1;
			DataBuffer db = new DataBufferByte(data, data.length);
			int[] bands;
			WritableRaster raster;
			DataBuffer convertedData;

			int[] bands4 = { 0, 1, 2, 3 };
			int[] bands3 = { 0, 1, 2 };
			if (compCount == 4) bands = bands4;
			else bands = bands3;

			{
				raster = Raster.createInterleavedRaster(db, width, height, width * compCount, compCount, bands, null);

				if (CSToRGB == null) initCMYKColorspace();
				CSToRGB = new ColorConvertOp(this.cs, rgbCS, ColorSpaces.hints);

				WritableRaster rgbRaster = rgbModel.createCompatibleWritableRaster(width, height);

				CSToRGB.filter(raster, rgbRaster);

				convertedData = rgbRaster.getDataBuffer();

				/** put into byte array */
				int size = width * height * 3;
				data = new byte[size];

				for (int ii = 0; ii < size; ii++) {
					data[ii] = (byte) convertedData.getElem(ii);
				}
			}
		}
		catch (Exception ee) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception  " + ee + " converting colorspace");
		}

		return data;
	}

	/**
	 * convert Index to RGB
	 */
	public byte[] convertIndexToRGB(byte[] index) {
		return index;
	}

	/**
	 * get an xml string with the color info
	 */
	public String getXMLColorToken() {

		String colorToken;

		// only cal if not set
		if (this.c == -1) { // approximate
			if (this.currentColor instanceof Color) {
				Color col = (Color) this.currentColor;
				float c = (255 - col.getRed()) / 255f;
				float m = (255 - col.getGreen()) / 255f;
				float y = (255 - col.getBlue()) / 255f;
				float k = c;
				if (k < m) k = m;
				if (k < y) k = y;

				if (this.pantoneName == null) colorToken = GenericColorSpace.cb + "C='" + c + "' M='" + m + "' Y='" + y + "' K='" + k + "' >";
				else colorToken = GenericColorSpace.cb + "C='" + c + "' M='" + m + "' Y='" + y + "' K='" + k + "' pantoneName='" + this.pantoneName
						+ "' >";
			}
			else {
				colorToken = GenericColorSpace.cb + "type='shading'>";
			}
		}
		else {
			if (this.pantoneName == null) colorToken = GenericColorSpace.cb + "C='" + this.c + "' M='" + this.m + "' Y='" + this.y + "' K='" + this.k
					+ "' >";
			else colorToken = GenericColorSpace.cb + "C='" + this.c + "' M='" + this.m + "' Y='" + this.y + "' K='" + this.k + "' pantoneName='"
					+ this.pantoneName + "' >";
		}

		return colorToken;
	}

	/**
	 * pass in list of patterns
	 */
	public void setPattern(Map patterns, int pageWidth, int pageHeight, float[][] CTM) {

		this.patterns = patterns;

		this.pageWidth = pageWidth;
		this.pageHeight = pageHeight;
		this.CTM = CTM;
		// System.out.println("set pattern called");
	}

	/** used by generic decoder to asign color */
	public void setColor(PdfPaint col) {
		this.currentColor = col;
	}

	/** used by generic decoder to assign color if invisible */
	public void setColorIsTransparent() {
		this.currentColor = new PdfColor(255, 0, 0, 0);
	}

	/** return number of values used for color (ie 3 for rgb) */
	public int getColorComponentCount() {
		return this.componentCount;
	}

	/** pattern colorspace needs access to graphicsState */
	public void setGS(GraphicsState currentGraphicsState) {

		this.gs = currentGraphicsState;
	}

	public void setIntent(String intent) {
		this.intent = intent;
	}

	public String getIntent() {
		return this.intent;
	}

	/** return raw values - only currently works for CMYK */
	public float[] getRawValues() {
		return this.rawValues;
	}

	/**
	 * flag to show if YCCK image decoded so we can draw attention to user
	 * 
	 */
	public boolean isImageYCCK() {
		return this.hasYCCKimages;
	}

	public void setDecodeParms(PdfObject parms) {
		this.decodeParms = parms;
	}

	public boolean isIndexConverted() {
		return this.isConverted;
	}

	private static final int multiplier = 100000;

	public Color getCachedShadingColor(float val) {

		if (this.cache == null) return null;
		else return (Color) this.cache[(int) (val * multiplier)];
	}

	public void setShadedColor(float val, Color col) {

		if (this.cache == null) this.cache = new Object[multiplier + 1];

		this.cache[((int) (val * multiplier))] = col;
	}

	/**
	 * method to flush any caching values for cases where may need resetting (ie in CMYK where restore will render lastvalues in setColor used for
	 * caching invalid
	 */
	public void clearCache() {
	}

	public static ColorSpace getColorSpaceInstance() {

		ColorSpace rgbCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);

		String profile = System.getProperty("org.jpedal.RGBprofile");

		if (profile != null) {
			try {
				rgbCS = new ICC_ColorSpace(ICC_Profile.getInstance(new FileInputStream(profile)));

				System.out.println("use " + profile);
			}
			catch (Exception e) {
				e.printStackTrace();
				if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Problem " + e.getMessage() + " with ICC data ");
			}
		}

		return rgbCS;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GenericColorSpace [");
		if (images != null) {
			builder.append("images=");
			builder.append(images);
			builder.append(", ");
		}
		builder.append("isConverted=");
		builder.append(isConverted);
		builder.append(", ");
		if (intent != null) {
			builder.append("intent=");
			builder.append(intent);
			builder.append(", ");
		}
		if (rawValues != null) {
			builder.append("rawValues=");
			builder.append(Arrays.toString(rawValues));
			builder.append(", ");
		}
		if (patterns != null) {
			builder.append("patterns=");
			builder.append(patterns);
			builder.append(", ");
		}
		if (CTM != null) {
			builder.append("CTM=");
			builder.append(Arrays.toString(CTM));
			builder.append(", ");
		}
		if (inputs != null) {
			builder.append("inputs=");
			builder.append(Arrays.toString(inputs));
			builder.append(", ");
		}
		builder.append("size=");
		builder.append(size);
		builder.append(", c=");
		builder.append(c);
		builder.append(", y=");
		builder.append(y);
		builder.append(", m=");
		builder.append(m);
		builder.append(", k=");
		builder.append(k);
		builder.append(", ");
		if (W != null) {
			builder.append("W=");
			builder.append(Arrays.toString(W));
			builder.append(", ");
		}
		if (G != null) {
			builder.append("G=");
			builder.append(Arrays.toString(G));
			builder.append(", ");
		}
		if (Ma != null) {
			builder.append("Ma=");
			builder.append(Arrays.toString(Ma));
			builder.append(", ");
		}
		if (R != null) {
			builder.append("R=");
			builder.append(Arrays.toString(R));
			builder.append(", ");
		}
		builder.append("value=");
		builder.append(value);
		builder.append(", ");
		if (cs != null) {
			builder.append("cs=");
			builder.append(cs);
			builder.append(", ");
		}
		if (currentColor != null) {
			builder.append("currentColor=");
			builder.append(currentColor);
			builder.append(", ");
		}
		builder.append("failed=");
		builder.append(failed);
		builder.append(", alternative=");
		builder.append(alternative);
		builder.append(", ");
		if (decodeParms != null) {
			builder.append("decodeParms=");
			builder.append(decodeParms);
			builder.append(", ");
		}
		builder.append("hasYCCKimages=");
		builder.append(hasYCCKimages);
		builder.append(", ");
		if (cache != null) {
			builder.append("cache=");
			builder.append(Arrays.toString(cache));
			builder.append(", ");
		}
		builder.append("isPrinting=");
		builder.append(isPrinting);
		builder.append(", r=");
		builder.append(r);
		builder.append(", g=");
		builder.append(g);
		builder.append(", b=");
		builder.append(b);
		builder.append(", ");
		if (IndexedColorMap != null) {
			builder.append("IndexedColorMap=");
			builder.append(Arrays.toString(IndexedColorMap));
			builder.append(", ");
		}
		if (pantoneName != null) {
			builder.append("pantoneName=");
			builder.append(pantoneName);
			builder.append(", ");
		}
		builder.append("componentCount=");
		builder.append(componentCount);
		builder.append(", ");
		if (gs != null) {
			builder.append("gs=");
			builder.append(gs);
			builder.append(", ");
		}
		builder.append("pageWidth=");
		builder.append(pageWidth);
		builder.append(", pageHeight=");
		builder.append(pageHeight);
		builder.append("]");
		return builder.toString();
	}
}
