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
 * FileDownload.java
 * ---------------
 */

package org.jpedal.examples.viewer.gui.popups;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.jpedal.io.ObjectStore;
import org.jpedal.utils.LogWriter;
import org.jpedal.utils.Messages;

public class FileDownload {
	// Load file from URL into file then open file
	File tempURLFile;

	// Components required to make the download window for online pdfs
	boolean downloadCreated = false;
	JFrame download = null;
	JPanel p;
	JProgressBar pb;
	JLabel downloadMessage;
	JLabel downloadFile;
	JLabel turnOff;
	int downloadCount = 0;
	boolean visible = true;
	String progress = "";

	// Coords to display the window at
	Point coords = null;

	public FileDownload(boolean showWindow, Point pos) {

		this.visible = showWindow;
		this.coords = pos;

		if (this.visible) {
			this.download = new JFrame();
			this.p = new JPanel(new GridBagLayout());
			this.pb = new JProgressBar();
			this.downloadMessage = new JLabel();
			this.downloadFile = new JLabel();
			this.turnOff = new JLabel();

			this.download.setResizable(false);
			this.download.setTitle(Messages.getMessage("PageLayoutViewMenu.DownloadWindowTitle"));

			// BoxLayout bl = new BoxLayout(p, BoxLayout.X_AXIS);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			// gbc.fill = GridBagConstraints.BOTH;
			this.downloadFile.setSize(250, this.downloadFile.getHeight());
			this.downloadFile.setMinimumSize(new Dimension(250, 15));
			this.downloadFile.setMaximumSize(new Dimension(250, 15));
			this.downloadFile.setPreferredSize(new Dimension(250, 15));
			this.p.add(this.downloadFile, gbc);

			gbc.gridy = 1;
			this.downloadMessage.setSize(250, this.downloadFile.getHeight());
			this.downloadMessage.setMinimumSize(new Dimension(250, 15));
			this.downloadMessage.setMaximumSize(new Dimension(250, 15));
			this.downloadMessage.setPreferredSize(new Dimension(250, 15));
			this.p.add(this.downloadMessage, gbc);

			gbc.gridy = 2;
			this.pb.setSize(260, this.downloadFile.getHeight());
			this.pb.setMinimumSize(new Dimension(260, 20));
			this.pb.setMaximumSize(new Dimension(260, 20));
			this.pb.setPreferredSize(new Dimension(260, 20));
			this.p.add(this.pb, gbc);

			gbc.gridy = 3;
			this.p.add(this.turnOff, gbc);

			this.download.getContentPane().add(this.p);
			this.download.setSize(320, 100);

			this.downloadCreated = true;
		}
	}

	public File createWindow(String pdfUrl) {
		URL url;
		InputStream is;

		try {
			int fileLength;

			String str;
			// if(pdfUrl.startsWith("jar:/")) {
			//
			//
			// str= "file.pdf";//Viewer.file;
			// is=this.getClass().getResourceAsStream(pdfUrl.substring(4));
			//
			// //fileLength=is.available();
			// //System.out.println(str+">>"+pdfUrl.substring(4)+"<<>>"+is);
			// }else{
			url = new URL(pdfUrl);

			is = url.openStream();

			str = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
			fileLength = url.openConnection().getContentLength();

			// }
			final String filename = str;

			this.tempURLFile = File.createTempFile(filename.substring(0, filename.lastIndexOf('.')), filename.substring(filename.lastIndexOf('.')),
					new File(ObjectStore.temp_dir));

			FileOutputStream fos = new FileOutputStream(this.tempURLFile);

			// <start-adobe><start-thin>
			if (this.visible && this.coords != null) {
				this.download.setLocation((this.coords.x - (this.download.getWidth() / 2)), (this.coords.y - (this.download.getHeight() / 2)));
				this.download.setVisible(true);
			}
			// <end-thin><end-adobe>

			if (this.visible) {
				this.pb.setMinimum(0);
				this.pb.setMaximum(fileLength);
				// saveLocal.setEnabled(false);

				String message = Messages.getMessage("PageLayoutViewMenu.DownloadWindowMessage");
				message = message.replaceAll("FILENAME", filename);
				this.downloadFile.setText(message);

				Font f = this.turnOff.getFont();
				this.turnOff.setFont(new Font(f.getName(), f.getStyle(), 8));
				this.turnOff.setAlignmentY(Component.RIGHT_ALIGNMENT);
				this.turnOff.setText(Messages.getMessage("PageLayoutViewMenu.DownloadWindowTurnOff"));
			}
			// download.setVisible(true);
			// Download buffer
			byte[] buffer = new byte[4096];
			// Download the PDF document
			int read;
			int current = 0;

			String rate = "kb"; // mb
			int mod = 1000; // 1000000

			if (fileLength > 1000000) {
				rate = "mb";
				mod = 1000000;
			}

			if (this.visible) {
				this.progress = Messages.getMessage("PageLayoutViewMenu.DownloadWindowProgress");
				if (fileLength < 1000000) this.progress = this.progress.replaceAll("DVALUE", (fileLength / mod) + " " + rate);
				else {
					String fraction = String.valueOf(((fileLength % mod) / 10000));
					if (((fileLength % mod) / 10000) < 10) fraction = '0' + fraction;

					this.progress = this.progress.replaceAll("DVALUE", (fileLength / mod) + "." + fraction + ' ' + rate);
				}
			}

			while ((read = is.read(buffer)) != -1) {
				current = current + read;
				this.downloadCount = this.downloadCount + read;

				if (this.visible) {
					if (fileLength < 1000000) this.downloadMessage.setText(this.progress.replaceAll("DSOME", (current / mod) + " " + rate));
					else {
						String fraction = String.valueOf(((current % mod) / 10000));
						if (((current % mod) / 10000) < 10) fraction = '0' + fraction;

						this.downloadMessage.setText(this.progress.replaceAll("DSOME", (current / mod) + "." + fraction + ' ' + rate));
					}
					this.pb.setValue(current);

					this.download.repaint();
				}

				fos.write(buffer, 0, read);
			}
			fos.flush();
			// Close streams
			is.close();
			fos.close();

			// File completed download, show the save button
			if (this.visible) this.downloadMessage.setText("Download of " + filename + " is complete.");
			// saveLocal.setEnabled(true);

		}
		catch (Exception e) {
			LogWriter.writeLog("[PDF] Exception " + e + " opening URL " + pdfUrl);
			e.printStackTrace();
		}

		if (this.visible) this.download.setVisible(false);

		return this.tempURLFile;
	}

}
