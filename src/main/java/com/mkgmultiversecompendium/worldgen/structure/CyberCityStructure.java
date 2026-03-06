package com.mkgmultiversecompendium.worldgen.structure;

import com.mkgmultiversecompendium.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

public class CyberCityStructure extends Structure {
    public static final MapCodec<CyberCityStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(settingsCodec(instance)).apply(instance, CyberCityStructure::new)
    );

    public CyberCityStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        BlockPos centerPos = context.chunkPos().getMiddleBlockPosition(0);
        int groundY = context.chunkGenerator().getFirstFreeHeight(
                centerPos.getX(), centerPos.getZ(),
                Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()
        );

        BlockPos spawnPos = new BlockPos(centerPos.getX(), groundY, centerPos.getZ());
        Rotation rotation = Rotation.getRandom(context.random());

        return Optional.of(new GenerationStub(spawnPos, builder -> {
            generateModularStack(builder, context, spawnPos, rotation);
        }));
    }

    /**
     * ==================================== ==================================== ====================================
     * */
    private void generateModularStack(StructurePiecesBuilder builder, GenerationContext context, BlockPos startPos, Rotation rotation) {
        StructureTemplateManager manager = context.structureTemplateManager();
        WorldgenRandom random = context.random();
        String ns = "mkgmultiversecompendium";

        // ==========================================
        // 1. 参数定义
        // ==========================================
        int rangeX = 64;
        int rangeZ = 64;
        int overlap = 1;
        int maxTowers = 100;

        // 预获取底座尺寸
        Vec3i baseDim = manager.get(ResourceLocation.fromNamespaceAndPath(ns, "test_base_1"))
                .map(t -> t.getSize(rotation)).orElse(new Vec3i(8, 4, 8));

        // ==========================================
        // 2. 第一阶段：平铺底座 (保持原朝向以对齐地板)
        // ==========================================
        int stepX = baseDim.getX() - overlap;
        int stepZ = baseDim.getZ() - overlap;
        for (int x = 0; x < rangeX; x += stepX) {
            for (int z = 0; z < rangeZ; z += stepZ) {
                BlockPos gridPos = startPos.offset(x, 0, z);
                CyberpunkPiece basePiece = new CyberpunkPiece(manager, "test_base_1", gridPos, rotation);
                builder.addPiece(basePiece);
                builder.getBoundingBox().encapsulate(basePiece.getBoundingBox());
            }
        }

        // ==========================================
        // 3. 第二阶段：随机生成 100 座异形塔
        // ==========================================
        for (int i = 0; i < maxTowers; i++) {
            // 随机坐标
            int randX = random.nextInt(rangeX);
            int randZ = random.nextInt(rangeZ);

            // 【新增：随机高度偏移】 0-2 格的随机向上位移
            int heightOffset = random.nextInt(3);
            BlockPos towerBasePos = startPos.offset(randX, heightOffset, randZ);

            // 【新增：随机旋转】 让每座塔的朝向都不一样
            Rotation towerRot = Rotation.getRandom(random);

            // 随机总层数
            int towerFloors = 3 + random.nextInt(6);
            int currentY = 0;

            for (int f = 0; f < towerFloors; f++) {
                // 选择 NBT 模块
                String pieceName = (f == 0) ? "test_root_1" : (f == towerFloors - 1) ? "test_roof_1" : "test_upper_floor_1";
                BlockPos floorPos = towerBasePos.above(currentY);

                // 使用随机后的 towerRot
                CyberpunkPiece towerPiece = new CyberpunkPiece(manager, pieceName, floorPos, towerRot);
                builder.addPiece(towerPiece);
                builder.getBoundingBox().encapsulate(towerPiece.getBoundingBox());

                // 动态累加高度（注意：高度必须基于该模块在对应旋转下的尺寸）
                currentY += manager.get(ResourceLocation.fromNamespaceAndPath(ns, pieceName))
                        .map(t -> t.getSize(towerRot).getY()).orElse(4);

                // 30% 概率生成桥梁（也会继承塔的随机朝向）
                if (f > 1 && f < towerFloors - 1 && random.nextFloat() < 0.3f) {
                    CyberpunkPiece bridge = new CyberpunkPiece(manager, "test_bridge_1", floorPos, towerRot);
                    builder.addPiece(bridge);
                    builder.getBoundingBox().encapsulate(bridge.getBoundingBox());
                }
            }
        }
    }
    /**
    * ==================================== ==================================== ====================================
    * */
    /**
     * 辅助方法：动态获取对应层级 NBT 的高度
     */
    private int getLevelHeight(StructureTemplateManager manager, Rotation rot, String ns, int layerIndex) {
        String name;
        if (layerIndex == 0) name = "test_base_1";
        else if (layerIndex == 1) name = "test_root_1";
        else name = "test_upper_floor_1"; // 默认用标准楼层高度计算

        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(ns, name);
        return manager.get(loc).map(t -> t.getSize(rot).getY()).orElse(4); // 默认 4 格高
    }

    /**
     * 核心迭代函数：在指定方向上，连续放置多个 NBT 建筑，并实现包围盒紧密对齐。
     *
     * @param builder    片段构建器
     * @param manager    模板管理器
     * @param startPos   起始坐标（开始生成的锚点）
     * @param rotation   全局旋转（用来修正方向和尺寸）
     * @param direction  迭代方向（UP, DOWN, NORTH, SOUTH, EAST, WEST）
     * @param nbtName    要迭代的 nbt 文件名 (如 "test1")
     * @param count      迭代次数
     * @param spacing    建筑之间的额外间距（0代表紧贴无缝拼接）
     * @return BlockPos  返回下一次本该生成的【起始坐标】，极其方便链式调用
     */
    private BlockPos appendPieces(StructurePiecesBuilder builder, StructureTemplateManager manager, BlockPos startPos, Rotation rotation, Direction direction, String nbtName, int count, int spacing) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("mkgmultiversecompendium", nbtName);
        Optional<StructureTemplate> templateOpt = manager.get(location);
        // 防御性编程：如果没有找到 NBT 或者不需要迭代，直接返回原位
        if (templateOpt.isEmpty() || count <= 0) {
            return startPos;
        }
        StructureTemplate template = templateOpt.get();
        // 关键1：获取旋转后的实际尺寸，保证对齐不穿模
        Vec3i rotatedSize = template.getSize(rotation);
        BlockPos currentPos = startPos;

        // 关键2：将你指定的相对方向，转换为全局旋转后的绝对方向
        // 这样即使整个城市被随机旋转了 90 度，"向右排布" 的逻辑依然是正确的
        Direction actualDir = rotation.rotate(direction);
        for (int i = 0; i < count; i++) {
            // 放置当前建筑
            builder.addPiece(new CyberpunkPiece(manager, nbtName, currentPos, rotation));
            // 计算该建筑占用空间 + 额外间距，得出下一个建筑的精确偏移量
            int offsetX = actualDir.getStepX() * (rotatedSize.getX() + spacing);
            int offsetY = actualDir.getStepY() * (rotatedSize.getY() + spacing);
            int offsetZ = actualDir.getStepZ() * (rotatedSize.getZ() + spacing);
            // 指针移动到下一个位置
            currentPos = currentPos.offset(offsetX, offsetY, offsetZ);
        }
        // 返回最终的接力点
        return currentPos;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CYBER_CITY.get();
    }
}