package com.overyourhead.craftandfind.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.overyourhead.craftandfind.mixin.client.PoseStackAccessor;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Preserves the caller's transform, including unbalanced third-party pushes/pops. */
final class GuiPoseSnapshot {
    private final List<PoseStack.Pose> frames;
    private final List<Matrix4f> matrices;
    private final List<Matrix3f> normals;

    GuiPoseSnapshot(PoseStack stack) {
        frames = new ArrayList<>(poses(stack));
        matrices = new ArrayList<>(frames.size());
        normals = new ArrayList<>(frames.size());
        for (PoseStack.Pose frame : frames) {
            matrices.add(new Matrix4f(frame.pose()));
            normals.add(new Matrix3f(frame.normal()));
        }
    }

    /** Restores exact entry state; returns whether anything was changed. */
    boolean restore(PoseStack stack) {
        Deque<PoseStack.Pose> current = poses(stack);
        boolean changed = current.size() != frames.size();
        int index = 0;
        for (PoseStack.Pose frame : current) {
            if (index >= frames.size() || frame != frames.get(index)) {
                changed = true;
                break;
            }
            index++;
        }
        for (int i = 0; i < frames.size(); i++) {
            PoseStack.Pose frame = frames.get(i);
            if (!frame.pose().equals(matrices.get(i)) || !frame.normal().equals(normals.get(i))) {
                changed = true;
            }
            frame.pose().set(matrices.get(i));
            frame.normal().set(normals.get(i));
        }
        if (changed) {
            current.clear();
            current.addAll(frames);
        }
        return changed;
    }

    private static Deque<PoseStack.Pose> poses(PoseStack stack) {
        return ((PoseStackAccessor) (Object) stack).craftandfind$getPoses();
    }
}
