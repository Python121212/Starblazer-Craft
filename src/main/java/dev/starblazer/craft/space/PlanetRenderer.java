package dev.starblazer.craft.space;

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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Cached colored meshes: no texture downloads, block scans or planet-sized chunks. */
@Mod.EventBusSubscriber(modid=StarblazerCraft.ID,value=Dist.CLIENT)
public final class PlanetRenderer {
    private static final float[] EARTH=mesh(false,32,64);
    private static final float[] MOON_FAR=mesh(true,16,32);
    private static final float[] MOON_NEAR=mesh(true,64,96);
    private PlanetRenderer() {}

    private static float[] mesh(boolean moon,int rows,int columns) {
        float[] data=new float[rows*columns*4*6]; int index=0;
        int[][] corners={{0,0},{1,0},{1,1},{0,1}};
        for(int row=0;row<rows;row++) for(int col=0;col<columns;col++) for(int[] corner:corners) {
            double lat=-Math.PI/2+Math.PI*(row+corner[0])/rows;
            double lon=2*Math.PI*(col+corner[1])/columns;
            double nx=Math.cos(lat)*Math.cos(lon), ny=Math.sin(lat), nz=Math.cos(lat)*Math.sin(lon);
            double radius=moon?SpaceBodies.MOON_RADIUS:SpaceBodies.EARTH_RADIUS;
            double x=nx*radius,y=ny*radius,z=nz*radius;
            if(moon && ny>0) y-=ny*SpaceBodies.craterDepth(x,z);
            double shade=.38+.62*Math.max(0,nx*.3+ny*.8+nz*.4);
            double r,g,b;
            if(moon) {
                double grain=.83+.06*Math.sin(nx*113+nz*79)*Math.sin(ny*97);
                r=grain*.82;g=grain*.81;b=grain*.78;
            } else {
                double land=Math.sin(nx*7+nz*4)+.65*Math.sin(ny*9-nz*6)+.4*Math.cos(nx*17+ny*11);
                boolean ice=Math.abs(ny)>.86;
                boolean cloud=Math.sin(nx*21+ny*13+nz*8)>.88;
                r=ice||cloud?.88:land>.5?.16:.035;
                g=ice||cloud?.91:land>.5?.45:.23;
                b=ice||cloud?.95:land>.5?.2:.64;
            }
            data[index++]=(float)x;data[index++]=(float)y;data[index++]=(float)z;
            data[index++]=(float)(r*shade);data[index++]=(float)(g*shade);data[index++]=(float)(b*shade);
        }
        return data;
    }
    private static void body(BufferBuilder b,Matrix4f pose,float[] mesh,double x,double y,double z) {
        for(int i=0;i<mesh.length;i+=6) b.vertex(pose,(float)(x+mesh[i]),(float)(y+mesh[i+1]),(float)(z+mesh[i+2]))
            .color(mesh[i+3],mesh[i+4],mesh[i+5],1).endVertex();
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.level.dimension()!=Level.OVERWORLD) return;
        var camera=event.getCamera();var position=camera.getPosition();
        if(position.y<3500 || camera.getFluidInCamera()!=net.minecraft.world.level.material.FogType.NONE) return;
        if(camera.getEntity() instanceof net.minecraft.world.entity.LivingEntity living
            && (living.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
            || living.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS))) return;
        Matrix4f previous=new Matrix4f(RenderSystem.getProjectionMatrix());
        float[] color=RenderSystem.getShaderColor().clone();
        // A background-stage far plane makes the bodies visible beyond chunk view distance.
        Matrix4f projection=new Matrix4f(event.getProjectionMatrix());
        float near=.05F, far=50000F;
        projection.m22((far+near)/(near-far)).m32(2*far*near/(near-far));
        RenderSystem.setProjectionMatrix(projection,VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.depthMask(false);RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();RenderSystem.setShaderColor(1,1,1,1);
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder b=Tesselator.getInstance().getBuilder();
            b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            Matrix4f pose=event.getPoseStack().last().pose();
            body(b,pose,EARTH,-position.x,SpaceBodies.EARTH_Y-position.y,-position.z);
            double distance=position.distanceTo(new net.minecraft.world.phys.Vec3(SpaceBodies.MOON_X,SpaceBodies.MOON_Y,SpaceBodies.MOON_Z));
            // Earth before moon; both are background meshes before ordinary world terrain.
            body(b,pose,distance<400?MOON_NEAR:MOON_FAR,
                SpaceBodies.MOON_X-position.x,SpaceBodies.MOON_Y-position.y,SpaceBodies.MOON_Z-position.z);
            BufferUploader.drawWithShader(b.end());
        } finally {
            RenderSystem.depthMask(true);RenderSystem.enableDepthTest();
            RenderSystem.setProjectionMatrix(previous,VertexSorting.DISTANCE_TO_ORIGIN);
            RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);
        }
    }
}
