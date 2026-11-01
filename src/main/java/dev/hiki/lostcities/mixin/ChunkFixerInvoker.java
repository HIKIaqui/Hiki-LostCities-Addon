package dev.hiki.lostcities.mixin;

import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.ChunkFixer;
import mcjty.lostcities.worldgen.IDimensionInfo;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reutiliza a finalização de vinhas do mod, sem copiar sua implementação. */
@Mixin(value = ChunkFixer.class, remap = false)
public interface ChunkFixerInvoker {
    @Invoker("generateVines")
    static void hiki$generateVines(ChunkCoord coord, LevelAccessor world,
                                    IDimensionInfo provider, RandomSource random) {
        throw new AssertionError("Mixin não aplicado");
    }

    @Invoker("removeUnsupportedBoundaryVines")
    static void hiki$removeUnsupportedBoundaryVines(ChunkCoord coord, LevelAccessor world) {
        throw new AssertionError("Mixin não aplicado");
    }
}
