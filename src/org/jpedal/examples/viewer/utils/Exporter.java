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
 * Exporter.java
 * ---------------
 */
package org.jpedal.examples.viewer.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ProgressMonitor;

import org.jpedal.PdfDecoder;
import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.examples.viewer.gui.popups.SaveImage;
import org.jpedal.examples.viewer.gui.popups.SaveText;
import org.jpedal.exception.PdfException;
import org.jpedal.exception.PdfSecurityException;
import org.jpedal.grouping.PdfGroupingAlgorithms;
import org.jpedal.gui.GUIFactory;
import org.jpedal.io.ColorSpaceConvertor;
import org.jpedal.io.JAIHelper;
import org.jpedal.objects.PdfImageData;
import org.jpedal.objects.PdfPageData;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;
import org.jpedal.utils.Strip;
import org.jpedal.utils.SwingWorker;

/** provide save functions for Viewer to write out text, images, etc */
public class Exporter {

	final public static int RECTANGLE = 1;
	final public static int WORDLIST = 2;
	final public static int TABLE = 3;

	/** file separator used */
	private final String separator = System.getProperty("file.separator");

	private String fileName = "";

	private GUIFactory currentGUI;

	private PdfDecoder dPDF;

	private String selectedFile;

	public Exporter(SwingGUI currentGUI, String selectedFile, PdfDecoder decode_pdf) {
		String fileName = new File(selectedFile).getName();
		if (fileName.lastIndexOf('.') != -1) fileName = fileName.substring(0, fileName.lastIndexOf('.'));

		StringBuilder fileNameBuffer = new StringBuilder(fileName);
		int index;
		while ((index = fileNameBuffer.toString().indexOf("%20")) != -1) {
			fileNameBuffer.replace(index, index + 3, " ");
		}

		this.fileName = fileNameBuffer.toString();
		this.currentGUI = currentGUI;
		this.selectedFile = selectedFile;
		this.dPDF = decode_pdf;
	}

