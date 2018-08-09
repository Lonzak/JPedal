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
 * PdfFilteredReader.java
 * ---------------
 */
package org.jpedal.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.io.filter.ASCII85;
import org.jpedal.io.filter.ASCIIHex;
import org.jpedal.io.filter.CCITT;
import org.jpedal.io.filter.Flate;
import org.jpedal.io.filter.LZW;
import org.jpedal.io.filter.PdfFilter;
import org.jpedal.io.filter.RunLength;
import org.jpedal.objects.raw.PdfArrayIterator;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

/**
 * Adds the abilty to decode streams to the PdfFileReader class
 */
public class PdfFilteredReader {

	private final static int A85 = 1116165;

	private final static int AHx = 1120328;

	private final static int ASCII85Decode = 1582784916;

	private final static int ASCIIHexDecode = 2074112677;

	final public static int CCITTFaxDecode = 2108391315;

	private final static int CCF = 1250070;

	private final static int Crypt = 1112096855;

	final public static int DCTDecode = 1180911742;

	final public static int Fl = 5692;

	final public static int FlateDecode = 2005566619;

	final public static int JBIG2Decode = 1247500931;

	final public static int JPXDecode = 1399277700;

	private final static int LZW = 1845799;

	private final static int LZWDecode = 1566984326;

	private final static int RL = 8732;

	private final static int RunLengthDecode = -1815163937;

	/** list of cached objects to delete */
	private final Map cachedObjects = new HashMap();

	private BufferedOutputStream streamCache = null;

	private BufferedInputStream bis = null;

	private boolean hasError = false;

