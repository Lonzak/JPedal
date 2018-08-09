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
 * PDFSampled.java
 * ---------------
 */

package org.jpedal.function;

/**
 * Class to handle Type 0 shading (Sampled) from a Pdf
 */
public class PDFSampled extends PDFGenericFunction implements PDFFunction {

	private static final long serialVersionUID = -2510153148040368247L;
	private int[] size;
	private float[] samples;

	int returnValues;

	public PDFSampled(byte[] stream, int bits, float[] domain, float[] range, float[] encode, float[] decode, int[] size) {

		// setup global values needed
		super(domain, range);

		/**
		 * set parameters
		 */
		this.size = size;
		int bitsPerSample = bits;

		if (encode != null) this.encode = encode;
		else {
			int defaultSize = size.length;
			this.encode = new float[defaultSize * 2];
			for (int ii = 0; ii < defaultSize; ii++)
				this.encode[(ii * 2) + 1] = size[ii] - 1;
		}

		if (decode != null) this.decode = decode;
		else {
			int defaultSize = range.length;
			this.decode = new float[defaultSize];
			System.arraycopy(range, 0, this.decode, 0, defaultSize);
		}

		/**
		 * read sample table
		 */
		if (bitsPerSample == 8) { // (easy common cases first optimised for each case)

			int count = stream.length;
			this.samples = new float[count];

			for (int ii = 0; ii < count; ii++)
				this.samples[ii] = (stream[ii] & 255) / 256f; // sample is fraction

		}
		else
			if (bitsPerSample == 4) {

				int count = stream.length * 2;
				this.samples = new float[count];
				int byteReached = 0;

				for (int ii = 0; ii < count; ii = ii + 2) {
					this.samples[ii] = ((stream[byteReached] & 240) >> 4) / 16f; // sample is fraction
					this.samples[ii + 1] = (stream[byteReached] & 15) / 16f; // sample is fraction

					byteReached++;
				}
			}
			else
				if (bitsPerSample == 2) {

					int count = stream.length * 4;
					this.samples = new float[count];
					int byteReached = 0;

					for (int ii = 0; ii < count; ii = ii + 4) {
						this.samples[ii] = ((stream[byteReached] & 192) >> 6) / 4f; // sample is fraction
						this.samples[ii + 1] = ((stream[byteReached] & 48) >> 4) / 4f; // sample is fraction
						this.samples[ii + 2] = ((stream[byteReached] & 12) >> 2) / 4f; // sample is fraction
						this.samples[ii + 3] = (stream[byteReached] & 3) / 4f; // sample is fraction

						byteReached++;
					}
				}
				else
					if (bitsPerSample == 1) {

						int count = stream.length * 8;
						this.samples = new float[count];
						int byteReached = 0;

						for (int ii = 0; ii < count; ii = ii + 8) {
							this.samples[ii] = ((stream[byteReached] & 128) >> 7) / 2f; // sample is fraction
							this.samples[ii + 1] = ((stream[byteReached] & 64) >> 6) / 2f; // sample is fraction
							this.samples[ii + 2] = ((stream[byteReached] & 32) >> 5) / 2f; // sample is fraction
							this.samples[ii + 3] = ((stream[byteReached] & 16) >> 4) / 2f; // sample is fraction
							this.samples[ii + 4] = ((stream[byteReached] & 8) >> 3) / 2f; // sample is fraction
							this.samples[ii + 5] = ((stream[byteReached] & 4) >> 2) / 2f; // sample is fraction
							this.samples[ii + 6] = ((stream[byteReached] & 2) >> 1) / 2f; // sample is fraction
							this.samples[ii + 7] = ((stream[byteReached] & 1)) / 2f; // sample is fraction

							byteReached++;
						}

					}
					else
						if (bitsPerSample == 12) {

							int samplesPerByte = 16 / bitsPerSample;

							int count = stream.length * samplesPerByte * 2;
							this.samples = new float[count];
							int byteReached = 0, bitsLeft = 0;
							int maxSize = (2 << bitsPerSample) - 1;

							for (int ii = 0; ii < count; ii++) {

								for (int jj = 0; jj < samplesPerByte; jj++)
									this.samples[ii] = ((((stream[byteReached] << 8) + stream[byteReached]) & (maxSize << (16 - (jj * bitsPerSample)))) >> (16 - bitsPerSample))
											/ maxSize; // sample is fraction

								// rollon
								while (bitsLeft > 16) {
									byteReached = byteReached + 2;
									bitsLeft = bitsLeft - 16;
								}
							}
						}
						else { // rest of values 16,24,32

							int bytes = bitsPerSample / 8;

							int count = stream.length / bytes;
							this.samples = new float[count];
							int byteReached = 0;
							long maxSize = 1;
							if (bitsPerSample == 16) maxSize = 65536;
							else
								if (bitsPerSample == 24) maxSize = 16777216;
								else
									if (bitsPerSample == 32) maxSize = 429457408;

							for (int ii = 0; ii < count; ii++) {
								long val = 0;
								for (int aa = 0; aa < bytes; aa++)
									val = val + (((stream[byteReached + aa] & 255) << (8 * (bytes - aa - 1))));

								this.samples[ii] = ((float) val) / (float) maxSize; // sample is fraction
								byteReached = byteReached + bytes;

							}

						}

		this.returnValues = range.length / 2;
	}

