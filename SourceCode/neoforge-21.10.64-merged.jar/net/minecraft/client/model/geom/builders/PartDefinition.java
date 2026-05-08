package net.minecraft.client.model.geom.builders;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PartDefinition {
    private final List<CubeDefinition> cubes;
    private final PartPose partPose;
    private final Map<String, PartDefinition> children = Maps.newHashMap();

    PartDefinition(List<CubeDefinition> cubes, PartPose partPose) {
        this.cubes = cubes;
        this.partPose = partPose;
    }

    public PartDefinition addOrReplaceChild(String name, CubeListBuilder cubes, PartPose partPose) {
        PartDefinition partdefinition = new PartDefinition(cubes.getCubes(), partPose);
        return this.addOrReplaceChild(name, partdefinition);
    }

    public PartDefinition addOrReplaceChild(String name, PartDefinition child) {
        PartDefinition partdefinition = this.children.put(name, child);
        if (partdefinition != null) {
            child.children.putAll(partdefinition.children);
        }

        return child;
    }

    public PartDefinition clearRecursively() {
        for (String s : this.children.keySet()) {
            this.clearChild(s).clearRecursively();
        }

        return this;
    }

    public PartDefinition clearChild(String name) {
        PartDefinition partdefinition = this.children.get(name);
        if (partdefinition == null) {
            throw new IllegalArgumentException("No child with name: " + name);
        } else {
            return this.addOrReplaceChild(name, CubeListBuilder.create(), partdefinition.partPose);
        }
    }

    public void retainPartsAndChildren(Set<String> parts) {
        for (Entry<String, PartDefinition> entry : this.children.entrySet()) {
            PartDefinition partdefinition = entry.getValue();
            if (!parts.contains(entry.getKey())) {
                this.addOrReplaceChild(entry.getKey(), CubeListBuilder.create(), partdefinition.partPose).retainPartsAndChildren(parts);
            }
        }
    }

    public void retainExactParts(Set<String> parts) {
        for (Entry<String, PartDefinition> entry : this.children.entrySet()) {
            PartDefinition partdefinition = entry.getValue();
            if (parts.contains(entry.getKey())) {
                partdefinition.clearRecursively();
            } else {
                this.addOrReplaceChild(entry.getKey(), CubeListBuilder.create(), partdefinition.partPose).retainExactParts(parts);
            }
        }
    }

    public ModelPart bake(int texWidth, int texHeight) {
        Object2ObjectArrayMap<String, ModelPart> object2objectarraymap = this.children
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    p_171593_ -> ((PartDefinition)p_171593_.getValue()).bake(texWidth, texHeight),
                    (p_171595_, p_171596_) -> p_171595_,
                    Object2ObjectArrayMap::new
                )
            );
        List<ModelPart.Cube> list = this.cubes.stream().map(p_171589_ -> p_171589_.bake(texWidth, texHeight)).toList();
        ModelPart modelpart = new ModelPart(list, object2objectarraymap);
        modelpart.setInitialPose(this.partPose);
        modelpart.loadPose(this.partPose);
        return modelpart;
    }

    public PartDefinition getChild(String name) {
        return this.children.get(name);
    }

    public Set<Entry<String, PartDefinition>> getChildren() {
        return this.children.entrySet();
    }

    public PartDefinition transformed(UnaryOperator<PartPose> transformer) {
        PartDefinition partdefinition = new PartDefinition(this.cubes, transformer.apply(this.partPose));
        partdefinition.children.putAll(this.children);
        return partdefinition;
    }
}
