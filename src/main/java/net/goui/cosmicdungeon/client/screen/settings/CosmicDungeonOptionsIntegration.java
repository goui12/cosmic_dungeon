package net.goui.cosmicdungeon.client.screen.settings;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.function.Supplier;

public final class CosmicDungeonOptionsIntegration {
    private CosmicDungeonOptionsIntegration() {}

    public static void registerConfigScreen(ModContainer container) {
        net.goui.cosmicdungeon.client.screen.skills.BogatyrClient.actions(net.goui.cosmicdungeon.network.ModNetwork::sendToServer);
        net.goui.cosmicdungeon.client.screen.skills.BogatyrClient.modeActions(net.goui.cosmicdungeon.network.ModNetwork::sendToServer);
        net.goui.cosmicdungeon.client.screen.skills.TheurgistClient.actions(net.goui.cosmicdungeon.network.ModNetwork::sendToServer);
        net.goui.cosmicdungeon.client.screen.requests.SupplyRequestsClient.actions(action ->
                net.goui.cosmicdungeon.network.ModNetwork.sendToServer(
                        new net.goui.cosmicdungeon.network.SupplyRequestPayloads.Action(action.runId(),action.revision(),
                                net.goui.cosmicdungeon.network.SupplyRequestPayloads.Decision.valueOf(action.decision().name()),
                                action.requestIds())));
        net.goui.cosmicdungeon.client.screen.skills.ClassResourceClient.recycleAction(snapshot ->
                net.goui.cosmicdungeon.network.ModNetwork.sendToServer(
                        new net.goui.cosmicdungeon.network.ClassResourcePayloads.Recycle(
                                snapshot.runId(),snapshot.resourceId(),snapshot.revision())));
        net.goui.cosmicdungeon.client.screen.skills.SkillsPanelClient.classHelp((parent, classId) ->
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        net.goui.cosmicdungeon.client.screen.HelpMenuScreen.forClass(parent, classId)));
        Supplier<IConfigScreenFactory> factory = () -> (modContainer, parent) -> new CosmicDungeonOptionsScreen(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }
}
