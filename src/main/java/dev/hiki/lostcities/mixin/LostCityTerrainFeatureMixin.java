package dev.hiki.lostcities.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.ChunkDriver;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.LostCityTerrainFeature;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Finaliza com o MESMO BuildingInfo que recebeu os callbacks durante a geração.
 * Não guarda estado em campos: a geração pode ocorrer em múltiplas threads.
 */
@Mixin(value = LostCityTerrainFeature.class, remap = false)
public abstract class LostCityTerrainFeatureMixin {
    @Shadow @Final public IDimensionInfo provider;
    @Shadow public abstract RandomSource getRandom();

    @WrapOperation(
        method = "generate",
        at = @At(value = "INVOKE",
            target = "Lmcjty/lostcities/worldgen/ChunkDriver;actuallyGenerate(Lnet/minecraft/world/level/chunk/ChunkAccess;)V"),
        require = 1, expect = 1, allow = 1
    )
    private void hiki$finishOriginalChunk(ChunkDriver driver, ChunkAccess chunk,
                                         Operation<Void> original,
                                         @Local ChunkCoord coord,
                                         @Local BuildingInfo info) {
        original.call(driver, chunk);
        // O driver ainda aponta para o chunk atual. O finally original continua
        // responsável por restaurá-lo, inclusive se um callback lançar exceção.
        RandomSource random = getRandom();
        random.setSeed(provider.getSeed()
                ^ (long) coord.chunkX() * 341873128712L
                ^ (long) coord.chunkZ() * 132897987541L);
        ChunkFixerInvoker.hiki$generateVines(coord, provider.getWorld(), provider, random);
        info.getPostTodo().forEach((pos, runnable) -> runnable.run());
        info.clearPostTodo();
        ChunkFixerInvoker.hiki$removeUnsupportedBoundaryVines(coord, provider.getWorld());
    }

    @Redirect(
        method = "generate",
        at = @At(value = "INVOKE",
            target = "Lmcjty/lostcities/worldgen/ChunkFixer;fix(Lmcjty/lostcities/worldgen/IDimensionInfo;Lmcjty/lostcities/varia/ChunkCoord;)V"),
        require = 1, expect = 1, allow = 1
    )
    private void hiki$skipReFetchingFinalization(IDimensionInfo provider, ChunkCoord coord) {
        // Já executado acima. Não buscar outro BuildingInfo no cache e não
        // repetir vinhas/callbacks depois de restaurar o driver.
    }
}
