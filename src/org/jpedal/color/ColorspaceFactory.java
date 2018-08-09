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
 * ColorspaceFactory.java
 * ---------------
 */
package org.jpedal.color;

import java.util.Map;

import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;

/**
 * @author markee
 * 
 *         returns the correct colorspace, decoding the values
 */
public class ColorspaceFactory {

	private ColorspaceFactory() {}

	/**
	 * used by commands which implicitly set colorspace
	 */
	final public static GenericColorSpace getColorSpaceInstance(PdfObjectReader currentPdfFile, PdfObject colorSpace, Map colorspacesObjects) {

		// see if we already have it
		String key;
		if (colorSpace.getStatus() == PdfObject.DECODED) key = colorSpace.getObjectRefAsString();
		else key = new String(colorSpace.getUnresolvedData());

		GenericColorSpace col = null;

		// cache some colorspaces for speed
		Object cachedValue = colorspacesObjects.get(key);

		if (cachedValue != null) {
			col = (GenericColorSpace) cachedValue;
			col.reset();
		}

		if (col == null) {

			col = getColorSpaceInstance(currentPdfFile, colorSpace);

			if (col.getID() == ColorSpaces.ICC) colorspacesObjects.put(key, col);

		}

		return col;
	}

	/**
	 * used by commands which implicitly set colorspace
	 */
	final public static GenericColorSpace getColorSpaceInstance(PdfObjectReader currentPdfFile, PdfObject colorSpace) {

		currentPdfFile.checkResolved(colorSpace);

		int ID = colorSpace.getParameterConstant(PdfDictionary.ColorSpace);

		// allow for CMYK in ID
		if (ID == PdfDictionary.CMYK) ID = ColorSpaces.DeviceCMYK;

		boolean isIndexed = false;
		int size = 0;
		byte[] lookup = null;

		int rawID = -1;

		/** setup colorspaces which map onto others */
		if (ID == ColorSpaces.Indexed || ID == PdfDictionary.I) {

			isIndexed = true;

			// get raw values from Indexed
			size = colorSpace.getInt(PdfDictionary.hival);
			lookup = colorSpace.getDictionary(PdfDictionary.Lookup).getDecodedStream();

			// actual colorspace
			colorSpace = colorSpace.getDictionary(PdfDictionary.Indexed);
			ID = colorSpace.getParameterConstant(PdfDictionary.ColorSpace);
			rawID = ID;

		}

		GenericColorSpace currentColorData = getColorspace(currentPdfFile, colorSpace, ID);

		/** handle CMAP as object or direct */
		if (isIndexed) {

			// ICC code will wrongly create RGB in case of indexed ICC with DeviceGray alt - here we fit this
			// (sampe file is Customers-june2011/early mockup.pdf)
			if (rawID == ColorSpaces.ICC && lookup.length < 3) currentColorData = new DeviceGrayColorSpace();

			currentColorData.setIndex(lookup, size);
		}

		currentColorData.setAlternateColorSpace(colorSpace.getParameterConstant(PdfDictionary.Alternate));

		return currentColorData;
	}

	private static GenericColorSpace getColorspace(PdfObjectReader currentPdfFile, PdfObject colorSpace, int ID) {

		// no DeviceRGB as set as default
		GenericColorSpace currentColorData = new DeviceRGBColorSpace();

		switch (ID) {
			case ColorSpaces.Separation:
				currentColorData = new SeparationColorSpace(currentPdfFile, colorSpace);
				break;

			case ColorSpaces.DeviceN:
				currentColorData = new DeviceNColorSpace(currentPdfFile, colorSpace);
				break;

			case ColorSpaces.DeviceGray:
				currentColorData = new DeviceGrayColorSpace();
				break;

			case ColorSpaces.DeviceCMYK:
				currentColorData = new DeviceCMYKColorSpace();
				break;

			case ColorSpaces.CalGray:
				currentColorData = getCalGrayColorspace(colorSpace);
				break;

			case ColorSpaces.CalRGB:
				currentColorData = getCalRGBColorspace(colorSpace);
				break;

			case ColorSpaces.Lab:
				currentColorData = getLabColorspace(colorSpace);
				break;

			case ColorSpaces.ICC:
				currentColorData = getICCColorspace(colorSpace);
				break;

			case ColorSpaces.Pattern:
				currentColorData = new PatternColorSpace(currentPdfFile);
				break;

		}

		return currentColorData;
	}

