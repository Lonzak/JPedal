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
 * PdfObjectReader.java
 * ---------------
 */
package org.jpedal.io;

import java.io.InputStream;

import javax.imageio.stream.ImageInputStream;

import org.jpedal.exception.PdfException;
import org.jpedal.objects.Javascript;
import org.jpedal.objects.raw.PdfObject;

public interface PdfObjectReader {

	/**
	 * read a stream
	 * 
	 * @param isMetaData
	 *            TODO
	 * @param cacheFile
	 */
	public byte[] readStream(PdfObject obj, boolean cacheValue, boolean decompress, boolean keepRaw, boolean isMetaData, boolean isCompressedStream,
			String cacheFile);

	/**
	 * read an object in the pdf as an Object
	 * 
	 */
	public void readObject(PdfObject pdfObject);

	/**
	 * read any names
	 */
	public void readNames(PdfObject obj, Javascript javascript, boolean isKid);

	/**
	 * convert name into object ref
	 */
	public String convertNameToRef(String value);

	/**
	 * open pdf file<br>
	 * Only files allowed (not http) so we can handle Random Access of pdf
	 */
	public void openPdfFile(String filename) throws PdfException;

	/**
	 * open pdf file using a byte stream
	 */
	public void openPdfFile(byte[] data) throws PdfException;

	/**
	 * close the file
	 */
	public void closePdfFile();

	public PdfObject readFDF() throws PdfException;

	public void checkResolved(PdfObject pdfObject);

	/**
	 * allow user to access SOME objects currently PdfDictionary.Encryption
	 */
	PdfObject getPDFObject(int key);

	public void dispose();

	PdfFileReader getObjectReader();

	public void openPdfFile(InputStream is) throws PdfException;

	public EncryptionUsed getEncryptionType();

	int convertObjectToPageNumber(String ref);

	void setLookup(String currentPageOffset, int tempPageCount);

	public String getReferenceforPage(int page);

	public void openPdfFile(ImageInputStream iis) throws PdfException;

	public void checkParentForResources(PdfObject pdfObject);
}
