package com.mkgmultiversecompendium.registry;

import com.mkgmultiversecompendium.MKGMultiverseCompendium;
import com.mkgmultiversecompendium.worldgen.structure.CyberpunkPiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModStructurePieces {
    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MKGMultiverseCompendium.MODID);

    // 注册自定义 Piece 逻辑类，序列化标识符为 "cyber_piece"
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CYBER_PIECE =
            PIECES.register("cyber_piece", () -> CyberpunkPiece::new);
}