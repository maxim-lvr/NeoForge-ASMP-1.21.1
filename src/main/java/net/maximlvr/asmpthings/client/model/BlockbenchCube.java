package net.maximlvr.asmpthings.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Keeps per-face UVs and fractional cube dimensions from the Blockbench export. */
final class BlockbenchCube extends ModelPart.Cube {
    private final float[][] faces;
    private final Vector3f[] normals;

    BlockbenchCube(float[][] faces) {
        super(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, 1, 1, Set.of());
        this.faces = faces;
        normals = new Vector3f[faces.length];
        for (int i = 0; i < faces.length; i++) {
            float[] f = faces[i];
            Vector3f a = new Vector3f(f[5]-f[0], f[6]-f[1], f[7]-f[2]);
            Vector3f b = new Vector3f(f[10]-f[0], f[11]-f[1], f[12]-f[2]);
            normals[i] = a.cross(b);
            if (normals[i].lengthSquared() > 0) normals[i].normalize();
        }
    }

    @Override
    public void compile(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        Vector3f normal = new Vector3f();
        for (int i = 0; i < faces.length; i++) {
            if (normals[i].lengthSquared() == 0) continue;
            pose.transformNormal(normals[i], normal);
            float[] f = faces[i];
            for (int v = 0; v < 20; v += 5) {
                buffer.addVertex(pose.pose(), f[v]/16, f[v+1]/16, f[v+2]/16)
                        .setColor(color).setUv(f[v+3], f[v+4]).setOverlay(overlay).setLight(light)
                        .setNormal(normal.x, normal.y, normal.z);
            }
        }
    }
}
