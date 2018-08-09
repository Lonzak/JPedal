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
 * ImageDecoder.java
 * ---------------
 */
package org.jpedal.parser;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import org.jpedal.PdfDecoder;
import org.jpedal.color.ColorSpaces;
import org.jpedal.color.ColorspaceFactory;
import org.jpedal.color.DeviceCMYKColorSpace;
import org.jpedal.color.DeviceGrayColorSpace;
import org.jpedal.color.DeviceRGBColorSpace;
import org.jpedal.color.GenericColorSpace;
import org.jpedal.constants.PDFImageProcessing;
import org.jpedal.exception.PdfException;
import org.jpedal.external.ImageHandler;
import org.jpedal.images.ImageOps;
import org.jpedal.images.ImageTransformer;
import org.jpedal.images.ImageTransformerDouble;
import org.jpedal.images.SamplingFactory;
import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.io.IDObjectDecoder;
import org.jpedal.io.JAIHelper;
import org.jpedal.io.ObjectStore;
import org.jpedal.io.ObjectUtils;
import org.jpedal.io.PdfFilteredReader;
import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.PdfImageData;
import org.jpedal.objects.PdfPageData;
import org.jpedal.objects.raw.MaskObject;
import org.jpedal.objects.raw.PdfArrayIterator;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.objects.raw.XObject;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.render.RenderUtils;
import org.jpedal.utils.LogWriter;

public class ImageDecoder extends BaseDecoder {

	PdfImageData pdfImages = null;

	private boolean getSamplingOnly = false;

	/** flag to show if image transparent */
	boolean isMask = true;

	String imagesInFile = null;

	boolean isPrinting;

	ImageHandler customImageHandler;

	boolean useHiResImageForDisplay;

	boolean isType3Font;

	boolean renderDirectly;

	int formLevel;

	PdfPageData pageData;

	ObjectStore objectStoreStreamRef;

	/** flag to show raw images extracted */
	boolean clippedImagesExtracted = true;

	private boolean extractRawCMYK = false;

	/** flag to show raw images extracted */
	boolean finalImagesExtracted = true;

	private boolean doNotRotate = false;

	/**
	 * flag to show if we physical generate a scaled version of the images extracted
	 */
	boolean createScaledVersion = true;

	/** flag to show content is being rendered */
	boolean renderImages = false;

	/** flag to show raw images extracted */
	boolean rawImagesExtracted = true;

	// used internally to show optimisations
	private int optionsApplied = PDFImageProcessing.NOTHING;

	/** name of current image in pdf */
	private String currentImage = "";

	private String formName;

	public ImageDecoder(ImageHandler customImageHandler, ObjectStore objectStoreStreamRef, boolean renderDirectly, PdfImageData pdfImages,
			int formLevel, PdfPageData pageData, String imagesInFile, String formName) {

		this.formName = formName;

		this.customImageHandler = customImageHandler;
		this.objectStoreStreamRef = objectStoreStreamRef;
		this.renderDirectly = renderDirectly;

		this.pdfImages = pdfImages;
		this.formLevel = formLevel;
		this.pageData = pageData;

		this.imagesInFile = imagesInFile;
	}

	private GenericColorSpace setupXObjectColorspace(PdfObject XObject, int depth, int width, int height, byte[] objectData) {

		PdfObject ColorSpace = XObject.getDictionary(PdfDictionary.ColorSpace);

		// handle colour information
		GenericColorSpace decodeColorData = new DeviceRGBColorSpace();

		if (ColorSpace != null) {
			decodeColorData = ColorspaceFactory.getColorSpaceInstance(this.currentPdfFile, ColorSpace, this.cache.XObjectColorspaces);

			decodeColorData.setPrinting(this.isPrinting);

			// track colorspace use
			this.cache.put(PdfObjectCache.ColorspacesUsed, decodeColorData.getID(), "x");

			if (depth == 1 && decodeColorData.getID() == ColorSpaces.DeviceRGB && XObject.getDictionary(PdfDictionary.Mask) == null) {

				byte[] data = decodeColorData.getIndexedMap();

				// no index or first colour is white so use grayscale
				if (decodeColorData.getIndexedMap() == null || (data.length == 6 && data[0] == 0 && data[1] == 0 && data[2] == 0)) decodeColorData = new DeviceGrayColorSpace();
			}
		}

		// fix for odd itext file (/PDFdata/baseline_screens/debug3/Leistung.pdf)
		byte[] indexData = decodeColorData.getIndexedMap();
		if (depth == 8 && indexData != null && decodeColorData.getID() == ColorSpaces.DeviceRGB && width * height == objectData.length) {

			PdfObject newMask = XObject.getDictionary(PdfDictionary.Mask);
			if (newMask != null) {

				int[] maskArray = newMask.getIntArray(PdfDictionary.Mask);

				// this specific case has all zeros
				if (maskArray != null && maskArray.length == 2 && maskArray[0] == 255 && maskArray[0] == maskArray[1]
						&& decodeColorData.getIndexedMap() != null && decodeColorData.getIndexedMap().length == 768) {

					// see if index looks corrupt (ie all zeros) We exit as soon as we have disproved
					boolean isCorrupt = true;
					for (int jj = 0; jj < 768; jj++) {
						if (indexData[jj] != 0) {
							isCorrupt = false;
							jj = 768;
						}
					}

					if (isCorrupt) decodeColorData = new DeviceGrayColorSpace();
				}
			}
		}

		// pass through decode params
		PdfObject parms = XObject.getDictionary(PdfDictionary.DecodeParms);
		if (parms != null) decodeColorData.setDecodeParms(parms);

		// set any intent
		decodeColorData.setIntent(XObject.getName(PdfDictionary.Intent));

		return decodeColorData;
	}

	public BufferedImage processImageXObject(PdfObject XObject, String image_name, byte[] objectData, boolean saveRawData, String details)
			throws PdfException {

		boolean imageMask;

		BufferedImage image = null;

		// add filename to make it unique
		image_name = this.fileName + '-' + image_name;

		int depth = 1;
		int width = XObject.getInt(PdfDictionary.Width);
		int height = XObject.getInt(PdfDictionary.Height);
		int newDepth = XObject.getInt(PdfDictionary.BitsPerComponent);
		if (newDepth != PdfDictionary.Unknown) depth = newDepth;

		this.isMask = XObject.getBoolean(PdfDictionary.ImageMask);
		imageMask = this.isMask;

		GenericColorSpace decodeColorData = setupXObjectColorspace(XObject, depth, width, height, objectData);

		// tell user and log
		if (LogWriter.isOutput()) LogWriter.writeLog("Processing XObject: " + image_name + ' ' + XObject.getObjectRefAsString() + " width=" + width
				+ " Height=" + height + " Depth=" + depth + " colorspace=" + decodeColorData);

		/**
		 * allow user to process image
		 */
		if (this.customImageHandler != null) image = this.customImageHandler.processImageData(this.gs, XObject);

		/**
		 * fix for add case where image blank and actual image on SMask (customer-June2011/10664.pdf)
		 */
		PdfObject newSMask = XObject.getDictionary(PdfDictionary.SMask);
		byte[] index = decodeColorData.getIndexedMap();
		if (newSMask != null && index != null && index.length == 3 && decodeColorData.getID() != ColorSpaces.ICC) { // swap out the image with
																													// inverted SMask if empty

			XObject = newSMask;
			XObject.setFloatArray(PdfDictionary.Decode, new float[] { 1, 0 });
			objectData = this.currentPdfFile.readStream(XObject, true, true, false, false, false, null);

			depth = 1;
			width = XObject.getInt(PdfDictionary.Width);
			height = XObject.getInt(PdfDictionary.Height);
			newDepth = XObject.getInt(PdfDictionary.BitsPerComponent);
			if (newDepth != PdfDictionary.Unknown) depth = newDepth;

			decodeColorData = setupXObjectColorspace(XObject, depth, width, height, objectData);
		}

		// deal with special case of 1x1 pixel backed onto large inverted Smask which would be very slow in Generic code
		// see (Customers-dec2011/grayscale.pdf)
		if (newSMask != null && XObject.getInt(PdfDictionary.Width) == 1 && XObject.getInt(PdfDictionary.Height) == 1
				&& XObject.getInt(PdfDictionary.BitsPerComponent) == 8) { // swap out the image with inverted SMask if empty

			byte opacity = (byte) 255;

			XObject = newSMask;

			/**
			 * get opacity if not default
			 */
			float[] maskDecode = XObject.getFloatArray(PdfDictionary.Decode);
			if (maskDecode != null) {
				opacity = (byte) ((maskDecode[0]) * 255);
			}

			// use black/white colour
			byte[] maskIndex = new byte[] { 0, 0, 0, (byte) 255, (byte) 255, (byte) 255 };

			// get raw 1 bit data for smask
			byte[] data = this.currentPdfFile.readStream(XObject, true, true, false, false, false, null);

			// get dimensions
			width = XObject.getInt(PdfDictionary.Width);
			height = XObject.getInt(PdfDictionary.Height);

			// buffer for byte data
			int length = width * height * 4;
			objectData = new byte[length];

			// create ARGB data from 1bit data and opacity
			ColorSpaceConvertor.flatten1bpc(width, data, maskIndex, true, length, opacity, objectData);

			// build image
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			DataBuffer db = new DataBufferByte(objectData, objectData.length);
			WritableRaster raster = Raster.createInterleavedRaster(db, width, height, width * 4, 4, new int[] { 0, 1, 2, 3 }, null);
			image.setData(raster);

		}

		// extract and process the image
		else
			if (this.customImageHandler == null || (image == null && !this.customImageHandler.alwaysIgnoreGenericHandler())) image = processImage(
					decodeColorData, objectData, image_name, width, height, depth, imageMask, XObject, saveRawData, ImageCommands.XOBJECT);

		// add details to string so we can pass back
		if (ImageCommands.trackImages && image != null && details != null) {

			// work out effective dpi
			float dpi = this.gs.CTM[0][0];
			if (dpi == 0) dpi = this.gs.CTM[0][1];
			if (dpi < 0) dpi = -dpi;

			dpi = (int) (width / dpi * 100);

			// add details to string
			StringBuilder imageInfo = new StringBuilder(details);
			imageInfo.append(" w=");
			imageInfo.append(String.valueOf(width));
			imageInfo.append(" h=");
			imageInfo.append(String.valueOf(height));
			imageInfo.append(' ');
			imageInfo.append(String.valueOf((int) dpi));
			imageInfo.append(' ');
			imageInfo.append(ColorSpaces.IDtoString(decodeColorData.getID()));

			imageInfo.append(" (");
			imageInfo.append(String.valueOf(image.getWidth()));
			imageInfo.append(' ');
			imageInfo.append(String.valueOf(image.getHeight()));
			imageInfo.append(" type=");
			imageInfo.append(String.valueOf(image.getType()));
			imageInfo.append(')');

			if (this.imagesInFile.length() == 0) this.imagesInFile = imageInfo.toString();
			else {
				imageInfo.append('\n');
				imageInfo.append(this.imagesInFile);
				this.imagesInFile = imageInfo.toString();
			}

		}

		return image;
	}

