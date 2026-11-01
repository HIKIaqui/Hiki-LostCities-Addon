package dev.hiki.lostcities;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Confere os contratos do JAR oficial sem inicializar o Minecraft.
 * Não substitui um teste de geração dentro do jogo.
 */
class LostCitiesContractTest {
    private static final String ROOT = "mcjty/lostcities/";

    private static ClassNode read(String name) throws IOException {
        try (InputStream input = LostCitiesContractTest.class.getClassLoader()
                .getResourceAsStream(ROOT + name + ".class")) {
            assertNotNull(input, "Lost Cities oficial deve estar no classpath");
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static MethodNode method(ClassNode node, String name, String desc) {
        return node.methods.stream()
                .filter(m -> m.name.equals(name) && m.desc.equals(desc))
                .findFirst().orElseThrow(() -> new AssertionError("Método ausente: " + name + desc));
    }

    private static List<MethodInsnNode> calls(MethodNode method, String owner, String name) {
        return StreamSupport.stream(method.instructions.spliterator(), false)
                .filter(i -> i instanceof MethodInsnNode)
                .map(i -> (MethodInsnNode) i)
                .filter(i -> i.owner.equals(ROOT + owner) && i.name.equals(name))
                .toList();
    }

    private static MethodNode generate() throws IOException {
        return method(read("worldgen/LostCityTerrainFeature"), "generate",
                "(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/chunk/ChunkAccess;)V");
    }

    @Test
    void exactlyOneFlushAndOneOldFinalization() throws IOException {
        MethodNode generate = generate();
        assertEquals(1, calls(generate, "worldgen/ChunkDriver", "actuallyGenerate").size());
        assertEquals(1, calls(generate, "worldgen/ChunkFixer", "fix").size());
        assertEquals("(Lnet/minecraft/world/level/chunk/ChunkAccess;)V",
                calls(generate, "worldgen/ChunkDriver", "actuallyGenerate").getFirst().desc);
        assertEquals("(Lmcjty/lostcities/worldgen/IDimensionInfo;Lmcjty/lostcities/varia/ChunkCoord;)V",
                calls(generate, "worldgen/ChunkFixer", "fix").getFirst().desc);
    }

    @Test
    void originalBuildingInfoAndCoordinateAreLiveAtWrappedCall() throws IOException {
        MethodNode generate = generate();
        AbstractInsnNode call = calls(generate, "worldgen/ChunkDriver", "actuallyGenerate").getFirst();
        int index = generate.instructions.indexOf(call);
        for (String desc : List.of("Lmcjty/lostcities/worldgen/lost/BuildingInfo;",
                                  "Lmcjty/lostcities/varia/ChunkCoord;")) {
            List<LocalVariableNode> live = generate.localVariables.stream()
                    .filter(v -> v.desc.equals(desc))
                    .filter(v -> generate.instructions.indexOf(v.start) <= index
                            && index < generate.instructions.indexOf(v.end))
                    .toList();
            assertEquals(1, live.size(), "@Local precisa de um único local válido: " + desc);
        }
    }

    @Test
    void wrappedCallIsInsideProtectedDriverScope() throws IOException {
        MethodNode generate = generate();
        int flush = generate.instructions.indexOf(
                calls(generate, "worldgen/ChunkDriver", "actuallyGenerate").getFirst());
        int oldFix = generate.instructions.indexOf(
                calls(generate, "worldgen/ChunkFixer", "fix").getFirst());
        assertTrue(flush < oldFix);
        assertTrue(generate.tryCatchBlocks.stream().anyMatch(t -> t.type == null
                && generate.instructions.indexOf(t.start) <= flush
                && flush < generate.instructions.indexOf(t.end)
                && generate.instructions.indexOf(t.end) < oldFix),
                "A finalização nova precisa continuar protegida pelo finally do driver");
    }

    @Test
    void bothPlannerCachesReceiveLazyIntSuppliers() throws IOException {
        MethodNode init = method(read("worldgen/plan/ChunkPlanner"), "<clinit>", "()V");
        List<MethodInsnNode> constructors = calls(init, "varia/TimedCache", "<init>");
        assertEquals(2, constructors.size());
        assertTrue(constructors.stream().allMatch(c ->
                c.desc.equals("(Ljava/util/function/IntSupplier;)V")));
    }

    @Test
    void finalizationHelpersMatchOriginalApi() throws IOException {
        MethodNode random = method(read("worldgen/LostCityTerrainFeature"),
                "getRandom", "()Lnet/minecraft/util/RandomSource;");
        assertNotEquals(0, random.access & Opcodes.ACC_PUBLIC);
        ClassNode fixer = read("worldgen/ChunkFixer");
        for (MethodNode m : List.of(
                method(fixer, "generateVines", "(Lmcjty/lostcities/varia/ChunkCoord;Lnet/minecraft/world/level/LevelAccessor;Lmcjty/lostcities/worldgen/IDimensionInfo;Lnet/minecraft/util/RandomSource;)V"),
                method(fixer, "removeUnsupportedBoundaryVines", "(Lmcjty/lostcities/varia/ChunkCoord;Lnet/minecraft/world/level/LevelAccessor;)V"))) {
            assertEquals(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                    m.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC));
        }
    }
}