	/**
	 * save image - different versions have different bugs for file formats so we use best for each image type
	 * 
	 * @param image_to_save
	 */
	private static void saveImage(BufferedImage image_to_save, String fileName, String prefix) {

		if (JAIHelper.isJAIused()) JAIHelper.confirmJAIOnClasspath();

		if (prefix.contains("tif") && JAIHelper.isJAIused()) {

			try {
				FileOutputStream os = new FileOutputStream(fileName);
				javax.media.jai.JAI.create("encode", image_to_save, os, "TIFF", null);
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}

		}
		else { // default
			try {

				ImageIO.write(image_to_save, prefix, new File(fileName));

			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * routine to write out clipped PDFs
	 */
	private void decodeHires(int start, int end, String imageType, String output_dir) {

		PdfDecoder decode_pdf = null;

		String target = "";

		// PdfDecoder returns a PdfException if there is a problem
		try {

			decode_pdf = new PdfDecoder(false);
			decode_pdf.setExtractionMode(PdfDecoder.FINALIMAGES + PdfDecoder.CLIPPEDIMAGES, 1);

			/** open the file (and read metadata including pages in file) */
			decode_pdf.openPdfFile(this.selectedFile);

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		/**
		 * extract data from pdf (if allowed).
		 */
		if ((decode_pdf.isEncrypted() && (!decode_pdf.isPasswordSupplied())) && (!decode_pdf.isExtractionAllowed())) return;

		ProgressMonitor status = new ProgressMonitor(this.currentGUI.getFrame(), Messages.getMessage("PdfViewerMessage.ExtractImages"), "", start,
				end);

		try {
			int count = 0;
			boolean yesToAll = false;
			for (int page = start; page < end + 1; page++) { // read pages
				if (status.isCanceled()) {
					this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerError.UserStoppedExport") + count + ' '
							+ Messages.getMessage("PdfViewerError.ReportNumberOfImagesExported"));
					return;
				}
				// decode the page
				decode_pdf.decodePage(page);

				// get the PdfImages object which now holds the images.
				// binary data is stored in a temp directory and we hold the
				// image name and other info in this object
				PdfImageData pdf_images = decode_pdf.getPdfImageData();

				// image count (note image 1 is item 0, so any loop runs 0 to count-1)
				int image_count = pdf_images.getImageCount();

				if (image_count > 0) {
					target = output_dir + page + this.separator;
					File targetExists = new File(target);
					if (!targetExists.exists()) targetExists.mkdir();
				}

				// work through and save each image
				for (int i = 0; i < image_count; i++) {

					String image_name = pdf_images.getImageName(i);
					BufferedImage image_to_save;

					float x1 = pdf_images.getImageXCoord(i);
					float y1 = pdf_images.getImageYCoord(i);
					float w = pdf_images.getImageWidth(i);
					float h = pdf_images.getImageHeight(i);

					try {

						image_to_save = decode_pdf.getObjectStore().loadStoredImage("CLIP_" + image_name);

						// save image

						if (image_to_save != null) {

							// remove transparency on jpeg
							if (imageType.toLowerCase().startsWith("jp")) image_to_save = ColorSpaceConvertor.convertToRGB(image_to_save);

							File fileToSave = new File(target + image_name + '.' + imageType);
							if (fileToSave.exists() && !yesToAll) {
								int n = this.currentGUI.showOverwriteDialog(fileToSave.getAbsolutePath(), true);

								if (n == 0) {
									// clicked yes so just carry on for this once
								}
								else
									if (n == 1) {
										// clicked yes to all, so set flag
										yesToAll = true;
									}
									else
										if (n == 2) {
											// clicked no, so loop round again
											status.setProgress(page);
											continue;
										}
										else {

											this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerError.UserStoppedExport") + count + ' '
													+ Messages.getMessage("PdfViewerError.ReportNumberOfImagesExported"));

											status.close();
											return;
										}
							}

							saveImage(image_to_save, target + image_name + '.' + imageType, imageType);
							count++;
						}

						// save an xml file with details
						/**
						 * output the data
						 */
						// LogWriter.writeLog( "Writing out "+(outputName + ".xml"));
						OutputStreamWriter output_stream = new OutputStreamWriter(new FileOutputStream(target + image_name + ".xml"), "UTF-8");

						output_stream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
						output_stream.write("<!-- Pixel Location of image x1,y1,x2,y2\n");
						output_stream.write("(x1,y1 is top left corner)\n");
						output_stream.write("(origin is bottom left corner)  -->\n");
						output_stream.write("\n\n<META>\n");
						output_stream.write("<PAGELOCATION x1=\"" + x1 + "\" " + "y1=\"" + (y1 + h) + "\" " + "x2=\"" + (x1 + w) + "\" " + "y2=\""
								+ (y1) + "\" />\n");
						output_stream.write("<FILE>" + this.fileName + "</FILE>\n");
						output_stream.write("</META>\n");
						output_stream.close();
					}
					catch (Exception ee) {
						ee.printStackTrace();
						LogWriter.writeLog("Exception " + ee + " in extracting images");
					}
				}

				// flush images in case we do more than 1 page so only contains
				// images from current page
				decode_pdf.flushObjectValues(true);

				status.setProgress(page + 1);

			}
			status.close();

			this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerMessage.ImagesSavedTo") + ' ' + output_dir);

		}
		catch (Exception e) {
			decode_pdf.closePdfFile();
			LogWriter.writeLog("Exception " + e.getMessage());
		}

		/** close the pdf file */
		decode_pdf.closePdfFile();
	}

	public void extractImagesOnPages(SaveImage current_selection) {
		final int startPage = current_selection.getStartPage();
		final int endPage = current_selection.getEndPage();

		if (startPage < 1 || endPage < 1) return;

		final int type = current_selection.getImageType();
		// get user choice
		final String format = current_selection.getPrefix();
		final String output_dir = current_selection.getRootDir() + this.separator + this.fileName + this.separator + "images" + this.separator;

		File testDirExists = new File(output_dir);
		if (!testDirExists.exists()) testDirExists.mkdirs();

		final SwingWorker worker = new SwingWorker() {
			@Override
			public Object construct() {
				// do the save

				switch (type) {
					case PdfDecoder.CLIPPEDIMAGES:
						decodeHires(startPage, endPage, format, output_dir);
						break;
					case PdfDecoder.RAWIMAGES:
						decodeImages(startPage, endPage, format, output_dir, false);
						break;
					case PdfDecoder.FINALIMAGES:
						decodeImages(startPage, endPage, format, output_dir, true);
						break;
					default:
						System.out.println("Unknown setting");
						break;
				}

				return null;
			}
		};

		worker.start();
	}

	/**
	 * routine to write out images in PDFs
	 */
	private void decodeImages(int start, int end, String prefix, String output_dir, boolean downsampled) {

		PdfDecoder decode_pdf = null;

		// PdfDecoder returns a PdfException if there is a problem
		try {

			decode_pdf = new PdfDecoder(false);

			decode_pdf.setExtractionMode(PdfDecoder.RAWIMAGES + PdfDecoder.FINALIMAGES, 1);
			/** open the file (and read metadata including pages in file) */
			decode_pdf.openPdfFile(this.selectedFile);

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		/**
		 * extract data from pdf (if allowed).
		 */
		if ((decode_pdf.isEncrypted() && (!decode_pdf.isPasswordSupplied())) && (!decode_pdf.isExtractionAllowed())) return;

		ProgressMonitor status = new ProgressMonitor(this.currentGUI.getFrame(), Messages.getMessage("PdfViewerMessage.ExtractImages"), "", start,
				end);

		try {
			int count = 0;
			boolean yesToAll = false;
			for (int page = start; page < end + 1; page++) { // read pages
				if (status.isCanceled()) {
					this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerError.UserStoppedExport") + count
							+ Messages.getMessage("PdfViewerError.ReportNumberOfImagesExported"));
					return;
				}
				// decode the page
				decode_pdf.decodePage(page);

				// get the PdfImages object which now holds the images.
				// binary data is stored in a temp directory and we hold the
				// image name and other info in this object
				PdfImageData pdf_images = decode_pdf.getPdfImageData();

				// image count (note image 1 is item 0, so any loop runs 0 to count-1)
				int image_count = pdf_images.getImageCount();

				String target = output_dir + this.separator;
				if (downsampled) target = target + "downsampled" + this.separator + page + this.separator;
				else target = target + "normal" + this.separator + page + this.separator;

				// tell user
				if (image_count > 0) {

					// create a directory for page
					File page_path = new File(target);
					if (page_path.exists() == false) page_path.mkdirs();

					// do it again as some OS struggle with creating nested dirs
					page_path = new File(target);
					if (page_path.exists() == false) page_path.mkdirs();

				}

				// work through and save each image
				for (int i = 0; i < image_count; i++) {
					String image_name = pdf_images.getImageName(i);
					BufferedImage image_to_save;

					try {
						if (downsampled) {
							// load processed version of image (converted to rgb)
							image_to_save = decode_pdf.getObjectStore().loadStoredImage(image_name);
							if (prefix.toLowerCase().startsWith("jp")) {
								image_to_save = ColorSpaceConvertor.convertToRGB(image_to_save);

							}
						}
						else {
							// get raw version of image (R prefix for raw image)
							image_to_save = decode_pdf.getObjectStore().loadStoredImage(image_name);
							if (prefix.toLowerCase().startsWith("jp")) {
								image_to_save = ColorSpaceConvertor.convertToRGB(image_to_save);
							}
						}

						File fileToSave = new File(target + image_name + '.' + prefix);
						if (fileToSave.exists() && !yesToAll) {
							int n = this.currentGUI.showOverwriteDialog(fileToSave.getAbsolutePath(), true);

							if (n == 0) {
								// clicked yes so just carry on for this once
							}
							else
								if (n == 1) {
									// clicked yes to all, so set flag
									yesToAll = true;
								}
								else
									if (n == 2) {
										// clicked no, so loop round again
										status.setProgress(page);
										continue;
									}
									else {

										this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerError.UserStoppedExport") + count + ' '
												+ Messages.getMessage("PdfViewerError.ReportNumberOfImagesExported"));

										status.close();
										return;
									}
						}

						// save image
						saveImage(image_to_save, target + image_name + '.' + prefix, prefix);
						count++;
					}

					catch (Exception ee) {
						System.err.println("Exception " + ee + " in extracting images");
					}
				}

				// flush images in case we do more than 1 page so only contains
				// images from current page
				decode_pdf.flushObjectValues(true);

				status.setProgress(page + 1);
			}

			this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerMessage.ImagesSavedTo") + ' ' + output_dir);

			status.close();
		}
		catch (Exception e) {
			decode_pdf.closePdfFile();
			LogWriter.writeLog("Exception " + e.getMessage());
		}

		/** close the pdf file */
		decode_pdf.closePdfFile();
	}

	public void extractTextOnPages(SaveText current_selection) {
		// get user choice
		final int startPage = current_selection.getStartPage();
		final int endPage = current_selection.getEndPage();

		if (startPage < 1 || endPage < 1) return;

		final int type = current_selection.getTextType();
		final boolean useXMLExtraction = current_selection.isXMLExtaction();

		final String output_dir = current_selection.getRootDir() + this.separator + this.fileName + this.separator + "text" + this.separator;

		File testDirExists = new File(output_dir);
		if (!testDirExists.exists()) testDirExists.mkdirs();

		final SwingWorker worker = new SwingWorker() {
			@Override
			public Object construct() {
				// do the save

				switch (type) {
					case Exporter.RECTANGLE:
						decodeTextRectangle(startPage, endPage, output_dir, useXMLExtraction);
						break;
					case Exporter.WORDLIST:
						decodeTextWordlist(startPage, endPage, output_dir, useXMLExtraction);
						break;
					case Exporter.TABLE:
						decodeTextTable(startPage, endPage, output_dir, useXMLExtraction);

						break;
					default:
						System.out.println("Unknown setting");
						break;
				}

				return null;
			}
		};

		worker.start();
	}

	private void decodeTextTable(int startPage, int endPage, String output_dir, boolean useXMLExtraction) {

		PdfDecoder decode_pdf = null;

		try {
			decode_pdf = new PdfDecoder(false);
			decode_pdf.setExtractionMode(PdfDecoder.TEXT); // extract just text

			PdfDecoder.init(true);

			/**
			 * open the file (and read metadata including pages in file)
			 */

			decode_pdf.openPdfFile(this.selectedFile);

		}
		catch (Exception e) {
			System.err.println("Exception " + e + " in pdf code");
		}

		/**
		 * extract data from pdf (if allowed).
		 */
		if ((decode_pdf.isEncrypted() && (!decode_pdf.isPasswordSupplied())) && (!decode_pdf.isExtractionAllowed())) {
			System.out.println("Encrypted settings");
			System.out.println("Please look at Viewer for code sample to handle such files");
		}
		else {

			ProgressMonitor status = new ProgressMonitor(this.currentGUI.getFrame(), Messages.getMessage("PdfViewerMessage.ExtractText"), "",
					startPage, endPage);
			/**
			 * extract data from pdf
			 */
			try {
				int count = 0;
				boolean yesToAll = false;
				for (int page = startPage; page < endPage + 1; page++) { // read pages
					if (status.isCanceled()) {
						this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerError.UserStoppedExport") + count + ' '
								+ Messages.getMessage("PdfViewerError.ReportNumberOfPagesExported"));
						return;
					}
					// decode the page
					decode_pdf.decodePage(page);

					/** create a grouping object to apply grouping to data */
					PdfGroupingAlgorithms currentGrouping = decode_pdf.getGroupingObject();

					/** use whole page size for demo - get data from PageData object */
					PdfPageData currentPageData = decode_pdf.getPdfPageData();

					int x1, y1, x2, y2;

					x1 = currentPageData.getMediaBoxX(page);
					x2 = currentPageData.getMediaBoxWidth(page) + x1;

					y2 = currentPageData.getMediaBoxY(page);
					y1 = currentPageData.getMediaBoxHeight(page) + y2;

					// default for xml
					String ending = "_text.csv";

					if (useXMLExtraction) ending = "_xml.txt";

					/** Co-ordinates are x1,y1 (top left hand corner), x2,y2(bottom right) */

					/** The call to extract the table */
					Map tableContent;
					String tableText = null;

					try {
						// the source code for this grouping is in the customer area
						// in class pdfGroupingAlgorithms
						// all these settings are defined in the Java

						tableContent = currentGrouping.extractTextAsTable(x1, y1, x2, y2, page, !useXMLExtraction, false, false, false, 0);

						// get the text from the Map object
						tableText = (String) tableContent.get("content");

					}
					catch (PdfException e) {
						decode_pdf.closePdfFile();
						System.err.println("Exception " + e.getMessage() + " with table extraction");
					}
					catch (Error e) {
						e.printStackTrace();
					}

					if (tableText == null) {
						System.out.println("No text found");
					}
					else {

						String target = output_dir + this.separator + "table" + this.separator;

						// create a directory if it doesn't exist
						File output_path = new File(target);
						if (output_path.exists() == false) output_path.mkdirs();

						File fileToSave = new File(target + this.fileName + '_' + page + ending);
						if (fileToSave.exists() && !yesToAll) {
							if ((endPage - startPage) > 1) {
								int n = this.currentGUI.showOverwriteDialog(fileToSave.getAbsolutePath(), true);

								if (n == 0) {
									// clicked yes so just carry on for this once
								}
								else
									if (n == 1) {
										// clicked yes to all, so set flag
										yesToAll = true;
									}
									else
										if (n == 2) {
											// clicked no, so loop round again
											status.setProgress(page);
											continue;
										}
										else {

											this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerError.UserStoppedExport") + count + ' '
													+ Messages.getMessage("PdfViewerError.ReportNumberOfPagesExported"));

											status.close();
											return;
										}
							}
							else {
								int n = this.currentGUI.showOverwriteDialog(fileToSave.getAbsolutePath(), false);

								if (n == 0) {
									// clicked yes so just carry on
								}
								else {
									// clicked no, so exit
									return;
								}
							}
						}

						/**
						 * output the data - you may wish to alter the encoding to suit
						 */
						OutputStreamWriter output_stream = new OutputStreamWriter(new FileOutputStream(target + this.fileName + '_' + page + ending),
								"UTF-8");

						// xml header
						if (useXMLExtraction) output_stream.write("<xml><BODY>\n\n");

						output_stream.write(tableText); // write actual data

						// xml footer
						if (useXMLExtraction) output_stream.write("\n</body></xml>");

						output_stream.close();

					}
					count++;
					status.setProgress(page + 1);
					// remove data once written out
					decode_pdf.flushObjectValues(false);
				}
				status.close();
				this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerMessage.TextSavedTo") + ' ' + output_dir);
			}
			catch (Exception e) {
				decode_pdf.closePdfFile();
				System.err.println("Exception " + e.getMessage());
				e.printStackTrace();
			}
			catch (Error e) {
				System.out.println("h34343");
				e.printStackTrace();
			}

			decode_pdf.flushObjectValues(true); // flush any text data read

		}

