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
 * PdfLayerList.java
 * ---------------
 */
package org.jpedal.objects.layers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.jpedal.io.PdfObjectReader;
import org.jpedal.objects.raw.OCObject;
import org.jpedal.objects.raw.PdfDictionary;
import org.jpedal.objects.raw.PdfKeyPairsIterator;
import org.jpedal.objects.raw.PdfObject;

public class PdfLayerList {

	private static boolean debug = false;

	/** page we have outlines for */
	private int OCpageNumber = -1;

	private String padding = "";

	// used in tree as unique separator
	public static String deliminator = "" + (char) 65535;

	private Map layerNames = new LinkedHashMap();

	private Map streamToName = new HashMap();

	private Map layersEnabled = new HashMap();

	private Map jsCommands = null;

	private Map metaData = new HashMap();

	private Map layersTested = new HashMap();

	private Map layerLocks = new HashMap();

	private boolean changesMade = false;

	private Map propertyMap, refToPropertyID, refTolayerName, RBconstraints;

	private Map minScale = new HashMap();
	private Map maxScale = new HashMap();

	// private float scaling=1f;

	private int layerCount = 0;
	private Object[] order;

	private PdfObjectReader currentPdfFile = null;

	private Layer[] layers = null;

	/**
	 * add layers and settings to list
	 * 
	 * @param OCProperties
	 * @param PropertiesObj
	 * @param currentPdfFile
	 * @param pageNumber
	 */
	public void init(PdfObject OCProperties, PdfObject PropertiesObj, PdfObjectReader currentPdfFile, int pageNumber) {

		this.OCpageNumber = pageNumber;

		this.propertyMap = new HashMap();
		this.refToPropertyID = new HashMap();
		this.refTolayerName = new HashMap();
		this.RBconstraints = new HashMap();

		this.currentPdfFile = currentPdfFile;

		if (PropertiesObj != null) setupOCMaps(PropertiesObj, currentPdfFile);

		PdfObject layerDict = OCProperties.getDictionary(PdfDictionary.D);

		if (layerDict == null) return;

		int OCBaseState = layerDict.getNameAsConstant(PdfDictionary.BaseState);

		// if not set use default
		if (OCBaseState == PdfDictionary.Unknown) OCBaseState = PdfDictionary.ON;

		// read order first and may be over-written by ON/OFF
		this.order = layerDict.getObjectArray(PdfDictionary.Order);

		if (debug) {
			System.out.println("PropertiesObj=" + PropertiesObj);
			System.out.println("layerDict=" + layerDict);
			System.out.println("propertyMap=" + this.propertyMap);
			System.out.println("propertyMap=" + this.propertyMap);
			System.out.println("refToPropertyID=" + this.refToPropertyID);
			System.out.println("refTolayerName=" + this.refTolayerName);

			System.out.println("OCBaseState=" + OCBaseState + " (ON=" + PdfDictionary.ON + ')');

			System.out.println("order=" + this.order);

			showValues("ON=", PdfDictionary.ON, layerDict);

			showValues("OFF=", PdfDictionary.OFF, layerDict);

			showValues("RBGroups=", PdfDictionary.RBGroups, layerDict);

		}
		/**
		 * workout list of layers (can be in several places)
		 */

		addLayer(OCBaseState, this.order, null);

		// read the ON and OFF values
		if (OCBaseState != PdfDictionary.ON) // redundant if basestate on
		addLayer(PdfDictionary.ON, layerDict.getKeyArray(PdfDictionary.ON), null);

		if (OCBaseState != PdfDictionary.OFF) // redundant if basestate off
		addLayer(PdfDictionary.OFF, layerDict.getKeyArray(PdfDictionary.OFF), null);

		/**
		 * handle case where layers not explicitly switched on
		 */
		if (OCBaseState == PdfDictionary.ON) {// && layerDict.getKeyArray(PdfDictionary.OFF)==null){
			Iterator keys = this.refToPropertyID.keySet().iterator();
			Object ref, layerName;
			while (keys.hasNext()) {
				ref = keys.next();
				layerName = this.refToPropertyID.get(ref);

				this.refTolayerName.put(ref, layerName);

				if (!this.layersTested.containsKey(layerName)) {
					this.layersTested.put(layerName, "x");
					this.layersEnabled.put(layerName, "x");
				}
			}
		}

		// set any locks
		setLocks(currentPdfFile, layerDict.getKeyArray(PdfDictionary.Locked));

		// any constraints
		setConstraints(layerDict.getKeyArray(PdfDictionary.RBGroups));

		// any Additional Dictionaries
		setAS(layerDict.getKeyArray(PdfDictionary.AS), currentPdfFile);

		/**
		 * read any metadata
		 */
		int[] keys = { PdfDictionary.Name, PdfDictionary.Creator };
		String[] titles = { "Name", "Creator" };

		int count = keys.length;
		String val;
		for (int jj = 0; jj < count; jj++) {
			val = layerDict.getTextStreamValue(keys[jj]);
			if (val != null) this.metaData.put(titles[jj], val);
		}

		// list mode if set
		val = layerDict.getName(PdfDictionary.ListMode);
		if (val != null) this.metaData.put("ListMode", val);
	}

