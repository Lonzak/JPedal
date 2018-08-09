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
 * PDFExponential.java
 * ---------------
 */
package org.jpedal.function;

/**
 * Class to handle Type 2 shading (Exponential)
 */
public class PDFExponential extends PDFGenericFunction implements PDFFunction {

	private static final long serialVersionUID = 5385374201115057519L;

	private float[] C0 = { 0.0f }, C1 = { 1.0f };

	private float N;

	int returnValues;

	public PDFExponential(float N, float[] C0, float[] C1, float[] domain, float[] range) {

		super(domain, range);

		this.N = N;

		if (C0 != null) this.C0 = C0;

		if (C1 != null) this.C1 = C1;

		// note C0 might be null so use this.C0
		this.returnValues = this.C0.length;
	}

	/**
	 * Compute the required values for exponential shading (Only used by Stitching)
	 * 
	 * @param subinput
	 *            : input values
	 * @return The shading values as float[]
	 */
	@Override
	public float[] computeStitch(float[] subinput) {
		return compute(subinput);
	}

	@Override
	public float[] compute(float[] values) {

		float[] output = new float[this.returnValues];
		float[] result = new float[this.returnValues];

		// Only first value required
		float x = min(max(values[0], this.domain[0 * 2]), this.domain[0 * 2 + 1]);

		if (this.N == 1f) {// special case

			for (int i = 0; i < this.C0.length; i++) {
				// x^1 = x so don't bother finding the power
				output[i] = this.C0[i] + x * (this.C1[i] - this.C0[i]);

				// clip to range if present
				if (this.range != null) output[i] = min(max(output[i], this.range[i * 2]), this.range[i * 2 + 1]); // Clip output

				result[i] = output[i];

			}
		}
		else {
			for (int i = 0; i < this.C0.length; i++) {
				output[i] = this.C0[i] + (float) Math.pow(x, this.N) * (this.C1[i] - this.C0[i]);

				// clip to range if present.
				if (this.range != null) output[i] = min(max(output[i], this.range[i * 2]), this.range[i * 2 + 1]); // Clip output

				result[i] = output[i];

			}
		}

		return result;
	}
}
