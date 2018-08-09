/**
 * ===========================================
 * Java Pdf Extraction Decoding Access Library
 * ===========================================
 *
 * Project Info:  http://www.jpedal.org
 * (C) Copyright 1997-2008, IDRsolutions and Contributors.
 * Main Developer: Simon Barnett
 *
 * 	This file is part of JPedal
 *
 * Copyright (c) 2008, IDRsolutions
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the IDRsolutions nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY IDRsolutions ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL IDRsolutions BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Other JBIG2 image decoding implementations include
 * jbig2dec (http://jbig2dec.sourceforge.net/)
 * xpdf (http://www.foolabs.com/xpdf/)
 * 
 * The final draft JBIG2 specification can be found at http://www.jpeg.org/public/fcd14492.pdf
 * 
 * All three of the above resources were used in the writing of this software, with methodologies,
 * processes and inspiration taken from all three.
 *
 * ---------------
 * SymbolDictionarySegment.java
 * ---------------
 */
package org.jpedal.jbig2.segment.symboldictionary;

import java.io.IOException;

import org.jpedal.jbig2.JBIG2Exception;
import org.jpedal.jbig2.decoders.ArithmeticDecoderStats;
import org.jpedal.jbig2.decoders.DecodeIntResult;
import org.jpedal.jbig2.decoders.HuffmanDecoder;
import org.jpedal.jbig2.decoders.JBIG2StreamDecoder;
import org.jpedal.jbig2.image.JBIG2Bitmap;
import org.jpedal.jbig2.segment.Segment;
import org.jpedal.jbig2.util.BinaryOperation;

public class SymbolDictionarySegment extends Segment {

	private int noOfExportedSymbols;
	private int noOfNewSymbols;

	short[] symbolDictionaryAdaptiveTemplateX = new short[4], symbolDictionaryAdaptiveTemplateY = new short[4];
	short[] symbolDictionaryRAdaptiveTemplateX = new short[2], symbolDictionaryRAdaptiveTemplateY = new short[2];

	private JBIG2Bitmap[] bitmaps;

	private SymbolDictionaryFlags symbolDictionaryFlags = new SymbolDictionaryFlags();

	private ArithmeticDecoderStats genericRegionStats;
	private ArithmeticDecoderStats refinementRegionStats;

	public SymbolDictionarySegment(JBIG2StreamDecoder streamDecoder) {
		super(streamDecoder);
	}

