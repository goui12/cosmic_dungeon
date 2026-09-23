package net.goui.cosmicdungeon.playerclass.d1;

/** Pure D1 combat decisions shared by native hooks and offline regression checks. */
public final class D1CombatRules {
    public enum Ammunition { VANILLA, DENIED, ABILITY }
    private D1CombatRules() {}

    public static Ammunition ammunition(String id, String classId, boolean activeOwner, boolean bindingValid) {
        var entry = D1AmmunitionCatalog.find(id);
        if (entry == null) return Ammunition.VANILLA;
        return activeOwner && bindingValid && entry.classes().contains(classId)
                ? Ammunition.ABILITY : Ammunition.DENIED;
    }

    public static boolean supportive(String id) {
        return switch (id == null ? "" : id) {
            case "mending_sting", "verdant_jolt", "scintilla_vitalis", "lux_vitalis", "ebonsight" -> true;
            default -> false;
        };
    }

    public static boolean existingWins(double oldPower, int oldDuration, int remaining,
                                       double newPower, int newDuration, boolean periodic) {
        double oldRate = oldPower / (periodic ? Math.max(1, oldDuration) : 1.0);
        double newRate = newPower / (periodic ? Math.max(1, newDuration) : 1.0);
        int comparison = Double.compare(oldRate, newRate);
        return comparison > 0 || comparison == 0 && (remaining == -1 || remaining >= newDuration);
    }

    public static double pulse(double power, int duration) {
        return Math.max(0, power) / Math.ceil(Math.max(1, duration) / 20.0);
    }

    public static double poisonPulse(double power, int duration, double health) {
        return Math.max(0, Math.min(pulse(power, duration), health - 1));
    }

    public static double spicule(double power, long harmfulEffects, double scale, int cap) {
        return power * (1 + Math.min(Math.max(0, harmfulEffects), Math.max(0, cap)) * scale);
    }

    public static double rocketDamage(double power, double distance, double radius) {
        if (!Double.isFinite(distance) || !Double.isFinite(radius) || radius <= 0 || distance < 0 || distance >= radius)
            return 0;
        return power * Math.sqrt(1 - distance / radius);
    }

    public static boolean tridentHit(boolean activeOwner, boolean thrownTridentDamage,
                                     boolean directMeleeWithTrident, double dealt) {
        return activeOwner && Double.isFinite(dealt) && dealt > 0
                && (thrownTridentDamage || directMeleeWithTrident);
    }
}
