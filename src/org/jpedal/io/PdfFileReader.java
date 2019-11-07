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
 * PdfFileReader.java
 * ---------------
 */
package org.jpedal.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.jpedal.constants.PDFflags;
import org.jpedal.exception.PdfException;
import org.jpedal.exception.PdfSecurityException;
import org.jpedal.objects.raw.CompressedObject;
import org.jpedal.objects.raw.EncryptionObject;
import org.jpedal.objects.raw.PageObject;
import org.jpedal.objects.raw.PdfArrayIterator;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.objects.raw.StreamObject;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.NumberUtils;
import org.jpedal.utils.Sorts;
import org.jpedal.utils.repositories.Vector_Int;
import org.jpedal.utils.repositories.Vector_boolean;

/**
 * provides access to the file using Random access class to read bytes and strings from a pdf file. Pdf file is a mix of character and binary data
 * streams
 */
public class PdfFileReader {

	private final static int UNSET = -1;
	private final static int COMPRESSED = 1;
	private final static int LEGACY = 2;

	private int newCacheSize = -1;

	private LinearizedHintTable linHintTable;

	private boolean refTableInvalid = false;

	private final static byte[] endPattern = { 101, 110, 100, 111, 98, 106 }; // pattern endobj

	/** used to cache last compressed object */
	private byte[] lastCompressedStream = null;

	/** used to cache last compressed object */
	private Map lastOffsetStart, lastOffsetEnd;

	private PdfObject compressedObj;

	/** used to cache last compressed object */
	private int lastFirst = -1, lastCompressedID = -1;

	/** copy so we can access in other routine */
	private PdfObject linearObj;

	private Certificate certificate;

	private PrivateKey key;

	public PdfObject getInfoObject() {
		return this.infoObject;
	}

	/**
	 * set size over which objects kept on disk
	 */
	public void setCacheSize(int miniumumCacheSize) {

		this.newCacheSize = miniumumCacheSize;
	}

	/** info object */
	private PdfObject infoObject = null;

	/** holds file ID */
	private byte[] ID = null;

	PdfObject encyptionObj = null;

	/** pattern to look for in objects */
	final static private String pattern = "obj";

	// private boolean isFDF=false;

	/** location of end ref */
	private Vector_Int xref = new Vector_Int(100);

	private DecryptionFactory decryption = null;

	/** encryption password */
	private byte[] encryptionPassword = null;

	private final static byte[] oldPattern = { 'x', 'r', 'e', 'f' };

	final static private byte[] EOFpattern = { 37, 37, 69, 79, 70 }; // pattern %%EOF

	final static private byte[] trailerpattern = { 't', 'r', 'a', 'i', 'l', 'e', 'r' }; // pattern %%EOF

	/** file access */
	private RandomAccessBuffer pdf_datafile = null;

	private final static byte[] endObj = { 32, 111, 98, 106 }; // pattern endobj

	private final static byte[] lengthString = { 47, 76, 101, 110, 103, 116, 104 }; // pattern /Length

	private final static byte[] startStream = { 115, 116, 114, 101, 97, 109 };

	/**
	 * location from the reference table of each object in the file
	 */
	private Vector_Int offset = new Vector_Int(2000);

	/** flag to show if compressed */
	private Vector_boolean isCompressed = new Vector_boolean(2000);

	/** generation of each object */
	private Vector_Int generation = new Vector_Int(2000);

	/** should never be final */
	public static int alwaysCacheInMemory = 16384;

	private long eof;

	/** length of each object */
	private int[] ObjLengthTable;

