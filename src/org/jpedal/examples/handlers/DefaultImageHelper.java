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
 * DefaultImageIO.java
 * ---------------
 */
package org.jpedal.examples.handlers;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.jpedal.color.GenericColorSpace;
import org.jpedal.external.ImageHelper;
import org.jpedal.io.JAIHelper;
import org.jpedal.utils.LogWriter;

/**
 * abstract image saving and reading so we can move between different libraries if ImageIO not sufficient
 **/
public class DefaultImageHelper implements ImageHelper {

	public DefaultImageHelper() {
		ImageIO.setUseCache(false);
	}

	@Override
	public void write(BufferedImage image, String type, String file_name) throws Exception {

		if (!type.equals("jpg") && JAIHelper.isJAIused()) { // we do not use JAI for JPEG at present

			JAIHelper.confirmJAIOnClasspath();
			javax.media.jai.JAI.create("filestore", image, file_name, type);

		}
		else {

			// indexed colorspace optimized by Sun so quicker to convert and then output
			if (GenericColorSpace.fasterPNG) {
				BufferedImage indexedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);
				Graphics2D g = indexedImage.createGraphics();
				g.drawImage(image, 0, 0, null);
				image = indexedImage;
			}

			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(file_name)));
			ImageIO.write(image, type, bos);
			bos.flush();
			bos.close();
		}
	}

	@Override
	public BufferedImage read(String file_name) {

		boolean isLoaded = false;

		BufferedImage image = null;

		if (JAIHelper.isJAIused()) {
			try {

				// Load the source image from a file.
				image = javax.media.jai.JAI.create("fileload", file_name).getAsBufferedImage();

				isLoaded = true;

			}
			catch (Exception e) {

				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());

				isLoaded = false;
			}
			catch (Error err) {

				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Error: " + err.getMessage());

				throw new RuntimeException("Error " + err + " loading " + file_name + " with JAI");

			}
		}

		if (!isLoaded) {

			try {
				image = ImageIO.read(new File(file_name));

				// BufferedInputStream in = new BufferedInputStream(new FileInputStream(file_name));
				// image = ImageIO.read(in);
				// in.close();
			}
			catch (IOException e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
			catch (Error err) {

				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Error: " + err.getMessage());
				throw new RuntimeException("Error " + err + " loading " + file_name + " with ImageIO");

			}
		}

		return image;
	}

	@Override
	public Raster readRasterFromJPeg(byte[] data) throws IOException {

		Raster ras = null;
		ImageReader iir = null;
		ImageInputStream iin = null;

		ByteArrayInputStream in = new ByteArrayInputStream(data);

		// suggestion from Carol
		try {
			Iterator iterator = ImageIO.getImageReadersByFormatName("JPEG");

			while (iterator.hasNext()) {

				Object o = iterator.next();
				iir = (ImageReader) o;
				if (iir.canReadRaster()) break;
			}

			ImageIO.setUseCache(false);

			iin = ImageIO.createImageInputStream((in));
			iir.setInput(iin, true); // new MemoryCacheImageInputStream(in));

			ras = iir.readRaster(0, null);

		}
		catch (Exception e) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Unable to find JAI jars on classpath");

		}
		finally {
			if (in != null) in.close();

			if (iin != null) {
				iin.flush();
				iin.close();
			}

			if (iir != null) iir.dispose();
		}
		return ras;
	}

	@Override
	public BufferedImage read(byte[] data) throws Exception {

		//
		ByteArrayInputStream bis = new ByteArrayInputStream(data);

		ImageIO.setUseCache(false);

		return ImageIO.read(bis);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DefaultImageHelper []");
		return builder.toString();
	}	
}
