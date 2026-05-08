package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Optional;
import net.minecraft.ResourceLocationException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class PlaceCommand {
    private static final SimpleCommandExceptionType ERROR_FEATURE_FAILED = new SimpleCommandExceptionType(
        Component.translatable("commands.place.feature.failed")
    );
    private static final SimpleCommandExceptionType ERROR_JIGSAW_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.place.jigsaw.failed"));
    private static final SimpleCommandExceptionType ERROR_STRUCTURE_FAILED = new SimpleCommandExceptionType(
        Component.translatable("commands.place.structure.failed")
    );
    private static final DynamicCommandExceptionType ERROR_TEMPLATE_INVALID = new DynamicCommandExceptionType(
        p_304274_ -> Component.translatableEscape("commands.place.template.invalid", p_304274_)
    );
    private static final SimpleCommandExceptionType ERROR_TEMPLATE_FAILED = new SimpleCommandExceptionType(
        Component.translatable("commands.place.template.failed")
    );
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEMPLATES = (p_214552_, p_214553_) -> {
        StructureTemplateManager structuretemplatemanager = p_214552_.getSource().getLevel().getStructureManager();
        return SharedSuggestionProvider.suggestResource(structuretemplatemanager.listTemplates(), p_214553_);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("place")
                .requires(Commands.hasPermission(2))
                .then(
                    Commands.literal("feature")
                        .then(
                            Commands.argument("feature", ResourceKeyArgument.key(Registries.CONFIGURED_FEATURE))
                                .executes(
                                    p_274824_ -> placeFeature(
                                        p_274824_.getSource(),
                                        ResourceKeyArgument.getConfiguredFeature(p_274824_, "feature"),
                                        BlockPos.containing(p_274824_.getSource().getPosition())
                                    )
                                )
                                .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(
                                            p_248163_ -> placeFeature(
                                                p_248163_.getSource(),
                                                ResourceKeyArgument.getConfiguredFeature(p_248163_, "feature"),
                                                BlockPosArgument.getLoadedBlockPos(p_248163_, "pos")
                                            )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("jigsaw")
                        .then(
                            Commands.argument("pool", ResourceKeyArgument.key(Registries.TEMPLATE_POOL))
                                .then(
                                    Commands.argument("target", ResourceLocationArgument.id())
                                        .then(
                                            Commands.argument("max_depth", IntegerArgumentType.integer(1, 20))
                                                .executes(
                                                    p_274825_ -> placeJigsaw(
                                                        p_274825_.getSource(),
                                                        ResourceKeyArgument.getStructureTemplatePool(p_274825_, "pool"),
                                                        ResourceLocationArgument.getId(p_274825_, "target"),
                                                        IntegerArgumentType.getInteger(p_274825_, "max_depth"),
                                                        BlockPos.containing(p_274825_.getSource().getPosition())
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("position", BlockPosArgument.blockPos())
                                                        .executes(
                                                            p_248167_ -> placeJigsaw(
                                                                p_248167_.getSource(),
                                                                ResourceKeyArgument.getStructureTemplatePool(p_248167_, "pool"),
                                                                ResourceLocationArgument.getId(p_248167_, "target"),
                                                                IntegerArgumentType.getInteger(p_248167_, "max_depth"),
                                                                BlockPosArgument.getLoadedBlockPos(p_248167_, "position")
                                                            )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("structure")
                        .then(
                            Commands.argument("structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                                .executes(
                                    p_274826_ -> placeStructure(
                                        p_274826_.getSource(),
                                        ResourceKeyArgument.getStructure(p_274826_, "structure"),
                                        BlockPos.containing(p_274826_.getSource().getPosition())
                                    )
                                )
                                .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(
                                            p_248168_ -> placeStructure(
                                                p_248168_.getSource(),
                                                ResourceKeyArgument.getStructure(p_248168_, "structure"),
                                                BlockPosArgument.getLoadedBlockPos(p_248168_, "pos")
                                            )
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("template")
                        .then(
                            Commands.argument("template", ResourceLocationArgument.id())
                                .suggests(SUGGEST_TEMPLATES)
                                .executes(
                                    p_392744_ -> placeTemplate(
                                        p_392744_.getSource(),
                                        ResourceLocationArgument.getId(p_392744_, "template"),
                                        BlockPos.containing(p_392744_.getSource().getPosition()),
                                        Rotation.NONE,
                                        Mirror.NONE,
                                        1.0F,
                                        0,
                                        false
                                    )
                                )
                                .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(
                                            p_392741_ -> placeTemplate(
                                                p_392741_.getSource(),
                                                ResourceLocationArgument.getId(p_392741_, "template"),
                                                BlockPosArgument.getLoadedBlockPos(p_392741_, "pos"),
                                                Rotation.NONE,
                                                Mirror.NONE,
                                                1.0F,
                                                0,
                                                false
                                            )
                                        )
                                        .then(
                                            Commands.argument("rotation", TemplateRotationArgument.templateRotation())
                                                .executes(
                                                    p_392738_ -> placeTemplate(
                                                        p_392738_.getSource(),
                                                        ResourceLocationArgument.getId(p_392738_, "template"),
                                                        BlockPosArgument.getLoadedBlockPos(p_392738_, "pos"),
                                                        TemplateRotationArgument.getRotation(p_392738_, "rotation"),
                                                        Mirror.NONE,
                                                        1.0F,
                                                        0,
                                                        false
                                                    )
                                                )
                                                .then(
                                                    Commands.argument("mirror", TemplateMirrorArgument.templateMirror())
                                                        .executes(
                                                            p_392740_ -> placeTemplate(
                                                                p_392740_.getSource(),
                                                                ResourceLocationArgument.getId(p_392740_, "template"),
                                                                BlockPosArgument.getLoadedBlockPos(p_392740_, "pos"),
                                                                TemplateRotationArgument.getRotation(p_392740_, "rotation"),
                                                                TemplateMirrorArgument.getMirror(p_392740_, "mirror"),
                                                                1.0F,
                                                                0,
                                                                false
                                                            )
                                                        )
                                                        .then(
                                                            Commands.argument("integrity", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                                .executes(
                                                                    p_392742_ -> placeTemplate(
                                                                        p_392742_.getSource(),
                                                                        ResourceLocationArgument.getId(p_392742_, "template"),
                                                                        BlockPosArgument.getLoadedBlockPos(p_392742_, "pos"),
                                                                        TemplateRotationArgument.getRotation(p_392742_, "rotation"),
                                                                        TemplateMirrorArgument.getMirror(p_392742_, "mirror"),
                                                                        FloatArgumentType.getFloat(p_392742_, "integrity"),
                                                                        0,
                                                                        false
                                                                    )
                                                                )
                                                                .then(
                                                                    Commands.argument("seed", IntegerArgumentType.integer())
                                                                        .executes(
                                                                            p_392739_ -> placeTemplate(
                                                                                p_392739_.getSource(),
                                                                                ResourceLocationArgument.getId(p_392739_, "template"),
                                                                                BlockPosArgument.getLoadedBlockPos(p_392739_, "pos"),
                                                                                TemplateRotationArgument.getRotation(p_392739_, "rotation"),
                                                                                TemplateMirrorArgument.getMirror(p_392739_, "mirror"),
                                                                                FloatArgumentType.getFloat(p_392739_, "integrity"),
                                                                                IntegerArgumentType.getInteger(p_392739_, "seed"),
                                                                                false
                                                                            )
                                                                        )
                                                                        .then(
                                                                            Commands.literal("strict")
                                                                                .executes(
                                                                                    p_392743_ -> placeTemplate(
                                                                                        p_392743_.getSource(),
                                                                                        ResourceLocationArgument.getId(p_392743_, "template"),
                                                                                        BlockPosArgument.getLoadedBlockPos(p_392743_, "pos"),
                                                                                        TemplateRotationArgument.getRotation(p_392743_, "rotation"),
                                                                                        TemplateMirrorArgument.getMirror(p_392743_, "mirror"),
                                                                                        FloatArgumentType.getFloat(p_392743_, "integrity"),
                                                                                        IntegerArgumentType.getInteger(p_392743_, "seed"),
                                                                                        true
                                                                                    )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    public static int placeFeature(CommandSourceStack source, Holder.Reference<ConfiguredFeature<?, ?>> feature, BlockPos pos) throws CommandSyntaxException {
        ServerLevel serverlevel = source.getLevel();
        ConfiguredFeature<?, ?> configuredfeature = feature.value();
        ChunkPos chunkpos = new ChunkPos(pos);
        checkLoaded(serverlevel, new ChunkPos(chunkpos.x - 1, chunkpos.z - 1), new ChunkPos(chunkpos.x + 1, chunkpos.z + 1));
        if (!configuredfeature.place(serverlevel, serverlevel.getChunkSource().getGenerator(), serverlevel.getRandom(), pos)) {
            throw ERROR_FEATURE_FAILED.create();
        } else {
            String s = feature.key().location().toString();
            source.sendSuccess(() -> Component.translatable("commands.place.feature.success", s, pos.getX(), pos.getY(), pos.getZ()), true);
            return 1;
        }
    }

    public static int placeJigsaw(
        CommandSourceStack source, Holder<StructureTemplatePool> templatePool, ResourceLocation target, int maxDepth, BlockPos pos
    ) throws CommandSyntaxException {
        ServerLevel serverlevel = source.getLevel();
        ChunkPos chunkpos = new ChunkPos(pos);
        checkLoaded(serverlevel, chunkpos, chunkpos);
        if (!JigsawPlacement.generateJigsaw(serverlevel, templatePool, target, maxDepth, pos, false)) {
            throw ERROR_JIGSAW_FAILED.create();
        } else {
            source.sendSuccess(() -> Component.translatable("commands.place.jigsaw.success", pos.getX(), pos.getY(), pos.getZ()), true);
            return 1;
        }
    }

    public static int placeStructure(CommandSourceStack source, Holder.Reference<Structure> p_structure, BlockPos pos) throws CommandSyntaxException {
        ServerLevel serverlevel = source.getLevel();
        Structure structure = p_structure.value();
        ChunkGenerator chunkgenerator = serverlevel.getChunkSource().getGenerator();
        StructureStart structurestart = structure.generate(
            p_structure,
            serverlevel.dimension(),
            source.registryAccess(),
            chunkgenerator,
            chunkgenerator.getBiomeSource(),
            serverlevel.getChunkSource().randomState(),
            serverlevel.getStructureManager(),
            serverlevel.getSeed(),
            new ChunkPos(pos),
            0,
            serverlevel,
            p_214580_ -> true
        );
        if (!structurestart.isValid()) {
            throw ERROR_STRUCTURE_FAILED.create();
        } else {
            BoundingBox boundingbox = structurestart.getBoundingBox();
            ChunkPos chunkpos = new ChunkPos(SectionPos.blockToSectionCoord(boundingbox.minX()), SectionPos.blockToSectionCoord(boundingbox.minZ()));
            ChunkPos chunkpos1 = new ChunkPos(SectionPos.blockToSectionCoord(boundingbox.maxX()), SectionPos.blockToSectionCoord(boundingbox.maxZ()));
            checkLoaded(serverlevel, chunkpos, chunkpos1);
            ChunkPos.rangeClosed(chunkpos, chunkpos1)
                .forEach(
                    p_451723_ -> structurestart.placeInChunk(
                        serverlevel,
                        serverlevel.structureManager(),
                        chunkgenerator,
                        serverlevel.getRandom(),
                        new BoundingBox(
                            p_451723_.getMinBlockX(),
                            serverlevel.getMinY(),
                            p_451723_.getMinBlockZ(),
                            p_451723_.getMaxBlockX(),
                            serverlevel.getMaxY() + 1,
                            p_451723_.getMaxBlockZ()
                        ),
                        p_451723_
                    )
                );
            String s = p_structure.key().location().toString();
            source.sendSuccess(
                () -> Component.translatable("commands.place.structure.success", s, pos.getX(), pos.getY(), pos.getZ()), true
            );
            return 1;
        }
    }

    public static int placeTemplate(
        CommandSourceStack source,
        ResourceLocation template,
        BlockPos pos,
        Rotation rotation,
        Mirror mirror,
        float integrity,
        int seed,
        boolean strict
    ) throws CommandSyntaxException {
        ServerLevel serverlevel = source.getLevel();
        StructureTemplateManager structuretemplatemanager = serverlevel.getStructureManager();

        Optional<StructureTemplate> optional;
        try {
            optional = structuretemplatemanager.get(template);
        } catch (ResourceLocationException resourcelocationexception) {
            throw ERROR_TEMPLATE_INVALID.create(template);
        }

        if (optional.isEmpty()) {
            throw ERROR_TEMPLATE_INVALID.create(template);
        } else {
            StructureTemplate structuretemplate = optional.get();
            checkLoaded(serverlevel, new ChunkPos(pos), new ChunkPos(pos.offset(structuretemplate.getSize())));
            StructurePlaceSettings structureplacesettings = new StructurePlaceSettings().setMirror(mirror).setRotation(rotation).setKnownShape(strict);
            if (integrity < 1.0F) {
                structureplacesettings.clearProcessors().addProcessor(new BlockRotProcessor(integrity)).setRandom(StructureBlockEntity.createRandom(seed));
            }

            boolean flag = structuretemplate.placeInWorld(
                serverlevel, pos, pos, structureplacesettings, StructureBlockEntity.createRandom(seed), 2 | (strict ? 816 : 0)
            );
            if (!flag) {
                throw ERROR_TEMPLATE_FAILED.create();
            } else {
                source.sendSuccess(
                    () -> Component.translatable(
                        "commands.place.template.success", Component.translationArg(template), pos.getX(), pos.getY(), pos.getZ()
                    ),
                    true
                );
                return 1;
            }
        }
    }

    private static void checkLoaded(ServerLevel level, ChunkPos start, ChunkPos end) throws CommandSyntaxException {
        if (ChunkPos.rangeClosed(start, end).filter(p_313494_ -> !level.isLoaded(p_313494_.getWorldPosition())).findAny().isPresent()) {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
        }
    }
}
