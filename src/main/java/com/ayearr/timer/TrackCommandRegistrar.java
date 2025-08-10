package com.ayearr.timer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.literal;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class TrackCommandRegistrar {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(literal("trackset")
                    .requires(src -> src.getEntity() instanceof ServerPlayerEntity)
                    .then(net.minecraft.server.command.CommandManager.argument("x1", integer())
                            .then(net.minecraft.server.command.CommandManager.argument("y1", integer())
                                    .then(net.minecraft.server.command.CommandManager.argument("z1", integer())
                                            .then(net.minecraft.server.command.CommandManager.argument("x2", integer())
                                                    .then(net.minecraft.server.command.CommandManager.argument("y2", integer())
                                                            .then(net.minecraft.server.command.CommandManager.argument("z2", integer())
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
        });
    }
}
