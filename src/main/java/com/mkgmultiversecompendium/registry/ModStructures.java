package com.mkgmultiversecompendium.registry;

import com.mkgmultiversecompendium.MKGMultiverseCompendium;
import com.mkgmultiversecompendium.worldgen.structure.CyberCityStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MKGMultiverseCompendium.MODID);

    // 注意：注册时传入的是 CyberCityStructure.CODEC
    public static final DeferredHolder<StructureType<?>, StructureType<CyberCityStructure>> CYBER_CITY =
            STRUCTURES.register("cyber_city", () -> () -> CyberCityStructure.CODEC);
}