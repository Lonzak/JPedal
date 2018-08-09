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
 * PdfReader.java
 * ---------------
 */
package org.jpedal.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.stream.ImageInputStream;

import org.jpedal.exception.PdfException;
import org.jpedal.objects.Javascript;
import org.jpedal.objects.PageLookup;
import org.jpedal.objects.raw.FDFObject;
import org.jpedal.objects.raw.PageObject;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

public class PdfReader implements PdfObjectReader, Serializable {

	private static final long serialVersionUID = 9005615362777773174L;

	private PdfFileReader objectReader = new PdfFileReader();

	/**
	 * holds pdf id (ie 4 0 R) which stores each object
	 */
	Map pagesReferences = new HashMap();

	/**
	 * page lookup table using objects as key
	 */
	private PageLookup pageLookup = new PageLookup();

	/** file length */
	private long eof = 0;

	private String tempFileName = null;

	/** names lookup table */
	private NameLookup nameLookup = null;

	RandomAccessBuffer pdf_datafile = null;

	public PdfReader() {}

	/**
	 * set password as well
	 * 
	 * @param password
	 */
	public PdfReader(String password) {
		super();

		if (password == null) password = "";

		this.objectReader.setPassword(password);
	}

	public PdfReader(Certificate certificate, PrivateKey key) {

		this.objectReader.setCertificate(certificate, key);
	}

	/**
	 * reference for Page object
	 * 
	 * @param page
	 * @return String ref (ie 1 0 R) pdfObject=new PageObject(currentPageOffset); currentPdfFile.readObject(pdfObject);
	 */
	@Override
	public String getReferenceforPage(int page) {
		return (String) this.pagesReferences.get(page);
	}

