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
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

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

    private void generateModularStack(StructurePiecesBuilder builder, GenerationContext context, BlockPos startPos, Rotation rotation) {
        StructureTemplateManager manager = context.structureTemplateManager();

//        // ==========================================
//        // ▼ 灵活的链式调用接口展示 ▼
//        // ==========================================
//        // 1. 在原点放 1 个底座 (test1)
//        BlockPos currentPos = appendPieces(builder, manager, startPos, rotation, Direction.UP, "test1", 1, 0);
//        // 2. 接着往上叠 3 层楼 (test2)，完美贴合 (间距0)
//        currentPos = appendPieces(builder, manager, currentPos, rotation, Direction.UP, "test2", 3, 0);
//        // 3. 然后向东（X正方向）铺开 2 个底座 (test1)，互相间隔 5 格
//        currentPos = appendPieces(builder, manager, currentPos, rotation, Direction.EAST, "test1", 2, 5);
//        // 4. 最后在新的位置向南延伸 1 个 test2
//        appendPieces(builder, manager, currentPos, rotation, Direction.SOUTH, "test2", 1, 0);


        // ==========================================
        // 1. 核心参数设置 (10宽 x 4高)
        // ==========================================
        int width = 10;   // 墙的宽度（列数）
        int height = 4;   // 每一列的总高度（1个底座 + 3个楼层）
        int offset = 4;   // 斜对角的偏移量（格数）

        // 定义墙的延伸方向（宽度方向）和 建筑后退的深度方向（斜对角的另一轴）
        Direction wallDirection = Direction.EAST;
        Direction depthDirection = Direction.SOUTH;

        // 根据整个城市的随机旋转，计算出绝对的物理朝向
        Direction actualWallDir = rotation.rotate(wallDirection);
        Direction actualDepthDir = rotation.rotate(depthDirection);

        // ==========================================
        // 2. 预读取 NBT 尺寸 (防止高度计算错误)
        // ==========================================
        ResourceLocation baseLoc = ResourceLocation.fromNamespaceAndPath("mkgmultiversecompendium", "test1");
        ResourceLocation floorLoc = ResourceLocation.fromNamespaceAndPath("mkgmultiversecompendium", "test2");

        // 如果没读到，给个默认尺寸防止崩溃
        Vec3i baseSize = manager.get(baseLoc).map(t -> t.getSize(rotation)).orElse(new Vec3i(5, 5, 5));
        Vec3i floorSize = manager.get(floorLoc).map(t -> t.getSize(rotation)).orElse(new Vec3i(5, 5, 5));

        // ==========================================
        // 3. 矩阵生成核心循环
        // ==========================================
        BlockPos currentColumnBase = startPos;

        // 外层循环：横向平移，生成 10 列
        for (int col = 0; col < width; col++) {
            BlockPos currentPos = currentColumnBase;

            // 内层循环：纵向堆叠，生成 4 层
            for (int layer = 0; layer < height; layer++) {
                // 第一层用 test1，上面全用 test2
                String pieceName = (layer == 0) ? "test1" : "test2";
                builder.addPiece(new CyberpunkPiece(manager, pieceName, currentPos, rotation));
                // 获取当前这块建筑的高度，以便知道下一个建筑该往上抬多高
                int currentPieceYSize = (layer == 0) ? baseSize.getY() : floorSize.getY();
                // 关键逻辑：如果下一层是“最上面两层”的一员，就不偏移（竖直向上）
                // height = 4 的情况：layer 为 0, 1 时发生偏移，layer 为 2 时（即倒数第二层）不再偏移，使最高两层垂直对齐。
                boolean nextIsVertical = (layer >= height - 2);
                // 计算对角线偏移向量：墙的方向偏移 4 格 + 深度的方向偏移 4 格
                int stepX = nextIsVertical ? 0 : -(actualWallDir.getStepX() * offset - actualDepthDir.getStepX() * offset);
                int stepZ = nextIsVertical ? 0 : -(actualWallDir.getStepZ() * offset - actualDepthDir.getStepZ() * offset);
                // 坐标指针移动到下一个位置
                currentPos = currentPos.offset(stepX, currentPieceYSize, stepZ);
            }
            // 一列盖完了，准备盖下一列。
            // 下一列的起点紧贴着当前列的宽度，深度不变。
            int nextColX = actualWallDir.getStepX() * baseSize.getX();
            int nextColZ = actualWallDir.getStepZ() * baseSize.getZ();

            currentColumnBase = currentColumnBase.offset(nextColX, 0, nextColZ);
        }



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