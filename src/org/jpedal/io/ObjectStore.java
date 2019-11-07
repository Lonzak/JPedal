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
 * ObjectStore.java
 * ---------------
 */
package org.jpedal.io;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jpedal.examples.handlers.DefaultImageHelper;
import org.jpedal.external.ImageHelper;
import org.jpedal.render.DynamicVectorRenderer;
import org.jpedal.render.SwingDisplay;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Strip;

/**
 * set of methods to save/load objects to keep memory usage to a minimum by spooling images to disk Also includes ancillary method to store a filename
 * - LogWriter is my logging class - Several methods are very similar and I should recode my code to use a common method for the RGB conversion
 * 
 * Converted to avoid threading issues, if this causes any problems, please raise them in our forums, http://www.jpedal.org/support.php
 */
public class ObjectStore {

	/** flag to verify if temp dir created and throw RuntimeException if not */
	public static final boolean verifyFilesSaved = false;

	/** list of files to delete */
	private static final Map undeletedFiles = new HashMap();

	/** do not set unless you know what you are doing */
	public static final boolean isMultiThreaded = false;

	ImageHelper images = new DefaultImageHelper();

	/** debug page cache */
	private static final boolean debugCache = false;

	/** ensure we check for 'dead' files only once per session */
	private static boolean checkedThisSession = false;

	/** correct separator for platform program running on */
	final static private String separator = System.getProperty("file.separator");

	/** file being decoded at present -used byOXbjects and other classes */
	private String currentFilename = "", currentFilePath = "";

	/** temp storage for the images so they are not held in memory */
	public static String temp_dir = "";

	public static final String multiThreaded_root_dir = null;

	/** temp storage for raw CMYK images */
	private static final String cmyk_dir = temp_dir + "cmyk" + separator;

	/** key added to each file to make sure unique to pdf being handled */
	private String key = "jpedal" + Math.random() + '_';

	/** track whether image saved as tif or jpg */
	private final Map image_type = new HashMap();

	/**
	 * map to hold file names
	 */
	private final Map tempFileNames = new HashMap();

	/** parameter stored on cached images */
	public static final Integer IMAGE_WIDTH = 1;

	/** parameter stored on cached images */
	public static final Integer IMAGE_HEIGHT = 2;

	/** parameter stored on cached images */
	public static final Integer IMAGE_pX = 3;

	/** parameter stored on cached images */
	public static final Integer IMAGE_pY = 4;

	/** paramter stored on cached images */
	public static final Integer IMAGE_MASKCOL = 5;

	/** paramter stored on cached images */
	public static final Integer IMAGE_COLORSPACE = 6;

	/**
	 * period after which we assumes files must be dead (ie from crashed instance) default is four hour image time
	 */
	public static final long time = 14400000;

	public String fullFileName;

	// list of cached pages
	private static final Map pagesOnDisk = new HashMap();
	private static final Map pagesOnDiskAsBytes = new HashMap();

	// list of images on disk
	private final Map imagesOnDiskAsBytes = new HashMap();

	private final Map imagesOnDiskAsBytesW = new HashMap();
	private final Map imagesOnDiskAsBytesH = new HashMap();
	private final Map imagesOnDiskAsBytespX = new HashMap();
	private final Map imagesOnDiskAsBytespY = new HashMap();
	private final Map imagesOnDiskMask = new HashMap();
	private final Map imagesOnDiskColSpaceID = new HashMap();

	/**
	 * ObjectStore - Converted for Threading purposes - To fix any errors please try replacing <b>ObjectStore</b> with <b>{your instance of
	 * PdfDecoder}.getObjectStore()</b> -
	 * 
	 */
	public ObjectStore(ImageHelper images) {

		if (images != null) {
			this.images = images;
		}

		init();
	}

	/**
	 * ObjectStore - Converted for Threading purposes - To fix any errors please try replacing <b>ObjectStore</b> with <b>{your instance of
	 * PdfDecoder}.getObjectStore()</b> -
	 * 
	 */
	public ObjectStore() {

		init();
	}

