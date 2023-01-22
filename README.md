[![Release](https://jitpack.io/v/umjammer/vavi-image-prcgs.svg)](https://jitpack.io/#umjammer/vavi-image-prcgs)
[![Java CI](https://github.com/umjammer/vavi-image-prcgs/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-image-prcgs/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-image-prcgs/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-image-prcgs/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-8-b07219)

# vavi-image-prcgs

PRCGS is the image format  locally popular in amateur radio packet communication
for several years in the early Heisei era.<br/>
PRCGS is "Packet Radio Computer Graphic System".

[Original](https://maaberu.web.fc2.com/prcgs.htm)

## Install

 * [maven](https://jitpack.io/#umjammer/vavi-image-prcgs)

## Usage

```java
    BufferedImage image = ImageIO.read(Paths.get("/foo/bar.prc").toFile());
```
## TODO

 * ~~imageio spi~~