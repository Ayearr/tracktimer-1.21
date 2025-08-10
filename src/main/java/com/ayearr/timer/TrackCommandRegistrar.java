package com.ayearr.timer;

//import com.mojang.brigadier.arguments.IntegerArgumentType;
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

            dispatcher.register(literal("trackset")
                    .requires(src -> src.getEntity() instanceof ServerPlayerEntity)
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

            dispatcher.register(literal("trackreset")
                    .executes(ctx -> {
                        ServerCommandSource src = ctx.getSource();
                        TrackManager.resetAll();
                        src.sendFeedback(() -> Text.literal("§c所有计时状态已重置。"), false);
                        return 1;
                    })
            );

            dispatcher.register(literal("tracksetlaps")
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
