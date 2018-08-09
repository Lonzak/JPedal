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
 * ObjectCloneFactory.java
 * ---------------
 */
package org.jpedal.utils;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.Map;

import org.jpedal.objects.raw.PdfObject;

/**
 * custom optimised cloning code for speed
 */
public class ObjectCloneFactory {

	public static int[] cloneArray(int[] array) {

		if (array == null) return null;

		int count = array.length;

		int[] returnValue = new int[count];

		System.arraycopy(array, 0, returnValue, 0, count);

		return returnValue;
	}

	public static float[] cloneArray(float[] array) {

		if (array == null) return null;

		int count = array.length;

		float[] returnValue = new float[count];

		System.arraycopy(array, 0, returnValue, 0, count);

		return returnValue;
	}

	public static byte[] cloneArray(byte[] array) {

		if (array == null) return null;

		int count = array.length;

		byte[] returnValue = new byte[count];

		System.arraycopy(array, 0, returnValue, 0, count);

		return returnValue;
	}

	public static byte[][] cloneDoubleArray(byte[][] byteDArray) {
		if (byteDArray == null) {
			return null;
		}
		else {
			byte[][] tmp = new byte[byteDArray.length][];
			for (int b = 0; b < byteDArray.length; b++) {
				tmp[b] = byteDArray[b].clone();
			}
			return tmp;
		}
	}

	public static BufferedImage deepCopy(BufferedImage bi) {
		if (bi == null) return null;

		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	public static PdfObject[] cloneArray(PdfObject[] pdfObjArray) {
		if (pdfObjArray == null) {
			return null;
		}
		else {
			PdfObject[] tmp = new PdfObject[pdfObjArray.length];
			for (int b = 0; b < pdfObjArray.length; b++) {
				tmp[b] = (PdfObject) pdfObjArray[b].clone();
			}
			return tmp;
		}
	}

	public static Map cloneMap(Map optValues) {
		if (optValues != null) {
			try {
				Map tmpMap = optValues.getClass().newInstance();
				tmpMap.putAll(optValues);
				return tmpMap;
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}

		return null;
	}

}
