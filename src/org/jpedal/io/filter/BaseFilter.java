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
 * BaseFilter.java
 * ---------------
 */
package org.jpedal.io.filter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.jpedal.io.ObjectStore;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

/**
 * common values
 */
public class BaseFilter {

	final PdfObject decodeParms;
	BufferedInputStream bis;
	BufferedOutputStream streamCache;
	Map cachedObjects;

	BaseFilter(PdfObject decodeParms) {

		this.decodeParms = decodeParms;
	}

	public boolean hasError() {
		return false;
	}

	void setupCachedObjectForDecoding(String cacheName) throws IOException {

		// rename file
		File tempFile2 = File.createTempFile("jpedal", ".raw", new File(ObjectStore.temp_dir));
		this.cachedObjects.put(tempFile2.getAbsolutePath(), "x"); // store to
																	// delete when
																	// PDF closed
		ObjectStore.copy(cacheName, tempFile2.getAbsolutePath());
		File rawFile = new File(cacheName);
		rawFile.delete();

		// where its going after decompression
		this.streamCache = new BufferedOutputStream(new FileOutputStream(cacheName));

		// where data is coming from
		this.bis = new BufferedInputStream(new FileInputStream(tempFile2));
	}

	byte[] applyPredictor(int predictor, byte[] data, int colors, int bitsPerComponent, int columns) throws Exception {

		// no prediction (TIFF =1 PNG=10)
		if (predictor == 1) {// || predictor==10){
			return data;
		}
		else {

			boolean isCached = data == null;
			if (isCached) {
				applyPredictorFunction(predictor, this.bis, this.streamCache, colors, bitsPerComponent, columns);
				return null;
				// calling it twice to size buffer itakes 1% on sample file as opposed to 9% to call it once with OutputStream
			}
			else
				if (1 == 2) {
					BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
					ByteArrayOutputStream bos = new ByteArrayOutputStream();

					applyPredictorFunction(predictor, bis, bos, colors, bitsPerComponent, columns);

					return bos.toByteArray();
				}
				else {

					ByteArrayInputStream byis = new ByteArrayInputStream(data);
					BufferedInputStream bis = new BufferedInputStream(byis);

					// just workout size
					int count = applyPredictorFunction2(predictor, bis, null, colors, bitsPerComponent, columns);

					byis.close();
					bis.close();

					byte[] bos = new byte[count];

					byis = new ByteArrayInputStream(data);
					bis = new BufferedInputStream(byis);

					// now actually get the size.
					applyPredictorFunction2(predictor, bis, bos, colors, bitsPerComponent, columns);

					byis.close();
					bis.close();

					return bos;
				}
		}
	}

