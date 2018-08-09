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
 * StatusBar.java
 * ---------------
 */
package org.jpedal.io;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.jpedal.utils.LogWriter;

/**
 * encapsulates a status bar to display progess of a page decode and messages for a GUI client and methods to access it - See
 * org.examples.jpedal.viewer.Viewer for example of usage
 * 
 */
public class StatusBar {

	/** amount of detail to show */
	private static final int debug_level = 0;

	/** current numeric value of Progress bar */
	private int progress_size = 0;

	/** message to display */
	private String current = "";

	/** numeric value Progress bar will count to (lines in data file) */
	private static final int progress_max_size = 100;

	/** actual status bar */
	private JProgressBar status = null;

	/** if there is a GUI display */
	private boolean showMessages = false;

	/** amount done on decode */
	public float percentageDone = 0;

	/** master color for statusBar, by default is red */
	private Color masterColor = null;

	/** boolean to show when the status has been reset */
	private boolean reset = false;

	/** initialises statusbar using default colors */
	public StatusBar() {
		initialiseStatus("");
	}

	/** initialises statusbar using specified color */
	public StatusBar(Color newColor) {
		this.masterColor = newColor;
		initialiseStatus("");
	}

	// //////////////////////////////////////
	/**
	 * initiate status bar
	 */
	final public void initialiseStatus(String current) {
		this.progress_size = 0;
		this.status = new JProgressBar();
		if (this.masterColor != null) this.status.setForeground(this.masterColor);
		// show that somethings happerning but not sure how long for
		// status.setIndeterminate(true);
		this.status.setStringPainted(true);
		this.status.setMaximum(progress_max_size);
		this.status.setMinimum(0);
		updateStatus(current, 4);
	}

	// //////////////////////////////////////
	/**
	 * update status if client being used also writes to log (called internally as file decoded)
	 */
	final public void updateStatus(String progress_bar, int debug_level_to_use) {

		this.current = progress_bar;

		// update status if in client
		if (this.showMessages == true) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					StatusBar.this.status.setString(StatusBar.this.current);
					StatusBar.this.status.setValue(StatusBar.this.progress_size);
				}
			});

		}
		if (debug_level > debug_level_to_use && LogWriter.isOutput()) LogWriter.writeLog(progress_bar);
	}

	// ///////////////////////////////////////////
	/**
	 * return handle on status bar so it can be displayed
	 */
	final public Component getStatusObject() {
		return this.status;
	}

	/**
	 * set progress value (called internally as page decoded)
	 */
	final public void setProgress(int size) {
		this.reset = false;
		if (this.status != null) {
			if (size == 0) this.progress_size = 0;
			if (this.progress_size < size) this.progress_size = size;
			// if( showMessages == true ){
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					StatusBar.this.status.setValue(StatusBar.this.progress_size);

				}
			});
		}
	}

	// //////////////////////////////////////
	/**
	 * set progress value (called internally as page decoded)
	 */
	final public void setProgress(final String message, int size) {
		this.reset = false;
		if (this.status != null) {
			if (size == 0) this.progress_size = 0;
			if (this.progress_size < size) this.progress_size = size;
			// if( showMessages == true ){
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					StatusBar.this.status.setString(message);
					StatusBar.this.status.setValue(StatusBar.this.progress_size);

				}
			});
		}
	}

	// //////////////////////////////////////////////////////////
	/**
	 * reset status bar
	 */
	final public void resetStatus(String current) {
		this.reset = true;
		this.progress_size = 0;
		updateStatus(current, 4);
	}

	// ////////////////////////
	/**
	 * set client flag to display
	 */
	final public void setClientDisplay() {
		this.showMessages = true;
	}

	public void setVisible(boolean visible) {
		this.status.setVisible(visible);
	}

	public void setEnabled(boolean enable) {
		this.status.setEnabled(enable);
	}

	public boolean isVisible() {
		return this.status.isVisible();
	}

	public boolean isEnabled() {
		return this.status.isEnabled();
	}

	public boolean isDone() {
		return this.reset || this.progress_size >= progress_max_size;
	}

}
