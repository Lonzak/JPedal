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
 * ExtractPagesAsJavaFX.java
 * ---------------
 */

/**
 *
 * This example opens a pdf file and extracts the JavaFX version of each page
 */
package org.jpedal.examples.javafx;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.jpedal.PdfDecoder;
import org.jpedal.external.Options;
import org.jpedal.fonts.FontMappings;
import org.jpedal.io.ObjectStore;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.render.output.GenericFontMapper;
import org.jpedal.render.output.OutputDisplay;
import org.jpedal.render.output.javafx.FXMLDisplay;
import org.jpedal.render.output.javafx.JavaFXDisplay;
import org.jpedal.utils.LogWriter;

public class ExtractPagesAsJavaFX {

	/** output where we put files */
	private String user_dir = System.getProperty("user.dir");

	/** flag to show if we print messages */
	public static boolean outputMessages = false;

	String output_dir = null;

	/** correct separator for OS */
	String separator = System.getProperty("file.separator");

	/** the decoder object which decodes the pdf and returns a data object */
	PdfDecoder decode_pdf = null;

	/** flag to show if using images at highest quality -switch on with command line flag Dhires */
	private boolean useHiresImage = false;

	/**
	 * sample file which can be setup - substitute your own. If a directory is given, all the files in the directory will be processed
	 */
	private String test_file = "/mnt/shared/sample_pdfs/general/World Factbook.pdf";

	/** used as part of test to limit pages to first 10 */
	public static boolean isTest = false;

	/** file password or null */
	private String password = "";

	// alt name for first page (ie index)
	private String firstPageName = null;

	/** Output a XML file contain the foonts in this extraction for user to edit **/
	private static boolean createTemplate = false;
	private static String saveTemplateFileName = "/Users/markee/Desktop/test.xml";

	private static boolean loadTemplate = false;
	private static String loadTemplateFileName;

	// Alternate between JavaFX and FXML
	private boolean outputAsFXML = false;

	int end, page;

	/** used by IDEs to exit on request */
	private boolean exitRequested;

	private int numPages = -1; // Used when JAR created.

	/**
	 * constructor to provide same functionality as main method
	 * 
	 */
	public ExtractPagesAsJavaFX() {

		init();
	}

