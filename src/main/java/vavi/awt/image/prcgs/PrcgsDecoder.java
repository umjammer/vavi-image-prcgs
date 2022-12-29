/*
 * https://maaberu.web.fc2.com/prcgs.htm
 */

package vavi.awt.image.prcgs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import vavi.util.Debug;


/**
 * PRCGS image decoder.
 *
 * @author JE4SMQ
 */
public class PrcgsDecoder {

    /** Decodes a PRCGS image */
    public BufferedImage decode(InputStream is) throws IOException {

        PrcgsHeader header = new PrcgsHeader();
        header.deserialize(is);
Debug.println(Level.FINE, header);

        int w = header.width;
        int h = header.height;
        int[][] vram = new int[3][w * h];
        int x = 0;
        int y = 0;
        int plane = 0;
        while (plane < 3) {
            int datum = is.read();
            if (datum < 0) {
                break;
            }
            int value = (datum >> 5) & 7;
            int color = datum & 0x1F;
            int times;
            if (value == 0) {
                times = 0;
            } else {
                times = header.compDict[value - 1] & 0xff;
            }
            int i = 0;
            do {
                int o = x + y * w;
                if (header.isMono()) {
                    for (int[] v : vram) v[o] = color * 8;
                } else {
                    vram[plane][o] = color * 8;
                }
                x++;
                if (x >= w) {
                    x = 0;
                    y++;
                    if (y >= h) {
                        y = 0;
                        plane++;
                    }
                }
                i++;
            } while (plane < 3 && i <= times);
        }

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = image.createGraphics();
        for (y = 0; y < header.height; y++) {
            for (x = 0; x < header.width; x++) {
                int o = x + y * header.width;
                g2d.setColor(new Color(vram[0][o], vram[1][o], vram[2][o]));
                g2d.fillRect(x, y, 1, 1);
            }
        }
        return image;
    }
}