	/**
	 * process image in XObject (XForm handled in PdfStreamDecoder)
	 */
	public int processDOImage(String name, int dataPointer, PdfObject XObject) throws PdfException {

		// name is not unique if in form so we add form level to separate out
		if (this.formLevel > 0) name = this.formName + '_' + this.formLevel + '_' + name;

		// set if we need
		String key = null;
		if (ImageCommands.rejectSuperimposedImages) {
			key = ((int) this.gs.CTM[2][0]) + "-" + ((int) this.gs.CTM[2][1]) + '-' + ((int) this.gs.CTM[0][0]) + '-' + ((int) this.gs.CTM[1][1])
					+ '-' + ((int) this.gs.CTM[0][1]) + '-' + ((int) this.gs.CTM[1][0]);
		}

		try {
			processXImage(name, name, key, XObject);
		}
		catch (Error e) {

			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Error: " + e.getMessage());

			this.imagesProcessedFully = false;
			this.errorTracker.addPageFailureMessage("Error " + e + " in DO");
		}
		catch (Exception e) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e);

			this.imagesProcessedFully = false;
			this.errorTracker.addPageFailureMessage("Error " + e + " in DO");

			if (e.getMessage().contains("JPeg 2000")) {
				throw new RuntimeException("XX JPeg 2000 Images needs the VM parameter -Dorg.jpedal.jai=true switch turned on");
			}
		}

		return dataPointer;
	}

	private void processXImage(String name, String details, String key, PdfObject XObject) throws PdfException {

		final int previousUse = -1;

		if (ImageCommands.trackImages) {
			details = details + " Image";
			if (this.imagesInFile == null) this.imagesInFile = "";
		}

		boolean isForHTML = ((this.current.getType() == DynamicVectorRenderer.CREATE_HTML
				|| this.current.getType() == DynamicVectorRenderer.CREATE_SVG || this.current.getType() == DynamicVectorRenderer.CREATE_JAVAFX));

		/** don't process unless needed */
		if (this.renderImages || this.finalImagesExtracted || this.clippedImagesExtracted || this.rawImagesExtracted) {

			// read stream for image
			byte[] objectData;
			if (previousUse == -1) {

				objectData = this.currentPdfFile.readStream(XObject, true, true, false, false, false,
						XObject.getCacheName(this.currentPdfFile.getObjectReader()));

				// flag issue
				if (objectData == null) this.imagesProcessedFully = false;
			}

			if (objectData != null || previousUse > 0) {

				boolean alreadyCached = false;// (useHiResImageForDisplay && current.isImageCached(this.pageNum));

				BufferedImage image = null;

				// generate name including filename to make it unique less /
				this.currentImage = this.fileName + '-' + name;

				// process the image and save raw version
				if (!alreadyCached && previousUse == -1) {

					// last flag change from true to false to fix issue
					image = processImageXObject(XObject, name, objectData, true, details);

				}

				// fix for oddity in Annotation
				if (image != null && image.getWidth() == 1 && image.getHeight() == 1 && this.isType3Font) {
					image.flush();
					image = null;
				}

				// save transformed image
				if (image != null || alreadyCached || previousUse > 0) {

					// manipulate CTM to allow for image truncated
					float[][] savedCMT = null;

					if (!isForHTML && (this.renderDirectly || this.useHiResImageForDisplay || previousUse > 0)) {

						this.gs.x = this.gs.CTM[2][0];
						this.gs.y = this.gs.CTM[2][1];

						/** save details if we are tracking */
						if (this.finalImagesExtracted || this.rawImagesExtracted) {
							int w = (int) Math.abs(this.gs.CTM[0][0]);
							if (w == 0) w = (int) Math.abs(this.gs.CTM[0][1]);

							int h = (int) Math.abs(this.gs.CTM[1][1]);
							if (h == 0) h = (int) Math.abs(this.gs.CTM[1][0]);

							this.pdfImages.setImageInfo(this.currentImage, this.pageNum, this.gs.x, this.gs.x, w, h);
						}

						if (this.renderDirectly) { // in own bit as other code not needed
							this.current.drawImage(this.pageNum, image, this.gs, alreadyCached, name, this.optionsApplied, -1);
						}
						else
							if (image != null || alreadyCached || previousUse > 0) {

								int id = this.current.drawImage(this.pageNum, image, this.gs, alreadyCached, name, this.optionsApplied, previousUse);

								/**
								 * delete previous used if this not transparent
								 */
								/**
								 * ignore multiple overlapping images
								 */
								// if (ImageCommands.rejectSuperimposedImages && image != null && image.getType() != BufferedImage.TYPE_INT_ARGB) {
								//
								// Object lastRef = cache.getImposedKey(key);
								//
								// //delete under image....
								// if (lastRef != null && gs.getClippingShape() == null) //limit to avoid issues on other files
								// current.flagImageDeleted((Integer) lastRef);
								// }

								/**
								 * store last usage in case it reappears unless it is transparent
								 */
								if (ImageCommands.rejectSuperimposedImages && key != null) this.cache.setImposedKey(key, id);
							}
					}
					else {

						if (this.clippedImagesExtracted || isForHTML) {
							generateTransformedImage(image, name);
						}
						else {
							try {
								generateTransformedImageSingle(image, name);
							}
							catch (Exception e) {

								if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " on transforming image in file");
							}
						}
					}

					if (image != null) image.flush();

					// restore
					if (savedCMT != null) this.gs.CTM = savedCMT;
				}
			}
		}
	}

	ImageDecoder() {}

	public ImageDecoder(ImageHandler customImageHandler, boolean useHiResImageForDisplay, ObjectStore objectStoreStreamRef, boolean renderDirectly,
			PdfImageData pdfImages, int formLevel, PdfPageData pageData) {

		this.customImageHandler = customImageHandler;
		this.useHiResImageForDisplay = useHiResImageForDisplay;
		this.objectStoreStreamRef = objectStoreStreamRef;
		this.renderDirectly = renderDirectly;

		this.pdfImages = pdfImages;
		this.formLevel = formLevel;
		this.pageData = pageData;
	}

	public void setSamplingOnly(boolean getSamplingOnly) {

		this.getSamplingOnly = getSamplingOnly;
	}

	public String getImagesInFile() {
		return this.imagesInFile;
	}

	public void setParameters(boolean isPageContent, boolean renderPage, int renderMode, int extractionMode, boolean isPrinting, boolean isType3Font,
			boolean useHiResImageForDisplay) {

		this.isPageContent = isPageContent;
		this.renderPage = renderPage;
		this.renderMode = renderMode;
		this.extractionMode = extractionMode;

		this.isPrinting = isPrinting;

		this.isType3Font = isType3Font;

		this.useHiResImageForDisplay = useHiResImageForDisplay;

		this.renderImages = renderPage && (renderMode & PdfDecoder.RENDERIMAGES) == PdfDecoder.RENDERIMAGES;

		this.finalImagesExtracted = (extractionMode & PdfDecoder.FINALIMAGES) == PdfDecoder.FINALIMAGES;

		this.extractRawCMYK = (extractionMode & PdfDecoder.CMYKIMAGES) == PdfDecoder.CMYKIMAGES;

		this.clippedImagesExtracted = (extractionMode & PdfDecoder.CLIPPEDIMAGES) == PdfDecoder.CLIPPEDIMAGES;

		this.rawImagesExtracted = (extractionMode & PdfDecoder.RAWIMAGES) == PdfDecoder.RAWIMAGES;

		this.createScaledVersion = this.finalImagesExtracted || this.renderImages;
	}

	public void setImageName(String currentImage) {
		this.currentImage = currentImage;
	}

	public int getOptionsApplied() {
		return this.optionsApplied;
	}

	public int processIDImage(int dataPointer, int startInlineStream, byte[] stream, int tokenNumber) throws Exception {

		/**
		 * read Dictionary
		 */
		PdfObject XObject = new XObject(PdfDictionary.ID);

		IDObjectDecoder objectDecoder = new IDObjectDecoder(this.currentPdfFile.getObjectReader());
		objectDecoder.setEndPt(dataPointer - 2);

		objectDecoder.readDictionaryAsObject(XObject, startInlineStream, stream);

		BufferedImage image = null;

		boolean inline_imageMask;

		// store pointer to current place in file
		int inline_start_pointer = dataPointer + 1, i_w, i_h, i_bpc;

		// find end of stream
		int i = inline_start_pointer, streamLength = stream.length;

		// find end
		while (true) {
			// look for end EI

			// handle Pdflib variety
			if (streamLength - i > 3 && stream[i + 1] == 69 && stream[i + 2] == 73 && stream[i + 3] == 10) break;

			// general case
			if ((streamLength - i > 3) && (stream[i] == 32 || stream[i] == 10 || stream[i] == 13 || (stream[i + 3] == 32 && stream[i + 4] == 'Q'))
					&& (stream[i + 1] == 69) && (stream[i + 2] == 73) && (stream[i + 3] == 32 || stream[i + 3] == 10 || stream[i + 3] == 13)) break;

			i++;

			if (i == streamLength) break;
		}

		if (this.renderImages || this.finalImagesExtracted || this.clippedImagesExtracted || this.rawImagesExtracted) {

			// load the data
			// generate the name including file name to make it unique
			String image_name = this.fileName + "-IN-" + tokenNumber;

			int endPtr = i;
			// hack for odd files
			if (i < stream.length && stream[endPtr] != 32 && stream[endPtr] != 10 && stream[endPtr] != 13) endPtr++;

			// correct data (ie idoq/FC1100000021259.pdf )
			if (stream[inline_start_pointer] == 10) inline_start_pointer++;

			/**
			 * put image data in array
			 */
			byte[] i_data = new byte[endPtr - inline_start_pointer];
			System.arraycopy(stream, inline_start_pointer, i_data, 0, endPtr - inline_start_pointer);

			// System.out.print(">>");
			// for(int ss=inline_start_pointer-5;ss<endPtr+15;ss++)
			// System.out.print((char)stream[ss]);
			// System.out.println("<<"+i_data.length+" end="+endPtr);
			// pass in image data
			XObject.setStream(i_data);

			/**
			 * work out colorspace
			 */
			PdfObject ColorSpace = XObject.getDictionary(PdfDictionary.ColorSpace);

			// check for Named value
			if (ColorSpace != null) {
				String colKey = ColorSpace.getGeneralStringValue();

				if (colKey != null) {
					Object col = this.cache.get(PdfObjectCache.Colorspaces, colKey);

					if (col != null) ColorSpace = (PdfObject) col;
					else {
						// throw new RuntimeException("error with "+colKey+" on ID "+colorspaces);
					}
				}
			}

			// if(ColorSpace!=null && ColorSpace.getParameterConstant(PdfDictionary.ColorSpace)==PdfDictionary.Unknown)
			// ColorSpace=null; //no values set

			/**
			 * allow user to process image
			 */
			if (this.customImageHandler != null) image = this.customImageHandler.processImageData(this.gs, XObject);

			PdfArrayIterator filters = XObject.getMixedArray(PdfDictionary.Filter);

			// check not handled elsewhere
			int firstValue;
			boolean needsDecoding = false;
			if (filters != null && filters.hasMoreTokens()) {
				firstValue = filters.getNextValueAsConstant(false);

				needsDecoding = (firstValue != PdfFilteredReader.JPXDecode && firstValue != PdfFilteredReader.DCTDecode);
			}

			i_w = XObject.getInt(PdfDictionary.Width);
			i_h = XObject.getInt(PdfDictionary.Height);
			i_bpc = XObject.getInt(PdfDictionary.BitsPerComponent);
			inline_imageMask = XObject.getBoolean(PdfDictionary.ImageMask);

			// handle filters (JPXDecode/DCT decode is handle by process image)
			if (needsDecoding) {
				PdfFilteredReader filter = new PdfFilteredReader();
				i_data = filter.decodeFilters(ObjectUtils.setupDecodeParms(XObject, this.currentPdfFile.getObjectReader()), i_data, filters, i_w,
						i_h, null);
			}

			// handle colour information
			GenericColorSpace decodeColorData = new DeviceRGBColorSpace();
			if (ColorSpace != null) {
				decodeColorData = ColorspaceFactory.getColorSpaceInstance(this.currentPdfFile, ColorSpace);
				decodeColorData.setPrinting(this.isPrinting);

				// track colorspace use
				this.cache.put(PdfObjectCache.ColorspacesUsed, decodeColorData.getID(), "x");

				// use alternate as preference if CMYK
				// if(newColorSpace.getID()==ColorSpaces.ICC && ColorSpace.getParameterConstant(PdfDictionary.Alternate)==ColorSpaces.DeviceCMYK)
				// newColorSpace=new DeviceCMYKColorSpace();
			}
			if (i_data != null) {

				if (this.customImageHandler == null || (image == null && !this.customImageHandler.alwaysIgnoreGenericHandler())) {

					image = processImage(decodeColorData, i_data, image_name, i_w, i_h, i_bpc, inline_imageMask, XObject, false, ImageCommands.ID);

					// generate name including filename to make it unique
					this.currentImage = image_name;

				}

				// skip if smaller than zero as work around for JPS bug
				if (this.isPrinting && image != null && this.gs != null && image.getHeight() == 1 && this.gs.CTM[1][1] < 1) {
					image = null;
				}

				if (image != null) {

					if ((this.current.getType() == DynamicVectorRenderer.CREATE_HTML || this.current.getType() == DynamicVectorRenderer.CREATE_SVG || this.current
							.getType() == DynamicVectorRenderer.CREATE_JAVAFX)) {
						generateTransformedImage(image, image_name);
					}
					else
						if (this.renderDirectly || this.useHiResImageForDisplay) {// || current.getType()==
																					// org.jpedal.render.output.html.HTMLDisplay.CREATE_HTML){

							this.gs.x = this.gs.CTM[2][0];
							this.gs.y = this.gs.CTM[2][1];

							this.current.drawImage(this.pageNum, image, this.gs, false, image_name, this.optionsApplied, -1);
						}
						else {
							if (this.clippedImagesExtracted) generateTransformedImage(image, image_name);
							else generateTransformedImageSingle(image, image_name);
						}

					if (image != null) image.flush();
				}
			}
		}

		dataPointer = i + 3;

		return dataPointer;
	}

	/**
	 * save the current image, clipping and resizing. This gives us a clipped hires copy. In reparse, we don't need to repeat some actions we know
	 * already done.
	 */
	public void generateTransformedImage(BufferedImage image, String image_name) {

		float x = 0, y = 0, w, h;

		// if valid image then process
		if (image != null) {

			/**
			 * scale the raw image to correct page size (at 72dpi)
			 */

			// object to scale and clip. Creating instance does the scaling
			ImageTransformerDouble image_transformation = new ImageTransformerDouble(this.gs, image, this.createScaledVersion);

			// extract images either scaled/clipped or scaled then clipped

			image_transformation.doubleScaleTransformShear(this.current);

			// get intermediate image and save
			image = image_transformation.getImage();

			// save the scaled/clipped version of image if allowed
			{// if(currentPdfFile.isExtractionAllowed()){

				/** make sure the right way */
				/*
				 * int dx=1,dy=1,iw=0,ih=0; if(currentGraphicsState.CTM[0][0]<0){ dx=-dx; iw=image.getWidth(); } if(currentGraphicsState.CTM[1][1]<0){
				 * dy=-dy; ih=image.getHeight(); } if((dy<0)|(dx<0)){ AffineTransform image_at =new AffineTransform(); image_at.scale(dx,dy);
				 * image_at.translate(-iw,-ih); AffineTransformOp invert= new AffineTransformOp(image_at, ColorSpaces.hints); image =
				 * invert.filter(image,null); }
				 */

				if ((this.current.getType() == DynamicVectorRenderer.CREATE_HTML || this.current.getType() == DynamicVectorRenderer.CREATE_SVG || this.current
						.getType() == DynamicVectorRenderer.CREATE_JAVAFX)) {

				}
				else {
					String image_type = this.objectStoreStreamRef.getImageType(this.currentImage);
					if (image_type == null) image_type = "tif";

					if (this.objectStoreStreamRef.saveStoredImage("CLIP_" + this.currentImage, ImageCommands.addBackgroundToMask(image, this.isMask),
							false, false, image_type)) this.errorTracker.addPageFailureMessage("Problem saving " + image);
				}
			}

			/**
			 * HTML5/JavaFX code is piggybacking on existing functionality to extract hires clipped image. We do not need other functionality so we
			 * ignore that
			 */
			if ((this.current.getType() == DynamicVectorRenderer.CREATE_HTML || this.current.getType() == DynamicVectorRenderer.CREATE_SVG || this.current
					.getType() == DynamicVectorRenderer.CREATE_JAVAFX)) {

				if (image != null) {
					this.gs.x = x;
					this.gs.y = y;
					this.current.drawImage(this.pageNum, image, this.gs, false, image_name, this.optionsApplied, -1);
				}

			}
			else {

				if (this.finalImagesExtracted || this.renderImages) image_transformation.doubleScaleTransformScale();

				// complete the image and workout co-ordinates
				image_transformation.completeImage();

				// get initial values
				x = image_transformation.getImageX();
				y = image_transformation.getImageY();
				w = image_transformation.getImageW();
				h = image_transformation.getImageH();

				// get final image to allow for way we draw 'upside down'
				image = image_transformation.getImage();

				// allow for null image returned (ie if too small)
				if (image != null) {

					// store final image on disk & in memory
					if (this.renderImages || this.finalImagesExtracted || this.clippedImagesExtracted || this.rawImagesExtracted) {
						this.pdfImages.setImageInfo(this.currentImage, this.pageNum, x, y, w, h);
					}

					// add to screen being drawn
					if ((this.renderImages || !this.isPageContent) && image != null) {
						this.gs.x = x;
						this.gs.y = y;
						this.current.drawImage(this.pageNum, image, this.gs, false, image_name, this.optionsApplied, -1);
					}

					/** save if required */
					if (!this.renderDirectly && this.isPageContent && this.finalImagesExtracted) {

						// save the scaled/clipped version of image if allowed
						if (ImageCommands.isExtractionAllowed(this.currentPdfFile)) {
							String image_type = this.objectStoreStreamRef.getImageType(this.currentImage);
							this.objectStoreStreamRef.saveStoredImage(this.currentImage, ImageCommands.addBackgroundToMask(image, this.isMask),
									false, false, image_type);
						}
					}
				}
			}
		}
		else
			if (LogWriter.isOutput())
			// flag no image and reset clip
			LogWriter.writeLog("NO image written");
	}

	/**
	 * save the current image, clipping and resizing. Id reparse, we don't need to repeat some actions we know already done.
	 */
	public void generateTransformedImageSingle(BufferedImage image, String image_name) {

		float x, y, w, h;

		// if valid image then process
		if (image != null) {

			// get clipped image and co-ords
			Area clipping_shape = this.gs.getClippingShape();

			/**
			 * scale the raw image to correct page size (at 72dpi)
			 */
			// object to scale and clip. Creating instance does the scaling
			ImageTransformer image_transformation;

			// object to scale and clip. Creating instance does the scaling
			image_transformation = new ImageTransformer(this.gs, image, true);

			// get initial values
			x = image_transformation.getImageX();
			y = image_transformation.getImageY();
			w = image_transformation.getImageW();
			h = image_transformation.getImageH();

			// get back image, which will become null if TOO small
			image = image_transformation.getImage();

			// apply clip as well if exists and not inline image
			if (image != null && this.customImageHandler != null && clipping_shape != null && clipping_shape.getBounds().getWidth() > 1
					&& clipping_shape.getBounds().getHeight() > 1 && !this.customImageHandler.imageHasBeenScaled()) {

				// see if clip is wider than image and ignore if so
				boolean ignore_image = clipping_shape.contains(x, y, w, h);

				if (!ignore_image) {
					// do the clipping
					image_transformation.clipImage(clipping_shape);

					// get ALTERED values
					x = image_transformation.getImageX();
					y = image_transformation.getImageY();
					w = image_transformation.getImageW();
					h = image_transformation.getImageH();
				}
			}

			// alter image to allow for way we draw 'upside down'
			image = image_transformation.getImage();

			// allow for null image returned (ie if too small)
			if (image != null) {

				/** turn correct way round if needed */
				// if((currentGraphicsState.CTM[0][1]!=0 )&&(currentGraphicsState.CTM[1][0]!=0 )&&(currentGraphicsState.CTM[0][0]>=0 )){

				/*
				 * if((currentGraphicsState.CTM[0][1]>0 )&&(currentGraphicsState.CTM[1][0]>0 )&&(currentGraphicsState.CTM[0][0]>=0 )){ double
				 * dx=1,dy=1,scaleX=0,scaleY=0; if(currentGraphicsState.CTM[0][1]>0){ dx=-1; scaleX=image.getWidth(); }
				 * if(currentGraphicsState.CTM[1][0]>0){ dy=-1; scaleY=image.getHeight(); } AffineTransform image_at =new AffineTransform();
				 * image_at.scale(dx,dy); image_at.translate(-scaleX,-scaleY); AffineTransformOp invert= new AffineTransformOp(image_at,
				 * ColorSpaces.hints); image = invert.filter(image,null); }
				 */
				// store final image on disk & in memory
				if (this.finalImagesExtracted || this.rawImagesExtracted) {
					this.pdfImages.setImageInfo(this.currentImage, this.pageNum, x, y, w, h);

					// if(includeImagesInData){
					//
					// float xx=x;
					// float yy=y;
					//
					// if(clipping_shape!=null){
					//
					// int minX=(int)clipping_shape.getBounds().getMinX();
					// int maxX=(int)clipping_shape.getBounds().getMaxX();
					//
					// int minY=(int)clipping_shape.getBounds().getMinY();
					// int maxY=(int)clipping_shape.getBounds().getMaxY();
					//
					// if((xx>0 && xx<minX)||(xx<0))
					// xx=minX;
					//
					// float currentW=xx+w;
					// if(xx<0)
					// currentW=w;
					// if(maxX<(currentW))
					// w=maxX-xx;
					//
					// if(yy>0 && yy<minY)
					// yy=minY;
					//
					// if(maxY<(yy+h))
					// h=maxY-yy;
					//
					// }
					//
					// pdfData.addImageElement(xx,yy,w,h,currentImage);
					// }
				}
				// add to screen being drawn
				if (this.renderImages || !this.isPageContent) {

					// check it is not null
					if (image != null) {
						this.gs.x = x;
						this.gs.y = y;
						this.current.drawImage(this.pageNum, image, this.gs, false, image_name, this.optionsApplied, -1);

					}
				}

				/** save if required */
				if (this.isPageContent && this.finalImagesExtracted) {

					// save the scaled/clipped version of image if allowed
					if (ImageCommands.isExtractionAllowed(this.currentPdfFile)) {

						String image_type = this.objectStoreStreamRef.getImageType(this.currentImage);
						this.objectStoreStreamRef.saveStoredImage(this.currentImage, ImageCommands.addBackgroundToMask(image, this.isMask), false,
								false, image_type);
					}
				}
			}
		}
		else
			if (LogWriter.isOutput())
			// flag no image and reset clip
			LogWriter.writeLog("NO image written");
	}

	/**
	 * read in the image and process and save raw image
	 */
	BufferedImage processImage(GenericColorSpace decodeColorData, byte[] data, String name, int w, int h, int d, boolean imageMask,
			PdfObject XObject, boolean saveRawData, int mode) throws PdfException {

		// track its use
		this.cache.put(PdfObjectCache.ColorspacesUsed, decodeColorData.getID(), "x");

		int rawd = d;

		int sampling = 1, newW, newH;

		float[] decodeArray = XObject.getFloatArray(PdfDictionary.Decode);

		if (LogWriter.debug && decodeArray != null) {
			String val = "";
			for (float aDecodeArray : decodeArray)
				val = val + ' ' + aDecodeArray;
		}

		PdfArrayIterator Filters = XObject.getMixedArray(PdfDictionary.Filter);

		boolean isDCT = false, isJPX = false;
		// check not handled elsewhere
		int firstValue;
		if (Filters != null && Filters.hasMoreTokens()) {
			while (Filters.hasMoreTokens()) {
				firstValue = Filters.getNextValueAsConstant(true);
				isDCT = firstValue == PdfFilteredReader.DCTDecode;
				isJPX = firstValue == PdfFilteredReader.JPXDecode;
			}

		}
		else Filters = null;

		boolean removed = false, isDownsampled = false;

		BufferedImage image = null;
		String type = "jpg";

		int colorspaceID = decodeColorData.getID();

		int compCount = decodeColorData.getColorSpace().getNumComponents();

		int pX = 0, pY = 0;

		/** setup any imageMask */
		byte[] maskCol = new byte[4];
		if (imageMask) ImageCommands.getMaskColor(maskCol, this.gs);

		byte[] index = decodeColorData.getIndexedMap();

		/** setup sub-sampling */
		if (this.renderPage && this.streamType != ValueTypes.PATTERN && !this.current.avoidDownSamplingImage()) {

			if (this.isPrinting && SamplingFactory.isPrintDownsampleEnabled && w < 4000) {
				pX = this.pageData.getCropBoxWidth(this.pageNum) * 4;
				pY = this.pageData.getCropBoxHeight(this.pageNum) * 4;

			}
			else
				if (SamplingFactory.downsampleLevel == SamplingFactory.high || this.getSamplingOnly) {// && w>500 && h>500){ // ignore small items

					// ensure all positive for comparison
					float[][] CTM = new float[3][3];
					for (int ii = 0; ii < 3; ii++) {
						for (int jj = 0; jj < 3; jj++) {
							if (this.gs.CTM[ii][jj] < 0) CTM[ii][jj] = -this.gs.CTM[ii][jj];
							else CTM[ii][jj] = this.gs.CTM[ii][jj];
						}
					}

					if (CTM[0][0] == 0 || CTM[0][0] < CTM[0][1]) pX = (int) (CTM[0][1]);
					else pX = (int) (CTM[0][0]);

					if (CTM[1][1] == 0 || CTM[1][1] < CTM[1][0]) pY = (int) (CTM[1][0]);
					else pY = (int) (CTM[1][1]);

					// don't bother on small itemsS
					if (!this.getSamplingOnly && (w < 500 || (h < 600 && (w < 1000 || isJPX)))) { // change??

						pX = 0;// pageData.getCropBoxWidth(this.pageNum);
						pY = 0;// pageData.getCropBoxHeight(this.pageNum);
					}

				}
				else
					if (SamplingFactory.downsampleLevel == SamplingFactory.medium) {
						pX = this.pageData.getCropBoxWidth(this.pageNum);
						pY = this.pageData.getCropBoxHeight(this.pageNum);
					}
		}

		/**
		 * turn off all scaling and allow user to control if switched off or HTML/svg/JavaFX (we still trap very large images in these cases as they
		 * blow away the memory footprint)
		 */
		final int maxHTMLImageSize = 4000;
		if (this.current.avoidDownSamplingImage()
				|| (w < maxHTMLImageSize && h < maxHTMLImageSize && (this.current.getType() == DynamicVectorRenderer.CREATE_HTML
						|| this.current.getType() == DynamicVectorRenderer.CREATE_SVG || this.current.getType() == DynamicVectorRenderer.CREATE_JAVAFX))) {
			pX = -1;
			pY = -1;
		}

		/**
		 * }else if((current.getType()== OutputDisplay.CREATE_HTML || current.getType()== OutputDisplay.CREATE_SVG || current.getType()==
		 * OutputDisplay.CREATE_JAVAFX)){
		 * 
		 * //float wScale=Math.abs(gs.CTM[0][0]/w); //float hScale=Math.abs(gs.CTM[1][1]/h);
		 * 
		 * //ignore silly figures if(h>4000 && w>4000){// ||wScale<0.1f || hScale<0.1f){ pX = -1; pY=-1;
		 * 
		 * //System.out.println(w+" "+h+" "+name+" "+gs.CTM[0][0]+" "+gs.CTM[1][1]+" "+wScale+" "+hScale); }
		 * 
		 * }
		 * 
		 * /
		 **/

		// needs to be factored in or images poor on hires modes
		if ((isDCT || isJPX) && this.multiplyer > 1) {

			pX = (int) (pX * this.multiplyer);
			pY = (int) (pY * this.multiplyer);
		}

		PdfObject DecodeParms = XObject.getDictionary(PdfDictionary.DecodeParms), newMask, newSMask;

		newMask = XObject.getDictionary(PdfDictionary.Mask);
		newSMask = XObject.getDictionary(PdfDictionary.SMask);

		// avoid for scanned text
		if (d == 1 && (newSMask != null || XObject.getObjectType() != PdfDictionary.Mask) && decodeColorData.getID() == ColorSpaces.DeviceGray
				&& h < 300) {

			// System.out.println("XObject="+XObject.getObjectType());
			// System.out.println("newSMask="+newSMask);
			pX = 0;
			pY = 0;
		}

		// flag masks
		if ((newMask != null || newSMask != null) && LogWriter.isOutput()) LogWriter.writeLog("newMask= " + newMask + " newSMask=" + newSMask);

		// work out if inverted (assume true and disprove)
		// work this into saved data @mariusz so 125% works
		boolean arrayInverted = false;
		if (decodeArray != null) {

			arrayInverted = true;
			int count = decodeArray.length;
			for (int aa = 0; aa < count; aa = aa + 2) {
				if (decodeArray[aa] == 1f && decodeArray[aa + 1] == 0f) {
					// okay
				}
				else {
					arrayInverted = false;
					aa = count;
				}
			}
		}

		/**
		 * also needs to be inverted in this case see Customers3/Architectural_Specification.pdf page 31 onwards 20100816 - no longer seems needed and
		 * removed by MS to fix abacus file (see Rog email 13th august 2010)
		 */
		// if(!arrayInverted && decodeColorData.getID()==ColorSpaces.DeviceGray && index!=null){// && index[0]==-1 && index[1]==-1 && index[2]==-1){
		// arrayInverted=true;
		// }

		/**
		 * down-sample size if displaying (some cases excluded at present)
		 */
		if (this.renderPage && newMask == null && decodeColorData.getID() != ColorSpaces.ICC
				&& (arrayInverted || decodeArray == null || decodeArray.length == 0) && (d == 1 || d == 8) && pX > 0 && pY > 0
				&& (SamplingFactory.isPrintDownsampleEnabled || !this.isPrinting)) {
			// @speed - debug

			// see what we could reduce to and still be big enough for page
			newW = w;
			newH = h;

			// limit size (allow bigger grayscale
			if (this.multiplyer <= 1 && !this.isPrinting) {

				int maxAllowed = 1000;
				if (decodeColorData.getID() == ColorSpaces.DeviceGray) {
					maxAllowed = 4000;
				}
				if (pX > maxAllowed) pX = maxAllowed;
				if (pY > maxAllowed) pY = maxAllowed;
			}

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

		// get sampling and exit from this code as we don't need to go further
		if (this.getSamplingOnly) {// && pX>0 && pY>0){

			float scaleX = (((float) w) / pX);
			float scaleY = (((float) h) / pY);

			if (scaleX < scaleY) {
				this.samplingUsed = scaleX;
			}
			else {
				this.samplingUsed = scaleY;
			}
			// we may need to check mask as well

			boolean checkMask = false;
			if (newSMask != null) {

				/** read the stream */
				byte[] objectData = this.currentPdfFile.readStream(newSMask, true, true, false, false, false,
						newSMask.getCacheName(this.currentPdfFile.getObjectReader()));

				if (objectData != null) {

					if (DecodeParms == null) DecodeParms = newSMask.getDictionary(PdfDictionary.DecodeParms);

					int maskW = newSMask.getInt(PdfDictionary.Width);
					int maskH = newSMask.getInt(PdfDictionary.Height);

					// if all white image with mask, use mask
					boolean isDownscaled = maskW / 2 > w && maskH / 2 > h;

					boolean ignoreMask = isDownscaled && DecodeParms != null && DecodeParms.getInt(PdfDictionary.Colors) != -1
							&& DecodeParms.getInt(PdfDictionary.Predictor) != 15;

					// ignoreMask is hack to fix odd Visuality files
					if (!ignoreMask) checkMask = true;
				}

			}

			if (!checkMask) {

				// getSamplingOnly=false;

				return null;
			}
		}

		{

			if (sampling > 1 && this.multiplyer > 1) {

				// samplingUsed= sampling;

				sampling = (int) (sampling / this.multiplyer);

			}

			// switch to 8 bit and reduce bw image size by averaging
			if (sampling > 1) {

				isDownsampled = true;

				newW = w / sampling;
				newH = h / sampling;

				boolean saveData = false;
				// flatten out high res raw data in this case so we can store and resample (see deebug3/DOC002.PDF and DOC003.PDF
				if (imageMask && w > 2000 && h > 2000 && d == 1 && decodeColorData.getID() == ColorSpaces.DeviceRGB && this.gs.CTM[0][0] > 0
						&& this.gs.CTM[1][1] > 0) {
					saveData = true;
				}

				if (d == 1 && (decodeColorData.getID() != ColorSpaces.DeviceRGB || index == null)) {

					// save raw 1 bit data
					// code in DynamicVectorRenderer may need alignment if it changes
					// 20090929 - re-enabled by Mark with deviceGray limit for Abacus files
					// breaks if form rotated so only use at top level
					// (sample file breaks so we added this as hack for fattura 451-10 del 31.10.10.pdf in customers3)
					if (this.formLevel < 2 && (saveData || (!imageMask && saveRawData && decodeColorData.getID() == ColorSpaces.DeviceGray))) {

						// copy and turn upside down first
						int count = data.length;

						byte[] turnedData = new byte[count];
						System.arraycopy(data, 0, turnedData, 0, count);

						// turnedData=ImageOps.invertImage(turnedData, w, h, d, 1, null);

						boolean isInverted = !saveData && !this.doNotRotate && (this.renderDirectly || this.useHiResImageForDisplay)
								&& RenderUtils.isInverted(this.gs.CTM);
						boolean isRotated = !saveData && !this.doNotRotate && (this.renderDirectly || this.useHiResImageForDisplay)
								&& RenderUtils.isRotated(this.gs.CTM);

						if (this.renderDirectly) {
							isInverted = false;
							isRotated = false;
						}

						if (isRotated) { // rotate at byte level with copy New Code still some issues
							turnedData = ImageOps.rotateImage(turnedData, w, h, d, 1, index);

							// reset
							int temp = h;
							h = w;
							w = temp;

							temp = pX;
							pX = pY;
							pY = temp;

						}

						if (isInverted) // invert at byte level with copy
						turnedData = ImageOps.invertImage(turnedData, w, h, d, 1, index);

						// invert all the bits if needed before we store
						if (arrayInverted) {
							for (int aa = 0; aa < count; aa++)
								turnedData[aa] = (byte) (turnedData[aa] ^ 255);
						}

						// cache if binary image (not Mask)
						if (decodeColorData.getID() == ColorSpaces.DeviceRGB && d == 1 && maskCol != null) { // avoid cases like Hand_test/DOC028.PDF

						}
						else
							if (((w < 4000 && h < 4000) || decodeColorData.getID() == ColorSpaces.DeviceGray) && !(XObject instanceof MaskObject)) { // limit
																																						// added
																																						// after
																																						// silly
																																						// sizes
																																						// on
																																						// Customers3/1773_A2.pdf

								// Integer pn = new Integer(this.pageNum);
								// Integer iC = new Integer(imageCount);
								String key = this.pageNum + String.valueOf(this.imageCount);

								if (saveData) {
									this.current.getObjectStore().saveRawImageData(key, turnedData, w, h, pX, pY, maskCol, decodeColorData.getID());
								}
								else {
									this.current.getObjectStore().saveRawImageData(key, turnedData, w, h, pX, pY, null, decodeColorData.getID());
								}
							}

						if (isRotated) {
							// reset
							int temp = h;
							h = w;
							w = temp;

							temp = pX;
							pX = pY;
							pY = temp;
						}
					}

					// make 1 bit indexed flat
					if (index != null) index = decodeColorData.convertIndexToRGB(index);

					int size = newW * newH;

					if (imageMask) {
						size = size * 4;
						maskCol[3] = (byte) 255;
					}
					else
						if (index != null) size = size * 3;

					byte[] newData = new byte[size];

					final int[] flag = { 1, 2, 4, 8, 16, 32, 64, 128 };

					int origLineLength = (w + 7) >> 3;

					int bit;
					byte currentByte;

					// scan all pixels and down-sample
					for (int y = 0; y < newH; y++) {
						for (int x = 0; x < newW; x++) {

							int bytes = 0, count = 0;

							// allow for edges in number of pixels left
							int wCount = sampling, hCount = sampling;
							int wGapLeft = w - x;
							int hGapLeft = h - y;
							if (wCount > wGapLeft) wCount = wGapLeft;
							if (hCount > hGapLeft) hCount = hGapLeft;

							// count pixels in sample we will make into a pixel (ie 2x2 is 4 pixels , 4x4 is 16 pixels)
							int ptr;
							for (int yy = 0; yy < hCount; yy++) {
								for (int xx = 0; xx < wCount; xx++) {

									ptr = ((yy + (y * sampling)) * origLineLength) + (((x * sampling) + xx) >> 3);

									if (ptr < data.length) {
										currentByte = data[ptr];
									}
									else {
										currentByte = 0;
									}

									if (imageMask && !arrayInverted) currentByte = (byte) (currentByte ^ 255);

									bit = currentByte & flag[7 - (((x * sampling) + xx) & 7)];

									if (bit != 0) bytes++;
									count++;
								}
							}

							// set value as white or average of pixels
							int offset = x + (newW * y);

							if (count > 0) {
								if (imageMask) {
									// System.out.println("xx");
									for (int ii = 0; ii < 4; ii++) {
										if (arrayInverted) newData[(offset * 4) + ii] = (byte) (255 - (((maskCol[ii] & 255) * bytes) / count));
										else newData[(offset * 4) + ii] = (byte) ((((maskCol[ii] & 255) * bytes) / count));
										// System.out.println(newData[(offset*4)+ii]+" "+(byte)(((maskCol[ii] & 255)*bytes)/count);

									}
								}
								else
									if (index != null && decodeColorData.getID() == ColorSpaces.Separation && 1 == 2) {

										for (int ii = 0; ii < 3; ii++)
											if ((bytes / count) > 0.5f) {
												newData[(offset * 3) + ii] = (byte) (((index[3 + ii] & 255)));
											}
											else {
												newData[(offset * 3) + ii] = (byte) (((index[ii] & 255)));

											}

									}
									else
										if (index != null && d == 1) {
											int av;

											for (int ii = 0; ii < 3; ii++) {

												// can be in either order so look at index
												if (index[0] == -1 && index[1] == -1 && index[2] == -1) {
													av = (index[ii] & 255) + (index[ii + 3] & 255);
													newData[(offset * 3) + ii] = (byte) (255 - ((av * bytes) / count));
												}
												else {// if(decodeColorData.getID()==ColorSpaces.DeviceCMYK){ //avoid color 'smoothing' - see
														// CustomersJune2011/lead base paint.pdf
													float ratio = bytes / count;
													if (ratio > 0.5) newData[(offset * 3) + ii] = index[ii + 3];
													else newData[(offset * 3) + ii] = index[ii];

												}
											}
										}
										else
											if (index != null) {
												for (int ii = 0; ii < 3; ii++)
													newData[(offset * 3) + ii] = (byte) (((index[ii] & 255) * bytes) / count);
											}
											else newData[offset] = (byte) ((255 * bytes) / count);
							}
							else {

								if (imageMask) {
									for (int ii = 0; ii < 3; ii++)
										newData[(offset * 4) + ii] = (byte) 0;

								}
								else
									if (index != null) {
										for (int ii = 0; ii < 3; ii++)
											newData[((offset) * 3) + ii] = 0;
									}
									else newData[offset] = (byte) 255;
							}
						}
					}

					data = newData;

					if (index != null) compCount = 3;

					h = newH;
					w = newW;
					decodeColorData.setIndex(null, 0);

					// remap Separation as already converted here
					if (decodeColorData.getID() == ColorSpaces.Separation) {
						decodeColorData = new DeviceRGBColorSpace();

						// needs to have these settings if 1 bit not indexed
						if (d == 1 && index == null) {
							compCount = 1;

							int count = data.length;
							for (int aa = 0; aa < count; aa++)
								data[aa] = (byte) (data[aa] ^ 255);
						}
					}

					d = 8;

					// imageMask=false;

				}
				else
					if (d == 8 && (Filters == null || (!isDCT && !isJPX))) {

						boolean hasIndex = decodeColorData.getIndexedMap() != null
								&& (decodeColorData.getID() == ColorSpaces.DeviceRGB || decodeColorData.getID() == ColorSpaces.CalRGB
										|| decodeColorData.getID() == ColorSpaces.DeviceCMYK || decodeColorData.getID() == ColorSpaces.ICC);

						int oldSize = data.length;

						int x = 0, y = 0, xx = 0, yy = 0, jj = 0, comp = 0, origLineLength = 0, indexCount = 1;
						try {

							if (hasIndex) { // convert to sRGB
								comp = 1;

								compCount = 3;
								indexCount = 3;
								index = decodeColorData.convertIndexToRGB(index);

								decodeColorData.setIndex(null, 0);
							}
							else {
								comp = decodeColorData.getColorComponentCount();
							}

							// black and white
							if (w * h == oldSize || decodeColorData.getID() == ColorSpaces.DeviceGray) comp = 1;

							byte[] newData;
							if (hasIndex) { // hard-coded to 3 values
								newData = new byte[newW * newH * indexCount];
								origLineLength = w;
							}
							else {
								newData = new byte[newW * newH * comp];
								origLineLength = w * comp;
							}
							// System.err.println(w+" "+h+" "+data.length+" comp="+comp+" scaling="+sampling+" "+decodeColorData);

							// System.err.println("size="+w*h*comp+" filter"+filter+" scaling="+sampling+" comp="+comp);
							// System.err.println("w="+w+" h="+h+" data="+data.length+" origLineLength="+origLineLength+" sampling="+sampling);
							// scan all pixels and down-sample
							for (y = 0; y < newH; y++) {
								for (x = 0; x < newW; x++) {

									// allow for edges in number of pixels left
									int wCount = sampling, hCount = sampling;
									int wGapLeft = w - x;
									int hGapLeft = h - y;
									if (wCount > wGapLeft) wCount = wGapLeft;
									if (hCount > hGapLeft) hCount = hGapLeft;

									int[] indexAv;
									for (jj = 0; jj < comp; jj++) {
										int byteTotal = 0, count = 0, ptr, newPtr;
										// noinspection ObjectAllocationInLoop
										indexAv = new int[indexCount];
										// count pixels in sample we will make into a pixel (ie 2x2 is 4 pixels , 4x4 is 16 pixels)
										for (yy = 0; yy < hCount; yy++) {
											for (xx = 0; xx < wCount; xx++) {

												ptr = ((yy + (y * sampling)) * origLineLength) + (((x * sampling * comp) + (xx * comp) + jj));
												if (ptr < oldSize) {
													if (!hasIndex) {
														byteTotal = byteTotal + (data[ptr] & 255);
													}
													else {
														for (int aa = 0; aa < indexCount; aa++)
															indexAv[aa] = indexAv[aa] + (index[(((data[ptr] & 255) * indexCount) + aa)] & 255);

													}

													count++;
												}
											}
										}

										// set value as white or average of pixels
										if (hasIndex) {
											newPtr = jj + (x * indexCount) + (newW * y * indexCount);
											for (int aa = 0; aa < indexCount; aa++)
												newData[newPtr + aa] = (byte) ((indexAv[aa]) / count);
										}
										else
											if (count > 0) {
												newPtr = jj + (x * comp) + (newW * y * comp);
												newData[newPtr] = (byte) ((byteTotal) / count);
											}
									}
								}
							}

							data = newData;
							h = newH;
							w = newW;

						}
						catch (Exception e) {

							// tell user and log
							if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
						}
					}
					else
						if (!isDCT && !isJPX && index == null) {}
			}
		}

		/** handle any decode array */
		if (decodeArray == null || decodeArray.length == 0) {}
		else
			if (Filters != null && (isJPX || isDCT)) { // don't apply on jpegs
			}
			else
				if (index == null) { // for the moment ignore if indexed (we may need to recode)
					ImageCommands.applyDecodeArray(data, d, decodeArray, colorspaceID, XObject);
				}

		if (imageMask) {
			/** create an image from the raw data */

			// allow for 1 x 1 pixel
			/**
			 * allow for 1 x 1 pixels scaled up or fine lines
			 */
			float ratio = ((float) h) / (float) w;

			if ((this.isPrinting && ratio < 0.1f && w > 4000 && h > 1) || (ratio < 0.001f && w > 4000 && h > 1) || (w == 1 && h == 1)) {// &&
																																		// data[0]!=0){

				float ix = this.gs.CTM[2][0];
				float iy = this.gs.CTM[2][1];

				float ih = this.gs.CTM[1][1];
				if (ih == 0) ih = this.gs.CTM[1][0];
				if (ih < 0) {
					iy = iy + ih;
					ih = -ih;
				}

				float iw = this.gs.CTM[0][0];
				if (iw == 0) iw = this.gs.CTM[0][1];
				if (iw < 0) {
					ix = ix + iw;
					iw = -iw;
				}

				// factor in GS rotation and swap w and h
				if (this.gs.CTM[0][0] == 0 && this.gs.CTM[0][1] > 0 && this.gs.CTM[1][0] != 0 && this.gs.CTM[1][1] == 0) {
					float tmp = ih;
					ih = iw;
					iw = tmp;
				}

				// allow for odd values less than 1 and ensure minimum width
				if (iw < 1) iw = 1;
				if (ih < 1) ih = 1;

				int lwidth = -1;

				// for thin lines, use line width to ensure appears
				if (ih < 3) {

					lwidth = (int) ih;
					ih = 1;
				}
				else
					if (iw < 3) {
						lwidth = (int) iw;
						iw = 1;
					}

				GeneralPath currentShape = new GeneralPath(Path2D.WIND_NON_ZERO);

				currentShape.moveTo(ix, iy);
				currentShape.lineTo(ix, iy + ih);
				currentShape.lineTo(ix + iw, iy + ih);
				currentShape.lineTo(ix + iw, iy);
				currentShape.closePath();

				// save for later
				if (this.renderPage && currentShape != null) {

					float lastLineWidth = this.gs.getLineWidth();

					if (lwidth > 0) this.gs.setLineWidth(lwidth);

					this.gs.setNonstrokeColor(this.gs.nonstrokeColorSpace.getColor());
					this.gs.setFillType(GraphicsState.FILL);

					this.current.drawShape(currentShape, this.gs, Cmd.F);

					// restore after draw
					if (lwidth > 0) this.gs.setLineWidth(lastLineWidth);

				}
				return null;

			}
			else
				if (h == 2 && d == 1 && ImageCommands.isRepeatingLine(data, h)) {
					/* Takes account of ef1603e.pdf. A thin horizontal dotted line is not scaled properly, therefore its converted to a shape. */
					/* Condition is only executed if line is uniform along vertical axis */

					float ix = this.gs.CTM[2][0];
					float iy = this.gs.CTM[2][1];
					float ih = this.gs.CTM[1][1];
					float iw = this.gs.CTM[0][0];

					// factor in GS rotation and swap w and h
					if (this.gs.CTM[0][0] == 0 && this.gs.CTM[0][1] > 0 && this.gs.CTM[1][0] != 0 && this.gs.CTM[1][1] == 0) {
						float tmp = ih;
						ih = iw;
						iw = tmp;
					}

					double byteWidth = iw / (data.length / h);
					double bitWidth = byteWidth / 8;

					for (int col = 0; col < data.length / h; col++) {
						int currentByte = data[col] & 0xff;
						currentByte = ~currentByte & 0xff;

						int bitCount = 8;
						double endX = 0, startX;
						boolean draw = false;

						while (currentByte != 0 || draw) {
							bitCount--;

							if ((currentByte & 0x1) == 1) {
								if (!draw) {
									endX = ((bitCount + 0.5) * bitWidth) + (col * byteWidth);
									draw = true;
								}
							}
							else
								if (draw) {
									draw = false;
									startX = ((bitCount + 0.5) * bitWidth) + (col * byteWidth);
									GeneralPath currentShape = new GeneralPath(Path2D.WIND_NON_ZERO);

									currentShape.moveTo((float) (ix + startX), iy);
									currentShape.lineTo((float) (ix + startX), iy + ih);
									currentShape.lineTo((float) (ix + endX), iy + ih);
									currentShape.lineTo((float) (ix + endX), iy);
									currentShape.closePath();

									// save for later
									if (this.renderPage && currentShape != null) {
										this.gs.setNonstrokeColor(this.gs.nonstrokeColorSpace.getColor());
										this.gs.setFillType(GraphicsState.FILL);

										this.current.drawShape(currentShape, this.gs, Cmd.F);

									}
								}
							currentByte = currentByte >>> 1;
						}
					}
					return null;
				}
				else {

					// see if black and back object

					if (isDownsampled) {
						/** create an image from the raw data */
						DataBuffer db = new DataBufferByte(data, data.length);

						int[] bands = { 0, 1, 2, 3 };
						image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
						Raster raster = Raster.createInterleavedRaster(db, w, h, w * 4, 4, bands, null);
						image.setData(raster);

					}
					else {

						// try to keep as binary if possible
						boolean hasObjectBehind = true;

						if (h < 20) // added as found file with huge number of tiny tiles
						hasObjectBehind = true;
						else
							if (mode != ImageCommands.ID) // not worth it for inline image
							hasObjectBehind = this.current.hasObjectsBehind(this.gs.CTM);

						// remove empty images in some files
						boolean isBlank = false, keepNonTransparent = false;
						if (imageMask && d == 1 && decodeColorData.getID() == ColorSpaces.DeviceRGB && maskCol[0] == 0 && maskCol[1] == 0
								&& maskCol[2] == 0) {

							// see if blank (assume true and disprove) and remove as totally see-through
							isBlank = true;
							for (int aa = 0; aa < data.length; aa++) {
								if (data[aa] != -1) {
									isBlank = false;
									aa = data.length;
								}
							}

							if (this.isPrinting && (mode == ImageCommands.ID || this.isType3Font || d == 1)) { // avoid transparency if possible
								WritableRaster raster = Raster.createPackedRaster(new DataBufferByte(data, data.length), w, h, 1, null);
								image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
								image.setData(raster);
								keepNonTransparent = true;
							}
							else
								if (isBlank) {
									image = null;
									removed = true;

								}
								else {
									byte[] newIndex = { (maskCol[0]), (maskCol[1]), (maskCol[2]), (byte) 255, (byte) 255, (byte) 255 };
									image = ColorSpaceConvertor.convertIndexedToFlat(d, w, h, data, newIndex, true, true);
								}
						}

						if (isBlank) {
							// done above so ignore
						}
						else
							if (!this.isPrinting && maskCol[0] == 0 && maskCol[1] == 0 && maskCol[2] == 0 && !hasObjectBehind && !this.isType3Font
									&& decodeColorData.getID() != ColorSpaces.DeviceRGB) {

								if (d == 1) {
									WritableRaster raster = Raster.createPackedRaster(new DataBufferByte(data, data.length), w, h, 1, null);
									image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
									image.setData(raster);

								}
								else { // down-sampled above //never called
									final int[] bands = { 0 };

									Raster raster = Raster.createInterleavedRaster(new DataBufferByte(data, data.length), w, h, w, 1, bands, null);

									image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
									image.setData(raster);

								}
							}
							else
								if (!keepNonTransparent) {

									// if(hasObjectBehind){
									// image=ColorSpaceConvertor.convertToARGB(image);
									if (d == 8 && isDownsampled) { // never called

										byte[] newIndex = { (maskCol[0]), (maskCol[1]), (maskCol[2]), (byte) 255, (byte) 255, (byte) 255 };
										image = ColorSpaceConvertor.convertIndexedToFlat(d, w, h, data, newIndex, true, true);
										// }else if(isType3Font){
										// WritableRaster raster =Raster.createPackedRaster(new DataBufferByte(data, data.length), w, h, 1, null);
										// image =new BufferedImage(w,h,BufferedImage.TYPE_BYTE_BINARY);
										// image.setData(raster);
										// System.out.println(image.getType()+" "+image);
									}
									else
										if ((w < 4000 && h < 4000) || hasObjectBehind) { // needed for hires
											byte[] newIndex = { maskCol[0], maskCol[1], maskCol[2], (byte) 255, (byte) 255, (byte) 255 };
											image = ColorSpaceConvertor.convertIndexedToFlat(1, w, h, data, newIndex, true, false);
											// }

										}
										else {
											// WritableRaster raster =Raster.createPackedRaster(new DataBufferByte(data, data.length), w, h, d, null);
											// ismage = new BufferedImage(new IndexColorModel(d, 1, maskCol, 0, false), raster, false, null);
											/**/
										}
								}
					}
				}
		}
		else
			if (Filters == null) { // handle no filters

				// save out image
				if (LogWriter.isOutput()) LogWriter.writeLog("Image " + name + ' ' + w + "W * " + h + "H with No Compression at BPC " + d);

				image = makeImage(decodeColorData, w, h, d, data, compCount, XObject);

			}
			else
				if (isDCT) { // handle JPEGS

					if (LogWriter.isOutput()) LogWriter.writeLog("JPeg Image " + name + ' ' + w + "W * " + h + 'H' + " arrayInverted="
							+ arrayInverted);

					/**
					 * get image data,convert to BufferedImage from JPEG & save out
					 */
					if (colorspaceID == ColorSpaces.DeviceCMYK && this.extractRawCMYK) {

						if (LogWriter.isOutput()) LogWriter.writeLog("Raw CMYK image " + name + " saved.");

						if (!this.objectStoreStreamRef.saveRawCMYKImage(data, name)) this.errorTracker
								.addPageFailureMessage("Problem saving Raw CMYK image " + name);

					}

					/**
					 * try { java.io.FileOutputStream a =new java.io.FileOutputStream("/Users/markee/Desktop/"+ name + ".jpg");
					 * 
					 * a.write(data); a.flush(); a.close();
					 * 
					 * } catch (Exception e) { LogWriter.writeLog("Unable to save jpeg " + name);
					 * 
					 * } /
					 **/

					// if ICC with Alt RGB, use alternative first
					boolean decodedOnAltColorspace = false;
					if (decodeColorData.getID() == ColorSpaces.ICC) {

						// try first and catch any error
						int alt = decodeColorData.getAlternateColorSpace();

						GenericColorSpace altDecodeColorData = null;

						if (alt == ColorSpaces.DeviceRGB) altDecodeColorData = new DeviceRGBColorSpace();
						else
							if (alt == ColorSpaces.DeviceCMYK) altDecodeColorData = new DeviceCMYKColorSpace();

						// try if any alt found
						if (altDecodeColorData != null) {

							try {
								image = altDecodeColorData.JPEGToRGBImage(data, w, h, decodeArray, pX, pY, arrayInverted, XObject);

								// if it returns image it worked flag and switch over
								if (image != null) {
									decodedOnAltColorspace = true;
									decodeColorData = altDecodeColorData;

									// flag if YCCK
									if (decodeColorData.isImageYCCK()) this.hasYCCKimages = true;
								}
							}
							catch (Exception e) {
								this.errorTracker.addPageFailureMessage("Unable to use alt colorspace with " + name + " to JPEG");
								// tell user and log
								if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
								image.flush();
								image = null;
							}
						}
					}

					/** decode if not done above */
					if (!decodedOnAltColorspace) {
						// separation, renderer
						try {
							image = decodeColorData.JPEGToRGBImage(data, w, h, decodeArray, pX, pY, arrayInverted, XObject);

							// flag if YCCK
							if (decodeColorData.isImageYCCK()) this.hasYCCKimages = true;

							// image=simulateOP(image);
						}
						catch (Exception e) {
							this.errorTracker.addPageFailureMessage("Problem converting " + name + " to JPEG");
							// tell user and log
							if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
							image.flush();
							image = null;
						}
						/**
						 * catch(Error err){ addPageFailureMessage("Problem converting "+name+" to JPEG");
						 * 
						 * image=null; }/
						 **/
					}

					type = "jpg";

					// set in makeImage so not set for JPEGS - we do it explicitly here
					setRotationOptionsOnJPEGImage();

				}
				else
					if (isJPX) { // needs imageio library

						if (LogWriter.isOutput()) LogWriter.writeLog("JPeg 2000 Image " + name + ' ' + w + "W * " + h + 'H');

						/**
						 * try { java.io.FileOutputStream a =new java.io.FileOutputStream("/Users/markee/Desktop/"+ name + ".jpg");
						 * 
						 * a.write(data); a.flush(); a.close();
						 * 
						 * } catch (Exception e) { LogWriter.writeLog("Unable to save jpeg " + name);
						 * 
						 * } /
						 **/

						if (JAIHelper.isJAIused()) {

							image = decodeColorData.JPEG2000ToRGBImage(data, w, h, decodeArray, pX, pY);

							type = "jpg";
						}
						else {
							if (System.getProperty("org.jpedal.jai") != null && System.getProperty("org.jpedal.jai").toLowerCase().equals("true")) {
								if (!ImageCommands.JAImessageShow) {
									ImageCommands.JAImessageShow = true;
									System.err.println("JPeg 2000 Images need both JAI and imageio.jar on classpath");
								}
								throw new RuntimeException("JPeg 2000 Images need both JAI and imageio.jar on classpath");
							}
							else {
								System.err.println("JPeg 2000 Images needs the VM parameter -Dorg.jpedal.jai=true switch turned on");
								throw new RuntimeException("JPeg 2000 Images needs the VM parameter -Dorg.jpedal.jai=true switch turned on");
							}
						}

						// set in makeImage so not set for JPEGS - we do it explicitly here
						setRotationOptionsOnJPEGImage();

					}
					else { // handle other types

						if (LogWriter.isOutput()) LogWriter.writeLog(name + ' ' + w + "W * " + h + "H BPC=" + d + ' ' + decodeColorData);

						image = makeImage(decodeColorData, w, h, d, data, compCount, XObject);

						// choose type on basis of size and avoid ICC as they seem to crash the Java class
						if (d == 8 || this.gs.nonstrokeColorSpace.getID() == ColorSpaces.DeviceRGB
								|| this.gs.nonstrokeColorSpace.getID() == ColorSpaces.ICC) type = "jpg";
					}

		if (image != null) {

			if (newSMask != null && DecodeParms == null) {
				DecodeParms = newSMask.getDictionary(PdfDictionary.DecodeParms);

				/**
				 * handle tiny gray dot used with larger mask for images in MSword (Customers customers-dec2011/January_Good_News2.pdf)
				 */
				if (decodeColorData.getID() == ColorSpaces.DeviceGray && w < newSMask.getInt(PdfDictionary.Width)
						&& h < newSMask.getInt(PdfDictionary.Height)) {

					// check data is all black
					int bytes = data.length;
					boolean isEmpty = true;
					for (int aa = 0; aa < bytes; aa++) {
						if (data[aa] != 0) {
							isEmpty = false;
							aa = bytes;
						}
					}

					if (isEmpty) {
						image = new BufferedImage(newSMask.getInt(PdfDictionary.Width), newSMask.getInt(PdfDictionary.Height),
								BufferedImage.TYPE_BYTE_GRAY);
					}
				}
			}

			/** handle any soft mask */
			if (newSMask != null) image = addSMaskObject(decodeColorData, data, name, w, h, XObject, isDCT, isJPX, image, DecodeParms, newSMask);
			else
				if (newMask != null) image = ImageCommands.addMaskObject(decodeColorData, d, isDCT, isJPX, image, colorspaceID, index, newMask,
						this.optionsApplied, this.currentPdfFile);

			if (image != null) image = ImageCommands.simulateOverprint(decodeColorData, data, isDCT, isJPX, image, colorspaceID, newMask, newSMask,
					this.current, this.gs);

			if (image == null) return null;

			if ((this.current.getType() == DynamicVectorRenderer.CREATE_HTML || this.current.getType() == DynamicVectorRenderer.CREATE_SVG || this.current
					.getType() == DynamicVectorRenderer.CREATE_JAVAFX)) {}
			else
				if (!this.renderDirectly && (this.finalImagesExtracted || this.rawImagesExtracted) && !this.cache.testIfImageAlreadySaved(XObject)) saveImage(
						name, this.createScaledVersion, image, type);

			// //Original save image code
			// if(!renderDirectly)
			// saveImage(name, createScaledVersion, image, type);

		}

		if (image == null && !removed) {
			this.imagesProcessedFully = false;
		}

		// apply any transfer function
		PdfObject TR = this.gs.getTR();
		if (TR != null) // array of values
		image = ImageCommands.applyTR(image, TR, this.currentPdfFile);

		// try to simulate some of blend by removing white if not bottom image
		if (DecodeParms != null && DecodeParms.getInt(PdfDictionary.Blend) != PdfDictionary.Unknown && this.current.hasObjectsBehind(this.gs.CTM)
				&& image != null && image.getType() != 2 && (!isDCT || DecodeParms.getInt(PdfDictionary.QFactor) == 0)) image = ImageCommands
				.makeBlackandWhiteTransparent(image);

		// sharpen 1 bit
		if (pX > 0 && pY > 0 && rawd == 1 && ImageCommands.sharpenDownsampledImages
				&& (decodeColorData.getID() == ColorSpaces.DeviceGray || decodeColorData.getID() == ColorSpaces.DeviceRGB)) {

			Kernel kernel = new Kernel(3, 3, new float[] { -1, -1, -1, -1, 9, -1, -1, -1, -1 });
			BufferedImageOp op = new ConvolveOp(kernel);
			image = op.filter(image, null);

		}

		// number of images used for caching
		this.imageCount++;

		/**
		 * transparency slows down printing so try to reduce if possible in printing
		 */
		if (mode == ImageCommands.ID && this.isPrinting && image != null && d == 1 && maskCol != null && maskCol[0] == 0 && maskCol[1] == 0
				&& maskCol[2] == 0 && maskCol[3] == 0) {

			int iw = image.getWidth();
			int ih = image.getHeight();
			BufferedImage newImage = new BufferedImage(iw, ih, BufferedImage.TYPE_BYTE_GRAY);

			newImage.getGraphics().setColor(Color.WHITE);
			newImage.getGraphics().fillRect(0, 0, iw, ih);
			newImage.getGraphics().drawImage(image, 0, 0, null);
			image = newImage;
		}

		return image;
	}

	/**
	 * needs to be explicitly set for JPEG images if getting image from object
	 */
	private void setRotationOptionsOnJPEGImage() {

		if (this.imageStatus > 0 && this.gs.CTM[0][0] > 0 && this.gs.CTM[0][1] > 0 && this.gs.CTM[1][1] > 0 && this.gs.CTM[1][0] < 0) {
			/**/
			// we need a different op for Image and viewer as we handle in diff ways
			if (this.imageStatus == IMAGE_getImageFromPdfObject) {
				// optionsApplied=optionsApplied+ PDFImageProcessing.IMAGE_INVERTED;
				this.gs.CTM[0][1] = -this.gs.CTM[0][1];
				// gs.CTM[1][0]=gs.CTM[1][0];
				this.gs.CTM[1][1] = -this.gs.CTM[1][1];
				this.gs.CTM[2][1] = this.gs.CTM[2][1] - this.gs.CTM[1][1];

			}
			else
				if (this.imageStatus == SCREEN_getImageFromPdfObject) this.optionsApplied = this.optionsApplied + PDFImageProcessing.IMAGE_INVERTED;
			/**/

		}
		else
			if (this.optionsApplied > 0 && this.gs.CTM[0][0] < 0 && this.gs.CTM[1][1] < 0 && this.gs.CTM[0][1] == 0 && this.gs.CTM[1][0] == 0) { // fix
																																					// for
																																					// elfo.pdf

				// optionsApplied=optionsApplied+ PDFImageProcessing.IMAGE_INVERTED;
				// gs.CTM[0][1]=-gs.CTM[0][1];
				// gs.CTM[1][0]=gs.CTM[1][0];
				this.gs.CTM[1][1] = -this.gs.CTM[1][1];
				this.gs.CTM[2][1] = this.gs.CTM[2][1] - this.gs.CTM[1][1] + this.gs.CTM[1][0];
				// gs.CTM[2][0]=gs.CTM[2][0]+30;

				this.gs.CTM[2][0] = this.gs.CTM[2][0] - this.gs.CTM[0][1];
			}
	}

	private void saveImage(String name, boolean createScaledVersion, BufferedImage image, String type) {
		if (image != null && image.getSampleModel().getNumBands() == 1) type = "tif";

		if (this.isPageContent && (this.renderImages || this.finalImagesExtracted || this.clippedImagesExtracted || this.rawImagesExtracted)) {

			/** create copy and scale if required */
			this.objectStoreStreamRef.saveStoredImage(name, ImageCommands.addBackgroundToMask(image, this.isMask), false, createScaledVersion, type);

		}
	}

	/**
	 * turn raw data into a BufferedImage
	 */
	private BufferedImage makeImage(GenericColorSpace decodeColorData, int w, int h, int d, byte[] data, int comp, PdfObject XObject) {

		// ensure correct size
		if (decodeColorData.getID() == ColorSpaces.DeviceGray) {
			if (d == 1) {
				int requiredSize = ((w + 7) >> 3) * h;
				int oldSize = data.length;
				if (oldSize < requiredSize) {
					byte[] oldData = data;
					data = new byte[requiredSize];
					System.arraycopy(oldData, 0, data, 0, oldSize);

					// and fill rest with 255 for white
					for (int aa = oldSize; aa < requiredSize; aa++)
						data[aa] = (byte) 255;
				}

			}
			else
				if (d == 8) {
					int requiredSize = w * h;
					int oldSize = data.length;
					if (oldSize < requiredSize) {
						byte[] oldData = data;
						data = new byte[requiredSize];
						System.arraycopy(oldData, 0, data, 0, oldSize);
					}
				}
		}

		/**
		 * put data into separate array. If we keep in PdfData then on pages where same image reused such as adobe/Capabilities and precisons, its
		 * flipped each time as its an object :-(
		 */
		// int byteCount=rawData.length;
		// byte[] data=new byte[byteCount];
		// System.arraycopy(rawData, 0, data, 0, byteCount);

		ColorSpace cs = decodeColorData.getColorSpace();
		int ID = decodeColorData.getID();

		BufferedImage image = null;
		byte[] index = decodeColorData.getIndexedMap();

		this.optionsApplied = PDFImageProcessing.NOTHING;

		/** fast op on data to speed up image manipulation */
		// optimse rotations here as MUCH faster and flag we have done this
		// something odd happens if CTM[2][1] is negative so factor ignore this case
		boolean isInverted = !this.doNotRotate && this.useHiResImageForDisplay && RenderUtils.isInverted(this.gs.CTM);
		boolean isRotated = !this.doNotRotate && this.useHiResImageForDisplay && RenderUtils.isRotated(this.gs.CTM);

		// do not apply to mask
		// last condition is for /Users/markee/PDFdata/baseline_screens/debug2/StampsProblems.pdf
		if (XObject.getGeneralType(PdfDictionary.Type) == PdfDictionary.Mask && this.streamType != ValueTypes.FORM) {
			isInverted = false;
			isRotated = false;
		}

		// This fix was masking miscalculation.
		// fix for image wrong on Customers3/ImageQualitySample_v0.2.docx.pdf
		// if(isInverted && gs.CTM[1][1]>0 && gs.lastCTM[1][1]<0)
		// isInverted=false;
		//
		// if(renderDirectly && ! this.isType3Font){
		// isInverted=false;
		// isRotated=false;
		// }

		// I optimised the code slightly - you were setting booleans are they had been
		// used - I removed so it keeps code shorter

		if (isInverted) {// invert at byte level with copy

			// needs to be 1 for sep
			int count = comp;
			if (ID == ColorSpaces.Separation) count = 1;
			else
				if (ID == ColorSpaces.DeviceN) count = decodeColorData.getColorComponentCount();

			byte[] processedData = ImageOps.invertImage(data, w, h, d, count, index);

			if (processedData != null) {

				data = processedData;
				this.optionsApplied = this.optionsApplied + PDFImageProcessing.IMAGE_INVERTED;

			}
		}

		if (isRotated) { // rotate at byte level with copy New Code still some issues
			byte[] processedData = ImageOps.rotateImage(data, w, h, d, comp, index);

			if (processedData != null) {
				data = processedData;

				this.optionsApplied = this.optionsApplied + PDFImageProcessing.IMAGE_ROTATED;

				// reset
				int temp = h;
				h = w;
				w = temp;
			}
		}

		// data=ColorSpaceConvertor.convertIndexedToFlat(d,w, h, data, index, 255);

		// System.out.println("index="+index);

		if (index != null) { // indexed images

			if (LogWriter.isOutput()) LogWriter.writeLog("Indexed " + w + ' ' + h);

			/** convert index to rgb if CMYK or ICC */
			if (!decodeColorData.isIndexConverted()) {
				index = decodeColorData.convertIndexToRGB(index);
			}

			// workout size and check in range
			// int size =decodeColorData.getIndexSize()+1;

			// pick out daft setting of totally empty iamge and ignore
			if (d == 8 && decodeColorData.getIndexSize() == 0 && decodeColorData.getID() == ColorSpaces.DeviceRGB) {

				boolean hasPixels = false;

				int indexCount = index.length;
				for (int ii = 0; ii < indexCount; ii++) {
					if (index[ii] != 0) {
						hasPixels = true;
						ii = indexCount;
					}
				}

				if (!hasPixels) {
					int pixelCount = data.length;

					for (int ii = 0; ii < pixelCount; ii++) {
						if (data[ii] != 0) {
							hasPixels = true;
							ii = pixelCount;
						}
					}
				}
				if (!hasPixels) {
					return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
				}
			}
			// allow for half bytes (ie bootitng.pdf)
			// if(d==4 && size>16)
			// size=16;

			// WritableRaster raster =Raster.createPackedRaster(db, w, h, d, null);

			// ColorModel cm=new IndexColorModel(d, size, index, 0, false);
			// image = new BufferedImage(cm, raster, false, null);

			// if(debugColor)
			// System.out.println("xx d="+d+" w="+w+" data="+data.length+" index="+index.length+" size="+size);

			try {
				// remove image in Itext which is white on white
				if (d == 1 && index.length == 6 && index[0] == index[3] && index[1] == index[4] && index[2] == index[5]) {
					image = null;
					// optimise silly Itext case of 1=1 white indexed image customers-dec2011/gattest.pdf
				}
				else
					if (d == 8 && w == 1 && h == 1 && index[0] == -1 && index[1] == -1 && index[2] == -1 && allBytesZero(data)) {
						image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
						image.createGraphics().setPaint(Color.CYAN);
						Raster raster = ColorSpaceConvertor.createInterleavedRaster((new byte[] { (byte) 255, (byte) 255, (byte) 255 }), 1, 1);
						image.setData(raster);

					}
					else {
						image = ColorSpaceConvertor.convertIndexedToFlat(d, w, h, data, index, false, false);
					}
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}

		}
		else
			if (d == 1) { // bitmaps next

				image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
				/** create an image from the raw data */
				DataBuffer db = new DataBufferByte(data, data.length);

				// needs to be inverted in this case (ie customers-dec2012/mariners_annual_2012.pdf)
				if (decodeColorData.getID() == ColorSpaces.Separation) {
					int count = data.length;
					for (int aa = 0; aa < count; aa++)
						data[aa] = (byte) (data[aa] ^ 255);
				}

				WritableRaster raster = Raster.createPackedRaster(db, w, h, d, null);
				image.setData(raster);

			}
			else
				if (ID == ColorSpaces.Separation || ID == ColorSpaces.DeviceN) {
					if (LogWriter.isOutput()) LogWriter.writeLog("Converting Separation/DeviceN colorspace to sRGB ");

					image = decodeColorData.dataToRGB(data, w, h);

					// direct images
				}
				else
					if (comp == 4) { // handle CMYK or ICC

						if (LogWriter.isOutput()) LogWriter.writeLog("Converting ICC/CMYK colorspace to sRGB ");

						image = ColorSpaceConvertor.convertFromICCCMYK(w, h, data);

						// ShowGUIMessage.showGUIMessage("y",image,"y");
					}
					else
						if (comp == 3) {

							if (LogWriter.isOutput()) LogWriter.writeLog("Converting 3 comp colorspace to sRGB index=" + index);

							// work out from size what sort of image data we have
							if (w * h == data.length) {
								if (d == 8 && index != null) {
									image = ColorSpaceConvertor.convertIndexedToFlat(d, w, h, data, index, false, false);
								}
								else {

									/** create an image from the raw data */
									DataBuffer db = new DataBufferByte(data, data.length);

									int[] bands = { 0 };

									image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
									Raster raster = Raster.createInterleavedRaster(db, w, h, w, 1, bands, null);
									image.setData(raster);

								}
							}
							else {

								if (LogWriter.isOutput()) LogWriter.writeLog("Converting data to sRGB ");

								// expand out 4 bit raster as does not appear to be easy way
								if (d == 4) {
									int origSize = data.length;
									int newSize = w * h * 3;
									boolean isOdd = (w & 1) == 1;
									int scanLine = ((w * 3) + 1) >> 1;

									byte[] newData = new byte[newSize];
									byte rawByte;
									int ptr = 0, currentLine = 0;
									for (int ii = 0; ii < origSize; ii++) {
										rawByte = data[ii];

										currentLine++;
										newData[ptr] = (byte) (rawByte & 240);
										if (newData[ptr] == -16) // fix for white
										newData[ptr] = (byte) 255;
										ptr++;

										if ((currentLine) == scanLine && isOdd) { // ignore pack bit at end of odd line
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
									data = newData;

								}

								image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

								data = ImageOps.checkSize(data, w, h, 3);

								Raster raster = ColorSpaceConvertor.createInterleavedRaster(data, w, h);
								image.setData(raster);

							}

						}
						else
							if (comp == 1) {

								if (LogWriter.isOutput()) LogWriter.writeLog("comp=1 and d= " + d);

								if (d != 8) {

									int newSize = w * h;

									byte[] newData = new byte[newSize];

									// Java needs 8 bit so expand out
									switch (d) {

										case 2:
											ColorSpaceConvertor.flatten2bpc(w, data, null, false, newSize, newData);

											break;

										case 4:
											ColorSpaceConvertor.flatten4bpc(w, data, newSize, newData);
											break;

										default:

											if (LogWriter.isOutput()) LogWriter.writeLog("unknown comp= " + d);
									}

									data = newData;

								}

								/** create an image from the raw data */
								DataBuffer db = new DataBufferByte(data, data.length);

								int[] bands = { 0 };
								image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
								Raster raster = Raster.createInterleavedRaster(db, w, h, w, 1, bands, null);
								image.setData(raster);

							}
							else
								if (LogWriter.isOutput()) LogWriter.writeLog("Image " + cs.getType() + " not currently supported with components "
										+ comp);

		return image;
	}

	private static boolean allBytesZero(byte[] data) {

		boolean allZero = true;

		for (byte bytes : data) {
			if (bytes != 0) {
				allZero = false;
				break;
			}
		}
		return allZero;
	}

	private BufferedImage addSMaskObject(GenericColorSpace decodeColorData, byte[] data, String name, int w, int h, PdfObject XObject, boolean isDCT,
			boolean isJPX, BufferedImage image, PdfObject DecodeParms, PdfObject newSMask) throws PdfException {
		{

			BufferedImage smaskImage;

			/** read the stream */
			byte[] objectData = this.currentPdfFile.readStream(newSMask, true, true, false, false, false,
					newSMask.getCacheName(this.currentPdfFile.getObjectReader()));

			if (objectData != null) {

				boolean ignoreMask = DecodeParms != null && DecodeParms.getInt(PdfDictionary.Colors) != -1
						&& DecodeParms.getInt(PdfDictionary.Predictor) != 15 && decodeColorData.getID() != ColorSpaces.ICC;

				// special case
				PdfObject maskColorSpace = newSMask.getDictionary(PdfDictionary.ColorSpace);
				if (ignoreMask && (decodeColorData.getID() == ColorSpaces.DeviceRGB || decodeColorData.getID() == ColorSpaces.DeviceCMYK)
						&& maskColorSpace.getParameterConstant(PdfDictionary.ColorSpace) == ColorSpaces.DeviceGray) ignoreMask = false;

				// ignore in this case of blank Smask on jpeg with grayscale
				if (isDCT && maskColorSpace.getParameterConstant(PdfDictionary.ColorSpace) == ColorSpaces.DeviceGray) {
					int len = objectData.length;
					ignoreMask = true;
					for (int aa = 0; aa < len; aa++) {
						if (objectData[aa] != -1) {
							ignoreMask = false;
							aa = len;
						}
					}
				}

				// ignoreMask is hack to fix odd Visuality files
				if (!ignoreMask) {

					int rawOptions = this.optionsApplied;

					if (this.optionsApplied == PDFImageProcessing.NOTHING) this.doNotRotate = true;

					int maskW = newSMask.getInt(PdfDictionary.Width);
					int maskH = newSMask.getInt(PdfDictionary.Height);

					boolean isWhiteAndDownscaled = false;

					boolean isIndexed = false;
					if (isWhiteAndDownscaled) {

						PdfObject XObjectColorSpace = XObject.getDictionary(PdfDictionary.ColorSpace);
						// PdfObject maskColorSpace=newSMask.getDictionary(PdfDictionary.ColorSpace);

						PdfArrayIterator maskFilters = newSMask.getMixedArray(PdfDictionary.Filter);

						boolean isJBIG2 = false;

						// only needed for this case
						if (XObjectColorSpace.getParameterConstant(PdfDictionary.ColorSpace) == ColorSpaces.DeviceRGB) {
							int maskFirstValue;
							if (maskFilters != null && maskFilters.hasMoreTokens()) {
								while (maskFilters.hasMoreTokens()) {
									maskFirstValue = maskFilters.getNextValueAsConstant(true);
									isJBIG2 = maskFirstValue == PdfFilteredReader.JBIG2Decode;
								}
							}
						}

						// special case customers3/si_test.pdf
						isIndexed = data.length == 2 && XObjectColorSpace.getParameterConstant(PdfDictionary.ColorSpace) == ColorSpaces.Indexed;

						isWhiteAndDownscaled = XObjectColorSpace != null
								&& ((XObjectColorSpace.getParameterConstant(PdfDictionary.ColorSpace) == ColorSpaces.DeviceRGB && (!isDCT || isJBIG2)) || (isIndexed && XObjectColorSpace
										.getDictionary(PdfDictionary.Indexed).getParameterConstant(PdfDictionary.ColorSpace) == ColorSpaces.DeviceRGB))
								&& maskColorSpace.getParameterConstant(PdfDictionary.ColorSpace) == ColorSpaces.DeviceGray;

					}

					if ((isWhiteAndDownscaled && (isDCT || isJPX || isIndexed))) {

						// invert and get image
						int c = objectData.length;
						for (int ii = 0; ii < c; ii++)
							objectData[ii] = (byte) (((byte) 255) - objectData[ii]);

						image = processImageXObject(newSMask, name, objectData, true, null);

					}
					else {

						smaskImage = processImageXObject(newSMask, name, objectData, true, null);

						// factor in a rotation to SMask
						if (this.pageNum > 0 && this.pageData.getRotation(this.pageNum) == 0 && rawOptions == PDFImageProcessing.IMAGE_ROTATED) {

							int width = smaskImage.getWidth();
							int height = smaskImage.getHeight();
							BufferedImage biFlip = new BufferedImage(height, width, smaskImage.getType());
							for (int i = 0; i < width; i++)
								for (int j = 0; j < height; j++) {
									biFlip.setRGB(height - 1 - j, i, smaskImage.getRGB(i, j));
								}
							smaskImage = biFlip;
						}

						// restore
						this.doNotRotate = false;
						this.optionsApplied = rawOptions;

						// apply mask
						if (smaskImage != null) {
							image = ImageCommands.applySmask(image, smaskImage, newSMask, false, decodeColorData.getID() == ColorSpaces.DeviceRGB,
									newSMask.getDictionary(PdfDictionary.ColorSpace), XObject, this.gs);
							smaskImage.flush();
						}

					}
				}
			}
		}
		return image;
	}
}
