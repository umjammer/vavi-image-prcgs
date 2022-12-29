/*
 * https://maaberu.web.fc2.com/prcgs.htm
 */

package vavi.awt.image.prcgs;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import vavi.util.Debug;


/**
 * PRCGS image encoder.
 *
 * @version 1.0 2019/5/2
 * @author JE4SMQ
 * @author JN3NTN (PRCGS-Press part)
 */
public class PrcgsEncoder {

    /** Outputs an image as a PRCGS stream. */
    public void encode(BufferedImage image, OutputStream os) throws Exception {

        int w = image.getWidth();
        int h = image.getHeight();

        PrcgsHeader header = new PrcgsHeader();

        long t = System.currentTimeMillis();
        header.setTime(t);

        header.computer = "vavi";
        header.encoder = "vavi";
        header.author = "vavi";
        header.width = w;
        header.height = h;
        header.isCompressed = PrcgsHeader.COMPRESSED;
        header.isMono = PrcgsHeader.COLOR;

        header.setComment("made by PrcgsEncoder");

        // BMPファイルをRBG別の配列に格納する(charで5bitカラーへ変換)
Debug.println("type: " + image.getType());
        byte[] raster = ((DataBufferByte) image.getRaster().getDataBuffer()).getData(); // TODO assume 3BYTE_RGB
        // flat w x h plane r, g, b order
        byte[] plane = new byte[w * h * 3];
        for (int i = 0; i < w * h; i++) {
            switch (image.getType()) {
            case BufferedImage.TYPE_4BYTE_ABGR:
                plane[w * h * 0 + i] = raster[i * 4 + 3];
                plane[w * h * 1 + i] = raster[i * 4 + 2];
                plane[w * h * 2 + i] = raster[i * 4 + 1];
                break;
            }
        }

        // PRCGS-Press
        // PRCGS RLE(ARC.c)

        // [length, color] * w * h (length is 0 origin, 0 means 1 dot)
        byte[][] rgb = new byte[w * h][2];
        for (int i = 0; i < w * h; i++) {
            rgb[i][0] = 0;
            rgb[i][1] = 0;
        }

        ByteArrayInputStream pis = new ByteArrayInputStream(plane);
        // length, color
        ByteArrayOutputStream cos = new ByteArrayOutputStream();

        // 8ビットから5ビットに減らした階調と長さをCOLW(1,2)へ格納
        // 最初の1ドット目は比較の元として記録する
        int cmax = 254;
        // 同じ階調のカウントfcount = 0;
        // rgb BMP1件目を読み出し
        int writeCount = 0;
        int wmo = 0;
        int countIndex = 0;
        // 長さのデータ調査用
        int[] countTable = new int[256];
        int bmpc = pis.read();
        // covert 5bit
        int pastd = bmpc >> 3;
        // compare if same color or not to previous dot from 2nd dot
        while (true) {
            // brは長さの上限(cmax)に達したら1)
            int br = 0;
            // 現在の色を取得する
            bmpc = pis.read();
            if (bmpc == -1) {
                break;
            }
            int newd = (bmpc & 0xff) >> 3;
            // 1つ手前と違う階調になったら必ず今までの内容を出力してリセットする
            if (pastd == newd) {
                // 圧縮率: 階調データを仮作成
                // 引数: 長さ -1, 1~254 迄で、長さ 0 は必ず 1 ドット
                // COLW: 0: 階調, 1: 並べるドット数
                // 階調、長さの中間ファイルへ記録
                countIndex++;
                if (countIndex >= cmax || pis.available() == 0) br = 1;
            }
            // 1つ手前と違う階調か、長さ上限を越えたらファイル出力
            if (pastd != newd || br == 1) {
                // 圧縮率:色のデータを仮作成
                //  0: 色, 1: 圧縮倍率
                // 階調と長さを1バイトづつで書き出し
                cos.write(pastd);
                cos.write(countIndex);
                // 出力階調、長さの件数カウント
                wmo = wmo + countIndex + 1;
                writeCount = writeCount + 2;
                // 長さの統計をカウントアップ 引数が実際の長さで、中身がカウント
                countTable[countIndex]++;
                if (countIndex >= cmax) {
                    bmpc = pis.read();
                    if (bmpc == -1) {
                        break;
                    }
                    newd = (bmpc & 0xff) >> 3;
                }
                countIndex = 0;
            }
            pastd = newd;
        }
        // flush last data
        cos.write(pastd);
        cos.write(countIndex);
        // count "color and length"
        wmo = wmo + countIndex + 1;
        writeCount = writeCount + 2;
        // 長さの統計をカウントアップ index is real length, data is count
        countTable[countIndex]++;
Debug.printf("intermediate: %d bytes, read: %d bytes, total: %d", writeCount, plane.length, wmo);
        // select mostly used as compression dictionary(1-7) (0 means 1 dot)
        byte[][] sortTable = new byte[2][256];
        for (int i = 0; i < 256; i++) {
            sortTable[0][i] = (byte) i;
            sortTable[1][i] = (byte) countTable[i];
        }
        sortTable[1][0] = 0;
        // sort by times
        for (int i = 0; i < 255; i++) {
            int l = i + 1;
            for (int j = l; j < 256; j++) {
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
Debug.println("selected compression");
Debug.println("length, times");

        for (int i = 0; i < 7; i++) {
Debug.printf("%d. %d\n", sortTable[0][i], sortTable[1][i]);
            header.compDict[i] = sortTable[0][i];
        }

        // output PRC compression
        // convert from color and length to upper 3bit:index of length, lower 5bit:color
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

        header.serialize(os);
        // color and length
        ByteArrayInputStream cis = new ByteArrayInputStream(cos.toByteArray());

        // shift table origin to 1
        byte[] compDict = new byte[8];
        for (int i = 0; i < 7; i++) {
            compDict[i] = (byte) (header.compDict[i] + 1);
        }
        // last search index must be 1 (dot)
        compDict[7] = 1;
        int dataCount = 0;
        writeCount = 0; // write count
        while (true) {
            int color = cis.read();
            if (color == -1) {
                break;
            }
            int length = cis.read();
            if (length == -1) {
                break;
            }
            length += 1;
            int compDictIndex = 0;
            do {
                int shift = compDictIndex + 1;
                if (shift >= 8) shift = 0;
                // 長さインデックスを長い方から比較する
                // データの長さ<=インデックスならファイル出力
                int alen = compDict[compDictIndex];
                if (alen <= length) {
                    int written = (shift << 5) | color;
                    os.write(written);
                    writeCount++;
                    dataCount = dataCount + length;
                    // 書き出した長さ分だけマイナス
                    length = length - alen;
                }
                if (alen > length) compDictIndex++;
            } while (length > 0);
        }
Debug.printf("in: %d bytes, total out: %d bytes, pixels: %d, compression: %3.1f%%", cos.size(), writeCount + 128, dataCount, (float) writeCount / dataCount * 100);
    }
}
