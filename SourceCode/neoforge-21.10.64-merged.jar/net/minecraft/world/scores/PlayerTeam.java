package net.minecraft.world.scores;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class PlayerTeam extends Team {
    private static final int BIT_FRIENDLY_FIRE = 0;
    private static final int BIT_SEE_INVISIBLES = 1;
    private final Scoreboard scoreboard;
    private final String name;
    private final Set<String> players = Sets.newHashSet();
    private Component displayName;
    private Component playerPrefix = CommonComponents.EMPTY;
    private Component playerSuffix = CommonComponents.EMPTY;
    private boolean allowFriendlyFire = true;
    private boolean seeFriendlyInvisibles = true;
    private Team.Visibility nameTagVisibility = Team.Visibility.ALWAYS;
    private Team.Visibility deathMessageVisibility = Team.Visibility.ALWAYS;
    private ChatFormatting color = ChatFormatting.RESET;
    private Team.CollisionRule collisionRule = Team.CollisionRule.ALWAYS;
    private final Style displayNameStyle;

    public PlayerTeam(Scoreboard scoreboard, String name) {
        this.scoreboard = scoreboard;
        this.name = name;
        this.displayName = Component.literal(name);
        this.displayNameStyle = Style.EMPTY.withInsertion(name).withHoverEvent(new HoverEvent.ShowText(Component.literal(name)));
    }

    public PlayerTeam.Packed pack() {
        return new PlayerTeam.Packed(
            this.name,
            Optional.of(this.displayName),
            this.color != ChatFormatting.RESET ? Optional.of(this.color) : Optional.empty(),
            this.allowFriendlyFire,
            this.seeFriendlyInvisibles,
            this.playerPrefix,
            this.playerSuffix,
            this.nameTagVisibility,
            this.deathMessageVisibility,
            this.collisionRule,
            List.copyOf(this.players)
        );
    }

    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public MutableComponent getFormattedDisplayName() {
        MutableComponent mutablecomponent = ComponentUtils.wrapInSquareBrackets(this.displayName.copy().withStyle(this.displayNameStyle));
        ChatFormatting chatformatting = this.getColor();
        if (chatformatting != ChatFormatting.RESET) {
            mutablecomponent.withStyle(chatformatting);
        }

        return mutablecomponent;
    }

    /**
     * Sets the display name for this team.
     */
    public void setDisplayName(Component name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        } else {
            this.displayName = name;
            this.scoreboard.onTeamChanged(this);
        }
    }

    public void setPlayerPrefix(@Nullable Component playerPrefix) {
        this.playerPrefix = playerPrefix == null ? CommonComponents.EMPTY : playerPrefix;
        this.scoreboard.onTeamChanged(this);
    }

    public Component getPlayerPrefix() {
        return this.playerPrefix;
    }

    public void setPlayerSuffix(@Nullable Component playerSuffix) {
        this.playerSuffix = playerSuffix == null ? CommonComponents.EMPTY : playerSuffix;
        this.scoreboard.onTeamChanged(this);
    }

    public Component getPlayerSuffix() {
        return this.playerSuffix;
    }

    @Override
    public Collection<String> getPlayers() {
        return this.players;
    }

    @Override
    public MutableComponent getFormattedName(Component formattedName) {
        MutableComponent mutablecomponent = Component.empty().append(this.playerPrefix).append(formattedName).append(this.playerSuffix);
        ChatFormatting chatformatting = this.getColor();
        if (chatformatting != ChatFormatting.RESET) {
            mutablecomponent.withStyle(chatformatting);
        }

        return mutablecomponent;
    }

    public static MutableComponent formatNameForTeam(@Nullable Team playerTeam, Component playerName) {
        return playerTeam == null ? playerName.copy() : playerTeam.getFormattedName(playerName);
    }

    @Override
    public boolean isAllowFriendlyFire() {
        return this.allowFriendlyFire;
    }

    /**
     * Sets whether friendly fire (PVP between members of the team) is allowed.
     */
    public void setAllowFriendlyFire(boolean friendlyFire) {
        this.allowFriendlyFire = friendlyFire;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public boolean canSeeFriendlyInvisibles() {
        return this.seeFriendlyInvisibles;
    }

    /**
     * Sets whether members of this team can see other members that are invisible.
     */
    public void setSeeFriendlyInvisibles(boolean friendlyInvisibles) {
        this.seeFriendlyInvisibles = friendlyInvisibles;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public Team.Visibility getNameTagVisibility() {
        return this.nameTagVisibility;
    }

    @Override
    public Team.Visibility getDeathMessageVisibility() {
        return this.deathMessageVisibility;
    }

    /**
     * Sets the visibility flags for player name tags.
     */
    public void setNameTagVisibility(Team.Visibility visibility) {
        this.nameTagVisibility = visibility;
        this.scoreboard.onTeamChanged(this);
    }

    /**
     * Sets the visibility flags for player death messages.
     */
    public void setDeathMessageVisibility(Team.Visibility visibility) {
        this.deathMessageVisibility = visibility;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public Team.CollisionRule getCollisionRule() {
        return this.collisionRule;
    }

    /**
     * Sets the rule to be used for handling collisions with members of this team.
     */
    public void setCollisionRule(Team.CollisionRule rule) {
        this.collisionRule = rule;
        this.scoreboard.onTeamChanged(this);
    }

    public int packOptions() {
        int i = 0;
        if (this.isAllowFriendlyFire()) {
            i |= 1;
        }

        if (this.canSeeFriendlyInvisibles()) {
            i |= 2;
        }

        return i;
    }

    /**
     * Sets friendly fire and invisibles flags based off of the given bitmask.
     */
    public void unpackOptions(int flags) {
        this.setAllowFriendlyFire((flags & 1) > 0);
        this.setSeeFriendlyInvisibles((flags & 2) > 0);
    }

    /**
     * Sets the color for this team. The team color is used mainly for team kill objectives and team-specific setDisplay usage. It does _not_ affect all situations (for instance, the prefix is used for the glowing effect).
     */
    public void setColor(ChatFormatting color) {
        this.color = color;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public ChatFormatting getColor() {
        return this.color;
    }

    public record Packed(
        String name,
        Optional<Component> displayName,
        Optional<ChatFormatting> color,
        boolean allowFriendlyFire,
        boolean seeFriendlyInvisibles,
        Component memberNamePrefix,
        Component memberNameSuffix,
        Team.Visibility nameTagVisibility,
        Team.Visibility deathMessageVisibility,
        Team.CollisionRule collisionRule,
        List<String> players
    ) {
        public static final Codec<PlayerTeam.Packed> CODEC = RecordCodecBuilder.create(
            p_401399_ -> p_401399_.group(
                    Codec.STRING.fieldOf("Name").forGetter(PlayerTeam.Packed::name),
                    ComponentSerialization.CODEC.optionalFieldOf("DisplayName").forGetter(PlayerTeam.Packed::displayName),
                    ChatFormatting.COLOR_CODEC.optionalFieldOf("TeamColor").forGetter(PlayerTeam.Packed::color),
                    Codec.BOOL.optionalFieldOf("AllowFriendlyFire", true).forGetter(PlayerTeam.Packed::allowFriendlyFire),
                    Codec.BOOL.optionalFieldOf("SeeFriendlyInvisibles", true).forGetter(PlayerTeam.Packed::seeFriendlyInvisibles),
                    ComponentSerialization.CODEC.optionalFieldOf("MemberNamePrefix", CommonComponents.EMPTY).forGetter(PlayerTeam.Packed::memberNamePrefix),
                    ComponentSerialization.CODEC.optionalFieldOf("MemberNameSuffix", CommonComponents.EMPTY).forGetter(PlayerTeam.Packed::memberNameSuffix),
                    Team.Visibility.CODEC.optionalFieldOf("NameTagVisibility", Team.Visibility.ALWAYS).forGetter(PlayerTeam.Packed::nameTagVisibility),
                    Team.Visibility.CODEC
                        .optionalFieldOf("DeathMessageVisibility", Team.Visibility.ALWAYS)
                        .forGetter(PlayerTeam.Packed::deathMessageVisibility),
                    Team.CollisionRule.CODEC.optionalFieldOf("CollisionRule", Team.CollisionRule.ALWAYS).forGetter(PlayerTeam.Packed::collisionRule),
                    Codec.STRING.listOf().optionalFieldOf("Players", List.of()).forGetter(PlayerTeam.Packed::players)
                )
                .apply(p_401399_, PlayerTeam.Packed::new)
        );
    }
}
