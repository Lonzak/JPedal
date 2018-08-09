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
 * T1Glyph.java
 * ---------------
 */
package org.jpedal.fonts.glyph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jpedal.color.PdfPaint;
import org.jpedal.color.PdfTexturePaint;
import org.jpedal.io.PathSerializer;
import org.jpedal.objects.GraphicsState;
import org.jpedal.utils.repositories.Vector_Path;

/**
 * <p>
 * defines the current shape which is created by command stream
 * </p>
 * <p>
 * <b>This class is NOT part of the API</b>
 * </p>
 * . Shapes can be drawn onto pdf or used as a clip on other image/shape/text. Shape is built up by storing commands and then turning these commands
 * into a shape. Has to be done this way as Winding rule is not necessarily declared at start.
 */
public class T1Glyph implements PdfGlyph, Serializable {

	private static final long serialVersionUID = 1821092028822458128L;

	/** marked as transient so it wont be serialized */
	private transient Vector_Path cached_current_path = null;

	private float glyfwidth = 1000f;

	private boolean isStroked = false;
	private Paint strokePaint;

	private Map strokedPositions = new HashMap();

	public T1Glyph() {}

	/**
	 * store scaling factors
	 */
	public T1Glyph(Vector_Path cached_current_path) {
		this.cached_current_path = cached_current_path;
	}

