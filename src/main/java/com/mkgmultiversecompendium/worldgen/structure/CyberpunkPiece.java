package com.mkgmultiversecompendium.worldgen.structure;

import com.mkgmultiversecompendium.registry.ModStructurePieces;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

public class CyberpunkPiece extends TemplateStructurePiece {
    private final ResourceLocation templatePath;

    public CyberpunkPiece(StructureTemplateManager manager, String templateName, BlockPos pos, Rotation rotation) {
        super(ModStructurePieces.CYBER_PIECE.get(), 0, manager,
                ResourceLocation.fromNamespaceAndPath("mkgmultiversecompendium", templateName),
                templateName, makeSettings(rotation), pos);
        this.templatePath = ResourceLocation.fromNamespaceAndPath("mkgmultiversecompendium", templateName);

        // 初始化基础包围盒，防止父类崩溃
        this.boundingBox = BoundingBox.fromCorners(pos, pos.offset(15, 15, 15));
    }

    public CyberpunkPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructurePieces.CYBER_PIECE.get(), tag, context.structureTemplateManager(),
                (res) -> makeSettings(Rotation.valueOf(tag.getString("Rot"))));
        this.templatePath = ResourceLocation.parse(tag.getString("Template"));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // 1. 放置地标 (保持不变)
        level.setBlock(this.templatePosition, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        level.setBlock(this.templatePosition.above(), Blocks.REDSTONE_TORCH.defaultBlockState(), 3);

        // --- 修复获取 TemplateManager 的代码 ---
        StructureTemplateManager templateManager = level.getLevel().getServer().getStructureManager();
        Optional<StructureTemplate> optionalTemplate = templateManager.get(this.templatePath);

        if (optionalTemplate.isPresent()) {
            StructureTemplate template = optionalTemplate.get();

            if (template.getSize().getX() == 0) {
                System.err.println("【警告】尺寸为 0！");
            } else {
                // ==========================================
                // 【核心修改点】设置忽略空气的处理器
                // ==========================================
                StructurePlaceSettings forcedSettings = new StructurePlaceSettings()
                        .setRotation(this.placeSettings.getRotation())
                        .setMirror(this.placeSettings.getMirror()) // 建议同步镜像设置
                        .setIgnoreEntities(false)
                        // 添加这一行：忽略 NBT 里的空气方块，保护世界原本的方块
                        .addProcessor(BlockIgnoreProcessor.AIR)
                        .setBoundingBox(box); // 确保只在当前区块范围内处理

                // 使用 flag 2 (仅同步到客户端，不触发方块更新)，从源头减少掉落物
                template.placeInWorld(level, this.templatePosition, this.templatePosition, forcedSettings, random, 2);

                System.out.println("【执行】已执行“空气掩码”放置逻辑，交叉部分已融合！");
            }
        } else {
            System.err.println("【致命错误】找不到文件: " + this.templatePath);
        }
    }
    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", this.placeSettings.getRotation().name());
        tag.putString("Template", this.templatePath.toString());
    }

    private static StructurePlaceSettings makeSettings(Rotation rotation) {
        return new StructurePlaceSettings()
                .setRotation(rotation)
                .setIgnoreEntities(false);
    }


    public BlockPos getTemplateSize(StructureTemplateManager manager) {
        return manager.get(this.templatePath)
                .map(t -> new BlockPos(t.getSize().getX(), t.getSize().getY(), t.getSize().getZ()))
                .orElse(new BlockPos(0, 0, 0));
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, net.minecraft.world.level.ServerLevelAccessor level, RandomSource random, BoundingBox box) {}
}