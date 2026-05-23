package com.lowdragmc.photon.gui.editor;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource;
import com.lowdragmc.lowdraglib2.editor.resource.Resources;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.photon.Photon;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.client.fx.TachyonExporter;
import com.lowdragmc.photon.client.gameobject.emitter.data.fixer.PhotonFXProjectDataFixer;
import com.lowdragmc.photon.gui.editor.resource.CurveResource;
import com.lowdragmc.photon.gui.editor.resource.GradientResource;
import com.lowdragmc.photon.gui.editor.resource.MaterialResource;
import com.lowdragmc.photon.gui.editor.resource.MeshResource;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public class FXProject implements IProject {
    public static int VERSION = 3;
    public static final ProjectType TYPE = ProjectType.of(IGuiTexture.EMPTY, "fx_project", ".fxproj", FXProject::new);

    @Getter
    private final Resources resources;
    @Getter
    private final FX fx = new FX();
    // runtime
    @Nullable
    private ISubscription exportMenuSubscription;

    public FXProject() {
        this.resources = Resources.of(
                MaterialResource.INSTANCE,
                ColorsResource.INSTANCE,
                CurveResource.INSTANCE,
                GradientResource.INSTANCE,
                MeshResource.INSTANCE
        );
    }

    @Override
    public String getVersion() {
        return "%d.0".formatted(VERSION) ;
    }

    @Override
    public ProjectType getProjectType() {
        return TYPE;
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        var data = new CompoundTag();
        data.put("fx", fx.serializeNBT(provider));
        return data;
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {
        fx.deserializeNBT(provider, nbt.getCompound("fx"));
    }

    @Override
    public CompoundTag getMetadata() {
        var meta = IProject.super.getMetadata();
        meta.putInt("version_num", VERSION);
        return meta;
    }

    @Override
    public void deserializeNBT(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {
        // apply data fix for cross-version
        var version = Math.max(1, nbt.getCompound("meta").getInt("version_num"));
        var fixedData = PhotonFXProjectDataFixer.INSTANCE.applyFixes(version, VERSION, nbt.getCompound("data"));
        deserializeProject(provider, fixedData);
    }

    @Override
    public void onLoad(Editor editor) {
        IProject.super.onLoad(editor);
        if (exportMenuSubscription != null) {
            exportMenuSubscription.unsubscribe();
        }
        exportMenuSubscription = editor.fileMenu.registerMenuCreator((tab, menu) ->
                menu.branch("ldlib.gui.editor.menu.export", m -> {

                    // ── Original: Export FX ──────────────────────────────────────
                    m.leaf("photon.export_fx", () -> {
                        Dialog.showFileDialog("ldlib.gui.editor.tips.save_as",
                                new File(LDLib2.getAssetsDir(), "%s/fx/".formatted(Photon.MOD_ID)),
                                false,
                                Dialog.suffixFilter(FX.SUFFIX), file -> {
                                    if (file != null && !file.isDirectory()) {
                                        if (!file.getName().endsWith(FX.SUFFIX)) {
                                            file = new File(file.getParentFile(), file.getName() + FX.SUFFIX);
                                        }
                                        try {
                                            NbtIo.writeCompressed(
                                                    fx.serializeNBT(Platform.getFrozenRegistry()),
                                                    file.toPath());
                                            FXHelper.clearCache();
                                        } catch (Exception ignored) {}
                                    }
                                }).show(editor);
                    });

                    // ── Tachyon: Export .fx + all assets ─────────────────────────
                    m.leaf("photon.export_tachyon_fx", () ->
                            Dialog.showFileDialog(
                                    "photon.export_tachyon_fx.title",
                                    new File(LDLib2.getAssetsDir(), "%s/fx/".formatted(Photon.MOD_ID)),
                                    false,
                                    Dialog.suffixFilter(FX.SUFFIX), file -> {
                                        if (file == null || file.isDirectory()) return;
                                        if (!file.getName().endsWith(FX.SUFFIX)) {
                                            file = new File(file.getParentFile(), file.getName() + FX.SUFFIX);
                                        }
                                        var stem      = stripSuffix(file.getName(), FX.SUFFIX);
                                        var ns        = deriveNamespace(file);
                                        var bundleDir = bundleDir(file, stem);
                                        var errors    = TachyonExporter.exportFXBundle(fx, bundleDir, ns, stem);
                                        FXHelper.clearCache();
                                        showExportResult(editor, bundleDir, errors);
                                    }).show(editor));

                    // ── Tachyon: Export .fxproj + .fx + all assets ───────────────
                    m.leaf("photon.export_tachyon_fxproj", () ->
                            Dialog.showFileDialog(
                                    "photon.export_tachyon_fxproj.title",
                                    new File(LDLib2.getAssetsDir(), "%s/fx/".formatted(Photon.MOD_ID)),
                                    false,
                                    Dialog.suffixFilter(".fxproj"), file -> {
                                        if (file == null || file.isDirectory()) return;
                                        var stem      = stripSuffix(stripSuffix(file.getName(), ".fxproj"), FX.SUFFIX);
                                        var ns        = deriveNamespace(file);
                                        var bundleDir = bundleDir(file, stem);
                                        var projTag   = serializeNBT(Platform.getFrozenRegistry());
                                        var errors    = TachyonExporter.exportFXProjBundle(fx, projTag, bundleDir, ns, stem);
                                        FXHelper.clearCache();
                                        showExportResult(editor, bundleDir, errors);
                                    }).show(editor));
                }));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Tries to determine the mod namespace from the file's path:
     * looks for an {@code assets/{ns}/fx/} segment; falls back to {@link Photon#MOD_ID}.
     */
    private static String deriveNamespace(File file) {
        var parts = file.getAbsolutePath().replace('\\', '/').split("/");
        for (int i = 0; i < parts.length - 2; i++) {
            if ("assets".equals(parts[i]) && i + 2 < parts.length) {
                return parts[i + 1]; // assets/{ns}/fx/...
            }
        }
        return Photon.MOD_ID;
    }

    private static String stripSuffix(String name, String suffix) {
        return name.endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
    }

    /** Walks up the path to find the parent of the {@code assets/} folder; fallback = file's parent. */
    private static File assetsParent(File file) {
        for (var f = file.getParentFile(); f != null; f = f.getParentFile()) {
            if ("assets".equals(f.getName())) return f.getParentFile();
        }
        return file.getParentFile();
    }

    /** Bundle output root: sibling of the {@code assets/} folder, named {@code tachyon_export_{stem}}. */
    private static File bundleDir(File file, String stem) {
        return new File(assetsParent(file), "tachyon_export_" + stem);
    }

    /** Shows a brief result notification: output folder path + any warnings. */
    private static void showExportResult(Editor editor, File bundleDir, List<String> errors) {
        var sb = new StringBuilder();
        sb.append(bundleDir.getAbsolutePath());
        if (errors.isEmpty()) {
            sb.append("\n\nAll assets copied successfully.");
        } else {
            sb.append("\n\nWarnings (").append(errors.size()).append("):");
            errors.stream().limit(5).forEach(e -> sb.append("\n  • ").append(e));
            if (errors.size() > 5) sb.append("\n  … and ").append(errors.size() - 5).append(" more");
        }
        Dialog.showNotification("Tachyon Export", sb.toString(), null).show(editor);
    }

    @Override
    public void onClosed(Editor editor) {
        IProject.super.onClosed(editor);
        if (exportMenuSubscription != null) {
            exportMenuSubscription.unsubscribe();
            exportMenuSubscription = null;
        }
    }
}
