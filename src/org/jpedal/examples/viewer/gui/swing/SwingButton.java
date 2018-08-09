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
 * SwingButton.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui.swing;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.jpedal.examples.viewer.gui.generic.GUIButton;

/** Swing specific implementation of GUIButton interface */
public class SwingButton extends JButton implements GUIButton {

	private static final long serialVersionUID = -7448813287258699275L;
	private int ID;

	public SwingButton() {
		super();
	}

	public SwingButton(String string) {
		super(string);
	}

	@Override
	public void init(URL path, int ID, String toolTip) {

		this.ID = ID;

		/** bookmarks icon */
		setToolTipText(toolTip);

		setBorderPainted(false);

		if (path != null) {

			ImageIcon icon = new ImageIcon(path);
			setIcon(icon);
			createPressedLook(this, icon);
		}

		/**
		 * if(path!=null ){ // actually its the text setText(path.getFile().split("\\.")[0]);
		 * 
		 * setFont(new Font("Lucida",Font.ITALIC,14));
		 * 
		 * if(path.equals("Buy")) setForeground(Color.BLUE); else setForeground(Color.RED); } /
		 **/
	}

	/**
	 * create a pressed look of the <b>icon</b> and added it to the pressed Icon of <b>button</b>
	 */
	private static void createPressedLook(AbstractButton button, ImageIcon icon) {
		BufferedImage image = new BufferedImage(icon.getIconWidth() + 2, icon.getIconHeight() + 2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.drawImage(icon.getImage(), 1, 1, null);
		g.dispose();
		ImageIcon iconPressed = new ImageIcon(image);
		button.setPressedIcon(iconPressed);
	}

	@Override
	public void setIcon(ImageIcon icon) {
		super.setIcon(icon);
	}

	@Override
	public boolean isEnabled() {
		return super.isEnabled();
	}

	/** command ID of button */
	@Override
	public int getID() {
		return this.ID;
	}

	@Override
	public void setName(String s) {
		super.setName(s);
	}
}
