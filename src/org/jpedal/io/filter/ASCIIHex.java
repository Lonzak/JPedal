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
 * ASCIIHex.java
 * ---------------
 */
package org.jpedal.io.filter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

public class ASCIIHex extends BaseFilter implements PdfFilter {
	public ASCIIHex(PdfObject decodeParms) {
		super(decodeParms);
	}

	/**
	 * asciihexdecode using our own implementation
	 */
	@Override
	public byte[] decode(byte[] data) throws IOException {

		String line;
		StringBuilder value = new StringBuilder();
		StringBuilder valuesRead = new StringBuilder();
		BufferedReader mappingStream = null;
		ByteArrayInputStream bis = null;

		// read in ASCII mode to handle line returns
		try {
			bis = new ByteArrayInputStream(data);
			mappingStream = new BufferedReader(new InputStreamReader(bis));

			// read values into lookup table
			if (mappingStream != null) {

				while (true) {
					line = mappingStream.readLine();

					if (line == null) break;

					// append to data
					valuesRead.append(line);

				}
			}

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " reading ASCII stream ");

		}

		if (mappingStream != null) {
			try {
				mappingStream.close();
				bis.close();
			}
			catch (IOException e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}

		int data_size = valuesRead.length();
		int i = 0, count = 0;
		char current;

		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);

		/** loop to read and process */
		while (true) {

			current = valuesRead.charAt(i);

			if ((current >= '0' && current <= '9') || (current >= 'a' && current <= 'f') || (current >= 'A' && current <= 'F')) {
				value.append(current);
				if (count == 1) {
					bos.write(Integer.valueOf(value.toString(), 16).intValue());
					count = 0;
					value = new StringBuilder();
				}
				else count++;

			}

			if (current == '>') break;

			i++;

			if (i == data_size) break;
		}

		// write any last char
		if (count == 1) {
			value.append('0');
			bos.write(Integer.valueOf(value.toString(), 16).intValue());
		}

		bos.close();

		return bos.toByteArray();
	}

	/**
	 * asciihexdecode using our own implementation
	 */
	@Override
	public void decode(BufferedInputStream bis, BufferedOutputStream streamCache, String cacheName, Map cachedObjects) {

		this.bis = bis;
		this.streamCache = streamCache;
		this.cachedObjects = cachedObjects;

		try {
			StringBuffer value = new StringBuffer();
			char current;

			/** loop to read and process */
			int count = bis.available();
			for (int i = 0; i < count; i++) {

				current = (char) bis.read();
				while (current == '\n')
					current = (char) bis.read();

				if ((current >= '0' && current <= '9') || (current >= 'a' && current <= 'f') || (current >= 'A' && current <= 'F')) {
					value.append(current);
					if (count == 1) {
						streamCache.write(Integer.valueOf(value.toString(), 16).intValue());
						count = 0;
						value = new StringBuffer();
					}
					else count++;

				}

				if (current == '>') break;
			}

			// write any last char
			if (count == 1) {
				value.append('0');
				streamCache.write(Integer.valueOf(value.toString(), 16).intValue());
			}

		}
		catch (IOException e1) {

			if (LogWriter.isOutput()) LogWriter.writeLog("IO exception in RunLength " + e1);
		}
	}

}
