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
 * PDFCalculator.java
 * ---------------
 */

package org.jpedal.function;

import org.jpedal.utils.LogWriter;

/**
 * Class to handle Type 4 shading (PostScript Calculator) from a Pdf
 */
public class PDFCalculator extends PDFGenericFunction implements PDFFunction {

	private static final long serialVersionUID = -2771458940865489908L;

	int returnValues;

	byte[] stream;

	public PDFCalculator(byte[] stream, float[] domain, float[] range) {

		super(domain, range);

		this.returnValues = range.length / 2;

		/**
		 * set stream
		 */
		this.stream = stream; // raw data
	}

	/**
	 * Calculate the output values for this point in the shading object. (Only used by Stitching)
	 * 
	 * @return returns the shading values for this point
	 */
	@Override
	public float[] computeStitch(float[] subinput) {
		return compute(subinput);
	}

	@Override
	public float[] compute(float[] values) {

		float[] output = new float[this.returnValues];
		float[] result = new float[this.returnValues];

		try {
			PostscriptFactory post = new PostscriptFactory(this.stream);

			post.resetStacks(values);
			double[] stack = post.executePostscript();

			if ((this.domain.length / 2) == 1) {
				for (int i = 0, imax = this.range.length / 2; i < imax; i++) {
					output[i] = (float) (stack[i]); // take output from stack
					result[i] = min(max(output[i], this.range[i * 2]), this.range[i * 2 + 1]);
				}
			}
			else {
				for (int i = 0, imax = this.range.length / 2; i < imax; i++) {
					output[i] = (float) (stack[i]); // take output from stack
					result[i] = min(max(output[i], this.range[i * 2]), this.range[i * 2 + 1]);
				}
			}

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		return result;
	}
}