	// ////////////////////////////////////////////////////////////////////////
	/**
	 * turn shape commands into a Shape object, storing info for later. Has to be done this way because we need the winding rule to initialise the
	 * shape in Java, and it could be set awywhere in the command stream
	 */
	@Override
	public void render(int text_fill_type, Graphics2D g2, float scaling, boolean isFormGlyph) {

		if (this.cached_current_path != null) {

			// Shape c=g2.getClip();
			//
			// g2.setClip(null);
			//
			// g2.setPaint(Color.RED);
			// g2.fillRect(0, 0, 300, 600);
			// g2.setPaint(Color.BLUE);
			// g2.fillRect(300, 0, 300, 600);
			// g2.drawLine(0,0,600,600);
			// g2.setClip(c);
			//
			//
			GeneralPath[] paths = this.cached_current_path.get();
			int cacheCount = paths.length;
			for (int i = 0; i < cacheCount; i++) {

				if (paths[i] == null) break;

				// if(id==228) {
				// System.out.println("-----"+i+"-----scaling="+scaling);
				if (1 == 2 && i == 0 && scaling > 0) {// && glyfwidth>=500){
					this.getFontBB(PdfGlyph.FontBB_X);

					if (this.minX < 0) {

						// System.out.println(paths[i].getBounds().getWidth()+" glyfwidth="+glyfwidth+" "+minX+" "+maxX);

						// double dx= (glyfwidth-(maxX-minX))/2;
						double dx = (this.glyfwidth - (this.maxX)) / 2;

						g2.translate(dx, 0);

						// if(id==228)
						// System.out.println(id+" "+dx+" "+" glyfwidth="+glyfwidth+" scaling="+scaling+" "+paths[i].getBounds()+" "+text_fill_type);
					}
				}
				// }
				if ((text_fill_type == GraphicsState.FILL)) {

					// replicate shadow effect
					if (this.isStroked) {
						Paint fillPaint = g2.getPaint();
						if (!(fillPaint instanceof PdfTexturePaint)
								&& ((Color) this.strokePaint).getRGB() != ((Color) fillPaint).getRGB()
								&& this.strokedPositions.containsKey(String.valueOf((int) g2.getTransform().getTranslateX()) + '-'
										+ (int) g2.getTransform().getTranslateY())) {

							Stroke fillStroke = g2.getStroke();

							g2.setPaint(this.strokePaint);
							float w = (float) (scaling / g2.getTransform().getScaleX());
							if (w < 0) w = -w;
							g2.setStroke(new BasicStroke(w));
							g2.draw(paths[i]);

							g2.setPaint(fillPaint);
							g2.setStroke(fillStroke);

							// System.out.println(this.getID()+" "+this.getGlyphName());
						}

					}
					g2.fill(paths[i]);
				}

				if (text_fill_type == GraphicsState.STROKE) {

					// ensure visible if just stroke
					if (text_fill_type != GraphicsState.FILL && scaling > 1.0f) {
						// System.out.println(">>"+glyfwidth+" "+scaling+" "+g2.getTransform()+" "+g2.getTransform().getScaleX());

						float w = (float) (scaling / g2.getTransform().getScaleX());

						if (w < 0) w = -w;
						g2.setStroke(new BasicStroke(w));
						// g2.setStroke(new BasicStroke(200));
						// System.out.println(((scaling/g2.getTransform().getScaleX())));

					}

					g2.draw(paths[i]);

					this.strokePaint = g2.getPaint();

					this.strokedPositions.put(
							String.valueOf((int) g2.getTransform().getTranslateX()) + '-' + (int) g2.getTransform().getTranslateY(), "x");

				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.jpedal.fonts.PdfGlyph#getmaxWidth()
	 */
	@Override
	public float getmaxWidth() {
		return this.glyfwidth;
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

	Area glyphShape = null;

	/** return shape of glyph */
	@Override
	public Area getShape() {

		if ((this.cached_current_path != null && this.glyphShape == null)) {

			GeneralPath[] paths = this.cached_current_path.get();
			int cacheCount = paths.length;

			for (int i = 1; i < cacheCount; i++) {

				if (paths[i] == null) break;

				paths[0].append(paths[i], false);

			}

			if ((paths != null) && (paths[0] != null)) this.glyphShape = new Area(paths[0]);

		}

		return this.glyphShape;
	}

	// used by Type3 fonts
	@Override
	public String getGlyphName() {
		return "";
	}

	/**
	 * method to set the paths after the object has be deserialized.
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param vp
	 *            - the Vector_Path to set
	 */
	public void setPaths(Vector_Path vp) {
		this.cached_current_path = vp;
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
		if ((this.cached_current_path != null)) {

			GeneralPath[] paths = this.cached_current_path.get();

			int count = 0;

			/** find out how many items are in the collection */
			for (int i = 0; i < paths.length; i++) {
				if (paths[i] == null) {
					count = i;
					break;
				}
			}

			/** write out the number of items are in the collection */
			os.writeObject(count);

			/** iterate throught the collection, and write out each path individualy */
			for (int i = 0; i < count; i++) {
				PathIterator pathIterator = paths[i].getPathIterator(new AffineTransform());
				PathSerializer.serializePath(os, pathIterator);
			}

		}
	}

	public void flushArea() {
		this.glyphShape = null;
	}

	@Override
	public void setWidth(float width) {
		this.glyfwidth = width;
	}

	int minX = 0, minY = 0, maxX = 0, maxY = 0;

	@Override
	public int getFontBB(int type) {

		// calc if not worked out
		if (this.minX == 0 && this.minY == 0 && this.maxX == 0 && this.maxY == 0) {

			if (this.cached_current_path != null) {

				GeneralPath[] paths = this.cached_current_path.get();
				int cacheCount = paths.length;

				for (int i = 0; i < cacheCount; i++) {

					if (paths[i] == null) break;

					Rectangle b = paths[i].getBounds();
					if (i == 0) {
						this.minX = b.x;
						this.minY = b.y;
						this.maxX = b.width;
						this.maxY = b.height;

					}
					else {

						if (this.minX > b.x) this.minX = b.x;
						if (this.minY > b.y) this.minY = b.y;

						if (this.maxX < b.width) this.maxX = b.width;
						if (this.maxY < b.height) this.maxY = b.height;

					}
				}
			}
		}

		if (type == PdfGlyph.FontBB_X) return this.minX;
		else
			if (type == PdfGlyph.FontBB_Y) return this.minY;
			else
				if (type == PdfGlyph.FontBB_WIDTH) return this.maxX;
				else
					if (type == PdfGlyph.FontBB_HEIGHT) return this.minY;// maxY-minY;
					else return 0;
	}

	@Override
	public void setStrokedOnly(boolean flag) {
		this.isStroked = flag;
	}

	// use by TT to handle broken TT fonts
	@Override
	public boolean containsBrokenData() {
		return false;
	}
}
