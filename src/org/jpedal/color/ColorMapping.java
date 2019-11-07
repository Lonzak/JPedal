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
 * ColorMapping.java
 * ---------------
 */
package org.jpedal.color;

import java.io.Serializable;

import org.jpedal.function.FunctionFactory;
import org.jpedal.function.PDFFunction;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;

/**
 * convert to actual RGB colour for Separation/DeviceN using a function
 */
public class ColorMapping implements Serializable {

	private static final long serialVersionUID = 2732809266903967232L;

	private PDFFunction function;

	/** defined in PDF spec */
	private int functionType;

	public ColorMapping(PdfObjectReader currentPdfFile, PdfObject functionObj) {

		// needed for this class
		this.functionType = functionObj.getInt(PdfDictionary.FunctionType);

		/** setup the translation function */
		this.function = FunctionFactory.getFunction(functionObj, currentPdfFile);
	}

	/**
	 * Do the actual conversion
	 * 
	 * @param values
	 */
	public float[] getOperandFloat(float[] values) {
		return this.function.compute(values);
	}

	public int getFunctionType() {
		return this.functionType;
	}
}