	public void init(RandomAccessBuffer pdf_datafile) {

		this.pdf_datafile = pdf_datafile;

		try {
			this.eof = pdf_datafile.length();
		}
		catch (IOException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	/**
	 * read an object in the pdf into a Object which can be an indirect or an object
	 * 
	 */
	final public void readObject(PdfObject pdfObject) {

		if (pdfObject.isDataExternal() && this.linHintTable != null) {
			readExternalObject(pdfObject);
		}
		else {

			String objectRef = pdfObject.getObjectRefAsString();

			final boolean debug = false;

			if (debug) System.err.println("reading objectRef=" + objectRef + "< isCompressed=" + isCompressed(objectRef));

			boolean isCompressed = isCompressed(objectRef);
			pdfObject.setCompressedStream(isCompressed);

			// any stream
			byte[] raw;// stream=null;

			/** read raw object data */
			if (isCompressed) {
				raw = readCompressedObject(pdfObject, objectRef);
			}
			else {
				movePointer(objectRef);

				if (objectRef.charAt(0) == '<') {
					raw = readObjectData(-1, pdfObject);
				}
				else {
					int pointer = objectRef.indexOf(' ');
					int id = Integer.parseInt(objectRef.substring(0, pointer));

					if (this.ObjLengthTable == null || this.refTableInvalid) { // isEncryptionObject

						// allow for bum object
						if (getPointer() == 0) raw = new byte[0];
						else raw = readObjectData(-1, pdfObject);

					}
					else
						if (id > this.ObjLengthTable.length || this.ObjLengthTable[id] == 0) {
							if (LogWriter.isOutput()) LogWriter.writeLog(objectRef + " cannot have offset 0");

							raw = new byte[0];
						}
						else raw = readObjectData(this.ObjLengthTable[id], pdfObject);
				}
			}

			if (raw.length > 1) {
				ObjectDecoder objDecoder = new ObjectDecoder(this);
				objDecoder.readDictionaryAsObject(pdfObject, 0, raw);
			}
		}
	}

	private void readExternalObject(PdfObject pdfObject) {

		int ref = pdfObject.getObjectRefID();
		int generation = pdfObject.getObjectRefGeneration();

		byte[] pageData = readObjectAsByteArray(pdfObject, isCompressed(ref, generation), ref, generation);

		pdfObject.setStatus(PdfObject.UNDECODED_DIRECT);
		pdfObject.setUnresolvedData(pageData, PdfDictionary.Page);

		ObjectDecoder objDecoder = new ObjectDecoder(this);
		objDecoder.resolveFully(pdfObject);
	}

	private byte[] readCompressedObject(PdfObject pdfObject, String objectRef) {

		byte[] raw;
		int objectID = Integer.parseInt(objectRef.substring(0, objectRef.indexOf(' ')));
		int compressedID = getCompressedStreamObject(objectRef);
		String compressedRef = compressedID + " 0 R", startID = null;
		int First = this.lastFirst;
		boolean isCached = true; // assume cached

		// see if we already have values
		byte[] compressedStream = this.lastCompressedStream;
		Map offsetStart = this.lastOffsetStart;
		Map offsetEnd = this.lastOffsetEnd;

		PdfObject Extends = null;

		if (this.lastOffsetStart != null && compressedID == this.lastCompressedID) startID = (String) this.lastOffsetStart.get(String
				.valueOf(objectID));

		// read 1 or more streams
		while (startID == null) {

			if (Extends != null) {
				this.compressedObj = Extends;
			}
			else
				if (compressedID != this.lastCompressedID) {

					isCached = false;

					movePointer(compressedRef);

					raw = readObjectData(this.ObjLengthTable[compressedID], null);

					this.compressedObj = new CompressedObject(compressedRef);
					ObjectDecoder objDecoder = new ObjectDecoder(this);
					objDecoder.readDictionaryAsObject(this.compressedObj, 0, raw);

				}

			/** get offsets table see if in this stream */
			offsetStart = new HashMap();
			offsetEnd = new HashMap();
			First = this.compressedObj.getInt(PdfDictionary.First);

			compressedStream = this.compressedObj.getDecodedStream();

			extractCompressedObjectOffset(offsetStart, offsetEnd, First, compressedStream, compressedID);

			startID = (String) offsetStart.get(String.valueOf(objectID));

			Extends = this.compressedObj.getDictionary(PdfDictionary.Extends);
			if (Extends == null) break;

		}

		if (!isCached) {
			this.lastCompressedStream = compressedStream;
			this.lastCompressedID = compressedID;
			this.lastOffsetStart = offsetStart;
			this.lastOffsetEnd = offsetEnd;
			this.lastFirst = First;
		}

		/** put bytes in stream */
		int start = First + Integer.parseInt(startID), end = compressedStream.length;

		String endID = (String) offsetEnd.get(String.valueOf(objectID));
		if (endID != null) end = First + Integer.parseInt(endID);

		int streamLength = end - start;
		raw = new byte[streamLength];
		System.arraycopy(compressedStream, start, raw, 0, streamLength);

		pdfObject.setInCompressedStream(true);

		return raw;
	}

	/**
	 * read 1.5 compression stream ref table
	 * 
	 * @throws PdfException
	 */
	private PdfObject readCompressedStream(int pointer) throws PdfException {

		PdfObject encryptObj = null, rootObj = null;

		while (pointer != -1) {

			/**
			 * get values to read stream ref
			 */
			movePointer(pointer);

			byte[] raw = readObjectData(-1, null);

			/** read the object name from the start */
			StringBuilder objectName = new StringBuilder();
			char current1, last = ' ';
			int matched = 0, i1 = 0;
			while (i1 < raw.length) {
				current1 = (char) raw[i1];

				// treat returns same as spaces
				if (current1 == 10 || current1 == 13) current1 = ' ';

				if (current1 == ' ' && last == ' ') {// lose duplicate or spaces
					matched = 0;
				}
				else
					if (current1 == pattern.charAt(matched)) { // looking for obj at end
						matched++;
					}
					else {
						matched = 0;
						objectName.append(current1);
					}
				if (matched == 3) break;
				last = current1;
				i1++;
			}

			// add end and put into Map
			objectName.append('R');

			PdfObject pdfObject = new CompressedObject(objectName.toString());
			pdfObject.setCompressedStream(true);
			ObjectDecoder objectDecoder = new ObjectDecoder(this);
			objectDecoder.readDictionaryAsObject(pdfObject, 0, raw);

			// read the field sizes
			int[] fieldSizes = pdfObject.getIntArray(PdfDictionary.W);

			// read the xrefs stream
			byte[] xrefs = pdfObject.getDecodedStream();

			// if encr
			if (xrefs == null) {
				xrefs = readStream(pdfObject, true, true, false, false, true, null);
			}

			int[] Index = pdfObject.getIntArray(PdfDictionary.Index);
			if (Index == null) { // single set of values

				// System.out.println("-------------1.Offsets-------------"+current+" "+numbEntries);
				readCompressedOffsets(0, 0, pdfObject.getInt(PdfDictionary.Size), fieldSizes, xrefs);

			}
			else { // pairs of values in Index[] array
				int count = Index.length, pntr = 0;

				for (int aa = 0; aa < count; aa = aa + 2) {

					// System.out.println("-------------2.Offsets-------------"+Index[aa]+" "+Index[aa+1]);

					pntr = readCompressedOffsets(pntr, Index[aa], Index[aa + 1], fieldSizes, xrefs);
				}
			}

			/**
			 * now process trailer values - only first set of table values for root, encryption and info
			 */
			if (rootObj == null) {

				rootObj = pdfObject.getDictionary(PdfDictionary.Root);

				/**
				 * handle encryption
				 */
				encryptObj = pdfObject.getDictionary(PdfDictionary.Encrypt);

				if (encryptObj != null) {

					byte[][] IDs = pdfObject.getStringArray(PdfDictionary.ID);
					if (IDs != null) this.ID = IDs[0];
				}

				this.infoObject = pdfObject.getDictionary(PdfDictionary.Info);

			}

			// make sure first values used if several tables and code for prev so long as not linearized
			// may need adjusting as more examples turn up
			if (this.linearObj != null) pointer = -1;
			else pointer = pdfObject.getInt(PdfDictionary.Prev);
		}

		if (encryptObj != null) setupDecryption(encryptObj);

		this.ObjLengthTable = calculateObjectLength((int) this.eof);

		return rootObj;
	}

	/** read a stream */
	final public byte[] readStream(PdfObject pdfObject, boolean cacheValue, boolean decompress, boolean keepRaw, boolean isMetaData,
			boolean isCompressedStream, String cacheName) {

		final boolean debugStream = false;

		boolean isCachedOnDisk = pdfObject.isCached();

		byte[] data = null;

		if (!isCachedOnDisk) data = pdfObject.getDecodedStream();

		// BufferedOutputStream streamCache=null;
		byte[] stream;

		// decompress first time
		if (data == null) {

			stream = pdfObject.stream;

			if (isCachedOnDisk) {

				// decrypt the stream
				try {
					if (this.decryption != null && !isCompressedStream
							&& (this.decryption.getBooleanValue(PDFflags.IS_METADATA_ENCRYPTED) || !isMetaData)) {

						this.decryption.decrypt(null, pdfObject.getObjectRefAsString(), false, cacheName, false, false);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					stream = null;
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e);
				}
			}

			if (stream != null) {
				/** decode and save stream */

				// decrypt the stream
				try {
					if (this.decryption != null && !isCompressedStream
							&& (this.decryption.getBooleanValue(PDFflags.IS_METADATA_ENCRYPTED) || !isMetaData)) {// &&
																													// pdfObject.getObjectType()!=PdfDictionary.ColorSpace){

						// System.out.println(objectRef+">>>"+pdfObject.getObjectRefAsString());
						if (pdfObject.getObjectType() == PdfDictionary.ColorSpace && pdfObject.getObjectRefAsString().startsWith("[")) {

						}
						else stream = this.decryption.decrypt(stream, pdfObject.getObjectRefAsString(), false, null, false, false);

					}
				}
				catch (Exception e) {
					e.printStackTrace();
					stream = null;

					if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " with " + pdfObject.getObjectRefAsString());
				}
			}

			if (keepRaw) pdfObject.stream = null;

			int length = 1;

			if (stream != null || isCachedOnDisk) {

				// values for CCITTDecode
				int height = 1, width = 1;

				int newH = pdfObject.getInt(PdfDictionary.Height);
				if (newH != -1) height = newH;

				int newW = pdfObject.getInt(PdfDictionary.Width);
				if (newW != -1) width = newW;

				int newLength = pdfObject.getInt(PdfDictionary.Length);
				if (newLength != -1) length = newLength;

				/** allow for no width or length */
				if (height * width == 1) width = length;

				PdfArrayIterator filters = pdfObject.getMixedArray(PdfDictionary.Filter);

				// check not handled elsewhere
				int firstValue = PdfDictionary.Unknown;
				if (filters != null && filters.hasMoreTokens()) firstValue = filters.getNextValueAsConstant(false);

				if (debugStream) System.out.println("First filter=" + firstValue);

				if (filters != null && firstValue != PdfDictionary.Unknown && firstValue != PdfFilteredReader.JPXDecode
						&& firstValue != PdfFilteredReader.DCTDecode) {

					if (debugStream) System.out.println("Decoding stream " + stream + ' ' + pdfObject.isCached() + ' '
							+ pdfObject.getObjectRefAsString());

					try {
						PdfFilteredReader filter = new PdfFilteredReader();
						stream = filter.decodeFilters(ObjectUtils.setupDecodeParms(pdfObject, this), stream, filters, width, height, cacheName);

						// flag if any error
						pdfObject.setStreamMayBeCorrupt(filter.hasError());

					}
					catch (Exception e) {

						if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Problem " + e + " decompressing stream ");

						stream = null;
						isCachedOnDisk = false; // make sure we return null, and not some bum values
					}

					// stop spurious match down below in caching code
					length = 1;
				}
				else
					if (stream != null && length != -1 && length < stream.length) {

						/** make sure length correct */
						// if(stream.length!=length){
						if (stream.length != length && length > 0) {// <-- last item breaks jbig??
							byte[] newStream = new byte[length];
							System.arraycopy(stream, 0, newStream, 0, length);

							stream = newStream;
						}
						else
							if (stream.length == 1 && length == 0) stream = new byte[0];
					}
			}

			if (stream != null && cacheValue) pdfObject.setDecodedStream(stream);

			if (decompress && isCachedOnDisk) {
				int streamLength = (int) new File(cacheName).length();

				byte[] bytes = new byte[streamLength];

				try {
					new BufferedInputStream(new FileInputStream(cacheName)).read(bytes);
				}
				catch (Exception e) {
					// tell user and log
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
				}

				/** resize if length supplied */
				if ((length != 1) && (length < streamLength)) {

					/** make sure length correct */
					byte[] newStream = new byte[length];
					System.arraycopy(bytes, 0, newStream, 0, length);

					bytes = newStream;

				}

				return bytes;
			}

		}
		else stream = data;

		if (stream == null) return null;

		// make a a DEEP copy so we cant alter
		int len = stream.length;
		byte[] copy = new byte[len];
		System.arraycopy(stream, 0, copy, 0, len);

		return copy;
	}

	/**
	 * read reference table from file so we can locate objects in pdf file and read the trailers
	 */
	private PdfObject readLegacyReferenceTable(int pointer, int eof) throws PdfException {

		int endTable, current = 0; // current object number
		byte[] Bytes;
		int bufSize = 1024;

		PdfObject encryptObj = null, rootObj = null;

		/** read and decode 1 or more trailers */
		while (true) {

			try {

				// allow for pointer outside file
				Bytes = readTrailer(bufSize, pointer, eof);

			}
			catch (Exception e) {

				try {
					closeFile();
				}
				catch (IOException e1) {
					if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " closing file");
				}
				throw new PdfException("Exception " + e + " reading trailer");
			}

			if (Bytes == null) // safety catch
			break;

			/** get trailer */
			int i = 0;

			int maxLen = Bytes.length;

			// for(int a=0;a<100;a++)
			// System.out.println((char)Bytes[i+a]);
			while (i < maxLen) {// look for trailer keyword
				if (Bytes[i] == 116 && Bytes[i + 1] == 114 && Bytes[i + 2] == 97 && Bytes[i + 3] == 105 && Bytes[i + 4] == 108 && Bytes[i + 5] == 101
						&& Bytes[i + 6] == 114) break;

				i++;
			}

			// save endtable position for later
			endTable = i;

			if (i == Bytes.length) break;

			// move to beyond <<
			while (Bytes[i] != 60 && Bytes[i - 1] != 60)
				i++;

			i++;
			PdfObject pdfObject = new CompressedObject("1 0 R");
			ObjectDecoder objectDecoder = new ObjectDecoder(this);
			objectDecoder.readDictionary(pdfObject, i, Bytes, -1, true);

			// move to beyond >>
			int level = 0;
			while (true) {

				if (Bytes[i] == 60 && Bytes[i - 1] == 60) {
					level++;
					i++;
				}
				else
					if (Bytes[i] == '[') {
						i++;
						while (Bytes[i] != ']') {
							i++;
							if (i == Bytes.length) break;
						}
					}
					else
						if (Bytes[i] == 62 && Bytes[i - 1] == 62) {
							level--;
							i++;
						}

				if (level == 0) break;

				i++;
			}

			// handle optional XRefStm
			int XRefStm = pdfObject.getInt(PdfDictionary.XRefStm);

			if (XRefStm != -1) {
				pointer = XRefStm;
			}
			else { // usual way

				boolean hasRef = true;

				// look for xref as end of startref
				while (Bytes[i] != 116 && Bytes[i + 1] != 120 && Bytes[i + 2] != 114 && Bytes[i + 3] != 101 && Bytes[i + 4] != 102) {

					if (Bytes[i] == 'o' && Bytes[i + 1] == 'b' && Bytes[i + 2] == 'j') {
						hasRef = false;
						break;
					}
					i++;
				}

				if (hasRef) {

					i = i + 8;
					// move to start of value ignoring spaces or returns
					while ((i < maxLen) && (Bytes[i] == 10 || Bytes[i] == 32 || Bytes[i] == 13))
						i++;

					int s = i;

					// allow for characters between xref and startref
					while (i < maxLen && Bytes[i] != 10 && Bytes[i] != 32 && Bytes[i] != 13)
						i++;

					/** convert xref to string to get pointer */
					if (s != i) pointer = NumberUtils.parseInt(s, i, Bytes);

				}
			}

			i = 0;

			// allow for bum data at start
			while (Bytes[i] == 13 || Bytes[i] == 10 || Bytes[i] == 9)
				i++;

			if (pointer == -1) {
				if (LogWriter.isOutput()) LogWriter.writeLog("No startRef");

				/** now read the objects for the trailers */
			}
			else
				if (Bytes[i] == 120 && Bytes[i + 1] == 114 && Bytes[i + 2] == 101 && Bytes[i + 3] == 102) { // make sure starts xref

					i = 5;

					// move to start of value ignoring spaces or returns
					while (Bytes[i] == 10 || Bytes[i] == 32 || Bytes[i] == 13)
						i++;

					current = readXRefs(this.xref, current, Bytes, endTable, i);

					/** now process trailer values - only first set of table values for root, encryption and info */
					if (rootObj == null) {

						rootObj = pdfObject.getDictionary(PdfDictionary.Root);

						encryptObj = pdfObject.getDictionary(PdfDictionary.Encrypt);
						if (encryptObj != null) {

							byte[][] IDs = pdfObject.getStringArray(PdfDictionary.ID);
							if (IDs != null) this.ID = IDs[0];
						}

						this.infoObject = pdfObject.getDictionary(PdfDictionary.Info);

					}

					// make sure first values used if several tables and code for prev
					pointer = pdfObject.getInt(PdfDictionary.Prev);

					// see if other trailers
					if (pointer != -1 && pointer < this.eof) {
						// reset values for loop
						bufSize = 1024;

						// track ref table so we can work out object length
						this.xref.addElement(pointer);

					}
					else // reset if fails second test above
					pointer = -1;

				}
				else {
					pointer = -1;

					// needs to be read to pick up potential /Pages value
					// noinspection ObjectAllocationInLoop
					rootObj = new PageObject(findOffsets());
					readObject(rootObj);

					this.refTableInvalid = true;
				}
			if (pointer == -1) break;
		}

		/**
		 * check offsets
		 */
		if (encryptObj != null) setupDecryption(encryptObj);

		if (!this.refTableInvalid) this.ObjLengthTable = calculateObjectLength(eof);

		return rootObj;
	}

