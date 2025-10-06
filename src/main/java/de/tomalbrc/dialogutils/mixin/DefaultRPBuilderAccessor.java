package de.tomalbrc.dialogutils.mixin;

import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.impl.generation.DefaultRPBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.HashMap;

@Mixin(value = DefaultRPBuilder.class, remap = false)
public interface DefaultRPBuilderAccessor {
    @Accessor(value = "fileMap")
    default HashMap<String, PackResource> getFileMap() {
        throw new UnsupportedOperationException();
    }
}