	/**
	 * constructor to provide same functionality as main method
	 * 
	 */
	public ExtractPagesAsJavaFX(String[] args) {

		init();

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

		/**
		 * allow user to set a JVM flag to enable JavaFx or FXML
		 */
		if (System.getProperty("org.jpedal.pdf2javafx.outputAsFXML") != null
				&& System.getProperty("org.jpedal.pdf2javafx.outputAsFXML").toLowerCase().equals("true")) this.outputAsFXML = true;

		// System.out.println("testing file="+file_name);
		extraction(file_name, this.output_dir);

		String pdfName = pdf_file.getName();
		String name;
		// Online converter doesn't need FX or FXML appended to the name.
		if (System.getProperty("IsOnlineConverter") != null) {
			name = getStrippedText(pdfName.substring(0, pdfName.length() - 4));
		}
		else {
			name = convertPDFName(pdfName.substring(0, pdfName.length() - 4), !this.outputAsFXML);
		}

		// Compile all .java files
		compile(this.output_dir, name);

		if (this.firstPageName == null) { // We haven't specified a first name so use default
			this.firstPageName = "page";
			for (int i = 1; i < String.valueOf(this.numPages).length(); i++) {
				this.firstPageName += '0';
			}
			this.firstPageName += '1';
		}

		// Make an executable JAR file
		try {
			mkJar(this.output_dir, name, this.firstPageName);
		}
		catch (IOException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		// Clean up all those .class files that got created
		tidyUpClassFiles(this.output_dir, name);
	}

	private static void tidyUpClassFiles(String dir, String name) {
		File directory = new File(dir, name);
		for (File f : directory.listFiles()) {
			if (f.getAbsolutePath().endsWith(".class")) {
				f.delete();
			}
		}
	}

	private static void mkJar(String dir, String name, String firstPageName) throws IOException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, name + '/' + firstPageName);
		JarOutputStream target = new JarOutputStream(new FileOutputStream(dir + name + ".jar"), manifest);
		add(new File(dir, name), target, dir);
		target.close();
	}

	private static void add(File source, JarOutputStream target, String dir) throws IOException {
		BufferedInputStream in = null;
		try {
			if (source.isDirectory()) {
				for (File nestedFile : source.listFiles())
					add(nestedFile, target, dir);
				return;
			}

			JarEntry entry = new JarEntry(source.getPath().replace("\\", "/").replace((new File(dir).getPath() + "/").replace("\\", "/"), ""));
			entry.setTime(source.lastModified());
			target.putNextEntry(entry);
			in = new BufferedInputStream(new FileInputStream(source));

			byte[] buffer = new byte[1024];
			while (true) {
				int count = in.read(buffer);
				if (count == -1) break;
				target.write(buffer, 0, count);
			}
			target.closeEntry();
		}
		finally {
			if (in != null) in.close();
		}
	}

	private static void compile(String outputDir, String pdfName) {

		File dir = new File(outputDir, pdfName);

		ArrayList<File> files = new ArrayList<File>();

		for (File f : dir.listFiles()) {
			if (f.getAbsolutePath().endsWith(".java")) {
				files.add(f);
			}
		}

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) throw new RuntimeException("Jar could not be created as Java version requires javac.");
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

		Iterable<? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjectsFromFiles(files);

		String[] compileOptions;
		if (System.getProperty("IsOnlineConverter") != null) {
			compileOptions = new String[] { "-encoding", "UTF-8", "-classpath", "../jfxrt.jar" };
		}
		else {
			compileOptions = new String[] { "-encoding", "UTF-8" };
		}
		Iterable<String> compilationOptionss = Arrays.asList(compileOptions);

		compiler.getTask(null, fileManager, null, compilationOptionss, null, compilationUnits1).call();

		try {
			fileManager.close();
		}
		catch (IOException e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	private static void init() {

		loadTemplateFileName = System.getProperty("org.jpedal.loadXML");
		if (loadTemplateFileName != null && (new File(loadTemplateFileName).exists())) {
			loadTemplate = true;
		}
		else {
			loadTemplate = false;
		}

		saveTemplateFileName = System.getProperty("org.jpedal.saveXML");
		if (saveTemplateFileName != null) {
			createTemplate = true;
		}
		else {
			createTemplate = false;
		}
	}

	public void extraction(String file_name, String output_dir) {

		this.output_dir = output_dir;
		// check output dir has separator
		if (this.user_dir.endsWith(this.separator) == false) this.user_dir = this.user_dir + this.separator;

		// System.out.println("output_dir: " + output_dir);

		/**
		 * allow user to set a JVM flag to enable first name page (null if not set)
		 */
		this.firstPageName = System.getProperty("org.jpedal.pdf2javafx.firstPageName");

		/**
		 * if file name ends pdf, do the file otherwise do every pdf file in the directory. We already know file or directory exists so no need to
		 * check that, but we do need to check its a directory
		 */
		if (file_name.toLowerCase().endsWith(".pdf")) {

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

					if (file.toLowerCase().endsWith(".pdf") && !file.startsWith(".")) {
						if (outputMessages) System.out.println(file_name + file);

						decodeFile(file_name + file, output_dir);

					}
				}
			}
		}

		/** tell user */
		if (outputMessages) System.out.println("JavaFX created");
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
			if ((!ExtractPagesAsJavaFX.isTest) && (file_name.toLowerCase().endsWith(".pdf"))) {
				name = file_name.substring(0, file_name.length() - 4);
			}

		name = getStrippedText(name); // changes the name to a java safe name

		// PdfDecoder returns a PdfException if there is a problem
		try {
			this.decode_pdf = new PdfDecoder(true);

			/**
			 * font mappings
			 */
			if (!isTest) {

				// mappings for non-embedded fonts to use
				FontMappings.setFontReplacements();

			}

			/**
			 * open the file (and read metadata including pages in file)
			 */
			if (this.password != null) this.decode_pdf.openPdfFile(file_name, this.password);
			else this.decode_pdf.openPdfFile(file_name);

			this.numPages = this.decode_pdf.getPageCount();

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

				if (!this.outputAsFXML) {
					// Added the name of the file to the output path so that a folder containing all the elements for the pdf is created.
					if (System.getProperty("IsOnlineConverter") != null) extractPageAsJavaFX(file_name, output_dir + name + this.separator, name);
					else extractPageAsJavaFX(file_name, output_dir + "FX" + name + this.separator, "FX" + name);
				}
				else {
					if (System.getProperty("IsOnlineConverter") != null) extractPageAsJavaFX(file_name, output_dir + name + this.separator, name);
					else extractPageAsJavaFX(file_name, output_dir + "FXML" + name + this.separator, "FXML" + name);
				}

			}

		/** close the pdf file */
		this.decode_pdf.closePdfFile();
	}

	public int getPageCount() {
		return this.end;
	}

	public int getPageReached() {
		return this.page;
	}

	public static String convertPDFName(String name, boolean isPDFtoFX) {
		if (isPDFtoFX) return "FX" + getStrippedText(name);
		else return "FXML" + getStrippedText(name);
	}

	private void extractPageAsJavaFX(String file_name, String output_dir, String name) {

		// create a directory if it doesn't exist
		if (output_dir != null) {
			File output_path = new File(output_dir);
			if (!output_path.exists()) output_path.mkdirs();
		}

		// page range
		int start = 1;
		this.end = this.decode_pdf.getPageCount();

		// limit to 1st ten pages in testing
		if (this.end > 10 && isTest) this.end = 10;

		/**
		 * extract data from pdf and then write out the pages as javaFX
		 */

		if (outputMessages) System.out.println("JavaFX file will be in  " + output_dir);

		try {

			GenericFontMapper.setXMLTemplate(createTemplate);

			if (loadTemplate) GenericFontMapper.loadCustomFontMappings(new FileInputStream(new File(loadTemplateFileName)));

			// add the icons to the directory
			File iconDir = new File(output_dir + "/icons");
			if (!iconDir.exists()) iconDir.mkdirs();

			/**
			 * copy all images
			 */
			String[] images = new String[] { "smstart.gif", "smback.gif", "smfback.gif", "smforward.gif", "smfforward.gif", "smend.gif", "logo.gif" };

			for (String image : images) {

				// data for each file in turn
				InputStream is = getClass().getResourceAsStream("/org/jpedal/examples/javafx/icons/" + image);
				BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(iconDir + this.separator + image));
				byte[] buffer = new byte[65536];// Not sure about this line
				int length;
				while ((length = is.read(buffer)) > 0) {
					os.write(buffer, 0, length);
				}
				os.close();
				is.close();
			}

			for (this.page = start; this.page < this.end + 1; this.page++) { // read pages

				/**
				 * create a name with zeros for if more than 9 pages appears in correct order
				 */
				String pageAsString = String.valueOf(this.page);

				if (this.firstPageName != null && this.page == start) {
					pageAsString = this.firstPageName;
				}
				else {
					String maxPageSize = String.valueOf(this.end);
					int padding = maxPageSize.length() - pageAsString.length();
					for (int ii = 0; ii < padding; ii++)
						pageAsString = '0' + pageAsString;
				}

				if (outputMessages) System.out.println("Page " + pageAsString);

				// String outputName =name +"page" + pageAsString; /*//use to debug multiple documents
				// System.out.println("========================================================"+outputName);

				int cropX = this.decode_pdf.getPdfPageData().getCropBoxX(this.page);
				int cropY = this.decode_pdf.getPdfPageData().getCropBoxY(this.page);
				int cropW = this.decode_pdf.getPdfPageData().getCropBoxWidth(this.page);
				int cropH = this.decode_pdf.getPdfPageData().getCropBoxHeight(this.page);

				// Create Rectangle object to match width and height of cropbox
				Rectangle cropBox = new Rectangle(0, 0, cropW, cropH);
				// Find middle of cropbox in Pdf Coordinates
				Point2D midPoint = new Point2D.Double((cropW / 2) + cropX, (cropH / 2) + cropY);

				DynamicVectorRenderer javaFXOutput;

				if (this.outputAsFXML) {
					javaFXOutput = new FXMLDisplay(this.page, midPoint, cropBox, false, 100, new ObjectStore(null));
				}
				else javaFXOutput = new JavaFXDisplay(this.page, midPoint, cropBox, false, 100, new ObjectStore(null));

				// have a scaling factor so we can alter the page size
				float scaling = 1.0f;

				/**
				 * if you want to fit it to a certain size, use this code work out max possible scaling for both width and height and use smaller to
				 * get max possible size but retain aspect ratio - will not always be exact match as preserves aspect ratio
				 * 
				 * float preferredWidth=1000,preferredHeight=1000;
				 * 
				 * float scalingX=preferredWidth/cropW; // scaling we need to scale w up to our value float scalingY=preferredHeight/cropH; // scaling
				 * we need to scale w up to our value
				 * 
				 * if(scalingX>scalingY) scaling=scalingY; else scaling=scalingX; /
				 **/

				javaFXOutput.setValue(OutputDisplay.PercentageScaling, (int) (scaling * 100)); // set page scaling (default is 100%)
				javaFXOutput.writeCustom(OutputDisplay.PAGEDATA, this.decode_pdf.getPdfPageData()); // pass in PageData object so we c
				javaFXOutput.setValue(OutputDisplay.MaxNumberOfDecimalPlaces, 0); // let use select max number of decimal places
				javaFXOutput.setOutputDir(output_dir, name, pageAsString); // root for output

				this.decode_pdf.addExternalHandler(javaFXOutput, Options.CustomOutput); // custom object to draw PDF

				// Set page range - Start and end of page decode
				javaFXOutput.setValue(OutputDisplay.StartOfDecode, start);
				javaFXOutput.setValue(OutputDisplay.EndOfDecode, this.end);

				/**
				 * This allows the user to have a nav bar on page
				 */
				javaFXOutput.setBooleanValue(OutputDisplay.AddNavBar, true);

				/**
				 * include irregular curved clips. (As used in SVG & HTML)
				 */
				javaFXOutput.setBooleanValue(OutputDisplay.IncludeClip, true);

				/**
				 * useful config options
				 */
				// JavaFXOutput.writeCustom(OutputDisplay.SET_ENCODING_USED, new String[]{"UTF-16","utf-16"}); //java/output string value

				/**
				 * get the current page as JavaFX
				 */
				this.decode_pdf.decodePage(this.page);

				// flush images in case we do more than 1 page so only contains
				// images from current page
				this.decode_pdf.flushObjectValues(true);
				// flush any text data read

				if (this.exitRequested) {
					this.end = this.page;
				}
			}
		}
		catch (Exception e) {

			this.decode_pdf.closePdfFile();
			throw new RuntimeException("Exception " + e.getMessage() + " on File=" + file_name);
		}

		if (createTemplate) {
			GenericFontMapper.createXMLTemplate(saveTemplateFileName);
		}
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * main routine which checks for any files passed and runs the demo
	 * 
	 */
	public static void main(String[] args) {

		if (outputMessages) System.out.println("Simple demo to extract JavaFX version of a page");

		new ExtractPagesAsJavaFX(args);
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

						// assume password if no / or \
						if (value.endsWith("/") || value.endsWith("\\")) this.output_dir = value;
						else this.password = value;

					}
				}
		return file_name;
	}

	private static void showCommandLineValues() {

		System.out.println("Example takes 2 or 3 parameters");
		System.out.println("Value 1 is the file name or directory of PDF files to process");
		System.out.println("Value 2 is the pass to write out JavaFX and directories and must end with / or \\ character)");
		System.out.println("Value 3 (optional) password for PDF file");

		System.exit(0);
	}

	/**
	 * @return Returns the output_dir.
	 */
	public String getOutputDir() {
		return this.output_dir;
	}

	/**
	 * used by IDEs to exit before end of file if requested
	 */
	public void stopConversion() {
		this.exitRequested = true;
	}

	/**
	 * Returns the stripped out, java coding friendly, version of input
	 * 
	 * @param input
	 * @return
	 */
	protected static String getStrippedText(String input) {

		String output = "";
		char illegalCharacters[] = { '<', '>', '\\', ':', ';', '*', '^', '@', '?', '=', '[', ']', '`' };
		char minVal = 48; // 0
		char maxVal = 122; // z
		for (int i = 0; i < input.length(); i++) {

			if (input.charAt(i) < minVal || input.charAt(i) > maxVal) {
				continue;
			}

			boolean foundIllegal = false;
			for (char illegalCharacter : illegalCharacters) {
				if (input.charAt(i) == illegalCharacter) {
					foundIllegal = true;
					break;
				}
			}
			if (foundIllegal) {
				continue;
			}

			output += input.charAt(i);
		}

		return output;
	}

}
