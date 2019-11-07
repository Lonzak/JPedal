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
 * FontData.java
 * ---------------
 */
package org.jpedal.fonts.objects;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.jpedal.utils.LogWriter;

/**
 * provides access to font data and caches large objects
 */
public class FontData {

	private byte[] fontData;

	/** flag to show if all fontData in memory or just some */
	private boolean isInMemory = false;

	/** real size of font object */
	private int fullLength = 0;

	/** offset to actual block loaded */
	private int offset = 0;

	/** bytes size of font we keep in memory */
	public static int maxSizeAllowedInMemory = -1;

	/** max size of data kept in memory at any one point */
	private int blockSize = 8192;

	private RandomAccessFile fontFile = null;

	public FontData(byte[] fontData) {

		this.fontData = fontData;

		this.isInMemory = true;

		this.fullLength = fontData.length;
	}

	/**
	 * pass in name of temp file on disk so we can just read part at a time - if not part of PDF user is responsible for deleting
	 * 
	 * @param cachedFile
	 */
	public FontData(String cachedFile) {// , byte[] stream) {

		try {
			this.fontFile = new RandomAccessFile(cachedFile, "r");
			this.fullLength = (int) this.fontFile.length();

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		// if small read all
		if (this.fullLength < maxSizeAllowedInMemory) {

			this.blockSize = maxSizeAllowedInMemory;

			adjustForCache(0);

			this.isInMemory = true;

		}
	}

	public byte getByte(int pointer) {

		if (!this.isInMemory) pointer = adjustForCache(pointer);

		// System.err.println("Now="+pointer+" "+fontData.length+" inMemory="+isInMemory);

		if (pointer >= this.fontData.length) return 0;
		else return this.fontData[pointer];
	}

	/**
	 * check block in memory, read if not and adjust pointer
	 * 
	 * @param pointer
	 */
	private int adjustForCache(int pointer) {

		// see if in memory and load if not
		if (this.fontData == null || pointer < this.offset || pointer >= (this.offset + this.blockSize - 1)) {

			try {

				this.fontFile.seek(pointer);
				this.fontData = new byte[this.blockSize];

				this.fontFile.read(this.fontData);

			}
			catch (IOException e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}

			this.offset = pointer;

		}

		// subtract offset to make it fall in loaded range
		return pointer - this.offset;
	}

	private int adjustForCache(int pointer, int blockSize) {

		// see if in memory and load if not
		// if(fontData==null || pointer<offset || pointer>=(offset+blockSize-1)){

		try {

			this.fontFile.seek(pointer);
			this.fontData = new byte[blockSize];

			this.fontFile.read(this.fontData);

		}
		catch (IOException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
		this.offset = pointer;

		// }

		// subtract offset to make it fall in loaded range
		return pointer - this.offset;
	}

	public byte[] getBytes(int startPointer, int length) {

		if (!this.isInMemory) startPointer = adjustForCache(startPointer, length + 1);

		byte[] block = new byte[length];
		System.arraycopy(this.fontData, startPointer, block, 0, length);
		return block;
	}

	/** total length of FontData in bytes */
	public int length() {

		if (this.isInMemory) return this.fontData.length;
		else return this.fullLength;
	}

	public void close() {
		if (this.fontFile != null) try {
			this.fontFile.close();
		}
		catch (IOException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}
}
