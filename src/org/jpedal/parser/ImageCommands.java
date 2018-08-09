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
 * ImageCommands.java
 * ---------------
 */
package org.jpedal.parser;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import org.jpedal.color.ColorSpaces;
import org.jpedal.color.GenericColorSpace;
import org.jpedal.constants.PDFImageProcessing;
import org.jpedal.constants.PDFflags;
import org.jpedal.function.FunctionFactory;
import org.jpedal.function.PDFFunction;
import org.jpedal.images.ImageOps;
import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.io.DecryptionFactory;
import org.jpedal.io.PdfFileReader;
import org.jpedal.io.PdfFilteredReader;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.raw.FunctionObject;
import org.jpedal.objects.raw.PdfArrayIterator;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.render.DynamicVectorRenderer;

public class ImageCommands {

	public static final int ID = 0;

	// public static final int TYPE3FONT=1;

	public static final int XOBJECT = 2;

	static boolean sharpenDownsampledImages = false;

	// only show message once
	static boolean JAImessageShow = false;

	public static boolean trackImages = false;

	public static boolean rejectSuperimposedImages = true;

	static {
		String operlapValue = System.getProperty("org.jpedal.rejectsuperimposedimages");
		if (operlapValue != null) ImageCommands.rejectSuperimposedImages = (operlapValue != null && operlapValue.toLowerCase().contains("true"));

		// hidden value to turn on function
		String imgSetting = System.getProperty("org.jpedal.trackImages");
		if (imgSetting != null) trackImages = (imgSetting != null && imgSetting.toLowerCase().contains("true"));

		String nodownsamplesharpen = System.getProperty("org.jpedal.sharpendownsampledimages");
		if (nodownsamplesharpen != null) sharpenDownsampledImages = (nodownsamplesharpen.toLowerCase().contains("true"));
	}

