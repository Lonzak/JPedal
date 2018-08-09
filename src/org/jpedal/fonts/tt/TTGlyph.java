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
 * TTGlyph.java
 * ---------------
 */
package org.jpedal.fonts.tt;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;

import org.jpedal.color.PdfPaint;
import org.jpedal.fonts.glyph.PdfGlyph;
import org.jpedal.io.PathSerializer;
import org.jpedal.objects.GraphicsState;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.repositories.Vector_Double;
import org.jpedal.utils.repositories.Vector_Int;
import org.jpedal.utils.repositories.Vector_Object;
import org.jpedal.utils.repositories.Vector_Path;
import org.jpedal.utils.repositories.Vector_Short;

public class TTGlyph implements PdfGlyph, Serializable {

	private static final long serialVersionUID = -4296884514669454220L;

	public static boolean useHinting = false;
	/**/

	public static boolean redecodePage = false;

	static {
		String value = System.getProperty("org.jpedal.useTTFontHinting");
		if (value != null) {
			useHinting = value.toLowerCase().equals("true");
		}
	}

	private static HashSet testedFonts = new HashSet();

	private boolean containsBrokenGlyfData = false;

	private boolean ttHintingRequired = false;

	private short minX, minY, maxX, maxY;

	int BPoint1, BPoint2;

	int BP1x = -1, BP2x = -1, BP1y = -1, BP2y = -1;

	private short compMinX, compMinY, compMaxX, compMaxY;

	private int[] scaledX, scaledY;

	private short leftSideBearing;
	// private int advanceWidth;

	private Vector_Int xtranslateValues = new Vector_Int(5);
	private Vector_Int ytranslateValues = new Vector_Int(5);
	private Vector_Double xscaleValues = new Vector_Double(5);
	private Vector_Double yscaleValues = new Vector_Double(5);
	private Vector_Double scale01Values = new Vector_Double(5);
	private Vector_Double scale10Values = new Vector_Double(5);

	private int xtranslate, ytranslate;

	private double xscale = 1, yscale = 1, scale01 = 0, scale10 = 0;

	private int[] instructions;
	private int currentInstructionDepth = Integer.MAX_VALUE;
	private Vector_Object glyfX = new Vector_Object(5);
	private Vector_Object glyfY = new Vector_Object(5);
	private Vector_Object curves = new Vector_Object(5);
	private Vector_Object contours = new Vector_Object(5);
	private Vector_Int endPtIndices = new Vector_Int(5);

	private int contourCount = 0;

	private float unitsPerEm = 64;

	public boolean debug = false;

	/** paths for the letter, marked as transient so it wont be serialized */
	private transient Vector_Path paths = new Vector_Path(10);

	// used to track which glyf for complex glyph
	private int compCount = 1;

	private boolean isComposite = false;

	String glyfName = "";
	private double pixelSize;

	// private int idx;

	/**
	 * method to set the paths after the object has be deserialized.
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param vp
	 *            - the Vector_Path to set
	 */
	public void setPaths(Vector_Path vp) {
		this.paths = vp;
	}

	/**
	 * method to serialize all the paths in this object. This method is needed because GeneralPath does not implement Serializable so we need to
	 * serialize it ourself. The correct usage is to first serialize this object, cached_current_path is marked as transient so it will not be
	 * serilized, this method should then be called, so the paths are serialized directly after the main object in the same ObjectOutput.
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param os
	 *            - ObjectOutput to write to
	 * @throws IOException
	 */
	public void writePathsToStream(ObjectOutput os) throws IOException {
		if ((this.paths != null)) {

			GeneralPath[] generalPaths = this.paths.get();

			int count = 0;

			/** find out how many items are in the collection */
			for (int i = 0; i < generalPaths.length; i++) {
				if (generalPaths[i] == null) {
					count = i;
					break;
				}
			}

			/** write out the number of items are in the collection */
			os.writeObject(count);

			/** iterate throught the collection, and write out each path individualy */
			for (int i = 0; i < count; i++) {
				PathIterator pathIterator = generalPaths[i].getPathIterator(new AffineTransform());
				PathSerializer.serializePath(os, pathIterator);
			}

		}
	}

	public TTGlyph() {}

