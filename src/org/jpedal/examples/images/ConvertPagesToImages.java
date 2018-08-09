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
 * ConvertPagesToImages.java
 * ---------------
 */

/**
 *
 * Example to convert PDF to Buffered images which can then be saved to Tiff, PNG or JPEG.
 * There is another example (org.jpedal.examples.images.ConvertPagesToHiResImages)
 * for producing higher res images of pages (but likely to be slower)
 *
 * It can run from jar directly using the command
 *
 * java -cp libraries_needed org/jpedal/examples/images/ConvertPagesToImages pdfFilepath inputValues
 *
 * where inputValues is 1-5 values
 * Value 1 is the file name or directory of PDF files to process (all doube quotes if it contains any spaces)
 * 4 optional values of:-
 * image type (jpeg,tiff,png)
 * scaling (100 = full size)
 * password for protected file (or null) can also be added
 * output path (must end with / or \\ character)
 *
 * There is a list of code examples to convert images at http://www.idrsolutions.com/how-to-convert-pdf-files-to-image
 *
 */
package org.jpedal.examples.images;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import org.jpedal.PdfDecoder;
import org.jpedal.color.ColorSpaces;
import org.jpedal.constants.PageInfo;
import org.jpedal.examples.viewer.Commands;
import org.jpedal.exception.PdfException;
import org.jpedal.fonts.FontMappings;
import org.jpedal.io.JAIHelper;
import org.jpedal.objects.PdfFileInformation;
import org.jpedal.utils.LogWriter;
import org.w3c.dom.Element;

import com.sun.imageio.plugins.jpeg.JPEGImageWriter;

public class ConvertPagesToImages {

	/**
	 * show if image transparent
	 */
	boolean isTransparent = false;

	/** output where we put files */
	private String user_dir = System.getProperty("user.dir");

	/** use 96 dpi as default so pages correct size (72 will be smaller) */
	private float pageScaling = 1.33f;

	/** flag to show if we print messages */
	public static boolean outputMessages = false;

	String output_dir = null;

	/** correct separator for OS */
	String separator = System.getProperty("file.separator");

	/** the decoder object which decodes the pdf and returns a data object */
	PdfDecoder decode_pdf = null;

	// type of image to save thumbnails
	private String format = "png";

	/** holding all creators that produce OCR pdf's */
	private String[] ocr = { "TeleForm" };

	/** flag to show if using images at highest quality -switch on with command line flag Dhires */
	private boolean useHiresImage = false;

	/**
	 * sample file which can be setup - substitute your own. If a directory is given, all the files in the directory will be processed
	 */
	private String test_file = "/mnt/shared/sample_pdfs/general/World Factbook.pdf";

	/** used as part of test to limit pages to first 10 */
	public static boolean isTest = false;

	// used for testing
	public static boolean orderReversed = false;

	/** scaling to use - default is 100 percent */
	private int scaling = 100;

	/** file password or null */
	private String password = null;

	// only used if between 0 and 1
	private float JPEGcompression = -1f;

	/**
	 * constructor to provide same functionality as main method
	 * 
	 */
	public ConvertPagesToImages(String[] args) {

		// read all values passed in by user and setup
		String file_name = setParams(args);

		// check file exists
		File pdf_file = new File(file_name);

		// if file exists, open and get number of pages
		if (!pdf_file.exists()) {
			System.out.println("File " + pdf_file + " not found");
			System.out.println("May need full path");

			return;
		}

		// System.out.println("testing file="+file_name);
		extraction(file_name, this.output_dir);
	}