		/** close the pdf file */
		decode_pdf.closePdfFile();
	}

	private void decodeTextWordlist(int startPage, int endPage, String output_dir, boolean useXMLExtraction) {

		PdfDecoder decode_pdf = null;

		// PdfDecoder returns a PdfException if there is a problem
		try {
			decode_pdf = new PdfDecoder(false);

			decode_pdf.setExtractionMode(PdfDecoder.TEXT); // extract just text
			PdfDecoder.init(true);

			// always reset to use unaltered co-ords - allow use of rotated or unrotated
			// co-ordinates on pages with rotation (used to be in PdfDecoder)
			PdfGroupingAlgorithms.useUnrotatedCoords = false;

			/**
			 * open the file (and read metadata including pages in file)
			 */
			decode_pdf.openPdfFile(this.selectedFile);

		}
		catch (PdfSecurityException e) {
			System.err.println("Exception " + e + " in pdf code for wordlist" + this.selectedFile);
		}
		catch (PdfException e) {
			System.err.println("Exception " + e + " in pdf code for wordlist" + this.selectedFile);

		}
		catch (Exception e) {
			System.err.println("Exception " + e + " in pdf code for wordlist" + this.selectedFile);
			e.printStackTrace();
		}

		/**
		 * extract data from pdf (if allowed).
		 */
		if ((decode_pdf.isEncrypted() && (!decode_pdf.isPasswordSupplied())) && (!decode_pdf.isExtractionAllowed())) {
			System.out.println("Encrypted settings");
			System.out.println("Please look at Viewer for code sample to handle such files");

		}
		else {
			// page range
			int start = startPage, end = endPage;
			int wordsExtracted = 0;

			ProgressMonitor status = new ProgressMonitor(this.currentGUI.getFrame(), Messages.getMessage("PdfViewerMessage.ExtractText"), "",
					startPage, endPage);

			/**
			 * extract data from pdf
			 */
			try {
				int count = 0;
				boolean yesToAll = false;
				for (int page = start; page < end + 1; page++) { // read pages
					if (status.isCanceled()) {
						this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerError.UserStoppedExport") + count + ' '
								+ Messages.getMessage("PdfViewerError.ReportNumberOfPagesExported"));
						return;
					}
					// decode the page
					decode_pdf.decodePage(page);

					/** create a grouping object to apply grouping to data */
					PdfGroupingAlgorithms currentGrouping = decode_pdf.getGroupingObject();

					/** use whole page size for demo - get data from PageData object */
					PdfPageData currentPageData = decode_pdf.getPdfPageData();

					int x1 = currentPageData.getMediaBoxX(page);
					int x2 = currentPageData.getMediaBoxWidth(page) + x1;

					int y2 = currentPageData.getMediaBoxX(page);
					int y1 = currentPageData.getMediaBoxHeight(page) - y2;

					/** Co-ordinates are x1,y1 (top left hand corner), x2,y2(bottom right) */

					/** The call to extract the list */
					List words = null;

					try {
						words = currentGrouping.extractTextAsWordlist(x1, y1, x2, y2, page, true, "&:=()!;.,\\/\"\"\'\'");
					}
					catch (PdfException e) {
						decode_pdf.closePdfFile();
						System.err.println("Exception= " + e + " in " + this.selectedFile);
						e.printStackTrace();
					}
					catch (Error e) {
						e.printStackTrace();
					}

					if (words == null) {

						System.out.println("No text found");

					}
					else {

						String target = output_dir + this.separator + "wordlist" + this.separator;

						// create a directory if it doesn't exist
						File output_path = new File(target);
						if (output_path.exists() == false) output_path.mkdirs();

						/**
						 * choose correct prefix
						 */
						String prefix = "_text.txt";
						String encoding = System.getProperty("file.encoding");

						if (useXMLExtraction) {
							prefix = "_xml.txt";
							encoding = "UTF-8";
						}

						/** each word is stored as 5 consecutive values (word,x1,y1,x2,y2) */
						int wordCount = words.size() / 5;

						// update our count
						wordsExtracted = wordsExtracted + wordCount;

						File fileToSave = new File(target + this.fileName + '_' + page + prefix);
						if (fileToSave.exists() && !yesToAll) {
							if ((endPage - startPage) > 1) {
								int n = this.currentGUI.showOverwriteDialog(fileToSave.getAbsolutePath(), true);

								if (n == 0) {
									// clicked yes so just carry on for this once
								}
								else
									if (n == 1) {
										// clicked yes to all, so set flag
										yesToAll = true;
									}
									else
										if (n == 2) {
											// clicked no, so loop round again
											status.setProgress(page);
											continue;
										}
										else {

											this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerError.UserStoppedExport") + count + ' '
													+ Messages.getMessage("PdfViewerError.ReportNumberOfPagesExported"));

											status.close();
											return;
										}
							}
							else {
								int n = this.currentGUI.showOverwriteDialog(fileToSave.getAbsolutePath(), false);

								if (n == 0) {
									// clicked yes so just carry on
								}
								else {
									// clicked no, so exit
									return;
								}
							}
						}

						/**
						 * output the data
						 */
						OutputStreamWriter output_stream = new OutputStreamWriter(new FileOutputStream(target + this.fileName + '_' + page + prefix),
								encoding);

						Iterator wordIterator = words.iterator();
						while (wordIterator.hasNext()) {

							String currentWord = (String) wordIterator.next();

							/** remove the XML formatting if present - not needed for pure text */
							if (!useXMLExtraction) currentWord = Strip.convertToText(currentWord, true);

							int wx1 = (int) Float.parseFloat((String) wordIterator.next());
							int wy1 = (int) Float.parseFloat((String) wordIterator.next());
							int wx2 = (int) Float.parseFloat((String) wordIterator.next());
							int wy2 = (int) Float.parseFloat((String) wordIterator.next());

							/** this could be inserting into a database instead */
							output_stream.write(currentWord + ',' + wx1 + ',' + wy1 + ',' + wx2 + ',' + wy2 + '\n');

						}
						output_stream.close();

					}

					count++;
					status.setProgress(page + 1);

					// remove data once written out
					decode_pdf.flushObjectValues(false);

				}
				status.close();
				this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerMessage.TextSavedTo") + ' ' + output_dir);
			}
			catch (Exception e) {
				decode_pdf.closePdfFile();
				System.err.println("Exception " + e + " in " + this.selectedFile);
				e.printStackTrace();
			}
			catch (Error e) {
				e.printStackTrace();
			}
		}

		/** close the pdf file */
		decode_pdf.closePdfFile();
	}

	private void decodeTextRectangle(int startPage, int endPage, String output_dir, boolean useXMLExtraction) {

		PdfDecoder decode_pdf = null;

		// PdfDecoder returns a PdfException if there is a problem
		try {
			decode_pdf = new PdfDecoder(false);

			if (!useXMLExtraction) decode_pdf.useTextExtraction();

			decode_pdf.setExtractionMode(PdfDecoder.TEXT); // extract just text
			PdfDecoder.init(true);

			/**
			 * open the file (and read metadata including pages in file)
			 */
			decode_pdf.openPdfFile(this.selectedFile);

		}
		catch (PdfSecurityException se) {
			System.err.println("Security Exception " + se + " in pdf code for text extraction on file ");
			// e.printStackTrace();
		}
		catch (PdfException se) {
			System.err.println("Pdf Exception " + se + " in pdf code for text extraction on file ");
			// e.printStackTrace();
		}
		catch (Exception e) {
			System.err.println("Exception " + e + " in pdf code for text extraction on file ");
			e.printStackTrace();
		}

		/**
		 * extract data from pdf (if allowed).
		 */
		if ((decode_pdf.isEncrypted() && (!decode_pdf.isPasswordSupplied())) && (!decode_pdf.isExtractionAllowed())) {
			System.out.println("Encrypted settings");
			System.out.println("Please look at Viewer for code sample to handle such files");

		}
		else {

			ProgressMonitor status = new ProgressMonitor(this.currentGUI.getFrame(), Messages.getMessage("PdfViewerMessage.ExtractText"), "",
					startPage, endPage);

			/**
			 * extract data from pdf
			 */
			try {
				int count = 0;
				boolean yesToAll = false;
				for (int page = startPage; page < endPage + 1; page++) { // read pages
					if (status.isCanceled()) {
						this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerError.UserStoppedExport") + count + ' '
								+ Messages.getMessage("PdfViewerError.ReportNumberOfPagesExported"));
						return;
					}
					// decode the page
					decode_pdf.decodePage(page);

					/** create a grouping object to apply grouping to data */
					PdfGroupingAlgorithms currentGrouping = decode_pdf.getGroupingObject();

					/** use whole page size for demo - get data from PageData object */
					PdfPageData currentPageData = decode_pdf.getPdfPageData();

					int x1 = currentPageData.getMediaBoxX(page);
					int x2 = currentPageData.getMediaBoxWidth(page) + x1;

					int y2 = currentPageData.getMediaBoxY(page);
					int y1 = currentPageData.getMediaBoxHeight(page) + y2;

					/** Co-ordinates are x1,y1 (top left hand corner), x2,y2(bottom right) */

					/** The call to extract the text */
					String text = null;

					try {
						text = currentGrouping.extractTextInRectangle(x1, y1, x2, y2, page, false, true);
					}
					catch (PdfException e) {
						decode_pdf.closePdfFile();
						System.err.println("Exception " + e.getMessage() + " in file " + decode_pdf.getObjectStore().fullFileName);
						e.printStackTrace();
					}

					// allow for no text
					if (text == null) continue;

					String target = output_dir + this.separator + "rectangle" + this.separator;

					// ensure a directory for data
					File page_path = new File(target);
					if (page_path.exists() == false) page_path.mkdirs();

					/**
					 * choose correct prefix
					 */
					String prefix = "_text.txt";
					String encoding = System.getProperty("file.encoding");

					if (useXMLExtraction) {
						prefix = "_xml.txt";
						encoding = "UTF-8";
					}

					File fileToSave = new File(target + this.fileName + '_' + page + prefix);
					if (fileToSave.exists() && !yesToAll) {
						if ((endPage - startPage) > 1) {
							int n = this.currentGUI.showOverwriteDialog(fileToSave.getAbsolutePath(), true);

							if (n == 0) {
								// clicked yes so just carry on for this once
							}
							else
								if (n == 1) {
									// clicked yes to all, so set flag
									yesToAll = true;
								}
								else
									if (n == 2) {
										// clicked no, so loop round again
										status.setProgress(page);
										continue;
									}
									else {

										this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerError.UserStoppedExport") + count + ' '
												+ Messages.getMessage("PdfViewerError.ReportNumberOfPagesExported"));

										status.close();
										return;
									}
						}
						else {
							int n = this.currentGUI.showOverwriteDialog(fileToSave.getAbsolutePath(), false);

							if (n == 0) {
								// clicked yes so just carry on
							}
							else {
								// clicked no, so exit
								return;
							}
						}
					}

					/**
					 * output the data
					 */
					OutputStreamWriter output_stream = new OutputStreamWriter(new FileOutputStream(target + this.fileName + '_' + page + prefix),
							encoding);

					if ((useXMLExtraction)) {
						output_stream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");
						output_stream.write("<!-- Pixel Location of text x1,y1,x2,y2\n");
						output_stream.write("(x1,y1 is top left corner)\n");
						output_stream.write("(x1,y1 is bottom right corner)\n");
						output_stream.write("(origin is bottom left corner)  -->\n");
						output_stream.write("\n\n<ARTICLE>\n");
						output_stream.write("<LOCATION x1=\"" + x1 + "\" " + "y1=\"" + y1 + "\" " + "x2=\"" + x2 + "\" " + "y2=\"" + y2 + "\" />\n");
						output_stream.write("\n\n<TEXT>\n");
						// NOTE DATA IS TECHNICALLY UNICODE
						output_stream.write(text); // write actual data
						output_stream.write("\n\n</TEXT>\n");
						output_stream.write("\n\n</ARTICLE>\n");
					}
					else output_stream.write(text); // write actual data

					count++;
					output_stream.close();

					status.setProgress(page + 1);

					// remove data once written out
					decode_pdf.flushObjectValues(true);
				}
				status.close();
				this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerMessage.TextSavedTo") + ' ' + output_dir);

			}
			catch (Exception e) {
				decode_pdf.closePdfFile();
				System.err.println("Exception " + e.getMessage());
				e.printStackTrace();
				System.out.println(decode_pdf.getObjectStore().getCurrentFilename());
			}
		}

		/** close the pdf file */
		decode_pdf.closePdfFile();
	}
}
