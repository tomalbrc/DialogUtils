package de.tomalbrc.dialogutils.mixin;

import eu.pb4.polymer.resourcepack.impl.generation.DefaultRPBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.io.InputStream;

@Mixin(value = DefaultRPBuilder.class, remap = false)
public interface DefaultRPBuilderAccessor {
    @Invoker("getSourceStream")
    InputStream inkvokeGetSourceStream(String path);
}