	private void setupDecryption(PdfObject encryptObj) throws PdfSecurityException {

		try {
			/**
			 * instance as appropriate
			 */
			if (this.certificate != null) this.decryption = new DecryptionFactory(this.ID, this.certificate, this.key);
			else this.decryption = new DecryptionFactory(this.ID, this.encryptionPassword);

			// get values
			if (this.encyptionObj == null) {
				this.encyptionObj = new EncryptionObject(new String(encryptObj.getUnresolvedData()));
				readObject(this.encyptionObj);
			}

			this.decryption.readEncryptionObject(this.encyptionObj);

		}
		catch (Error err) {
			throw new RuntimeException("This PDF file is encrypted and JPedal needs an additional library to \n"
					+ "decode on the classpath (we recommend bouncycastle library).\n"
					+ "There is additional explanation at http://www.idrsolutions.com/additional-jars" + '\n');

		}
	}

	/** give user access to internal flags such as user permissions */
	public int getPDFflag(Integer flag) {

		if (this.decryption == null) return -1;
		else return this.decryption.getPDFflag(flag);
	}

	/**
	 * read first start ref from last 1024 bytes
	 */
	private int readFirstStartRef() throws PdfException {

		// reset flag
		this.refTableInvalid = false;
		int pointer = -1;
		int i = 1019;
		StringBuilder startRef = new StringBuilder(10);

		/** move to end of file and read last 1024 bytes */
		int block = 1024;
		byte[] lastBytes = new byte[block];
		long end;

		/**
		 * set endpoint, losing null chars and anything before EOF
		 */
		final int[] EndOfFileMarker = { 37, 37, 69, 79 };
		int valReached = 3;
		boolean EOFFound = false;
		try {
			end = this.eof;

			/**
			 * lose nulls and other trash from end of file
			 */
			int bufSize = 255;
			while (true) {
				byte[] buffer = getBytes(end - bufSize, bufSize);

				int offset = 0;

				for (int ii = bufSize - 1; ii > -1; ii--) {

					// see if we can decrement EOF tracker or restart check
					if (!EOFFound) valReached = 3;

					if (buffer[ii] == EndOfFileMarker[valReached]) {
						valReached--;
						EOFFound = true;
					}
					else EOFFound = false;

					// move to next byte
					offset--;

					if (valReached < 0) ii = -1;

				}

				// exit if found values on loop
				if (valReached < 0) {
					end = end - offset;
					break;
				}
				else {
					end = end - bufSize;
				}

				// allow for no eof
				if (end < 0) {
					end = this.eof;
					break;
				}
			}

			// end=end+bufSize;

			// allow for very small file
			int count = (int) (end - block);

			if (count < 0) {
				count = 0;
				int size = (int) this.eof;
				lastBytes = new byte[size];
				i = size + 3; // force reset below
			}

			lastBytes = getBytes(count, lastBytes.length);

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " reading last 1024 bytes");

			throw new PdfException(e + " reading last 1024 bytes");
		}

		// for(int ii=0;ii<lastBytes.length;ii++){
		// System.out.print((char)lastBytes[ii]);
		// }
		// System.out.println();

		// look for tref as end of startxref
		int fileSize = lastBytes.length;

		if (i > fileSize) i = fileSize - 5;

		while (i > -1) {

			// first check is because startref works as well a startxref !!
			if (((lastBytes[i] == 116 && lastBytes[i + 1] == 120) || (lastBytes[i] == 114 && lastBytes[i + 1] == 116)) && (lastBytes[i + 2] == 114)
					&& (lastBytes[i + 3] == 101) && (lastBytes[i + 4] == 102)) break;

			i--;

		}

