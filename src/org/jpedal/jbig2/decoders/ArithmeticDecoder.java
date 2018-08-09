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
 * ArithmeticDecoder.java
 * ---------------
 */
package org.jpedal.jbig2.decoders;

import java.io.IOException;

import org.jpedal.jbig2.io.StreamReader;
import org.jpedal.jbig2.util.BinaryOperation;

public class ArithmeticDecoder {

	private StreamReader reader;

	public ArithmeticDecoderStats genericRegionStats, refinementRegionStats;

	public ArithmeticDecoderStats iadhStats, iadwStats, iaexStats, iaaiStats, iadtStats, iaitStats, iafsStats, iadsStats, iardxStats, iardyStats,
			iardwStats, iardhStats, iariStats, iaidStats;

	int contextSize[] = { 16, 13, 10, 10 }, referredToContextSize[] = { 13, 10 };

	long buffer0, buffer1;
	long c, a;
	long previous;

	int counter;

	private ArithmeticDecoder() {}

	public ArithmeticDecoder(StreamReader reader) {
		this.reader = reader;

		this.genericRegionStats = new ArithmeticDecoderStats(1 << 1);
		this.refinementRegionStats = new ArithmeticDecoderStats(1 << 1);

		this.iadhStats = new ArithmeticDecoderStats(1 << 9);
		this.iadwStats = new ArithmeticDecoderStats(1 << 9);
		this.iaexStats = new ArithmeticDecoderStats(1 << 9);
		this.iaaiStats = new ArithmeticDecoderStats(1 << 9);
		this.iadtStats = new ArithmeticDecoderStats(1 << 9);
		this.iaitStats = new ArithmeticDecoderStats(1 << 9);
		this.iafsStats = new ArithmeticDecoderStats(1 << 9);
		this.iadsStats = new ArithmeticDecoderStats(1 << 9);
		this.iardxStats = new ArithmeticDecoderStats(1 << 9);
		this.iardyStats = new ArithmeticDecoderStats(1 << 9);
		this.iardwStats = new ArithmeticDecoderStats(1 << 9);
		this.iardhStats = new ArithmeticDecoderStats(1 << 9);
		this.iariStats = new ArithmeticDecoderStats(1 << 9);
		this.iaidStats = new ArithmeticDecoderStats(1 << 1);
	}

	public void resetIntStats(int symbolCodeLength) {
		this.iadhStats.reset();
		this.iadwStats.reset();
		this.iaexStats.reset();
		this.iaaiStats.reset();
		this.iadtStats.reset();
		this.iaitStats.reset();
		this.iafsStats.reset();
		this.iadsStats.reset();
		this.iardxStats.reset();
		this.iardyStats.reset();
		this.iardwStats.reset();
		this.iardhStats.reset();
		this.iariStats.reset();

		if (this.iaidStats.getContextSize() == 1 << (symbolCodeLength + 1)) {
			this.iaidStats.reset();
		}
		else {
			this.iaidStats = new ArithmeticDecoderStats(1 << (symbolCodeLength + 1));
		}
	}

	public void resetGenericStats(int template, ArithmeticDecoderStats previousStats) {
		int size = this.contextSize[template];

		if (previousStats != null && previousStats.getContextSize() == size) {
			if (this.genericRegionStats.getContextSize() == size) {
				this.genericRegionStats.overwrite(previousStats);
			}
			else {
				this.genericRegionStats = previousStats.copy();
			}
		}
		else {
			if (this.genericRegionStats.getContextSize() == size) {
				this.genericRegionStats.reset();
			}
			else {
				this.genericRegionStats = new ArithmeticDecoderStats(1 << size);
			}
		}
	}

