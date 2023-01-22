/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.imageio.prcgs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.logging.Level;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import vavi.util.Debug;


/**
 * PrcgsImageReaderSpi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 230123 nsano initial version <br>
 */
public class PrcgsImageReaderSpi extends ImageReaderSpi {

    private static final String VendorName = "http://www.vavi.com";
    private static final String Version = "0.0.2";
    private static final String ReaderClassName =
        "vavi.imageio.prcgs.PrcgsImageReader";
    private static final String[] Names = {
        "PRCGS", "prcgs"
    };
    private static final String[] Suffixes = {
        "PRC", "prc", "prcgs"
    };
    private static final String[] mimeTypes = {
        "image/x-prcgs"
    };
    static final String[] WriterSpiNames = {
        /*"vavi.imageio.prcgs.PrcgsImageWriterSpi"*/
    };
    private static final boolean SupportsStandardStreamMetadataFormat = false;
    private static final String NativeStreamMetadataFormatName = null;
    private static final String NativeStreamMetadataFormatClassName = null;
    private static final String[] ExtraStreamMetadataFormatNames = null;
    private static final String[] ExtraStreamMetadataFormatClassNames = null;
    private static final boolean SupportsStandardImageMetadataFormat = false;
    private static final String NativeImageMetadataFormatName = "prcgs";
    private static final String NativeImageMetadataFormatClassName =
        /*"vavi.imageio.recoil.PrcgsMetaData"*/ null;
    private static final String[] ExtraImageMetadataFormatNames = null;
    private static final String[] ExtraImageMetadataFormatClassNames = null;

    /** */
    public PrcgsImageReaderSpi() {
        super(VendorName,
              Version,
              Names,
              Suffixes,
              mimeTypes,
              ReaderClassName,
              new Class[] { ImageInputStream.class, InputStream.class },
              WriterSpiNames,
              SupportsStandardStreamMetadataFormat,
              NativeStreamMetadataFormatName,
              NativeStreamMetadataFormatClassName,
              ExtraStreamMetadataFormatNames,
              ExtraStreamMetadataFormatClassNames,
              SupportsStandardImageMetadataFormat,
              NativeImageMetadataFormatName,
              NativeImageMetadataFormatClassName,
              ExtraImageMetadataFormatNames,
              ExtraImageMetadataFormatClassNames);
    }

    @Override
    public String getDescription(Locale locale) {
        return "PRCGS";
    }

    @Override
    public boolean canDecodeInput(Object obj) throws IOException {
        if (obj instanceof ImageInputStream) {
            ImageInputStream stream = (ImageInputStream) obj;
            stream.mark();
            byte[] b = new byte[5];
            stream.readFully(b);
            boolean r = b[0] == 0x50 && b[1] == 0x5F && b[2] == 0x33;
Debug.println(Level.FINE, "can decode: " + r);
            return r;
        } else {
Debug.println(Level.FINE, obj);
            return false;
        }
    }

    @Override
    public ImageReader createReaderInstance(Object obj) {
        return new PrcgsImageReader(this);
    }
}

/* */
