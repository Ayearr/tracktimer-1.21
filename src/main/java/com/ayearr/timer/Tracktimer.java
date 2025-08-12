package com.ayearr.timer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class Tracktimer implements ModInitializer {
    @Override
    public void onInitialize() {
        // 注册已有的命令
        TrackCommandRegistrar.registerCommands();

        // 注册新的 trackjoin 命令
        TrackCommandRegistrar.registerCommands();

        // 每个 tick 检查玩家的赛事状态
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getPlayerManager().getPlayerList().forEach(TrackManager::maybeTriggerLap);
        });

        System.out.println("[Tracktimer] Initialized.");
    }
}