	private static void showValues(String s, int key, PdfObject layerDict) {

		byte[][] keyValues = layerDict.getKeyArray(key);
		if (keyValues != null) {

			String values = "";
			for (byte[] keyValue : keyValues) {
				if (keyValue == null) values = values + "null ";
				else values = values + new String(keyValue) + ' ';
			}

			System.out.println(s + values);

		}
	}

	/**
	 * used by Javascript to flag that state has changed
	 * 
	 * @param flag
	 */
	public void setChangesMade(boolean flag) {
		this.changesMade = flag;
	}

	/**
	 * build a list of constraints using layer names so we can switch off if needed
	 * 
	 * @param layer
	 */
	private void setConstraints(byte[][] layer) {

		if (layer == null) return;

		int layerCount = layer.length;

		// turn into list of names
		String[] layers = new String[layerCount];
		for (int ii = 0; ii < layerCount; ii++) {

			String ref = new String(layer[ii]);
			layers[ii] = (String) this.refTolayerName.get(ref);
		}

		for (int ii = 0; ii < layerCount; ii++) {

			if (isLayerName(layers[ii])) {

				String effectedLayers = "";
				for (int ii2 = 0; ii2 < layerCount; ii2++) {

					if (ii == ii2) continue;

					effectedLayers = effectedLayers + layers[ii2] + ',';
				}

				this.RBconstraints.put(layers[ii], effectedLayers);

			}
		}
	}

	/**
	 * create list for lookup
	 */
	private void setupOCMaps(PdfObject propertiesObj, PdfObjectReader currentPdfFile) {

		PdfKeyPairsIterator keyPairs = propertiesObj.getKeyPairsIterator();

		String glyphKey, ref;
		PdfObject glyphObj;

		while (keyPairs.hasMorePairs()) {

			glyphKey = keyPairs.getNextKeyAsString();

			glyphObj = keyPairs.getNextValueAsDictionary();
			ref = glyphObj.getObjectRefAsString();

			currentPdfFile.checkResolved(glyphObj);

			byte[][] childPairs = glyphObj.getKeyArray(PdfDictionary.OCGs);

			if (childPairs != null) setupchildOCMaps(childPairs, glyphKey, currentPdfFile);
			else {
				this.propertyMap.put(ref, glyphObj);

				String currentNames = (String) this.refToPropertyID.get(ref);
				if (currentNames == null) this.refToPropertyID.put(ref, glyphKey);
				else this.refToPropertyID.put(ref, currentNames + ',' + glyphKey);
			}
			// roll on
			keyPairs.nextPair();
		}
	}

	private void setupchildOCMaps(byte[][] keys, String glyphKey, PdfObjectReader currentPdfFile) {

		String ref;
		PdfObject glyphObj;

		for (byte[] key : keys) {

			ref = new String(key);
			glyphObj = new OCObject(ref);

			currentPdfFile.readObject(glyphObj);

			currentPdfFile.checkResolved(glyphObj);

			byte[][] childPairs = glyphObj.getKeyArray(PdfDictionary.OCGs);

			// System.out.println(glyphKey+" === "+glyphObj+" childPropertiesObj="+childPairs);

			if (childPairs != null) setupchildOCMaps(childPairs, glyphKey, currentPdfFile);
			else {

				this.propertyMap.put(ref, glyphObj);
				String currentNames = (String) this.refToPropertyID.get(ref);
				if (currentNames == null) this.refToPropertyID.put(ref, glyphKey);
				else this.refToPropertyID.put(ref, currentNames + ',' + glyphKey);
				// System.out.println("Add key "+glyphKey+" "+refToPropertyID);
			}
		}
	}

