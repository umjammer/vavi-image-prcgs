/*
 * https://maaberu.web.fc2.com/prcgs.htm
 */

package vavi.awt.image.prcgs;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.logging.Level;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * PRCGS image encoder.
 *
 * @version 1.0 2019/5/2
 * @author JE4SMQ
 * @author JN3NTN (PRCGS-Press part)
 */
@PropsEntity(url = "classpath:prcgs.properties")
public class PrcgsEncoder {

    @Property
    String computer = "vavi";
    @Property
    String encoder = "vavi";
    @Property
    String author = "vavi";
    @Property
    String comment = "made by PrcgsEncoder";

    /** */
    public PrcgsEncoder() {
        try {
            PropsEntity.Util.bind(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Outputs an image as a PRCGS stream. */
    public void encode(BufferedImage image, OutputStream os) throws Exception {

        int w = image.getWidth();
        int h = image.getHeight();

        PrcgsHeader header = new PrcgsHeader();

        long t = System.currentTimeMillis();
        header.setTime(t);

        header.computer = this.computer;
        header.encoder = this.encoder;
        header.author = this.author;
        header.width = w;
        header.height = h;
        header.isCompressed = PrcgsHeader.COMPRESSED;
        header.isMono = PrcgsHeader.COLOR;

        header.setComment(this.comment);

Debug.println(Level.FINE, "type: " + image.getType());
        byte[] packed = ((DataBufferByte) image.getRaster().getDataBuffer()).getData(); // TODO assume 3BYTE_RGB

        // w * h * (r, g, b) planar
        byte[] planar = new byte[w * h * 3];
        for (int i = 0; i < w * h; i++) {
            switch (image.getType()) {
            case BufferedImage.TYPE_4BYTE_ABGR:
                planar[w * h * 0 + i] = packed[i * 4 + 3];
                planar[w * h * 1 + i] = packed[i * 4 + 2];
                planar[w * h * 2 + i] = packed[i * 4 + 1];
                break;
            default: // TODO implement other types
                throw new UnsupportedOperationException("type: " + image.getType());
            }
        }

        // PRCGS-Press
        // PRCGS RLE(ARC.c)

        // rgb planar
        ByteArrayInputStream cis = new ByteArrayInputStream(planar);

        // intermediate data (5bit color and same color continuous length)
        // (index means length from -1, 1 to 254, and 0 means 1 dot)
        ByteArrayOutputStream mos = new ByteArrayOutputStream();

        //
        // make intermediate data
        //
        final int MAX = 254;

        // read first dot from rgb planar
        int total = 0;
        // same color dots count
        int lengthAsIndex = 0;
        // for counting length data
        int[] countTable = new int[256];
        // record 1st dot for comparison
        int c = cis.read();
        // make color 5bit
        int prevColor = c >> 3;

        // compare if same color or not to previous from 2nd dot
        while (true) {
            boolean maxReached = false;
            // read current color
            c = cis.read();
            if (c == -1) {
                break;
            }
            int currColor = c >> 3;
            if (prevColor == currColor) {
                // reset when previous color is different from current
                lengthAsIndex++;
                maxReached = lengthAsIndex >= MAX || cis.available() == 0;
            }
            if (prevColor != currColor || maxReached) {
                // when previous color is different or length is over MAX
                mos.write(prevColor);
                mos.write(lengthAsIndex);
                // count color, length
                total += lengthAsIndex + 1;
                // increment: index is real length, data is count
                countTable[lengthAsIndex]++;
                if (lengthAsIndex == MAX) {
                    c = cis.read();
                    if (c == -1) {
                        break;
                    }
                    currColor = c >> 3;
                }
                lengthAsIndex = 0;
            }
            prevColor = currColor;
        }
        // flush last data
        mos.write(prevColor);
        mos.write(lengthAsIndex);
        // count "color and length"
        total += lengthAsIndex + 1;
        // increment: index is real length, data is count
        countTable[lengthAsIndex]++;
Debug.printf(Level.FINE, "intermediate: %d bytes, read: %d bytes, total: %d", mos.size(), planar.length, total);

        //
        // select mostly used as compression dictionary(1-7) (0 means 1 dot)
        //
        byte[][] sortTable = new byte[2][256];
        for (int i = 0; i < 256; i++) {
            sortTable[0][i] = (byte) i;
            sortTable[1][i] = (byte) countTable[i];
        }
        sortTable[1][0] = 0;
        // sort by times
        for (int i = 0; i < 255; i++) {
            for (int j = i + 1; j < 256; j++) {
                // select mostly used
                int hi = sortTable[1][i] * sortTable[0][i];
                int hj = sortTable[1][j] * sortTable[0][j];
                if (hi < hj) {
                    byte tmp = sortTable[1][j];
                    sortTable[1][j] = sortTable[1][i];
                    sortTable[1][i] = tmp;
                    tmp = sortTable[0][j];
                    sortTable[0][j] = sortTable[0][i];
                    sortTable[0][i] = tmp;
                }
            }
        }
Debug.println(Level.FINE, "selected compression");
Debug.println(Level.FINE, "length, times");

        for (int i = 0; i < 7; i++) {
Debug.printf(Level.FINE, "%d. %d\n", sortTable[0][i], sortTable[1][i]);
            header.compDict[i] = sortTable[0][i];
        }

        //
        // output PRC compression
        //

        // convert intermediate data to output data
        // (upper 3bit:index of length, lower 5bit:color)
        for (int i = 0; i < 6; i++) {
            int l = i + 1;
            for (int j = l; j < 7; j++) {
                if (header.compDict[i] < header.compDict[j]) {
                    byte tmp = header.compDict[i];
                    header.compDict[i] = header.compDict[j];
                    header.compDict[j] = tmp;
                }
            }
        }

        // output header
        header.serialize(os);

        // intermediate data
        ByteArrayInputStream mis = new ByteArrayInputStream(mos.toByteArray());

        // shift table origin to 1
        byte[] compDict = new byte[8];
        for (int i = 0; i < 7; i++) {
            compDict[i] = (byte) (header.compDict[i] + 1);
        }
        // last search index must be 1 (dot)
        compDict[7] = 1;
        int dataCount = 0;
        int writeCount = 0; // write count
        while (true) {
            int color = mis.read();
            if (color == -1) {
                break;
            }
            int length = mis.read();
            if (length == -1) {
                break;
            }
            length += 1;
            int compDictIndex = 0;
            do {
                int shift = compDictIndex + 1;
                if (shift >= 8) shift = 0;
                // compare index from larger
                // output when data length <= index
                int alen = compDict[compDictIndex];
                if (alen <= length) {
                    int written = (shift << 5) | color;
                    os.write(written);
                    writeCount++;
                    dataCount = dataCount + length;
                    // subtract length written
                    length = length - alen;
                }
                if (alen > length) compDictIndex++;
            } while (length > 0);
        }
Debug.printf(Level.FINE, "in: %d bytes, total out: %d bytes, pixels: %d, compression: %3.1f%%", mos.size(), writeCount + 128, dataCount, (float) writeCount / dataCount * 100);
    }
}
