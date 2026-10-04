package net.goui.cosmicdungeon.client.screen;

import net.goui.cosmicdungeon.menu.ClassSelectorMenu;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import static org.junit.jupiter.api.Assertions.*;

final class TamsinKeyboardTest {
    private static ClassSelectorScreen screen(){
        var inventory=new Inventory(null,new EntityEquipment());
        return new ClassSelectorScreen(new ClassSelectorMenu(1,inventory),inventory,Component.empty());
    }
    @Test void eIsConsumedWithoutTextFocusOrContainerClose(){
        var screen=screen();
        assertTrue(screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_E,0,0)));
        assertTrue(screen.shouldCloseOnEsc());
    }
    @Test void eAndUppercaseEReachTheFocusedNativeTextWidget(){
        var screen=screen();var name=new EditBox(null,160,20,Component.literal("Group name"));
        screen.setFocused(name);
        assertTrue(screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_E,0,0)));
        assertTrue(screen.charTyped(new CharacterEvent('e',0)));
        assertTrue(screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_E,0,GLFW.GLFW_MOD_SHIFT)));
        assertTrue(screen.charTyped(new CharacterEvent('E',GLFW.GLFW_MOD_SHIFT)));
        assertEquals("eE",name.getValue());
        assertSame(name,screen.getFocused());
        assertTrue(screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_BACKSPACE,0,0)));
        assertEquals("e",name.getValue());
    }
}
