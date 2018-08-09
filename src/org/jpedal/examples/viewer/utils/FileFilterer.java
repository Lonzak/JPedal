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
 * FileFilterer.java
 * ---------------
 */
package org.jpedal.examples.viewer.utils;

import java.io.File;

/**
 * Used in GUI example code Provides filters for save dialogs
 * 
 * (based on p362 in Oreilly swing)
 */
public class FileFilterer extends javax.swing.filechooser.FileFilter {

	String[] extensions;
	String description;

	// number off possible values
	int items = 0;

	// setup file and descriptor
	public FileFilterer(String[] ext, String desc) {
		this.items = ext.length;

		// setup as lower case list
		this.extensions = new String[this.items];
		for (int i = 0; i < this.items; i++) {
			this.extensions[i] = ext[i].toLowerCase();

			// and add a description
			this.description = desc;
		}
	}

	@Override
	final public String getDescription() {
		return this.description;
	}

	@Override
	final public boolean accept(File f) {
		boolean accept_flag = false;

		// allow directories
		if (f.isDirectory()) accept_flag = true;
		else {
			// check file against list
			String file_name = f.getName().toLowerCase();
			for (int i = 0; i < this.items; i++) {
				if (file_name.endsWith(this.extensions[i])) accept_flag = true;
			}
		}
		return accept_flag;
	}
}
