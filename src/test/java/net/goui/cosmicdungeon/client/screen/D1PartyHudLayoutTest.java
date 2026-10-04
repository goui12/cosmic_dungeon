package net.goui.cosmicdungeon.client.screen;

import io.netty.buffer.Unpooled;
import java.util.List;
import net.goui.cosmicdungeon.network.PartyPayloads;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class D1PartyHudLayoutTest {
    @Test void inventoryPanelAndControlsStayOutsideSlotsAtNormalGuiScales() {
        for (int viewport : new int[]{320, 426, 640, 960, 1920}) {
            for (int inventoryWidth : new int[]{176, 195}) {
                int left = (viewport - inventoryWidth) / 2;
                for (int members = 0; members <= 6; members++) {
                    var box = D1PartyHudLayout.forView(left, members);
                    assertTrue(box.x() + box.width() + 8 <= left);
                    assertTrue(box.controlsY() + 44 <= 240);
                    assertTrue(box.width() > 0 && box.width() <= 224);
                }
            }
        }
    }
    @Test void recipeBookSummaryLeavesCurrencyAndBottomControlsTheirOwnSpace() {
        for (int width : new int[]{320, 378, 379, 426, 640, 960}) {
            var box = D1PartyHudLayout.withRecipeBook(width);
            assertTrue(box.y() + box.height() < (240 - 166) / 2);
            assertTrue(box.x() + box.width() * 2 + 8 <= width - 8);
            assertTrue(width - box.x() - box.width() - 16 >= 100);
            assertTrue(240 - 26 > (240 - 166) / 2 + 166);
        }
    }
    @Test void worldHudKeepsSixMembersBoundedAndInventoryActionsUseReservedSentinel() {
        var box = D1PartyHudLayout.forView(-1, 6);
        assertEquals(224, box.width());
        assertEquals(104, box.height());
        var action = new PartyPayloads.Action(-1, 93, "ready", "");
        var view = new PartyPayloads.View(-1, new PartyPayloads.State(93, "READY_CHECK", true, 6, 0, -1),
                List.of(new PartyPayloads.Member("Cameron", "bogatyr", true, true)),
                new PartyPayloads.Invite("", "", false, false),
                new PartyPayloads.Recruitment("Roamers", false, 0, 1, List.of()));
        var buffer = Unpooled.buffer();
        try {
            PartyPayloads.Action.STREAM_CODEC.encode(buffer, action);
            assertEquals(action, PartyPayloads.Action.STREAM_CODEC.decode(buffer));
            PartyPayloads.View.STREAM_CODEC.encode(buffer, view);
            assertEquals(view, PartyPayloads.View.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
}