	private void init() {
		try {

			// if user has not set static value already, use tempdir
			if (temp_dir.length() == 0) temp_dir = System.getProperty("java.io.tmpdir");

			if (isMultiThreaded) { // public static variable to ensure unique
				if (multiThreaded_root_dir != null) temp_dir = multiThreaded_root_dir + separator + "jpedal-" + System.currentTimeMillis()
						+ separator;
				else temp_dir = temp_dir + separator + "jpedal-" + System.currentTimeMillis() + separator;
			}
			else
				if (temp_dir.length() == 0) temp_dir = temp_dir + separator + "jpedal" + separator;
				else
					if (!temp_dir.endsWith(separator)) temp_dir = temp_dir + separator;

			// create temp dir if it does not exist
			File f = new File(temp_dir);
			if (f.exists() == false) f.mkdirs();

			if (verifyFilesSaved) checkExists(f);

			if (isMultiThreaded) f.deleteOnExit();

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Unable to create temp dir at " + temp_dir);
		}
	}

	private static void checkExists(String s) {

		File f = new File(s);

		if (!f.exists()) {
			throw new RuntimeException("Unable to create " + f.getAbsolutePath());
		}
	}

	private static void checkExists(File f) {

		if (!f.exists()) {
			throw new RuntimeException("Unable to create " + f.getAbsolutePath());
		}
	}

	/**
	 * 
	 * get the file name - we use this as a get in our file repository -
	 * 
	 * 
	 * <b>Note </b> this method is not part of the API and is not guaranteed to be in future versions of JPedal -
	 * 
	 */
	public String getCurrentFilename() {
		return this.currentFilename;
	}

	/**
	 * 
	 * get the file path for current PDF
	 * 
	 * 
	 * <b>Note </b> this method is not part of the API and is not guaranteed to be in future versions of JPedal -
	 * 
	 */
	public String getCurrentFilepath() {
		return this.currentFilePath;
	}

	/**
	 * store filename as a key we can use to differentiate images,etc - <b>Note</b> this method is not part of the API and is not guaranteed to be in
	 * future versions of JPedal -
	 * 
	 */
	final public void storeFileName(String name) {

		// System.err.println("7");

		// name = removeIllegalFileNameCharacters(name);
		this.fullFileName = name;

		// get path
		int ptr = this.fullFileName.lastIndexOf('/');
		int ptr2 = this.fullFileName.lastIndexOf('\\');
		if (ptr2 > ptr) ptr = ptr2;
		if (ptr > 0) this.currentFilePath = this.fullFileName.substring(0, ptr + 1);
		else this.currentFilePath = "";

		/**
		 * work through to get last / or \ first we make sure there is one in the name. We could use the properties to get the correct value but the
		 * user can still use the Windows format under Unix
		 */
		int temp_pointer = name.indexOf('\\');
		if (temp_pointer == -1) // runs on either Unix or Windows!!!
		temp_pointer = name.indexOf('/');
		while (temp_pointer != -1) {
			name = name.substring(temp_pointer + 1);
			temp_pointer = name.indexOf('\\');
			if (temp_pointer == -1) // runs on either Unix or Windows!!!
			temp_pointer = name.indexOf('/');
		}

		/** strip any ending from name */
		int pointer = name.lastIndexOf('.');
		if (pointer != -1) name = name.substring(0, pointer);

		/** remove any spaces using my own class and enforce lower case */
		name = Strip.stripAllSpaces(name);
		this.currentFilename = name.toLowerCase();

		// System.err.println("8");
	}

