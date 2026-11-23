package dev.starblazer.craft.environment;

import java.util.Random;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.starblazer.craft.StarblazerCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Darken only the sky stage, before terrain is drawn. Fixed stars never generate chunks. */
@Mod.EventBusSubscriber(modid = StarblazerCraft.ID, value = Dist.CLIENT)
public final class AtmosphereSky {
    private static final double[][] STARS = new double[512][3];
    static {
        Random random = new Random(5278);
        for (double[] star : STARS) {
            double y = random.nextDouble() * 2 - 1, angle = random.nextDouble() * Math.PI * 2;
            double radius = Math.sqrt(1 - y * y);
            star[0] = Math.cos(angle) * radius; star[1] = y; star[2] = Math.sin(angle) * radius;
        }
    }
    private AtmosphereSky() {}
    private static void quad(BufferBuilder b, float x, float y, float half, float alpha) {
        b.vertex(x-half,y-half,0).color(1F,1F,1F,alpha).endVertex();
        b.vertex(x+half,y-half,0).color(1F,1F,1F,alpha).endVertex();
        b.vertex(x+half,y+half,0).color(1F,1F,1F,alpha).endVertex();
        b.vertex(x-half,y+half,0).color(1F,1F,1F,alpha).endVertex();
    }

    @SubscribeEvent
    public static void sky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.dimension() != Level.OVERWORLD) return;
        var camera = event.getCamera();
        if (camera.getFluidInCameraType() != net.minecraft.world.level.material.FogType.NONE) return;
        var air = AtmosphereProfile.sample(camera.getPosition().y);
        if (air.darkness() <= 0) return;
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var stack = RenderSystem.getModelViewStack();
        stack.pushPose(); stack.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            float alpha = (float) air.darkness();
            b.vertex(-1,-1,0).color(.002F,.004F,.015F,alpha).endVertex();
            b.vertex(1,-1,0).color(.002F,.004F,.015F,alpha).endVertex();
            b.vertex(1,1,0).color(.002F,.004F,.015F,alpha).endVertex();
            b.vertex(-1,1,0).color(.002F,.004F,.015F,alpha).endVertex();
            if (air.stars() > 0) {
                double yaw = Math.toRadians(camera.getYRot()), pitch = Math.toRadians(camera.getXRot());
                float half = 1.5F / Math.max(1, mc.getWindow().getWidth());
                for (double[] star : STARS) {
                    double right = -Math.cos(yaw)*star[0]-Math.sin(yaw)*star[2];
                    double forward = -Math.sin(yaw)*star[0]+Math.cos(yaw)*star[2];
                    double up = Math.cos(pitch)*star[1]+Math.sin(pitch)*forward;
                    double viewForward = -Math.sin(pitch)*star[1]+Math.cos(pitch)*forward;
                    if (viewForward <= .01) continue;
                    Vector4f p = event.getProjectionMatrix().transform(new Vector4f((float)right,(float)up,(float)-viewForward,1));
                    float x=p.x/p.w, y=p.y/p.w;
                    if (Math.abs(x)>1 || Math.abs(y)>1) continue;
                    quad(b,x,y,half,(float)air.stars());
                }
            }
            BufferUploader.drawWithShader(b.end());
        } finally {
            RenderSystem.disableBlend(); RenderSystem.depthMask(true); RenderSystem.enableDepthTest();
            RenderSystem.setProjectionMatrix(oldProjection, VertexSorting.DISTANCE_TO_ORIGIN);
            stack.popPose(); RenderSystem.applyModelViewMatrix();
        }
    }
}
