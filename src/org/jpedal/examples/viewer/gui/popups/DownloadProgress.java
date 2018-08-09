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
 * DownloadProgress.java
 * ---------------
 */

package org.jpedal.examples.viewer.gui.popups;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.io.ObjectStore;
import org.jpedal.utils.LogWriter;

public class DownloadProgress {
	// Load file from URL into file then open file
	File tempURLFile;

	boolean isDownloading = true;

	int progress = 0;

	SwingGUI gui;

	private String pdfUrl;

	public DownloadProgress(SwingGUI gui, String pdfUrl) {

		this.gui = gui;
		this.pdfUrl = pdfUrl;
	}

	public void startDownload() {

		URL url;
		InputStream is;

		try {
			int fileLength;
			int fileLengthPercent = 0;

			this.progress = 0;

			String str = "file.pdf";
			if (this.pdfUrl.startsWith("jar:/")) {
				is = this.getClass().getResourceAsStream(this.pdfUrl.substring(4));
			}
			else {
				url = new URL(this.pdfUrl);

				is = url.openStream();

				str = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
				fileLength = url.openConnection().getContentLength();
				fileLengthPercent = fileLength / 100;
			}
			final String filename = str;

			this.tempURLFile = ObjectStore.createTempFile(filename);

			FileOutputStream fos = new FileOutputStream(this.tempURLFile);

			// Download buffer
			byte[] buffer = new byte[4096];

			// Download the PDF document
			int read;
			int current = 0;

			while ((read = is.read(buffer)) != -1) {
				current = current + read;
				this.progress = current / fileLengthPercent;
				fos.write(buffer, 0, read);
			}
			fos.flush();

			// Close streams
			is.close();
			fos.close();
			this.progress = 100;

		}
		catch (Exception e) {
			LogWriter.writeLog("[PDF] Exception " + e + " opening URL " + this.pdfUrl);
			e.printStackTrace();
			this.progress = 100;
		}

		this.isDownloading = false;
	}

	public File getFile() {
		return this.tempURLFile;
	}

	public boolean isDownloading() {
		return this.isDownloading;
	}

	public int getProgress() {
		return this.progress;
	}
}