	/**
	 * save raw CMYK data in CMYK directory - We extract the DCT encoded image stream and save as a file with a .jpeg ending so we have the raw image
	 * - This works for DeviceCMYK -
	 * 
	 */
	public boolean saveRawCMYKImage(byte[] image_data, String name) {

		// assume successful
		boolean isSuccessful = true;
		name = removeIllegalFileNameCharacters(name);

		/** create/check directories exists */
		File cmyk_d = new File(cmyk_dir);
		if (cmyk_d.exists() == false) cmyk_d.mkdirs();

		/** stream the data out - not currently Buffered */
		try {
			FileOutputStream a = new FileOutputStream(cmyk_dir + name + ".jpg");
			this.tempFileNames.put(cmyk_dir + name + ".jpg", "#");

			a.write(image_data);
			a.flush();
			a.close();

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Unable to save CMYK jpeg " + name);

			isSuccessful = false;
		}

		return isSuccessful;
	}

	/**
	 * save buffered image as JPEG or tif
	 * 
	 */
	final public synchronized boolean saveStoredImage(String current_image, BufferedImage image, boolean file_name_is_path, boolean save_unclipped,
			String type) {

		boolean was_error = false;

		current_image = removeIllegalFileNameCharacters(current_image);

		// if(image.getType()==1)
		// image=stripAlpha(image);
		int type_id = image.getType();

		// make sure temp directory exists
		File checkDir = new File(temp_dir);
		if (checkDir.exists() == false) checkDir.mkdirs();

		// save image and id so we can reload
		if (type.contains("tif")) {

			if (((type_id == 1 || type_id == 2)) && (!current_image.contains("HIRES_"))) image = ColorSpaceConvertor.convertColorspace(image,
					BufferedImage.TYPE_3BYTE_BGR);

			if (file_name_is_path == false) this.image_type.put(current_image, "tif");

			was_error = saveStoredImage("TIFF", ".tif", ".tiff", current_image, image, file_name_is_path, save_unclipped);
		}
		else
			if (type.contains("jpg")) {
				if (file_name_is_path == false) this.image_type.put(current_image, "jpg");

				was_error = saveStoredJPEGImage(current_image, image, file_name_is_path, save_unclipped);
			}
			else
				if (type.contains("png")) {

					if (file_name_is_path == false) this.image_type.put(current_image, "png");

					was_error = saveStoredImage("PNG", ".png", ".png", current_image, image, file_name_is_path, save_unclipped);

				}
		return was_error;
	}

	/**
	 * get type of image used to store graphic
	 * 
	 */
	final public String getImageType(String current_image) {
		return (String) this.image_type.get(current_image);
	}

	/**
	 * init method to pass in values for temp directory, unique key, etc so program knows where to store files
	 * 
	 */
	final public void init(String current_key) {
		this.key = current_key + System.currentTimeMillis();

		// create temp dir if it does not exist
		File f = new File(temp_dir);
		if (f.exists() == false) f.mkdirs();
	}

	/**
	 * load a image when required and remove from store
	 * 
	 */
	final public synchronized BufferedImage loadStoredImage(String current_image) {

		if (current_image == null) return null;

		current_image = removeIllegalFileNameCharacters(current_image);

		// see if jpg
		String flag = (String) this.image_type.get(current_image);
		BufferedImage image = null;
		if (flag == null) return null;
		else
			if (flag.equals("tif")) image = loadStoredImage(current_image, ".tif");
			else
				if (flag.equals("jpg")) image = loadStoredJPEGImage(current_image);
				else
					if (flag.equals("png")) image = loadStoredImage(current_image, ".png");

		return image;
	}

	/**
	 * see if image already saved to disk (ie multiple pages)
	 */
	final public synchronized boolean isImageCached(String current_image) {

		current_image = removeIllegalFileNameCharacters(current_image);

		// see if jpg
		String flag = (String) this.image_type.get(current_image);

		if (flag == null) return false;
		else {
			String file_name = temp_dir + this.key + current_image + '.' + flag;

			File imgFile = new File(file_name);

			return imgFile.exists();

		}
	}