	private void extraction(String file_name, String output_dir) {

		// get any user set dpi
		String hiresFlag = System.getProperty("org.jpedal.hires");
		if (Commands.hires || hiresFlag != null) {
			this.useHiresImage = true;
		}

		this.output_dir = output_dir;
		// check output dir has separator
		if (this.user_dir.endsWith(this.separator) == false) this.user_dir = this.user_dir + this.separator;

		/**
		 * if file name ends pdf, do the file otherwise do every pdf file in the directory. We already know file or directory exists so no need to
		 * check that, but we do need to check its a directory
		 */
		if (file_name.toLowerCase().endsWith(".pdf")) {

			if (!ConvertPagesToImages.isTest && output_dir == null) output_dir = this.user_dir + "thumbnails" + this.separator;

			decodeFile(file_name, output_dir);
		}
		else {

			/**
			 * get list of files and check directory
			 */

			String[] files = null;
			File inputFiles;

			/** make sure name ends with a deliminator for correct path later */
			if (!file_name.endsWith(this.separator)) file_name = file_name + this.separator;

			try {
				inputFiles = new File(file_name);

				if (!inputFiles.isDirectory()) {
					System.err.println(file_name + " is not a directory. Exiting program");

				}
				else files = inputFiles.list();
			}
			catch (Exception ee) {
				LogWriter.writeLog("Exception trying to access file " + ee.getMessage());

			}

			if (files != null) {
				/** now work through all pdf files */
				for (String file : files) {

					if (file.toLowerCase().endsWith(".pdf")) {
						if (outputMessages) System.out.println(file_name + file);

						decodeFile(file_name + file, output_dir);
					}
				}
			}
		}

		/** tell user */
		if (outputMessages) System.out.println("Thumbnails created");
	}

	/**
	 * routine to decode a file
	 */
	private void decodeFile(String file_name, String output_dir) {

		/**
		 * get just the name of the file without the path to use as a sub-directory
		 */

		String name = "demo"; // set a default just in case

		int pointer = file_name.lastIndexOf(this.separator);

		if (pointer == -1) pointer = file_name.lastIndexOf('/');

		if (pointer != -1) {
			name = file_name.substring(pointer + 1, file_name.length() - 4);
		}
		else
			if ((!ConvertPagesToImages.isTest) && (file_name.toLowerCase().endsWith(".pdf"))) {
				name = file_name.substring(0, file_name.length() - 4);
			}

		// create output dir for images
		if (output_dir == null) output_dir = this.user_dir + "thumbnails" + this.separator;

		// PdfDecoder returns a PdfException if there is a problem
		try {
			this.decode_pdf = new PdfDecoder(true);

			/** optional JAI code for faster rendering */
			// org.jpedal.external.ImageHandler myExampleImageHandler=new org.jpedal.examples.handlers.ExampleImageDrawOnScreenHandler();
			// decode_pdf.addExternalHandler(myExampleImageHandler, Options.ImageHandler);

			/**/

			/**
			 * font mappings
			 */
			if (!isTest) {

				// mappings for non-embedded fonts to use
				FontMappings.setFontReplacements();

			}

			// avoid breaking all my tests with code change!
			if (isTest) this.pageScaling = 1f;

			// true as we are rendering page
			this.decode_pdf.setExtractionMode(0, this.pageScaling);
			// don't bother to extract text and images

			/**
			 * open the file (and read metadata including pages in file)
			 */
			if (outputMessages) System.out.println("Opening file :" + file_name);

			if (this.password != null) this.decode_pdf.openPdfFile(file_name, this.password);
			else this.decode_pdf.openPdfFile(file_name);

		}
		catch (Exception e) {

			System.err.println("8.Exception " + e + " in pdf code in " + file_name);
		}

		/**
		 * extract data from pdf (if allowed).
		 */
		if (this.decode_pdf.isEncrypted() && !this.decode_pdf.isFileViewable()) {
			// exit with error if not test
			if (!isTest) throw new RuntimeException("Wrong password password used=>" + this.password + '<');
		}
		else
			if ((this.decode_pdf.isEncrypted() && (!this.decode_pdf.isPasswordSupplied())) && (!this.decode_pdf.isExtractionAllowed())) {
				throw new RuntimeException("Extraction not allowed");
			}
			else {

				// <link><a name="separation" />
				/**
				 * allow output to multiple images with different values on each
				 * 
				 * Note we REMOVE shapes as it is a new feature and we do not want to break existing functions
				 */
				String separation = System.getProperty("org.jpedal.separation");
				if (separation != null) {

					Object[] sepValues = new Object[] { 7, "", Boolean.FALSE }; // default of normal
					if (separation.equals("all")) {
						sepValues = new Object[] { PdfDecoder.RENDERIMAGES, "image_and_shapes", Boolean.FALSE,
								PdfDecoder.RENDERIMAGES + PdfDecoder.REMOVE_RENDERSHAPES, "image_without_shapes", Boolean.FALSE,
								PdfDecoder.RENDERTEXT, "text_and_shapes", Boolean.TRUE, 7, "all", Boolean.FALSE,
								PdfDecoder.RENDERTEXT + PdfDecoder.REMOVE_RENDERSHAPES, "text_without_shapes", Boolean.TRUE };
					}

					int sepCount = sepValues.length;
					for (int seps = 0; seps < sepCount; seps = seps + 3) {

						this.decode_pdf.setRenderMode((Integer) sepValues[seps]);
						extractPageAsImage(file_name, output_dir, name + '_' + sepValues[seps + 1], (Boolean) sepValues[seps + 2]); // boolean makes
																																	// last
																																	// transparent so
																																	// we can see
																																	// white text

					}

				}
				else // just get the page
				extractPageAsImage(file_name, output_dir, name, this.isTransparent);
			}

		/** close the pdf file */
		this.decode_pdf.closePdfFile();
	}

