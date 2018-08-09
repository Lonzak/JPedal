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
 * LayerDecoder.java
 * ---------------
 */

package org.jpedal.parser;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.objects.GraphicsState;
import org.jpedal.objects.layers.PdfLayerList;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfObject;
import org.jpedal.render.DynamicVectorRenderer;

public class LayerDecoder {

	private boolean isLayerVisible = true;

	int layerLevel = 0;

	private Map layerVisibility = new HashMap(50);

	private Map layerClips = new HashMap(50);

	PdfLayerList layers;

	public boolean isLayerVisible() {
		return this.isLayerVisible;
	}

	public void BMC() {
		this.layerLevel++;
	}

	public void BDC(PdfObject BDCobj, GraphicsState gs, DynamicVectorRenderer current, int dataPointer, byte[] raw, boolean hasDictionary,
			int rawStart) {

		this.layerLevel++;

		// add in layer if visible
		if (this.layers != null && this.isLayerVisible) {

			String name = "";

			if (hasDictionary) {
				// see if name and if shown
				name = BDCobj.getName(PdfDictionary.OC);

				// see if Layer defined and get title if no Name as alternative
				if (name == null) {

					PdfObject layerObj = BDCobj.getDictionary(PdfDictionary.Layer);
					if (layerObj != null) name = layerObj.getTextStreamValue(PdfDictionary.Title);
				}

				// needed to flags its a BMC
				this.layerClips.put(this.layerLevel, null);

				// apply any clip, saving old to restore on EMC
				float[] BBox = BDCobj.getFloatArray(PdfDictionary.BBox);
				if (BBox != null) {
					Area currentClip = gs.getClippingShape();

					// store so we restore in EMC
					if (currentClip != null) this.layerClips.put(this.layerLevel, currentClip.clone());

					Area clip = new Area(new Rectangle2D.Float(BBox[0], BBox[1], -gs.CTM[2][0] + (BBox[2] - BBox[0]), -gs.CTM[2][1]
							+ (BBox[3] - BBox[1])));
					gs.setClippingShape(clip);

					current.drawClip(gs, clip, true);
				}
			}
			else { // direct just /OC and /MCxx

				// find /OC
				name = readOPName(dataPointer, raw, rawStart, name);
			}

			if (name != null && name.length() > 0) // name referring to Layer or Title
			this.isLayerVisible = this.layers.decodeLayer(name, true);

			// flag so we can next values
			if (this.isLayerVisible) this.layerVisibility.put(this.layerLevel, "x");

		}
	}

	private static String readOPName(int dataPointer, byte[] raw, int rawStart, String name) {
		for (int ii = rawStart; ii < dataPointer; ii++) {
			if (raw[ii] == '/' && raw[ii + 1] == 'O' && raw[ii + 2] == 'C') { // find oc

				ii = ii + 2;
				// roll onto value
				while (raw[ii] != '/')
					ii++;

				ii++; // roll pass /

				int strStart = ii, charCount = 0;

				while (ii < dataPointer) {
					ii++;
					charCount++;

					if (raw[ii] == 13 || raw[ii] == 10 || raw[ii] == 32 || raw[ii] == '/') break;
				}

				name = new String(raw, strStart, charCount);

			}
		}
		return name;
	}

	public void EMC(DynamicVectorRenderer current, GraphicsState gs) {

		// remove any clip
		Integer key = this.layerLevel;
		if (this.layerClips.containsKey(key)) {

			Area currentClip = (Area) this.layerClips.get(key);

			gs.setClippingShape(currentClip);
			current.drawClip(gs, currentClip, true);
		}

		this.layerLevel--;

		// reset flag
		this.isLayerVisible = this.layers == null || this.layerLevel == 0 || this.layerVisibility.containsKey(this.layerLevel);
	}

	public void setPdfLayerList(PdfLayerList layers) {
		this.layers = layers;
	}

	public Object getPdfLayerList() {
		return this.layers;
	}
}
