package dev.hiki.lostcities.mixin;

import java.util.function.IntSupplier;
import mcjty.lostcities.setup.Config;
import mcjty.lostcities.worldgen.plan.ChunkPlanner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** O menu de criação de mundo usa os caches antes de existir config de servidor. */
@Mixin(value = ChunkPlanner.class, remap = false)
public abstract class ChunkPlannerMixin {
    @ModifyArg(
        method = "<clinit>",
        at = @At(value = "INVOKE",
            target = "Lmcjty/lostcities/varia/TimedCache;<init>(Ljava/util/function/IntSupplier;)V"),
        index = 0,
        require = 2, expect = 2, allow = 2
    )
    private static IntSupplier hiki$guardUnloadedServerConfig(IntSupplier original) {
        // Checado a cada consulta, para respeitar carregamento/reload da config.
        return () -> Config.SERVER_CONFIG.isLoaded()
                ? original.getAsInt()
                : Config.CACHE_CLEANUP_SECONDS.getDefault();
    }
}