	static BufferedImage addMaskObject(GenericColorSpace decodeColorData, int d, boolean isDCT, boolean isJPX, BufferedImage image, int colorspaceID,
			byte[] index, PdfObject newMask, int optionsApplied, PdfObjectReader currentPdfFile) {
		{

			int[] maskArray = newMask.getIntArray(PdfDictionary.Mask);

			// fix for odd file Customers_June2011/maskIssue.pdf
			if (maskArray != null && image.getWidth() == 1 && image.getHeight() == 1 && maskArray[0] == maskArray[1]) {
				return null;
			}

			// fix for odd file
			if (maskArray != null && maskArray.length == 2 && maskArray[0] == maskArray[1] && maskArray[0] > 0 && index != null
					&& index[maskArray[0]] == 0 && decodeColorData.getIndexedMap().length == 768) {

				// if(index!=null)
				// System.out.println(maskArray[0]+" "+index[maskArray[0]]);

				maskArray = null;
			}

			// see if object or colors
			if (maskArray != null) {

				int colorComponents = decodeColorData.getColorComponentCount();
				// byte[] index=decodeColorData.getIndexedMap();

				if (index != null) {

					int itemCount = maskArray.length, indexValue;
					int[] newIndex = new int[colorComponents * itemCount];
					for (int jj = 0; jj < itemCount; jj++) {
						indexValue = maskArray[jj];
						for (int i = 0; i < colorComponents; i++)
							newIndex[i + (jj * colorComponents)] = index[(indexValue * colorComponents) + i] & 255;
					}

					maskArray = newIndex;
				}

				// just invert the image in this case
				if (image.getType() == BufferedImage.TYPE_BYTE_GRAY && maskArray.length == 6 && maskArray[0] == maskArray[1]
						&& maskArray[2] == maskArray[3] && maskArray[4] == maskArray[5]) {

					DataBufferByte data = (DataBufferByte) (image.getRaster().getDataBuffer());
					byte[] rawImageData = data.getData();
					int length = rawImageData.length;
					for (int ii = 0; ii < length; ii++)
						rawImageData[ii] = (byte) (rawImageData[ii] ^ 255);

					final int[] bands = new int[] { 0 };
					int w = image.getWidth();
					int h = image.getHeight();

					Raster raster = Raster.createInterleavedRaster(new DataBufferByte(rawImageData, length), w, h, w, 1, bands, null);

					image.setData(raster);

				}
				else image = convertPixelsToTransparent(image, maskArray);

			}
			else {

				byte[] objectData = currentPdfFile.readStream(newMask, true, true, false, false, false,
						newMask.getCacheName(currentPdfFile.getObjectReader()));

				int maskW = newMask.getInt(PdfDictionary.Width);
				int maskH = newMask.getInt(PdfDictionary.Height);

				// include Decode if present
				float[] maskDecodeArray = newMask.getFloatArray(PdfDictionary.Decode);

				if (maskDecodeArray != null && (colorspaceID == ColorSpaces.DeviceRGB || colorspaceID == ColorSpaces.Separation)) applyDecodeArray(
						objectData, maskDecodeArray.length / 2, maskDecodeArray, colorspaceID, newMask);

				/** fast op on data to speed up image manipulation */
				int both = PDFImageProcessing.IMAGE_INVERTED + PDFImageProcessing.IMAGE_ROTATED;

				if ((optionsApplied & both) == both) {
					byte[] processedData = ImageOps.rotateImage(objectData, maskW, maskH, 1, 1, null);
					if (processedData != null) {
						int temp = maskH;
						maskH = maskW;
						maskW = temp;
						processedData = ImageOps.rotateImage(processedData, maskW, maskH, d, 1, null);
						if (processedData != null) {
							temp = maskH;
							maskH = maskW;
							maskW = temp;
						}
					}

					objectData = processedData;

				}
				else
					if ((optionsApplied & PDFImageProcessing.IMAGE_INVERTED) == PDFImageProcessing.IMAGE_INVERTED) {// invert at byte level with copy
						objectData = ImageOps.invertImage(objectData, maskW, maskH, 1, 1, null);
					}

				if ((optionsApplied & PDFImageProcessing.IMAGE_ROTATED) == PDFImageProcessing.IMAGE_ROTATED) { // rotate at byte level with copy New
																												// Code still some issues
					objectData = ImageOps.rotateImage(objectData, maskW, maskH, 1, 1, null);
				}

				if (objectData != null) {

					/**
					 * java stroes images in different ways so we need to work out which and handle differently
					 **/
					boolean needsConversion = decodeColorData != null
							&& (decodeColorData.getID() == ColorSpaces.DeviceGray || decodeColorData.getID() == ColorSpaces.CalRGB) && !isJPX;
					boolean isRGB = decodeColorData != null && decodeColorData.getID() == ColorSpaces.DeviceRGB;

					if ((needsConversion && (decodeColorData.getID() == ColorSpaces.DeviceGray || decodeColorData.getID() == ColorSpaces.CalRGB))
							|| (!needsConversion && !isRGB && isDCT)) {

						PdfArrayIterator maskFilters = newMask.getMixedArray(PdfDictionary.Filter);

						// get type as need different handling
						boolean maskNeedsInversion = false;

						int firstMaskValue;
						if (maskFilters != null && maskFilters.hasMoreTokens()) {
							while (maskFilters.hasMoreTokens()) {
								firstMaskValue = maskFilters.getNextValueAsConstant(true);
								maskNeedsInversion = firstMaskValue == PdfFilteredReader.CCITTFaxDecode
										|| firstMaskValue == PdfFilteredReader.JBIG2Decode;
							}
						}

						// need to invert value in this case to make it work
						if (decodeColorData.getID() == ColorSpaces.CalRGB) maskNeedsInversion = !maskNeedsInversion;

						if (!maskNeedsInversion) {
							needsConversion = false;
						}
						else
							if (needsConversion && decodeColorData.getID() == ColorSpaces.DeviceGray) {
								needsConversion = false;
							}

						// needed to make this case work
						if (maskDecodeArray != null && decodeColorData.getID() == ColorSpaces.DeviceGray && maskDecodeArray[0] == 1
								&& maskDecodeArray[1] == 0) needsConversion = !needsConversion;
					}

					image = overlayImage(image, objectData, newMask, needsConversion);
				}
			}
		}
		return image;
	}

