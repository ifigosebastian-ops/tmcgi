//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.leon.tmcgiue.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.leon.tmcgiue.core.MeshBuilder;
import dev.leon.tmcgiue.core.NeutralMesh;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class CustomGeometryRecorder {
    private final MeshBuilder builder = new MeshBuilder(2048);
    private final RecordingVertexConsumer consumer;

    public CustomGeometryRecorder() {
        this.consumer = new RecordingVertexConsumer(this.builder);
    }

    public NeutralMesh capture(PoseStack stack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        String topology = renderType.primitiveTopology().name();
        boolean triangles = topology.equals("TRIANGLES");
        if (!triangles && !topology.equals("QUADS")) {
            return null;
        } else {
            this.builder.begin(triangles);
            renderer.render(stack.last(), this.consumer);
            return this.builder.build();
        }
    }
}