		/** trap buggy files */
		if (i == -1) {
			try {
				closeFile();
			}
			catch (IOException e1) {
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e1 + " closing file");
			}
			throw new PdfException("No Startxref found in last 1024 bytes ");
		}

		i = i + 5; // allow for word length

		// move to start of value ignoring spaces or returns
		while (i < 1024 && (lastBytes[i] == 10 || lastBytes[i] == 32 || lastBytes[i] == 13))
			i++;

		// move to start of value ignoring spaces or returns
		while ((i < 1024) && (lastBytes[i] != 10) && (lastBytes[i] != 32) && (lastBytes[i] != 13)) {
			startRef.append((char) lastBytes[i]);
			i++;
		}

		/** convert xref to string to get pointer */
		if (startRef.length() > 0) pointer = Integer.parseInt(startRef.toString());

		if (pointer == -1) {
			if (LogWriter.isOutput()) LogWriter.writeLog("No Startref found in last 1024 bytes ");
			try {
				closeFile();
			}
			catch (IOException e1) {
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e1 + " closing file");
			}
			throw new PdfException("No Startref found in last 1024 bytes ");
		}

		return pointer;
	}

	/**
	 * read reference table start to see if new 1.5 type or traditional xref
	 * 
	 * @throws PdfException
	 */
	final public PdfObject readReferenceTable(PdfObject linearObj) throws PdfException {

		this.linearObj = linearObj;

		int pointer = -1, eof = (int) this.eof;

		boolean islinearizedCompressed = false;

		if (linearObj == null) {
			pointer = readFirstStartRef();
		}
		else { // find at start of Linearized
			byte[] data = getBuffer();

			int count = data.length, ptr = 5;
			for (int i = 0; i < count; i++) {

				// track start of this object (needed for compressed)
				if (data[i] == 'e' && data[i + 1] == 'n' && data[i + 2] == 'd' && data[i + 3] == 'o' && data[i + 4] == 'b' && data[i + 5] == 'j') {
					ptr = i + 6;

				}

				if (data[i] == 'x' && data[i + 1] == 'r' && data[i + 2] == 'e' && data[i + 3] == 'f') {
					pointer = i;
					i = count;
				}
				else
					if (data[i] == 'X' && data[i + 1] == 'R' && data[i + 2] == 'e' && data[i + 3] == 'f') {

						islinearizedCompressed = true;

						pointer = ptr;
						while (data[pointer] == 10 || data[pointer] == 13 || data[pointer] == 32) {
							pointer++;
						}

						i = count;
					}
			}
		}

		this.xref.addElement(pointer);

		if (pointer >= eof) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Pointer not if file - trying to manually find startref");

			this.refTableInvalid = true;

			PdfObject rootObj = new PageObject(findOffsets());
			readObject(rootObj);
			return rootObj;

		}
		else
			if (islinearizedCompressed || isCompressedStream(pointer, eof)) {
				return readCompressedStream(pointer);
			}
			else {
				return readLegacyReferenceTable(pointer, eof);
			}
	}

	public void spoolStreamDataToDisk(File tmpFile, long start) throws Exception {

		movePointer(start);

		boolean hasValues = false;

		// Create output file
		BufferedOutputStream array = new BufferedOutputStream(new FileOutputStream(tmpFile));

		int bufSize = -1;
		// PdfObject pdfObject=null;

		int startStreamCount = 0;// newCacheSize=-1,;
		boolean startStreamFound = false;

		// if(pdfObject!=null) //only use if values found
		// newCacheSize=this.newCacheSize;

		final int XXX = 2 * 1024 * 1024;

		int rawSize = bufSize, realPos = 0;

		final boolean debug = false;

		boolean lengthSet = false; // start false and set to true if we find /Length in metadata
		boolean streamFound = false;

		if (debug) System.out.println("=============================");

		if (bufSize < 1) bufSize = 128;

		// if(newCacheSize!=-1 && bufSize>newCacheSize)
		// bufSize=newCacheSize;

		// array for data
		int ptr = 0, maxPtr = bufSize;

		byte[] readData = new byte[maxPtr];

		int charReached = 0, charReached2 = 0, charReached3 = 0;

		byte[] buffer = null;
		boolean inStream = false, ignoreByte;

		/** adjust buffer if less than bytes left in file */
		long pointer;// lastEndStream=-1,objStart=-1;

		/** read the object or block */
		try {

			byte currentByte;// lastByte;

			int i = bufSize - 1, offset = -bufSize;
			// int blocksRead=0;//lastEnd=-1,lastComment=-1;

			while (true) {

				i++;

				if (i == bufSize) {

					// cache data and update counter
					// if(blocksRead==1){
					// dataRead=buffer;
					// }else if(blocksRead>1){
					//
					// int bytesRead=dataRead.length;
					// int newBytes=buffer.length;
					// byte[] tmp=new byte[bytesRead+newBytes];
					//
					// //existing data into new array
					// System.arraycopy(dataRead, 0, tmp, 0, bytesRead);
					//
					// //data from current block
					// System.arraycopy(buffer, 0, tmp, bytesRead, newBytes);
					//
					// dataRead=tmp;
					//
					// //PUT BACK to switch on caching
					// if(1==2 && streamFound && dataRead.length>newCacheSize) //stop if over max size
					// break;
					// }
					// blocksRead++;

					/**
					 * read the next block
					 */
					pointer = getPointer();

					if (start == -1) start = pointer;

					/** adjust buffer if less than bytes left in file */
					if (pointer + bufSize > this.eof) bufSize = (int) (this.eof - pointer);

					bufSize += 6;
					buffer = new byte[bufSize];

					/** get bytes into buffer */
					read(buffer);

					offset = offset + i;
					i = 0;

				}

				/** write out and look for endobj at end */
				// lastByte=currentByte;
				currentByte = buffer[i];
				ignoreByte = false;

				// track comments
				// if(currentByte=='%')
				// lastComment=realPos;

				/** check for endobj at end - reset if not */
				if (currentByte == ObjectDecoder.endPattern[charReached] && !inStream) charReached++;
				else charReached = 0;

				// also scan for <SPACE>obj after endstream incase no endobj
				if (streamFound && currentByte == endObj[charReached2] && !inStream) charReached2++;
				else charReached2 = 0;

				// look for start of stream and set inStream true

				if (startStreamFound) {
					if (hasValues || currentByte != 13 && currentByte != 10) { // avoid trailing CR/LF
						array.write(currentByte);
						hasValues = true;
					}
				}

				if (startStreamCount < 6 && currentByte == startStream[startStreamCount]) {
					startStreamCount++;
				}
				else startStreamCount = 0;

				if (!startStreamFound && (startStreamCount == 6)) { // stream start found so log
					// startStreamCount=offset+startStreamCount;
					// pdfObject.setCache(start,this);
					startStreamFound = true;
				}

				/** if length not set we go on endstream in data */
				if (!lengthSet) {

					// also scan for /Length if it had a valid size
					if (rawSize != -1) {
						if (!streamFound && currentByte == lengthString[charReached3] && !inStream) {
							charReached3++;
							if (charReached3 == 6) lengthSet = true;
						}
						else charReached3 = 0;
					}
				}

				if (charReached == 6 || charReached2 == 4) {

					if (!lengthSet) break;

					charReached = 0;
					charReached2 = 0;
					// lastEnd=realPos;

				}

				if (lengthSet && realPos >= rawSize) break;

				if (!ignoreByte && !inStream) {// || !inStream)

					readData[ptr] = currentByte;

					ptr++;
					if (ptr == maxPtr) {
						if (maxPtr < XXX) maxPtr = maxPtr * 2;
						else maxPtr = maxPtr + 100000;

						byte[] tmpArray = new byte[maxPtr];
						System.arraycopy(readData, 0, tmpArray, 0, readData.length);

						readData = tmpArray;
					}
				}

				realPos++;
			}

		}
		catch (Exception e) {
			e.printStackTrace();

			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " reading object");
		}

		if (array != null) {
			array.flush();
			array.close();
		}
	}

	/**
	 * test first bytes to see if new 1.5 style table with obj or contains ref
	 * 
	 * @throws PdfException
	 */
	private boolean isCompressedStream(int pointer, int eof) throws PdfException {

		final boolean debug = false;

		int bufSize = 50, charReached_legacy = 0, charReached_comp1 = 0, charReached_comp2 = 0;

		final int[] objStm = { 'O', 'b', 'j', 'S', 't', 'm' };
		final int[] XRef = { 'X', 'R', 'e', 'f' };

		int type = UNSET;

		// flag to show if at start of data for check
		boolean firstRead = true;

		while (true) {

			/** adjust buffer if less than 1024 bytes left in file */
			if (pointer + bufSize > eof) bufSize = eof - pointer;

			if (bufSize < 0) bufSize = 50;

			byte[] buffer = getBytes(pointer, bufSize);

			// allow for fact sometimes start of data wrong
			if (firstRead && buffer[0] == 'r' && buffer[1] == 'e' && buffer[2] == 'f') charReached_legacy = 1;

			firstRead = false; // switch off

			/** look for xref or obj */
			for (int i = 0; i < bufSize; i++) {

				byte currentByte = buffer[i];

				if (debug) System.out.print((char) currentByte);

				/** check for xref OR end - reset if not */
				if (currentByte == oldPattern[charReached_legacy] && type != COMPRESSED) {
					charReached_legacy++;
					type = LEGACY;
				}
				else
					if ((currentByte == objStm[charReached_comp1]) && (charReached_comp1 == 0 || type == COMPRESSED)) {

						charReached_comp1++;
						type = COMPRESSED;
					}
					else
						if ((currentByte == XRef[charReached_comp2]) && (charReached_comp2 == 0 || type == COMPRESSED)) {

							charReached_comp2++;
							type = COMPRESSED;
						}
						else {

							charReached_legacy = 0;
							charReached_comp1 = 0;
							charReached_comp2 = 0;

							type = UNSET;
						}

				if (charReached_legacy == 3 || charReached_comp1 == 4 || charReached_comp2 == 3) break;

			}

			if (charReached_legacy == 3 || charReached_comp1 == 4 || charReached_comp2 == 3) break;

			// update pointer
			pointer = pointer + bufSize;

		}

		/**
		 * throw exception if no match or tell user which type
		 */
		if (type == UNSET) {
			try {
				closeFile();
			}
			catch (IOException e1) {
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + 1 + " closing file");
			}
			throw new PdfException("Exception unable to find ref or obj in trailer");
		}

		return type == COMPRESSED;
	}

	void closeFile() throws IOException {

		if (this.pdf_datafile != null) {
			this.pdf_datafile.close();
			this.pdf_datafile = null;
		}
	}

	private byte[] readTrailer(int bufSize, int pointer, int eof) throws IOException {

		int charReached = 0, charReached2 = 0, trailerCount = 0;
		final int end = 4;

		/** read in the bytes, using the startRef as our terminator */
		ByteArrayOutputStream bis = new ByteArrayOutputStream();

		while (true) {

			/** adjust buffer if less than 1024 bytes left in file */
			if (pointer + bufSize > eof) {
				bufSize = eof - pointer;
			}

			if (bufSize == 0) break;

			byte[] buffer = getBytes(pointer, bufSize);

			boolean endFound = false;

			/** write out and lookf for startref at end */
			for (int i = 0; i < bufSize; i++) {

				byte currentByte = buffer[i];

				/** check for startref at end - reset if not */
				if (currentByte == EOFpattern[charReached]) {
					charReached++;
				}
				else {
					charReached = 0;
				}

				/** check for trailer at end - ie second spurious trailer obj */
				if (currentByte == trailerpattern[charReached2]) {
					charReached2++;
				}
				else {
					charReached2 = 0;
				}

				if (charReached2 == 7) {
					trailerCount++;
					charReached2 = 0;
				}

				if (charReached == end || trailerCount == 2) { // located %%EOF and get last few bytes

					for (int j = 0; j < i + 1; j++) {
						bis.write(buffer[j]);
					}

					i = bufSize;
					endFound = true;

				}
			}

			// write out block if whole block used
			if (!endFound) {
				bis.write(buffer);
			}

			// update pointer
			pointer = pointer + bufSize;

			if (charReached == end || trailerCount == 2) {
				break;
			}

		}

		bis.close();
		return bis.toByteArray();
	}

	/**
	 * return pdf data
	 */
	public byte[] getBuffer() {
		return this.pdf_datafile.getPdfBuffer();
	}

	/** Utility method used during processing of type1C files */
	static private int getWord(byte[] content, int index, int size) {
		int result = 0;
		for (int i = 0; i < size; i++) {
			result = (result << 8) + (content[index + i] & 0xff);

		}
		return result;
	}

	/**
	 * read table of values
	 */
	private int readXRefs(Vector_Int xref, int current, byte[] Bytes, int endTable, int i) {

		char flag;
		int id, tokenCount, generation, lineLen, startLine, endLine;
		boolean skipNext = false;
		boolean isFirstValue = true;

		int[] breaks = new int[5];
		int[] starts = new int[5];

		// loop to read all references
		while (i < endTable) { // exit end at trailer

			startLine = i;
			endLine = -1;

			/**
			 * read line locations
			 */
			// move to start of value ignoring spaces or returns
			while (Bytes[i] != 10 && Bytes[i] != 13) {
				// scan for %
				if ((endLine == -1) && (Bytes[i] == 37)) endLine = i - 1;

				i++;
			}

			// set end if no comment
			if (endLine == -1) endLine = i - 1;

			// strip any spaces
			while (Bytes[startLine] == 32)
				startLine++;

			// strip any spaces
			while (Bytes[endLine] == 32)
				endLine--;

			i++;

			/**
			 * decode the line
			 */
			tokenCount = 0;
			lineLen = endLine - startLine + 1;

			if (lineLen > 0) {

				// decide if line is a section header or value

				// first count tokens
				int lastChar = 1, currentChar;
				for (int j = 1; j < lineLen; j++) {
					currentChar = Bytes[startLine + j];

					if ((currentChar == 32) && (lastChar != 32)) {
						breaks[tokenCount] = j;
						tokenCount++;
					}
					else
						if ((currentChar != 32) && (lastChar == 32)) {
							starts[tokenCount] = j;
						}

					lastChar = currentChar;
				}

				// update numbers so loops work
				breaks[tokenCount] = lineLen;
				tokenCount++;

				if (tokenCount == 1) { // fix for first 2 values on separate lines

					if (skipNext) skipNext = false;
					else {
						current = NumberUtils.parseInt(startLine, startLine + breaks[0], Bytes);
						skipNext = true;
					}

				}
				else
					if (tokenCount == 2) {
						current = NumberUtils.parseInt(startLine, startLine + breaks[0], Bytes);
					}
					else {

						id = NumberUtils.parseInt(startLine, startLine + breaks[0], Bytes);
						generation = NumberUtils.parseInt(startLine + starts[1], startLine + breaks[1], Bytes);

						flag = (char) Bytes[startLine + starts[2]];

						if ((flag == 'n')) { // only add objects in use

							/**
							 * assume not valid and test to see if valid
							 */
							boolean isValid = false;

							// get bytes
							int bufSize = 20;

							// adjust buffer if less than 1024 bytes left in file
							if (id + bufSize > this.eof) bufSize = (int) (this.eof - id);

							if (bufSize > 0) {

								/** get bytes into buffer */
								byte[] buffer = getBytes(id, bufSize);

								// look for space o b j
								for (int ii = 4; ii < bufSize; ii++) {
									if ((buffer[ii - 3] == 32 || buffer[ii - 3] == 10) && (buffer[ii - 2] == 111) && (buffer[ii - 1] == 98)
											&& (buffer[ii] == 106)) {
										isValid = true;
										ii = bufSize;
									}
								}

								// check number
								if (isValid && isFirstValue) {

									isFirstValue = false;

									if (buffer[0] == 48 && buffer[1] != 48 && current == 1) {
										current = 0;
									}
									else
										if (buffer[0] == 49 && buffer[1] == 32) {
											current = 1;
										}

								}

								if (isValid) {
									storeObjectOffset(current, id, generation, false, false);
									xref.addElement(id);
								}
								else {}
							}

							current++; // update our pointer
						}
						else
							if (flag == 'f') current++; // update our pointer

					}
			}
		}
		return current;
	}

	/**
	 * @param First
	 * @param compressedStream
	 */
	private void extractCompressedObjectOffset(Map offsetStart, Map offsetEnd, int First, byte[] compressedStream, int compressedID) {

		String lastKey = null, key, offset;

		int startKey, endKey, startOff, endOff, id;

		// read the offsets table
		for (int ii = 0; ii < First; ii++) {

			if (compressedStream.length == 0) continue;

			// ignore any gaps between entries
			// (for loop assumes single char and not always correct)
			while (compressedStream[ii] == 10 || compressedStream[ii] == 13 || compressedStream[ii] == 32)
				ii++;

			/** work out key size */
			startKey = ii;
			while (compressedStream[ii] != 32 && compressedStream[ii] != 13 && compressedStream[ii] != 10) {
				ii++;
			}
			endKey = ii - 1;

			/** extract key */
			int length = endKey - startKey + 1;
			char[] newCommand = new char[length];

			for (int i = 0; i < length; i++)
				newCommand[i] = (char) compressedStream[startKey + i];

			key = new String(newCommand);

			// track as number for later
			id = NumberUtils.parseInt(startKey, startKey + length, compressedStream);

			/** move to offset */
			while (compressedStream[ii] == 32 || compressedStream[ii] == 13 || compressedStream[ii] == 10)
				ii++;

			/** get size */
			startOff = ii;
			while ((compressedStream[ii] != 32 && compressedStream[ii] != 13 && compressedStream[ii] != 10) && (ii < First)) {
				ii++;
			}
			endOff = ii - 1;

			/** extract offset */
			length = endOff - startOff + 1;
			newCommand = new char[length];
			for (int i = 0; i < length; i++)
				newCommand[i] = (char) compressedStream[startOff + i];

			offset = new String(newCommand);

			/**
			 * save values if in correct block (can list items over-written in another compressed obj)
			 */
			if (compressedID == getOffset(id)) {
				offsetStart.put(key, offset);

				// save end as well
				if (lastKey != null) offsetEnd.put(lastKey, offset);

				lastKey = key;
			}
		}
	}

	private int readCompressedOffsets(int pntr, int current, int numbEntries, int[] fieldSizes, byte[] xrefs) throws PdfException {

		// now parse the stream and extract values

		final boolean debug = false;

		if (debug) System.out.println("===============read offsets============= current=" + current + " numbEntries=" + numbEntries);

		int[] defaultValue = { 1, 0, 0 };

		boolean hasCase0 = false;

		for (int i = 0; i < numbEntries; i++) {

			// read the next 3 values
			int[] nextValue = new int[3];
			for (int ii = 0; ii < 3; ii++) {
				if (fieldSizes[ii] == 0) {
					nextValue[ii] = defaultValue[ii];
				}
				else {
					nextValue[ii] = getWord(xrefs, pntr, fieldSizes[ii]);
					pntr = pntr + fieldSizes[ii];
				}
			}

			// handle values appropriately
			int id, gen;
			switch (nextValue[0]) {
				case 0: // linked list of free objects
					current++;

					hasCase0 = nextValue[1] == 0 && nextValue[2] == 0;

					if (debug) System.out.println("case 0 nextFree=" + nextValue[1] + " gen=" + nextValue[2]);

					break;
				case 1: // non-compressed
					id = nextValue[1];
					gen = nextValue[2];

					if (debug) System.out.println("case 1   current=" + current + " id=" + id + " byteOffset=" + nextValue[1] + " gen="
							+ nextValue[2]);

					// if number equals offset , test if valid
					boolean refIsvalid = true;
					if (current == id) {
						refIsvalid = false;

						// get the data and see if genuine match
						int size = 20;
						byte[] data = getBytes(current, size);

						// find space
						int ptr = 0;
						for (int ii = 0; ii < size; ii++) {
							if (data[ii] == 32 || data[ii] == 10 || data[ii] == 13) {
								ptr = ii;
								ii = size;

							}
						}

						if (ptr > 0) {
							int ref = NumberUtils.parseInt(0, ptr, data);
							if (ref == current) refIsvalid = true;
						}
					}

					if (refIsvalid || !hasCase0) storeObjectOffset(current, id, gen, false, false);

					current++;
					break;
				case 2: // compressed
					id = nextValue[1];
					// gen=nextValue[2];

					if (debug) System.out.println("case 2  current=" + current + " object number=" + id + " index=" + gen);

					storeObjectOffset(current, id, 0, true, false);

					current++;

					break;

				default:
					throw new PdfException("Exception Unsupported Compression mode with value " + nextValue[0]);
			}
		}

		return pntr;
	}

	byte[] readFDFData() throws IOException {

		int eof = (int) this.pdf_datafile.length();

		this.pdf_datafile.readLine(); // lose first line with definition
		int start = (int) this.pdf_datafile.getFilePointer();

		eof = eof - start;
		byte[] fileData = new byte[eof];
		this.pdf_datafile.read(fileData);

		return fileData;
	}

	/**
	 * precalculate sizes for each object
	 */
	private int[] calculateObjectLength(int eof) {

		// add eol to refs as catchall
		this.xref.addElement(eof);

		int[] xrefs = this.xref.get();

		// get order list of refs
		int xrefCount = xrefs.length;
		int[] xrefID = new int[xrefCount];
		for (int i = 0; i < xrefCount; i++)
			xrefID[i] = i;
		xrefID = Sorts.quicksort(xrefs, xrefID);

		// get ordered list of objects in offset order
		int objectCount = this.offset.getCapacity();

		int[] id = new int[objectCount];
		int[] offsets = new int[objectCount];

		// read from local copies and pop lookup table
		int[] off = this.offset.get();
		boolean[] isComp = this.isCompressed.get();
		for (int i = 0; i < objectCount; i++) {
			if (!isComp[i]) {
				offsets[i] = off[i];
				id[i] = i;
			}
		}

		id = Sorts.quicksort(offsets, id);

		int i = 0;
		// ignore empty values
		while (true) {

			if (offsets[id[i]] != 0) break;
			i++;

		}

		/**
		 * loop to calc all object lengths
		 * */
		int start = offsets[id[i]], end;

		// find next xref
		int j = 0;
		while (xrefs[xrefID[j]] < start + 1)
			j++;

		int[] ObjLengthTable = new int[objectCount];

		while (i < objectCount - 1) {

			end = offsets[id[i + 1]];
			int objLength = end - start - 1;

			// adjust for any xref
			if (xrefs[xrefID[j]] < end) {
				objLength = xrefs[xrefID[j]] - start - 1;
				while (xrefs[xrefID[j]] < end + 1)
					j++;
			}
			ObjLengthTable[id[i]] = objLength;
			// System.out.println(id[i]+" "+objLength+" "+start+" "+end);
			start = end;
			while (xrefs[xrefID[j]] < start + 1)
				j++;
			i++;
		}

		// special case - last object

		ObjLengthTable[id[i]] = xrefs[xrefID[j]] - start - 1;
		// System.out.println("*"+id[i]+" "+start+" "+xref+" "+eof);

		return ObjLengthTable;
	}

	// ///////////////////////////////////////////////////////////////////////
	/**
	 * version of move pointer which takes object name as int
	 */
	private void movePointer(int currentID, int generation) {
		movePointer(this.offset.elementAt(currentID));
	}

	public long getOffset(int currentID) {
		return this.offset.elementAt(currentID);
	}

	public byte[] getBytes(long start, int count) {
		byte[] buffer = new byte[count];

		movePointer(start);
		try {
			this.pdf_datafile.read(buffer); // get next chars
		}
		catch (IOException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		return buffer;
	}

	/**
	 * find a valid offset
	 */
	private String findOffsets() throws PdfSecurityException {

		if (LogWriter.isOutput()) LogWriter.writeLog("Corrupt xref table - trying to find objects manually");

		String root_id = "", line = null;
		int pointer, i;

		movePointer(0);

		while (true) {

			i = (int) this.getPointer();

			try {
				line = this.pdf_datafile.readLine();
			}
			catch (Exception e) {
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " reading line");
			}
			if (line == null) break;

			if (line.contains(" obj")) {

				pointer = line.indexOf(' ');
				if (pointer > -1) storeObjectOffset(Integer.parseInt(line.substring(0, pointer)), i, 1, false, true);

			}
			else
				if (line.contains("/Root")) {

					int start = line.indexOf("/Root") + 5;
					pointer = line.indexOf('R', start);
					if (pointer > -1) root_id = line.substring(start, pointer + 1).trim();
				}
				else
					if (line.contains("/Encrypt")) {
						// too much risk on corrupt file
						throw new PdfSecurityException("Corrupted, encrypted file");
					}
		}

		return root_id;
	}

	public void storeLinearizedTables(LinearizedHintTable linHintTable) {
		this.linHintTable = linHintTable;
	}

	public void dispose() {

		if (this.decryption != null) {
			this.decryption.flush();
			this.decryption.dispose();
		}

		if (this.decryption != null) this.decryption.cipher = null;

		this.decryption = null;

		this.compressedObj = null;

		// any linearized data
		if (this.linHintTable != null) {
			this.linHintTable = null;
		}

		this.offset = null;
		this.generation = null;
		this.isCompressed = null;

		try {
			if (this.pdf_datafile != null) this.pdf_datafile.close();
		}
		catch (IOException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		this.pdf_datafile = null;

		this.xref = null;
	}

	// ///////////////////////////////////////////////////////////////////////
	/**
	 * version of move pointer which takes object name and converts before calling main routine
	 */
	private void movePointer(String pages_id) {
		long pointer = getOffset(pages_id);

		movePointer(pointer);
	}

	// ////////////////////////////////////////////////////////////////////
	/**
	 * get pdf type in file (found at start of file)
	 */
	final public String getType() {

		String pdf_type = "";
		try {
			movePointer(0);
			pdf_type = this.pdf_datafile.readLine();

			// strip off anything before
			int pos = pdf_type.indexOf("%PDF");
			if (pos != -1) pdf_type = pdf_type.substring(pos + 5);

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " in reading type");
		}
		return pdf_type;
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * returns current location pointer and sets to new value
	 */
	private void movePointer(long pointer) {
		try {
			// make sure inside file
			if (pointer > this.pdf_datafile.length()) {

				if (LogWriter.isOutput()) LogWriter.writeLog("Attempting to access ref outside file");
				// throw new PdfException("Exception moving file pointer - ref outside file");
			}
			else {
				this.pdf_datafile.seek(pointer);
			}
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " moving pointer to  " + pointer + " in file.");
		}
	}

	private void read(byte[] buffer) throws IOException {
		this.pdf_datafile.read(buffer);
	}

	// ////////////////////////////////////////////////
	/**
	 * gets pointer to current location in the file
	 */
	private long getPointer() {
		long old_pointer = 0;
		try {
			old_pointer = this.pdf_datafile.getFilePointer();
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " getting pointer in file");
		}
		return old_pointer;
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * place object details in queue
	 */
	private void storeObjectOffset(int current_number, int current_offset, int current_generation, boolean isEntryCompressed, boolean isBumData) {

		/**
		 * check it does not already exist
		 */
		int existing_generation = 0;
		int offsetNumber = 0;

		if (current_number < this.generation.getCapacity()) {
			existing_generation = this.generation.elementAt(current_number);
			offsetNumber = this.offset.elementAt(current_number);

		}

		// write out if not a newer copy (ignore items from Prev tables if newer)
		// if bum data accept if higher position a swe are trawling file manually anf higher figure probably newer
		if (existing_generation < current_generation || offsetNumber == 0 || isBumData && (current_offset > this.offset.elementAt(current_number))) {
			this.offset.setElementAt(current_offset, current_number);
			this.generation.setElementAt(current_generation, current_number);
			this.isCompressed.setElementAt(isEntryCompressed, current_number);
		}
		else {
			// LogWriter.writeLog("Object "+current_number + ", generation "+
			// current_generation + " already exists as"+
			// existing_generation);
		}
	}

	// /////////////////////////////////////////////////////////////////////////

	/**
	 * returns stream in which compressed object will be found
	 */
	private int getCompressedStreamObject(int currentID, int gen) {
		return this.offset.elementAt(currentID);
	}

	/**
	 * returns stream in which compressed object will be found (actually reuses getOffset internally)
	 */
	private int getCompressedStreamObject(String value) {

		int currentID = 0;
		// handle indirect reference
		if (value.endsWith("R")) {
			StringTokenizer values = new StringTokenizer(value);
			currentID = Integer.parseInt(values.nextToken());
		}
		else
			if (LogWriter.isOutput()) LogWriter.writeLog("Error with reference ..value=" + value + '<');

		return this.offset.elementAt(currentID);
	}

	/**
	 * general routine to turn reference into id with object name
	 */
	private int getOffset(String value) {

		int currentID = 0;
		// handle indirect reference
		if (value.endsWith("R")) {
			StringTokenizer values = new StringTokenizer(value);
			currentID = Integer.parseInt(values.nextToken());
		}
		else
			if (LogWriter.isOutput()) LogWriter.writeLog("2. Error with reference .." + value + "<<");

		return this.offset.elementAt(currentID);
	}