	/**
	 * make transparent
	 */
	static BufferedImage makeBlackandWhiteTransparent(BufferedImage image) {

		Raster ras = image.getRaster();

		int w = ras.getWidth();
		int h = ras.getHeight();

		// image=ColorSpaceConvertor.convertToARGB(image);
		BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

		boolean validPixelsFound = false, transparent, isBlack;
		int[] values = new int[3];

		int[] transparentPixel = { 255, 0, 0, 0 };
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {

				// get raw color data
				ras.getPixels(x, y, 1, 1, values);

				// see if white
				transparent = (values[0] > 245 && values[1] > 245 && values[2] > 245);
				isBlack = (values[0] < 10 && values[1] < 10 && values[2] < 10);

				// if it matched replace and move on
				if (transparent || isBlack) {
					newImage.getRaster().setPixels(x, y, 1, 1, transparentPixel);
				}
				else {
					validPixelsFound = true;

					int[] newPixel = new int[4];

					newPixel[3] = 255;
					newPixel[0] = values[0];
					newPixel[1] = values[1];
					newPixel[2] = values[2];

					newImage.getRaster().setPixels(x, y, 1, 1, newPixel);
				}
			}
		}

		if (validPixelsFound) return newImage;
		else return null;
	}

	/**
	 * add MASK to image
	 */
	private static BufferedImage overlayImage(BufferedImage image, byte[] maskData, PdfObject newMask, boolean needsInversion) {

		image = ColorSpaceConvertor.convertToRGB(image);

		Raster ras = image.getRaster();

		int maskW = newMask.getInt(PdfDictionary.Width);
		int maskH = newMask.getInt(PdfDictionary.Height);

		int width = image.getWidth();
		int height = image.getHeight();

		boolean isScaled = (width != maskW || height != maskH);
		float scaling = 0;

		if (isScaled) {
			float scalingW = (float) width / (float) maskW;
			float scalingH = (float) height / (float) maskH;

			if (scalingW > scalingH) scaling = scalingW;
			else scaling = scalingH;
		}

		BufferedImage newImage = new BufferedImage(maskW, maskH, BufferedImage.TYPE_INT_ARGB);

		WritableRaster output = newImage.getRaster();

		// workout y offset (remember needs to be factor of 8)
		int lineBytes = maskW;
		if ((lineBytes & 7) != 0) lineBytes = lineBytes + 8;

		lineBytes = lineBytes >> 3;

		int bytes = 0, x, y;

		final int[] bit = { 128, 64, 32, 16, 8, 4, 2, 1 };

		for (int rawy = 0; rawy < maskH; rawy++) {

			if (isScaled) {
				y = (int) (scaling * rawy);

				if (y > height) y = height;
			}
			else y = rawy;

			boolean isTransparent;
			int xOffset;
			byte b;
			for (int rawx = 0; rawx < maskW; rawx++) {

				if (isScaled) {
					x = (int) (scaling * rawx);

					if (x > width) x = height;
				}
				else x = rawx;

				xOffset = (rawx >> 3);

				b = maskData[bytes + xOffset];

				// invert if needed
				if (needsInversion) isTransparent = (b & bit[rawx & 7]) == 0;
				else isTransparent = (b & bit[rawx & 7]) != 0;

				// if it matched replace and move on
				if (!isTransparent && x < width && y < height) {
					int[] values = new int[3];
					values = ras.getPixel(x, y, values); // get pixel from data
					output.setPixel(rawx, rawy, new int[] { values[0], values[1], values[2], 255 });
				}
			}

			bytes = bytes + lineBytes;
		}

		return newImage;
	}

	/**
	 * add MASK to image by scanning all pixels and seeing if each colour in range
	 */
	private static BufferedImage convertPixelsToTransparent(BufferedImage image, int[] maskArray) {

		// raster we read original pixels from
		Raster ras = image.getRaster();

		// number of colours in image
		int compCount = ras.getNumBands();

		// new image to add non-transparent pixels onto
		image = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {

				int[] values = new int[compCount];
				// get raw color data
				ras.getPixel(x, y, values);

				// assume true and see if false
				boolean isMatch = true;
				for (int aa = 0; aa < compCount; aa++) {
					if (maskArray[2 * aa] <= values[aa] && values[aa] <= maskArray[(2 * aa) + 1]) {}
					else {
						isMatch = false;
						aa = compCount;
					}
				}

				// see if we do not have a match copy through
				if (!isMatch) {

					if (compCount == 1) image.getRaster().setPixel(x, y, new int[] { values[0], values[0], values[0], 255 });
					else image.getRaster().setPixel(x, y, new int[] { values[0], values[1], values[2], 255 });
				}
			}
		}

		return image;
	}

	/**
	 * CMYK overprint mode
	 */
	static BufferedImage simulateOP(BufferedImage image, boolean whiteIs255) {

		Raster ras = image.getRaster();
		image = ColorSpaceConvertor.convertToARGB(image);
		int w = image.getWidth();
		int h = image.getHeight();

		boolean hasNoTransparent = false;// pixelsSet=false;

		// reset
		// minX=w;
		// minY=h;
		// maxX=-1;
		// maxY=-1;

		int[] transparentPixel = { 255, 0, 0, 0 };
		int[] values = new int[4];

		boolean transparent;

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {

				// get raw color data
				ras.getPixel(x, y, values);

				// see if black
				if (whiteIs255) {
					transparent = values[0] > 243 && values[1] > 243 && values[2] > 243;
				}
				else {
					transparent = values[1] < 3 && values[2] < 3 && values[3] < 3;
				}

				// if it matched replace and move on
				if (transparent) {
					image.getRaster().setPixel(x, y, transparentPixel);
				}
				else {
					hasNoTransparent = true;
				}
			}
		}

		if (hasNoTransparent) {
			return image;
		}
		else return null;
	}

	/**
	 * @param maskCol
	 */
	static void getMaskColor(byte[] maskCol, GraphicsState gs) {
		int foreground = gs.nonstrokeColorSpace.getColor().getRGB();
		maskCol[0] = (byte) ((foreground >> 16) & 0xFF);
		maskCol[1] = (byte) ((foreground >> 8) & 0xFF);
		maskCol[2] = (byte) ((foreground) & 0xFF);
	}

	/**
	 * Test whether the data representing a line is uniform along it height
	 */
	static boolean isRepeatingLine(byte[] lineData, int height) {
		if (lineData.length % height != 0) return false;

		int step = lineData.length / height;

		for (int x = 0; x < (lineData.length / height) - 1; x++) {
			int targetIndex = step;
			while (targetIndex < lineData.length - 1) {
				if (lineData[x] != lineData[targetIndex]) {
					return false;
				}
				targetIndex += step;
			}
		}
		return true;
	}

	/**
	 * apply soft mask
	 **/
	public static BufferedImage applySmask(BufferedImage image, BufferedImage smask, PdfObject newSMask, boolean isForm, boolean isRGB,
			PdfObject ColorSpace, PdfObject XObject, GraphicsState gs) {

		// get type as need different handling
		PdfArrayIterator maskFilters = XObject.getMixedArray(PdfDictionary.Filter);

		boolean imageIsDCT = false;// ,maskIsJPX=false;

		int firstValue;
		if (maskFilters != null && maskFilters.hasMoreTokens()) {
			while (maskFilters.hasMoreTokens()) {
				firstValue = maskFilters.getNextValueAsConstant(true);
				imageIsDCT = firstValue == PdfFilteredReader.DCTDecode;
				// isJPX=firstValue==PdfFilteredReader.JPXDecode;
			}
		}

		boolean invert = !imageIsDCT && gs.CTM[1][1] > 0 && gs.CTM[0][0] < 0;

		int imageType = image.getType();

		int[] gray = { 0 };
		int[] val = { 0, 0, 0, 0 };
		int[] transparentPixel = { 0, 0, 0, 0 };

		// get type as need different handling
		maskFilters = newSMask.getMixedArray(PdfDictionary.Filter);

		boolean maskIsDCT = false;// ,maskIsJPX=false;

		if (maskFilters != null && maskFilters.hasMoreTokens()) {
			while (maskFilters.hasMoreTokens()) {
				firstValue = maskFilters.getNextValueAsConstant(true);
				maskIsDCT = firstValue == PdfFilteredReader.DCTDecode;
				// isJPX=firstValue==PdfFilteredReader.JPXDecode;
			}
		}

		boolean isDeviceGray = ColorSpace != null && ColorSpace.getParameterConstant(PdfDictionary.ColorSpace) == ColorSpaces.DeviceGray;
		boolean needsConversion = (maskIsDCT) && isDeviceGray;

		int type = -1;
		if (ColorSpace != null) type = ColorSpace.getParameterConstant(PdfDictionary.ColorSpace);

		needsConversion = !needsConversion && isForm && ColorSpace != null && (type == ColorSpaces.DeviceCMYK || type == ColorSpaces.ICC);

		// fix for Smask encoded with DCTDecode but not JPX
		if (needsConversion) {
			smask = ColorSpaceConvertor.convertColorspace(smask, BufferedImage.TYPE_BYTE_GRAY);
			val = gray;
		}

		if (smask == null) {
			return image;
		}

		Raster mask = smask.getRaster();
		WritableRaster imgRas = null;

		boolean isConverted = false;

		/**
		 * allow for scaled mask
		 */
		int imageW = image.getWidth();
		int imageH = image.getHeight();

		int smaskW = smask.getWidth();
		int smaskH = smask.getHeight();
		float ratioW = 0, ratioH = 0;

		if (imageW != smaskW || imageH != smaskH) {
			ratioW = (float) imageW / (float) smaskW;
			ratioH = (float) imageH / (float) smaskH;

			// resize if half size to improve image quality on RGB
			if (isRGB && ratioW == 0.5 && ratioH == 0.5) {

				BufferedImage resizedImage = new BufferedImage(smaskW, smaskH, image.getType());
				Graphics2D g = resizedImage.createGraphics();

				g.dispose();
				g.setComposite(AlphaComposite.Src);
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				// g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

				g.drawImage(image, 0, 0, smaskW, smaskH, null);

				image = resizedImage;

				imageW = smaskW;
				imageH = smaskH;
				ratioW = 1;
				ratioH = 1;
			}

		}

		int colorComponents = smask.getColorModel().getNumComponents();
		int[] values = new int[colorComponents];
		int[] pix = new int[4];

		// apply smask
		int line, maskH = mask.getHeight();
		for (int y = 0; y < imageH; y++) {
			for (int x = 0; x < imageW; x++) {

				if (invert && maskH >= imageH) line = maskH - y - 1;
				else line = y;

				// get raw color data
				if (ratioW == 0) mask.getPixels(x, line, 1, 1, values);
				else mask.getPixels((int) (x / ratioW), (int) (line / ratioH), 1, 1, values);

				// see if we have a match (assume it matches)
				boolean noMatch = false;

				// test assumption
				if (colorComponents == 1) { // hack to filter out DCTDecode stream
					if (values[0] > 127 && maskIsDCT) noMatch = true;
				}
				else {

					for (int comp = 0; comp < colorComponents; comp++) {
						if (values[comp] != val[comp]) {
							comp = colorComponents;
							noMatch = true;
						}
					}
				}

				// if it matched replace and move on
				if (!noMatch) {
					if (!isConverted) { // do it first time needed
						image = ColorSpaceConvertor.convertToARGB(image);
						imgRas = image.getRaster();
						isConverted = true;
					}

					// handle 8bit gray, not DCT
					if (colorComponents == 1) {

						imgRas.getPixels(x, y, 1, 1, pix);

						// remove what appears invisible in Acrobat
						if (values[0] == pix[0]) {// pix[0]>32 && pix[1]>32 && pix[2]>32 && values[0]<32)
							if (pix[0] == 255 && (imageType == 1 || imageType == 5 || imageType == 10) && isDeviceGray) { // white is different in
																															// diff Buffered images
								imgRas.setPixels(x, y, 1, 1, new int[] { (pix[0]), (pix[1]), (pix[2]), values[0] });
							}
							else {
								imgRas.setPixels(x, y, 1, 1, transparentPixel);
							}

						}
						else imgRas.setPixels(x, y, 1, 1, new int[] { (pix[0]), (pix[1]), (pix[2]), values[0] });
					}
					else imgRas.setPixels(x, y, 1, 1, transparentPixel);
				}
			}
		}

		return image;
	}

	static BufferedImage simulateOverprint(GenericColorSpace decodeColorData, byte[] data, boolean isDCT, boolean isJPX, BufferedImage image,
			int colorspaceID, PdfObject newMask, PdfObject newSMask, DynamicVectorRenderer current, GraphicsState gs) {

		// simulate overPrint //currentGraphicsState.getNonStrokeOP() &&
		if ((colorspaceID == ColorSpaces.DeviceCMYK || colorspaceID == ColorSpaces.ICC) && gs.getOPM() == 1.0f) {
			// if((colorspaceID==ColorSpaces.DeviceCMYK || colorspaceID==ColorSpaces.ICC) && gs.getOPM()==1.0f){

			// try to keep as binary if possible
			boolean isBlank = false;

			// indexed colors
			byte[] index = decodeColorData.getIndexedMap();

			// see if allblack
			if (index == null && current.hasObjectsBehind(gs.CTM)) {

				isBlank = true; // assume true and disprove
				for (int ii = 0; ii < data.length; ii++) {
					// if(index!=null){
					// int colUsed=(data[ii] &255)*3;
					// if(colorspaceID+2<index.length && index[colUsed]==0 && index[colUsed+1]==0 && index[colUsed+2]==0){
					// ii=data.length;
					// isBlank=false;
					// }
					// }else
					if (data[ii] != 0) {
						ii = data.length;
						isBlank = false;
					}
				}
			}

			// if so reject
			if (isBlank) {
				image.flush();
				image = null;

			}
			else
				if ((isDCT || isJPX) && gs.getNonStrokeOP()) {
					image = ImageCommands.simulateOP(image, isJPX);
				}
				else
					if (isDCT && newSMask == null && newMask == null
							&& (gs.nonstrokeColorSpace.getColor().getRGB() == -16777216 || gs.nonstrokeColorSpace.getColor().getRGB() == -1)
							&& decodeColorData.isImageYCCK() && decodeColorData.getIntent() != null
							&& decodeColorData.getIntent().equals("RelativeColorimetric")) {
						image = ImageCommands.simulateOP(image, true);
					}
					else
						if (gs.getNonStrokeOP()) {
							if (colorspaceID == ColorSpaces.DeviceCMYK) image = ImageCommands.simulateOP(image, false);// image.getType()==1);
							else image = ImageCommands.simulateOP(image, image.getType() == 1);
						}
		}

		return image;
	}

	static BufferedImage addBackgroundToMask(BufferedImage image, boolean isMask) {

		if (isMask) {

			int cw = image.getWidth();
			int ch = image.getHeight();

			BufferedImage background = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = background.createGraphics();
			g2.setColor(Color.white);
			g2.fillRect(0, 0, cw, ch);
			g2.drawImage(image, 0, 0, null);
			image = background;

		}
		return image;
	}

	/**
	 * apply TR
	 */
	static BufferedImage applyTR(BufferedImage image, PdfObject TR, PdfObjectReader currentPdfFile) {

		/**
		 * get TR function first
		 **/
		PDFFunction[] functions = new PDFFunction[4];

		int total = 0;

		byte[][] kidList = TR.getKeyArray(PdfDictionary.TR);

		if (kidList != null) total = kidList.length;

		// get functions
		for (int count = 0; count < total; count++) {

			if (kidList[count] == null) continue;

			String ref = new String(kidList[count]);
			PdfObject Function = new FunctionObject(ref);

			// handle /Identity as null or read
			byte[] possIdent = kidList[count];
			if (possIdent != null && possIdent.length > 4 && possIdent[0] == 47 && possIdent[1] == 73 && possIdent[2] == 100 && possIdent[3] == 101) // (/Identity
			Function = null;
			else currentPdfFile.readObject(Function);

			/** setup the translation function */
			if (Function != null) functions[count] = FunctionFactory.getFunction(Function, currentPdfFile);

		}

		/**
		 * apply colour transform
		 */
		Raster ras = image.getRaster();
		// image=ColorSpaceConvertor.convertToARGB(image);

		int[] values = new int[4];

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {

				// get raw color data
				ras.getPixels(x, y, 1, 1, values);

				for (int a = 0; a < 3; a++) {
					float[] raw = { values[a] / 255f };

					if (functions[a] != null) {
						float[] processed = functions[a].compute(raw);

						values[a] = (int) (255 * processed[0]);
					}
				}

				image.getRaster().setPixels(x, y, 1, 1, values);
			}
		}

		return image;
	}

	/**
	 * apply DecodeArray
	 */
	static void applyDecodeArray(byte[] data, int d, float[] decodeArray, int type, PdfObject XObject) {

		int count = decodeArray.length;

		int maxValue = 0;
		for (float aDecodeArray : decodeArray) {
			if (maxValue < aDecodeArray) maxValue = (int) aDecodeArray;
		}

		/**
		 * see if will not change output and ignore if unnecessary
		 */
		boolean isIdentify = true; // assume true and disprove
		int compCount = decodeArray.length;

		for (int comp = 0; comp < compCount; comp = comp + 2) {
			if ((decodeArray[comp] != 0.0f) || ((decodeArray[comp + 1] != 1.0f) && (decodeArray[comp + 1] != 255.0f))) {
				isIdentify = false;
				comp = compCount;
			}
		}

		if (isIdentify) return;

		if (d == 1) { // bw straight switch (ignore gray)

			// changed for /baseline_screens/Customers-Dec2011/Jones contract for Dotloop.pdf
			if (decodeArray[0] > decodeArray[1]) {

				// if(type!=ColorSpaces.DeviceGray){// || (decodeArray[0]>decodeArray[1] && XObject instanceof MaskObject)){
				int byteCount = data.length;
				for (int ii = 0; ii < byteCount; ii++) {
					data[ii] = (byte) ~data[ii];

				}
			}

			/**
			 * handle rgb
			 */
		}
		else
			if ((d == 8 && maxValue > 1) && (type == ColorSpaces.DeviceRGB || type == ColorSpaces.CalRGB || type == ColorSpaces.DeviceCMYK)) {

				int j = 0;

				for (int ii = 0; ii < data.length; ii++) {
					int currentByte = (data[ii] & 0xff);
					if (currentByte < decodeArray[j]) currentByte = (int) decodeArray[j];
					else
						if (currentByte > decodeArray[j + 1]) currentByte = (int) decodeArray[j + 1];

					j = j + 2;
					if (j == decodeArray.length) j = 0;
					data[ii] = (byte) currentByte;
				}
			}
			else {
				/**
				 * apply array
				 * 
				 * Assumes black and white or gray colorspace
				 * */
				maxValue = (d << 1);
				int divisor = maxValue - 1;

				for (int ii = 0; ii < data.length; ii++) {
					byte currentByte = data[ii];

					int dd = 0;
					int newByte = 0;
					int min = 0, max = 1;
					for (int bits = 7; bits > -1; bits--) {
						int current = (currentByte >> bits) & 1;

						current = (int) (decodeArray[min] + (current * ((decodeArray[max] - decodeArray[min]) / (divisor))));

						/** check in range and set */
						if (current > maxValue) current = maxValue;
						if (current < 0) current = 0;

						current = ((current & 1) << bits);

						newByte = newByte + current;

						// rotate around array
						dd = dd + 2;

						if (dd == count) {
							dd = 0;
							min = 0;
							max = 1;
						}
						else {
							min = min + 2;
							max = max + 2;
						}
					}

					data[ii] = (byte) newByte;

				}
			}
	}

	static boolean isExtractionAllowed(PdfObjectReader currentPdfFile) {

		PdfFileReader objectReader = currentPdfFile.getObjectReader();

		DecryptionFactory decryption = objectReader.getDecryptionObject();

		return decryption == null || decryption.getBooleanValue(PDFflags.IS_EXTRACTION_ALLOWED);
	}

}
