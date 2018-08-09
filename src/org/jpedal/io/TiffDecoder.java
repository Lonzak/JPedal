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
 * TiffDecoder.java
 * ---------------
 */
package org.jpedal.io;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.utils.LogWriter;

/**
 * converts CCITT stream into either an image of bytestream
 * 
 * Many thanks to Brian Burkhalter for all his help
 */
public class TiffDecoder {

	private byte[] bytes;

	/**
	 * called with values from PDF Map contains values from PDF as stream pair
	 */
	public TiffDecoder(int w, int h, Map values, byte[] data) {

		// return value
		this.bytes = null;

		/**
		 * get values from stream
		 */
		// flag to show if default is black or white
		boolean isBlack = false;
		// int columns = 1728; //in PDF spec
		int k = 0;
		// boolean isByteAligned=false; //in PDF spec

		// get k (type of encoding)
		String value = (String) values.get("K");
		if (value != null) k = Integer.parseInt(value);

		/**
		 * //get flag for white/black as default value = (String) values.get("EncodedByteAlign"); if (value != null) isByteAligned =
		 * Boolean.valueOf(value).booleanValue();
		 */

		// get flag for white/black as default
		value = (String) values.get("BlackIs1");
		if (value != null) {
			isBlack = Boolean.valueOf(value);

		}

		/**
		 * not used but in Map from PDF value = (String) values.get("Rows"); if (value != null) rows = Integer.parseInt(value);
		 * 
		 * value = (String) values.get("Columns"); if (value != null) columns= Integer.parseInt(value);
		 */

		buildImage(w, h, data, isBlack, k);
	}

	public TiffDecoder(int w, int h, PdfObject DecodeParms, byte[] data) {

		// Map values=new HashMap();
		// return value
		this.bytes = null;

		/**
		 * get values from stream
		 */
		// flag to show if default is black or white
		boolean isBlack = false;
		// int columns = 1728; //in PDF spec
		int k = 0;
		// boolean isByteAligned=false; //in PDF spec

		if (DecodeParms != null) {

			// get k (type of encoding)
			k = DecodeParms.getInt(PdfDictionary.K);

			int columnsSet = DecodeParms.getInt(PdfDictionary.Columns);
			if (columnsSet != -1) w = columnsSet;

			// get flag for white/black as default
			isBlack = DecodeParms.getBoolean(PdfDictionary.BlackIs1);

		}

		/**
		 * not used but in Map from PDF value = (String) values.get("Rows"); if (value != null) rows = Integer.parseInt(value);
		 * 
		 * value = (String) values.get("Columns"); if (value != null) columns= Integer.parseInt(value);
		 */

		buildImage(w, h, data, isBlack, k);
	}

	/**
	 * convenience method to add a header to a CCITT data block so it can be viewed as a TIFF
	 * 
	 * @param w
	 * @param h
	 * @param DecodeParms
	 * @param data
	 * @param fileName
	 */
	public static void saveAsTIFF(int w, int h, PdfObject DecodeParms, byte[] data, String fileName) {

		/**
		 * get values from stream
		 */
		boolean isBlack = false; // flag to show if default is black or white
		int k = 0;

		if (DecodeParms != null) {

			// get k (type of encoding)
			k = DecodeParms.getInt(PdfDictionary.K);

			int columnsSet = DecodeParms.getInt(PdfDictionary.Columns);
			if (columnsSet != -1) w = columnsSet;

			// get flag for white/black as default
			isBlack = DecodeParms.getBoolean(PdfDictionary.BlackIs1);

		}

		/**
		 * build the image
		 */
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		/**
		 * tiff header (id, version, offset)
		 * */
		final String[] headerValues = { "4d", "4d", "00", "2a", "00", "00", "00", "08" };
		for (String headerValue : headerValues)
			bos.write(Integer.parseInt(headerValue, 16));

		int tagCount = 9; // appears to be minimum needed

		/**
		 * write IFD image file directory
		 */
		writeWord(String.valueOf(tagCount), bos); // num of directory entries
		writeTag("256", "04", "01", String.valueOf(w), bos);
		/** image width */
		writeTag("257", "04", "01", String.valueOf(h), bos);
		/** image length */
		writeTag("258", "03", "01", "00010000h", bos);
		/** BitsPerSample 258 - b&w 1 bit image */

		if (k == 0) {
			writeTag("259", "03", "01", "00030000h", bos);
			/** compression 259 */
		}
		else
			if (k > 0) writeTag("259", "03", "01", "00020000h", bos);
			/** compression 259 */
			else
				if (k < 0) writeTag("259", "03", "01", "00040000h", bos);
		/** compression 259 */

		if (!isBlack) writeTag("262", "03", "01", "00000000h", bos);
		/** photometricInterpretation 262 */
		else writeTag("262", "03", "01", "00010000h", bos);
		/** photometricInterpretation 262 */

		writeTag("273", "04", "1", "122", bos);
		/** stripOffsets 273 -start of data after tables */
		writeTag("277", "03", "01", "00010000h", bos);
		/** samplesPerPixel 277 */
		writeTag("278", "04", "01", String.valueOf(h), bos);
		/** rowsPerStrip 278 - uses height */
		writeTag("279", "04", "1", String.valueOf(data.length), bos);
		/** stripByteCount 279 - 1 strip so all data */
		writeDWord("0", bos);
		/** write next IOD offset zero as no other table */

		/**
		 * write the CCITT image data at the end
		 */
		try {

			bos.write(data);
			bos.close();

		}
		catch (IOException e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Tiff exception  " + e);
		}

		/** save image */
		try {

			java.io.FileOutputStream fos = new java.io.FileOutputStream(fileName);
			fos.write(bos.toByteArray());
			fos.close();

		}
		catch (Error err) {

			if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Tiff error " + err);

		}
		catch (Exception e1) {
			if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Tiff exception  " + e1);
		}
	}

