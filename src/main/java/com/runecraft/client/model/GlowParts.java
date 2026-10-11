package com.runecraft.client.model;

import com.runecraft.RuneCraft;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which parts of each model have emissive texels in its "_glow" layer ({@code assets/runecraft/glow_parts.json},
 * written by tools/gen_assets.py from the painted glow textures). The glow pass only needs to draw those
 * parts: everything else in the layer is transparent black, which adds nothing under the additive blend.
 */
final class GlowParts {
    private static final String PATH = "/assets/" + RuneCraft.MOD_ID + "/glow_parts.json";
    private static Map<String, Set<String>> byModel;

    private GlowParts() {
    }

    /** The lit part names of a model, or null if unknown (then the whole model is drawn, as before). */
    static synchronized Set<String> lit(String modelId) {
        if (byModel == null) {
            byModel = load();
        }
        return byModel.get(modelId);
    }

    private static Map<String, Set<String>> load() {
        try (InputStream in = GlowParts.class.getResourceAsStream(PATH)) {
            if (in == null) {
                return new HashMap<>();
            }
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            RuneCraft.LOGGER.warn("Could not read {}; glow layers draw whole models", PATH, e);
            return new HashMap<>();
        }
    }

    /** Parses the file's fixed shape, {@code {"model": ["part", ...], ...}} (nothing else is needed). */
    static Map<String, Set<String>> parse(String json) {
        Map<String, Set<String>> out = new HashMap<>();
        Set<String> current = null;
        int i = 0;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"') {
                StringBuilder sb = new StringBuilder();
                i++;
                while (json.charAt(i) != '"') {
                    char ch = json.charAt(i++);
                    sb.append(ch == '\\' ? json.charAt(i++) : ch);
                }
                if (current == null) {
                    current = new HashSet<>();
                    out.put(sb.toString(), current);
                } else {
                    current.add(sb.toString());
                }
            } else if (c == ']') {
                current = null;
            }
            i++;
        }
        return out;
    }
}
