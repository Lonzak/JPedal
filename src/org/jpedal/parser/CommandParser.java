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
 * CommandParser.java
 * ---------------
 */
package org.jpedal.parser;

public class CommandParser {

	private byte[] characterStream;

	private int commandID = -1;

	private final static int[] prefixes = { 60, 40 }; // important that [ comes before ( '<'=60 '('=40

	private final static int[] suffixes = { 62, 41 }; // '>'=62 ')'=41

	private static final int[][] intValues = { { 0, 100000, 200000, 300000, 400000, 500000, 600000, 700000, 800000, 900000 },
			{ 0, 10000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000 }, { 0, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000 },
			{ 0, 100, 200, 300, 400, 500, 600, 700, 800, 900 }, { 0, 10, 20, 30, 40, 50, 60, 70, 80, 90 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 } };

	/** maximum ops */
	private static final int MAXOPS = 50;

	/** lookup table for operands on commands */
	private int[] opStart = new int[MAXOPS];
	private int[] opEnd = new int[MAXOPS];

	private int operandCount;

	/** current op */
	private int currentOp = 0;

	public CommandParser(byte[] characterStr) {
		this.characterStream = characterStr;
	}

	int getCommandValues(int dataPointer, int streamSize, int tokenNumber) {

		final boolean debug = false;

		int count = prefixes.length;
		int nextChar = this.characterStream[dataPointer], start, end = 0;

		this.commandID = -1;
		int sLen = this.characterStream.length;

		int current = nextChar;

		if (nextChar == 13 || nextChar == 10 || nextChar == 32 || nextChar == 9) {

			dataPointer++;

			while (true) { // read next valid char

				if (dataPointer == streamSize) // allow for end of stream
				break;

				current = this.characterStream[dataPointer];

				if (current != 13 && current != 10 && current != 32 && current != 9) break;

				dataPointer++;

			}
		}

		// lose any comments in stream which start %
		while (current == 37) {

			dataPointer++;
			while (true) { // read next valid char

				if (dataPointer == streamSize) // allow for end of stream
				break;

				current = this.characterStream[dataPointer];

				if (current == 13 || current == 10) {

					// exit at end of comment (shown by line ending)
					// loop need as can get double spacing (ie debug2/hpbrokenFIle)
					while (dataPointer + 1 < streamSize && this.characterStream[dataPointer + 1] == 10) {
						dataPointer++;
						current = this.characterStream[dataPointer];
					}

					break;
				}

				dataPointer++;

			}

			dataPointer++;

			if (dataPointer >= streamSize) // allow for end of stream
			break;

			current = this.characterStream[dataPointer];
		}

		if (dataPointer == streamSize) // allow for end of stream
		return dataPointer;

		/**
		 * read in value (note several options)
		 */
		boolean matchFound = false;
		int type = getType(current, dataPointer);

		if (type == 3) { // option - its an aphabetical so may be command or operand values

			start = dataPointer;

			while (true) { // read next valid char

				dataPointer++;
				if ((dataPointer) >= sLen) // trap for end of stream
				break;

				current = this.characterStream[dataPointer];
				// return,space,( / or [
				if (current == 13 || current == 10 || current == 32 || current == 40 || current == 47 || current == 91 || current == 9
						|| current == '<') break;

			}

			end = dataPointer - 1;

			if (end >= sLen) return end;

			// move back if ends with / or [
			int endC = this.characterStream[end];
			if (endC == 47 || endC == 91 || endC == '<') end--;

			// see if command
			this.commandID = -1;
			if (end - start < 3) { // no command over 3 chars long
				// @turn key into ID.
				// convert token to int
				int key = 0, x = 0;
				for (int i2 = end; i2 > start - 1; i2--) {
					key = key + (this.characterStream[i2] << x);
					x = x + 8;
				}
				this.commandID = Cmd.getCommandID(key);
			}

			/**
			 * if command execute otherwise add to stack
			 */
			if (this.commandID == -1) {

				this.opStart[this.currentOp] = start;
				this.opEnd[this.currentOp] = end;

				this.currentOp++;
				if (this.currentOp == MAXOPS) this.currentOp = 0;
				this.operandCount++;
			}
			else {

				// showCommands=(tokenNumber>6300);
				// this makes rest of page disappear
				// if(tokenNumber>70)
				// return streamSize;

				// reorder values so work
				if (this.operandCount > 0) {

					int[] orderedOpStart = new int[MAXOPS];
					int[] orderedOpEnd = new int[MAXOPS];
					int opid = 0;
					for (int jj = this.currentOp - 1; jj > -1; jj--) {

						orderedOpStart[opid] = this.opStart[jj];
						orderedOpEnd[opid] = this.opEnd[jj];
						if (opid == this.operandCount) jj = -1;
						opid++;
					}
					if (opid == this.operandCount) {
						this.currentOp--; // decrease to make loop comparison faster
						for (int jj = MAXOPS - 1; jj > this.currentOp; jj--) {

							orderedOpStart[opid] = this.opStart[jj];
							orderedOpEnd[opid] = this.opEnd[jj];
							if (opid == this.operandCount) jj = this.currentOp;
							opid++;
						}
						this.currentOp++;
					}

					this.opStart = orderedOpStart;
					this.opEnd = orderedOpEnd;
				}

				// use negative to flag values found
				return -dataPointer;

			}
		}
		else
			if (type != 4) {

				start = dataPointer;

				// option << values >>
				// option [value] and [value (may have spaces and brackets)]
				if (type == 1 || type == 2) {

					boolean inStream = false;
					matchFound = true;

					int last = 32; // ' '=32

					while (true) { // read rest of chars

						if (last == 92 && current == 92) // allow for \\ \\=92
						last = 120; // 'x'=120

						else last = current;

						dataPointer++; // roll on counter

						if (dataPointer == sLen) // allow for end of stream
						break;

						// read next valid char, converting CR to space
						current = this.characterStream[dataPointer];
						if (current == 13 || current == 10 || current == 9) current = 32;

						// exit at end
						boolean isBreak = false;

						if (current == 62 && last == 62 && (type == 1)) // '>'=62
						isBreak = true;

						if (type == 2) {
							// stream flags
							if ((current == 40) && (last != 92)) // '('=40 '\\'=92
							inStream = true;
							else
								if ((current == 41) && (last != 92)) inStream = false;

							// exit at end
							if (!inStream && current == 93 && last != 92) // ']'=93
							isBreak = true;
						}

						if (isBreak) break;
					}

					end = dataPointer;
				}

				if (!matchFound) { // option 3 other braces

					int last = 32;
					for (int startChars = 0; startChars < count; startChars++) {

						if (current == prefixes[startChars]) {
							matchFound = true;

							start = dataPointer;

							int numOfPrefixs = 0;// counts the brackets when inside a text stream
							while (true) { // read rest of chars

								if ((last == 92) && (current == 92)) // allow for \\ '\\'=92
								last = 120; // 'x'=120
								else last = current;
								dataPointer++; // roll on counter

								if (dataPointer == sLen) break;
								current = this.characterStream[dataPointer]; // read next valid char, converting CR to space
								if (current == 13 || current == 10 || current == 9) current = 32;

								if (current == prefixes[startChars] && last != 92) // '\\'=92
								numOfPrefixs++;

								if ((current == suffixes[startChars]) && (last != 92)) { // exit at end '\\'=92
									if (numOfPrefixs == 0) break;
									else {
										numOfPrefixs--;

									}
								}
							}
							startChars = count; // exit loop after match
						}
					}
					end = dataPointer;
				}

				// option 2 -its a value followed by a deliminator (CR,space,/)
				if (!matchFound) {

					if (debug) System.out.println("Not type 2");

					start = dataPointer;
					int firstChar = this.characterStream[start];

					while (true) { // read next valid char
						dataPointer++;
						if ((dataPointer) == sLen) // trap for end of stream
						break;

						current = this.characterStream[dataPointer];
						if (current == 13 || current == 10 || current == 32 || current == 40 || current == 47 || current == 91 || current == 9
								|| (firstChar == '/' && current == '<')){
							// // '('=40 '/'=47 '['=91
							break;
						}

					}

					end = dataPointer;

					if (debug) System.out.println("end=" + end);
				}

				if (debug) System.out.println("stored start=" + start + " end=" + end);

				if (end < this.characterStream.length) {
					int next = this.characterStream[end];
					if (next == 47 || next == 91) end--;
				}

				this.opStart[this.currentOp] = start;
				this.opEnd[this.currentOp] = end;

				this.currentOp++;
				if (this.currentOp == MAXOPS) this.currentOp = 0;
				this.operandCount++;

			}

		// increment pointer
		if (dataPointer < streamSize) {

			nextChar = this.characterStream[dataPointer];
			if (nextChar != 47 && nextChar != 40 && nextChar != 91 && nextChar != '<') {
				dataPointer++;
			}
		}

		return dataPointer;
	}

