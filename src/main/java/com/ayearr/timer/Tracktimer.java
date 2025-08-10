package com.ayearr.timer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class Tracktimer implements ModInitializer {
    @Override
    public void onInitialize() {
        TrackCommandRegistrar.registerCommands();


        ServerTickEvents.END_SERVER_TICK.register(server -> {

            server.getPlayerManager().getPlayerList().forEach(TrackManager::maybeTriggerTiming
            );
        });

        System.out.println("[Tracktimer] Initialized.");
    }
}
