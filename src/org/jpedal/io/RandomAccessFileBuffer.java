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
 * RandomAccessFileBuffer.java
 * ---------------
 */

package org.jpedal.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;

import org.jpedal.utils.LogWriter;

public class RandomAccessFileBuffer extends RandomAccessFile implements RandomAccessBuffer {

	private String fileName = "";

	public RandomAccessFileBuffer(File file, String mode) throws FileNotFoundException {
		super(file, mode);
		this.fileName = file.getAbsolutePath();
	}

	public RandomAccessFileBuffer(String file, String mode) throws FileNotFoundException {

		super(file, mode);
		this.fileName = file;
	}

	@Override
	public byte[] getPdfBuffer() {

		URL url;
		byte[] pdfByteArray = null;
		InputStream is;
		ByteArrayOutputStream os;

		try {
			url = new URL("file:///" + this.fileName);

			is = url.openStream();
			os = new ByteArrayOutputStream();

			// Download buffer
			byte[] buffer = new byte[4096];

			// Download the PDF document
			int read;
			while ((read = is.read(buffer)) != -1) {
				os.write(buffer, 0, read);
			}

			os.flush();

			// Close streams
			is.close();
			os.close();

			// Copy output stream to byte array
			pdfByteArray = os.toByteArray();

		}
		catch (IOException e) {
			e.printStackTrace();
			if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Exception " + e + " getting byte[] for " + this.fileName);
		}

		return pdfByteArray;
	}
}
