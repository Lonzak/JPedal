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
 * ExampleActionHandler.java
 * ---------------
 */

package org.jpedal.examples.handlers;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import org.jpedal.examples.viewer.Commands;
import org.jpedal.examples.viewer.Viewer;
import org.jpedal.examples.viewer.gui.SwingGUI;
import org.jpedal.external.JPedalActionHandler;
import org.jpedal.external.Options;

public class ExampleActionHandler extends JFrame {

	private static final long serialVersionUID = -1368375044254061563L;

	public static void main(String[] args) {
		new ExampleActionHandler();
	}

	public ExampleActionHandler() {
		/** add the Viewer component */
		Viewer viewer = new Viewer();
		viewer.setRootContainer(getContentPane());

		/** Initiate viewer */
		viewer.setupViewer();

		/** create a new JPedalActionHandler implementation */
		JPedalActionHandler helpAction = new JPedalActionHandler() {
			@Override
			public void actionPerformed(SwingGUI currentGUI, Commands commands) {
				JOptionPane.showMessageDialog(currentGUI.getFrame(), "Custom help dialog", "JPedal Help", JOptionPane.INFORMATION_MESSAGE);
			}
		};

		/** add the implementation to a Map, with its corresponding command, in this case Commands.HELP */
		Map actions = new HashMap();
		actions.put(Commands.HELP, helpAction);

		/** pass the map into the external handler */
		viewer.addExternalHandler(actions, Options.JPedalActionHandler);

		/** display the Viewer */
		displayViewer();
	}

	private void displayViewer() {
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int width = d.width / 2, height = d.height / 2;
		if (width < 700) width = 700;

		setSize(width, height);
		setLocationRelativeTo(null); // centre on screen
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setVisible(true);
	}
}
