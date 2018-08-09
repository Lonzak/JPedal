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
 * LinearParser.java
 * ---------------
 */
package org.jpedal.linear;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;
import org.jpedal.io.LinearizedHintTable;
import org.jpedal.io.ObjectDecoder;
import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.raw.LinearizedObject;
import org.jpedal.objects.raw.PageObject;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.parser.PdfStreamDecoder;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.NumberUtils;

public class LinearParser {

	/** flag if we have tested - reset for every file */
	public boolean isLinearizationTested = false;

	private PageObject linObject = null;

	private Map linObjects = new HashMap();

	private int linearPageCount = -1;

	/** present if file Linearized */
	private PdfObject linearObj = null;

	/**
	 * hold all data in Linearized Obj
	 */
	private LinearizedHintTable linHintTable = null;

	private int E = -1;

	public org.jpedal.linear.LinearThread linearizedBackgroundReaderer = null;

	public void closePdfFile() {

		this.E = -1;
		this.linearObj = null;
		this.isLinearizationTested = false;
		this.linObjects.clear();
		if (this.linearizedBackgroundReaderer != null && this.linearizedBackgroundReaderer.isAlive()) {
			this.linearizedBackgroundReaderer.interrupt();
		}

		// wait to die
		while (this.linearizedBackgroundReaderer != null && this.linearizedBackgroundReaderer.isAlive()
				&& !this.linearizedBackgroundReaderer.isInterrupted()) {
			try {
				Thread.sleep(500);
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}

		this.linHintTable = null;
	}

	public boolean isPageAvailable(int rawPage, PdfObjectReader currentPdfFile) {

		boolean isPageAvailable = true;

		try {
			if (this.linearizedBackgroundReaderer != null && this.linearizedBackgroundReaderer.isAlive() && rawPage > 1 && this.linHintTable != null) {

				Integer key = rawPage;

				// cached data
				if (this.linObjects.containsKey(key)) {
					this.linObject = (PageObject) this.linObjects.get(key);

					return true;
				}

				int objID = this.linHintTable.getPageObjectRef(rawPage);

				// return if Page data not available
				byte[] pageData = this.linHintTable.getObjData(objID);
				if (pageData != null) {

					/**
					 * turn page into obj
					 */
					this.linObject = new PageObject(objID + " 0 R");
					this.linObject.setStatus(PdfObject.UNDECODED_DIRECT);
					this.linObject.setUnresolvedData(pageData, PdfDictionary.Page);
					this.linObject.isDataExternal(true);

					ObjectDecoder objDecoder = new ObjectDecoder(currentPdfFile.getObjectReader());

					// see if object and all refs loaded otherwise exit
					if (!objDecoder.resolveFully(this.linObject)) isPageAvailable = false;
					else { // cache once available

						/**
						 * check content as well
						 */
						if (this.linObject != null) {

							byte[] b_data = currentPdfFile.getObjectReader().readPageIntoStream(this.linObject);

							if (b_data == null) {
								isPageAvailable = false;
							}
							else {
								// check Resources
								PdfObject Resources = this.linObject.getDictionary(PdfDictionary.Resources);

								if (Resources == null) {
									this.linObject = null;
									isPageAvailable = false;
								}
								else
									if (!objDecoder.resolveFully(Resources)) {
										this.linObject = null;
										isPageAvailable = false;
									}
									else {
										Resources.isDataExternal(true);
										new PdfStreamDecoder(currentPdfFile).readResources(Resources, true);
										if (!Resources.isFullyResolved()) {
											this.linObject = null;
											isPageAvailable = false;
										}
									}
							}
						}

						if (isPageAvailable && this.linObject != null) {
							this.linObjects.put(key, this.linObject);
						}
					}
				}
				else isPageAvailable = false;
			}
			else this.linObject = null;

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());

			isPageAvailable = false;
		}

		return isPageAvailable;
	}

	public byte[] readLinearData(PdfObjectReader currentPdfFile, File tempURLFile, InputStream is, PdfDecoder pdfDecoder) throws IOException {

		final FileChannel fos = new RandomAccessFile(tempURLFile, "rws").getChannel();
		fos.force(true);

		final ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);

