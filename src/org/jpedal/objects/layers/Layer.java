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
 * Layer.java
 * ---------------
 */

package org.jpedal.objects.layers;

/**
 * used by JavaScript for access
 */
public class Layer {

	public static boolean debugLayer = false;

	private PdfLayerList layerList;

	public String name;

	Layer(String name, PdfLayerList layerList) {

		this.name = name;
		this.layerList = layerList;
	}

	public void setAction(String js) {
		this.layerList.addJScommand(this.name, js);
	}

	public boolean getState() {
		return this.layerList.isVisible(this.name);
	}

	public void setState(boolean state) {

		boolean currentValue = this.layerList.isVisible(this.name);

		this.layerList.setVisiblity(this.name, state);

		// tell JPedal we need to update
		if (currentValue != state) {

			if (debugLayer) System.out.println(this.name + ' ' + state);

			this.layerList.setChangesMade(true);

		}
	}

}
