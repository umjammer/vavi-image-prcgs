/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.image.prcgs;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.logging.Level;

import vavi.util.Debug;


/**
 * PrcgsHeader.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-12-28 nsano initial version <br>
 */
class PrcgsHeader {

    /** 0-4 */
    byte[] signature = new byte[5];
    /** at where: 5-20 */
    String computer;
    /** by what encoder: 21-28 */
    String encoder;
    /** by who: 29-36 */
    String author;
    /** date part of when: 37-44 */
    String date;
    /** time part of when: 45-52 */
    String time;
    /** unsigned short:53,54 */
    int width;
    /** unsigned short:55,56 */
    int height;
    /** 57 */
    int isCompressed;
    /** 58-64 */
    byte[] compDict = new byte[7];
    /** 65 */
    int isMono;
    /** 66-126 */
    byte[] comment = new byte[61];

    /** for compressed image */
    public static final int COMPRESSED = 0x10;

    /** is compressed or not */
    boolean isCompressed() {
        return isCompressed != 0;
    }

    /** for color image */
    public static final int COLOR = 0;

    /** is monotone image or not */
    boolean isMono() {
        return isMono != 0;
    }

    @Override
    public String toString() {
        return "Header{" +
                "signature=" + Arrays.toString(signature) +
                ", computer='" + computer + '\'' +
                ", encoder='" + encoder + '\'' +
                ", author='" + author + '\'' +
                ", datetime='" + Instant.ofEpochMilli(getTime()) + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", isCompressed=" + isCompressed +
                ", compDict=" + Arrays.toString(compDict) +
                ", isMono=" + isMono +
                ", comment=" + trimZero(new String(comment, 0, comment.length, Charset.forName("JisAUTODetect"))) +
                '}';
    }

    /** trim as asciiz strings */
    private static String trimZero(String s) {
        return s.indexOf('\0') > -1 ? s.substring(0, s.indexOf('\0')) : s;
    }

    /** Deserializes a PRCGS header. */
    public void deserialize(InputStream is) throws IOException {
        DataInput di = new DataInputStream(is);

        di.readFully(signature);
        // check major version
        // not check minor version
        if (signature[0] != 0x50 || signature[1] != 0x5F || signature[2] != 0x33) {
            throw new IllegalArgumentException("not PRCGS image");
        }

        byte[] buf = new byte[16];
        // running on
        di.readFully(buf);
        computer = trimZero(new String(buf, 0, 16));
        // encoder
        di.readFully(buf, 0, 8);
        encoder = trimZero(new String(buf, 0, 8));
        // author
        di.readFully(buf, 0, 8);
        author = trimZero(new String(buf, 0, 8));

        // date created YY/MM/DD
        di.readFully(buf, 0, 8);
        date = new String(buf, 0, 8);
        // time created YY:MM:DD
        di.readFully(buf, 0, 8);
        time = new String(buf, 0, 8);

        width = di.readUnsignedShort();
        height = di.readUnsignedShort();

        // compression: 0x00: not compressed, else: compressed
        isCompressed = di.readUnsignedByte();
        // compressed lengths
        di.readFully(compDict, 0, 7);
        // mono: 1,  else: color
        isMono = di.readUnsignedByte();
        //
        di.readFully(comment, 0, 61);
    }

    /** @param t epoc millis */
    public void setTime(long t) {
        date = String.format("%1$ty/%1$tm/%1$td", t);
Debug.println(Level.FINE, "date: " + date);
        time = String.format("%tT", t);
Debug.println(Level.FINE, "time: " + time);
    }

    /** "yy/MM/dd'T'HH:mm:ss" */
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yy/MM/dd'T'HH:mm:ss");

    /** @return epoc millis */
    public long getTime() {
        return LocalDateTime.parse(date + "T" + time, formatter).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /** */
    public void setComment(String comment) {
        Arrays.fill(this.comment, (byte) 0);
        fillString(comment, this.comment);
    }

    /** string to byte array in asciiz */
    private static byte[] fillString(String s, byte[] b) {
        byte[] bytes = s.getBytes(Charset.forName("Shift_JIS"));
        System.arraycopy(bytes, 0, b, 0, Math.min(bytes.length, b.length));
        return b;
    }

    /** writes string as asciiz filled by 0 to the length */
    private static void writeString(DataOutput dos, String s, int length) throws IOException {
        dos.write(fillString(s, new byte[length]));
    }

    /** Serializes a PRCGS header. */
    public void serialize(OutputStream os) throws IOException {
        DataOutput dos = new DataOutputStream(os);

        dos.write("P_305".getBytes());
Debug.println("signature: " + "P_305");
        writeString(dos, computer, 16);
Debug.println("computer: " + computer);
        writeString(dos, encoder, 8);
Debug.println("encoder:" + encoder);
        writeString(dos, author, 8);
Debug.println("author: " + author);
        dos.write(date.getBytes());
        dos.write(time.getBytes());
        dos.writeShort(width);
        dos.writeShort(height);
Debug.printf("size: " + width + "x" + height);
        dos.write(isCompressed);
Debug.println("isCompressed: " + isCompressed);
        dos.write(compDict);
        dos.write(isMono);
Debug.println("isMono: " + isMono);
        dos.write(comment);
    }
}