/**
     * returns where in compressed stream value can be found
     * (actually reuses getGen internally)
     *
     protected final int getOffsetInCompressedStream( String value )
     {

     int currentID=0;
     //		handle indirect reference
     if( value.endsWith( "R" ) == true )
     {
     StringTokenizer values = new StringTokenizer( value );
     currentID = Integer.parseInt( values.nextToken() );
     currentGeneration=Integer.parseInt( values.nextToken() );
     }
     else
     LogWriter.writeLog( "3. Error with reference .." + value+"<" );

     return generation.elementAt(currentID );
     }/**/

/**
     * general routine to turn reference into id with object name
     *
     protected final int getGen( String value )
     {

     int currentID=0;
     //		handle indirect reference
     if( value.endsWith( "R" ) == true )
     {
     StringTokenizer values = new StringTokenizer( value );
     currentID = Integer.parseInt( values.nextToken() );
     currentGeneration=Integer.parseInt( values.nextToken() );
     }
     else
     LogWriter.writeLog( "4. Error with reference .." + value+"<" );

     return generation.elementAt(currentID );
     }/**/

	/**
	 * general routine to turn reference into id with object name
	 */
	public final boolean isCompressed(int ref, int gen) {
		return this.isCompressed.elementAt(ref);
	}

	/**
	 * general routine to turn reference into id with object name
	 */
	private boolean isCompressed(String value) {

		int currentID = 0;
		// handle indirect reference
		if (value.endsWith("R")) {
			StringTokenizer values = new StringTokenizer(value);
			currentID = Integer.parseInt(values.nextToken());
		}
		else
			if (LogWriter.isOutput()) LogWriter.writeLog("5.Error with reference .." + value + '<');

		return this.isCompressed.elementAt(currentID);
	}

	public DecryptionFactory getDecryptionObject() {
		return this.decryption;
	}

	public void setPassword(String password) {

		this.encryptionPassword = password.getBytes();

		// reset
		if (this.decryption != null) this.decryption.reset(this.encryptionPassword);
	}

	/**
	 * read an object in the pdf into a Object which can be an indirect or an object
	 * 
	 */
	byte[] readObjectData(PdfObject pdfObject) {

		String objectRef = pdfObject.getObjectRefAsString();

		// read the Dictionary data
		if (pdfObject.isDataExternal()) {
			// byte[] data=readObjectAsByteArray(pdfObject, objectRef, isCompressed(number,generation),number,generation);
			byte[] data = readObjectAsByteArray(pdfObject, false, pdfObject.getObjectRefID(), 0);

			// allow for data in Linear object not yet loaded
			if (data == null) {
				pdfObject.setFullyResolved(false);

				// if(debugFastCode)
				// System.out.println(paddingString+"Data not yet loaded");

				if (LogWriter.isOutput()) LogWriter.writeLog("[Linearized] " + pdfObject.getObjectRefAsString() + " not yet available (15)");

				return data;
			}
		}

		final boolean debug = false;

		if (debug) System.err.println("reading objectRef=" + objectRef + "< isCompressed=" + isCompressed(objectRef));

		boolean isCompressed = isCompressed(objectRef);
		pdfObject.setCompressedStream(isCompressed);

		// any stream
		byte[] raw;

		/** read raw object data */
		if (isCompressed) {
			raw = readCompressedObjectData(pdfObject, objectRef);
		}
		else {
			movePointer(objectRef);

			if (objectRef.charAt(0) == '<') {
				raw = readObjectData(-1, pdfObject);
			}
			else {
				int pointer = objectRef.indexOf(' ');
				int id = Integer.parseInt(objectRef.substring(0, pointer));

				if (this.ObjLengthTable == null || this.refTableInvalid) { // isEncryptionObject

					// allow for bum object
					if (getPointer() == 0) raw = new byte[0];
					else raw = readObjectData(-1, pdfObject);

				}
				else
					if (id > this.ObjLengthTable.length || this.ObjLengthTable[id] == 0) {
						if (LogWriter.isOutput()) LogWriter.writeLog(objectRef + " cannot have offset 0");

						raw = new byte[0];
					}
					else raw = readObjectData(this.ObjLengthTable[id], pdfObject);
			}
		}

		return raw;
	}

	private byte[] readCompressedObjectData(PdfObject pdfObject, String objectRef) {
		byte[] raw;
		int objectID = Integer.parseInt(objectRef.substring(0, objectRef.indexOf(' ')));
		int compressedID = getCompressedStreamObject(objectRef);
		String compressedRef = compressedID + " 0 R", startID = null;
		int First = this.lastFirst;
		boolean isCached = true; // assume cached

		// see if we already have values
		byte[] compressedStream = this.lastCompressedStream;
		Map offsetStart = this.lastOffsetStart;
		Map offsetEnd = this.lastOffsetEnd;

		PdfObject Extends = null;

		if (this.lastOffsetStart != null) startID = (String) this.lastOffsetStart.get(String.valueOf(objectID));

		// read 1 or more streams
		while (startID == null) {

			if (Extends != null) {
				this.compressedObj = Extends;
			}
			else
				if (compressedID != this.lastCompressedID) {

					isCached = false;

					movePointer(compressedRef);

					raw = readObjectData(this.ObjLengthTable[compressedID], null);

					this.compressedObj = new CompressedObject(compressedRef);
					ObjectDecoder objDecoder = new ObjectDecoder(this);
					objDecoder.readDictionaryAsObject(this.compressedObj, 0, raw);

				}

			/** get offsets table see if in this stream */
			offsetStart = new HashMap();
			offsetEnd = new HashMap();
			First = this.compressedObj.getInt(PdfDictionary.First);

			compressedStream = this.compressedObj.getDecodedStream();

			extractCompressedObjectOffset(offsetStart, offsetEnd, First, compressedStream, compressedID);

			startID = (String) offsetStart.get(String.valueOf(objectID));

			Extends = this.compressedObj.getDictionary(PdfDictionary.Extends);
			if (Extends == null) break;

		}

		if (!isCached) {
			this.lastCompressedStream = compressedStream;
			this.lastCompressedID = compressedID;
			this.lastOffsetStart = offsetStart;
			this.lastOffsetEnd = offsetEnd;
			this.lastFirst = First;
		}

		/** put bytes in stream */
		int start = First + Integer.parseInt(startID), end = compressedStream.length;

		String endID = (String) offsetEnd.get(String.valueOf(objectID));
		if (endID != null) end = First + Integer.parseInt(endID);

		int streamLength = end - start;
		raw = new byte[streamLength];
		System.arraycopy(compressedStream, start, raw, 0, streamLength);

		pdfObject.setInCompressedStream(true);
		return raw;
	}

	private byte[] readObjectData(int bufSize, PdfObject pdfObject) {

		// old version
		if (bufSize < 1 || this.newCacheSize != -1) return readObjectDataXX(bufSize, pdfObject);

		byte[] dataRead = null;

		// trap for odd file with no endobj
		if (bufSize > 0) {

			bufSize += 6;
			dataRead = new byte[bufSize];

			fillBuffer(dataRead);
		}

		return dataRead;
	}

	private void fillBuffer(byte[] buffer) {
		try {
			read(buffer); // get data
		}
		catch (IOException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	private byte[] readObjectDataXX(int bufSize, PdfObject pdfObject) {

		int newCacheSize = -1, startStreamCount = 0, charReached = 0, charReached3 = 0;
		boolean startStreamFound = false, reachedCacheLimit = false, inStream = false, inLoop = true;
		long start = getPointer();

		if (pdfObject != null) // only use if values found
		newCacheSize = this.newCacheSize;

		int rawSize = bufSize, realPos = 0;
		boolean lengthSet = false; // start false and set to true if we find /Length in metadata

		if (bufSize < 1) bufSize = 128;

		if (newCacheSize != -1 && bufSize > newCacheSize) bufSize = newCacheSize;

		byte[] dataRead = null;
		byte currentByte;
		int i = bufSize - 1;

		/** read the object or block adjust buffer if less than bytes left in file */
		while (inLoop) {

			i++;

			if (i == bufSize) {

				/** read the next block and adjust buffer if less than bytes left in file */
				long pointer = getPointer();

				if (pointer + bufSize > this.eof) bufSize = (int) (this.eof - pointer);

				// trap for odd file with no endobj
				if (bufSize == 0) break;

				bufSize += 6;
				byte[] buffer = new byte[bufSize];

				fillBuffer(buffer); // get data

				/**
				 * allow for offset being wrong on first block and hitting part of endobj and cleanup so does not break later code and set DataRead to
				 * buffer
				 */
				if (dataRead == null) {
					int j = 0;
					while (buffer[j] == 'e' || buffer[j] == 'n' || buffer[j] == 'd' || buffer[j] == 'o' || buffer[j] == 'b' || buffer[j] == 'j') {
						j++;
					}

					if (j > 0) { // adjust to remove stuff at start
						byte[] oldBuffer = buffer;
						int newLength = buffer.length - j;
						buffer = new byte[newLength];
						System.arraycopy(oldBuffer, j, buffer, 0, newLength);
					}

					dataRead = buffer;

				}
				else {
					dataRead = appendDataBlock(buffer.length, buffer, dataRead);
				}

				i = 0;
			}

			currentByte = dataRead[realPos];

			if (!inStream) {
				/** check for endobj at end - reset if not */
				if (currentByte == endPattern[charReached]) charReached++;
				else {
					charReached = 0;
				}
			}

			// look for start of stream and set inStream true
			if (!startStreamFound && newCacheSize != -1 && !reachedCacheLimit) {
				if (startStreamCount < 6 && currentByte == startStream[startStreamCount]) {
					startStreamCount++;

					if (startStreamCount == 6) // stream start found so log
					startStreamFound = true;
				}
				else startStreamCount = 0;
			}

			// switch on caching
			if (startStreamFound && dataRead != null && dataRead.length > newCacheSize) { // stop if over max size
				pdfObject.setCache(start, this);
				reachedCacheLimit = true;
			}

			// also scan for /Length if it had a valid size - if length not set we go on endstream in data
			if (!startStreamFound && !lengthSet && rawSize != -1) {
				if (currentByte == lengthString[charReached3] && !inStream) {
					charReached3++;
					if (charReached3 == 6) lengthSet = true;
				}
				else charReached3 = 0;
			}

			realPos++;

			if (charReached == 6) {

				if (!lengthSet) inLoop = false;

				charReached = 0;
			}

			if (lengthSet && realPos > rawSize) inLoop = false;
		}

		if (!lengthSet) dataRead = ObjectUtils.checkEndObject(dataRead);

		return dataRead;
	}

	private static byte[] appendDataBlock(int newBytes, byte[] buffer, byte[] dataRead) {

		int bytesRead = dataRead.length;

		byte[] tmp = new byte[bytesRead + newBytes];

		// existing data into new array
		System.arraycopy(dataRead, 0, tmp, 0, bytesRead);
		System.arraycopy(buffer, 0, tmp, bytesRead, newBytes);

		return tmp;
	}

	/**
	 * get object as byte[]
	 * 
	 * @param isCompressed
	 * @param objectID
	 * @param gen
	 */
	public byte[] readObjectAsByteArray(PdfObject pdfObject, boolean isCompressed, int objectID, int gen) {

		byte[] raw = null;

		// data not in PDF stream
		// if(pdfObject.isDataExternal()){
		if (this.linHintTable != null) {
			raw = this.linHintTable.getObjData(objectID);
		}

		if (raw == null) {

			/** read raw object data */
			if (isCompressed) {
				raw = readCompressedObjectAsByteArray(pdfObject, objectID, gen);
			}
			else {
				movePointer(objectID, gen);

				if (this.ObjLengthTable == null || this.refTableInvalid) raw = readObjectData(-1, pdfObject);
				else
					if (objectID > this.ObjLengthTable.length) return null;
					else raw = readObjectData(this.ObjLengthTable[objectID], pdfObject);
			}

		}
		return raw;
	}

	private byte[] readCompressedObjectAsByteArray(PdfObject pdfObject, int objectID, int gen) {
		byte[] raw;
		int compressedID = getCompressedStreamObject(objectID, gen);
		String startID = null, compressedRef;
		Map offsetStart = this.lastOffsetStart, offsetEnd = this.lastOffsetEnd;
		int First = this.lastFirst;
		byte[] compressedStream;
		boolean isCached = true; // assume cached

		PdfObject compressedObj, Extends;

		// see if we already have values
		compressedStream = this.lastCompressedStream;
		if (this.lastOffsetStart != null) startID = (String) this.lastOffsetStart.get(String.valueOf(objectID));

		// read 1 or more streams
		while (startID == null) {

			isCached = false;

			movePointer(compressedID, 0);

			raw = readObjectData(this.ObjLengthTable[compressedID], null);

			// may need to use compObj and not objectRef
			String compref = compressedID + " " + gen + " R";
			compressedObj = new CompressedObject(compref);
			ObjectDecoder objDecoder = new ObjectDecoder(this);
			objDecoder.readDictionaryAsObject(compressedObj, 0, raw);

			/** get offsets table see if in this stream */
			offsetStart = new HashMap();
			offsetEnd = new HashMap();

			First = compressedObj.getInt(PdfDictionary.First);

			// do later due to code above
			compressedStream = compressedObj.getDecodedStream();

			extractCompressedObjectOffset(offsetStart, offsetEnd, First, compressedStream, compressedID);

			startID = (String) offsetStart.get(String.valueOf(objectID));

			Extends = compressedObj.getDictionary(PdfDictionary.Extends);
			if (Extends == null) compressedRef = null;
			else compressedRef = Extends.getObjectRefAsString();

			if (compressedRef != null) compressedID = Integer.parseInt(compressedRef.substring(0, compressedRef.indexOf(' ')));

		}

		if (!isCached) {
			this.lastCompressedStream = compressedStream;
			this.lastOffsetStart = offsetStart;
			this.lastOffsetEnd = offsetEnd;
			this.lastFirst = First;
		}

		/** put bytes in stream */
		int start = First + Integer.parseInt(startID), end = compressedStream.length;
		String endID = (String) offsetEnd.get(String.valueOf(objectID));
		if (endID != null) end = First + Integer.parseInt(endID);

		int streamLength = end - start;
		raw = new byte[streamLength];
		System.arraycopy(compressedStream, start, raw, 0, streamLength);

		pdfObject.setInCompressedStream(true);
		return raw;
	}

	// /////////////////////////////////////////////////////////////////
	/**
	 * get postscript data (which may be split across several objects)
	 */
	public byte[] readPageIntoStream(PdfObject pdfObject) {

		byte[][] pageContents = pdfObject.getKeyArray(PdfDictionary.Contents);

		// reset buffer object
		byte[] binary_data = new byte[0];

		// exit on empty
		if (pageContents == null || (pageContents != null && pageContents.length > 0 && pageContents[0] == null)) return binary_data;

		/** read an array */
		if (pageContents != null) {

			int count = pageContents.length;

			byte[] decoded_stream_data;
			PdfObject streamData;

			// read all objects for page into stream
			for (int ii = 0; ii < count; ii++) {

				// if(pageContents[ii].length==0)
				// break;

				// get the data for an object
				// currentPdfFile.resetCache();
				// decoded_stream_data =currentPdfFile.readStream(new String(pageContents[ii]),true);

				streamData = new StreamObject(new String(pageContents[ii]));
				streamData.isDataExternal(pdfObject.isDataExternal());// flag if being read from external stream
				readObject(streamData);

				decoded_stream_data = streamData.getDecodedStream();

				// System.out.println(decoded_stream_data+" "+OLDdecoded_stream_data);
				if (ii == 0 && decoded_stream_data != null) binary_data = decoded_stream_data;
				else binary_data = appendData(binary_data, decoded_stream_data);
			}
		}

		return binary_data;
	}

	/**
	 * append into data_buffer by copying processed_data then binary_data into temp and then temp back into binary_data
	 * 
	 * @param binary_data
	 * @param decoded_stream_data
	 */
	private static byte[] appendData(byte[] binary_data, byte[] decoded_stream_data) {

		if (decoded_stream_data != null) {
			int current_length = binary_data.length + 1;

			// find end of our data which we decompressed.
			int processed_length = decoded_stream_data.length;
			if (processed_length > 0) { // trap error
				while (decoded_stream_data[processed_length - 1] == 0)
					processed_length--;

				// put current into temp so I can resize array
				byte[] temp = new byte[current_length];
				System.arraycopy(binary_data, 0, temp, 0, current_length - 1);

				// add a space between streams
				temp[current_length - 1] = ' ';

				// resize
				binary_data = new byte[current_length + processed_length];

				// put original data back
				System.arraycopy(temp, 0, binary_data, 0, current_length);

				// and add in new data
				System.arraycopy(decoded_stream_data, 0, binary_data, current_length, processed_length);
			}
		}
		return binary_data;
	}

	public void setCertificate(Certificate certificate, PrivateKey key) {
		this.certificate = certificate;
		this.key = key;
	}
}
