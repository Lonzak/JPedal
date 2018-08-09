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
 * ASCII85.java
 * ---------------
 */
package org.jpedal.io.filter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Map;

import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

/**
 * Created by IntelliJ IDEA. User: test Date: 11/04/11 Time: 09:49 To change this template use File | Settings | File Templates.
 */
public class ASCII85 extends BaseFilter implements PdfFilter {

	/** lookup for hex multiplication */
	private final static long[] hex_indices = { 256 * 256 * 256, 256 * 256, 256, 1 };

	/** lookup for ASCII85 decode */
	private final static long[] base_85_indices = { 85 * 85 * 85 * 85, 85 * 85 * 85, 85 * 85, 85, 1 };

	public ASCII85(PdfObject decodeParms) {
		super(decodeParms);
	}

	// ////////////////////////////////////////////////
	/**
	 * ascii85decode using our own implementation
	 */
	@Override
	public byte[] decode(byte[] valuesRead) {

		int special_cases = 0, returns = 0, data_size = valuesRead.length;

		// allow for special cases
		for (int i = 0; i < data_size; i++) {
			if (valuesRead[i] == 122) special_cases++;
			else
				if (valuesRead[i] == 10) returns++;
		}

		// fix for odd file
		if (returns == 5 && special_cases == 0 && ((data_size - returns) % 5) == 4) {
			data_size++;
		}

		// pointer in output buffer
		int output_pointer = 0;
		long value;

		// buffer to hold data
		byte[] temp_data = new byte[data_size - returns + 1 + (special_cases * 3)];
		int ii, next;

		/**
		 * translate each set of 5 to 4 bytes (note lookup tables)
		 */

		// 5 bytes in base 85
		for (int i = 0; i < data_size; i++) {
			value = 0;
			next = valuesRead[i];
			while ((next == 10) || (next == 13)) {
				i++;
				if (i == data_size) next = 0;
				else next = valuesRead[i];
			}

			// check special case first
			if (next == 122) {

				// and write out 4 bytes
				for (int i3 = 0; i3 < 4; i3++) {
					temp_data[output_pointer] = 0;
					output_pointer++;
				}
			}
			else
				if ((data_size - i > 4) && (next > 32) && (next < 118)) {

					for (ii = 0; ii < 5; ii++) {

						if (i < valuesRead.length) next = valuesRead[i];

						while ((next == 10) || (next == 13)) {
							i++;
							if (i == data_size) next = 0;
							else next = valuesRead[i];
						}
						i++;
						if (((next > 32) && (next < 118)) || (next == 126)) value = value + ((next - 33) * base_85_indices[ii]);
					}

					// and write out 4 bytes
					for (int i3 = 0; i3 < 4; i3++) {
						temp_data[output_pointer] = (byte) ((value / hex_indices[i3]) & 255);
						output_pointer++;
					}
					i--;// correction as loop will also increment
				}
		}

		// now put values into processed data
		byte[] processed_data = new byte[output_pointer];
		System.arraycopy(temp_data, 0, processed_data, 0, output_pointer);

		return processed_data;
	}

	/**
	 * ascii85decode using our own implementation
	 */
	@Override
	public void decode(BufferedInputStream bis, BufferedOutputStream streamCache, String cacheName, Map cachedObjects) {

		this.bis = bis;
		this.streamCache = streamCache;
		this.cachedObjects = cachedObjects;

		long value;
		int nextValue;

		try {
			/**
			 * translate each set of 5 to 4 bytes (note lookup tables)
			 */

			// int data_size = bis.available(), lastValue = 0;
			boolean ignoreLastItem = false;
			while (bis.available() > 0) {
				value = 0;

				nextValue = read(bis);

				// check special case first
				if (nextValue == 122) {

					// and write out 4 bytes
					for (int i3 = 0; i3 < 4; i3++)
						streamCache.write(0);

				}
				else
					if ((bis.available() >= 4) && (nextValue > 32) && (nextValue < 118)) {

						// lastValue = nextValue;

						value = value + ((nextValue - 33) * base_85_indices[0]);

						// String list = "";

						for (int ii = 1; ii < 5; ii++) {
							nextValue = read(bis);

							// list = list + nextValue + ' ';

							if (nextValue == -1) nextValue = 0;
							// System.out.println(">>"+nextValue);
							// if((lastValue==126)&&(nextValue==62)&&(bis.available()<6))
							if (nextValue == -1) ignoreLastItem = true;

							// lastValue = nextValue;
							// System.out.println(nextValue+" "+(char)nextValue);
							if (((nextValue > 32) && (nextValue < 118)) || (nextValue == 126)) value = value
									+ ((nextValue - 33) * base_85_indices[ii]);
						}

						if (!ignoreLastItem) {
							// and write out 4 bytes
							for (int i3 = 0; i3 < 4; i3++) {
								byte b = (byte) ((value / hex_indices[i3]) & 255);

								streamCache.write(b);
							}
						}
					}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " accessing Ascii85Decode filter ");
		}
	}

	private static int read(BufferedInputStream bis) throws IOException {

		int nextValue = bis.read();
		while ((nextValue == 13) || (nextValue == 10))
			nextValue = bis.read();

		return nextValue;
	}

}
