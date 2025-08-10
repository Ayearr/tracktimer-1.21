package com.ayearr.timer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class Tracktimer implements ModInitializer {
    @Override
    public void onInitialize() {
        TrackCommandRegistrar.registerCommands();

        // 每个 server tick 的末尾检查玩家位置
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 遍历当前在线玩家，触发计时检测
            server.getPlayerManager().getPlayerList().forEach(TrackManager::maybeTriggerTiming
            );
        });

        System.out.println("[Tracktimer] Initialized.");
    }
}