	public int getCommandID() {
		return this.commandID;
	}

	private int getType(int current, int dataPointer) {

		int type = 0;

		if (current == 60 && this.characterStream[dataPointer + 1] == 60) // look for <<
		type = 1;
		else
			if (current == 32) type = 4;
			else
				if (current == 91) // [
				type = 2;
				else
					if (current >= 97 && current <= 122) // lower case alphabetical a-z
					type = 3;
					else
						if (current >= 65 && current <= 90) // upper case alphabetical A-Z
						type = 3;
						else
							if (current == 39 || current == 34) // not forgetting the non-alphabetical commands '\'-'\"'/*
							type = 3;

		return type;
	}

	/**
	 * convert to to String
	 */
	String generateOpAsString(int p, boolean loseSlashPrefix) {

		byte[] dataStream = this.characterStream;

		String s;

		int start = this.opStart[p];

		// remove / on keys
		if (loseSlashPrefix && dataStream[start] == 47) start++;

		int end = this.opEnd[p];

		// lose spaces or returns at end
		while ((dataStream[end] == 32) || (dataStream[end] == 13) || (dataStream[end] == 10))
			end--;

		int count = end - start + 1;

		// discount duplicate spaces
		int spaces = 0;
		for (int ii = 0; ii < count; ii++) {
			if ((ii > 0) && ((dataStream[start + ii] == 32) || (dataStream[start + ii] == 13) || (dataStream[start + ii] == 10))
					&& ((dataStream[start + ii - 1] == 32) || (dataStream[start + ii - 1] == 13) || (dataStream[start + ii - 1] == 10))) spaces++;
		}

		char[] charString = new char[count - spaces];
		int pos = 0;

		for (int ii = 0; ii < count; ii++) {
			if ((ii > 0) && ((dataStream[start + ii] == 32) || (dataStream[start + ii] == 13) || (dataStream[start + ii] == 10))
					&& ((dataStream[start + ii - 1] == 32) || (dataStream[start + ii - 1] == 13) || (dataStream[start + ii - 1] == 10))) {}
			else {
				if ((dataStream[start + ii] == 10) || (dataStream[start + ii] == 13)) charString[pos] = ' ';
				else charString[pos] = (char) dataStream[start + ii];
				pos++;
			}
		}

		s = String.copyValueOf(charString);

		return s;
	}

