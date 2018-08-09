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
 * RandomAccessMemoryMapBuffer.java
 * ---------------
 */

package org.jpedal.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.jpedal.utils.LogWriter;

public class RandomAccessMemoryMapBuffer implements RandomAccessBuffer {

	// private byte[] data;
	private long pointer;

	private int length = 0;

	private File file;
	private RandomAccessFile buf;

	public RandomAccessMemoryMapBuffer(InputStream in) {

		this.pointer = -1;

		this.length = 0;

		FileOutputStream to = null;
		BufferedInputStream from = null;

		try {

			this.file = File.createTempFile("page", ".bin", new File(ObjectStore.temp_dir));
			// file.deleteOnExit();

			to = new java.io.FileOutputStream(this.file);

			from = new BufferedInputStream(in);

			// write
			byte[] buffer = new byte[65535];
			int bytes_read;
			while ((bytes_read = from.read(buffer)) != -1) {
				to.write(buffer, 0, bytes_read);
				this.length = this.length + bytes_read;
			}

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());

		}
		// close streams
		try {
			if (to != null) to.close();
			if (from != null) from.close();
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " closing files");
		}

		try {
			init();
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	public RandomAccessMemoryMapBuffer(byte[] data) {

		this.pointer = -1;

		this.length = data.length;

		try {

			this.file = File.createTempFile("page", ".bin", new File(ObjectStore.temp_dir));
			// file.deleteOnExit();

			java.io.FileOutputStream a = new java.io.FileOutputStream(this.file);

			a.write(data);
			a.flush();
			a.close();

			init();

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	private void init() throws Exception {

		// Create a read-only memory-mapped file
		this.buf = new RandomAccessFile(this.file, "r");
	}

	@Override
	public long getFilePointer() throws IOException {
		return this.pointer;
	}

	@Override
	public void seek(long pos) throws IOException {
		if (checkPos(pos)) {
			this.pointer = pos;
		}
		else {
			throw new IOException("Position out of bounds");
		}
	}

	@Override
	public void close() throws IOException {

		if (this.buf != null) {

			this.buf.close();
			this.buf = null;
		}

		this.pointer = -1;

		if (this.file != null && this.file.exists()) {
			this.file.delete();
		}
	}

	/**/@Override
	public void finalize() {

		try {
			super.finalize();
		}
		catch (Throwable e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		// ensure removal actual file
		try {
			close();
		}
		catch (IOException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	} /**/

	@Override
	public long length() throws IOException {

		if (this.buf != null) {
			return this.length;
		}
		else {
			throw new IOException("Data buffer not initialized.");
		}
	}

	@Override
	public int read() throws IOException {
		if (checkPos(this.pointer)) {
			this.buf.seek(this.pointer++);
			return b2i(this.buf.readByte());
		}
		else {
			return -1;
		}
	}

	private int peek() throws IOException {
		if (checkPos(this.pointer)) {
			this.buf.seek(this.pointer++);
			return b2i(this.buf.readByte());
		}
		else {
			return -1;
		}
	}

	/**
	 * return next line (returns null if no line)
	 */
	@Override
	public String readLine() throws IOException {

		if (this.pointer >= this.length - 1) {
			return null;
		}
		else {

			StringBuilder buf = new StringBuilder();
			int c;
			while ((c = read()) >= 0) {
				if ((c == 10) || (c == 13)) {
					if (((peek() == 10) || (peek() == 13)) && (peek() != c)) read();
					break;
				}
				buf.append((char) c);
			}
			return buf.toString();
		}
	}

	@Override
	public int read(byte[] b) throws IOException {

		if (this.buf == null) throw new IOException("Data buffer not initialized.");

		if (this.pointer < 0 || this.pointer >= this.length) return -1;

		int length = this.length - (int) this.pointer;
		if (length > b.length) length = b.length;

		// replaced inefficient code with an highly improved performance (up to ~60 %)
		this.buf.seek(this.pointer);
		this.buf.read(b);
		this.pointer += b.length;

		return length;
	}

	private static int b2i(byte b) {
		if (b >= 0) return b;
		return 256 + b;
	}

	private boolean checkPos(long pos) throws IOException {
		return ((pos >= 0) && (pos < length()));
	}

	/* returns the byte data */
	@Override
	public byte[] getPdfBuffer() {

		byte[] bytes = new byte[this.length];
		try {
			this.buf.seek(0);
			this.buf.read(bytes);
		}
		catch (IOException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		return bytes;
	}
}