	private void extractPageAsImage(String file_name, String output_dir, String name, boolean isTransparent) {

		// create a directory if it doesn't exist
		File output_path = new File(output_dir);
		if (!output_path.exists()) output_path.mkdirs();

		String multiPageFlag = System.getProperty("org.jpedal.multipage_tiff");
		boolean isSingleOutputFile = multiPageFlag != null && multiPageFlag.toLowerCase().equals("true");

		// allow user to specify value
		String rawJPEGComp = System.getProperty("org.jpedal.compression_jpeg");
		if (rawJPEGComp != null) {
			try {
				this.JPEGcompression = Float.parseFloat(rawJPEGComp);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			if (this.JPEGcompression < 0 || this.JPEGcompression > 1) throw new RuntimeException(
					"Invalid value for JPEG compression - must be between 0 and 1");

		}

		String tiffFlag = System.getProperty("org.jpedal.compress_tiff");
		String jpgFlag = System.getProperty("org.jpedal.jpeg_dpi");
		boolean compressTiffs = tiffFlag != null && tiffFlag.toLowerCase().equals("true");

		if (JAIHelper.isJAIused()) JAIHelper.confirmJAIOnClasspath();

		// page range
		int start = 1, end = this.decode_pdf.getPageCount();

		// limit to 1st ten pages in testing
		if ((end > 10) && (isTest)) end = 10;

		/**
		 * extract data from pdf and then write out the pages as images
		 */

		if (outputMessages) System.out.println("Thumbnails will be in  " + output_dir);

		try {

			BufferedImage[] multiPages = new BufferedImage[1 + (end - start)];

			if (orderReversed) {
				for (int page = end; page >= start; page--)
					getPage(output_dir, name, isTransparent, isSingleOutputFile, rawJPEGComp, jpgFlag, compressTiffs, start, end, multiPages, page);
			}
			else {
				for (int page = start; page < end + 1; page++)
					getPage(output_dir, name, isTransparent, isSingleOutputFile, rawJPEGComp, jpgFlag, compressTiffs, start, end, multiPages, page);
			}
		}
		catch (Exception e) {

			this.decode_pdf.closePdfFile();
			throw new RuntimeException("Exception " + e.getMessage() + " with thumbnails on File=" + file_name);
		}
	}

	private void getPage(String output_dir, String name, boolean isTransparent, boolean isSingleOutputFile, String rawJPEGComp, String jpgFlag,
			boolean compressTiffs, int start, int end, BufferedImage[] multiPages, int page) throws PdfException, IOException, FileNotFoundException {
		{ // read pages

			if (outputMessages) System.out.println("Page " + page);

			/**
			 * create a name with zeros for if more than 9 pages appears in correct order
			 */
			String pageAsString = String.valueOf(page);
			String maxPageSize = String.valueOf(end);
			int padding = maxPageSize.length() - pageAsString.length();
			for (int ii = 0; ii < padding; ii++)
				pageAsString = '0' + pageAsString;

			String image_name;
			if (isSingleOutputFile) image_name = name;
			else image_name = name + "_page_" + pageAsString;

			/**
			 * get PRODUCER and if OCR disable text printing
			 */
			PdfFileInformation currentFileInformation = this.decode_pdf.getFileInformationData();

			String[] values = currentFileInformation.getFieldValues();
			String[] fields = PdfFileInformation.getFieldNames();

			for (int i = 0; i < fields.length; i++) {

				if (fields[i].equals("Creator")) {

					for (String anOcr : this.ocr) {

						if (values[i].equals(anOcr)) {

							this.decode_pdf.setRenderMode(PdfDecoder.RENDERIMAGES);

						}
					}
				}
			}

			/**
			 * get the current page as a BufferedImage
			 */
			BufferedImage image_to_save;
			if (!isTransparent) image_to_save = this.decode_pdf.getPageAsImage(page);
			else { // use this if you want a transparent image
				image_to_save = this.decode_pdf.getPageAsTransparentImage(page);

				// <link><a name="jpegBlogArticle" />
				// java adds odd tint if you save this as JPEG which does not have transparency
				// so put as RGB on white background
				// (or save as PNG or TIFF which has transparency)
				// or just call decode_pdf.getPageAsImage(page)
				if (image_to_save != null && this.format.toLowerCase().startsWith("jp")) {

					BufferedImage rawVersion = image_to_save;

					int w = rawVersion.getWidth(), h = rawVersion.getHeight();
					// blank canvas
					image_to_save = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

					//
					Graphics2D g2 = image_to_save.createGraphics();
					// white background
					g2.setPaint(Color.WHITE);
					g2.fillRect(0, 0, w, h);
					// paint on image
					g2.drawImage(rawVersion, 0, 0, null);
				}
			}

			// <link><a name="grayscale" />
			// if just gray we can reduce memory usage by converting image to Grayscale

			/**
			 * see what Colorspaces used and reduce image if appropriate (only does Gray at present)
			 * 
			 * null if JPedal unsure
			 */
			Iterator colorspacesUsed = this.decode_pdf.getPageInfo(PageInfo.COLORSPACES);

			int nextID;
			boolean isGrayOnly = colorspacesUsed != null; // assume true and disprove
			while (colorspacesUsed != null && colorspacesUsed.hasNext()) {
				nextID = (Integer) (colorspacesUsed.next());

				if (nextID != ColorSpaces.DeviceGray && nextID != ColorSpaces.CalGray) isGrayOnly = false;
			}

			// draw onto GRAY image to reduce colour depth
			// (converts ARGB to gray)
			if (isGrayOnly) {
				BufferedImage image_to_save2 = new BufferedImage(image_to_save.getWidth(), image_to_save.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
				image_to_save2.getGraphics().drawImage(image_to_save, 0, 0, null);
				image_to_save = image_to_save2;
			}

			// put image in array if multi-images
			if (isSingleOutputFile) multiPages[page - start] = image_to_save;

			if (image_to_save == null) {
				if (outputMessages && !isTest) System.out.println("No image generated - are you using client mode?");
			}
			else {

				/**
				 * BufferedImage does not support any dpi concept. A higher dpi can be created using JAI to convert to a higher dpi image
				 */

				// shrink the page to 50% with graphics2D transformation
				// - add your own parameters as needed
				// you may want to replace null with a hints object if you
				// want to fine tune quality.

				/**
				 * example 1 biliniear scaling AffineTransform scale = new AffineTransform(); scale.scale(.5, .5); //50% as a decimal
				 * AffineTransformOp scalingOp =new AffineTransformOp(scale, null); image_to_save =scalingOp.filter(image_to_save, null);
				 */

				/**
				 * example 2 bicubic scaling - better quality but slower to preserve aspect ratio set newWidth or newHeight to -1
				 */

				/** allow user to specify maximum dimension for thumbnail */
				String maxDimensionAsString = System.getProperty("maxDimension");
				int maxDimension = -1;

				if (maxDimensionAsString != null) maxDimension = Integer.parseInt(maxDimensionAsString);

				if (this.scaling != 100 || maxDimension != -1) {
					int newWidth = image_to_save.getWidth() * this.scaling / 100;
					int newHeight = image_to_save.getHeight() * this.scaling / 100;

					Image scaledImage;
					if (maxDimension != -1 && (newWidth > maxDimension || newHeight > maxDimension)) {
						if (newWidth > newHeight) {
							newWidth = maxDimension;
							scaledImage = image_to_save.getScaledInstance(newWidth, -1, Image.SCALE_SMOOTH);
						}
						else {
							newHeight = maxDimension;
							scaledImage = image_to_save.getScaledInstance(-1, newHeight, Image.SCALE_SMOOTH);
						}
					}
					else {
						scaledImage = image_to_save.getScaledInstance(newWidth, -1, Image.SCALE_SMOOTH);
					}

					if (this.format.toLowerCase().startsWith("jp")) image_to_save = new BufferedImage(scaledImage.getWidth(null),
							scaledImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
					else image_to_save = new BufferedImage(scaledImage.getWidth(null), scaledImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);

					Graphics2D g2 = image_to_save.createGraphics();

					g2.drawImage(scaledImage, 0, 0, null);
				}

				String imageFormat = System.getProperty("org.jpedal.imageType");
				if (imageFormat != null) {
					if (isNumber(imageFormat)) {
						int iFormat = Integer.parseInt(imageFormat);
						if (iFormat > -1 && iFormat < 14) {
							BufferedImage tempImage = new BufferedImage(image_to_save.getWidth(), image_to_save.getHeight(), iFormat);
							Graphics2D g = tempImage.createGraphics();
							g.drawImage(image_to_save, null, null);

							image_to_save = tempImage;
						}
						else {
							System.err
									.println("Image Type is not valid. Value should be a digit between 0 - 13 based on the BufferedImage TYPE variables.");
						}
					}
					else {
						System.err
								.println("Image Type provided is not an Integer. Value should be a digit between 0 - 13 based on the BufferedImage TYPE variables.");
					}
				}

				if (JAIHelper.isJAIused() && this.format.startsWith("tif")) {

					com.sun.media.jai.codec.TIFFEncodeParam params = new com.sun.media.jai.codec.TIFFEncodeParam();

					if (compressTiffs) params.setCompression(com.sun.media.jai.codec.TIFFEncodeParam.COMPRESSION_PACKBITS);

					if (!isSingleOutputFile) {
						BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(output_dir + pageAsString + image_name + ".tif"));

						javax.media.jai.JAI.create("encode", image_to_save, os, "TIFF", params);
					}
					else
						if (isSingleOutputFile && page == end) {
							OutputStream out = new BufferedOutputStream(new FileOutputStream(output_dir + image_name + ".tif"));
							com.sun.media.jai.codec.ImageEncoder encoder = com.sun.media.jai.codec.ImageCodec.createImageEncoder("TIFF", out, params);
							java.util.List<BufferedImage> vector = new ArrayList<BufferedImage>();
							vector.addAll(Arrays.asList(multiPages).subList(1, multiPages.length));

							params.setExtraImages(vector.iterator());

							encoder.encode(multiPages[0]);
							out.close();
						}

					// commented out as bug -does not work as it should
					// }else if(isSingleOutputFile){
					// non-JAI

				}
				else
					if ((jpgFlag != null || rawJPEGComp != null) && this.format.startsWith("jp") && JAIHelper.isJAIused()) {

						saveAsJPEG(jpgFlag, image_to_save, this.JPEGcompression, new BufferedOutputStream(new FileOutputStream(output_dir
								+ pageAsString + image_name + '.' + this.format)));

					}
					else {

						// save image
						this.decode_pdf.getObjectStore().saveStoredImage(output_dir + pageAsString + image_name, image_to_save, true, false,
								this.format);
					}
				// if you just want to save the image, use something like
				// javax.imageio.ImageIO.write((java.awt.image.RenderedImage)image_to_save,"png",new java.io.FileOutputStream(output_dir + page +
				// image_name+".png"));

			}

			// flush images in case we do more than 1 page so only contains
			// images from current page
			this.decode_pdf.flushObjectValues(true);
			// flush any text data read

		}
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * main routine which checks for any files passed and runs the demo
	 * 
	 * Full details at http://www.jpedal.org/support_egEPT.php
	 */
	public static void main(String[] args) {

		System.out.println("Simple demo to extract images from a page");

		// check values first and exit with info if too many
		int count = args.length;
		boolean failed = count > 4 || count == 0;
		if (failed) {

			if (count > 0) {
				System.out.println("too many arguments entered - run with no values to see defaults");

				String arguments = "";
				for (String arg : args)
					arguments = arguments + arg + '\n';
				System.out.println("you entered:\n" + arguments + "as the arguments");
			}

			showCommandLineValues();
		}

		new ConvertPagesToImages(args);
	}

	private String setParams(String[] args) {
		// set to default
		String file_name = this.test_file;

		// check user has passed us a filename and use default if none
		int len = args.length;
		if (len == 0) {
			showCommandLineValues();
		}
		else
			if (len == 1) {
				file_name = args[0];
			}
			else
				if (len < 6) {

					// input
					file_name = args[0];

					for (int j = 1; j < args.length; j++) {
						String value = args[j];
						boolean isNumber = isNumber(value);

						if (isNumber) {
							try {
								this.scaling = Integer.parseInt(value);
							}
							catch (Exception e) {
								throw new RuntimeException(value + " is not an integer");
							}
						}
						else {
							String in = value.toLowerCase();
							if ((in.equals("jpg")) || (in.equals("jpeg"))) this.format = "jpg";
							else
								if (in.equals("tif") || in.equals("tiff")) this.format = "tif";
								else
									if (in.equals("png")) this.format = "png";
									else {

										// assume password if no / or \
										if (value.endsWith("/") || value.endsWith("\\")) this.output_dir = value;
										else this.password = value;

										// failed=true;
										// System.out.println("value args not recognised as valid parameter.");
										// System.out.println("please enter \"jpg\", \"jpeg\", \"tif\", \"tiff\" or \"png\".");
									}
						}
					}
				}
		return file_name;
	}

	static void showCommandLineValues() {
		System.out.println("Example can take 1-5 parameters");
		System.out.println("Value 1 is the file name or directory of PDF files to process");
		System.out
				.println("4 optional values of:-\nimage type (jpeg,tiff,png), \nscaling (100 = full size), \npassword for protected file (or null) can also be added ,\noutput path (must end with / or \\ character)");
		System.exit(0);
	}

	/** test to see if string or number */
	private static boolean isNumber(String value) {

		// assume true and see if proved wrong
		boolean isNumber = true;

		int charCount = value.length();
		for (int i = 0; i < charCount; i++) {
			char c = value.charAt(i);
			if ((c < '0') | (c > '9')) {
				isNumber = false;
				i = charCount;
			}
		}

		return isNumber;
	}

	/**
	 * @return Returns the output_dir.
	 */
	public String getOutputDir() {
		return this.output_dir;
	}

	// <link><a name="savejpeg" />
	private static void saveAsJPEG(String jpgFlag, BufferedImage image_to_save, float JPEGcompression, BufferedOutputStream fos) throws IOException {

		// useful documentation at http://docs.oracle.com/javase/7/docs/api/javax/imageio/metadata/doc-files/jpeg_metadata.html
		// useful example program at http://johnbokma.com/java/obtaining-image-metadata.html to output JPEG data

		// old jpeg class
		// com.sun.image.codec.jpeg.JPEGImageEncoder jpegEncoder = com.sun.image.codec.jpeg.JPEGCodec.createJPEGEncoder(fos);
		// com.sun.image.codec.jpeg.JPEGEncodeParam jpegEncodeParam = jpegEncoder.getDefaultJPEGEncodeParam(image_to_save);

		// Image writer
		JPEGImageWriter imageWriter = (JPEGImageWriter) ImageIO.getImageWritersBySuffix("jpeg").next();
		ImageOutputStream ios = ImageIO.createImageOutputStream(fos);
		imageWriter.setOutput(ios);

		// and metadata
		IIOMetadata imageMetaData = imageWriter.getDefaultImageMetadata(new ImageTypeSpecifier(image_to_save), null);

		if (jpgFlag != null) {

			int dpi = 96;

			try {
				dpi = Integer.parseInt(jpgFlag);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			// old metadata
			// jpegEncodeParam.setDensityUnit(com.sun.image.codec.jpeg.JPEGEncodeParam.DENSITY_UNIT_DOTS_INCH);
			// jpegEncodeParam.setXDensity(dpi);
			// jpegEncodeParam.setYDensity(dpi);

			// new metadata
			Element tree = (Element) imageMetaData.getAsTree("javax_imageio_jpeg_image_1.0");
			Element jfif = (Element) tree.getElementsByTagName("app0JFIF").item(0);
			jfif.setAttribute("Xdensity", Integer.toString(dpi));
			jfif.setAttribute("Ydensity", Integer.toString(dpi));

		}

		if (JPEGcompression >= 0 && JPEGcompression <= 1f) {

			// old compression
			// jpegEncodeParam.setQuality(JPEGcompression,false);

			// new Compression
			JPEGImageWriteParam jpegParams = (JPEGImageWriteParam) imageWriter.getDefaultWriteParam();
			jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			jpegParams.setCompressionQuality(JPEGcompression);

		}

		// old write and clean
		// jpegEncoder.encode(image_to_save, jpegEncodeParam);

		// new Write and clean up
		imageWriter.write(imageMetaData, new IIOImage(image_to_save, null, null), null);
		ios.close();
		imageWriter.dispose();
	}

}
