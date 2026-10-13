package com.runecraft.osrs.world;

import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.Region;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Renders a top-down colour/relief map of the {@link Elevation} model (not a test).
 *
 * <pre>java -cp classes:test com.runecraft.osrs.world.TerrainPreview docs/previews/terrain_map.png [tilesPerPixel] [minX minY maxX maxY]</pre>
 * Water is blue by depth, lowlands green, desert sand, swamps olive, mountains brown-grey-white.
 * Town footprints are outlined and landmarks labelled.
 */
public final class TerrainPreview {
    private TerrainPreview() {
    }

    public static void main(String[] args) throws IOException {
        String out = args.length > 0 ? args[0] : "docs/previews/terrain_map.png";
        double tpp = args.length > 1 ? Double.parseDouble(args[1]) : 2;
        double minX = 1060, minY = 2620, maxX = 3880, maxY = 4100;
        if (args.length > 5) {
            minX = Double.parseDouble(args[2]);
            minY = Double.parseDouble(args[3]);
            maxX = Double.parseDouble(args[4]);
            maxY = Double.parseDouble(args[5]);
        }
        boolean labels = args.length <= 6 || !args[6].equals("nolabels");
        int w = (int) Math.round((maxX - minX) / tpp);
        int h = (int) Math.round((maxY - minY) / tpp);
        double[][] hm = new double[h + 2][w + 2];
        for (int j = 0; j < h + 2; j++) {
            for (int i = 0; i < w + 2; i++) {
                double ox = minX + (i - 1) * tpp;
                double oy = maxY - (j - 1) * tpp;
                hm[j][i] = Elevation.height(GielinorMap.toBlockX((int) Math.floor(ox)), GielinorMap.toBlockZ((int) Math.floor(oy)));
            }
        }
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                double ox = minX + i * tpp;
                double oy = maxY - j * tpp;
                double v = hm[j + 1][i + 1];
                int bx = GielinorMap.toBlockX((int) Math.floor(ox));
                int bz = GielinorMap.toBlockZ((int) Math.floor(oy));
                Color c = colour(v, ox, oy, bx, bz);
                if (v > Elevation.SEA_LEVEL) {
                    // hill shading, light from the north-west
                    double gx = (hm[j + 1][i + 2] - hm[j + 1][i]) / (2 * tpp * 2);
                    double gy = (hm[j + 2][i + 1] - hm[j][i + 1]) / (2 * tpp * 2);
                    double shade = Math.max(0.5, Math.min(1.4, 1 - 2.2 * (gx + gy)));
                    c = scale(c, shade);
                }
                img.setRGB(i, j, c.getRGB());
            }
        }
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // structure footprints (levelled cores)
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(120, 40, 30, 110));
        for (TerrainAnchors.Anchor a : TerrainAnchors.get().anchors()) {
            if (a.kind() != TerrainAnchors.Kind.CORE || a.source().startsWith("place:") || a.source().startsWith("landmark:")) {
                continue;
            }
            double x1 = (GielinorMap.toOsrsX(a.x1()) - minX) / tpp;
            double x2 = (GielinorMap.toOsrsX(a.x2()) + 1 - minX) / tpp;
            double y1 = (maxY - GielinorMap.toOsrsY(a.z1()) - 1) / tpp;
            double y2 = (maxY - GielinorMap.toOsrsY(a.z2())) / tpp;
            g.drawRect((int) Math.round(x1), (int) Math.round(y1), Math.max(1, (int) Math.round(x2 - x1)), Math.max(1, (int) Math.round(y2 - y1)));
        }
        if (labels) {
            try {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, (int) Math.round(11 / Math.sqrt(tpp / 2)))));
                for (Landmark l : Landmark.values()) {
                    if (l.isAltar()) {
                        continue;
                    }
                    double x = (GielinorMap.toOsrsX(l.blockX()) - minX) / tpp;
                    double y = (maxY - GielinorMap.toOsrsY(l.blockZ())) / tpp;
                    if (x < 0 || y < 0 || x > w || y > h) {
                        continue;
                    }
                    g.setColor(Color.BLACK);
                    g.fillOval((int) x - 2, (int) y - 2, 5, 5);
                    g.setColor(new Color(0, 0, 0, 160));
                    g.drawString(l.displayName(), (int) x + 5, (int) y + 5);
                    g.setColor(Color.WHITE);
                    g.drawString(l.displayName(), (int) x + 4, (int) y + 4);
                }
                for (GielinorGeography.Ridge r : GielinorGeography.RIDGES) {
                    double[] p = r.xyh();
                    int best = 0;
                    for (int i = 0; i < p.length; i += 3) {
                        if (p[i + 2] > p[best + 2]) {
                            best = i;
                        }
                    }
                    double x = (p[best] - minX) / tpp;
                    double y = (maxY - p[best + 1]) / tpp;
                    if (x < 0 || y < 0 || x > w || y > h) {
                        continue;
                    }
                    g.setColor(new Color(60, 40, 20));
                    g.drawString("^ " + r.name(), (int) x - 4, (int) y + 4);
                }
            } catch (Throwable t) {
                System.err.println("Labels skipped: " + t);
            }
        }
        g.dispose();
        File f = new File(out);
        if (f.getParentFile() != null) {
            f.getParentFile().mkdirs();
        }
        ImageIO.write(img, "png", f);
        System.out.println("Wrote " + out + " (" + w + "x" + h + ")");
    }

    private static Color colour(double v, double ox, double oy, int bx, int bz) {
        if (v <= Elevation.SEA_LEVEL) {
            double depth = Math.max(0, Math.min(1, (Elevation.SEA_LEVEL - v) / 20));
            return mix(new Color(96, 160, 222), new Color(22, 52, 120), depth);
        }
        Region r = GielinorMap.regionAt(ox, oy);
        if (Elevation.isSnowyPeak(bx, bz)) {
            return mix(new Color(205, 210, 215), new Color(250, 250, 252), Math.min(1, (v - 96) / 50));
        }
        if (v > 104) {
            return mix(new Color(128, 112, 92), new Color(165, 160, 155), Math.min(1, (v - 104) / 36));
        }
        if (v < 63.2) {
            return r == Region.LUMBRIDGE_SWAMP || r == Region.MORYTANIA ? new Color(98, 104, 60) : new Color(222, 208, 150);
        }
        // kingdom tint, blended across borders
        double[] rgb = new double[3];
        double[][] offs = {{0, 0}, {14, 0}, {-14, 0}, {0, 14}, {0, -14}, {10, 10}, {-10, -10}, {10, -10}, {-10, 10}};
        for (double[] o : offs) {
            Color t = tint(GielinorMap.regionAt(ox + o[0], oy + o[1]), v);
            rgb[0] += t.getRed();
            rgb[1] += t.getGreen();
            rgb[2] += t.getBlue();
        }
        Color low = new Color((int) (rgb[0] / offs.length), (int) (rgb[1] / offs.length), (int) (rgb[2] / offs.length));
        if (r == Region.KHARIDIAN_DESERT || v < 84) {
            return low;
        }
        return mix(scale(low, 0.8), new Color(128, 112, 92), (v - 84) / 20);
    }

    private static Color tint(Region r, double v) {
        if (r == Region.KHARIDIAN_DESERT) {
            return mix(new Color(232, 206, 140), new Color(196, 160, 100), Math.min(1, (v - 63) / 25));
        }
        Color low = switch (r) {
            case LUMBRIDGE_SWAMP -> new Color(96, 110, 62);
            case MORYTANIA -> new Color(84, 104, 66);
            case WILDERNESS -> new Color(122, 116, 70);
            case KARAMJA, TIRANNWN -> new Color(58, 140, 52);
            case FREMENNIK_PROVINCE, TROLL_COUNTRY -> new Color(98, 140, 108);
            default -> new Color(112, 168, 76);
        };
        if (v < 84) {
            return mix(low, scale(low, 0.8), (v - 63) / 21);
        }
        return mix(scale(low, 0.8), new Color(128, 112, 92), (v - 84) / 20);
    }

    private static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static Color scale(Color c, double s) {
        return new Color(clamp(c.getRed() * s), clamp(c.getGreen() * s), clamp(c.getBlue() * s));
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, v));
    }
}
