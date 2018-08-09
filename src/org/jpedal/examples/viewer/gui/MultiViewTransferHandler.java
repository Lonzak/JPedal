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
 * MultiViewTransferHandler.java
 * ---------------
 */
package org.jpedal.examples.viewer.gui;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.jpedal.examples.viewer.Commands;
import org.jpedal.examples.viewer.Values;
import org.jpedal.examples.viewer.gui.generic.GUIThumbnailPanel;
import org.jpedal.utils.Messages;
import org.jpedal.utils.SwingWorker;

public class MultiViewTransferHandler extends BaseTransferHandler {

	private static final long serialVersionUID = 271129748262568046L;
	private int fileCount = 0;

	public MultiViewTransferHandler(Values commonValues, GUIThumbnailPanel thumbnails, SwingGUI currentGUI, Commands currentCommands) {
		super(commonValues, thumbnails, currentGUI, currentCommands);
	}

	@Override
	public boolean importData(JComponent src, Transferable transferable) {
		try {
			Object dragImport = getImport(transferable);

			if (dragImport instanceof String) {
				String url = (String) dragImport;
				System.out.println(url);
				String testURL = url.toLowerCase();
				if (testURL.startsWith("http:/")) {
					this.currentCommands.openTransferedFile(testURL);
					return true;
				}
				else
					if (testURL.startsWith("file:/")) {
						String[] urls = url.split("file:/");

						List<File> files = new LinkedList<File>();
						for (String file : urls) {
							if (file.length() > 0) {
								File file2 = new File(new URL("file:/" + file).getFile());
								System.out.println(file2);
								files.add(file2);
							}
						}

						return openFiles(files);
					}
			}
			else
				if (dragImport instanceof List) {
					List<File> files = (List<File>) dragImport;

					return openFiles(files);
				}
		}
		catch (Exception e) {}

		return false;
	}

	private boolean openFiles(List<File> files) {
		this.fileCount = 0;
		List<String> flattenedFiles = getFlattenedFiles(files, new ArrayList<String>());

		if (this.fileCount == this.commonValues.getMaxMiltiViewers()) {
			this.currentGUI.showMessageDialog(
					"You have choosen to import more files than your current set " + "maximum (" + this.commonValues.getMaxMiltiViewers()
							+ ").  Only the first " + this.commonValues.getMaxMiltiViewers() + " files will be imported.\nYou can change this value "
							+ "in View | Preferences", "Maximum number of files reached", JOptionPane.INFORMATION_MESSAGE);
		}

		List[] filterdFiles = filterFiles(flattenedFiles);
		final List allowedFiles = filterdFiles[0];
		List disAllowedFiles = filterdFiles[1];

		int noOfDisAllowedFiles = disAllowedFiles.size();
		int noOfAllowedFiles = allowedFiles.size();

		if (noOfDisAllowedFiles > 0) {
			String unOpenableFiles = "";
			for (Object disAllowedFile : disAllowedFiles) {
				String file = (String) disAllowedFile;
				String fileName = new File(file).getName();
				unOpenableFiles += fileName + '\n';
			}

			int result = this.currentGUI.showConfirmDialog("You have selected " + flattenedFiles.size()
					+ " files to open.  The following file(s) cannot be opened\nas they are not valid PDFs " + "or images.\n" + unOpenableFiles
					+ "\nWould you like to open the remaining " + noOfAllowedFiles + " files?", "File Import", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);

			if (result == JOptionPane.NO_OPTION) {
				return false;
			}
		}

		final SwingWorker worker = new SwingWorker() {
			@Override
			public Object construct() {
				for (Object allowedFile : allowedFiles) {
					final String file = (String) allowedFile;

					try {
						MultiViewTransferHandler.this.currentCommands.openTransferedFile(file);
					}
					catch (Exception e) {

						int result;
						if (allowedFiles.size() == 1) {
							MultiViewTransferHandler.this.currentGUI.showMessageDialog(Messages.getMessage("PdfViewerOpenerror"),
									MultiViewTransferHandler.this.commonValues.getSelectedFile(), JOptionPane.ERROR_MESSAGE);
							result = JOptionPane.NO_OPTION;
						}
						else {
							result = MultiViewTransferHandler.this.currentGUI.showConfirmDialog(Messages.getMessage("PdfViewerOpenerror")
									+ ". Continue opening remaining files?", MultiViewTransferHandler.this.commonValues.getSelectedFile(),
									JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

						}

						MultiViewTransferHandler.this.currentGUI.closeMultiViewerWindow(MultiViewTransferHandler.this.commonValues.getSelectedFile());

						if (result == JOptionPane.NO_OPTION) {
							return null;
						}
					}
				}
				return null;
			}
		};
		worker.start();

		// while (currentCommands.openingTransferedFiles()) {
		// Thread.sleep(250);
		// }
		//
		// SwingUtilities.invokeLater(new Runnable() {
		// public void run() {
		// JInternalFrame[] allFrames = currentGUI.getMultiViewerFrames().getAllFrames();
		//
		// for (int i = allFrames.length - 1; i >= 0; i--) {
		// JInternalFrame pdf = allFrames[i];
		//
		// pdf.updateUI();
		// pdf.repaint();
		// try {
		// pdf.setSelected(true);
		// } catch (PropertyVetoException e) {
		// e.printStackTrace();
		// }
		// }
		// currentGUI.getMultiViewerFrames().repaint();
		// }
		// });

		return true;
	}

	private static List[] filterFiles(List<String> flattenedFiles) {
		List<String> allowedFiles = new LinkedList<String>();
		List<String> disAllowedFiles = new LinkedList<String>();

		for (Object flattenedFile : flattenedFiles) {
			String file = ((String) flattenedFile);
			String testFile = file.toLowerCase();

			boolean isValid = ((testFile.endsWith(".pdf")) || (testFile.endsWith(".fdf")) || (testFile.endsWith(".tif"))
					|| (testFile.endsWith(".tiff")) || (testFile.endsWith(".png")) || (testFile.endsWith(".jpg")) || (testFile.endsWith(".jpeg")));

			if (isValid) {
				allowedFiles.add(file);
			}
			else {
				disAllowedFiles.add(file);
			}
		}

		return new List[] { allowedFiles, disAllowedFiles };
	}

	private List<String> getFlattenedFiles(List<File> files, List<String> flattenedFiles) {
		for (Object file1 : files) {
			if (this.fileCount == this.commonValues.getMaxMiltiViewers()) {
				return flattenedFiles;
			}

			File file = (File) file1;
			// System.out.println(file);
			if (file.isDirectory()) {
				getFlattenedFiles(Arrays.asList(file.listFiles()), flattenedFiles);
			}
			else {
				flattenedFiles.add(file.getAbsolutePath());

				this.fileCount++;
			}
		}

		return flattenedFiles;
	}

	// protected void openTransferedFile(String file) {
	// String testFile = file.toLowerCase();
	//
	// boolean isValid = ((testFile.endsWith(".pdf"))
	// || (testFile.endsWith(".fdf")) || (testFile.endsWith(".tif"))
	// || (testFile.endsWith(".tiff")) || (testFile.endsWith(".png"))
	// || (testFile.endsWith(".jpg")) || (testFile.endsWith(".jpeg")));
	//
	// if (isValid) {
	// currentCommands.openTransferedFile(file);
	// } else {
	// currentGUI.showMessageDialog("You may only import a valid PDF or image");
	// }
	// }
}
