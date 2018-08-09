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
 * ViewerBean.java
 * ---------------
 */

package org.jpedal.examples.viewer.javabean;

import java.io.File;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jpedal.examples.viewer.Commands;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.Viewer;

public class ViewerBean extends JPanel {

	private static final long serialVersionUID = -7585269902413817254L;

	private Viewer viewer;

	private File document = null;
	private Integer pageNumber = null;
	private Integer rotation = null;
	private Integer zoom = null;

	private Boolean isMenuBarVisible = null;
	private Boolean isToolBarVisible = null;
	private Boolean isDisplayOptionsBarVisible = null;
	private Boolean isSideTabBarVisible = null;
	private Boolean isNavigationBarVisible = null;

	public ViewerBean() {
		this.viewer = new Viewer(this, Viewer.PREFERENCES_BEAN);
		this.viewer.setupViewer();
	}

	public Viewer getViewer() {
		return this.viewer;
	}

	// Document ////////
	public void setDocument(final File document) {
		this.document = document;

		excuteCommand(Commands.OPENFILE, new String[] { String.valueOf(document) });

		if (this.pageNumber != null) {
			excuteCommand(Commands.GOTO, new String[] { String.valueOf(this.pageNumber) });
		}

		if (this.rotation != null) {
			excuteCommand(Commands.ROTATION, new String[] { String.valueOf(this.rotation) });
		}

		if (this.zoom != null) {
			excuteCommand(Commands.SCALING, new String[] { String.valueOf(this.zoom) });
		}
		else {
			excuteCommand(Commands.SCALING, new String[] { String.valueOf(100) });
		}

		if (this.isMenuBarVisible != null) {
			setMenuBar(this.isMenuBarVisible);
		}

		if (this.isToolBarVisible != null) {
			setToolBar(this.isToolBarVisible);
		}

		if (this.isDisplayOptionsBarVisible != null) {
			setDisplayOptionsBar(this.isDisplayOptionsBarVisible);
		}

		if (this.isSideTabBarVisible != null) {
			setSideTabBar(this.isSideTabBarVisible);
		}

		if (this.isNavigationBarVisible != null) {
			setNavigationBar(this.isNavigationBarVisible);
		}
	}

	// Page Number ////////
	public int getPageNumber() {
		if (this.pageNumber == null) return 1;
		else return this.pageNumber;
	}

	public void setPageNumber(final int pageNumber) {
		this.pageNumber = pageNumber;

		if (this.document != null) {
			excuteCommand(Commands.GOTO, new String[] { String.valueOf(pageNumber) });
		}
	}

	// Rotation ////////
	public int getRotation() {
		if (this.rotation == null) return 0;
		else return this.rotation;
	}

	public void setRotation(final int rotation) {
		this.rotation = rotation;

		if (this.document != null) {
			excuteCommand(Commands.ROTATION, new String[] { String.valueOf(rotation) });
		}
	}

	// Zoom ////////
	public int getZoom() {
		if (this.zoom == null) return 100;
		else return this.zoom;
	}

	public void setZoom(int zoom) {
		this.zoom = zoom;

		if (this.document != null) {
			excuteCommand(Commands.SCALING, new String[] { String.valueOf(zoom) });
		}
	}

	// setToolBar, setDisplayOptionsBar, setSideTabBar, setNavigationBar,
	public void setMenuBar(boolean visible) {
		this.isMenuBarVisible = visible;

		// if(document != null)
		this.viewer.executeCommand(Commands.UPDATEGUILAYOUT, new Object[] { "ShowMenubar", visible });
	}

	public boolean getMenuBar() {
		if (this.isMenuBarVisible == null) return true;
		else return this.isMenuBarVisible;
	}

	public void setToolBar(boolean visible) {
		this.isToolBarVisible = visible;

		// @kieran
		// I did not write this class so not familiar with it
		// Did you write or or Simon?
		// is a null document goint to cause any issues in MAtisse?
		// if(document != null)
		this.viewer.executeCommand(Commands.UPDATEGUILAYOUT, new Object[] { "ShowButtons", visible });
	}

	public boolean getToolBar() {
		if (this.isToolBarVisible == null) return true;
		else return this.isToolBarVisible;
	}

	public void setDisplayOptionsBar(boolean visible) {
		this.isDisplayOptionsBarVisible = visible;

		// if(document != null)
		this.viewer.executeCommand(Commands.UPDATEGUILAYOUT, new Object[] { "ShowDisplayoptions", visible });
	}

	public boolean getDisplayOptionsBar() {
		if (this.isDisplayOptionsBarVisible == null) return true;
		else return this.isDisplayOptionsBarVisible;
	}

	public void setSideTabBar(boolean visible) {
		this.isSideTabBarVisible = visible;

		// if(document != null)
		this.viewer.executeCommand(Commands.UPDATEGUILAYOUT, new Object[] { "ShowSidetabbar", visible });
	}

	public boolean getSideTabBar() {
		if (this.isSideTabBarVisible == null) return true;
		else return this.isSideTabBarVisible;
	}

	public void setNavigationBar(boolean visible) {
		this.isNavigationBarVisible = visible;

		// if(document != null)
		this.viewer.executeCommand(Commands.UPDATEGUILAYOUT, new Object[] { "ShowNavigationbar", visible });
	}

	public boolean getNavigationBar() {
		if (this.isNavigationBarVisible == null) return true;
		else return this.isNavigationBarVisible;
	}

	private void excuteCommand(final int command, final Object[] input) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ViewerBean.this.viewer.executeCommand(command, input);

				while (Values.isProcessing()) {
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				repaint();
			}
		});
	}

	// // Page Layout ////////
	// private String pageLayout = "Single";
	//
	// public String getPageLayout() {
	// return pageLayout;
	// }
	//
	// public void setPageLayout(String pageLayout) {
	// this.pageLayout = pageLayout;
	// }
}