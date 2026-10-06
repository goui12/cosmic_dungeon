package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Lifecycle;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.wolf.Wolf;

/** Native wolf data/behavior with real vanilla variant registries, without starting a world. */
final class MercenaryTestWolf extends Wolf {
    private static final RegistryAccess REGISTRIES=registries();
    private static RegistryAccess registries(){
        var vanilla=VanillaRegistries.createLookup();
        return new RegistryAccess.ImmutableRegistryAccess(List.of(
                copy(vanilla,Registries.WOLF_VARIANT),copy(vanilla,Registries.WOLF_SOUND_VARIANT)));
    }
    private static <T> Registry<T> copy(HolderLookup.Provider source,ResourceKey<? extends Registry<T>> key){
        var target=new MappedRegistry<T>(key,Lifecycle.stable());
        source.lookupOrThrow(key).listElements().forEach(entry->Registry.register(target,entry.key(),entry.value()));
        return target.freeze();
    }
    MercenaryTestWolf(){super(EntityType.WOLF,null);}
    int ageForTest;
    @Override public int getAge(){return ageForTest;} // Server age without a world/client-side lookup.
    @Override public RegistryAccess registryAccess(){return REGISTRIES;}
}
