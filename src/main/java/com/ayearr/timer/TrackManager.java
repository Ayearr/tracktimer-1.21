package com.ayearr.timer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrackManager {

    private static volatile BlockPos startPos1 = null;
    private static volatile BlockPos startPos2 = null;

    private static final Map<UUID, Long> startTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> wasInArea = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lapCount = new ConcurrentHashMap<>();
    private static final Map<UUID, ServerBossBar> bossBars = new ConcurrentHashMap<>();

    private static int lapTarget = 3;

    public static void setStartPoint(BlockPos p1, BlockPos p2) {
        startPos1 = p1.toImmutable();
        startPos2 = p2.toImmutable();
    }

    public static void setLapTarget(int laps) {
        lapTarget = laps;
    }

    public static void resetAll() {
        startTimes.clear();
        wasInArea.clear();
        lapCount.clear();
        bossBars.clear();
        startPos1 = null;
        startPos2 = null;
    }

    private static boolean isEntityInStartArea(Entity entity) {
        if (startPos1 == null || startPos2 == null) return false;
        BlockPos pos = entity.getBlockPos();

        int minX = Math.min(startPos1.getX(), startPos2.getX());
        int minY = Math.min(startPos1.getY(), startPos2.getY());
        int minZ = Math.min(startPos1.getZ(), startPos2.getZ());
        int maxX = Math.max(startPos1.getX(), startPos2.getX());
        int maxY = Math.max(startPos1.getY(), startPos2.getY());
        int maxZ = Math.max(startPos1.getZ(), startPos2.getZ());

        return pos.getX() >= minX && pos.getX() <= maxX &&
                pos.getY() >= minY && pos.getY() <= maxY &&
                pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    private static void updateBossBar(ServerPlayerEntity player, int currentLap) {
        UUID uuid = player.getUuid();
        float percent = Math.min((float) currentLap / lapTarget, 1.0f);

        ServerBossBar bossBar = bossBars.computeIfAbsent(uuid, id -> {
            ServerBossBar newBar = new ServerBossBar(
                    Text.literal("§a比赛进度"),
                    BossBar.Color.GREEN,
                    BossBar.Style.PROGRESS
            );
            newBar.addPlayer(player);
            player.networkHandler.sendPacket(BossBarS2CPacket.add(newBar));
            return newBar;
        });


        bossBar.setPercent(percent);
        bossBar.setName(Text.literal(String.format("§aLAP: %d / %d  (%.0f%%)", currentLap, lapTarget, percent * 100)));


        if (currentLap >= lapTarget) {
            bossBar.setColor(BossBar.Color.PURPLE);
        } else if (currentLap == lapTarget - 1) {
            bossBar.setColor(BossBar.Color.PINK);
        } else {
            bossBar.setColor(BossBar.Color.WHITE);
        }

        // 临近终点闪烁（>90% 且未完成）
        /*if (percent >= 0.9f && currentLap < lapTarget) {
            long time = System.currentTimeMillis() / 500; // 半秒切一次
            if (time % 2 == 0) {
                bossBar.setColor(BossBar.Color.YELLOW);
            } else {
                bossBar.setColor(BossBar.Color.PINK);
            }
        }*/

        // 发送更新包
        player.networkHandler.sendPacket(BossBarS2CPacket.updateProgress(bossBar));
        player.networkHandler.sendPacket(BossBarS2CPacket.updateName(bossBar));
        player.networkHandler.sendPacket(BossBarS2CPacket.updateStyle(bossBar));
    }

    public static void maybeTriggerTiming(ServerPlayerEntity player) {
        if (startPos1 == null || startPos2 == null) return;

        UUID uuid = player.getUuid();
        Entity entityToCheck = player.getRootVehicle() != null ? player.getRootVehicle() : player;
        boolean inArea = isEntityInStartArea(entityToCheck);
        boolean wasInside = wasInArea.getOrDefault(uuid, false);

        if (inArea && !wasInside) {
            long now = System.currentTimeMillis();


            if (!startTimes.containsKey(uuid)) {
                startTimes.put(uuid, now);
                lapCount.put(uuid, 1); // 表示正在第 1 圈
                player.sendMessage(Text.literal("§e计时开始！开始第 1 圈").formatted(Formatting.GOLD), false);
                updateBossBar(player, 1);
            } else {

                int previousLap = lapCount.getOrDefault(uuid, 1); // 当前正在进行的圈
                int nextLap = previousLap + 1;


                if (nextLap > lapTarget) {
                    long elapsedMs = now - startTimes.get(uuid);
                    double seconds = elapsedMs / 1000.0;

                    player.sendMessage(Text.literal(String.format(
                                    "§a完成第 %d 圈，比赛结束！用时 %.3f 秒", lapTarget, seconds))
                            .formatted(Formatting.GREEN), false);


                    ServerBossBar bossBar = bossBars.get(uuid);
                    if (bossBar != null) {
                        bossBar.setName(Text.literal("§cRace Ended"));
                        bossBar.setColor(BossBar.Color.RED);
                        bossBar.setPercent(1.0f);
                    }


                    spawnFireworks(player);


                    new Thread(() -> {
                        try {
                            Thread.sleep(3000); // 延迟 2 秒
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        ServerBossBar bar = bossBars.remove(uuid);
                        if (bar != null) {
                            bar.removePlayer(player);
                            player.networkHandler.sendPacket(BossBarS2CPacket.remove(bar.getUuid()));
                        }


                        startTimes.remove(uuid);
                        lapCount.remove(uuid);
                        wasInArea.remove(uuid);
                    }).start();

                } else {

                    lapCount.put(uuid, nextLap);

                    player.sendMessage(Text.literal(String.format("§a完成第 %d 圈，开始第 %d 圈", previousLap, nextLap))
                            .formatted(Formatting.GREEN), false);


                    updateBossBar(player, nextLap);
                }
            }
        }

        wasInArea.put(uuid, inArea);
    }


    private static void spawnFireworks(ServerPlayerEntity player) {
        var world = player.getServerWorld();
        var pos = player.getBlockPos();

        for (int i = 0; i < 3; i++) { // 放3个烟花
            var firework = new net.minecraft.entity.projectile.FireworkRocketEntity(
                    world,
                    pos.getX() + 0.5,
                    pos.getY() + 1,
                    pos.getZ() + 0.5,
                    new net.minecraft.item.ItemStack(net.minecraft.item.Items.FIREWORK_ROCKET)
            );
            world.spawnEntity(firework);
        }
    }
}