	/**
	 * routine to remove all objects from temp store
	 * 
	 */
	final synchronized public void flush() {

		/**
		 * flush any image data serialized as bytes
		 */
		Iterator filesTodelete = this.imagesOnDiskAsBytes.keySet().iterator();
		while (filesTodelete.hasNext()) {
			Object file = filesTodelete.next();

			if (file != null) {
				File delete_file = new File((String) this.imagesOnDiskAsBytes.get(file));
				if (delete_file.exists()) delete_file.delete();
			}
		}

		/**/
		this.imagesOnDiskAsBytes.clear();
		this.imagesOnDiskAsBytesW.clear();
		this.imagesOnDiskAsBytesH.clear();
		this.imagesOnDiskAsBytespX.clear();
		this.imagesOnDiskAsBytespY.clear();
		this.imagesOnDiskMask.clear();
		this.imagesOnDiskColSpaceID.clear();
		/**/

		filesTodelete = this.tempFileNames.keySet().iterator();
		while (filesTodelete.hasNext()) {
			String file = ((String) filesTodelete.next());

			if (file.contains(this.key)) {

				// System.out.println("temp_dir="+temp_dir);
				File delete_file = new File(file);
				// System.out.println("Delete "+file);
				// delete_file.delete();

				if (delete_file.delete()) filesTodelete.remove();
				else // bug in Java stops files being deleted
				undeletedFiles.put(this.key, "x");
			}
		}

		try {

			// if setup then flush temp dir
			if (!checkedThisSession && temp_dir.length() > 2) {

				checkedThisSession = true;

				// get contents
				/**/
				File temp_files = new File(temp_dir);
				String[] file_list = temp_files.list();
				File[] to_be_del = temp_files.listFiles();
				if (file_list != null) {
					for (int ii = 0; ii < file_list.length; ii++) {
						if (file_list[ii].contains(this.key)) {
							File delete_file = new File(temp_dir + file_list[ii]);
							delete_file.delete();
						}
						// can we also delete any file more than 4 hours old here
						// its a static variable so user can change

						// flag to turn the redundant obj deletion on/off
						boolean delOldFiles = true;

						if (delOldFiles && (!file_list[ii].endsWith(".pdf") && (System.currentTimeMillis() - to_be_del[ii].lastModified() >= time))) {
								// System.out.println("File time : " + to_be_del[ii].lastModified() );
								// System.out.println("Current time: " + System.currentTimeMillis());
								// System.out.println("Redundant File Removed : " + to_be_del[ii].getName() );
								to_be_del[ii].delete();
						}
					}
				}

				/*
				 * //suggested by Manuel to ensure flushes correctly //System.gc(); Iterator filesTodelete = tempFileNames.keySet().iterator();
				 * while(filesTodelete.hasNext()) { String file = ((String)filesTodelete.next()); if (file.indexOf(key) != -1) { File delete_file =
				 * new File(file); //System.out.println("Delete "+file); //delete_file.delete(); //suggested by Manuel to ensure flushes correctly if
				 * (delete_file.delete()) filesTodelete.remove(); } } /*
				 */
			}

			/** flush cmyk directory as well */
			final File cmyk_d = new File(cmyk_dir);
			if (cmyk_d.exists()) {
				/*
				 * boolean filesExist = false; String[] file_list = cmyk_d.list(); for (int ii = 0; ii < file_list.length; ii++) { File delete_file =
				 * new File(cmyk_dir + file_list[ii]); delete_file.delete(); //make sure deleted if (delete_file.exists()) filesExist = true; }
				 */
				cmyk_d.delete();

				/*
				 * if (filesExist) LogWriter.writeLog("CMYK files not deleted at end");
				 */
			}

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " flushing files");
		}
	}

	/**
	 * copies cmyk raw data from cmyk temp dir to target directory
	 * 
	 */
	public static void copyCMYKimages(String target_dir) {

		File cmyk_d = new File(cmyk_dir);
		if (cmyk_d.exists()) {
			String[] file_list = cmyk_d.list();

			if (file_list.length > 0) {
				/** check separator on target dir and exists */
				if (target_dir.endsWith(separator) == false) target_dir = target_dir + separator;

				File test_d = new File(target_dir);
				if (test_d.exists() == false) test_d.mkdirs();

			}
			for (String aFile_list : file_list) {
				File source = new File(cmyk_dir + aFile_list);
				File dest = new File(target_dir + aFile_list);

				source.renameTo(dest);

			}
		}
	}

	/**
	 * save buffered image as JPEG
	 */
	private synchronized boolean saveStoredJPEGImage(String current_image, BufferedImage image, boolean file_name_is_path, boolean save_unclipped) {

		boolean was_error = false;

		// generate path name or use
		// value supplied by user
		String file_name = current_image;
		String unclipped_file_name = "";
		if (file_name_is_path == false) {
			file_name = temp_dir + this.key + current_image;
			unclipped_file_name = temp_dir + this.key + 'R' + current_image;

			// log the type in our table so we can lookup
			this.image_type.put('R' + current_image, this.image_type.get(current_image));
		}

		// add ending if needed
		if ((file_name.toLowerCase().endsWith(".jpg") == false) & (file_name.toLowerCase().endsWith(".jpeg") == false)) {
			file_name = file_name + ".jpg";
			unclipped_file_name = unclipped_file_name + ".jpg";
		}

		/**
		 * fudge to write out high quality then try low if failed
		 */
		try { // write out data to create image in temp dir

			this.images.write(image, "jpg", file_name);

			this.tempFileNames.put(file_name, "#");

			if (verifyFilesSaved) checkExists(file_name);

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " writing image " + image + " as " + file_name);
		}

		// save unclipped copy or make sure only original
		if (save_unclipped == true) {
			saveCopy(file_name, unclipped_file_name);
			this.tempFileNames.put(unclipped_file_name, "#");

		}

		return was_error;
	}

	public String getFileForCachedImage(String current_image) {
		return temp_dir + this.key + current_image + '.' + this.image_type.get(current_image);
	}

	/**
	 * load a image when required and remove from store
	 */
	private synchronized BufferedImage loadStoredImage(String current_image, String ending) {

		current_image = removeIllegalFileNameCharacters(current_image);

		String file_name = temp_dir + this.key + current_image + ending;

		// load the image to process
		BufferedImage image = this.images.read(file_name);

		return image;
	}

	/**
	 * save copy
	 */
	static private void saveCopy(String source, String destination) {
		BufferedInputStream from = null;
		BufferedOutputStream to = null;
		try {
			// create streams
			from = new BufferedInputStream(new FileInputStream(source));
			to = new BufferedOutputStream(new FileOutputStream(destination));

			// write
			byte[] buffer = new byte[65535];
			int bytes_read;
			while ((bytes_read = from.read(buffer)) != -1)
				to.write(buffer, 0, bytes_read);
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " copying file");
		}

		// close streams
		try {
			to.close();
			from.close();
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " closing files");
		}
	}

	/**
	 * save copy
	 * 
	 */
	final public void saveAsCopy(String current_image, String destination) {
		BufferedInputStream from = null;
		BufferedOutputStream to = null;

		current_image = removeIllegalFileNameCharacters(current_image);
		String source = temp_dir + this.key + current_image;

		try {
			// create streams
			from = new BufferedInputStream(new FileInputStream(source));
			to = new BufferedOutputStream(new FileOutputStream(destination));

			// write
			byte[] buffer = new byte[65535];
			int bytes_read;
			while ((bytes_read = from.read(buffer)) != -1)
				to.write(buffer, 0, bytes_read);
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " copying file");
		}

		// close streams
		try {
			to.close();
			from.close();
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " closing files");
		}
	}

	/**
	 * save copy
	 * 
	 * Converted to avoid threading issues, if this causes any problems, please raise them in our forums, http://www.jpedal.org/
	 */
	static public void copy(String source, String destination) {

		BufferedInputStream from = null;
		BufferedOutputStream to = null;

		try {
			// create streams
			from = new BufferedInputStream(new FileInputStream(source));
			to = new BufferedOutputStream(new FileOutputStream(destination));
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " copying file");
		}

		copy(from, to);
	}

	public static void copy(BufferedInputStream from, BufferedOutputStream to) {
		try {
			// write
			byte[] buffer = new byte[65535];
			int bytes_read;
			while ((bytes_read = from.read(buffer)) != -1)
				to.write(buffer, 0, bytes_read);
		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " copying file");
		}

		// close streams
		try {
			to.close();
			from.close();
		}
		catch (Exception e) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " closing files");
		}
	}

	/**
	 * load a image when required and remove from store
	 */
	private synchronized BufferedImage loadStoredJPEGImage(String current_image) {
		String file_name = temp_dir + this.key + current_image + ".jpg";

		// load the image to process
		BufferedImage image = null;
		File a = new File(file_name);
		if (a.exists()) {
			try {

				image = this.images.read(file_name);

			}
			catch (Exception e) {
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " loading " + current_image);
			}
		}
		else image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);

		return image;
	}

	/**
	 * save buffered image
	 */
	private synchronized boolean saveStoredImage(String format, String ending1, String ending2, String current_image, BufferedImage image,
			boolean file_name_is_path, boolean save_unclipped) {
		boolean was_error = false;

		// generate path name or use
		// value supplied by user
		current_image = removeIllegalFileNameCharacters(current_image);
		String file_name = current_image;
		String unclipped_file_name = "";

		if (file_name_is_path == false) {
			file_name = temp_dir + this.key + current_image;
			unclipped_file_name = temp_dir + this.key + 'R' + current_image;

			// log the type in our table so we can lookup
			this.image_type.put('R' + current_image, this.image_type.get(current_image));
		}

		// add ending if needed
		if ((file_name.toLowerCase().endsWith(ending1) == false) & (file_name.toLowerCase().endsWith(ending2) == false)) {
			file_name = file_name + ending1;
			unclipped_file_name = unclipped_file_name + ending1;
		}

		try { // write out data to create image in temp dir

			if (!JAIHelper.isJAIused() && format.equals("TIFF")) // we recommend JAI for tiffs
			this.images.write(image, "png", file_name);
			else this.images.write(image, format, file_name);

			// if it failed retry as RGB
			File f = new File(file_name);
			if (f.length() == 0) {

				image = ColorSpaceConvertor.convertToRGB(image);

				if (!JAIHelper.isJAIused() && format.equals("TIFF")) // we recommend JAI for tiffs
				this.images.write(image, "png", file_name);
				else this.images.write(image, format, file_name);
			}

			this.tempFileNames.put(file_name, "#");

			if (verifyFilesSaved) checkExists(file_name);

		}
		catch (Exception e) {
			e.printStackTrace();
			if (LogWriter.isOutput()) LogWriter.writeLog(" Exception " + e + " writing image " + image + " with type " + image.getType());
			was_error = true;

		}
		catch (Error ee) {

			if (LogWriter.isOutput()) LogWriter.writeLog("Error " + ee + " writing image " + image + " with type " + image.getType());

			was_error = true;

		}

		// save unclipped copy
		if (save_unclipped == true) {
			saveCopy(file_name, unclipped_file_name);
			this.tempFileNames.put(unclipped_file_name, "#");

		}

		return was_error;
	}

	/**
	 * delete all cached pages
	 */
	public synchronized static void flushPages() {

		try {

			Iterator filesTodelete = pagesOnDisk.keySet().iterator();
			while (filesTodelete.hasNext()) {
				Object file = filesTodelete.next();

				if (file != null) {
					File delete_file = new File((String) pagesOnDisk.get(file));
					if (delete_file.exists()) delete_file.delete();
				}
			}

			pagesOnDisk.clear();

			/**
			 * flush any pages serialized as bytes
			 */

			filesTodelete = pagesOnDiskAsBytes.keySet().iterator();
			while (filesTodelete.hasNext()) {
				Object file = filesTodelete.next();

				if (file != null) {
					File delete_file = new File((String) pagesOnDiskAsBytes.get(file));
					if (delete_file.exists()) delete_file.delete();
				}
			}

			pagesOnDiskAsBytes.clear();

			if (debugCache) System.out.println("Flush cache ");

		}
		catch (Exception e) {
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception " + e + " flushing files");
		}
	}

	@Override
	public void finalize() {

		try {
			super.finalize();
		}
		catch (Throwable e) {

			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}

		flush();

		/**
		 * try to redelete files again
		 */
		for (Object o : undeletedFiles.keySet()) {
			String file = ((String) o);

			File delete_file = new File(file);

			if (delete_file.delete()) undeletedFiles.remove(file);

		}
	}

	/**
	 * note may not actually be written by JVM immediately so do not rely for fast actions
	 */
	public static DynamicVectorRenderer getCachedPage(Integer key) {

		DynamicVectorRenderer currentDisplay = null;

		Object cachedFile = pagesOnDisk.get(key);

		if (debugCache) System.out.println("read from cache " + currentDisplay);

		if (cachedFile != null) {
			BufferedInputStream from;
			try {
				File fis = new File((String) cachedFile);
				from = new BufferedInputStream(new FileInputStream(fis));

				byte[] data = new byte[(int) fis.length()];
				from.read(data);
				from.close();
				currentDisplay = new SwingDisplay(data, new HashMap());

				//
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}
		return currentDisplay;
	}

	public static void cachePage(Integer key, DynamicVectorRenderer currentDisplay) {

		try {
			File ff = File.createTempFile("page", ".bin", new File(ObjectStore.temp_dir));
			// ff.deleteOnExit();

			BufferedOutputStream to = new BufferedOutputStream(new FileOutputStream(ff));
			to.write(currentDisplay.serializeToByteArray(null));
			to.flush();
			to.close();

			pagesOnDisk.put(key, ff.getAbsolutePath());

			if (debugCache) System.out.println("save to cache " + key + ' ' + ff.getAbsolutePath());

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
		}
	}

	public static byte[] getCachedPageAsBytes(String key) {

		byte[] data = null;

		Object cachedFile = pagesOnDiskAsBytes.get(key);

		if (cachedFile != null) {
			BufferedInputStream from;
			try {
				File fis = new File((String) cachedFile);
				from = new BufferedInputStream(new FileInputStream(fis));

				data = new byte[(int) fis.length()];
				from.read(data);
				from.close();

				//
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}
		return data;
	}

	public static void cachePageAsBytes(String key, byte[] bytes) {

		try {

			// if you already use key, delete it first now
			// as will not be removed otherwise
			if (pagesOnDiskAsBytes.containsKey(key)) {
				File delete_file = new File((String) pagesOnDiskAsBytes.get(key));
				if (delete_file.exists()) {
					delete_file.delete();

				}
			}

			File ff = File.createTempFile("bytes", ".bin", new File(ObjectStore.temp_dir));
			// ff.deleteOnExit();

			BufferedOutputStream to = new BufferedOutputStream(new FileOutputStream(ff));
			to.write(bytes);
			to.flush();
			to.close();

			// save to delete at end
			pagesOnDiskAsBytes.put(key, ff.getAbsolutePath());

			if (debugCache) System.out.println("save to cache " + key + ' ' + ff.getAbsolutePath());

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());

		}
	}

	public void saveRawImageData(String pageImgCount, byte[] bytes, int w, int h, int pX, int pY, byte[] maskCol, int colorSpaceID) {

		try {
			// System.out.println("ObjectStore.temp_dir="+ObjectStore.temp_dir);

			File ff = File.createTempFile("image", ".bin", new File(ObjectStore.temp_dir));
			// ff.deleteOnExit();

			BufferedOutputStream to = new BufferedOutputStream(new FileOutputStream(ff));
			to.write(bytes);
			to.flush();
			to.close();

			Integer key = new Integer(pageImgCount);
			this.imagesOnDiskAsBytes.put(key, ff.getAbsolutePath());
			this.imagesOnDiskAsBytesW.put(key, w);
			this.imagesOnDiskAsBytesH.put(key, h);

			this.imagesOnDiskAsBytespX.put(key, pX);
			this.imagesOnDiskAsBytespY.put(key, pY);
			this.imagesOnDiskMask.put(key, maskCol);
			this.imagesOnDiskColSpaceID.put(key, colorSpaceID);

			if (debugCache) System.out.println("save to image cache " + pageImgCount + ' ' + ff.getAbsolutePath());

		}
		catch (Exception e) {
			// tell user and log
			if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());

		}
	}

	/**
	 * see if image data saved
	 */
	public boolean isRawImageDataSaved(String number) {

		// System.out.println("isSaved="+imagesOnDiskAsBytes.get(new Integer(number))!=null);

		return this.imagesOnDiskAsBytes.get(new Integer(number)) != null;
	}

	/**
	 * retrieve byte data on disk
	 */
	public byte[] getRawImageData(String i) {

		byte[] data = null;

		Object cachedFile = this.imagesOnDiskAsBytes.get(new Integer(i));

		if (cachedFile != null) {
			BufferedInputStream from;
			try {
				File fis = new File((String) cachedFile);
				from = new BufferedInputStream(new FileInputStream(fis));

				data = new byte[(int) fis.length()];
				from.read(data);
				from.close();

				//
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}

		return data;
	}

	/**
	 * return parameter stored for image or null
	 */
	public Object getRawImageDataParameter(String imageID, Integer key) {

		if (key.equals(IMAGE_WIDTH)) {
			return this.imagesOnDiskAsBytesW.get(new Integer(imageID));
		}
		else
			if (key.equals(IMAGE_HEIGHT)) {
				return this.imagesOnDiskAsBytesH.get(new Integer(imageID));
			}
			else
				if (key.equals(IMAGE_pX)) {
					return this.imagesOnDiskAsBytespX.get(new Integer(imageID));
				}
				else
					if (key.equals(IMAGE_pY)) {
						return this.imagesOnDiskAsBytespY.get(new Integer(imageID));
					}
					else
						if (key.equals(IMAGE_MASKCOL)) {
							return this.imagesOnDiskMask.get(new Integer(imageID));
						}
						else
							if (key.equals(IMAGE_COLORSPACE)) {
								return this.imagesOnDiskColSpaceID.get(new Integer(imageID));
							}
							else return null;
	}

	public static File createTempFile(String filename) throws IOException {

		File tempURLFile;
        String prefix = filename;
        String suffix = "pdf";
        
        int separatorIndex = filename.lastIndexOf('.');
        if (separatorIndex >= 0) {
            prefix = filename.substring(0, separatorIndex);
            suffix = filename.substring(separatorIndex);
        }
        while (prefix.length() < 3) {
            prefix = prefix + 'a';
        }
		
		if (suffix.length() < 3) suffix = "pdf";

		tempURLFile = File.createTempFile(prefix, suffix, new File(ObjectStore.temp_dir));

		return tempURLFile;
	}

	/**
	 * Remove troublesome characters from temp file names.
	 * 
	 * @param s
	 */
	public static String removeIllegalFileNameCharacters(String s) {

		// Disabled for the time being. See case 9311 case 9316.
		// //reduce scope of fix for windows path used as Name of image
		// //as it breaks other files
		// if(s.indexOf(":")!=-1){//use indexOf!=-1 instead of contains for compatability with JAVAME
		// //s = s.replace('\\', '_');
		// s = s.replace('/', '_');
		// s = s.replace(':', '_');
		// s = s.replace('*', '_');
		// s = s.replace('?', '_');
		// s = s.replace('"','_');
		// s = s.replace('<','_');
		// s = s.replace('>','_');
		// s = s.replace('|','_');
		// }
		return s;
	}

	/**
	 * add file to list we delete on flush so we can clear any temp files we create
	 * 
	 * @param rawFileName
	 */
	public void setFileToDeleteOnFlush(String rawFileName) {
		this.tempFileNames.put(rawFileName, "#");
	}

	public String getKey() {
		return this.key;
	}
}