	/**
	 * Unhinted constructor
	 */
	public TTGlyph(String glyfName, boolean debug, Glyf currentGlyf, FontFile2 glyfTable, Hmtx currentHmtx, int idx, float unitsPerEm,
			String baseFontName) {

		// debug=idx==2246;
		this.debug = debug;

		this.glyfName = glyfName;
		// this.idx=idx;
		this.leftSideBearing = currentHmtx.getLeftSideBearing(idx);
		// this.advanceWidth = currentHmtx.getAdvanceWidth(idx);
		this.unitsPerEm = unitsPerEm;

		int p = currentGlyf.getCharString(idx);
		glyfTable.setPointer(p);

		readGlyph(currentGlyf, glyfTable);

		/** create glyphs the first time */
		for (int i = 0; i < this.compCount; i++) {

			int[] pX = (int[]) this.glyfX.elementAt(i);
			int[] pY = (int[]) this.glyfY.elementAt(i);
			boolean[] onCurve = (boolean[]) this.curves.elementAt(i);
			boolean[] endOfContour = (boolean[]) this.contours.elementAt(i);
			int endIndex = this.endPtIndices.elementAt(i);

			if (this.isComposite) {
				this.xtranslate = this.xtranslateValues.elementAt(i);
				this.ytranslate = this.ytranslateValues.elementAt(i);
				this.xscale = this.xscaleValues.elementAt(i);
				this.yscale = this.yscaleValues.elementAt(i);
				this.scale01 = this.scale01Values.elementAt(i);
				this.scale10 = this.scale10Values.elementAt(i);

				// factor in BPoint where points overlap
				if (this.BPoint1 != -1 && this.BPoint2 != -1) {
					if (this.BP1x == -1 && this.BP2x == -1 && this.BP1y == -1 && this.BP2y == -1) { // first point
						this.BP1x = pX[this.BPoint1];
						this.BP1y = pY[this.BPoint1];

					}
					else { // second and reset

						this.BP2x = pX[this.BPoint2];
						this.BP2y = pY[this.BPoint2];

						int xx = this.BP1x - this.BP2x;
						int yy = this.BP1y - this.BP2y;

						int count = pX.length;
						for (int ii = 0; ii < count; ii++) {
							pX[ii] = pX[ii] + xx;

							if (debug) System.out.println(pY[ii] + " " + yy + " BP1y=" + this.BP1y + " BP1y=" + this.BP1y);
							pY[ii] = pY[ii] + yy;
						}

						// reset for next
						this.BP1x = -1;
						this.BP2x = -1;
						this.BP1y = -1;
						this.BP2y = -1;
					}
				}
			}

			// drawGlyf(pX,pY,onCurve,endOfContour,endIndex,debug);
			createPaths(pX, pY, onCurve, endOfContour, endIndex, debug);
		}

		/**/
		if (debug) {
			try {
				System.out.println("debugging" + idx);
				java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(700, 700, java.awt.image.BufferedImage.TYPE_INT_ARGB);

				Graphics2D gg2 = img.createGraphics();

				for (int jj = 0; jj < this.paths.size() - 1; jj++) {
					if (jj == 0) gg2.setColor(java.awt.Color.red);
					else gg2.setColor(java.awt.Color.blue);

					gg2.fill(this.paths.elementAt(jj));

					gg2.draw(this.paths.elementAt(jj).getBounds());
				}
				// org.jpedal.gui.ShowGUIMessage.showGUIMessage("glyf "+ '/' +paths.size(),img,"glyf "+ '/' +paths.size()+ '/' +compCount);
			}
			catch (Exception e) {
				// tell user and log
				if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
			}
		}

		// if(idx==2060)
		// ShowGUIMessage.showGUIMessage("done",img,"glyf done");
	}

	private void readGlyph(Glyf currentGlyf, FontFile2 currentFontFile) {

		//LogWriter.writeLog("{readGlyph}");

		this.contourCount = currentFontFile.getNextUint16();

		// read the max/min co-ords
		this.minX = (short) currentFontFile.getNextUint16();
		this.minY = (short) currentFontFile.getNextUint16();
		this.maxX = (short) currentFontFile.getNextUint16();
		this.maxY = (short) currentFontFile.getNextUint16();

		if (this.minX > this.maxX || this.minY > this.maxY) {
			return;
		}

		if (this.debug) {
			System.out.println("------------------------------------------------------------");
			System.out.println("min=" + this.minX + ' ' + this.minY + " max=" + this.maxX + ' ' + this.maxY + " contourCount=" + this.contourCount);
		}

		if (this.contourCount != 65535) {
			if (this.contourCount > 0) {
				readSimpleGlyph(currentFontFile);
			}
		}
		else {

			this.compMinX = this.minX;
			this.compMinY = this.minY;
			this.compMaxX = this.maxX;
			this.compMaxY = this.maxY;

			if (this.debug) System.out.println("XXmain=" + this.minX + ' ' + this.minY + ' ' + this.maxX + ' ' + this.maxY);

			readComplexGlyph(currentGlyf, currentFontFile);
		}
	}

	// Track translations for recursion
	int existingXTranslate = 0;
	int existingYTranslate = 0;
	int depth = 0;

