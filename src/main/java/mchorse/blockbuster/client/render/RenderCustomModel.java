package mchorse.blockbuster.client.render;

import java.util.Map;

import mchorse.blockbuster.api.Model;
import mchorse.blockbuster.api.ModelPose;
import mchorse.blockbuster.client.model.ModelCustom;
import mchorse.blockbuster.client.model.ModelCustomRenderer;
import mchorse.blockbuster.client.render.layer.LayerHeldItem;
import mchorse.blockbuster_pack.morphs.CustomMorph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RenderCustomModel extends RenderLivingBase<EntityLivingBase>
{
    public static ResourceLocation lastTexture;

    /**
     * Currently used morph 
     */
    public CustomMorph current;

    public static void bindLastTexture(ResourceLocation location)
    {
        lastTexture = location;
        bindLastTexture();
    }

    public static void bindLastTexture()
    {
        if (lastTexture != null)
        {
            Minecraft.getMinecraft().renderEngine.bindTexture(lastTexture);
        }
    }

    public RenderCustomModel(RenderManager renderManagerIn, ModelBase modelBaseIn, float shadowSizeIn)
    {
        super(renderManagerIn, null, shadowSizeIn);

        this.addLayer(new LayerHeldItem(this));
    }

    /**
     * Get default texture for entity 
     */
    @Override
    protected ResourceLocation getEntityTexture(EntityLivingBase entity)
    {
        return this.mainModel == null ? null : ((ModelCustom) this.mainModel).model.defaultTexture;
    }

    @Override
    protected boolean bindEntityTexture(EntityLivingBase entity)
    {
        if (this.mainModel != null && ((ModelCustom) this.mainModel).model.providesMtl)
        {
            return true;
        }

        return super.bindEntityTexture(entity);
    }

    /**
     * Override method in order to save the last texture. Used by OBJ 
     * renderer with materials to bind texture back 
     */
    @Override
    public void bindTexture(ResourceLocation location)
    {
        lastTexture = location;

        super.bindTexture(location);
    }

    /**
     * Render morph's name only if the player is pointed at the entity
     */
    @Override
    protected boolean canRenderName(EntityLivingBase entity)
    {
        return super.canRenderName(entity) && entity.hasCustomName() && entity == this.renderManager.pointedEntity;
    }

    @Override
    public void doRender(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        this.setupModel(entity);

        if (this.mainModel != null)
        {
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        }
    }

    /**
     * Setup the model for player instance.
     *
     * This method is responsible for picking the right model and pose based
     * on player properties.
     */
    public void setupModel(EntityLivingBase entity)
    {
        Map<String, ModelCustom> models = ModelCustom.MODELS;
        ModelCustom model = null;
        ModelPose pose = null;

        if (this.current != null)
        {
            model = models.get(this.current.getKey());
            pose = this.current.getPose(entity);
        }

        if (model != null)
        {
            if (pose == null)
            {
                pose = model.model.getPose("standing");
            }

            model.materials = this.current.materials;
            model.pose = pose;
            this.mainModel = model;
        }
    }

    /**
     * Make player a little bit smaller (so he looked like steve, and not like an 
     * overgrown rodent).
     */
    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTickTime)
    {
        Model model = ((ModelCustom) this.mainModel).model;

        GlStateManager.scale(model.scale[0], model.scale[1], model.scale[2]);
    }

    /**
     * Taken from RenderPlayer
     *
     * This code is primarily changes the angle of the player while it's flying
     * an elytra. You know?
     */
    @Override
    protected void rotateCorpse(EntityLivingBase entity, float pitch, float yaw, float partialTicks)
    {
        if (entity.isEntityAlive() && entity.isPlayerSleeping())
        {
            /* Nap time! */
            GlStateManager.rotate(((EntityPlayer) entity).getBedOrientationInDegrees(), 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(this.getDeathMaxRotation(entity), 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(270.0F, 0.0F, 1.0F, 0.0F);
        }
        else if (entity.isElytraFlying())
        {
            /* Elytra rotation */
            super.rotateCorpse(entity, pitch, yaw, partialTicks);

            float f = entity.getTicksElytraFlying() + partialTicks;
            float f1 = MathHelper.clamp_float(f * f / 100.0F, 0.0F, 1.0F);

            Vec3d vec3d = entity.getLook(partialTicks);

            double d0 = entity.motionX * entity.motionX + entity.motionZ * entity.motionZ;
            double d1 = vec3d.xCoord * vec3d.xCoord + vec3d.zCoord * vec3d.zCoord;

            GlStateManager.rotate(f1 * (-90.0F - entity.rotationPitch), 1.0F, 0.0F, 0.0F);

            if (d0 > 0.0D && d1 > 0.0D)
            {
                double d2 = (entity.motionX * vec3d.xCoord + entity.motionZ * vec3d.zCoord) / (Math.sqrt(d0) * Math.sqrt(d1));
                double d3 = entity.motionX * vec3d.zCoord - entity.motionZ * vec3d.xCoord;

                GlStateManager.rotate((float) (Math.signum(d3) * Math.acos(d2)) * 180.0F / (float) Math.PI, 0.0F, 1.0F, 0.0F);
            }
        }
        else
        {
            super.rotateCorpse(entity, pitch, yaw, partialTicks);
        }
    }

    /**
     * Render right hand 
     */
    public void renderRightArm(EntityPlayer player)
    {
        ResourceLocation texture = this.getEntityTexture(player);

        if (texture != null)
        {
            Minecraft.getMinecraft().renderEngine.bindTexture(texture);
        }

        this.mainModel.swingProgress = 0.0F;
        this.mainModel.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);

        GlStateManager.color(1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();

        for (ModelCustomRenderer arm : ((ModelCustom) this.mainModel).right)
        {
            arm.rotateAngleX = 0;
            arm.rotationPointX = -6;
            arm.rotationPointY = 13.8F - (arm.limb.size[1] > 8 ? arm.limb.size[1] : arm.limb.size[1] + 2);
            arm.rotationPointZ = 0;
            arm.render(0.0625F);
        }

        GlStateManager.disableBlend();
    }

    /**
     * Render left hand 
     */
    public void renderLeftArm(EntityPlayer player)
    {
        ResourceLocation texture = this.getEntityTexture(player);

        if (texture != null)
        {
            Minecraft.getMinecraft().renderEngine.bindTexture(texture);
        }

        this.mainModel.swingProgress = 0.0F;
        this.mainModel.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);

        GlStateManager.color(1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();

        for (ModelCustomRenderer arm : ((ModelCustom) this.mainModel).left)
        {
            arm.rotateAngleX = 0;
            arm.rotationPointX = 6;
            arm.rotationPointY = 13.8F - (arm.limb.size[1] > 8 ? arm.limb.size[1] : arm.limb.size[1] + 2F);
            arm.rotationPointZ = 0;
            arm.render(0.0625F);
        }

        GlStateManager.disableBlend();
    }
}