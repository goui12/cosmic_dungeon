package net.goui.cosmicdungeon.economy.pricing;

import java.util.Set;

public record GearSetDefinition(
        String id,
        Set<String> pieceItemIds,
        long fullSetTraceValue,
        long individualPieceTraceValue
) {
}