	private static GenericColorSpace getICCColorspace(PdfObject colorSpace) {

		GenericColorSpace currentColorData = new DeviceRGBColorSpace();

		// int ID;
		/**
		 * Switch ICC to alt Gray
		 */
		int alt = colorSpace.getParameterConstant(PdfDictionary.Alternate);
		if (alt == ColorSpaces.DeviceGray) {// || alt== ColorSpaces.DeviceRGB){

			// use the RGB

			// }else if(alt==ColorSpaces.DeviceCMYK){

			// currentColorData=new DeviceCMYKColorSpace();
		}
		else {
			currentColorData = new ICCColorSpace(colorSpace);
		}

		// use if issue with data
		if (alt == ColorSpaces.DeviceCMYK && currentColorData.isInvalid()) {
			currentColorData = new DeviceCMYKColorSpace();
		}

		return currentColorData;
	}

	private static GenericColorSpace getLabColorspace(PdfObject colorSpace) {

		float[] R = { -100f, 100f, -100.0f, 100.0f };
		float[] W = { 0.0f, 1.0f, 0.0f };
		float[] B = { 0.0f, 0.0f, 0.0f };

		float[] blackpointArray = colorSpace.getFloatArray(PdfDictionary.BlackPoint);
		float[] whitepointArray = colorSpace.getFloatArray(PdfDictionary.WhitePoint);
		float[] rangeArray = colorSpace.getFloatArray(PdfDictionary.Range);

		if (whitepointArray != null) W = whitepointArray;

		if (blackpointArray != null) B = blackpointArray;

		if (rangeArray != null) R = rangeArray;

		return new LabColorSpace(W, B, R);
	}

	private static GenericColorSpace getCalRGBColorspace(PdfObject colorSpace) {

		float[] W = { 0.0f, 1.0f, 0.0f };
		float[] B = { 0.0f, 0.0f, 0.0f };
		float[] G = { 1.0f, 1.0f, 1.0f };
		float[] Ma = { 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f };

		float[] gammaArray = colorSpace.getFloatArray(PdfDictionary.Gamma);
		float[] blackpointArray = colorSpace.getFloatArray(PdfDictionary.BlackPoint);
		float[] whitepointArray = colorSpace.getFloatArray(PdfDictionary.WhitePoint);
		float[] matrixArray = colorSpace.getFloatArray(PdfDictionary.Matrix);

		if (whitepointArray != null) W = whitepointArray;

		if (blackpointArray != null) B = blackpointArray;

		if (gammaArray != null) G = gammaArray;

		if (matrixArray != null) Ma = matrixArray;

		return new CalRGBColorSpace(W, B, Ma, G);
	}

	private static GenericColorSpace getCalGrayColorspace(PdfObject colorSpace) {

		float[] W = { 0.0f, 1.0f, 0.0f };
		float[] B = { 0.0f, 0.0f, 0.0f };
		float[] G = { 1.0f, 1.0f, 1.0f };

		float[] gammaArray = null;
		float[] blackpointArray = colorSpace.getFloatArray(PdfDictionary.BlackPoint);
		float[] whitepointArray = colorSpace.getFloatArray(PdfDictionary.WhitePoint);
		float rawGamma = colorSpace.getFloatNumber(PdfDictionary.Gamma);
		if (rawGamma != -1) {
			gammaArray = new float[1];
			gammaArray[0] = rawGamma;
		}

		if (whitepointArray != null) W = whitepointArray;

		if (blackpointArray != null) B = blackpointArray;

		if (gammaArray != null) G = gammaArray;

		return new CalGrayColorSpace(W, B, G);
	}

}
