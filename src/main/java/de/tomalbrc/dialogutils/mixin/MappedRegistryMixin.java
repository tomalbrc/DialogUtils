package de.tomalbrc.dialogutils.mixin;

import de.tomalbrc.dialogutils.util.RegistryHack;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> implements RegistryHack {
    @Shadow
    @Nullable
    private Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;

    @Shadow
    private boolean frozen;

    @Shadow MappedRegistry.TagSet<T> allTags;

    @Shadow public abstract Holder.Reference<T> register(ResourceKey<T> resourceKey, T object, RegistrationInfo registrationInfo);

    @Shadow @Final private ObjectList<Holder.Reference<T>> byId;

    @Shadow @Final private Map<ResourceLocation, Holder.Reference<T>> byLocation;

    @Shadow @Final private Map<ResourceKey<T>, Holder.Reference<T>> byKey;

    @Override
    public void du$unfreeze() {
        this.unregisteredIntrusiveHolders = new IdentityHashMap<>();
        this.allTags = MappedRegistry.TagSet.unbound();
        this.frozen = false;
    }

    @Override
    public void du$remove(ResourceLocation key) {
        var o = byLocation.remove(key);
        if (o != null) {
            this.byId.remove(o);
            this.byKey.remove(o.key());
        }
    }
}
