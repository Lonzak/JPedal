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
 * NumberUtils.java
 * ---------------
 */
package org.jpedal.utils;

/**
 * useful static number methods
 */
public class NumberUtils {

	public final static int[] powers = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000 };

	/**
	 * turn stream of bytes into a number
	 */
	public static int parseInt(int i, int j, byte[] bytes) {
		int finalValue = 0;
		int power = 0;

		boolean isNegative = false;
		i--; // decrement pointer to speed up
		for (int current = j - 1; current > i; current--) {

			if (bytes[current] == '-') {
				isNegative = true;
			}
			else {
				finalValue = finalValue + ((bytes[current] - 48) * powers[power]);
				// System.out.println(finalValue+" "+powers[power]+" "+current+" "+(char)bytes[current]+" "+bytes[current]);
				power++;
			}
		}

		if (isNegative) return -finalValue;
		else return finalValue;
	}

	/**
	 * turn stream of bytes into a flaot number
	 */
	public static double parseDouble(int start, int end, byte[] stream) {

		double d, dec, num;

		int ptr = end;
		int intStart = start;
		boolean isMinus = false;
		// hand optimised float code

		// find decimal point
		for (int j = end - 1; j > start - 1; j--) {
			if (stream[j] == 46) { // '.'=46
				ptr = j;
				break;
			}
		}

		int intChars = ptr;

		int decStart = ptr;

		// allow for minus
		if (stream[start] == 43) { // '+'=43
			intChars--;
			intStart++;
		}
		else
			if (stream[start] == 45) { // '-'=45
				// intChars--;
				intStart++;
				isMinus = true;
			}

		// optimisations
		int intNumbers = intChars - intStart;
		int decNumbers = end - ptr;

		if ((intNumbers > 4)) { // non-optimised to cover others
			isMinus = false;

			int count = end - start;
			byte[] doubleValue = new byte[count];

			System.arraycopy(stream, start, doubleValue, 0, count);

			// System.out.println(new String(floatVal)+"<");
			d = Double.parseDouble(new String(doubleValue));

		}
		else {

			double thous = 0f, units = 0f, tens = 0f, hundreds = 0f, tenths = 0f, hundredths = 0f, thousands = 0f, tenthousands = 0f, hunthousands = 0f, millis = 0f;
			int c;

			// thousands
			if (intNumbers > 3) {
				c = stream[intStart] - 48;
				switch (c) {
					case 1:
						thous = 1000.0f;
						break;
					case 2:
						thous = 2000.0f;
						break;
					case 3:
						thous = 3000.0f;
						break;
					case 4:
						thous = 4000.0f;
						break;
					case 5:
						thous = 5000.0f;
						break;
					case 6:
						thous = 6000.0f;
						break;
					case 7:
						thous = 7000.0f;
						break;
					case 8:
						thous = 8000.0f;
						break;
					case 9:
						thous = 9000.0f;
						break;
				}
				intStart++;
			}

			// hundreds
			if (intNumbers > 2) {
				c = stream[intStart] - 48;
				switch (c) {
					case 1:
						hundreds = 100.0f;
						break;
					case 2:
						hundreds = 200.0f;
						break;
					case 3:
						hundreds = 300.0f;
						break;
					case 4:
						hundreds = 400.0f;
						break;
					case 5:
						hundreds = 500.0f;
						break;
					case 6:
						hundreds = 600.0f;
						break;
					case 7:
						hundreds = 700.0f;
						break;
					case 8:
						hundreds = 800.0f;
						break;
					case 9:
						hundreds = 900.0f;
						break;
				}
				intStart++;
			}

			// tens
			if (intNumbers > 1) {
				c = stream[intStart] - 48;
				switch (c) {
					case 1:
						tens = 10.0f;
						break;
					case 2:
						tens = 20.0f;
						break;
					case 3:
						tens = 30.0f;
						break;
					case 4:
						tens = 40.0f;
						break;
					case 5:
						tens = 50.0f;
						break;
					case 6:
						tens = 60.0f;
						break;
					case 7:
						tens = 70.0f;
						break;
					case 8:
						tens = 80.0f;
						break;
					case 9:
						tens = 90.0f;
						break;
				}
				intStart++;
			}

			// units
			if (intNumbers > 0) {
				c = stream[intStart] - 48;
				switch (c) {
					case 1:
						units = 1.0f;
						break;
					case 2:
						units = 2.0f;
						break;
					case 3:
						units = 3.0f;
						break;
					case 4:
						units = 4.0f;
						break;
					case 5:
						units = 5.0f;
						break;
					case 6:
						units = 6.0f;
						break;
					case 7:
						units = 7.0f;
						break;
					case 8:
						units = 8.0f;
						break;
					case 9:
						units = 9.0f;
						break;
				}
			}

			// tenths
			if (decNumbers > 1) {
				decStart++; // move beyond.
				c = stream[decStart] - 48;
				switch (c) {
					case 1:
						tenths = 0.1f;
						break;
					case 2:
						tenths = 0.2f;
						break;
					case 3:
						tenths = 0.3f;
						break;
					case 4:
						tenths = 0.4f;
						break;
					case 5:
						tenths = 0.5f;
						break;
					case 6:
						tenths = 0.6f;
						break;
					case 7:
						tenths = 0.7f;
						break;
					case 8:
						tenths = 0.8f;
						break;
					case 9:
						tenths = 0.9f;
						break;
				}
			}

			// hundredths
			if (decNumbers > 2) {
				decStart++; // move beyond.
				c = stream[decStart] - 48;
				switch (c) {
					case 1:
						hundredths = 0.01f;
						break;
					case 2:
						hundredths = 0.02f;
						break;
					case 3:
						hundredths = 0.03f;
						break;
					case 4:
						hundredths = 0.04f;
						break;
					case 5:
						hundredths = 0.05f;
						break;
					case 6:
						hundredths = 0.06f;
						break;
					case 7:
						hundredths = 0.07f;
						break;
					case 8:
						hundredths = 0.08f;
						break;
					case 9:
						hundredths = 0.09f;
						break;
				}
			}

			// thousands
			if (decNumbers > 3) {
				decStart++; // move beyond.
				c = stream[decStart] - 48;
				switch (c) {
					case 1:
						thousands = 0.001f;
						break;
					case 2:
						thousands = 0.002f;
						break;
					case 3:
						thousands = 0.003f;
						break;
					case 4:
						thousands = 0.004f;
						break;
					case 5:
						thousands = 0.005f;
						break;
					case 6:
						thousands = 0.006f;
						break;
					case 7:
						thousands = 0.007f;
						break;
					case 8:
						thousands = 0.008f;
						break;
					case 9:
						thousands = 0.009f;
						break;
				}
			}

			// tenthousands
			if (decNumbers > 4) {
				decStart++; // move beyond.
				c = stream[decStart] - 48;
				switch (c) {
					case 1:
						tenthousands = 0.0001f;
						break;
					case 2:
						tenthousands = 0.0002f;
						break;
					case 3:
						tenthousands = 0.0003f;
						break;
					case 4:
						tenthousands = 0.0004f;
						break;
					case 5:
						tenthousands = 0.0005f;
						break;
					case 6:
						tenthousands = 0.0006f;
						break;
					case 7:
						tenthousands = 0.0007f;
						break;
					case 8:
						tenthousands = 0.0008f;
						break;
					case 9:
						tenthousands = 0.0009f;
						break;
				}
			}

			// 100thousands
			if (decNumbers > 5) {
				decStart++; // move beyond.
				c = stream[decStart] - 48;

				switch (c) {
					case 1:
						hunthousands = 0.00001f;
						break;
					case 2:
						hunthousands = 0.00002f;
						break;
					case 3:
						hunthousands = 0.00003f;
						break;
					case 4:
						hunthousands = 0.00004f;
						break;
					case 5:
						hunthousands = 0.00005f;
						break;
					case 6:
						hunthousands = 0.00006f;
						break;
					case 7:
						hunthousands = 0.00007f;
						break;
					case 8:
						hunthousands = 0.00008f;
						break;
					case 9:
						hunthousands = 0.00009f;
						break;
				}
			}

			if (decNumbers > 6) {
				decStart++; // move beyond.
				c = stream[decStart] - 48;

				switch (c) {
					case 1:
						millis = 0.000001f;
						break;
					case 2:
						millis = 0.000002f;
						break;
					case 3:
						millis = 0.000003f;
						break;
					case 4:
						millis = 0.000004f;
						break;
					case 5:
						millis = 0.000005f;
						break;
					case 6:
						millis = 0.000006f;
						break;
					case 7:
						millis = 0.000007f;
						break;
					case 8:
						millis = 0.000008f;
						break;
					case 9:
						millis = 0.000009f;
						break;
				}
			}

			dec = tenths + hundredths + thousands + tenthousands + hunthousands + millis;
			num = thous + hundreds + tens + units;
			d = num + dec;

		}

		if (isMinus) return -d;
		else return d;
	}

	/**
	 * turn stream of bytes into a flaot number
	 */
	public static float parseFloat(int start, int end, byte[] stream) {

		float d, dec, num;

		int ptr = end;
		int intStart = start;
		boolean isMinus = false;
		// hand optimised float code

		// find decimal point
		for (int j = end - 1; j > start - 1; j--) {
			if (stream[j] == 46) { // '.'=46
				ptr = j;
				break;
			}
		}

		int intChars = ptr;

		int decStart = ptr;

		// allow for minus
		if (stream[start] == 43) { // '+'=43
			intChars--;
			intStart++;
		}
		else
			if (stream[start] == 45) { // '-'=45
				// intChars--;
				intStart++;
				isMinus = true;
			}

		// optimisations
		int intNumbers = intChars - intStart;
		int decNumbers = end - ptr;

		if (intNumbers > 4) {// || decNumbers>4){ //non-optimised to cover others
			isMinus = false;

			int count = end - start;
			byte[] floatVal = new byte[count];

			System.arraycopy(stream, start, floatVal, 0, count);

			// System.out.println(new String(floatVal)+"<");
			d = Float.parseFloat(new String(floatVal));

		}
		else {

			float thous = 0f, units = 0f, tens = 0f, hundreds = 0f, tenths = 0f, hundredths = 0f, thousands = 0f, tenthousands = 0f, hunthousands = 0f, millis = 0f;
			int c;

			// thousands
			if (intNumbers > 3) {
				c = stream[intStart] - 48;
				switch (c) {
					case 1:
						thous = 1000.0f;
						break;
					case 2:
						thous = 2000.0f;
						break;
					case 3:
						thous = 3000.0f;
						break;
					case 4:
						thous = 4000.0f;
						break;
					case 5:
						thous = 5000.0f;
						break;
					case 6:
						thous = 6000.0f;
						break;
					case 7:
						thous = 7000.0f;
						break;
					case 8:
						thous = 8000.0f;
						break;
					case 9:
						thous = 9000.0f;
						break;
				}
				intStart++;
			}

			// hundreds
			if (intNumbers > 2) {
				c = stream[intStart] - 48;
				switch (c) {
					case 1:
						hundreds = 100.0f;
						break;
					case 2:
						hundreds = 200.0f;
						break;
					case 3:
						hundreds = 300.0f;
						break;
					case 4:
						hundreds = 400.0f;
						break;
					case 5:
						hundreds = 500.0f;
						break;
					case 6:
						hundreds = 600.0f;
						break;
					case 7:
						hundreds = 700.0f;
						break;
					case 8:
						hundreds = 800.0f;
						break;
					case 9:
						hundreds = 900.0f;
						break;
				}
				intStart++;
			}

			// tens
			if (intNumbers > 1) {
				c = stream[intStart] - 48;
				switch (c) {
					case 1:
						tens = 10.0f;
						break;
					case 2:
						tens = 20.0f;
						break;
					case 3:
						tens = 30.0f;
						break;
					case 4:
						tens = 40.0f;
						break;
					case 5:
						tens = 50.0f;
						break;
					case 6:
						tens = 60.0f;
						break;
					case 7:
						tens = 70.0f;
						break;
					case 8:
						tens = 80.0f;
						break;
					case 9:
						tens = 90.0f;
						break;
				}
				intStart++;
			}

			// units
			if (intNumbers > 0) {
				c = stream[intStart] - 48;
				switch (c) {
					case 1:
						units = 1.0f;
						break;
					case 2:
						units = 2.0f;
						break;
					case 3:
						units = 3.0f;
						break;
					case 4:
						units = 4.0f;
						break;
					case 5:
						units = 5.0f;
						break;
					case 6:
						units = 6.0f;
						break;
					case 7:
						units = 7.0f;
						break;
					case 8:
						units = 8.0f;
						break;
					case 9:
						units = 9.0f;
						break;
				}
			}

			// tenths
			if (decNumbers > 1) {
				decStart++; // move beyond.
				c = stream[decStart] - 48;
				switch (c) {
					case 1:
						tenths = 0.1f;
						break;
					case 2:
						tenths = 0.2f;
						break;
					case 3:
						tenths = 0.3f;
						break;
					case 4:
						tenths = 0.4f;
						break;
					case 5:
						tenths = 0.5f;
						break;
					case 6:
						tenths = 0.6f;
						break;
					case 7:
						tenths = 0.7f;
						break;
					case 8:
						tenths = 0.8f;
						break;
					case 9:
						tenths = 0.9f;
						break;
				}
			}

			// hundredths
			if (decNumbers > 2) {
				decStart++; // move beyond.
				c = stream[decStart] - 48;
				switch (c) {
					case 1:
						hundredths = 0.01f;
						break;
					case 2:
						hundredths = 0.02f;
						break;
					case 3:
						hundredths = 0.03f;
						break;
					case 4:
						hundredths = 0.04f;
						break;
					case 5:
						hundredths = 0.05f;
						break;
					case 6:
						hundredths = 0.06f;
						break;
					case 7:
						hundredths = 0.07f;
						break;
					case 8:
						hundredths = 0.08f;
						break;
					case 9:
						hundredths = 0.09f;
						break;
				}
			}

			// thousands
			if (decNumbers > 3) {
				decStart++; // move beyond.
				c = stream[decStart] - 48;
				switch (c) {
					case 1:
						thousands = 0.001f;
						break;
					case 2:
						thousands = 0.002f;
						break;
					case 3:
						thousands = 0.003f;
						break;
					case 4:
						thousands = 0.004f;
						break;
					case 5:
						thousands = 0.005f;
						break;
					case 6:
						thousands = 0.006f;
						break;
					case 7:
						thousands = 0.007f;
						break;
					case 8:
						thousands = 0.008f;
						break;
					case 9:
						thousands = 0.009f;
						break;
				}
			}

			// tenthousands
			if (decNumbers > 4) {
				decStart++; // move beyond.
				c = stream[decStart] - 48;
				switch (c) {
					case 1:
						tenthousands = 0.0001f;
						break;
					case 2:
						tenthousands = 0.0002f;
						break;
					case 3:
						tenthousands = 0.0003f;
						break;
					case 4:
						tenthousands = 0.0004f;
						break;
					case 5:
						tenthousands = 0.0005f;
						break;
					case 6:
						tenthousands = 0.0006f;
						break;
					case 7:
						tenthousands = 0.0007f;
						break;
					case 8:
						tenthousands = 0.0008f;
						break;
					case 9:
						tenthousands = 0.0009f;
						break;
				}
			}

			// 100thousands
			if (decNumbers > 5) {
				decStart++; // move beyond.
				c = stream[decStart] - 48;

				switch (c) {
					case 1:
						hunthousands = 0.00001f;
						break;
					case 2:
						hunthousands = 0.00002f;
						break;
					case 3:
						hunthousands = 0.00003f;
						break;
					case 4:
						hunthousands = 0.00004f;
						break;
					case 5:
						hunthousands = 0.00005f;
						break;
					case 6:
						hunthousands = 0.00006f;
						break;
					case 7:
						hunthousands = 0.00007f;
						break;
					case 8:
						hunthousands = 0.00008f;
						break;
					case 9:
						hunthousands = 0.00009f;
						break;
				}
			}

			if (decNumbers > 6) {
				decStart++; // move beyond.
				c = stream[decStart] - 48;

				switch (c) {
					case 1:
						millis = 0.000001f;
						break;
					case 2:
						millis = 0.000002f;
						break;
					case 3:
						millis = 0.000003f;
						break;
					case 4:
						millis = 0.000004f;
						break;
					case 5:
						millis = 0.000005f;
						break;
					case 6:
						millis = 0.000006f;
						break;
					case 7:
						millis = 0.000007f;
						break;
					case 8:
						millis = 0.000008f;
						break;
					case 9:
						millis = 0.000009f;
						break;
				}
			}

			dec = tenths + hundredths + thousands + tenthousands + hunthousands + millis;
			num = thous + hundreds + tens + units;
			d = num + dec;

		}

		if (isMinus) return -d;
		else return d;
	}
}
