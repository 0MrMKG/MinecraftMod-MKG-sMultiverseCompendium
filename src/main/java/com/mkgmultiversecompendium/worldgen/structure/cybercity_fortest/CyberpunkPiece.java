package com.mkgmultiversecompendium.worldgen.structure.cybercity_fortest;

import com.mkgmultiversecompendium.registry.ModStructurePieces;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
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

        // ==========================================
        // 【核心修复：动态计算真实包围盒】
        // 绝对不能写死 15x15x15！
        // ==========================================
        StructureTemplate template = manager.getOrCreate(this.templatePath);
        // 通过模板和当前的旋转设定，计算出严丝合缝的包围盒
        this.boundingBox = template.getBoundingBox(this.placeSettings, pos);
    }

    public CyberpunkPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructurePieces.CYBER_PIECE.get(), tag, context.structureTemplateManager(),
                (res) -> makeSettings(Rotation.valueOf(tag.getString("Rot"))));
        this.templatePath = ResourceLocation.parse(tag.getString("Template"));
        // 注意：反序列化时父类会自动从 tag 中读取包围盒，无需手动覆盖
    }

    // 提前将 AIR 处理器放入基础设置，保证所有操作一致
    private static StructurePlaceSettings makeSettings(Rotation rotation) {
        return new StructurePlaceSettings()
                .setRotation(rotation)
                .setIgnoreEntities(false)
                .addProcessor(BlockIgnoreProcessor.AIR); // 忽略 NBT 中的空气方块
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        StructureTemplateManager templateManager = level.getLevel().getServer().getStructureManager();
        Optional<StructureTemplate> optionalTemplate = templateManager.get(this.templatePath);

        if (optionalTemplate.isPresent()) {
            StructureTemplate template = optionalTemplate.get();
            if (template.getSize().getX() == 0) {
                System.err.println("【警告】模块尺寸为 0！路径: " + this.templatePath);
            } else {
                // 确保放置时使用的是和构造时一样的设置
                StructurePlaceSettings forcedSettings = new StructurePlaceSettings()
                        .setRotation(this.placeSettings.getRotation())
                        .setMirror(this.placeSettings.getMirror())
                        .setIgnoreEntities(false)
                        .addProcessor(BlockIgnoreProcessor.AIR)
                        // 【非常关键】box 是当前生成的区块与你零件包围盒的交集
                        // 传入它保证引擎不会跨区块瞎放方块
                        .setBoundingBox(box);

                // flag 2 减少方块更新卡顿和掉落物
                template.placeInWorld(level, this.templatePosition, this.templatePosition, forcedSettings, random, 2);
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

    public BlockPos getTemplateSize(StructureTemplateManager manager) {
        return manager.get(this.templatePath)
                .map(t -> new BlockPos(t.getSize().getX(), t.getSize().getY(), t.getSize().getZ()))
                .orElse(new BlockPos(0, 0, 0));
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, net.minecraft.world.level.ServerLevelAccessor level, RandomSource random, BoundingBox box) {}
}