	/**
	 * Calculate the output values for this point in the shading object. (Only used by Stitching)
	 * 
	 * @param subinput
	 *            : Shading input values
	 * @return returns the shading values for this point
	 */
	@Override
	public float[] computeStitch(float[] subinput) {
		return compute(subinput);
	}

	@Override
	public float[] compute(float[] input) {

		float[] result = new float[this.returnValues];

		// set values
		int m = this.domain.length / 2;
		int n = this.range.length / 2;

		// @odd
		if (n < m) {
			// reverse
			int size = input.length;
			float[] reversed = new float[size];
			for (int ii = 0; ii < size; ii++) {
				reversed[size - ii - 1] = input[ii];
			}
			input = reversed;
		}

		float[] e = new float[m * 2];

		// use x to match def in ref guide
		float[] x = input;

		for (int i = 0; i < m; i++) {

			// Encode and clip value to sample table
			e[i * 2] = encodeInput(x[i], i);

			if (n == m) {
				result[i] = decodeSample(e[i * 2], i, n, 0);
			}
			else
				if (m < n) {
					for (int j = 0; j < n; j++)
						result[j] = decodeSample(e[i * 2], j, n, j);
				}
				else
					if (n < m) {

						// @odd - see above as well
						// Current issue with hexachromatic colorspaces.
						// Possible need for transparency
						// also must figure out how to turn 6 into n components
						for (int j = 0; j < n; j++)
							result[j] = decodeSample(e[i * 2], j, n, j);
					}
		}

		return result;
	}

	/**
	 * Take the input value and encode it to the samples array. Clip, Interpolate, Clip and return the encoded value.
	 * 
	 * @param value
	 *            : Input Value
	 * @param point
	 *            : Point in domain / size
	 * @return encoded input value
	 */
	private float encodeInput(float value, int point) {
		// This is the exact implementation of the encoding from the PDF spec

		// clip input to the input Domains
		value = min(max(value, this.domain[point * 2]), this.domain[point * 2 + 1]);

		// encode the input value to the sample value
		value = interpolate(value, this.domain[point * 2], this.domain[point * 2 + 1], this.encode[point * 2], this.encode[point * 2 + 1]);

		// clip input the samples array
		value = min(max(value, 0), this.size[point] - 1);

		// Pass array position back
		return value;
	}

	/**
	 * Get the decoded value for the input value. Get Sampled value, interpolate, clip and return.
	 * 
	 * @param value
	 *            : Encoded input value
	 * @param j
	 *            : Decode and Range array point
	 * @param n
	 *            : Return Length (used to find correct sample)
	 * @param modifier
	 *            : Modifer from start of samples.
	 * @return : final output value
	 */
	private float decodeSample(float value, int j, int n, int modifier) {

		// Convert input value into a sampled value
		int sample = (int) (value);

		if ((value - (int) value) > 0) sample = (int) value + 1;

		// Calculate the fraction between this value and value+1
		float frac1 = sample - value, frac0 = 1f - frac1;

		// Calculate the point in the samples array
		int lower = (((int) value * n) + modifier);
		int upper = ((sample * n) + modifier);

		// Get output value + the fraction between the values
		float output = (frac1 * this.samples[lower]) + (frac0 * (this.samples[upper]));

		// uses 1 and not maxSize as we have already factored in
		output = interpolate(output, 0, 1, this.decode[j * 2], this.decode[j * 2 + 1]);

		// clip to output range
		output = min(max(output, this.range[j * 2]), this.range[j * 2 + 1]);

		// final output result
		return output;
	}
}
