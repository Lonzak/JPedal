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
 * LogWriter.java
 * ---------------
 */
package org.jpedal.utils;

import java.io.FileWriter;
import java.io.PrintWriter;

import org.jpedal.PdfDecoder;

/**
 * <p>
 * logs all activity. And some low level variables/methods as it is visible to all classes.
 * <p>
 * Provided for debugging and NOT officially part of the API
 */
public class LogWriter {

	/** allow user to scan log output */
	public static LogScanner logScanner = null;

	/** amount of debugging detail we put in log */
	public static boolean debug = false;

	/** filename of logfile */
	static public String log_name = null;

	/** flag we can set to signal code being tested */
	static public boolean testing = false;

	/** if we echo to console. VERY USEFUL for debugging */
	private static boolean verbose = false;

	/**
	 * reset logfile
	 */
	final public static void resetLogFile() {
		if (log_name != null) {
			// write message
			PrintWriter log_file;
			try {
				log_file = new PrintWriter(new FileWriter(log_name, false));
				log_file.println(TimeNow.getTimeNow() + " Running Storypad");
				log_file.flush();
				log_file.close();
			}
			catch (Exception e) {
				System.err.println("Exception " + e + " attempting to write to log file " + log_name);
			}
		}
	}

	final public static boolean isOutput() {
		return verbose || logScanner != null;
	}

	// /////////////////////////////////////////////
	final public static void writeLog(String message) {

		// implement your own version of org.jpedal.utils.LogScanner
		// and set will then allow you to track any error messages
		if (logScanner != null) logScanner.message(message);

		/**
		 * write message to pane if client active and put to front
		 */
		if (verbose == true) System.out.println(message);

		if (log_name != null) {

			// write message
			PrintWriter log_file;
			try {
				log_file = new PrintWriter(new FileWriter(log_name, true));

				if (!testing) log_file.println(TimeNow.getTimeNow() + ' ' + message); // write date to the log

				log_file.println(message);
				log_file.flush();
				log_file.close();
			}
			catch (Exception e) {
				System.err.println("Exception " + e + " attempting to write to log file " + log_name);
			}

		}
	}

	// ////////////////////////////////////////////
	/**
	 * setup log file and check it is readable also sets command line options
	 */
	final public static void setupLogFile(String command_line_values) {

		if (command_line_values != null) {

			// verbose mode echos to screen
			if (command_line_values.indexOf('v') != -1) {
				verbose = true;
				writeLog("Verbose on");
			}
			else verbose = false;

		}

		// write out info
		if (!testing) {
			writeLog("Software version - " + PdfDecoder.version);
			writeLog("Software started - " + TimeNow.getTimeNow());
		}
		writeLog("=======================================================");
	}

	// /////////////////////////////////////////////////////////
	/**
	 * write out logging information for forms, <b>print</b> is a boolean flag, if true prints to the screen
	 */
	public static void writeFormLog(String message, boolean print) {
		if (print) System.out.println("[forms] " + message);

		writeLog("[forms] " + message);
	}
}
