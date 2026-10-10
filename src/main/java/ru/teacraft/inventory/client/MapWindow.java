package ru.teacraft.inventory.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import ru.teacraft.inventory.mixin.MinecraftTargetAccessor;
import java.lang.reflect.Field;

/** Render Xaero's real map into a private target without replacing the player's menu. */
public final class MapWindow {
    private Screen nativeMap;
    private TextureTarget target;
    private long renderedAt;
    private boolean dragging,failed;
    private Field backup,force,forced,hidden;
    public boolean available(){return nativeMap!=null&&target!=null&&!failed;}
    private boolean prepare(Screen parent){
        if(failed||!CompatBridge.loaded("xaeroworldmap"))return false;
        var mc=Minecraft.getInstance();
        try{
            Object session=Class.forName("xaero.map.WorldMapSession").getMethod("getCurrentSession").invoke(null);
            if(session==null||!(boolean)session.getClass().getMethod("isUsable").invoke(session))return false;
            if(nativeMap==null){
                Object processor=session.getClass().getMethod("getMapProcessor").invoke(session);
                Class<?> mapClass=Class.forName("xaero.map.gui.GuiMap");
                nativeMap=(Screen)mapClass.getConstructor(Screen.class,Screen.class,Class.forName("xaero.map.MapProcessor"),net.minecraft.world.entity.Entity.class).newInstance(null,null,processor,mc.getCameraEntity());
                hidden=mapClass.getField("hiddenUI");
                Class<?> fb=Class.forName("xaero.map.graphics.ImprovedFramebuffer");
                backup=fb.getDeclaredField("mainRenderTargetBackup");force=fb.getDeclaredField("forceMainFBO");forced=fb.getDeclaredField("forcedMainFBO");
                backup.setAccessible(true);force.setAccessible(true);forced.setAccessible(true);
            }
            if(nativeMap.width!=mc.getWindow().getGuiScaledWidth()||nativeMap.height!=mc.getWindow().getGuiScaledHeight())nativeMap.init(mc,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());
            int w=mc.getWindow().getWidth(),h=mc.getWindow().getHeight();
            if(target==null||target.width!=w||target.height!=h){if(target!=null)target.destroyBuffers();target=new TextureTarget(w,h,true,Minecraft.ON_OSX);mc.getMainRenderTarget().bindWrite(true);renderedAt=0;}
            return true;
        }catch(ReflectiveOperationException|LinkageError ex){fail(ex);return false;}
    }
    public void render(GuiGraphics g,Screen parent,InventoryPanel p,int mx,int my,float tick){
        g.flush();
        var state=new State();
        try {
        if(!prepare(parent)){g.drawWordWrap(Minecraft.getInstance().font,CivitatumInventoryScreen.tr("map.waiting"),p.x+9,p.y+30,p.width-18,0xABA799);return;}
        if(System.currentTimeMillis()-renderedAt>=100){capture(mx,my,p,tick);renderedAt=System.currentTimeMillis();}
        if(failed)return;
        int[] bindings=bindings();var shader=RenderSystem.getShader();float[] color=RenderSystem.getShaderColor().clone();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);RenderSystem.setShaderTexture(0,target.getColorTextureId());RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableDepthTest();RenderSystem.disableBlend();
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX);var m=g.pose().last().pose();
        int x=p.x+4,y=p.y+22,w=p.width-8,h=p.height-26;
        b.addVertex(m,x,y+h,0).setUv(0,0);b.addVertex(m,x+w,y+h,0).setUv(1,0);b.addVertex(m,x+w,y,0).setUv(1,1);b.addVertex(m,x,y,0).setUv(0,1);
        BufferUploader.drawWithShader(b.buildOrThrow());
        if(RenderSystem.getShader()!=null)RenderSystem.getShader().clear();BufferUploader.reset();restoreBindings(bindings);RenderSystem.setShader(()->shader);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);RenderSystem.enableDepthTest();
        } finally {state.restore();}
    }
    /** Xaero uses direct OpenGL calls; restore both driver state and Minecraft caches. */
    private static final class State {
        final boolean blend=GL11.glIsEnabled(GL11.GL_BLEND),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),logic=GL11.glIsEnabled(GL11.GL_COLOR_LOGIC_OP);
        final boolean depthWrite=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final int depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC),logicOp=GL11.glGetInteger(GL11.GL_LOGIC_OP_MODE);
        final int src=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dst=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),srcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        final int equation=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),alphaEquation=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        final int drawFbo=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),readFbo=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        final int vao=GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),array=GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        final int[] textureIds=bindings();
        final java.nio.ByteBuffer masks=org.lwjgl.BufferUtils.createByteBuffer(4);
        final net.minecraft.client.renderer.ShaderInstance shader=RenderSystem.getShader();
        State(){GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK,masks);}
        void restore(){
            if(RenderSystem.getShader()!=null)RenderSystem.getShader().clear();BufferUploader.reset();
            GlStateManager._blendFuncSeparate(src,dst,srcAlpha,dstAlpha);GL14.glBlendFuncSeparate(src,dst,srcAlpha,dstAlpha);GlStateManager._blendEquation(equation);GL20.glBlendEquationSeparate(equation,alphaEquation);
            if(blend)RenderSystem.enableBlend();else RenderSystem.disableBlend();force(GL11.GL_BLEND,blend);
            if(depth)RenderSystem.enableDepthTest();else RenderSystem.disableDepthTest();force(GL11.GL_DEPTH_TEST,depth);
            RenderSystem.depthFunc(depthFunc);GL11.glDepthFunc(depthFunc);RenderSystem.depthMask(depthWrite);GL11.glDepthMask(depthWrite);
            if(cull)RenderSystem.enableCull();else RenderSystem.disableCull();force(GL11.GL_CULL_FACE,cull);
            if(logic)GlStateManager._enableColorLogicOp();else GlStateManager._disableColorLogicOp();force(GL11.GL_COLOR_LOGIC_OP,logic);GlStateManager._logicOp(logicOp);GL11.glLogicOp(logicOp);
            RenderSystem.colorMask(masks.get(0)!=0,masks.get(1)!=0,masks.get(2)!=0,masks.get(3)!=0);GL11.glColorMask(masks.get(0)!=0,masks.get(1)!=0,masks.get(2)!=0,masks.get(3)!=0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,drawFbo);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,readFbo);GL30.glBindVertexArray(vao);GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,array);
            restoreBindings(textureIds);RenderSystem.setShader(()->shader);
        }
        private static void force(int capability,boolean enabled){if(enabled)GL11.glEnable(capability);else GL11.glDisable(capability);}
    }
    private void capture(int mx,int my,InventoryPanel panel,float tick){
        var mc=Minecraft.getInstance();RenderTarget main=mc.getMainRenderTarget();
        Matrix4f projection=new Matrix4f(RenderSystem.getProjectionMatrix()),texture=new Matrix4f(RenderSystem.getTextureMatrix());var sorting=RenderSystem.getVertexSorting();
        var model=RenderSystem.getModelViewStack();model.pushMatrix();
        float[] color=RenderSystem.getShaderColor().clone();var shader=RenderSystem.getShader();int[] bindings=bindings();
        int[] viewport=new int[4],scissor=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX,scissor);boolean clipped=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        Object oldBackup=null;boolean oldForce=false,oldHidden=false;int oldForced=0;
        try{
            oldBackup=backup.get(null);oldForce=force.getBoolean(null);oldForced=forced.getInt(null);oldHidden=hidden.getBoolean(null);
            backup.set(null,target);force.setBoolean(null,false);forced.setInt(null,0);hidden.setBoolean(null,true);
            ((MinecraftTargetAccessor)mc).civitatumInventory$setTarget(target);target.setClearColor(0.06F,0.055F,0.04F,1);target.clear(Minecraft.ON_OSX);target.bindWrite(true);RenderSystem.disableScissor();
            var offscreen=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
            nativeMap.render(offscreen,(int)mapX(panel,mx),(int)mapY(panel,my),tick);offscreen.flush();
        }catch(ReflectiveOperationException|RuntimeException|LinkageError ex){fail(ex);}
        finally{
            try{backup.set(null,oldBackup);force.setBoolean(null,oldForce);forced.setInt(null,oldForced);hidden.setBoolean(null,oldHidden);}catch(ReflectiveOperationException ignored){}
            ((MinecraftTargetAccessor)mc).civitatumInventory$setTarget(main);main.bindWrite(true);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);RenderSystem.setProjectionMatrix(projection,sorting);RenderSystem.setTextureMatrix(texture);model.popMatrix();RenderSystem.applyModelViewMatrix();
            if(clipped)RenderSystem.enableScissor(scissor[0],scissor[1],scissor[2],scissor[3]);else RenderSystem.disableScissor();
            if(RenderSystem.getShader()!=null)RenderSystem.getShader().clear();BufferUploader.reset();restoreBindings(bindings);RenderSystem.setShader(()->shader);RenderSystem.setShaderColor(color[0],color[1],color[2],color[3]);RenderSystem.enableDepthTest();
        }
    }
    private static int[] bindings(){int[] b=new int[12];for(int i=0;i<b.length;i++)b[i]=RenderSystem.getShaderTexture(i);return b;}
    private static void restoreBindings(int[] b){for(int i=0;i<b.length;i++){RenderSystem.activeTexture(GL13.GL_TEXTURE0+i);GL13.glActiveTexture(GL13.GL_TEXTURE0+i);GlStateManager._bindTexture(0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,0);RenderSystem.setShaderTexture(i,b[i]);}RenderSystem.activeTexture(GL13.GL_TEXTURE0);}
    private double mapX(InventoryPanel p,double x){return (x-p.x-4)*nativeMap.width/Math.max(1,p.width-8);}
    private double mapY(InventoryPanel p,double y){return (y-p.y-22)*nativeMap.height/Math.max(1,p.height-26);}
    public boolean click(InventoryPanel p,double x,double y,int button){if(!available()||button!=0||y<p.y+22)return false;dragging=nativeMap.mouseClicked(mapX(p,x),mapY(p,y),button);renderedAt=0;return true;}
    public boolean drag(InventoryPanel p,double x,double y,int button,double dx,double dy){if(!available()||!dragging)return false;nativeMap.mouseDragged(mapX(p,x),mapY(p,y),button,dx*nativeMap.width/(p.width-8),dy*nativeMap.height/(p.height-26));renderedAt=0;return true;}
    public boolean release(InventoryPanel p,double x,double y,int button){if(!available()||!dragging)return false;dragging=false;nativeMap.mouseReleased(mapX(p,x),mapY(p,y),button);renderedAt=0;return true;}
    public void scroll(InventoryPanel p,double x,double y,double horizontal,double vertical){if(available()){nativeMap.mouseScrolled(mapX(p,x),mapY(p,y),horizontal,vertical);renderedAt=0;}}
    public void close(){dragging=false;if(nativeMap!=null)nativeMap.removed();nativeMap=null;if(target!=null)target.destroyBuffers();target=null;renderedAt=0;}
    private void fail(Throwable ex){if(!failed)org.slf4j.LoggerFactory.getLogger("Civitatum Inventory").warn("Embedded Xaero map unavailable",ex);failed=true;}
}