	public void resetRefinementStats(int template, ArithmeticDecoderStats previousStats) {
		int size = this.referredToContextSize[template];
		if (previousStats != null && previousStats.getContextSize() == size) {
			if (this.refinementRegionStats.getContextSize() == size) {
				this.refinementRegionStats.overwrite(previousStats);
			}
			else {
				this.refinementRegionStats = previousStats.copy();
			}
		}
		else {
			if (this.refinementRegionStats.getContextSize() == size) {
				this.refinementRegionStats.reset();
			}
			else {
				this.refinementRegionStats = new ArithmeticDecoderStats(1 << size);
			}
		}
	}

	public void start() throws IOException {
		this.buffer0 = this.reader.readByte();
		this.buffer1 = this.reader.readByte();

		this.c = BinaryOperation.bit32ShiftL((this.buffer0 ^ 0xff), 16);
		readByte();
		this.c = BinaryOperation.bit32ShiftL(this.c, 7);
		this.counter -= 7;
		this.a = 0x80000000l;
	}

	public DecodeIntResult decodeInt(ArithmeticDecoderStats stats) throws IOException {
		long value;

		this.previous = 1;
		int s = decodeIntBit(stats);
		if (decodeIntBit(stats) != 0) {
			if (decodeIntBit(stats) != 0) {
				if (decodeIntBit(stats) != 0) {
					if (decodeIntBit(stats) != 0) {
						if (decodeIntBit(stats) != 0) {
							value = 0;
							for (int i = 0; i < 32; i++) {
								value = BinaryOperation.bit32ShiftL(value, 1) | decodeIntBit(stats);
							}
							value += 4436;
						}
						else {
							value = 0;
							for (int i = 0; i < 12; i++) {
								value = BinaryOperation.bit32ShiftL(value, 1) | decodeIntBit(stats);
							}
							value += 340;
						}
					}
					else {
						value = 0;
						for (int i = 0; i < 8; i++) {
							value = BinaryOperation.bit32ShiftL(value, 1) | decodeIntBit(stats);
						}
						value += 84;
					}
				}
				else {
					value = 0;
					for (int i = 0; i < 6; i++) {
						value = BinaryOperation.bit32ShiftL(value, 1) | decodeIntBit(stats);
					}
					value += 20;
				}
			}
			else {
				value = decodeIntBit(stats);
				value = BinaryOperation.bit32ShiftL(value, 1) | decodeIntBit(stats);
				value = BinaryOperation.bit32ShiftL(value, 1) | decodeIntBit(stats);
				value = BinaryOperation.bit32ShiftL(value, 1) | decodeIntBit(stats);
				value += 4;
			}
		}
		else {
			value = decodeIntBit(stats);
			value = BinaryOperation.bit32ShiftL(value, 1) | decodeIntBit(stats);
		}

		int decodedInt;
		if (s != 0) {
			if (value == 0) {
				return new DecodeIntResult((int) value, false);
			}
			decodedInt = (int) -value;
		}
		else {
			decodedInt = (int) value;
		}

		return new DecodeIntResult(decodedInt, true);
	}

	public long decodeIAID(long codeLen, ArithmeticDecoderStats stats) throws IOException {
		this.previous = 1;
		for (long i = 0; i < codeLen; i++) {
			int bit = decodeBit(this.previous, stats);
			this.previous = BinaryOperation.bit32ShiftL(this.previous, 1) | bit;
		}

		return this.previous - (1 << codeLen);
	}