	private void addLayer(int status, Object[] layer, String parentName) {

		if (layer == null) return;

		if (debug) this.padding = this.padding + "   ";

		int layers = layer.length;

		String ref, name, layerName = null;

		PdfObject nextObject;

		for (int ii = 0; ii < layers; ii++) {

			if (layer[ii] instanceof String) {
				// ignore
			}
			else
				if (layer[ii] instanceof byte[]) {

					byte[] rawRef = (byte[]) layer[ii];
					ref = new String(rawRef);
					name = (String) this.refToPropertyID.get(ref);

					nextObject = (PdfObject) this.propertyMap.get(ref);

					if (nextObject == null) {

						if (rawRef != null && rawRef[rawRef.length - 1] == 'R') {
							nextObject = new OCObject(ref);
							this.currentPdfFile.readObject(nextObject);
							name = ref;
						}
						else { // it is a name for the level so add into path of name

							if (parentName == null) parentName = ref;
							else parentName = ref + deliminator + parentName;
						}
					}

					if (nextObject != null) {

						this.layerCount++;

						layerName = nextObject.getTextStreamValue(PdfDictionary.Name);

						if (parentName != null) layerName = layerName + deliminator + parentName;

						if (debug) System.out.println(this.padding + "[layer1] add layer=" + layerName + " ref=" + ref + " parent=" + parentName
								+ " refToLayerName=" + this.refTolayerName.get(ref) + " ref=" + ref);

						this.refTolayerName.put(ref, layerName);

						// and write back name value
						layer[ii] = layerName;

						this.layerNames.put(layerName, status);
						if (name.indexOf(',') == -1) {
							String oldValue = (String) this.streamToName.get(name);
							if (oldValue == null) this.streamToName.put(name, layerName);
							else this.streamToName.put(name, oldValue + ',' + layerName);
						}
						else {
							StringTokenizer names = new StringTokenizer(name, ",");
							while (names.hasMoreTokens()) {
								name = names.nextToken();
								String oldValue = (String) this.streamToName.get(name);
								if (oldValue == null) this.streamToName.put(name, layerName);
								else this.streamToName.put(name, oldValue + ',' + layerName);
							}
						}

						// must be done as can be defined in order with default and then ON/OFF as well
						if (status == PdfDictionary.ON) {
							this.layersEnabled.put(layerName, "x");
						}
						else {
							this.layersEnabled.remove(layerName);
						}
					}
				}
				else addLayer(status, (Object[]) layer[ii], layerName);
		}

		if (debug) {
			int len = this.padding.length();

			if (len > 3) this.padding = this.padding.substring(0, len - 3);
		}
	}

	private void addLayer(int status, byte[][] layer, String parentName) {

		if (layer == null) return;

		String ref, name;

		PdfObject nextObject;

		for (byte[] aLayer : layer) {

			ref = new String(aLayer);
			name = (String) this.refToPropertyID.get(ref);
			nextObject = (PdfObject) this.propertyMap.get(ref);

			if (nextObject != null) {

				this.layerCount++;

				String layerName = nextObject.getTextStreamValue(PdfDictionary.Name);

				if (parentName != null) layerName = layerName + deliminator + parentName;

				// pick up full name set by Order
				if (status == PdfDictionary.ON || status == PdfDictionary.OFF) {
					String possName = (String) this.refTolayerName.get(ref);
					if (possName != null) layerName = possName;
				}

				if (debug) System.out.println(this.padding + "[layer0] add layer=" + layerName + " ref=" + ref + " parent=" + parentName
						+ " refToLayerName=" + this.refTolayerName.get(ref) + " status=" + status);

				if (this.refTolayerName.get(ref) == null) {

					this.refTolayerName.put(ref, layerName);

					this.layerNames.put(layerName, status);
				}

				if (this.streamToName.get(name) != null) {// ignore if done
				}
				else
					if (name.indexOf(',') == -1) {
						String oldValue = (String) this.streamToName.get(name);
						if (oldValue == null) this.streamToName.put(name, layerName);
						else this.streamToName.put(name, oldValue + ',' + layerName);
					}
					else {
						StringTokenizer names = new StringTokenizer(name, ",");
						while (names.hasMoreTokens()) {
							name = names.nextToken();
							String oldValue = (String) this.streamToName.get(name);
							if (oldValue == null) this.streamToName.put(name, layerName);
							else this.streamToName.put(name, oldValue + ',' + layerName);
						}
					}

				// must be done as can be defined in order with default and then ON/OFF as well
				if (status == PdfDictionary.ON) this.layersEnabled.put(layerName, "x");
				else this.layersEnabled.remove(layerName);

				this.layersTested.put(layerName, "x");
			}
		}
	}