	final private void readComplexGlyph(Glyf currentGlyf, FontFile2 currentFontFile) {

		this.isComposite = true;

		// Remove elements for the compound as it's only a container
		this.xtranslateValues.pull();
		this.ytranslateValues.pull();
		this.xscaleValues.pull();
		this.yscaleValues.pull();
		this.scale01Values.pull();
		this.scale10Values.pull();

		this.BPoint1 = -1;
		this.BPoint2 = -1;

		// LogWriter.writeMethod("{readComplexGlyph}", 0);

		boolean WE_HAVE_INSTRUCTIONS = false;

		int count = currentGlyf.getGlypfCount();

		while (true) {
			int flag = currentFontFile.getNextUint16();
			int glyphIndex = currentFontFile.getNextUint16();

			if (this.debug) System.err.println("Index=" + glyphIndex + " flag=" + flag + ' ' + count);

			// allow for bum data
			if (glyphIndex >= count) {
				this.containsBrokenGlyfData = true;
				break;
			}

			// set flag options
			boolean ARG_1AND_2_ARE_WORDS = (flag & 1) == 1;
			boolean ARGS_ARE_XY_VALUES = (flag & 2) == 2;
			boolean WE_HAVE_A_SCALE = (flag & 8) == 8;
			boolean WE_HAVE_AN_X_AND_Y_SCALE = (flag & 64) == 64;
			boolean WE_HAVE_A_TWO_BY_TWO = (flag & 128) == 128;
			WE_HAVE_INSTRUCTIONS = WE_HAVE_INSTRUCTIONS || (flag & 256) == 256;

			if (ARG_1AND_2_ARE_WORDS && ARGS_ARE_XY_VALUES) {
				// 1st short contains the value of e
				// 2nd short contains the value of f
				this.xtranslate = currentFontFile.getNextInt16();
				this.ytranslate = currentFontFile.getNextInt16();
			}
			else
				if (!ARG_1AND_2_ARE_WORDS && ARGS_ARE_XY_VALUES) {
					// 1st byte contains the value of e
					// 2nd byte contains the value of f
					this.xtranslate = currentFontFile.getNextint8();
					this.ytranslate = currentFontFile.getNextint8();
				}
				else
					if (ARG_1AND_2_ARE_WORDS && !ARGS_ARE_XY_VALUES) {
						// 1st short contains the index of matching point in compound being constructed
						// 2nd short contains index of matching point in component
						this.BPoint1 = currentFontFile.getNextUint16();
						this.BPoint2 = currentFontFile.getNextUint16();
						this.xtranslate = 0;
						this.ytranslate = 0;

					}
					else
						if (!ARG_1AND_2_ARE_WORDS && !ARGS_ARE_XY_VALUES) {
							// 1st byte containing index of matching point in compound being constructed
							// 2nd byte containing index of matching point in component
							this.BPoint1 = currentFontFile.getNextUint8();
							this.BPoint2 = currentFontFile.getNextUint8();
							this.xtranslate = 0;
							this.ytranslate = 0;

						}

			// set defaults
			this.xscale = 1; // a
			this.scale01 = 0; // b
			this.scale10 = 0; // c
			this.yscale = 1; // d

			/** workout scaling factors */
			if ((!WE_HAVE_A_SCALE) && (!WE_HAVE_AN_X_AND_Y_SCALE) && (!WE_HAVE_A_TWO_BY_TWO)) {
				// uses defaults already set

			}
			else
				if ((WE_HAVE_A_SCALE) && (!WE_HAVE_AN_X_AND_Y_SCALE) && (!WE_HAVE_A_TWO_BY_TWO)) {

					this.xscale = currentFontFile.getF2Dot14(); // a
					this.scale01 = 0; // b
					this.scale10 = 0; // c
					this.yscale = this.xscale; // d
				}
				else
					if ((!WE_HAVE_A_SCALE) && (WE_HAVE_AN_X_AND_Y_SCALE) && (!WE_HAVE_A_TWO_BY_TWO)) {

						this.xscale = currentFontFile.getF2Dot14(); // a
						this.scale01 = 0; // b
						this.scale10 = 0; // c
						this.yscale = currentFontFile.getF2Dot14(); // d

					}
					else
						if ((!WE_HAVE_A_SCALE) && (!WE_HAVE_AN_X_AND_Y_SCALE) && (WE_HAVE_A_TWO_BY_TWO)) {

							this.xscale = currentFontFile.getF2Dot14(); // a
							this.scale01 = currentFontFile.getF2Dot14(); // b
							this.scale10 = currentFontFile.getF2Dot14(); // c
							this.yscale = currentFontFile.getF2Dot14(); // d
						}

			// store so we can remove later
			int localX = this.xtranslate;
			int localY = this.ytranslate;

			// Get total translation
			this.xtranslate += this.existingXTranslate;
			this.ytranslate += this.existingYTranslate;

			// save values
			this.xtranslateValues.addElement(this.xtranslate);
			this.ytranslateValues.addElement(this.ytranslate);
			this.xscaleValues.addElement(this.xscale);
			this.yscaleValues.addElement(this.yscale);
			this.scale01Values.addElement(this.scale01);
			this.scale10Values.addElement(this.scale10);

			// save location so we can restore
			int pointer = currentFontFile.getPointer();

			/**/
			// now read the simple glyphs
			int p = currentGlyf.getCharString(glyphIndex);

			if (p != -1) {
				if (p < 0) p = -p;
				currentFontFile.setPointer(p);
				this.existingXTranslate = this.xtranslate;
				this.existingYTranslate = this.ytranslate;
				this.depth++;
				readGlyph(currentGlyf, currentFontFile);
				this.depth--;
				this.existingXTranslate -= localX;
				this.existingYTranslate -= localY;
			}
			else {
				System.err.println("Wrong value in complex");
			}

			currentFontFile.setPointer(pointer);

			// break out at end
			if ((flag & 32) == 0) {

				if (WE_HAVE_INSTRUCTIONS) {
					int instructionLength = currentFontFile.getNextUint16();
					int[] instructions = new int[instructionLength];
					for (int i = 0; i < instructionLength; i++)
						instructions[i] = currentFontFile.getNextUint8();
					if (this.depth <= this.currentInstructionDepth) {
						this.instructions = instructions;
						this.currentInstructionDepth = this.depth;
					}
				}
				else {
					if (this.depth <= this.currentInstructionDepth) {
						this.instructions = new int[] {};
						this.currentInstructionDepth = this.depth;
					}
				}

				break;
			}

			this.compCount++;
		}
	}