	final float parseFloat(int id) {

		byte[] stream = this.characterStream;

		float f, dec, num;

		int start = this.opStart[id];
		int charCount = this.opEnd[id] - start;

		int floatptr = charCount, intStart = 0;

		boolean isMinus = false;
		// hand optimised float code
		// find decimal point
		for (int j = charCount - 1; j > -1; j--) {
			if (stream[start + j] == 46) { // '.'=46
				floatptr = j;
				break;
			}
		}

		int intChars = floatptr;
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
		int decNumbers = charCount - floatptr;

		if (intNumbers > 3 || decNumbers > 11) { // non-optimised to cover others (tiny decimals on big scaling can add up to a big diff)
			isMinus = false;

			f = Float.parseFloat(this.generateOpAsString(id, false));

		}
		else {

			float units = 0f, tens = 0f, hundreds = 0f, tenths = 0f, hundredths = 0f, thousands = 0f, tenthousands = 0f, hunthousands = 0f;
			int c;

			// hundreds
			if (intNumbers > 2) {
				c = stream[start + intStart] - 48;
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
				c = stream[start + intStart] - 48;
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
				c = stream[start + intStart] - 48;
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
				floatptr++; // move beyond.
				c = stream[start + floatptr] - 48;
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
				floatptr++; // move beyond.
				// c=value.charAt(floatptr)-48;
				c = stream[start + floatptr] - 48;
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
				floatptr++; // move beyond.
				c = stream[start + floatptr] - 48;
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
				floatptr++; // move beyond.
				c = stream[start + floatptr] - 48;
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

			// tenthousands
			if (decNumbers > 5) {
				floatptr++; // move beyond.
				c = stream[start + floatptr] - 48;

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

			dec = tenths + hundredths + thousands + tenthousands + hunthousands;
			num = hundreds + tens + units;
			f = num + dec;

		}

		if (isMinus) return -f;
		else return f;
	}

	float[] getValuesAsFloat() {

		float[] op = new float[this.operandCount];
		for (int i = 0; i < this.operandCount; i++)
			op[i] = parseFloat(i);

		return op;
	}

	String[] getValuesAsString() {

		String[] op = new String[this.operandCount];
		for (int i = 0; i < this.operandCount; i++)
			op[i] = generateOpAsString(i, true);
		return op;
	}

	final int parseInt(int i) {

		int start = this.opStart[i];
		int end = this.opEnd[i];

		byte[] stream = this.characterStream;

		int number = 0, id = 0;

		int charCount = end - start;

		int intStart = 0;
		boolean isMinus = false;

		int intChars = charCount;
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

		if ((intNumbers > 6)) { // non-optimised to cover others
			isMinus = false;
			number = Integer.parseInt(generateOpAsString(id, false));

		}
		else { // optimised lookup version

			int c;

			for (int jj = 5; jj > -1; jj--) {
				if (intNumbers > jj) {
					c = stream[start + intStart] - 48;
					number = number + intValues[5 - jj][c];
					intStart++;
				}
			}
		}

		if (isMinus) return -number;
		else return number;
	}

	public void reset() {
		this.currentOp = 0;
		this.operandCount = 0;
	}

	public int getOperandCount() {
		return this.operandCount;
	}

	public byte[] getStream() {
		return this.characterStream;
	}

	public int getcurrentOp() {
		return this.currentOp;
	}
}
