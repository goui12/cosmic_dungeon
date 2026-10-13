package net.goui.cosmicdungeon.playerclass.d1;

import java.util.*;
import net.goui.cosmicdungeon.crafting.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.functions.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

/** Real loot-function semantics remain independent from player and machine production permission. */
public final class LootSmeltingGameTests {
    private LootSmeltingGameTests(){}
    private static LootContext context(ServerLevel level){
        return new LootContext.Builder(new LootParams.Builder(level).create(LootContextParamSets.EMPTY))
                .withOptionalRandomSeed(19).create(Optional.empty());
    }
    @SuppressWarnings("unchecked")
    private static RecipeHolder<SmeltingRecipe> nativeRecipe(ServerLevel level,SingleRecipeInput input){
        for(var holder:level.recipeAccess().getRecipes())
            if(holder.value() instanceof SmeltingRecipe recipe&&recipe.matches(input,level))
                return (RecipeHolder<SmeltingRecipe>)(RecipeHolder<?>)holder;
        throw new IllegalStateException("Fixture requires a real installed vanilla smelting recipe");
    }
    public static void loot(GameTestHelper helper){
        var previous=CraftingPolicy.snapshot();
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-201)){
            CraftingPolicy.install(CraftingRules.parse(true,CraftingRules.DEFAULT_PLAYERS,List.of()));
            var function=SmeltItemFunction.smelted().build();var context=context(f.level);
            var inputs=List.of(Items.POTATO,Items.RAW_IRON,Items.IRON_ORE,Items.RAW_GOLD,Items.RAW_COPPER);
            var outputs=List.of(Items.BAKED_POTATO,Items.IRON_INGOT,Items.IRON_INGOT,Items.GOLD_INGOT,Items.COPPER_INGOT);
            for(int i=0;i<inputs.size();i++){
                var input=new ItemStack(inputs.get(i),7+i);
                input.set(DataComponents.CUSTOM_NAME,Component.literal("Authored loot input"));
                CustomData.update(DataComponents.CUSTOM_DATA,input,t->t.putString("future_loot_field","unchanged"));
                var before=input.copy();var recipeInput=new SingleRecipeInput(input);
                var recipe=nativeRecipe(f.level,recipeInput);
                f.check(f.level.recipeAccess().getRecipeFor(RecipeType.SMELTING,recipeInput,f.level).isEmpty(),
                        "The same recipe is forbidden to ordinary machine lookup before loot conversion");
                var assembled=recipe.value().assemble(recipeInput,f.level.registryAccess());
                var expected=assembled.copyWithCount(input.getCount()*assembled.getCount());
                var result=function.apply(input,context);
                f.check(result.is(outputs.get(i))&&ItemStack.matches(expected,result),
                        "Native loot smelting uses the actual recipe output, count multiplier and output components");
                f.check(ItemStack.matches(before,input),"Native conversion leaves the original authored input unchanged");
                f.check(!LootSmeltingLookup.permits(recipeInput)
                        &&f.level.recipeAccess().getRecipeFor(RecipeType.SMELTING,recipeInput,f.level).isEmpty(),
                        "Loot lookup privilege ends before later production can query the recipe");
            }
            var skipped=new ItemStack(Items.POTATO,13);
            CustomData.update(DataComponents.CUSTOM_DATA,skipped,t->t.putString("condition_failed","keep"));
            var original=skipped.copy();
            var conditional=SmeltItemFunction.smelted().when(LootItemRandomChanceCondition.randomChance(0)).build();
            f.check(conditional.apply(skipped,context)==skipped&&ItemStack.matches(original,skipped),
                    "A failed native loot condition returns the identical untouched stack");
            var unsmeltable=new ItemStack(Items.DIAMOND,3);var unconverted=unsmeltable.copy();
            f.check(function.apply(unsmeltable,context)==unsmeltable&&ItemStack.matches(unconverted,unsmeltable),
                    "Missing recipes preserve vanilla warning/fallback without inventing a drop");
            f.check(function.apply(ItemStack.EMPTY,context).isEmpty(),"Empty native loot stays empty");
            var outer=new SingleRecipeInput(new ItemStack(Items.RAW_IRON));
            var unrelated=new SingleRecipeInput(new ItemStack(Items.RAW_IRON));
            LootSmeltingLookup.query(outer,()->{
                f.check(f.level.recipeAccess().getRecipeFor(RecipeType.SMELTING,outer,f.level).isPresent(),
                        "The explicitly scoped native loot input may resolve");
                f.check(f.level.recipeAccess().getRecipeFor(RecipeType.SMELTING,unrelated,f.level).isEmpty()
                        &&f.level.recipeAccess().getRecipeFor(RecipeType.BLASTING,outer,f.level).isEmpty(),
                        "Concurrent nested production inputs and other recipe types receive no exemption");
                return null;
            });
            helper.succeed();
        }finally{CraftingPolicy.install(previous);}
    }
    private static void tickFurnace(CosmicSpawnerGameTests.Fixture f,AbstractFurnaceBlockEntity furnace,int ticks){
        var pos=furnace.getBlockPos();
        for(int i=0;i<ticks;i++)AbstractFurnaceBlockEntity.serverTick(f.level,pos,f.level.getBlockState(pos),furnace);
    }
    public static void production(GameTestHelper helper){
        var previous=CraftingPolicy.snapshot();
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-202)){
            try{
            var denied=CraftingRules.parse(true,List.of("minecraft:bread|theurgist"),List.of());
            CraftingPolicy.install(denied);
            var loot=SmeltItemFunction.smelted().build();
            f.check(loot.apply(new ItemStack(Items.RAW_IRON,3),context(f.level)).is(Items.IRON_INGOT),
                    "Real loot smelting succeeds immediately before production checks");
            var pos=f.place(2,0,0,Blocks.FURNACE.defaultBlockState());
            var furnace=(AbstractFurnaceBlockEntity)f.level.getBlockEntity(pos);
            furnace.setItem(0,new ItemStack(Items.RAW_IRON,3));furnace.setItem(1,new ItemStack(Items.COAL,2));
            tickFurnace(f,furnace,220);
            f.check(furnace.getItem(0).getCount()==3&&furnace.getItem(1).getCount()==2&&furnace.getItem(2).isEmpty(),
                    "A forbidden furnace consumes no input or fuel and produces no loot after the exception closes");
            var iron=nativeRecipe(f.level,new SingleRecipeInput(new ItemStack(Items.RAW_IRON)));
            CraftingPolicy.install(CraftingRules.parse(true,List.of("minecraft:bread|theurgist"),List.of(iron.id().location().toString())));
            tickFurnace(f,furnace,20);
            f.check(furnace.getItem(1).getCount()==1,"Explicit automation permission starts native furnace fuel consumption");
            CraftingPolicy.install(denied);tickFurnace(f,furnace,220);
            f.check(furnace.getItem(0).getCount()==3&&furnace.getItem(2).isEmpty(),
                    "Revoking permission denies the furnace's cached recipe before completion");

            var crafterPos=f.place(-2,0,0,Blocks.CRAFTER.defaultBlockState());
            var crafter=(CrafterBlockEntity)f.level.getBlockEntity(crafterPos);
            for(int slot=0;slot<3;slot++)crafter.setItem(slot,new ItemStack(Items.WHEAT,2));
            var craftingInput=crafter.asCraftInput();
            f.check(f.level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,craftingInput,f.level).isPresent(),
                    "Player-approved bread remains discoverable without becoming automation-approved");
            CraftingPolicy.install(CraftingRules.parse(true,List.of("minecraft:bread|theurgist"),List.of("minecraft:bread")));
            f.check(CrafterBlock.getPotentialResults(f.level,craftingInput).isPresent(),"Explicit crafter permission can resolve the recipe");
            CraftingPolicy.install(denied);
            f.level.getBlockState(crafterPos).tick(f.level,crafterPos,f.level.getRandom());
            f.check(CrafterBlock.getPotentialResults(f.level,crafter.asCraftInput()).isEmpty()
                    &&java.util.stream.IntStream.range(0,3).allMatch(slot->crafter.getItem(slot).getCount()==2)
                    &&f.newDrops().isEmpty(),"Native crafter execution cannot consume or dispense a revoked recipe");

            var table=f.place(0,0,-2,Blocks.CRAFTING_TABLE.defaultBlockState());
            var menu=new CraftingMenu(19,f.player.getInventory(),ContainerLevelAccess.create(f.level,table));
            for(int slot=0;slot<3;slot++)menu.getInputGridSlots().get(slot).set(new ItemStack(Items.WHEAT,2));
            menu.slotsChanged(menu.getInputGridSlots().getFirst().container);
            f.check(menu.getResultSlot().getItem().isEmpty(),"Player result preview denies another class's approved recipe");
            menu.getResultSlot().set(new ItemStack(Items.BREAD)); // stale result after a policy/class change
            f.check(menu.quickMoveStack(f.player,0).isEmpty()&&menu.getResultSlot().getItem().isEmpty()
                    &&menu.getInputGridSlots().stream().limit(3).allMatch(slot->slot.getItem().getCount()==2),
                    "Native shift-take reauthorizes stale results before ingredient consumption or output movement");
            f.check(f.player.getInventory().countItem(Items.BREAD)==0,"Denied player production grants no output");
            menu.getInputGridSlots().forEach(slot->slot.set(ItemStack.EMPTY));
            helper.succeed();
            }finally{
                // Restoring a replaced inventory block must not eject fixture ingredients after cleanup.
                for(var block:f.blocks.keySet())
                    if(f.level.getBlockEntity(block) instanceof net.minecraft.world.Container container)container.clearContent();
            }
        }finally{CraftingPolicy.install(previous);}
    }
}
