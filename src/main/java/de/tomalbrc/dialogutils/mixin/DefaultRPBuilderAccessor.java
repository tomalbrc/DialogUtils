package de.tomalbrc.dialogutils.mixin;

import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.impl.generation.DefaultRPBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = DefaultRPBuilder.class, remap = false)
public interface DefaultRPBuilderAccessor {
    @Accessor(value = "fileMap")
    HashMap<String, PackResource> getFileMap();
}