	public byte[] decodeFilters(PdfObject[] DecodeParmsArray, byte[] data, PdfArrayIterator filters, int width, int height, String cacheName)
			throws Exception {

		this.streamCache = null;
		this.bis = null;

		final boolean debug = false;

		// get count and set global setting
		int parmsCount = DecodeParmsArray.length;
		PdfObject DecodeParms = DecodeParmsArray[0];

		byte[] globalData = null;// used by JBIG but needs to be read now so we can decode
		if (DecodeParms != null) {
			PdfObject Globals = DecodeParms.getDictionary(PdfDictionary.JBIG2Globals);
			if (Globals != null) globalData = Globals.getDecodedStream();
		}

		boolean isCached = (cacheName != null);

		int filterType, filterCount = filters.getTokenCount();

		if (debug) System.out.println("=================filterCount=" + filterCount + " DecodeParms=" + DecodeParms);

		int counter = 0;
		boolean resetDataToNull;
		// allow for no filters
		if (filterCount > 0) {

			// set each time for filter
			PdfFilter filter;
			// resetDataToNull=true;

			if (debug) System.out.println("---------filterCount=" + filterCount + " hasMore" + filters.hasMoreTokens() + " parmsCount=" + parmsCount);

			/**
			 * apply each filter in turn to data
			 */
			while (filters.hasMoreTokens()) {

				filterType = filters.getNextValueAsConstant(true);
				resetDataToNull = false;

				// pick up specific Params if set
				if (parmsCount > 1) {
					DecodeParms = DecodeParmsArray[counter];

					globalData = null;
					if (DecodeParms != null) {
						PdfObject Globals = DecodeParms.getDictionary(PdfDictionary.JBIG2Globals);
						if (Globals != null) globalData = Globals.getDecodedStream();
					}
				}

				if (debug) System.out.println("---------filter=" + getFilterName(filterType) + " DecodeParms=" + DecodeParms + ' ' + cacheName + ' '
						+ isCached);

				if (isCached && cacheName != null && (filterType == Crypt || filterType == DCTDecode || filterType == JPXDecode)) {

					counter++;
					continue;
				}

				// handle cached objects
				if (isCached && cacheName != null) setupCachedObjectForDecoding(cacheName);

				// apply decode
				if (filterType == FlateDecode || filterType == Fl) {
					filter = new Flate(DecodeParms);
				}
				else
					if (filterType == PdfFilteredReader.ASCII85Decode || filterType == PdfFilteredReader.A85) {
						filter = new ASCII85(DecodeParms);
					}
					else
						if (filterType == CCITTFaxDecode || filterType == CCF) {
							filter = new CCITT(DecodeParms, width, height);
						}
						else
							if (filterType == LZWDecode || filterType == LZW) {
								filter = new LZW(DecodeParms, width, height);
								resetDataToNull = true;

							}
							else
								if (filterType == RunLengthDecode || filterType == RL) {
									filter = new RunLength(DecodeParms);
								}
								else
									if (filterType == JBIG2Decode) {

										// To work we need to add data as an input to JSD to save the resulting data.
										// We do not write back to stream.

										if (data == null) { // hack to read into byte[] and spool back for cache

											data = new byte[this.bis.available()];
											this.bis.read(data);

											int ptr = -1;
											// resize as JBIG fussy
											for (int ii = data.length - 1; ii > 9; ii--) {
												if (data[ii] == 'e' && data[ii + 1] == 'n' && data[ii + 2] == 'd' && data[ii + 3] == 's'
														&& data[ii + 4] == 't' && data[ii + 5] == 'r' && data[ii + 6] == 'e' && data[ii + 7] == 'a'
														&& data[ii + 8] == 'm') {
													ptr = ii - 1;
													ii = -1;
												}
											}

											if (ptr != -1) {

												if (data[ptr] == 10 && data[ptr - 1] == 13) ptr--;

												byte[] tmp = data;
												data = new byte[ptr];
												System.arraycopy(tmp, 0, data, 0, ptr);
											}

											data = JBIG2.JBIGDecode(data, globalData);

											this.streamCache.write(data);
											data = null;

										}
										else {
											data = JBIG2.JBIGDecode(data, globalData);
										}

										filter = null;

									}
									else
										if (filterType == ASCIIHexDecode || filterType == AHx) {
											filter = new ASCIIHex(DecodeParms);
										}
										else
											if (filterType == Crypt) { // just pass though
												filter = null;
											}
											else {
												filter = null; // handled elsewhere
											}

				if (filter != null) {
					try {
						if (data != null) data = filter.decode(data);
						else
							if (this.bis != null) filter.decode(this.bis, this.streamCache, cacheName, this.cachedObjects);

						// flag we may have issues in stream
						if (!this.hasError && filter.hasError()) this.hasError = true;

					}
					catch (Exception ee) {

						if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + ee + " in " + getFilterName(filterType) + " decompression");

						if (resetDataToNull) data = null;
					}
				}

				if (isCached) {
					if (this.bis != null) this.bis.close();

					if (this.streamCache != null) {
						this.streamCache.flush();
						this.streamCache.close();
					}
				}

				counter++;
			}
		}

		return data;
	}

	private static String getFilterName(int filterType) {
		switch (filterType) {

			case A85:
				return "A85";

			case AHx:
				return "AHx";

			case ASCII85Decode:
				return "ASCII85Decode";

			case ASCIIHexDecode:
				return "ASCIIHexDecode";

			case CCITTFaxDecode:
				return "CCITTFaxDecode";

			case CCF:
				return "CCF";

			case Crypt:
				return "Crypt";

			case DCTDecode:
				return "DCTDecode";

			case Fl:
				return "Fl";

			case FlateDecode:
				return "FlateDecode";

			case JBIG2Decode:
				return "JBIG2Decode";

			case JPXDecode:
				return "";

			case LZW:
				return "";

			case LZWDecode:
				return "";

			case RL:
				return "";

			case RunLengthDecode:
				return "";

			default:
				return "Unknown";
		}
	}

	private void setupCachedObjectForDecoding(String cacheName) throws IOException {
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

	public boolean hasError() {
		return this.hasError;
	}
}
