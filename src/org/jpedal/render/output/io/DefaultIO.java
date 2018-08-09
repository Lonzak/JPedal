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
 * DefaultIO.java
 * ---------------
 */
package org.jpedal.render.output.io;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jpedal.io.JAIHelper;
import org.jpedal.io.ObjectStore;
import org.jpedal.utils.LogWriter;

public class DefaultIO implements CustomIO {

	private BufferedWriter output = null;

	Map imagesWritten = new HashMap(); // @here

	@Override
	public void writeFont(String path, byte[] rawFontData) {

		try {

			BufferedOutputStream fontOutput = new BufferedOutputStream(new FileOutputStream(path));
			fontOutput.write(rawFontData);
			fontOutput.flush();
			fontOutput.close();

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	@Override
	public void writeJS(String rootDir, InputStream url) throws IOException {

		// make sure js Dir exists
		String cssPath = rootDir + "/js";
		File cssDir = new File(cssPath);
		if (!cssDir.exists()) {
			cssDir.mkdirs();
		}

		BufferedInputStream stylesheet = new BufferedInputStream(url);

		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(rootDir + "/js/aform.js"));
		ObjectStore.copy(stylesheet, bos);
		bos.flush();
		bos.close();

		stylesheet.close();
	}

	@Override
	public void writeCSS(String rootDir, String fileName, StringBuilder css) {

		// make sure css Dir exists
		String cssPath = rootDir + fileName + '/';
		File cssDir = new File(cssPath);
		if (!cssDir.exists()) {
			cssDir.mkdirs();
		}

		try {
			// PrintWriter CSSOutput = new PrintWriter(new FileOutputStream(cssPath + "styles.css"));
			BufferedOutputStream CSSOutput = new BufferedOutputStream(new FileOutputStream(cssPath + "styles.css"));

			// css header

			CSSOutput.write(css.toString().getBytes());

			CSSOutput.flush();
			CSSOutput.close();

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	@Override
	public boolean isOutputOpen() {
		return this.output != null;
	}

	@Override
	public void setupOutput(String path, boolean append, String encodingUsed) throws FileNotFoundException, UnsupportedEncodingException {

		this.output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, append), encodingUsed));
	}

	@Override
	public void flush() {

		try {
			this.output.flush();
			this.output.close();

			this.imagesWritten.clear(); // @here

			this.output = null;
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	@Override
	public void writeString(String str) {

		try {
			this.output.write(str);
			this.output.write('\n');
			this.output.flush();
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	// @here - lots in routine
	@Override
	public String writeImage(String rootDir, String path, BufferedImage image) {

		String file = path + getImageTypeUsed();
		String fullPath = rootDir + file;

		/**
		 * reject repeat images (assume identical name is same) root will include pageNumber as X1 on page 1 and 2 usually different
		 */
		// if(!imagesWritten.containsKey(fullPath)){

		// imagesWritten.put(fullPath,"x");

		try {
			if (!JAIHelper.isJAIused()) {
				ImageIO.write(image, "PNG", new File(fullPath));
			}
			else {
				JAIHelper.confirmJAIOnClasspath();
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(fullPath)));
				com.sun.media.jai.codec.ImageEncoder encoder = com.sun.media.jai.codec.ImageCodec.createImageEncoder("PNG", bos, null);
				encoder.encode(image);
				bos.close();
			}
		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		// }

		return file;
	}

	@Override
	public String getImageTypeUsed() {
		return ".png";
	}
}
