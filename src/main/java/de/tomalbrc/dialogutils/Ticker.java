package de.tomalbrc.dialogutils;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class Ticker {
    public static Object2ObjectArrayMap<UUID, ViewerData> children = new Object2ObjectArrayMap<>();

    public static void add(UUID uuid, ViewerData tickingChild) {
        children.put(uuid, tickingChild);
    }

    public static void remove(UUID uuid) {
        children.remove(uuid);
    }

    public static void tick(MinecraftServer server) {
        for (ViewerData viewerData : children.values()) {
            viewerData.tickingChild.tick(server, viewerData.time);
            viewerData.addTime();
        }
    }

    public static boolean contains(UUID uuid) {
        return children.containsKey(uuid);
    }

    @FunctionalInterface
    public interface TickingChild {
        void tick(MinecraftServer server, long time);
    }

    public static class ViewerData {
        private long time;
        private final TickingChild tickingChild;

        public int page;

        public ViewerData(TickingChild tickingChild, int page) {
            this.time = 0;
            this.tickingChild = tickingChild;
            this.page = page;
        }

        public TickingChild tickingChild() {
            return this.tickingChild;
        }

        public long time() {
            return this.time;
        }

        public void addTime() {
            this.time++;
        }
    }
}