		// Download buffer
		byte[] buffer = new byte[4096];
		int read, bytesRead = 0;
		byte[] b;

		// main loop to read all the file bytes (carries on in thread if linearized)
		while ((read = is.read(buffer)) != -1) {

			if (read > 0) {
				synchronized (fos) {

					b = new byte[read];
					System.arraycopy(buffer, 0, b, 0, read);
					ByteBuffer f = ByteBuffer.wrap(b);
					fos.write(f);
				}
			}

			bytesRead = bytesRead + read;

		}

		// Close streams
		is.close();
		synchronized (fos) {
			fos.close();
		}

		return null;
	}

	public PdfObject readHintTable(PdfObject pdfObject, PdfObjectReader currentPdfFile) throws PdfException {

		long Ooffset = -1;

		this.linearPageCount = -1;

		int O = this.linearObj.getInt(PdfDictionary.O);

		// read in the pages from the catalog and set values
		if (O != -1) {
			this.linearObj.setIntNumber(PdfDictionary.O, -1);
			currentPdfFile.getObjectReader().readReferenceTable(this.linearObj);
			pdfObject = new PageObject(O, 0);
			currentPdfFile.readObject(pdfObject);

			// get page count from linear data
			this.linearPageCount = this.linearObj.getInt(PdfDictionary.N);

			Ooffset = currentPdfFile.getObjectReader().getOffset(O);

		}
		else { // use O as flag and reset
			pdfObject = currentPdfFile.getObjectReader().readReferenceTable(null);
		}

		/**
		 * read and decode the hints table
		 */
		int[] H = this.linearObj.getIntArray(PdfDictionary.H);

		byte[] hintStream = currentPdfFile.getObjectReader().getBytes(H[0], H[1]);

		// find <<
		int length = hintStream.length;
		int startHint = 0;
		int i = 0;
		boolean contentIsDodgy = false;

		// number
		int keyStart2 = i;
		while (hintStream[i] != 10 && hintStream[i] != 13 && hintStream[i] != 32 && hintStream[i] != 47 && hintStream[i] != 60 && hintStream[i] != 62) {

			if (hintStream[i] < 48 || hintStream[i] > 57) // if its not a number value it looks suspicious
			contentIsDodgy = true;

			i++;
		}

		// trap for content not correct
		if (!contentIsDodgy) {

			int number = NumberUtils.parseInt(keyStart2, i, hintStream);

			// generation
			while (hintStream[i] == 10 || hintStream[i] == 13 || hintStream[i] == 32 || hintStream[i] == 47 || hintStream[i] == 60)
				i++;

			keyStart2 = i;
			// move cursor to end of reference
			while (i < 10 && hintStream[i] != 10 && hintStream[i] != 13 && hintStream[i] != 32 && hintStream[i] != 47 && hintStream[i] != 60
					&& hintStream[i] != 62)
				i++;
			int generation = NumberUtils.parseInt(keyStart2, i, hintStream);

			while (i < length - 1) {

				if (hintStream[i] == '<' && hintStream[i + 1] == '<') {
					startHint = i;
					i = length;
				}

				i++;
			}

			byte[] data = new byte[length - startHint];

			// convert the raw data into a PDF object
			System.arraycopy(hintStream, startHint, data, 0, data.length);
			LinearizedObject hintObj = new LinearizedObject(number, generation);
			hintObj.setStatus(PdfObject.UNDECODED_DIRECT);
			hintObj.setUnresolvedData(data, PdfDictionary.Linearized);
			currentPdfFile.checkResolved(hintObj);

			// get page content pointers
			this.linHintTable.readTable(hintObj, this.linearObj, O, Ooffset);

		}

		return pdfObject;
	}

	public int getPageCount() {
		return this.linearPageCount;
	}

	public boolean hasLinearData() {
		return this.linearObj != null && this.E != -1;
	}

	public PdfObject getLinearPageObject() {
		return this.linObject;
	}

	public PdfObject getLinearObject(boolean isOpen, PdfObjectReader currentPdfFile) {

		// lazy initialisation if not URLstream
		if (!this.isLinearizationTested) {

		}
		return this.linearObj;
	}
}