	public int decodeBit(long context, ArithmeticDecoderStats stats) throws IOException {
		int iCX = BinaryOperation.bit8Shift(stats.getContextCodingTableValue((int) context), 1, BinaryOperation.RIGHT_SHIFT);
		int mpsCX = stats.getContextCodingTableValue((int) context) & 1;
		int qe = this.qeTable[iCX];

		this.a -= qe;

		int bit;
		if (this.c < this.a) {
			if ((this.a & 0x80000000) != 0) {
				bit = mpsCX;
			}
			else {
				if (this.a < qe) {
					bit = 1 - mpsCX;
					if (this.switchTable[iCX] != 0) {
						stats.setContextCodingTableValue((int) context, (this.nlpsTable[iCX] << 1) | (1 - mpsCX));
					}
					else {
						stats.setContextCodingTableValue((int) context, (this.nlpsTable[iCX] << 1) | mpsCX);
					}
				}
				else {
					bit = mpsCX;
					stats.setContextCodingTableValue((int) context, (this.nmpsTable[iCX] << 1) | mpsCX);
				}
				do {
					if (this.counter == 0) {
						readByte();
					}

					this.a = BinaryOperation.bit32ShiftL(this.a, 1);
					this.c = BinaryOperation.bit32ShiftL(this.c, 1);

					this.counter--;
				}
				while ((this.a & 0x80000000) == 0);
			}
		}
		else {
			this.c -= this.a;

			if (this.a < qe) {
				bit = mpsCX;
				stats.setContextCodingTableValue((int) context, (this.nmpsTable[iCX] << 1) | mpsCX);
			}
			else {
				bit = 1 - mpsCX;
				if (this.switchTable[iCX] != 0) {
					stats.setContextCodingTableValue((int) context, (this.nlpsTable[iCX] << 1) | (1 - mpsCX));
				}
				else {
					stats.setContextCodingTableValue((int) context, (this.nlpsTable[iCX] << 1) | mpsCX);
				}
			}
			this.a = qe;

			do {
				if (this.counter == 0) {
					readByte();
				}

				this.a = BinaryOperation.bit32ShiftL(this.a, 1);
				this.c = BinaryOperation.bit32ShiftL(this.c, 1);

				this.counter--;
			}
			while ((this.a & 0x80000000) == 0);
		}
		return bit;
	}

	private void readByte() throws IOException {
		if (this.buffer0 == 0xff) {
			if (this.buffer1 > 0x8f) {
				this.counter = 8;
			}
			else {
				this.buffer0 = this.buffer1;
				this.buffer1 = this.reader.readByte();
				this.c = this.c + 0xfe00 - (BinaryOperation.bit32ShiftL(this.buffer0, 9));
				this.counter = 7;
			}
		}
		else {
			this.buffer0 = this.buffer1;
			this.buffer1 = this.reader.readByte();
			this.c = this.c + 0xff00 - (BinaryOperation.bit32ShiftL(this.buffer0, 8));
			this.counter = 8;
		}
	}

	private int decodeIntBit(ArithmeticDecoderStats stats) throws IOException {
		int bit;

		bit = decodeBit(this.previous, stats);
		if (this.previous < 0x100) {
			this.previous = BinaryOperation.bit32ShiftL(this.previous, 1) | bit;
		}
		else {
			this.previous = (((BinaryOperation.bit32ShiftL(this.previous, 1)) | bit) & 0x1ff) | 0x100;
		}
		return bit;
	}

	int qeTable[] = { 0x56010000, 0x34010000, 0x18010000, 0x0AC10000, 0x05210000, 0x02210000, 0x56010000, 0x54010000, 0x48010000, 0x38010000,
			0x30010000, 0x24010000, 0x1C010000, 0x16010000, 0x56010000, 0x54010000, 0x51010000, 0x48010000, 0x38010000, 0x34010000, 0x30010000,
			0x28010000, 0x24010000, 0x22010000, 0x1C010000, 0x18010000, 0x16010000, 0x14010000, 0x12010000, 0x11010000, 0x0AC10000, 0x09C10000,
			0x08A10000, 0x05210000, 0x04410000, 0x02A10000, 0x02210000, 0x01410000, 0x01110000, 0x00850000, 0x00490000, 0x00250000, 0x00150000,
			0x00090000, 0x00050000, 0x00010000, 0x56010000 };

	int nmpsTable[] = { 1, 2, 3, 4, 5, 38, 7, 8, 9, 10, 11, 12, 13, 29, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33,
			34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 45, 46 };

	int nlpsTable[] = { 1, 6, 9, 12, 29, 33, 6, 14, 14, 14, 17, 18, 20, 21, 14, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
			30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 46 };

	int switchTable[] = { 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0 };
}
