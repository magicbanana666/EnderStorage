package com.magicbanana666.enderstorage;

import java.util.Optional;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * Finds the team of a player for team chests and tanks: the FTB Teams party when FTB Teams is installed, otherwise
 * the vanilla scoreboard team (/team). FTB Teams is used through reflection so it stays optional.
 */
public final class Teams {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean ftbFailed;

    /** A team id (prefixed with where it comes from) and its display name. */
    public record TeamInfo(String id, String name) {
    }

    private Teams() {
    }

    /** The team of the player, only reliable on the server. */
    public static Optional<TeamInfo> teamOf(Player player) {
        Optional<TeamInfo> ftb = ftbTeam(player.getUUID());
        if (ftb.isPresent()) {
            return ftb;
        }
        PlayerTeam team = player.getTeam();
        if (team != null) {
            return Optional.of(new TeamInfo("scoreboard:" + team.getName(), team.getDisplayName().getString()));
        }
        return Optional.empty();
    }

    public static boolean isMember(Player player, String teamId) {
        return teamOf(player).map(team -> team.id().equals(teamId)).orElse(false);
    }

    private static Optional<TeamInfo> ftbTeam(UUID player) {
        if (ftbFailed || !ModList.get().isLoaded("ftbteams")) {
            return Optional.empty();
        }
        try {
            // Call through the public API interfaces; the implementing classes are internal
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            Class<?> managerClass = Class.forName("dev.ftb.mods.ftbteams.api.TeamManager");
            Class<?> teamClass = Class.forName("dev.ftb.mods.ftbteams.api.Team");
            Object api = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI").getMethod("api").invoke(null);
            if (!(boolean) apiClass.getMethod("isManagerLoaded").invoke(api)) {
                return Optional.empty();
            }
            Object manager = apiClass.getMethod("getManager").invoke(api);
            Optional<?> team = (Optional<?>) managerClass.getMethod("getTeamForPlayerID", UUID.class).invoke(manager, player);
            // Every player has a personal FTB team; only a party counts as a team here
            if (team.isEmpty() || !(boolean) teamClass.getMethod("isPartyTeam").invoke(team.get())) {
                return Optional.empty();
            }
            UUID id = (UUID) teamClass.getMethod("getId").invoke(team.get());
            Component name = (Component) teamClass.getMethod("getName").invoke(team.get());
            return Optional.of(new TeamInfo("ftb:" + id, name.getString()));
        } catch (ReflectiveOperationException | RuntimeException e) {
            ftbFailed = true;
            LOGGER.warn("Could not read FTB Teams, using scoreboard teams instead", e);
            return Optional.empty();
        }
    }
}
