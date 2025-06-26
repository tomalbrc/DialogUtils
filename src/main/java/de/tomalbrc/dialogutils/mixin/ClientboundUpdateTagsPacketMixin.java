package de.tomalbrc.dialogutils.mixin;

import de.tomalbrc.dialogutils.DialogUtils;
import de.tomalbrc.dialogutils.util.Globals;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DialogTags;
import net.minecraft.tags.TagNetworkSerialization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ClientboundUpdateTagsPacket.class)
public class ClientboundUpdateTagsPacketMixin {
    @Inject(method = "<init>(Ljava/util/Map;)V", at = @At("RETURN"))
    private void du$hackQuickActions(Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> map, CallbackInfo ci) {
        var payload = map.get(Registries.DIALOG);

        var reg = DialogUtils.SERVER.registryAccess().lookup(Registries.DIALOG);
        reg.ifPresent(registry -> {
            var list = new IntArrayList(payload.tags.get(DialogTags.QUICK_ACTIONS.location()));
            for (ResourceLocation id : DialogUtils.getQuickActions()) {
                var obj = registry.getValue(id);
                var rawid = registry.getId(obj);
                list.add(rawid);
            }

            payload.tags.put(DialogTags.QUICK_ACTIONS.location(), list);
        });
    }
}
