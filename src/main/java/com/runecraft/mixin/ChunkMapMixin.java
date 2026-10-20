package com.runecraft.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 1.21.1 livelock in chunk unloading. An unload callback re-arms itself while its holder is not ready for
 * saving, which since 1.21 includes "a generation task still references it", and checks that before checking whether
 * the holder is still pending unload. When a player comes back to a chunk that was being unloaded (a teleport under
 * lag), the holder is taken back and claimed by new generation tasks; its old callback then re-queues itself
 * synchronously onto the queue ChunkMap.processUnloads is draining. With the "always has time" supplier that
 * MinecraftServer.stopServer passes, that call never returns, the main-thread generation steps that would release the
 * claims never run, and Save and Quit or /stop hangs. Dropping the callback of a holder that is no longer pending
 * unload is what vanilla does a moment later anyway (its pendingUnloads.remove(pos, holder) check fails); if the
 * holder is dropped again, processUnloads schedules a fresh callback.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Shadow
    @Final
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads;

    @Inject(method = "scheduleUnload", at = @At("HEAD"), cancellable = true)
    private void runecraft$dropStaleUnload(long pos, ChunkHolder holder, CallbackInfo ci) {
        if (this.pendingUnloads.get(pos) != holder) {
            ci.cancel();
        }
    }
}
