package com.autumnwind.botb.mixin.client;

import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the title art swap the texture sampler. */
@Mixin(AbstractTexture.class)
public interface AbstractTextureAccessor {

    @Accessor("sampler")
    void botb$setSampler(GpuSampler sampler);
}
