package com.lowdragmc.photon.client.fx;

import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.photon.Photon;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Exports a Photon FX (or fxproj) with all referenced assets bundled,
 * ready to drop into another mod's {@code resources/assets/} directory.
 *
 * <h3>What gets exported</h3>
 * <ul>
 *   <li>The {@code .fx} / {@code .fxproj} file itself (with material paths rewritten)</li>
 *   <li>Every {@code TextureMaterial} PNG texture</li>
 *   <li>Every {@code CustomShaderMaterial} JSON + VSH/FSH programs + {@code #moj_import} includes</li>
 *   <li>Every {@code UIResourceMaterial} {@code .material.nbt} file, plus its own textures/shaders</li>
 * </ul>
 *
 * <h3>UIResourceMaterial path rewriting</h3>
 * Paths like {@code file(./ldlib2/assets/ldlib2/resources/global/pixel_circle.material.nbt)}
 * are rewritten to {@code resource({targetNamespace}:pixel_circle)} so Tachyon can resolve them.
 * The material file is copied to {@code assets/{targetNamespace}/materials/pixel_circle.nbt}.
 *
 * <h3>Output layout</h3>
 * <pre>
 *   outputDir/
 *   └── assets/
 *       ├── photon/
 *       │   ├── shaders/core/circle.json + circle.fsh + particle.vsh
 *       │   ├── shaders/include/particle.glsl
 *       │   └── textures/particle/circle.png
 *       └── {fxNamespace}/
 *           ├── fx/myeffect.fx
 *           └── materials/pixel_circle.nbt
 * </pre>
 */
public final class TachyonExporter {

    private static final Pattern MOJ_IMPORT =
            Pattern.compile("#moj_import\\s*<([^>]+)>");

    private TachyonExporter() {}

    // =========================================================================
    //  Public API
    // =========================================================================

    /**
     * Full bundle export for a {@code .fx} file:
     * <ol>
     *   <li>Serialise FX to NBT.</li>
     *   <li>Collect {@code UIResourceMaterial} paths and rewrite them to
     *       {@code resource(targetNs:stem)}.</li>
     *   <li>Copy all {@code .material.nbt} files to the bundle.</li>
     *   <li>Collect and copy every texture / shader asset (incl. those inside
     *       copied material files).</li>
     *   <li>Write the (rewritten) {@code .fx} file.</li>
     * </ol>
     */
    public static List<String> exportFXBundle(FX fx, File outputDir,
                                               String fxNamespace, String fxName) {
        var errors = new ArrayList<String>();
        var fxTag  = fx.serializeNBT(Platform.getFrozenRegistry());
        processAndWrite(fxTag, fxNamespace, fxName + FX.SUFFIX, outputDir, errors);
        return errors;
    }

    /**
     * Full bundle export for a {@code .fxproj} file.
     * Rewrites material paths inside the project's embedded FX data.
     *
     * @param projectTag serialised fxproj NBT from {@code IProject.serializeNBT()}
     */
    public static List<String> exportFXProjBundle(FX fx, CompoundTag projectTag,
                                                   File outputDir,
                                                   String fxNamespace, String fxName) {
        var errors = new ArrayList<String>();

        // The FX data is at projectTag → "data" → "fx"
        var fxTag = projectTag.getCompound("data").getCompound("fx");

        // Rewrite material paths inside the embedded FX (mutates fxTag in-place,
        // which is a direct reference inside projectTag)
        var remapping = new LinkedHashMap<String, String>();
        var matFiles  = new LinkedHashSet<File>();
        collectMaterialEntries(fxTag, fxNamespace, remapping, matFiles);
        rewriteMaterialPaths(fxTag, remapping);

        // Copy material files + scan their own assets
        for (var mf : matFiles) copyMaterialFile(mf, null, fxNamespace, extractStem(mf.getName()), outputDir, errors);

        // Copy textures + shaders referenced in the (rewritten) FX
        var manifest = new ExportManifest();
        scanTag(fxTag, manifest);
        resolveShaderDependencies(manifest);
        errors.addAll(copyAssets(manifest, outputDir));

        // Write .fxproj and .fx
        var fxDir = new File(outputDir, "assets/" + fxNamespace + "/fx");
        fxDir.mkdirs();
        writeCompressed(projectTag, new File(fxDir, fxName + ".fxproj"), errors);
        // Also write standalone .fx for runtime use
        writeCompressed(fxTag, new File(fxDir, fxName + FX.SUFFIX), errors);
        return errors;
    }

    // =========================================================================
    //  Core pipeline (shared by both export methods)
    // =========================================================================

    private static void processAndWrite(CompoundTag fxTag, String targetNs,
                                         String fileName, File outputDir,
                                         List<String> errors) {
        // 1. Collect UIResourceMaterial paths and rewrite
        var remapping = new LinkedHashMap<String, String>();
        var matFiles  = new LinkedHashSet<File>();
        collectMaterialEntries(fxTag, targetNs, remapping, matFiles);
        rewriteMaterialPaths(fxTag, remapping);

        // 2. Copy each material file (and scan its assets)
        for (var mf : matFiles) {
            copyMaterialFile(mf, null, targetNs, extractStem(mf.getName()), outputDir, errors);
        }

        // For unresolved file-paths (file not found on disk) try ResourceManager
        for (var entry : remapping.entrySet()) {
            var origPath = entry.getKey();
            if (origPath.startsWith("file(") && origPath.endsWith(")")) {
                var path = origPath.substring(5, origPath.length() - 1);
                var rl   = pathToResourceLocation(path);
                if (rl != null) {
                    var stem = extractStem(path);
                    copyMaterialViaRM(rl, targetNs, stem, outputDir, errors);
                }
            }
        }

        // 3. Collect and copy textures / shaders from the rewritten FX
        var manifest = new ExportManifest();
        scanTag(fxTag, manifest);
        resolveShaderDependencies(manifest);
        errors.addAll(copyAssets(manifest, outputDir));

        // 4. Write the output file
        var fxDir = new File(outputDir, "assets/" + targetNs + "/fx");
        fxDir.mkdirs();
        writeCompressed(fxTag, new File(fxDir, fileName), errors);
    }

    // =========================================================================
    //  UIResourceMaterial: collect + rewrite
    // =========================================================================

    /**
     * Walks {@code tag} looking for {@code type="ui_resource_material"} nodes.
     * For each one whose {@code data.resourcePath} uses the {@code file(...)} format:
     * <ul>
     *   <li>Resolves the actual file on disk and adds it to {@code matFiles}.</li>
     *   <li>Records {@code oldPath → "resource(targetNs:stem)"} in {@code remapping}.</li>
     * </ul>
     */
    private static void collectMaterialEntries(Tag tag, String targetNs,
                                                Map<String, String> remapping,
                                                Set<File> matFiles) {
        if (tag instanceof CompoundTag compound) {
            if ("ui_resource_material".equals(compound.getString("type"))
                    && compound.contains("data")) {
                var data         = compound.getCompound("data");
                var resourcePath = data.getString("resourcePath");
                if (resourcePath.startsWith("file(") && resourcePath.endsWith(")")
                        && !remapping.containsKey(resourcePath)) {
                    var path = resourcePath.substring(5, resourcePath.length() - 1);
                    var stem = extractStem(path);
                    remapping.put(resourcePath, "resource(" + targetNs + ":" + stem + ")");
                    var file = resolveFilePath(path);
                    if (file != null) matFiles.add(file);
                }
                // Still recurse into data for nested materials
                collectMaterialEntries(data, targetNs, remapping, matFiles);
            } else {
                for (var key : compound.getAllKeys()) {
                    var child = compound.get(key);
                    if (child != null) collectMaterialEntries(child, targetNs, remapping, matFiles);
                }
            }
        } else if (tag instanceof ListTag list) {
            for (var item : list) collectMaterialEntries(item, targetNs, remapping, matFiles);
        }
    }

    /** Rewrites {@code data.resourcePath} strings in-place using the remapping table. */
    private static void rewriteMaterialPaths(Tag tag, Map<String, String> remapping) {
        if (remapping.isEmpty()) return;
        if (tag instanceof CompoundTag compound) {
            if ("ui_resource_material".equals(compound.getString("type"))
                    && compound.contains("data")) {
                var data         = compound.getCompound("data");
                var resourcePath = data.getString("resourcePath");
                var newPath      = remapping.get(resourcePath);
                if (newPath != null) data.putString("resourcePath", newPath);
            }
            for (var key : compound.getAllKeys()) {
                var child = compound.get(key);
                if (child != null) rewriteMaterialPaths(child, remapping);
            }
        } else if (tag instanceof ListTag list) {
            for (var item : list) rewriteMaterialPaths(item, remapping);
        }
    }

    // =========================================================================
    //  Material file copying
    // =========================================================================

    /**
     * Copies a {@code .material.nbt} file to
     * {@code outputDir/assets/{targetNs}/materials/{stem}.nbt}
     * and scans it for embedded texture / shader references.
     *
     * @param file     source file (may be null if it couldn't be found on disk)
     * @param content  raw bytes (alternative to file; used when loaded via RM)
     */
    private static void copyMaterialFile(@Nullable File file, @Nullable byte[] content,
                                          String targetNs, String stem,
                                          File outputDir, List<String> errors) {
        var target = new File(outputDir, "assets/" + targetNs + "/materials/" + stem + ".nbt");
        if (target.exists()) return;
        target.getParentFile().mkdirs();

        byte[] bytes = content;
        if (bytes == null && file != null && file.exists()) {
            try { bytes = java.nio.file.Files.readAllBytes(file.toPath()); }
            catch (Exception e) { errors.add("material " + stem + " read: " + e.getMessage()); return; }
        }
        if (bytes == null) { errors.add("material " + stem + ": source not found"); return; }

        // Write the material file
        try (var os = new FileOutputStream(target)) {
            os.write(bytes);
        } catch (Exception e) {
            errors.add("material " + stem + " write: " + e.getMessage());
            return;
        }

        // Scan the material file for its own texture/shader assets
        var materialTag = readNbt(bytes);
        if (materialTag != null) {
            var innerManifest = new ExportManifest();
            scanTag(materialTag, innerManifest);
            resolveShaderDependencies(innerManifest);
            errors.addAll(copyAssets(innerManifest, outputDir));
        }
    }

    /** Copies a material file from the ResourceManager (fallback when not on disk). */
    private static void copyMaterialViaRM(ResourceLocation rl, String targetNs, String stem,
                                           File outputDir, List<String> errors) {
        var target = new File(outputDir, "assets/" + targetNs + "/materials/" + stem + ".nbt");
        if (target.exists()) return;
        try {
            var rm = Minecraft.getInstance().getResourceManager();
            byte[] bytes;
            try (var is = rm.open(rl)) { bytes = is.readAllBytes(); }
            copyMaterialFile(null, bytes, targetNs, stem, outputDir, errors);
        } catch (Exception e) {
            Photon.LOGGER.debug("[TachyonExporter] Could not copy material {} via RM: {}", rl, e.getMessage());
        }
    }

    // =========================================================================
    //  NBT scanning (textures + shaders)
    // =========================================================================

    /**
     * Recursively scans {@code tag} and adds every {@code "texture"} and
     * {@code "shaderLocation"} string value to the manifest.
     */
    public static ExportManifest collectAssets(FX fx) {
        var manifest = new ExportManifest();
        scanTag(fx.serializeNBT(Platform.getFrozenRegistry()), manifest);
        return manifest;
    }

    private static void scanTag(Tag tag, ExportManifest manifest) {
        switch (tag) {
            case CompoundTag compound -> {
                for (var key : compound.getAllKeys()) {
                    var child = compound.get(key);
                    if (child instanceof StringTag str) {
                        var val = str.getAsString();
                        if (val.isEmpty() || !val.contains(":")) continue;
                        switch (key) {
                            case "texture"        -> addRL(val, manifest.textures);
                            case "shaderLocation" -> addRL(val, manifest.shaders);
                        }
                    } else if (child != null) {
                        scanTag(child, manifest);
                    }
                }
            }
            case ListTag list -> { for (var item : list) scanTag(item, manifest); }
            default -> {}
        }
    }

    private static void addRL(String value, Set<ResourceLocation> target) {
        try { target.add(ResourceLocation.parse(value)); } catch (Exception ignored) {}
    }

    // =========================================================================
    //  Shader dependency resolution
    // =========================================================================

    /** Resolves every shader name in the manifest into its JSON + program files + includes. */
    public static void resolveShaderDependencies(ExportManifest manifest) {
        var rm = Minecraft.getInstance().getResourceManager();
        for (var shader : new ArrayList<>(manifest.shaders)) {
            resolveShaderJson(shader, manifest, rm, new HashSet<>());
        }
    }

    private static void resolveShaderJson(ResourceLocation shader, ExportManifest manifest,
                                           ResourceManager rm, Set<ResourceLocation> visited) {
        var jsonRl = ResourceLocation.fromNamespaceAndPath(
                shader.getNamespace(), "shaders/core/" + shader.getPath() + ".json");
        if (!visited.add(jsonRl)) return;
        manifest.shaderFiles.add(jsonRl);
        try (var is = rm.open(jsonRl)) {
            var json = JsonParser.parseReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : Map.of("vertex", ".vsh", "fragment", ".fsh", "geometry", ".gsh").entrySet()) {
                if (!json.has(entry.getKey())) continue;
                var progRl = parseRL(json.get(entry.getKey()).getAsString());
                if (progRl == null) continue;
                var fileRl = ResourceLocation.fromNamespaceAndPath(
                        progRl.getNamespace(), "shaders/core/" + progRl.getPath() + entry.getValue());
                if (manifest.shaderFiles.add(fileRl)) {
                    resolveGlslIncludes(fileRl, manifest, rm, new HashSet<>());
                }
            }
        } catch (Exception e) {
            Photon.LOGGER.warn("[TachyonExporter] Cannot read shader JSON {}: {}", jsonRl, e.getMessage());
        }
    }

    private static void resolveGlslIncludes(ResourceLocation shaderFile, ExportManifest manifest,
                                             ResourceManager rm, Set<ResourceLocation> visited) {
        if (!visited.add(shaderFile)) return;
        try (var is = rm.open(shaderFile)) {
            var source  = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            var matcher = MOJ_IMPORT.matcher(source);
            while (matcher.find()) {
                var importPath = matcher.group(1).trim();
                if (!importPath.contains(":")) continue;
                var rl = parseRL(importPath);
                if (rl == null) continue;
                var includeRl = ResourceLocation.fromNamespaceAndPath(
                        rl.getNamespace(), "shaders/include/" + rl.getPath());
                if (manifest.shaderFiles.add(includeRl)) {
                    resolveGlslIncludes(includeRl, manifest, rm, visited);
                }
            }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    //  Asset copying
    // =========================================================================

    /** Copies every texture and shader file in the manifest to {@code outputDir/assets/…}. */
    public static List<String> copyAssets(ExportManifest manifest, File outputDir) {
        var rm     = Minecraft.getInstance().getResourceManager();
        var errors = new ArrayList<String>();
        for (var rl : manifest.textures)    { if (rl != null) copyResource(rm, rl, outputDir, errors); }
        for (var rl : manifest.shaderFiles) { copyResource(rm, rl, outputDir, errors); }
        return errors;
    }

    private static void copyResource(ResourceManager rm, ResourceLocation rl,
                                      File outputDir, List<String> errors) {
        var target = new File(outputDir,
                "assets" + File.separator + rl.getNamespace() + File.separator
                        + rl.getPath().replace('/', File.separatorChar));
        if (target.exists()) return;
        target.getParentFile().mkdirs();
        try (var in = rm.open(rl); var out = new FileOutputStream(target)) {
            in.transferTo(out);
        } catch (Exception e) {
            errors.add(rl + " → " + e.getMessage());
        }
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    /** Extracts the stem from a file name: removes {@code .material.nbt} or {@code .nbt} suffix. */
    private static String extractStem(String path) {
        var name = path.contains("/")  ? path.substring(path.lastIndexOf('/') + 1)
                 : path.contains("\\") ? path.substring(path.lastIndexOf('\\') + 1)
                 : path;
        if (name.endsWith(".material.nbt")) return name.substring(0, name.length() - ".material.nbt".length());
        if (name.endsWith(".nbt"))          return name.substring(0, name.length() - ".nbt".length());
        return name;
    }

    /**
     * Tries to locate a file referenced in a {@code file(...)} path.
     * Checks:
     * <ol>
     *   <li>As-is (absolute path).</li>
     *   <li>Relative to the Minecraft game directory.</li>
     *   <li>Relative to the game directory after stripping a leading {@code ./}.</li>
     * </ol>
     */
    @Nullable
    private static File resolveFilePath(String path) {
        var f = new File(path);
        if (f.isAbsolute() && f.exists()) return f;
        var gameDir = Minecraft.getInstance().gameDirectory;
        f = new File(gameDir, path);
        if (f.exists()) return f;
        if (path.startsWith("./")) {
            f = new File(gameDir, path.substring(2));
            if (f.exists()) return f;
        }
        return null;
    }

    /**
     * Converts a {@code file(...)} raw path to a {@link ResourceLocation} by locating
     * the {@code assets/} segment: {@code .../assets/{ns}/{subpath}} → {@code ns:subpath}.
     */
    @Nullable
    private static ResourceLocation pathToResourceLocation(String path) {
        var normalized = path.replace('\\', '/');
        var idx        = normalized.indexOf("assets/");
        if (idx < 0) return null;
        var rel   = normalized.substring(idx + "assets/".length());
        var slash = rel.indexOf('/');
        if (slash < 0) return null;
        try {
            return ResourceLocation.fromNamespaceAndPath(
                    rel.substring(0, slash), rel.substring(slash + 1));
        } catch (Exception e) { return null; }
    }

    /** Reads an NBT CompoundTag from raw bytes, trying gzip first then uncompressed. */
    @Nullable
    private static CompoundTag readNbt(byte[] data) {
        try {
            return NbtIo.readCompressed(
                    new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
        } catch (Exception ignored) {}
        try {
            return NbtIo.read(
                    new DataInputStream(new ByteArrayInputStream(data)));
        } catch (Exception ignored) {}
        return null;
    }

    private static void writeCompressed(CompoundTag tag, File file, List<String> errors) {
        try { NbtIo.writeCompressed(tag, file.toPath()); }
        catch (Exception e) { errors.add("write " + file.getName() + ": " + e.getMessage()); }
    }

    @Nullable
    private static ResourceLocation parseRL(String s) {
        try { return ResourceLocation.parse(s); } catch (Exception e) { return null; }
    }

    // =========================================================================
    //  Manifest
    // =========================================================================

    /** Collected asset references for one export operation. */
    public static final class ExportManifest {
        public final Set<ResourceLocation> textures    = new LinkedHashSet<>();
        public final Set<ResourceLocation> shaders     = new LinkedHashSet<>();
        public final Set<ResourceLocation> shaderFiles = new LinkedHashSet<>();

        public int totalAssets() { return textures.size() + shaderFiles.size(); }
    }
}
