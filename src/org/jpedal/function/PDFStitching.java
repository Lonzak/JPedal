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
 * PDFStitching.java
 * ---------------
 */

package org.jpedal.function;

/**
 * Class to handle Type 3 shading (Stitching) in PDF
 */
public class PDFStitching extends PDFGenericFunction implements PDFFunction {

	private static final long serialVersionUID = -3854746004397674933L;
	/**
	 * composed of other Functions
	 */
	private PDFFunction[] functions;
	private float[] bounds;

	public PDFStitching(PDFFunction[] functions, float[] encode, float[] bounds, float[] domain, float[] range) {

		// setup global values needed
		super(domain, range);

		if (bounds != null) this.bounds = bounds;

		if (encode != null) this.encode = encode;

		if (functions != null) this.functions = functions;

		// n = encode.length/2;
	}

	/**
	 * Calculate shading for current location
	 * 
	 * @param values
	 *            input values for shading calculation
	 * @return float[] containing the color values for shading at this point
	 */
	@Override
	public float[] compute(float[] values) {

		// take raw input number
		float x = min(max(values[0], this.domain[0 * 2]), this.domain[0 * 2 + 1]);

		// hack for error in a PDF
		if (this.bounds == null) this.bounds = new float[0];

		// see if value lies outside a boundary
		int subi = this.bounds.length - 1;
		for (; subi >= 0; subi--)
			if (x >= this.bounds[subi]) break;
		subi++;

		// if it does, truncate it
		float[] subinput = new float[1];
		float xmin = this.domain[0], xmax = this.domain[1];
		if (subi > 0) xmin = (this.bounds[subi - 1]);
		if (subi < this.bounds.length) xmax = (this.bounds[subi]);

		float ymin = this.encode[subi * 2];
		float ymax = this.encode[subi * 2 + 1];
		subinput[0] = interpolate(x, xmin, xmax, ymin, ymax);

		float[] output = this.functions[subi].computeStitch(subinput);

		float[] result = new float[output.length];

		if (this.range != null) {
			for (int i = 0; i != this.range.length / 2; i++)
				result[i] = min(max(output[i], this.range[0 * 2]), this.range[0 * 2 + 1]);
		}
		else {
			for (int i = 0; i != output.length; i++)
				result[i] = output[i];
		}

		return result;
	}

	/**
	 * Calculate shading for current location (Only used by Stitching)
	 * 
	 * @param values
	 *            : input values for shading calculation
	 * @return float[] containing the color values for shading at this point
	 */
	@Override
	public float[] computeStitch(float[] values) {

		// take raw input number
		float x = min(max(values[0], this.domain[0 * 2]), this.domain[0 * 2 + 1]);

		// see if value lies outside a boundary
		int subi = this.bounds.length - 1;
		for (; subi >= 0; subi--) {
			if (x >= this.bounds[subi]) break;
		}
		subi++;

		// if it does, truncate it
		float[] subinput = new float[1];
		float xmin = this.domain[0], xmax = this.domain[1];
		if (subi > 0) xmin = (this.bounds[subi - 1]);
		if (subi < this.bounds.length) xmax = (this.bounds[subi]);

		float ymin = this.encode[subi * 2];
		float ymax = this.encode[subi * 2 + 1];
		subinput[0] = interpolate(x, xmin, xmax, ymin, ymax);

		float[] output = this.functions[subi].compute(subinput);

		float[] result = new float[output.length];

		for (int i = 0; i != this.range.length / 2; i++) {
			if (this.range != null) result[i] = min(max(output[i], this.range[0 * 2]), this.range[0 * 2 + 1]);
			else result[i] = output[i];
		}

		return result;
	}

}
