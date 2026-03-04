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
        // 1. 放置地标
        level.setBlock(this.templatePosition, Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
        level.setBlock(this.templatePosition.above(), Blocks.REDSTONE_TORCH.defaultBlockState(), 3);

        // --- 修复获取 TemplateManager 的代码 ---
        // 从当前生成世界中获取正确的 StructureTemplateManager
        StructureTemplateManager templateManager = level.getLevel().getServer().getStructureManager();
        Optional<StructureTemplate> optionalTemplate = templateManager.get(this.templatePath);

        if (optionalTemplate.isPresent()) {
            StructureTemplate template = optionalTemplate.get();
            System.out.println("==================================================");
            System.out.println("【成功读取 NBT】");
            System.out.println("文件路径: " + this.templatePath);
            System.out.println("实际尺寸: " + template.getSize());
            System.out.println("目标位置: " + this.templatePosition);
            System.out.println("==================================================");

            if (template.getSize().getX() == 0) {
                System.err.println("【警告】尺寸依然是 0！你可能忘记刷新 Gradle 缓存了！");
            } else {
                // ！！！终极暴力放置！！！
                StructurePlaceSettings forcedSettings = new StructurePlaceSettings()
                        .setRotation(this.placeSettings.getRotation())
                        .setIgnoreEntities(false);

                template.placeInWorld(level, this.templatePosition, this.templatePosition, forcedSettings, random, 2);
                System.out.println("【执行】已强制将模板放置于世界中！");
            }
        } else {
            System.err.println("【致命错误】运行时环境根本找不到 " + this.templatePath + " 文件！");
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