	private void setAS(byte[][] AS, PdfObjectReader currentPdfFile) {

		if (AS == null) return;

		int event;

		String ref, name, layerName;

		byte[][] OCGs;

		PdfObject nextObject;

		for (byte[] A : AS) {

			// can also be a direct command which is not yet implemented
			if (A == null) {
				continue;
			}

			ref = new String(A);

			nextObject = new OCObject(ref);
			if (A[0] == '<') nextObject.setStatus(PdfObject.UNDECODED_DIRECT);
			else nextObject.setStatus(PdfObject.UNDECODED_REF);

			// must be done AFTER setStatus()
			nextObject.setUnresolvedData(A, PdfDictionary.AS);
			currentPdfFile.checkResolved(nextObject);

			event = nextObject.getParameterConstant(PdfDictionary.Event);
			if (nextObject != null) {

				if (event == PdfDictionary.View) {
					OCGs = nextObject.getKeyArray(PdfDictionary.OCGs);

					if (OCGs != null) {

						for (byte[] OCG : OCGs) {

							ref = new String(OCG);
							nextObject = new OCObject(ref);
							if (OCG[0] == '<') {
								nextObject.setStatus(PdfObject.UNDECODED_DIRECT);
							}
							else nextObject.setStatus(PdfObject.UNDECODED_REF);

							// must be done AFTER setStatus()
							nextObject.setUnresolvedData(OCG, PdfDictionary.OCGs);
							currentPdfFile.checkResolved(nextObject);

							layerName = nextObject.getTextStreamValue(PdfDictionary.Name);
							name = (String) this.refToPropertyID.get(ref);

							this.streamToName.put(name, layerName);

							// System.out.println((char)OCGs[jj][0]+" "+ref+" "+" "+nextObject+" "+nextObject.getTextStreamValue(PdfDictionary.Name));

							PdfObject usageObj = nextObject.getDictionary(PdfDictionary.Usage);

							if (usageObj != null) {
								PdfObject zoomObj = usageObj.getDictionary(PdfDictionary.Zoom);

								// set zoom values
								if (zoomObj != null) {
									float min = zoomObj.getFloatNumber(PdfDictionary.min);
									if (min != 0) {
										this.minScale.put(layerName, min);
									}
									float max = zoomObj.getFloatNumber(PdfDictionary.max);

									if (max != 0) {
										this.maxScale.put(layerName, max);
									}
								}
							}
						}
					}
				}
				else {}
				// layerCount++;

				// String layerName=nextObject.getTextStreamValue(PdfDictionary.Name);

				// if(debug)
				// System.out.println("[AS] add AS="+layerName);

				// refTolayerName.put(ref,layerName);

				// layerNames.put(layerName,new Integer(status));

				// if(layerName.indexOf(",")==-1){
				// String oldValue=(String)streamToName.get(layerName);
				// if(oldValue==null)
				// streamToName.put(layerName,layerName);
				// else
				// streamToName.put(layerName,oldValue+","+layerName);
				// }else{
				// StringTokenizer names=new StringTokenizer(layerName,",");
				// while(names.hasMoreTokens()){
				// layerName=names.nextToken();
				// String oldValue=(String)streamToName.get(layerName);
				// if(oldValue==null)
				// streamToName.put(layerName,layerName);
				// else
				// streamToName.put(layerName,oldValue+","+layerName);
				// }
				// }

			}
		}
	}

	private void setLocks(PdfObjectReader currentPdfFile, byte[][] layer) {

		if (layer == null) return;

		for (byte[] aLayer : layer) {

			String nextValue = new String(aLayer);

			PdfObject nextObject = new OCObject(nextValue);

			currentPdfFile.readObject(nextObject);

			String layerName = nextObject.getTextStreamValue(PdfDictionary.Name);

			this.layerLocks.put(layerName, "x");

		}
	}

