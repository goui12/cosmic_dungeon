package net.minecraft.client.data.models.model;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextureMapping {
    private final Map<TextureSlot, ResourceLocation> slots = Maps.newHashMap();
    private final Set<TextureSlot> forcedSlots = Sets.newHashSet();

    public TextureMapping put(TextureSlot slot, ResourceLocation texture) {
        this.slots.put(slot, texture);
        return this;
    }

    public TextureMapping putForced(TextureSlot slot, ResourceLocation texture) {
        this.slots.put(slot, texture);
        this.forcedSlots.add(slot);
        return this;
    }

    public Stream<TextureSlot> getForced() {
        return this.forcedSlots.stream();
    }

    public TextureMapping copySlot(TextureSlot source, TextureSlot destination) {
        this.slots.put(destination, this.slots.get(source));
        return this;
    }

    public TextureMapping copyForced(TextureSlot source, TextureSlot destination) {
        this.slots.put(destination, this.slots.get(source));
        this.forcedSlots.add(destination);
        return this;
    }

    public ResourceLocation get(TextureSlot slot) {
        for (TextureSlot textureslot = slot; textureslot != null; textureslot = textureslot.getParent()) {
            ResourceLocation resourcelocation = this.slots.get(textureslot);
            if (resourcelocation != null) {
                return resourcelocation;
            }
        }

        throw new IllegalStateException("Can't find texture for slot " + slot);
    }

    public TextureMapping copyAndUpdate(TextureSlot slot, ResourceLocation texture) {
        TextureMapping texturemapping = new TextureMapping();
        texturemapping.slots.putAll(this.slots);
        texturemapping.forcedSlots.addAll(this.forcedSlots);
        texturemapping.put(slot, texture);
        return texturemapping;
    }

    public static TextureMapping cube(Block block) {
        ResourceLocation resourcelocation = getBlockTexture(block);
        return cube(resourcelocation);
    }

    public static TextureMapping defaultTexture(Block block) {
        ResourceLocation resourcelocation = getBlockTexture(block);
        return defaultTexture(resourcelocation);
    }

    public static TextureMapping defaultTexture(ResourceLocation texture) {
        return new TextureMapping().put(TextureSlot.TEXTURE, texture);
    }

    public static TextureMapping cube(ResourceLocation texture) {
        return new TextureMapping().put(TextureSlot.ALL, texture);
    }

    public static TextureMapping cross(Block block) {
        return singleSlot(TextureSlot.CROSS, getBlockTexture(block));
    }

    public static TextureMapping side(Block block) {
        return singleSlot(TextureSlot.SIDE, getBlockTexture(block));
    }

    public static TextureMapping crossEmissive(Block block) {
        return new TextureMapping().put(TextureSlot.CROSS, getBlockTexture(block)).put(TextureSlot.CROSS_EMISSIVE, getBlockTexture(block, "_emissive"));
    }

    public static TextureMapping cross(ResourceLocation texture) {
        return singleSlot(TextureSlot.CROSS, texture);
    }

    public static TextureMapping plant(Block block) {
        return singleSlot(TextureSlot.PLANT, getBlockTexture(block));
    }

    public static TextureMapping plantEmissive(Block block) {
        return new TextureMapping().put(TextureSlot.PLANT, getBlockTexture(block)).put(TextureSlot.CROSS_EMISSIVE, getBlockTexture(block, "_emissive"));
    }

    public static TextureMapping plant(ResourceLocation texture) {
        return singleSlot(TextureSlot.PLANT, texture);
    }

    public static TextureMapping rail(Block block) {
        return singleSlot(TextureSlot.RAIL, getBlockTexture(block));
    }

    public static TextureMapping rail(ResourceLocation texture) {
        return singleSlot(TextureSlot.RAIL, texture);
    }

    public static TextureMapping wool(Block block) {
        return singleSlot(TextureSlot.WOOL, getBlockTexture(block));
    }

    public static TextureMapping flowerbed(Block block) {
        return new TextureMapping().put(TextureSlot.FLOWERBED, getBlockTexture(block)).put(TextureSlot.STEM, getBlockTexture(block, "_stem"));
    }

    public static TextureMapping wool(ResourceLocation texture) {
        return singleSlot(TextureSlot.WOOL, texture);
    }

    public static TextureMapping stem(Block block) {
        return singleSlot(TextureSlot.STEM, getBlockTexture(block));
    }

    public static TextureMapping attachedStem(Block stemBlock, Block upperStemBlock) {
        return new TextureMapping().put(TextureSlot.STEM, getBlockTexture(stemBlock)).put(TextureSlot.UPPER_STEM, getBlockTexture(upperStemBlock));
    }

    public static TextureMapping pattern(Block block) {
        return singleSlot(TextureSlot.PATTERN, getBlockTexture(block));
    }

    public static TextureMapping fan(Block block) {
        return singleSlot(TextureSlot.FAN, getBlockTexture(block));
    }

    public static TextureMapping crop(ResourceLocation block) {
        return singleSlot(TextureSlot.CROP, block);
    }

    public static TextureMapping pane(Block block, Block edgeBlock) {
        return new TextureMapping().put(TextureSlot.PANE, getBlockTexture(block)).put(TextureSlot.EDGE, getBlockTexture(edgeBlock, "_top"));
    }

    public static TextureMapping singleSlot(TextureSlot slot, ResourceLocation texture) {
        return new TextureMapping().put(slot, texture);
    }

    public static TextureMapping column(Block block) {
        return new TextureMapping().put(TextureSlot.SIDE, getBlockTexture(block, "_side")).put(TextureSlot.END, getBlockTexture(block, "_top"));
    }

    public static TextureMapping cubeTop(Block block) {
        return new TextureMapping().put(TextureSlot.SIDE, getBlockTexture(block, "_side")).put(TextureSlot.TOP, getBlockTexture(block, "_top"));
    }

    public static TextureMapping pottedAzalea(Block block) {
        return new TextureMapping()
            .put(TextureSlot.PLANT, getBlockTexture(block, "_plant"))
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(block, "_top"));
    }

    public static TextureMapping logColumn(Block block) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block))
            .put(TextureSlot.END, getBlockTexture(block, "_top"))
            .put(TextureSlot.PARTICLE, getBlockTexture(block));
    }

    public static TextureMapping column(ResourceLocation side, ResourceLocation end) {
        return new TextureMapping().put(TextureSlot.SIDE, side).put(TextureSlot.END, end);
    }

    public static TextureMapping fence(Block block) {
        return new TextureMapping()
            .put(TextureSlot.TEXTURE, getBlockTexture(block))
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(block, "_top"));
    }

    public static TextureMapping customParticle(Block block) {
        return new TextureMapping().put(TextureSlot.TEXTURE, getBlockTexture(block)).put(TextureSlot.PARTICLE, getBlockTexture(block, "_particle"));
    }

    public static TextureMapping cubeBottomTop(Block block) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(block, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping cubeBottomTopWithWall(Block block) {
        ResourceLocation resourcelocation = getBlockTexture(block);
        return new TextureMapping()
            .put(TextureSlot.WALL, resourcelocation)
            .put(TextureSlot.SIDE, resourcelocation)
            .put(TextureSlot.TOP, getBlockTexture(block, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping columnWithWall(Block block) {
        ResourceLocation resourcelocation = getBlockTexture(block);
        return new TextureMapping()
            .put(TextureSlot.TEXTURE, resourcelocation)
            .put(TextureSlot.WALL, resourcelocation)
            .put(TextureSlot.SIDE, resourcelocation)
            .put(TextureSlot.END, getBlockTexture(block, "_top"));
    }

    public static TextureMapping door(ResourceLocation top, ResourceLocation bottom) {
        return new TextureMapping().put(TextureSlot.TOP, top).put(TextureSlot.BOTTOM, bottom);
    }

    public static TextureMapping door(Block block) {
        return new TextureMapping().put(TextureSlot.TOP, getBlockTexture(block, "_top")).put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping particle(Block block) {
        return new TextureMapping().put(TextureSlot.PARTICLE, getBlockTexture(block));
    }

    public static TextureMapping particle(ResourceLocation texture) {
        return new TextureMapping().put(TextureSlot.PARTICLE, texture);
    }

    public static TextureMapping fire0(Block block) {
        return new TextureMapping().put(TextureSlot.FIRE, getBlockTexture(block, "_0"));
    }

    public static TextureMapping fire1(Block block) {
        return new TextureMapping().put(TextureSlot.FIRE, getBlockTexture(block, "_1"));
    }

    public static TextureMapping lantern(Block block) {
        return new TextureMapping().put(TextureSlot.LANTERN, getBlockTexture(block));
    }

    public static TextureMapping torch(Block block) {
        return new TextureMapping().put(TextureSlot.TORCH, getBlockTexture(block));
    }

    public static TextureMapping torch(ResourceLocation texture) {
        return new TextureMapping().put(TextureSlot.TORCH, texture);
    }

    public static TextureMapping trialSpawner(Block block, String sideSuffix, String topSuffix) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block, sideSuffix))
            .put(TextureSlot.TOP, getBlockTexture(block, topSuffix))
            .put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping vault(Block block, String frontSuffix, String sideSuffix, String topSuffix, String bottomSuffix) {
        return new TextureMapping()
            .put(TextureSlot.FRONT, getBlockTexture(block, frontSuffix))
            .put(TextureSlot.SIDE, getBlockTexture(block, sideSuffix))
            .put(TextureSlot.TOP, getBlockTexture(block, topSuffix))
            .put(TextureSlot.BOTTOM, getBlockTexture(block, bottomSuffix));
    }

    public static TextureMapping particleFromItem(Item item) {
        return new TextureMapping().put(TextureSlot.PARTICLE, getItemTexture(item));
    }

    public static TextureMapping commandBlock(Block block) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.FRONT, getBlockTexture(block, "_front"))
            .put(TextureSlot.BACK, getBlockTexture(block, "_back"));
    }

    public static TextureMapping orientableCube(Block block) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.FRONT, getBlockTexture(block, "_front"))
            .put(TextureSlot.TOP, getBlockTexture(block, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping orientableCubeOnlyTop(Block block) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.FRONT, getBlockTexture(block, "_front"))
            .put(TextureSlot.TOP, getBlockTexture(block, "_top"));
    }

    public static TextureMapping orientableCubeSameEnds(Block block) {
        return new TextureMapping()
            .put(TextureSlot.SIDE, getBlockTexture(block, "_side"))
            .put(TextureSlot.FRONT, getBlockTexture(block, "_front"))
            .put(TextureSlot.END, getBlockTexture(block, "_end"));
    }

    public static TextureMapping top(Block block) {
        return new TextureMapping().put(TextureSlot.TOP, getBlockTexture(block, "_top"));
    }

    public static TextureMapping craftingTable(Block block, Block bottom) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(block, "_front"))
            .put(TextureSlot.DOWN, getBlockTexture(bottom))
            .put(TextureSlot.UP, getBlockTexture(block, "_top"))
            .put(TextureSlot.NORTH, getBlockTexture(block, "_front"))
            .put(TextureSlot.EAST, getBlockTexture(block, "_side"))
            .put(TextureSlot.SOUTH, getBlockTexture(block, "_side"))
            .put(TextureSlot.WEST, getBlockTexture(block, "_front"));
    }

    public static TextureMapping fletchingTable(Block block, Block bottom) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(block, "_front"))
            .put(TextureSlot.DOWN, getBlockTexture(bottom))
            .put(TextureSlot.UP, getBlockTexture(block, "_top"))
            .put(TextureSlot.NORTH, getBlockTexture(block, "_front"))
            .put(TextureSlot.SOUTH, getBlockTexture(block, "_front"))
            .put(TextureSlot.EAST, getBlockTexture(block, "_side"))
            .put(TextureSlot.WEST, getBlockTexture(block, "_side"));
    }

    public static TextureMapping snifferEgg(String name) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.SNIFFER_EGG, name + "_north"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.SNIFFER_EGG, name + "_bottom"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.SNIFFER_EGG, name + "_top"))
            .put(TextureSlot.NORTH, getBlockTexture(Blocks.SNIFFER_EGG, name + "_north"))
            .put(TextureSlot.SOUTH, getBlockTexture(Blocks.SNIFFER_EGG, name + "_south"))
            .put(TextureSlot.EAST, getBlockTexture(Blocks.SNIFFER_EGG, name + "_east"))
            .put(TextureSlot.WEST, getBlockTexture(Blocks.SNIFFER_EGG, name + "_west"));
    }

    public static TextureMapping driedGhast(String name) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.DRIED_GHAST, name + "_north"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.DRIED_GHAST, name + "_bottom"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.DRIED_GHAST, name + "_top"))
            .put(TextureSlot.NORTH, getBlockTexture(Blocks.DRIED_GHAST, name + "_north"))
            .put(TextureSlot.SOUTH, getBlockTexture(Blocks.DRIED_GHAST, name + "_south"))
            .put(TextureSlot.EAST, getBlockTexture(Blocks.DRIED_GHAST, name + "_east"))
            .put(TextureSlot.WEST, getBlockTexture(Blocks.DRIED_GHAST, name + "_west"))
            .put(TextureSlot.TENTACLES, getBlockTexture(Blocks.DRIED_GHAST, name + "_tentacles"));
    }

    public static TextureMapping campfire(Block block) {
        return new TextureMapping().put(TextureSlot.LIT_LOG, getBlockTexture(block, "_log_lit")).put(TextureSlot.FIRE, getBlockTexture(block, "_fire"));
    }

    public static TextureMapping candleCake(Block block, boolean lit) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.CAKE, "_side"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.CAKE, "_bottom"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.CAKE, "_top"))
            .put(TextureSlot.SIDE, getBlockTexture(Blocks.CAKE, "_side"))
            .put(TextureSlot.CANDLE, getBlockTexture(block, lit ? "_lit" : ""));
    }

    public static TextureMapping cauldron(ResourceLocation texture) {
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.CAULDRON, "_side"))
            .put(TextureSlot.SIDE, getBlockTexture(Blocks.CAULDRON, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.CAULDRON, "_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.CAULDRON, "_bottom"))
            .put(TextureSlot.INSIDE, getBlockTexture(Blocks.CAULDRON, "_inner"))
            .put(TextureSlot.CONTENT, texture);
    }

    public static TextureMapping sculkShrieker(boolean canSummon) {
        String s = canSummon ? "_can_summon" : "";
        return new TextureMapping()
            .put(TextureSlot.PARTICLE, getBlockTexture(Blocks.SCULK_SHRIEKER, "_bottom"))
            .put(TextureSlot.SIDE, getBlockTexture(Blocks.SCULK_SHRIEKER, "_side"))
            .put(TextureSlot.TOP, getBlockTexture(Blocks.SCULK_SHRIEKER, "_top"))
            .put(TextureSlot.INNER_TOP, getBlockTexture(Blocks.SCULK_SHRIEKER, s + "_inner_top"))
            .put(TextureSlot.BOTTOM, getBlockTexture(Blocks.SCULK_SHRIEKER, "_bottom"));
    }

    public static TextureMapping bars(Block block) {
        return new TextureMapping().put(TextureSlot.BARS, getBlockTexture(block)).put(TextureSlot.EDGE, getBlockTexture(block));
    }

    public static TextureMapping layer0(Item item) {
        return new TextureMapping().put(TextureSlot.LAYER0, getItemTexture(item));
    }

    public static TextureMapping layer0(Block block) {
        return new TextureMapping().put(TextureSlot.LAYER0, getBlockTexture(block));
    }

    public static TextureMapping layer0(ResourceLocation texture) {
        return new TextureMapping().put(TextureSlot.LAYER0, texture);
    }

    public static TextureMapping layered(ResourceLocation layer0, ResourceLocation layer1) {
        return new TextureMapping().put(TextureSlot.LAYER0, layer0).put(TextureSlot.LAYER1, layer1);
    }

    public static TextureMapping layered(ResourceLocation layer0, ResourceLocation layer1, ResourceLocation layer2) {
        return new TextureMapping().put(TextureSlot.LAYER0, layer0).put(TextureSlot.LAYER1, layer1).put(TextureSlot.LAYER2, layer2);
    }

    public static ResourceLocation getBlockTexture(Block block) {
        ResourceLocation resourcelocation = BuiltInRegistries.BLOCK.getKey(block);
        return resourcelocation.withPrefix("block/");
    }

    public static ResourceLocation getBlockTexture(Block block, String suffix) {
        ResourceLocation resourcelocation = BuiltInRegistries.BLOCK.getKey(block);
        return resourcelocation.withPath(p_388162_ -> "block/" + p_388162_ + suffix);
    }

    public static ResourceLocation getItemTexture(Item item) {
        ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(item);
        return resourcelocation.withPrefix("item/");
    }

    public static ResourceLocation getItemTexture(Item item, String suffix) {
        ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(item);
        return resourcelocation.withPath(p_387396_ -> "item/" + p_387396_ + suffix);
    }

    // Neo: Added to allow easier texture map copying
    public TextureMapping copy() {
        TextureMapping texturemapping = new TextureMapping();
        texturemapping.slots.putAll(this.slots);
        texturemapping.forcedSlots.addAll(this.forcedSlots);
        return texturemapping;
    }
}