	/**
	 * close the file
	 */
	@Override
	final public void closePdfFile() {
		try {
			this.objectReader.closeFile();

			if (this.pdf_datafile != null) this.pdf_datafile.close();

			// ensure temp file deleted
			if (this.tempFileName != null) {
				File fileToDelete = new File(this.tempFileName);
				fileToDelete.delete();
				this.tempFileName = null;
			}
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " closing file");
		}
	}

	/**
	 * allow user to access SOME PDF objects currently PdfDictionary.Encryption
	 */
	@Override
	public PdfObject getPDFObject(int key) {

		if (key == PdfDictionary.Encrypt) {
			return this.objectReader.encyptionObj;
		}
		else throw new RuntimeException("Access to " + key + " not supported");
	}

	@Override
	public PdfFileReader getObjectReader() {
		return this.objectReader;
	}

	/**
	 * convert name into object ref
	 */
	@Override
	public String convertNameToRef(String value) {

		// see if decoded
		if (this.nameLookup == null) return null;
		else return (String) this.nameLookup.get(value);
	}

	// /////////////////////////////////////////////////////////////////////////

	/**
	 * read FDF
	 */
	@Override
	final public PdfObject readFDF() throws PdfException {

		PdfObject fdfObj;

		try {

			byte[] fileData = this.objectReader.readFDFData();

			fdfObj = new FDFObject("1 0 R");

			// find /FDF key
			int ii = 0;
			while (ii < this.eof) {
				if (fileData[ii] == '/' && fileData[ii + 1] == 'F' && fileData[ii + 2] == 'D' && fileData[ii + 3] == 'F') break;

				ii++;
			}

			ii = ii + 4;

			// move beyond <<
			while (ii < this.eof) {
				if (fileData[ii] == '<' && fileData[ii + 1] == '<') break;

				ii++;
			}
			ii = ii + 2;
			ObjectDecoder objectDecoder = new ObjectDecoder(this.objectReader);
			objectDecoder.readDictionaryAsObject(fdfObj, ii, fileData);

		}
		catch (Exception e) {
			try {
				this.objectReader.closeFile();
			}
			catch (IOException e1) {
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " closing file");
			}

			throw new PdfException("Exception " + e + " reading trailer");
		}

		return fdfObj;
	}

	/**
	 * read any names into names lookup
	 */
	@Override
	public void readNames(PdfObject nameObject, Javascript javascript, boolean isKid) {

		this.nameLookup = new NameLookup(this.objectReader);
		this.nameLookup.readNames(nameObject, javascript, isKid);
	}

	/**
	 * given a ref, what is the page
	 * 
	 * @param ref
	 *            - PDF object reference
	 * @return - page number with being first page
	 */
	@Override
	public int convertObjectToPageNumber(String ref) {
		return this.pageLookup.convertObjectToPageNumber(ref);
	}

	@Override
	public void setLookup(String currentPageOffset, int tempPageCount) {
		this.pageLookup.put(currentPageOffset, tempPageCount);
		this.pagesReferences.put(tempPageCount, currentPageOffset);
	}

	@Override
	public void dispose() {

		// this.objData=null;
		// this.lastRef=null;

		this.nameLookup = null;

		// this.fields=null;

		if (this.objectReader != null) this.objectReader.dispose();
		this.objectReader = null;

		if (this.pageLookup != null) this.pageLookup.dispose();
		this.pageLookup = null;
	}

	/**
	 * open pdf file<br>
	 * Only files allowed (not http) so we can handle Random Access of pdf
	 */
	@Override
	final public void openPdfFile(InputStream in) throws PdfException {

		try {

			// use byte[] directly if small otherwise use Memory Map
			this.pdf_datafile = new RandomAccessMemoryMapBuffer(in);

			this.objectReader.init(this.pdf_datafile);

			this.eof = this.pdf_datafile.length();
			// pdf_datafile = new RandomAccessFile( filename, "r" );

		}
		catch (Exception e) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " accessing file");

			throw new PdfException("Exception " + e + " accessing file");
		}
	}

	/**
	 * open pdf file<br>
	 * Only files allowed (not http) so we can handle Random Access of pdf
	 */
	@Override
	final public void openPdfFile(ImageInputStream iis) throws PdfException {

		RandomAccessBuffer pdf_datafile;

		try {

			// use byte[] directly if small otherwise use Memory Map
			pdf_datafile = new ImageInputStreamFileBuffer(iis);

			// pdf_datafile = new RandomAccessFileBuffer( filename, "r" );
			// pdf_datafile = new RandomAccessFCTest( new FileInputStream(filename));

			this.objectReader.init(pdf_datafile);

			this.eof = pdf_datafile.length();

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " accessing file");

			throw new PdfException("Exception " + e + " accessing file");
		}
	}

	@Override
	public void checkParentForResources(PdfObject pdfObject) {

		/**
		 * if no resource, check parent for one (in theory should recurse up whole tree)
		 */
		if (pdfObject.getDictionary(PdfDictionary.Resources) == null) {

			String parent = pdfObject.getStringKey(PdfDictionary.Parent);

			if (parent != null) {
				PdfObject parentObj = new PageObject(parent);
				readObject(parentObj);

				PdfObject resObj = parentObj.getDictionary(PdfDictionary.Resources);

				if (resObj != null) {
					pdfObject.setDictionary(PdfDictionary.Resources, resObj);
				}
			}
		}
	}

	/**
	 * open pdf file<br>
	 * Only files allowed (not http) so we can handle Random Access of pdf
	 */
	@Override
	final public void openPdfFile(String filename) throws PdfException {

		RandomAccessBuffer pdf_datafile;

		try {

			pdf_datafile = new RandomAccessFileBuffer(filename, "r");
			// pdf_datafile = new RandomAccessFCTest( new FileInputStream(filename));

			this.objectReader.init(pdf_datafile);

			this.eof = pdf_datafile.length();

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " accessing file");

			throw new PdfException("Exception " + e + " accessing file");
		}
	}

	/**
	 * open pdf file using a byte stream - By default files under 16384 bytes are cached to disk but this can be altered by setting
	 * PdfFileReader.alwaysCacheInMemory to a maximimum size or -1 (always keep in memory)
	 */
	@Override
	final public void openPdfFile(byte[] data) throws PdfException {

		RandomAccessBuffer pdf_datafile;

		try {
			// use byte[] directly if small otherwise use Memory Map
			if (PdfFileReader.alwaysCacheInMemory == -1 || data.length < PdfFileReader.alwaysCacheInMemory) pdf_datafile = new RandomAccessDataBuffer(
					data);
			else { // cache as file and access via RandomAccess

				// pdf_datafile = new RandomAccessMemoryMapBuffer( data ); old version very slow

				try {

					File file = File.createTempFile("page", ".bin", new File(ObjectStore.temp_dir));
					this.tempFileName = file.getAbsolutePath();

					// file.deleteOnExit();

					java.io.FileOutputStream a = new java.io.FileOutputStream(file);

					a.write(data);
					a.flush();
					a.close();

					pdf_datafile = new RandomAccessFileBuffer(this.tempFileName, "r");
				}
				catch (Exception e) {
					throw new RuntimeException("Unable to create temporary file in " + ObjectStore.temp_dir);
				}
			}

			this.objectReader.init(pdf_datafile);

			this.eof = pdf_datafile.length();
			// pdf_datafile = new RandomAccessFile( filename, "r" );

		}
		catch (Exception e) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " accessing file");

			throw new PdfException("Exception " + e + " accessing file");
		}
	}

	/** handle onto JS object */
	private Javascript javascript;

	/** pass in Javascript object from JPedal */
	public void setJavaScriptObject(Javascript javascript) {
		this.javascript = javascript;
	}

	@Override
	public void checkResolved(PdfObject pdfObject) {
		ObjectDecoder objectDecoder = new ObjectDecoder(this.objectReader);
		objectDecoder.checkResolved(pdfObject);
	}

	@Override
	public byte[] readStream(PdfObject obj, boolean cacheValue, boolean decompress, boolean keepRaw, boolean isMetaData, boolean isCompressedStream,
			String cacheFile) {
		return this.objectReader.readStream(obj, cacheValue, decompress, keepRaw, isMetaData, isCompressedStream, cacheFile);
	}

	@Override
	public void readObject(PdfObject pdfObject) {
		this.objectReader.readObject(pdfObject);
	}

	/**
	 * return type of encryption as Enum in EncryptionUsed
	 */
	@Override
	public EncryptionUsed getEncryptionType() {

		PdfFileReader objectReader = this.objectReader;
		DecryptionFactory decryption = objectReader.getDecryptionObject();

		if (decryption == null) {
			return EncryptionUsed.NO_ENCRYPTION;
		}
		else
			if (decryption.hasPassword()) {
				return EncryptionUsed.PASSWORD;
			}
			else // cert by process of elimination
			return EncryptionUsed.CERTIFICATE;
	}

}