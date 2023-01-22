/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.image.prcgs;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test1.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-12-28 nsano initial version <br>
 */
class Test1 {

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
        Path path = Paths.get(Test1.class.getResource("/prcgs/bikan1.prc").toURI());
        BufferedImage image = new PrcgsDecoder().decode(new BufferedInputStream(Files.newInputStream(path)));
        show(image);
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test2() throws Exception {
        Path path = Paths.get(Test1.class.getResource("/prcgs").toURI());
        Files.list(path)
                .filter(p -> p.toString().endsWith(".prc") ||
                             p.toString().endsWith(".PRC"))
                .forEach(p -> {
            try {
Debug.println(p);
                BufferedImage image = new PrcgsDecoder().decode(new BufferedInputStream(Files.newInputStream(p)));
                show(image);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    void show(BufferedImage image) throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);
        JFrame frame = new JFrame("PRCGS");
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { cdl.countDown(); }
        });
        JPanel panel = new JPanel() {
            @Override public void paintComponent(Graphics g) { g.drawImage(image, 0, 0, null); }
        };
        panel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
        cdl.await();
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test3() throws Exception {
        Path prcgs = Paths.get("tmp/out.prc");
        if (!Files.exists(prcgs.getParent())) {
            Files.createDirectories(prcgs.getParent());
        }
        BufferedImage image = ImageIO.read(Paths.get(Test1.class.getResource("/test.png").toURI()).toFile());
        new PrcgsEncoder().encode(image, new BufferedOutputStream(Files.newOutputStream(prcgs)));

        show(new PrcgsDecoder().decode(new BufferedInputStream(Files.newInputStream(prcgs))));
    }

    @Test
    void test4() throws Exception {
        Path prcgs = Paths.get("tmp/out.prc");
        if (!Files.exists(prcgs.getParent())) {
            Files.createDirectories(prcgs.getParent());
        }
        BufferedImage image = ImageIO.read(Paths.get(Test1.class.getResource("/test.png").toURI()).toFile());
        new PrcgsEncoder().encode(image, new BufferedOutputStream(Files.newOutputStream(prcgs)));
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test5() throws Exception {
        BufferedImage image = ImageIO.read(Test1.class.getResourceAsStream("/prcgs/bikan1.prc"));
        show(image);
    }

    @Test
    @DisplayName("spi")
    void test00() throws Exception {
        String[] rs = ImageIO.getReaderFormatNames();
        System.err.println("-- reader --");
        for (String r : rs) {
            System.err.println(r);
        }
        assertTrue(Arrays.asList(rs).contains("PRCGS"));
        String[] ws = ImageIO.getWriterFormatNames();
        System.err.println("-- writer --");
        for (String w : ws) {
            System.err.println(w);
        }
        assertFalse(Arrays.asList(ws).contains("PRCGS"));
    }

    @Test
    @DisplayName("spi specified")
    void test01() throws Exception {
        ImageReader ir = ImageIO.getImageReadersByFormatName("PRCGS").next();
        ImageInputStream iis = ImageIO.createImageInputStream(Test1.class.getResourceAsStream("/prcgs/bikan1.prc"));
        ir.setInput(iis);
        BufferedImage image = ir.read(0);
        assertNotNull(image);
    }

    @Test
    @DisplayName("spi auto")
    void test02() throws Exception {
        BufferedImage image = ImageIO.read(Test1.class.getResourceAsStream("/prcgs/bikan1.prc"));
        assertNotNull(image);
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test03() throws Exception {
        BufferedImage image = ImageIO.read(Test1.class.getResourceAsStream("/prcgs/bikan1.prc"));
        show(image);
    }
}

/* */