	public Map getMetaData() {
		return this.metaData;
	}

	public Object[] getDisplayTree() {

		if (this.order != null) return this.order;
		else return getNames();
	}

	/**
	 * return list of layer names as String array
	 */
	private String[] getNames() {

		int count = this.layerNames.size();
		String[] nameList = new String[count];

		Iterator names = this.layerNames.keySet().iterator();

		int jj = 0;
		while (names.hasNext()) {
			nameList[jj] = names.next().toString();
			jj++;
		}

		return nameList;
	}

	/**
	 * will display only these layers and hide all others and will override any constraints. If you pass null in, all layers will be removed
	 * 
	 * @param layerNames
	 */
	public void setVisibleLayers(String[] layerNames) {

		this.layersEnabled.clear();

		if (layerNames != null) {

			for (String layerName : layerNames)
				this.layersEnabled.put(layerName, "x");
		}

		// flag it has been altered
		this.changesMade = true;
	}

	/**
	 * Used internally only. takes name in Stream (ie MC7 and works out if we need to decode) if isID==true
	 */
	public boolean decodeLayer(String name, boolean isID) {

		if (this.layerCount == 0) return true;

		boolean isLayerVisible = false;

		String layerName = name;

		// see if match found otherwise assume name
		if (isID) {
			String mappedName = (String) this.streamToName.get(name);

			if (mappedName != null) layerName = mappedName;
		}

		if (layerName == null) return false;
		else {

			// if multiple layers them comma separated list
			if (layerName.indexOf(',') == -1) {
				isLayerVisible = this.layersEnabled.containsKey(layerName);

				if (isLayerVisible) isLayerVisible = hiddenByParent(isLayerVisible, layerName);

			}
			else {
				StringTokenizer names = new StringTokenizer(layerName, ",");
				while (names.hasMoreTokens()) {

					String nextName = names.nextToken();
					isLayerVisible = this.layersEnabled.containsKey(nextName);

					if (isLayerVisible) isLayerVisible = hiddenByParent(isLayerVisible, nextName);

					if (isLayerVisible) // exit on first match
					break;
				}
			}

			if (debug) System.out.println("[isVisible] " + name + " decode=" + isLayerVisible + " enabled=" + this.layersEnabled + " layerName="
					+ layerName + " isEnabled=" + this.layersEnabled);
			// System.out.println("stream="+streamToName);

			return isLayerVisible;
		}
	}

	// check not disabled by Parent up tree
	private boolean hiddenByParent(boolean layerVisible, String layerName) {

		int id = layerName.indexOf(deliminator);

		if (layerVisible && id != -1) {

			String parent = layerName.substring(id + 1, layerName.length());

			while (parent != null && layerVisible && isLayerName(parent)) {

				layerVisible = decodeLayer(parent, false);

				layerName = parent;
				id = layerName.indexOf(deliminator);
				if (id == -1) parent = null;
				else parent = layerName.substring(id + 1, layerName.length());
			}
		}

		return layerVisible;
	}

	/**
	 * switch on/off layers based on Zoom
	 * 
	 * @param scaling
	 */
	public boolean setZoom(float scaling) {

		String layerName;
		Iterator minZoomLayers = this.minScale.keySet().iterator();
		while (minZoomLayers.hasNext()) {

			layerName = (String) minZoomLayers.next();
			Float minScalingValue = (Float) this.minScale.get(layerName);

			// Zoom off
			if (minScalingValue != null) {

				// System.out.println(layerName+" "+scaling+" "+minScalingValue);

				if (scaling < minScalingValue) {
					this.layersEnabled.remove(layerName);
					this.changesMade = true;
				}
				else
					if (!this.layersEnabled.containsKey(layerName)) {
						this.layersEnabled.put(layerName, "x");
						this.changesMade = true;
					}
			}
		}

		Iterator maxZoomLayers = this.maxScale.keySet().iterator();
		while (maxZoomLayers.hasNext()) {

			layerName = (String) minZoomLayers.next();
			Float maxScalingValue = (Float) this.maxScale.get(layerName);
			if (maxScalingValue != null) {
				if (scaling > maxScalingValue) {
					this.layersEnabled.remove(layerName);
					this.changesMade = true;
				}
				else
					if (!this.layersEnabled.containsKey(layerName)) {
						this.layersEnabled.put(layerName, "x");
						this.changesMade = true;
					}
			}
		}

		return this.changesMade;
	}

