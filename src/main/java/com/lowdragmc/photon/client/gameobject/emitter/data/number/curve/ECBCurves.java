package com.lowdragmc.photon.client.gameobject.emitter.data.number.curve;

import com.lowdragmc.lowdraglib2.math.curve.ExplicitCubicBezierCurve2;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import org.joml.Vector2f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import com.lowdragmc.lowdraglib2.utils.INBTSerializable;

/**
 * @author KilaBash
 * @date 2023/5/29
 * @implNote ECBCurves
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@EqualsAndHashCode
public class ECBCurves implements INBTSerializable<Tag> {
    @Getter
    private final List<ExplicitCubicBezierCurve2> segments = new ArrayList<>();

    public ECBCurves() {
        segments.add(new ExplicitCubicBezierCurve2(new Vector2f(0, 0.5f), new Vector2f(0.1f, 0.5f), new Vector2f(0.9f, 0.5f), new Vector2f(1, 0.5f)));
    }

    public ECBCurves(float... data) {
        for (int i = 0; i < data.length; i+=8) {
            segments.add(new ExplicitCubicBezierCurve2(new Vector2f(data[i], data[i + 1]), new Vector2f(data[i + 2], data[i + 3]), new Vector2f(data[i + 4], data[i + 5]), new Vector2f(data[i + 6], data[i + 7])));
        }
    }

    public float getCurveY(float x) {
        var value = segments.getFirst().p0.y;
        var found = x < segments.getFirst().p0.x;
        if (!found) {
            for (var curve : segments) {
                if (x >= curve.p0.x && x <= curve.p1.x) {
                    value = curve.getPoint((x - curve.p0.x) / (curve.p1.x - curve.p0.x)).y;
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            value = segments.getLast().p1.y;
        }
        return value;
    }

    @Override
    public Tag serializeNBT(@Nonnull HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        var list = new ListTag();
        for (var curve : segments) {
            list.add(curve.serializeNBT(provider));
        }
        tag.put("segments", list);
        return tag;
    }

    @Override
    public void deserializeNBT(@Nonnull HolderLookup.Provider provider, @Nonnull Tag tag) {
        segments.clear();
        if (tag instanceof ListTag listTag) {
            for (Tag t : listTag) {
                if (t instanceof ListTag curve) {
                    segments.add(new ExplicitCubicBezierCurve2(curve));
                }
            }
        } else if (tag instanceof CompoundTag compoundTag) {
            if (compoundTag.contains("segments", net.minecraft.nbt.Tag.TAG_LIST)) {
                var list = compoundTag.getList("segments", net.minecraft.nbt.Tag.TAG_LIST);
                for (Tag t : list) {
                    if (t instanceof ListTag curve) {
                        segments.add(new ExplicitCubicBezierCurve2(curve));
                    }
                }
            }
        }
    }

    public ECBCurves copy() {
        var curves = new ECBCurves();
        curves.segments.clear();
        for (var segment : this.segments) {
            curves.segments.add(segment.copy());
        }
        return curves;
    }
}