	private void buildImage(int w, int h, byte[] data, boolean isBlack, int k) {
		/**
		 * build the image
		 */
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		/**
		 * tiff header (id, version, offset)
		 * */
		final String[] headerValues = { "4d", "4d", "00", "2a", "00", "00", "00", "08" };
		for (String headerValue : headerValues)
			bos.write(Integer.parseInt(headerValue, 16));

		int tagCount = 9; // appears to be minimum needed
		// int stripCount=1; //1 strip with all CCITT data

		/**
		 * write IFD image file directory
		 */
		writeWord(String.valueOf(tagCount), bos); // num of directory entries
		writeTag("256", "04", "01", String.valueOf(w), bos);
		/** image width */
		writeTag("257", "04", "01", String.valueOf(h), bos);
		/** image length */
		writeTag("258", "03", "01", "00010000h", bos);
		/** BitsPerSample 258 - b&w 1 bit image */

		if (k == 0) {
			writeTag("259", "03", "01", "00030000h", bos);
			/** compression 259 */
		}
		else
			if (k > 0) writeTag("259", "03", "01", "00020000h", bos);
			/** compression 259 */
			else
				if (k < 0) writeTag("259", "03", "01", "00040000h", bos);
		/** compression 259 */

		if (!isBlack) writeTag("262", "03", "01", "00000000h", bos);
		/** photometricInterpretation 262 */
		else writeTag("262", "03", "01", "00010000h", bos);
		/** photometricInterpretation 262 */

		writeTag("273", "04", "1", "122", bos);
		/** stripOffsets 273 -start of data after tables */
		writeTag("277", "03", "01", "00010000h", bos);
		/** samplesPerPixel 277 */
		writeTag("278", "04", "01", String.valueOf(h), bos);
		/** rowsPerStrip 278 - uses height */
		writeTag("279", "04", "1", String.valueOf(data.length), bos);
		/** stripByteCount 279 - 1 strip so all data */
		writeDWord("0", bos);
		/** write next IOD offset zero as no other table */

		/**
		 * write the CCITT image data at the end
		 */
		try {

			bos.write(data);
			bos.close();

		}
		catch (IOException e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Tiff exception  " + e);
		}

		/** setup image */
		try {

			/**
			 * write out to debug* System.out.println("mac_"+data.length+".tiff"); java.io.FileOutputStream fos=new
			 * java.io.FileOutputStream("mac_"+data.length+".tiff"); fos.write(bos.toByteArray()); fos.close();
			 **/

			JAIHelper.confirmJAIOnClasspath();

			com.sun.media.jai.codec.ByteArraySeekableStream fss = new com.sun.media.jai.codec.ByteArraySeekableStream(bos.toByteArray());// .wrapInputStream(bis,true);

			javax.media.jai.RenderedOp op = (javax.media.jai.JAI.create("stream", fss));

			Raster raster = op.getData();

			// Raster raster = img2.getData();
			DataBuffer db = raster.getDataBuffer();

			DataBufferByte dbb = (DataBufferByte) db;

			this.bytes = dbb.getData();

			if (!isBlack) { // invert if needed
				int bcount = this.bytes.length;
				for (int i = 0; i < bcount; i++) {
					this.bytes[i] = (byte) (255 - (this.bytes[i]));
				}
			}

		}
		catch (Error err) {

			if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Tiff error " + err);

		}
		catch (Exception e1) {
			if (LogWriter.isOutput()) LogWriter.writeLog("[PDF] Tiff exception  " + e1);
		}
	}

	/** return raw bytes from image */
	public byte[] getRawBytes() {
		return this.bytes;
	}

	/** write word (2 bytes to stream) */
	private static void writeWord(String i, ByteArrayOutputStream bos) {

		int value;

		// allow decimal,octal or hex
		if (i.endsWith("h")) value = Integer.parseInt(i.substring(0, i.length() - 1), 16);
		else
			if (i.endsWith("o")) value = Integer.parseInt(i.substring(0, i.length() - 1), 8);
			else value = Integer.parseInt(i);

		bos.write((value >> 8)); // high byte
		bos.write(value & 0xFF); // low byte
	}

	/** write Dword (4 bytes to stream) */
	private static void writeDWord(String i, ByteArrayOutputStream bos) {

		int value;

		// allow decimal,octal or hex
		if (i.endsWith("h")) value = Integer.parseInt(i.substring(0, i.length() - 1), 16);
		else
			if (i.endsWith("o")) value = Integer.parseInt(i.substring(0, i.length() - 1), 8);
			else value = Integer.parseInt(i);

		bos.write((value >> 24) & 0xff); // high byte
		bos.write((value >> 16) & 0xff);
		bos.write((value >> 8) & 0xff);
		bos.write(value & 0xFF); // low byte
	}

	/** write a tag to stream */
	private static void writeTag(String TagId, String dataType, String DataCount, String DataOffset, ByteArrayOutputStream bos) {

		writeWord(TagId, bos);
		writeWord(dataType, bos);
		writeDWord(DataCount, bos);
		writeDWord(DataOffset, bos);
	}
}