	private void readSimpleGlyph(FontFile2 currentFontFile) {

		// LogWriter.writeMethod("{readSimpleGlyph}", 0);

		int flagCount = 1;

		short x1;

		Vector_Int rawFlags = new Vector_Int(50);
		Vector_Int endPts = new Vector_Int(50);
		Vector_Short XX = new Vector_Short(50);
		Vector_Short Y = new Vector_Short(50);

		// all endpoints
		if (this.debug) {
			System.out.println("endPoints");
			System.out.println("---------");
		}

		try {

			int lastPt = 0;
			for (int i = 0; i < this.contourCount; i++) {

				lastPt = currentFontFile.getNextUint16();

				if (this.debug) System.out.println(i + " " + lastPt);

				endPts.addElement(lastPt);

			}

			// allow for corrupted value with not enough entries
			// ie customers3/ICG3Q03.pdf
			if (currentFontFile.hasValuesLeft()) {

				/**
				 * Don;t comment out !!!!!!!!! needs to be read to advance pointer
				 */
				int instructionLength = currentFontFile.getNextUint16();

				int[] instructions = new int[instructionLength];
				for (int i = 0; i < instructionLength; i++)
					instructions[i] = currentFontFile.getNextUint8();

				if (this.depth < this.currentInstructionDepth) this.instructions = instructions;

				if (this.debug) {
					System.out.println("Instructions");
					System.out.println("------------");
					System.out.println("count=" + instructionLength);
				}

				int count = lastPt + 1;
				int flag;

				/** we read the flags (some can repeat) */
				for (int i = 0; i < count; i++) {
					if (currentFontFile.getBytesLeft() < 1) {
						return;
					}
					flag = currentFontFile.getNextUint8();
					rawFlags.addElement(flag);
					flagCount++;

					if ((flag & 8) == 8) { // repeating flags }
						int repeatCount = currentFontFile.getNextUint8();
						for (int r = 1; r <= repeatCount; r++) {
							rawFlags.addElement(flag);
							flagCount++;
						}
						i += repeatCount;
					}
				}

				/** read the x values and set segment for complex glyph */
				for (int i = 0; i < count; i++) {
					flag = rawFlags.elementAt(i);

					// boolean twoByteValue=((flag & 2)==0);
					if ((flag & 16) != 0) { //
						if ((flag & 2) != 0) { // 1 byte + value
							x1 = (short) currentFontFile.getNextUint8();
							XX.addElement(x1);
						}
						else { // 2 byte value - same as previous - ??? same X coord or value
							XX.addElement((short) 0);
						}

					}
					else {
						if ((flag & 2) != 0) { // 1 byte - value
							x1 = (short) -currentFontFile.getNextUint8();
							XX.addElement(x1);
						}
						else { // signed 16 bit delta vector
							x1 = currentFontFile.getNextSignedInt16();
							XX.addElement(x1);
						}
					}
				}

				/** read the y values */
				for (int i = 0; i < count; i++) {
					flag = rawFlags.elementAt(i);
					if ((flag & 32) != 0) {
						if ((flag & 4) != 0) {
							if (currentFontFile.getBytesLeft() < 1) {
								return;
							}
							Y.addElement((short) currentFontFile.getNextUint8());
						}
						else {
							Y.addElement((short) 0);
						}
					}
					else {
						if ((flag & 4) != 0) {
							Y.addElement((short) -currentFontFile.getNextUint8());
						}
						else {
							short val = currentFontFile.getNextSignedInt16();
							Y.addElement(val);
						}
					}
				}

				/**
				 * calculate the points
				 */
				int endPtIndex = 0;
				int x = 0, y = 0;

				int[] flags = rawFlags.get();

				int[] endPtsOfContours = endPts.get();
				short[] XPoints = XX.get();
				short[] YPoints = Y.get();
				count = XPoints.length;

				int[] pX = new int[count + 2];
				int[] pY = new int[count + 2];
				boolean[] onCurve = new boolean[count + 2];
				boolean[] endOfContour = new boolean[count + 2];

				int endIndex = 0;

				if (this.debug) {
					System.out.println("Points");
					System.out.println("------");
				}
				for (int i = 0; i < count; i++) {

					boolean endPt = endPtsOfContours[endPtIndex] == i;
					if (endPt) {
						endPtIndex++;
						endIndex = i + 1;
					}
					x += XPoints[i];
					y += YPoints[i];

					pX[i] = x;
					pY[i] = y;

					onCurve[i] = i < flagCount && (flags[i] & 1) != 0;

					endOfContour[i] = endPt;

					if (this.debug) System.out.println(i + " " + pX[i] + ' ' + pY[i] + " on curve=" + onCurve[i] + " endOfContour[i]="
							+ endOfContour[i]);

				}

				for (int i = 0; i < pX.length; i++) {
					int lX = pX[i];
					int lY = pY[i];

					// Convert x
					// pX[i] = convertX(lX,lY);
					if (!this.isComposite) {
						if (!useHinting) pX[i] = (int) (lX / this.unitsPerEm);
						else pX[i] = lX;
					}
					else {
						if (!useHinting) pX[i] = (int) ((((lX * this.xscale) + (lY * this.scale10)) + this.xtranslate) / this.unitsPerEm);
						else pX[i] = (int) ((((lX * this.xscale) + (lY * this.scale10)) + this.xtranslate));
					}

					// Convert Y
					// pY[i] = convertY(lX,lY);
					if (!this.isComposite) {
						if (!useHinting) pY[i] = (int) (lY / this.unitsPerEm);
						else pY[i] = lY;
					}
					else {
						if (!useHinting) pY[i] = (int) ((((lX * this.scale01) + (lY * this.yscale)) + this.ytranslate) / this.unitsPerEm);
						else pY[i] = (int) ((((lX * this.scale01) + (lY * this.yscale)) + this.ytranslate));
					}
				}

				// store
				this.glyfX.addElement(pX);
				this.glyfY.addElement(pY);
				this.curves.addElement(onCurve);
				this.contours.addElement(endOfContour);
				this.endPtIndices.addElement(endIndex);

				// move back at end
				/**
				 * pX[count]=0; pY[count]=0; onCurve[count]=true; endOfContour[count]=true; pX[count+1]=advanceWidth; pY[count+1]=0;
				 * onCurve[count+1]=true; endOfContour[count+1]=true;
				 * 
				 * /** debug=false;
				 * 
				 * if(idx==92){ //if(glyfName.equals("y")){ ii++; if(ii>0){ System.out.println(ii+"=============="+glyfName+"=====================");
				 * 
				 * debug=true;
				 * 
				 * img=new BufferedImage(1000,1000,BufferedImage.TYPE_INT_ARGB); Graphics2D g2=(Graphics2D) img.createGraphics();
				 * 
				 * g2.setColor(Color.red); render(g2,debug); } }/
				 **/
			}
		}
		catch (Exception e) {
			// System.err.println("error occured while reading TTGlyph bytes");
			return; // there are many files in which the glyph length is not matched with specification
		}
	}

