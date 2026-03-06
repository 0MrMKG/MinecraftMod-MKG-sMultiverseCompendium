package com.mkgmultiversecompendium.worldgen.structure.cybercity_fortest;

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

import java.util.ArrayList;
import java.util.List;
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

    public void generateModularStack(StructurePiecesBuilder builder, GenerationContext context, BlockPos startPos, Rotation rotation) {
        StructureTemplateManager manager = context.structureTemplateManager();
        if (manager == null) return;

        WorldgenRandom random = context.random();
        String ns = "mkgmultiversecompendium";

        int rangeX = 64;
        int rangeZ = 64;

        // ==========================================
        // 【新增：控制接口】 (你可以将这四个变量移到方法参数中)
        // ==========================================
        int housesPerRow = 5;       // 控制接口：每排生成几栋房子
        int streetWidth = 20;       // 控制接口：两排房子之间的“街道”宽度 (Z轴方向)
        int houseSpacingX = 4;      // 【新增】控制接口：同一排房屋之间的距离 (X轴方向)

        // ==========================================
        // 1. 获取基础模块的原始尺寸 (Rotation.NONE)
        // ==========================================
        Vec3i rawBaseDim = manager.get(ResourceLocation.fromNamespaceAndPath(ns, "test_base_1"))
                .map(t -> t.getSize(Rotation.NONE)).orElse(new Vec3i(8, 4, 8));

        Vec3i rawRootDim = manager.get(ResourceLocation.fromNamespaceAndPath(ns, "test_root_1"))
                .map(t -> t.getSize(Rotation.NONE)).orElse(new Vec3i(8, 4, 8));

        int baseY = rawBaseDim.getY();

        // ==========================================
        // 2. 铺设地基 (局部坐标系)
        // ==========================================
        int stepX = Math.max(1, rawBaseDim.getX() - 1);
        int stepZ = Math.max(1, rawBaseDim.getZ() - 1);

        for (int lx = 0; lx <= rangeX; lx += stepX) {
            for (int lz = 0; lz <= rangeZ; lz += stepZ) {
                BlockPos worldOffset = getRotatedOffset(lx, 0, lz, rotation);
                CyberpunkPiece basePiece = new CyberpunkPiece(manager, "test_base_1", startPos.offset(worldOffset), rotation);
                builder.addPiece(basePiece);
                builder.getBoundingBox().encapsulate(basePiece.getBoundingBox());
            }
        }

        // ==========================================
        // 3. 动态计算以中轴线为对称的房屋 Z 坐标
        // ==========================================
        int centerZ = rangeZ / 2; // 地基中轴线 (Z = 32)

        // 第一排在北边，面向街道 (+Z)
        int row0_lz = centerZ - (streetWidth / 2) - rawRootDim.getZ();
        // 第二排在南边，面向街道 (旋转180度后，向-Z生长)
        int row1_lz = centerZ + (streetWidth / 2);

        int[] localZRows = { row0_lz, row1_lz };

        // ==========================================
        // 4. 【修改点】计算房屋在 X 轴的分布 (居中对齐算法)
        // ==========================================
        // 计算这一排房屋的总跨度：所有房屋的宽度之和 + 所有间距的宽度之和
        int totalRowWidthX = (housesPerRow * rawRootDim.getX()) + ((housesPerRow - 1) * houseSpacingX);

        // 计算起步的 X 坐标，让这一整排房子在地基 (rangeX) 上完美居中
        int startLx = (rangeX - totalRowWidthX) / 2;

        // ==========================================
        // 5. 生成房屋
        // ==========================================
        for (int i = 0; i < localZRows.length; i++) {
            int baseLz = localZRows[i];
            Rotation houseFacing = (i == 0) ? rotation : rotation.getRotated(Rotation.CLOCKWISE_180);

            for (int j = 0; j < housesPerRow; j++) {
                // 【修改点】当前房屋的逻辑左上角 X 坐标
                int baseLx = startLx + j * (rawRootDim.getX() + houseSpacingX);

                // 180度旋转的锚点偏移补偿
                int houseAnchorX = (i == 0) ? baseLx : baseLx + rawRootDim.getX() - 1;
                int houseAnchorZ = (i == 0) ? baseLz : baseLz + rawRootDim.getZ() - 1;

                // 地基补全的锚点同理 (以地基的尺寸为准)
                int foundAnchorX = (i == 0) ? baseLx : baseLx + rawBaseDim.getX() - 1;
                int foundAnchorZ = (i == 0) ? baseLz : baseLz + rawBaseDim.getZ() - 1;

                int randHeight = random.nextInt(3); // 0-2格起伏
                int houseStartY = baseY + randHeight;

                // --- 垂直补全地基 ---
                for (int h = 1; h <= randHeight; h++) {
                    BlockPos downOffset = getRotatedOffset(foundAnchorX, houseStartY - h, foundAnchorZ, rotation);
                    CyberpunkPiece foundation = new CyberpunkPiece(manager, "test_base_1", startPos.offset(downOffset), houseFacing);
                    builder.addPiece(foundation);
                    builder.getBoundingBox().encapsulate(foundation.getBoundingBox());
                }

                // --- 堆叠楼层 ---
                int currentLocalY = houseStartY;
                int totalFloors = 3 + random.nextInt(4);

                for (int f = 0; f < totalFloors; f++) {
                    String pieceName = (f == 0) ? "test_root_1" : (f == totalFloors - 1) ? "test_roof_1" : "test_upper_floor_1";

                    BlockPos floorWorldOffset = getRotatedOffset(houseAnchorX, currentLocalY, houseAnchorZ, rotation);
                    CyberpunkPiece housePiece = new CyberpunkPiece(manager, pieceName, startPos.offset(floorWorldOffset), houseFacing);

                    builder.addPiece(housePiece);
                    builder.getBoundingBox().encapsulate(housePiece.getBoundingBox());

                    // 获取并累加真实高度
                    int floorRawHeight = manager.get(ResourceLocation.fromNamespaceAndPath(ns, pieceName))
                            .map(t -> t.getSize(Rotation.NONE).getY())
                            .orElse(4);

                    currentLocalY += floorRawHeight;
                }
            }
        }
    }

    /**
     * 核心坐标转换工具：将局部偏移量转化为基于整体朝向的世界偏移量
     * 彻底解决旋转后坐标撕裂、漂移的问题
     */
    private BlockPos getRotatedOffset(int localX, int localY, int localZ, Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                return new BlockPos(-localZ, localY, localX);
            case CLOCKWISE_180:
                return new BlockPos(-localX, localY, -localZ);
            case COUNTERCLOCKWISE_90:
                return new BlockPos(localZ, localY, -localX);
            case NONE:
            default:
                return new BlockPos(localX, localY, localZ);
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