	/**
	 * implement predictor function
	 */
	private static void applyPredictorFunction(int mainPred, BufferedInputStream bis, OutputStream bos, int colors, int bitsPerComponent, int columns)
			throws Exception {

		int predictor;
		int bytesAvailable = bis.available();

		/**
		 * calculate values
		 */

		int bpp = (colors * bitsPerComponent + 7) / 8; // actual Bytes for a pixel;

		int rowLength = (columns * colors * bitsPerComponent + 7) / 8 + bpp; // length of each row + predictor

		// array to hold 2 lines
		byte[] thisLine = new byte[rowLength];
		byte[] lastLine = new byte[rowLength];

		// extra predictor needed for optimization
		int curPred;

		// actual processing loop
		try {
			int byteCount = 0;
			while (true) {

				// exit after all used
				if (bytesAvailable <= byteCount) break;

				// set predictor
				predictor = mainPred;

				/**
				 * read line
				 */
				int i = 0;
				int offset = bpp;

				// PNG optimization.
				if (predictor >= 10) {
					curPred = bis.read();
					if (curPred == -1) {
						break;
					}
					curPred += 10;
				}
				else {
					curPred = predictor;
				}

				while (offset < rowLength) {

					i = bis.read(thisLine, offset, rowLength - offset);

					if (i == -1) break;

					offset += i;
					byteCount += i;
				}

				if (i == -1) break;

				// apply

				switch (curPred) {

					case 2: // tiff (same as sub)
						for (int i1 = bpp; i1 < rowLength; i1++) {

							int sub = thisLine[i1] & 0xff;
							int raw = thisLine[i1 - bpp] & 0xff;
							thisLine[i1] = (byte) ((sub + raw) & 0xff);
							bos.write(thisLine[i1]);

						}
						break;

					case 10: // just pass through
						for (int i1 = bpp; i1 < rowLength; i1++) {

							bos.write(thisLine[i1]);

						}

						break;

					case 11: // sub
						for (int i1 = bpp; i1 < rowLength; i1++) {

							int sub = thisLine[i1] & 0xff;
							int raw = thisLine[i1 - bpp] & 0xff;
							thisLine[i1] = (byte) ((sub + raw));
							bos.write(thisLine[i1]);
						}
						break;

					case 12: // up
						for (int i1 = bpp; i1 < rowLength; i1++) {

							int sub = (lastLine[i1] & 0xff) + (thisLine[i1] & 0xff);
							thisLine[i1] = (byte) (sub);
							bos.write(thisLine[i1]);
						}

						break;

					case 13: // avg
						for (int i1 = bpp; i1 < rowLength; i1++) {

							int av = thisLine[i1] & 0xff;
							int floor = ((thisLine[i1 - bpp] & 0xff) + (lastLine[i1] & 0xff) >> 1);
							thisLine[i1] = (byte) (av + floor);
							bos.write(thisLine[i1]);
						}
						break;

					case 14: // paeth (see http://www.w3.org/TR/PNG-Filters.html)
						for (int i1 = bpp; i1 < rowLength; i1++) {

							int a = thisLine[i1 - bpp] & 0xff, b = lastLine[i1] & 0xff, c = lastLine[i1 - bpp] & 0xff;

							int p = a + b - c;

							int pa = p - a, pb = p - b, pc = p - c;

							// make sure positive
							if (pa < 0) pa = -pa;
							if (pb < 0) pb = -pb;
							if (pc < 0) pc = -pc;

							if (pa <= pb && pa <= pc) thisLine[i1] = (byte) (thisLine[i1] + a);
							else
								if (pb <= pc) thisLine[i1] = (byte) (thisLine[i1] + b);
								else thisLine[i1] = (byte) (thisLine[i1] + c);

							bos.write(thisLine[i1]);

						}
						break;

					case 15:
						// implemented inside main code body
						break;
					default:

						break;
				}

				// add to output and update line
				System.arraycopy(thisLine, 0, lastLine, 0, lastLine.length);

			}

			bos.flush();
			bos.close();

		}
		catch (Exception e) {
			e.printStackTrace();
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " accessing Predictor");
		}
	}

	/**
	 * implement predictor function
	 */
	private static int applyPredictorFunction2(int mainPred, BufferedInputStream bis, byte[] bos, int colors, int bitsPerComponent, int columns)
			throws Exception {

		int count = 0;

		int predictor;
		int bytesAvailable = bis.available();

		/**
		 * calculate values
		 */

		int bpp = (colors * bitsPerComponent + 7) / 8; // actual Bytes for a pixel;

		int rowLength = (columns * colors * bitsPerComponent + 7) / 8 + bpp; // length of each row + predictor

		// array to hold 2 lines
		byte[] thisLine = new byte[rowLength];
		byte[] lastLine = new byte[rowLength];

		// extra predictor needed for optimization
		int curPred;

		// actual processing loop
		try {
			int byteCount = 0;
			while (true) {

				// exit after all used
				if (bytesAvailable <= byteCount) break;

				// set predictor
				predictor = mainPred;

				/**
				 * read line
				 */
				int i = 0;
				int offset = bpp;

				// PNG optimization.
				if (predictor >= 10) {
					curPred = bis.read();
					if (curPred == -1) {
						break;
					}
					curPred += 10;
				}
				else {
					curPred = predictor;
				}

				while (offset < rowLength) {

					i = bis.read(thisLine, offset, rowLength - offset);

					if (i == -1) break;

					offset += i;
					byteCount += i;
				}

				if (i == -1) break;

				// apply

				switch (curPred) {

					case 2: // tiff (same as sub)
						for (int i1 = bpp; i1 < rowLength; i1++) {

							int sub = thisLine[i1] & 0xff;
							int raw = thisLine[i1 - bpp] & 0xff;
							thisLine[i1] = (byte) ((sub + raw) & 0xff);
							if (bos != null) bos[count] = thisLine[i1];

							count++;

						}
						break;

					case 10: // just pass through
						for (int i1 = bpp; i1 < rowLength; i1++) {

							if (bos != null) bos[count] = thisLine[i1];

							count++;

						}

						break;

					case 11: // sub
						for (int i1 = bpp; i1 < rowLength; i1++) {

							int sub = thisLine[i1] & 0xff;
							int raw = thisLine[i1 - bpp] & 0xff;
							thisLine[i1] = (byte) ((sub + raw));

							if (bos != null) bos[count] = thisLine[i1];

							count++;
						}
						break;

					case 12: // up
						for (int i1 = bpp; i1 < rowLength; i1++) {

							int sub = (lastLine[i1] & 0xff) + (thisLine[i1] & 0xff);
							thisLine[i1] = (byte) (sub);

							if (bos != null) bos[count] = thisLine[i1];

							count++;
						}

						break;

					case 13: // avg
						for (int i1 = bpp; i1 < rowLength; i1++) {

							int av = thisLine[i1] & 0xff;
							int floor = ((thisLine[i1 - bpp] & 0xff) + (lastLine[i1] & 0xff) >> 1);
							thisLine[i1] = (byte) (av + floor);

							if (bos != null) bos[count] = thisLine[i1];

							count++;
						}
						break;

					case 14: // paeth (see http://www.w3.org/TR/PNG-Filters.html)
						for (int i1 = bpp; i1 < rowLength; i1++) {

							int a = thisLine[i1 - bpp] & 0xff, b = lastLine[i1] & 0xff, c = lastLine[i1 - bpp] & 0xff;

							int p = a + b - c;

							int pa = p - a, pb = p - b, pc = p - c;

							// make sure positive
							if (pa < 0) pa = -pa;
							if (pb < 0) pb = -pb;
							if (pc < 0) pc = -pc;

							if (pa <= pb && pa <= pc) thisLine[i1] = (byte) (thisLine[i1] + a);
							else
								if (pb <= pc) thisLine[i1] = (byte) (thisLine[i1] + b);
								else thisLine[i1] = (byte) (thisLine[i1] + c);

							if (bos != null) bos[count] = thisLine[i1];

							count++;

						}
						break;

					case 15:
						// implemented inside main code body
						break;
					default:

						break;
				}

				// add to output and update line
				System.arraycopy(thisLine, 0, lastLine, 0, lastLine.length);

			}

		}
		catch (Exception e) {
			e.printStackTrace();

			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " accessing Predictor");
		}

		return count;
	}
}