	// public void render(Graphics2D g2){}
	@Override
	public void render(int type, Graphics2D g2, float scaling, boolean isFormGlyph) {

		AffineTransform restore = g2.getTransform();
		BasicStroke oldStroke = (BasicStroke) g2.getStroke();

		float strokeWidth = oldStroke.getLineWidth();

		if (strokeWidth < 0) strokeWidth = -strokeWidth;

		if (useHinting) {
			// Scale down glyph
			g2.scale(1 / 100d, 1 / 100d);

			// Widen stroke to compensate for scale
			strokeWidth = strokeWidth * 100;
		}

		g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, oldStroke.getMiterLimit(), oldStroke.getDashArray(),
				oldStroke.getDashPhase()));

		/** drawn the paths */
		for (int jj = 0; jj < this.paths.size() - 1; jj++) {

			if ((type & GraphicsState.FILL) == GraphicsState.FILL) {
				g2.fill(this.paths.elementAt(jj));
			}
			else
				if ((type & GraphicsState.STROKE) == GraphicsState.STROKE) {
					g2.draw(this.paths.elementAt(jj));
				}
		}

		if (useHinting) {
			// Restore stroke width and scaling
			g2.setStroke(oldStroke);
			g2.setTransform(restore);
		}
	}

	Area glyphShape = null;

	/** return outline of shape */
	@Override
	public Area getShape() {/**/

		if (this.glyphShape == null) {

			/** drawn the paths */
			GeneralPath path = this.paths.elementAt(0);

			for (int jj = 1; jj < this.paths.size() - 1; jj++)
				path.append(this.paths.elementAt(jj), false);

			if (path == null) {
				return null;
			}

			this.glyphShape = new Area(path);

		}

		return this.glyphShape;
	}

	// used by Type3 fonts
	@Override
	public String getGlyphName() {
		return null;
	}

	/**
	 * create the actual shape
	 * 
	 * @param pX
	 */
	public void scaler(int[] pX, int[] pY) {

		this.scaledX = new int[pX.length];
		this.scaledY = new int[pY.length];

		double scale = (this.pixelSize / (this.unitsPerEm * 1000)) * 64;

		for (int i = 0; i < pX.length; i++) {
			this.scaledX[i] = (int) ((scale * pX[i]) + 0.5);
			this.scaledY[i] = (int) ((scale * pY[i]) + 0.5);
		}

		this.scaledX[pX.length - 2] = 0;
		this.scaledY[pY.length - 2] = 0;
		this.scaledX[pX.length - 1] = (int) ((scale * this.leftSideBearing) + 0.5);
		this.scaledY[pY.length - 1] = 0;
	}

	/** create the actual shape */
	public void createPaths(int[] pX, int[] pY, boolean[] onCurve, boolean[] endOfContour, int endIndex, boolean debug) {

		// allow for bum data
		if (endOfContour == null) return;

		/**
		 * scan data and adjust glyfs after first if do not end in contour
		 */

		int ptCount = endOfContour.length;

		int start = 0, firstPt = -1;
		for (int ii = 0; ii < ptCount; ii++) {

			if (endOfContour[ii]) {

				if (firstPt != -1 && (!onCurve[start] || !onCurve[ii])) { // last point not on curve and we have a first point

					int diff = firstPt - start, newPos;

					// make a deep copy of values
					int pXlength = pX.length;
					int[] old_pX = new int[pXlength];
					System.arraycopy(pX, 0, old_pX, 0, pXlength);

					int[] old_pY = new int[pXlength];
					System.arraycopy(pY, 0, old_pY, 0, pXlength);

					boolean[] old_onCurve = new boolean[pXlength];
					System.arraycopy(onCurve, 0, old_onCurve, 0, pXlength);

					// rotate values to ensure point at start
					for (int oldPos = start; oldPos < ii + 1; oldPos++) {

						newPos = oldPos + diff;
						if (newPos > ii) newPos = newPos - (ii - start + 1);
						pX[oldPos] = old_pX[newPos];
						pY[oldPos] = old_pY[newPos];
						onCurve[oldPos] = old_onCurve[newPos];

					}
				}

				// reset values
				start = ii + 1;
				firstPt = -1;

			}
			else
				if (onCurve[ii] && firstPt == -1) { // track first point
					firstPt = ii;
				}

		}

		boolean isFirstDraw = true;

		GeneralPath current_path = new GeneralPath(Path2D.WIND_NON_ZERO);

		int c = pX.length, fc = -1;

		// find first end contour
		for (int jj = 0; jj < c; jj++) {
			if (endOfContour[jj]) {
				fc = jj + 1;
				jj = c;
			}
		}

		int x1, y1, x2 = 0, y2 = 0, x3 = 0, y3 = 0;

		x1 = pX[0];
		y1 = pY[0];

		if (this.debug) System.out.println(pX[0] + " " + pY[0] + " move to x1,y1=" + x1 + ' ' + y1);

		current_path.moveTo(x1, y1);

		if (this.debug) {
			System.out.println("first contour=" + fc + "====================================" + pX[0] + ' ' + pY[0]);
			// System.out.println("start="+x1+ ' ' +y1+" unitsPerEm="+unitsPerEm);
			// for (int i = 0; i <c-2; i++)
			// System.out.println(i+" "+convertX(pX[i],pY[i])+ ' ' +convertY(pX[i],pY[i])+ ' ' +onCurve[i]+ ' ' +endOfContour[i]+" raw="+pX[i]+ ' '
			// +pY[i]);

			// System.out.println("Move to "+x1+ ' ' +y1);

		}

		int xs = 0, ys = 0, lc = 0;
		boolean isEnd = false;

		for (int j = 0; j < endIndex; j++) {

			int p = j % fc;
			int p1 = (j + 1) % fc;
			int p2 = (j + 2) % fc;
			int pm1 = (j - 1) % fc;

			/**
			 * special cases
			 * 
			 * round up to last point at end First point
			 */
			if (j == 0) pm1 = fc - 1;
			if (p1 < lc) p1 = p1 + lc;
			if (p2 < lc) p2 = p2 + lc;

			if (debug) System.out.println("points=" + lc + '/' + fc + ' ' + pm1 + ' ' + p + ' ' + p1 + ' ' + p2 + " j=" + j + " endOfContour[j]="
					+ endOfContour[j]);

			// allow for wrap around on contour
			if (endOfContour[j]) {
				isEnd = true;

				if (onCurve[fc]) {
					xs = pX[fc];
					ys = pY[fc];
				}
				else {
					xs = pX[j + 1];
					ys = pY[j + 1];
				}

				// remember start point
				lc = fc;
				// find next contour
				for (int jj = j + 1; jj < c; jj++) {
					if (endOfContour[jj]) {
						fc = jj + 1;
						jj = c;
					}
				}

				if (debug) System.out.println("End of contour. next=" + j + ' ' + fc + ' ' + lc);

			}

			if (debug) {
				if (j > 0) System.out.println("curves=" + onCurve[p] + ' ' + onCurve[p1] + ' ' + onCurve[p2] + " EndOfContour j-1="
						+ endOfContour[j - 1] + " j=" + endOfContour[j] + " j+1=" + endOfContour[j + 1]);
				else System.out.println("curves=" + onCurve[p] + ' ' + onCurve[p1] + ' ' + onCurve[p2] + " EndOfContour j=" + endOfContour[j]
						+ " j+1=" + endOfContour[j + 1]);
			}

			if (lc == fc && onCurve[p]) {
				j = c;
				if (debug) System.out.println("last 2 match");
			}
			else {

				if (debug) System.out.println(fc + " " + pm1 + ' ' + p + ' ' + p1 + ' ' + p2);

				if (onCurve[p] && onCurve[p1]) { // straight line
					x3 = pX[p1];
					y3 = pY[p1];
					current_path.lineTo(x3, y3);
					if (debug) System.out.println(p + " pt,pt " + x3 + ' ' + y3 + " (lineTo)");

					isFirstDraw = false;
					// curves
				}
				else
					if (j < (c - 3) && ((fc - lc) > 1 || fc == lc)) {
						boolean checkEnd = false;
						if (onCurve[p] && !onCurve[p1] && onCurve[p2]) { // 2 points + control

							x1 = pX[p];
							y1 = pY[p];
							x2 = pX[p1];
							y2 = pY[p1];
							x3 = pX[p2];
							y3 = pY[p2];
							j++;
							checkEnd = true;
							if (debug) System.out.println(p + " pt,cv,pt " + x1 + ' ' + y1 + ' ' + x2 + ' ' + y2 + ' ' + x3 + ' ' + y3);

						}
						else
							if (onCurve[p] && !onCurve[p1] && !onCurve[p2]) { // 1 point + 2 control

								x1 = pX[p];
								y1 = pY[p];
								x2 = pX[p1];
								y2 = pY[p1];
								x3 = midPt(pX[p1], pX[p2]);
								y3 = midPt(pY[p1], pY[p2]);
								j++;

								checkEnd = true;

								if (debug) System.out.println(p + " pt,cv,cv " + x1 + ' ' + y1 + ' ' + x2 + ' ' + y2 + ' ' + x3 + ' ' + y3);

							}
							else
								if (!onCurve[p] && !onCurve[p1] && (!endOfContour[p2] || fc - p2 == 1)) { // 2 control + 1 point (final check allows
																											// for last point to complete loop

									x1 = midPt(pX[pm1], pX[p]);
									y1 = midPt(pY[pm1], pY[p]);
									x2 = pX[p];
									y2 = pY[p];

									x3 = midPt(pX[p], pX[p1]);
									y3 = midPt(pY[p], pY[p1]);
									if (debug) System.out.println(p + " cv,cv1 " + x1 + ' ' + y1 + ' ' + x2 + ' ' + y2 + ' ' + x3 + ' ' + y3);

								}
								else
									if (!onCurve[p] && onCurve[p1]) { // 1 control + 2 point

										x1 = midPt(pX[pm1], pX[p]);
										y1 = midPt(pY[pm1], pY[p]);
										x2 = pX[p];
										y2 = pY[p];
										x3 = pX[p1];
										y3 = pY[p1];
										if (debug) System.out.println(p + " cv,pt " + x1 + ' ' + y1 + ' ' + x2 + ' ' + y2 + ' ' + x3 + ' ' + y3);
									}

						if (isFirstDraw) {
							current_path.moveTo(x1, y1);
							isFirstDraw = false;

							if (debug) System.out.println("first draw move to " + x1 + ' ' + y1);

						}

						if (!(endOfContour[p] && p > 0 && endOfContour[p - 1])) current_path.curveTo(x1, y1, x2, y2, x3, y3);

						if (debug) System.out.println("curveto " + x1 + ' ' + y1 + ' ' + x2 + ' ' + y2 + ' ' + x3 + ' ' + y3);

						/** if end after curve, roll back so we pick up the end */
						if (checkEnd && endOfContour[j]) {

							isEnd = true;

							xs = pX[fc];
							ys = pY[fc];
							// remmeber start point
							lc = fc;
							// find next contour
							for (int jj = j + 1; jj < c; jj++) {
								if (endOfContour[jj]) {
									fc = jj + 1;
									jj = c;
								}
							}

							if (debug) System.out.println("Curve");
						}
					}

				if (endOfContour[p]) current_path.closePath();

				if (debug) System.out.println("x2 " + xs + ' ' + ys + ' ' + isEnd);

				if (isEnd) {
					current_path.moveTo(xs, ys);
					isEnd = false;
					if (debug) System.out.println("Move to " + xs + ' ' + ys);
				}

				if (debug) {
					try {
						if (this.img == null) this.img = new java.awt.image.BufferedImage(800, 800, java.awt.image.BufferedImage.TYPE_INT_ARGB);

						Graphics2D g2 = this.img.createGraphics();
						g2.setColor(java.awt.Color.green);
						g2.draw(current_path);

						final String key = String.valueOf(p);

						org.jpedal.gui.ShowGUIMessage.showGUIMessage(key, this.img, key);

					}
					catch (Exception e) {
						// tell user and log
						if (LogWriter.isOutput()) LogWriter.writeLog("Exception: " + e.getMessage());
					}
				}
			}
		}

		/**
		 * store so we can draw glyf as set of paths
		 */
		this.paths.addElement(current_path);

		if (debug) System.out.println("Ends at " + x1 + ' ' + y1 + " x=" + this.minX + ',' + this.maxX + " y=" + this.minY + ',' + this.maxY
				+ " glyph x=" + this.compMinX + ',' + this.compMaxX + " y=" + this.compMinY + ',' + this.compMaxY);
	}

	java.awt.image.BufferedImage img = null;

	static private int midPt(int a, int b) {
		return a + (b - a) / 2;
	}

	// /**convert to 1000 size*/
	// final private int convertX(int x,int y){
	//
	//
	// if(!isComposite) {
	// if (!useHinting)
	// return (int)(x/unitsPerEm);
	// else
	// return x;
	// } else {
	// //This code seems to match the spec more closely, but doesn't seem to make any difference...
	// //It's also wildly inefficient, so rewrite it if you're going to turn it on!
	// // double absA = xscale;
	// // if (absA < 0)
	// // absA = -absA;
	// // double absB = scale01;
	// // if (absB < 0)
	// // absB = -absB;
	// // double absC = scale10;
	// // if (absC < 0)
	// // absC = -absC;
	// //
	// // double m = absA;
	// // if (absB > m)
	// // m = absB;
	// // if (absA - absC <= 33d/65536 && absA - absC >= -33d/65536)
	// // m = 2*m;
	// //
	// // return (int)((m*(((x * (xscale/m)) + (y * (scale10/m)))+xtranslate))/unitsPerEm);
	// if (!useHinting)
	// return (int)((((x * xscale) + (y * scale10))+xtranslate)/unitsPerEm);
	// else
	// return (int)((((x * xscale) + (y * scale10))+xtranslate));
	// }
	// }
	//
	// /**convert to 1000 size*/
	// final private int convertY(int x,int y){
	//
	// if(!isComposite) {
	// if (!useHinting)
	// return (int)(y/unitsPerEm);
	// else
	// return y;
	// } else {
	// //This code seems to match the spec more closely, but doesn't seem to make any difference...
	// //It's also wildly inefficient, so rewrite it if you're going to turn it on!
	// // double absC = scale10;
	// // if (absC < 0)
	// // absC = -absC;
	// // double absD = yscale;
	// // if (absD < 0)
	// // absD = -absD;
	// //
	// // double n = absC;
	// // if (absD > n)
	// // n = absD;
	// // if (absC - absD <= 33d/65536 && absC - absD >= -33d/65536)
	// // n = 2*n;
	// //
	// // return (int)((n*(((x * (scale01/n)) + (y * (yscale/n)))+ytranslate))/unitsPerEm);//+30;
	// if (!useHinting)
	// return (int)((((x * scale01) + (y * yscale))+ytranslate)/unitsPerEm);
	// else
	// return (int)((((x * scale01) + (y * yscale))+ytranslate));
	// }
	// }

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.fonts.PdfGlyph#getWidth()
	 */
	@Override
	public float getmaxWidth() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.fonts.PdfGlyph#getmaxHeight()
	 */
	@Override
	public int getmaxHeight() {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.fonts.PdfGlyph#setT3Colors(java.awt.Color, java.awt.Color)
	 */
	@Override
	public void setT3Colors(PdfPaint strokeColor, PdfPaint nonstrokeColor, boolean lockColours) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.fonts.PdfGlyph#ignoreColors()
	 */
	@Override
	public boolean ignoreColors() {
		return false;
	}

	/*
	 * Makes debugging easier
	 */
	@Override
	public int getID() {
		return this.id;
	}

	int id = 0;

	/*
	 * Makes debugging easier
	 */
	@Override
	public void setID(int id) {
		this.id = id;
	}

	public void flushArea() {
		this.glyphShape = null;
	}

	@Override
	public void setWidth(float width) {
	}

	@Override
	public int getFontBB(int type) {

		if (this.isComposite) {
			if (type == PdfGlyph.FontBB_X) return this.compMinX;
			else
				if (type == PdfGlyph.FontBB_Y) return this.compMinY;
				else
					if (type == PdfGlyph.FontBB_WIDTH) return this.compMaxX;
					else
						if (type == PdfGlyph.FontBB_HEIGHT) return this.compMaxY;
						else return 0;
		}
		else {
			if (type == PdfGlyph.FontBB_X) return this.minX;
			else
				if (type == PdfGlyph.FontBB_Y) return this.minY;
				else
					if (type == PdfGlyph.FontBB_WIDTH) return this.maxX;
					else
						if (type == PdfGlyph.FontBB_HEIGHT) return this.maxY;
						else return 0;
		}
	}

	@Override
	public void setStrokedOnly(boolean b) {
		// not used here
	}

	// use by TT to handle broken TT fonts
	@Override
	public boolean containsBrokenData() {
		return this.containsBrokenGlyfData;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TTGlyph [containsBrokenGlyfData=");
		builder.append(containsBrokenGlyfData);
		builder.append(", ttHintingRequired=");
		builder.append(ttHintingRequired);
		builder.append(", minX=");
		builder.append(minX);
		builder.append(", minY=");
		builder.append(minY);
		builder.append(", maxX=");
		builder.append(maxX);
		builder.append(", maxY=");
		builder.append(maxY);
		builder.append(", BPoint1=");
		builder.append(BPoint1);
		builder.append(", BPoint2=");
		builder.append(BPoint2);
		builder.append(", BP1x=");
		builder.append(BP1x);
		builder.append(", BP2x=");
		builder.append(BP2x);
		builder.append(", BP1y=");
		builder.append(BP1y);
		builder.append(", BP2y=");
		builder.append(BP2y);
		builder.append(", compMinX=");
		builder.append(compMinX);
		builder.append(", compMinY=");
		builder.append(compMinY);
		builder.append(", compMaxX=");
		builder.append(compMaxX);
		builder.append(", compMaxY=");
		builder.append(compMaxY);
		builder.append(", ");
		if (scaledX != null) {
			builder.append("scaledX=");
			builder.append(Arrays.toString(scaledX));
			builder.append(", ");
		}
		if (scaledY != null) {
			builder.append("scaledY=");
			builder.append(Arrays.toString(scaledY));
			builder.append(", ");
		}
		builder.append("leftSideBearing=");
		builder.append(leftSideBearing);
		builder.append(", ");
		if (xtranslateValues != null) {
			builder.append("xtranslateValues=");
			builder.append(xtranslateValues);
			builder.append(", ");
		}
		if (ytranslateValues != null) {
			builder.append("ytranslateValues=");
			builder.append(ytranslateValues);
			builder.append(", ");
		}
		if (xscaleValues != null) {
			builder.append("xscaleValues=");
			builder.append(xscaleValues);
			builder.append(", ");
		}
		if (yscaleValues != null) {
			builder.append("yscaleValues=");
			builder.append(yscaleValues);
			builder.append(", ");
		}
		if (scale01Values != null) {
			builder.append("scale01Values=");
			builder.append(scale01Values);
			builder.append(", ");
		}
		if (scale10Values != null) {
			builder.append("scale10Values=");
			builder.append(scale10Values);
			builder.append(", ");
		}
		builder.append("xtranslate=");
		builder.append(xtranslate);
		builder.append(", ytranslate=");
		builder.append(ytranslate);
		builder.append(", xscale=");
		builder.append(xscale);
		builder.append(", yscale=");
		builder.append(yscale);
		builder.append(", scale01=");
		builder.append(scale01);
		builder.append(", scale10=");
		builder.append(scale10);
		builder.append(", ");
		if (instructions != null) {
			builder.append("instructions=");
			builder.append(Arrays.toString(instructions));
			builder.append(", ");
		}
		builder.append("currentInstructionDepth=");
		builder.append(currentInstructionDepth);
		builder.append(", ");
		if (glyfX != null) {
			builder.append("glyfX=");
			builder.append(glyfX);
			builder.append(", ");
		}
		if (glyfY != null) {
			builder.append("glyfY=");
			builder.append(glyfY);
			builder.append(", ");
		}
		if (curves != null) {
			builder.append("curves=");
			builder.append(curves);
			builder.append(", ");
		}
		if (contours != null) {
			builder.append("contours=");
			builder.append(contours);
			builder.append(", ");
		}
		if (endPtIndices != null) {
			builder.append("endPtIndices=");
			builder.append(endPtIndices);
			builder.append(", ");
		}
		builder.append("contourCount=");
		builder.append(contourCount);
		builder.append(", unitsPerEm=");
		builder.append(unitsPerEm);
		builder.append(", debug=");
		builder.append(debug);
		builder.append(", compCount=");
		builder.append(compCount);
		builder.append(", isComposite=");
		builder.append(isComposite);
		builder.append(", ");
		if (glyfName != null) {
			builder.append("glyfName=");
			builder.append(glyfName);
			builder.append(", ");
		}
		builder.append("pixelSize=");
		builder.append(pixelSize);
		builder.append(", existingXTranslate=");
		builder.append(existingXTranslate);
		builder.append(", existingYTranslate=");
		builder.append(existingYTranslate);
		builder.append(", depth=");
		builder.append(depth);
		builder.append(", ");
		if (glyphShape != null) {
			builder.append("glyphShape=");
			builder.append(glyphShape);
			builder.append(", ");
		}
		if (img != null) {
			builder.append("img=");
			builder.append(img);
			builder.append(", ");
		}
		builder.append("id=");
		builder.append(id);
		builder.append("]");
		return builder.toString();
	}
}