package com.ayearr.timer;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class TrackCommandRegistrar {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // ========= /trackset（需要管理员权限） =========
            dispatcher.register(literal("trackset")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(argument("x1", integer()).suggests(TrackCommandRegistrar::suggestPlayerX)
                            .then(argument("y1", integer()).suggests(TrackCommandRegistrar::suggestPlayerY)
                                    .then(argument("z1", integer()).suggests(TrackCommandRegistrar::suggestPlayerZ)
                                            .then(argument("x2", integer()).suggests(TrackCommandRegistrar::suggestPlayerX)
                                                    .then(argument("y2", integer()).suggests(TrackCommandRegistrar::suggestPlayerY)
                                                            .then(argument("z2", integer()).suggests(TrackCommandRegistrar::suggestPlayerZ)
                                                                    .executes(ctx -> {
                                                                        int x1 = ctx.getArgument("x1", Integer.class);
                                                                        int y1 = ctx.getArgument("y1", Integer.class);
                                                                        int z1 = ctx.getArgument("z1", Integer.class);
                                                                        int x2 = ctx.getArgument("x2", Integer.class);
                                                                        int y2 = ctx.getArgument("y2", Integer.class);
                                                                        int z2 = ctx.getArgument("z2", Integer.class);

                                                                        BlockPos point1 = new BlockPos(x1, y1, z1);
                                                                        BlockPos point2 = new BlockPos(x2, y2, z2);
                                                                        TrackManager.setStartPoint(point1, point2);

                                                                        ServerCommandSource src = ctx.getSource();
                                                                        src.sendFeedback(() -> Text.literal("§a赛道起点区域已设置！"), false);
                                                                        return 1;
                                                                    })
                                                            )
                                                    )
                                            )
                                    )
                            )
                    )
            );

            // ========= /trackreset（需要管理员权限） =========
            dispatcher.register(literal("trackreset")
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(ctx -> {
                        ServerCommandSource src = ctx.getSource();
                        TrackManager.resetAll();
                        src.sendFeedback(() -> Text.literal("§c所有计时状态已重置。"), false);
                        return 1;
                    })
            );

            // ========= /tracksetlaps（需要管理员权限） =========
            dispatcher.register(literal("tracksetlaps")
                    .requires(src -> src.hasPermissionLevel(2))
                    .then(argument("laps", integer())
                            .executes(ctx -> {
                                int laps = ctx.getArgument("laps", Integer.class);
                                TrackManager.setLapTarget(laps);
                                ServerCommandSource src = ctx.getSource();
                                src.sendFeedback(() -> Text.literal("§a目标圈数已设置为 " + laps + " 圈！"), false);
                                return 1;
                            })
                    )
            );

            // ========= /starttrack（需要管理员权限） =========
            dispatcher.register(literal("starttrack")
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(ctx -> {
                        ServerCommandSource source = ctx.getSource();
                        if (source.getEntity() instanceof ServerPlayerEntity admin) {
                            TrackManager.startRace(admin);
                        } else {
                            source.sendFeedback(() -> Text.literal("§c该命令只能玩家执行。"), false);
                        }
                        return 1;
                    })
            );

            // ========= /trackjoin（所有玩家可用） =========
            dispatcher.register(literal("trackjoin")
                    .executes(ctx -> {
                        if (ctx.getSource().getEntity() instanceof ServerPlayerEntity player) {
                            TrackManager.joinRaceMode(player);
                        } else {
                            ctx.getSource().sendFeedback(() -> Text.literal("§c该命令只能玩家执行。"), false);
                        }
                        return 1;
                    })
            );
            dispatcher.register(literal("trackleave")
                    .executes(ctx -> {
                        if (ctx.getSource().getEntity() instanceof ServerPlayerEntity player) {
                            TrackManager.leaveRaceMode(player);
                        } else {
                            ctx.getSource().sendFeedback(() -> Text.literal("§c该命令只能玩家执行。"), false);
                        }
                        return 1;
                    })
            );
        });
    }

    private static CompletableFuture<Suggestions> suggestPlayerX(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player != null) {
            builder.suggest(player.getBlockX());
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPlayerY(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player != null) {
            builder.suggest(player.getBlockY());
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPlayerZ(CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player != null) {
            builder.suggest(player.getBlockZ());
        }
        return builder.buildFuture();
    }
}
