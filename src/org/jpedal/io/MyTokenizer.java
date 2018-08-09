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
 * MyTokenizer.java
 * ---------------
 */
package org.jpedal.io;

import org.jpedal.utils.StringUtils;

/**
 * encapsualtes function to read values from a text string
 */
class MyTokenizer {

	/** holds content */
	private final byte[] content;

	/** pointers to position reached in string */
	private int currentCharPointer;

	/** length of string */
	private final int stringLength;

	/**
	 * initialise text with value
	 */
	public MyTokenizer(String line) {

		// turn string into byte stream
		this.content = StringUtils.toBytes(line);

		this.stringLength = this.content.length;
	}

	/** get the char */
	private char getChar(int pointer) {

		int number = (this.content[pointer] & 0xFF);

		return (char) number;
	}

	/**
	 * read a next value up to space
	 */
	final public String nextToken() {

		StringBuilder tokenValue = new StringBuilder();

		boolean hasChars = false;

		char nextChar = getChar(this.currentCharPointer);
		this.currentCharPointer++;

		// exit on space, otherwise add to string
		while (true) {

			if (nextChar != ' ') {
				tokenValue.append(nextChar);
				hasChars = true;
			}

			if (((nextChar == ' ') && (hasChars)) | (this.currentCharPointer == this.stringLength)) break;

			nextChar = getChar(this.currentCharPointer);
			this.currentCharPointer++;

		}

		return tokenValue.toString();
	}

	/**
	 * count number of tokens using space as deliminator
	 */
	public int countTokens() {

		int tokenCount = 1;

		/**
		 * count spaces in string, ignoring double spaces and any first and last space
		 */
		int count = this.stringLength - 1;
		for (int i = 1; i < count; i++) {
			if ((this.content[i] == 32) && (this.content[i - 1] != 32)) tokenCount++;
		}

		return tokenCount;
	}
}