	public boolean isVisible(String layerName) {
		return this.layersEnabled.containsKey(layerName);
	}

	public void setVisiblity(String layerName, boolean isVisible) {

		if (debug) System.out.println("[layer] setVisiblity=" + layerName + " isVisible=" + isVisible);

		if (isVisible) {
			this.layersEnabled.put(layerName, "x");

			// disable any other layers
			String layersToDisable = (String) this.RBconstraints.get(layerName);
			if (layersToDisable != null) {
				StringTokenizer layers = new StringTokenizer(layersToDisable, ",");
				while (layers.hasMoreTokens())
					this.layersEnabled.remove(layers.nextToken());
			}
		}
		else this.layersEnabled.remove(layerName);

		// flag it has been altered
		this.changesMade = true;
	}

	public boolean isVisible(PdfObject XObject) {

		// see if visible
		boolean isVisible = true;

		// if layer object attached see if should be visible
		PdfObject layerObj = XObject.getDictionary(PdfDictionary.OC);

		if (layerObj != null) {

			String layerName = null;
			byte[][] OCGS = layerObj.getKeyArray(PdfDictionary.OCGs);

			if (OCGS != null) {
				for (byte[] OCG : OCGS) {
					String ref = new String(OCG);
					layerName = getNameFromRef(ref);
				}
			}

			if (layerName == null) layerName = layerObj.getTextStreamValue(PdfDictionary.Name);

			if (layerName != null && isLayerName(layerName)) {
				isVisible = isVisible(layerName);
			}
		}

		return isVisible;
	}

	public boolean isLocked(String layerName) {
		return this.layerLocks.containsKey(layerName); // To change body of created methods use File | Settings | File Templates.
	}

	/**
	 * show if decoded version match visibility flags which can be altered by user
	 */
	public boolean getChangesMade() {
		return this.changesMade;
	}

	/**
	 * show if is name of layer (as opposed to just label)
	 */
	public boolean isLayerName(String name) {
		return this.layerNames.containsKey(name);
	}

	/**
	 * number of layers setup
	 */
	public int getLayersCount() {
		return this.layerCount;
	}

	public String getNameFromRef(String ref) {
		return (String) this.refTolayerName.get(ref);
	}

	// public void setScaling(float scaling) {
	// this.scaling=scaling;
	// }

	/** JS returns all the OCG objects in the document. */
	public Object[] getOCGs() {
		return getOCGs(-1);
	}

	/**
	 * JS Gets an array of OCG objects found on a specified page.
	 * 
	 * @param page
	 *            - (optional) The 0-based page number. If not specified, all the OCGs found in the document are returned. If no argument is passed,
	 *            returns all OCGs listed in alphabetical order, by name. If nPage is passed, this method returns the OCGs for that page, in the order
	 *            they were created.
	 * @return - An array of OCG objects or null if no OCGs are present.
	 */
	public Object[] getOCGs(int page) {

		// return once initialised
		if (this.layers != null) return this.layers;

		int count = this.layerNames.size();

		// create array of values with access to this so we can reset
		Layer[] layers = new Layer[count];

		Iterator layersIt = this.layerNames.keySet().iterator();
		int ii = 0;
		String name;
		while (layersIt.hasNext()) {
			name = (String) layersIt.next();

			layers[ii] = new Layer(name, this);
			ii++;
		}

		return layers;
	}

	public void addJScommand(String name, String js) {

		if (this.jsCommands == null) this.jsCommands = new HashMap();

		// add to list to execute
		this.jsCommands.put(name, js);
	}

	public Iterator getJSCommands() {

		if (this.jsCommands != null) {
			Iterator names = this.jsCommands.keySet().iterator();
			Map visibleJSCommands = new HashMap();

			while (names.hasNext()) {
				String name = (String) names.next();
				if (this.isVisible(name)) {
					visibleJSCommands.put(this.jsCommands.get(name), "x");
				}
			}

			return visibleJSCommands.keySet().iterator();
		}
		else return null;
	}

	public int getOCpageNumber() {
		return this.OCpageNumber;
	}
}