	@Override
	public void readSegment() throws IOException, JBIG2Exception {

		if (JBIG2StreamDecoder.debug) System.out.println("==== Read Segment Symbol Dictionary ====");

		/** read symbol dictionary flags */
		readSymbolDictionaryFlags();

		// List codeTables = new ArrayList();
		int numberOfInputSymbols = 0;
		int noOfReferredToSegments = this.segmentHeader.getReferredToSegmentCount();
		int[] referredToSegments = this.segmentHeader.getReferredToSegments();

		for (int i = 0; i < noOfReferredToSegments; i++) {
			Segment seg = this.decoder.findSegment(referredToSegments[i]);
			int type = seg.getSegmentHeader().getSegmentType();

			if (type == Segment.SYMBOL_DICTIONARY) {
				numberOfInputSymbols += ((SymbolDictionarySegment) seg).noOfExportedSymbols;
			}
			else
				if (type == Segment.TABLES) {
					// codeTables.add(seg);
				}
		}

		int symbolCodeLength = 0;
		int i = 1;
		while (i < numberOfInputSymbols + this.noOfNewSymbols) {
			symbolCodeLength++;
			i <<= 1;
		}

		JBIG2Bitmap[] bitmaps = new JBIG2Bitmap[numberOfInputSymbols + this.noOfNewSymbols];

		int k = 0;
		SymbolDictionarySegment inputSymbolDictionary = null;
		for (i = 0; i < noOfReferredToSegments; i++) {
			Segment seg = this.decoder.findSegment(referredToSegments[i]);
			if (seg.getSegmentHeader().getSegmentType() == Segment.SYMBOL_DICTIONARY) {
				inputSymbolDictionary = (SymbolDictionarySegment) seg;
				for (int j = 0; j < inputSymbolDictionary.noOfExportedSymbols; j++) {
					bitmaps[k++] = inputSymbolDictionary.bitmaps[j];
				}
			}
		}

		int[][] huffmanDHTable = null;
		int[][] huffmanDWTable = null;

		int[][] huffmanBMSizeTable = null;
		int[][] huffmanAggInstTable = null;

		boolean sdHuffman = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_HUFF) != 0;
		int sdHuffmanDifferenceHeight = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_HUFF_DH);
		int sdHuffmanDiferrenceWidth = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_HUFF_DW);
		int sdHuffBitmapSize = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_HUFF_BM_SIZE);
		int sdHuffAggregationInstances = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_HUFF_AGG_INST);

		i = 0;
		if (sdHuffman) {
			if (sdHuffmanDifferenceHeight == 0) {
				huffmanDHTable = HuffmanDecoder.huffmanTableD;
			}
			else
				if (sdHuffmanDifferenceHeight == 1) {
					huffmanDHTable = HuffmanDecoder.huffmanTableE;
				}
				else {
					// huffmanDHTable = ((JBIG2CodeTable) codeTables.get(i++)).getHuffTable();
				}

			if (sdHuffmanDiferrenceWidth == 0) {
				huffmanDWTable = HuffmanDecoder.huffmanTableB;
			}
			else
				if (sdHuffmanDiferrenceWidth == 1) {
					huffmanDWTable = HuffmanDecoder.huffmanTableC;
				}
				else {
					// huffmanDWTable = ((JBIG2CodeTable) codeTables.get(i++)).getHuffTable();
				}

			if (sdHuffBitmapSize == 0) {
				huffmanBMSizeTable = HuffmanDecoder.huffmanTableA;
			}
			else {
				// huffmanBMSizeTable = ((JBIG2CodeTable) codeTables.get(i++)).getHuffTable();
			}

			if (sdHuffAggregationInstances == 0) {
				huffmanAggInstTable = HuffmanDecoder.huffmanTableA;
			}
			else {
				// huffmanAggInstTable = ((JBIG2CodeTable) codeTables.get(i++)).getHuffTable();
			}
		}

		int contextUsed = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.BITMAP_CC_USED);
		int sdTemplate = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_TEMPLATE);

		if (!sdHuffman) {
			if (contextUsed != 0 && inputSymbolDictionary != null) {
				this.arithmeticDecoder.resetGenericStats(sdTemplate, inputSymbolDictionary.genericRegionStats);
			}
			else {
				this.arithmeticDecoder.resetGenericStats(sdTemplate, null);
			}
			this.arithmeticDecoder.resetIntStats(symbolCodeLength);
			this.arithmeticDecoder.start();
		}

		int sdRefinementAggregate = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_REF_AGG);
		int sdRefinementTemplate = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_R_TEMPLATE);
		if (sdRefinementAggregate != 0) {
			if (contextUsed != 0 && inputSymbolDictionary != null) {
				this.arithmeticDecoder.resetRefinementStats(sdRefinementTemplate, inputSymbolDictionary.refinementRegionStats);
			}
			else {
				this.arithmeticDecoder.resetRefinementStats(sdRefinementTemplate, null);
			}
		}

		int deltaWidths[] = new int[this.noOfNewSymbols];

		int deltaHeight = 0;
		i = 0;

		while (i < this.noOfNewSymbols) {

			int instanceDeltaHeight = 0;

			if (sdHuffman) {
				instanceDeltaHeight = this.huffmanDecoder.decodeInt(huffmanDHTable).intResult();
			}
			else {
				instanceDeltaHeight = this.arithmeticDecoder.decodeInt(this.arithmeticDecoder.iadhStats).intResult();
			}

			if (instanceDeltaHeight < 0 && -instanceDeltaHeight >= deltaHeight) {
				if (JBIG2StreamDecoder.debug) System.out.println("Bad delta-height value in JBIG2 symbol dictionary");
			}

			deltaHeight += instanceDeltaHeight;
			int symbolWidth = 0;
			int totalWidth = 0;
			int j = i;

			while (true) {

				int deltaWidth = 0;

				DecodeIntResult decodeIntResult;
				if (sdHuffman) {
					decodeIntResult = this.huffmanDecoder.decodeInt(huffmanDWTable);
				}
				else {
					decodeIntResult = this.arithmeticDecoder.decodeInt(this.arithmeticDecoder.iadwStats);
				}

				if (!decodeIntResult.booleanResult()) break;

				deltaWidth = decodeIntResult.intResult();

				if (deltaWidth < 0 && -deltaWidth >= symbolWidth) {
					if (JBIG2StreamDecoder.debug) System.out.println("Bad delta-width value in JBIG2 symbol dictionary");
				}

				symbolWidth += deltaWidth;

				if (sdHuffman && sdRefinementAggregate == 0) {
					deltaWidths[i] = symbolWidth;
					totalWidth += symbolWidth;

				}
				else
					if (sdRefinementAggregate == 1) {

						int refAggNum = 0;

						if (sdHuffman) {
							refAggNum = this.huffmanDecoder.decodeInt(huffmanAggInstTable).intResult();
						}
						else {
							refAggNum = this.arithmeticDecoder.decodeInt(this.arithmeticDecoder.iaaiStats).intResult();
						}

						if (refAggNum == 1) {

							int symbolID = 0, referenceDX = 0, referenceDY = 0;

							if (sdHuffman) {
								symbolID = this.decoder.readBits(symbolCodeLength);
								referenceDX = this.huffmanDecoder.decodeInt(HuffmanDecoder.huffmanTableO).intResult();
								referenceDY = this.huffmanDecoder.decodeInt(HuffmanDecoder.huffmanTableO).intResult();

								this.decoder.consumeRemainingBits();
								this.arithmeticDecoder.start();
							}
							else {
								symbolID = (int) this.arithmeticDecoder.decodeIAID(symbolCodeLength, this.arithmeticDecoder.iaidStats);
								referenceDX = this.arithmeticDecoder.decodeInt(this.arithmeticDecoder.iardxStats).intResult();
								referenceDY = this.arithmeticDecoder.decodeInt(this.arithmeticDecoder.iardyStats).intResult();
							}

							JBIG2Bitmap referredToBitmap = bitmaps[symbolID];

							JBIG2Bitmap bitmap = new JBIG2Bitmap(symbolWidth, deltaHeight, this.arithmeticDecoder, this.huffmanDecoder,
									this.mmrDecoder);
							bitmap.readGenericRefinementRegion(sdRefinementTemplate, false, referredToBitmap, referenceDX, referenceDY,
									this.symbolDictionaryRAdaptiveTemplateX, this.symbolDictionaryRAdaptiveTemplateY);

							bitmaps[numberOfInputSymbols + i] = bitmap;

						}
						else {
							JBIG2Bitmap bitmap = new JBIG2Bitmap(symbolWidth, deltaHeight, this.arithmeticDecoder, this.huffmanDecoder,
									this.mmrDecoder);
							bitmap.readTextRegion(sdHuffman, true, refAggNum, 0, numberOfInputSymbols + i, null, symbolCodeLength, bitmaps, 0, 0,
									false, 1, 0, HuffmanDecoder.huffmanTableF, HuffmanDecoder.huffmanTableH, HuffmanDecoder.huffmanTableK,
									HuffmanDecoder.huffmanTableO, HuffmanDecoder.huffmanTableO, HuffmanDecoder.huffmanTableO,
									HuffmanDecoder.huffmanTableO, HuffmanDecoder.huffmanTableA, sdRefinementTemplate,
									this.symbolDictionaryRAdaptiveTemplateX, this.symbolDictionaryRAdaptiveTemplateY, this.decoder);

							bitmaps[numberOfInputSymbols + i] = bitmap;
						}
					}
					else {
						JBIG2Bitmap bitmap = new JBIG2Bitmap(symbolWidth, deltaHeight, this.arithmeticDecoder, this.huffmanDecoder, this.mmrDecoder);
						bitmap.readBitmap(false, sdTemplate, false, false, null, this.symbolDictionaryAdaptiveTemplateX,
								this.symbolDictionaryAdaptiveTemplateY, 0);
						bitmaps[numberOfInputSymbols + i] = bitmap;
					}

				i++;
			}

			if (sdHuffman && sdRefinementAggregate == 0) {
				int bmSize = this.huffmanDecoder.decodeInt(huffmanBMSizeTable).intResult();
				this.decoder.consumeRemainingBits();

				JBIG2Bitmap collectiveBitmap = new JBIG2Bitmap(totalWidth, deltaHeight, this.arithmeticDecoder, this.huffmanDecoder, this.mmrDecoder);

				if (bmSize == 0) {

					int padding = totalWidth % 8;
					int bytesPerRow = (int) Math.ceil(totalWidth / 8d);

					// short[] bitmap = new short[totalWidth];
					// decoder.readByte(bitmap);
					int size = deltaHeight * ((totalWidth + 7) >> 3);
					short[] bitmap = new short[size];
					this.decoder.readByte(bitmap);

					short[][] logicalMap = new short[deltaHeight][bytesPerRow];
					int count = 0;
					for (int row = 0; row < deltaHeight; row++) {
						for (int col = 0; col < bytesPerRow; col++) {
							logicalMap[row][col] = bitmap[count];
							count++;
						}
					}

					int collectiveBitmapRow = 0, collectiveBitmapCol = 0;

					for (int row = 0; row < deltaHeight; row++) {
						for (int col = 0; col < bytesPerRow; col++) {
							if (col == (bytesPerRow - 1)) { // this is the last
								// byte in the row
								short currentByte = logicalMap[row][col];
								for (int bitPointer = 7; bitPointer >= padding; bitPointer--) {
									short mask = (short) (1 << bitPointer);
									int bit = (currentByte & mask) >> bitPointer;

									collectiveBitmap.setPixel(collectiveBitmapCol, collectiveBitmapRow, bit);
									collectiveBitmapCol++;
								}
								collectiveBitmapRow++;
								collectiveBitmapCol = 0;
							}
							else {
								short currentByte = logicalMap[row][col];
								for (int bitPointer = 7; bitPointer >= 0; bitPointer--) {
									short mask = (short) (1 << bitPointer);
									int bit = (currentByte & mask) >> bitPointer;

									collectiveBitmap.setPixel(collectiveBitmapCol, collectiveBitmapRow, bit);
									collectiveBitmapCol++;
								}
							}
						}
					}

				}
				else {
					collectiveBitmap.readBitmap(true, 0, false, false, null, null, null, bmSize);
				}

				int x = 0;
				while (j < i) {
					bitmaps[numberOfInputSymbols + j] = collectiveBitmap.getSlice(x, 0, deltaWidths[j], deltaHeight);
					x += deltaWidths[j];

					j++;
				}
			}
		}

		this.bitmaps = new JBIG2Bitmap[this.noOfExportedSymbols];

		int j = i = 0;
		boolean export = false;
		while (i < numberOfInputSymbols + this.noOfNewSymbols) {

			int run = 0;
			if (sdHuffman) {
				run = this.huffmanDecoder.decodeInt(HuffmanDecoder.huffmanTableA).intResult();
			}
			else {
				run = this.arithmeticDecoder.decodeInt(this.arithmeticDecoder.iaexStats).intResult();
			}

			if (export) {
				for (int cnt = 0; cnt < run; cnt++) {
					this.bitmaps[j++] = bitmaps[i++];
				}
			}
			else {
				i += run;
			}

			export = !export;
		}

		int contextRetained = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.BITMAP_CC_RETAINED);
		if (!sdHuffman && contextRetained == 1) {
			this.genericRegionStats = this.genericRegionStats.copy();
			if (sdRefinementAggregate == 1) {
				this.refinementRegionStats = this.refinementRegionStats.copy();
			}
		}

		/** consume any remaining bits */
		this.decoder.consumeRemainingBits();
	}

	private void readSymbolDictionaryFlags() throws IOException {
		/** extract symbol dictionary flags */
		short[] symbolDictionaryFlagsField = new short[2];
		this.decoder.readByte(symbolDictionaryFlagsField);

		int flags = BinaryOperation.getInt16(symbolDictionaryFlagsField);
		this.symbolDictionaryFlags.setFlags(flags);

		if (JBIG2StreamDecoder.debug) System.out.println("symbolDictionaryFlags = " + flags);

		// symbol dictionary AT flags
		int sdHuff = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_HUFF);
		int sdTemplate = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_TEMPLATE);
		if (sdHuff == 0) {
			if (sdTemplate == 0) {
				this.symbolDictionaryAdaptiveTemplateX[0] = readATValue();
				this.symbolDictionaryAdaptiveTemplateY[0] = readATValue();
				this.symbolDictionaryAdaptiveTemplateX[1] = readATValue();
				this.symbolDictionaryAdaptiveTemplateY[1] = readATValue();
				this.symbolDictionaryAdaptiveTemplateX[2] = readATValue();
				this.symbolDictionaryAdaptiveTemplateY[2] = readATValue();
				this.symbolDictionaryAdaptiveTemplateX[3] = readATValue();
				this.symbolDictionaryAdaptiveTemplateY[3] = readATValue();
			}
			else {
				this.symbolDictionaryAdaptiveTemplateX[0] = readATValue();
				this.symbolDictionaryAdaptiveTemplateY[0] = readATValue();
			}
		}

		// symbol dictionary refinement AT flags
		int refAgg = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_REF_AGG);
		int sdrTemplate = this.symbolDictionaryFlags.getFlagValue(SymbolDictionaryFlags.SD_R_TEMPLATE);
		if (refAgg != 0 && sdrTemplate == 0) {
			this.symbolDictionaryRAdaptiveTemplateX[0] = readATValue();
			this.symbolDictionaryRAdaptiveTemplateY[0] = readATValue();
			this.symbolDictionaryRAdaptiveTemplateX[1] = readATValue();
			this.symbolDictionaryRAdaptiveTemplateY[1] = readATValue();
		}

		/** extract no of exported symbols */
		short[] noOfExportedSymbolsField = new short[4];
		this.decoder.readByte(noOfExportedSymbolsField);

		int noOfExportedSymbols = BinaryOperation.getInt32(noOfExportedSymbolsField);
		this.noOfExportedSymbols = noOfExportedSymbols;

		if (JBIG2StreamDecoder.debug) System.out.println("noOfExportedSymbols = " + noOfExportedSymbols);

		/** extract no of new symbols */
		short[] noOfNewSymbolsField = new short[4];
		this.decoder.readByte(noOfNewSymbolsField);

		int noOfNewSymbols = BinaryOperation.getInt32(noOfNewSymbolsField);
		this.noOfNewSymbols = noOfNewSymbols;

		if (JBIG2StreamDecoder.debug) System.out.println("noOfNewSymbols = " + noOfNewSymbols);
	}

	public int getNoOfExportedSymbols() {
		return this.noOfExportedSymbols;
	}

	public void setNoOfExportedSymbols(int noOfExportedSymbols) {
		this.noOfExportedSymbols = noOfExportedSymbols;
	}

	public int getNoOfNewSymbols() {
		return this.noOfNewSymbols;
	}

	public void setNoOfNewSymbols(int noOfNewSymbols) {
		this.noOfNewSymbols = noOfNewSymbols;
	}

	public JBIG2Bitmap[] getBitmaps() {
		return this.bitmaps;
	}

	public SymbolDictionaryFlags getSymbolDictionaryFlags() {
		return this.symbolDictionaryFlags;
	}

	public void setSymbolDictionaryFlags(SymbolDictionaryFlags symbolDictionaryFlags) {
		this.symbolDictionaryFlags = symbolDictionaryFlags;
	}

	private ArithmeticDecoderStats getGenericRegionStats() {
		return this.genericRegionStats;
	}

	private void setGenericRegionStats(ArithmeticDecoderStats genericRegionStats) {
		this.genericRegionStats = genericRegionStats;
	}

	private void setRefinementRegionStats(ArithmeticDecoderStats refinementRegionStats) {
		this.refinementRegionStats = refinementRegionStats;
	}

	private ArithmeticDecoderStats getRefinementRegionStats() {
		return this.refinementRegionStats;
	}
}
