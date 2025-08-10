package com.ayearr.timer;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrackManager {

    // 赛道起点区域的两个角（包含在内）
    private static volatile BlockPos startPos1 = null;
    private static volatile BlockPos startPos2 = null;

    // 每个玩家的开始时间（毫秒）
    private static final Map<UUID, Long> startTimes = new ConcurrentHashMap<>();

    // 设置赛道区域
    public static void setStartPoint(BlockPos point1, BlockPos point2) {
        startPos1 = point1.toImmutable();
        startPos2 = point2.toImmutable();
    }

    // 重置所有计时状态（清空开始时间）
    public static void resetAll() {
        startTimes.clear();
        startPos1 = null;
        startPos2 = null;
    }

    // 检查玩家是否进入赛道区域
    public static boolean isPlayerInStartArea(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
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

    // 每个 tick 调用：检查玩家是否进入赛道区域并触发计时
    public static void maybeTriggerTiming(ServerPlayerEntity player) {
        if (startPos1 == null || startPos2 == null) return;

        if (isPlayerInStartArea(player)) {
            UUID uuid = player.getUuid();
            long now = System.currentTimeMillis(); // 使用外部计时器

            // 如果没有开始时间则开始计时
            if (!startTimes.containsKey(uuid)) {
                startTimes.put(uuid, now);
                player.sendMessage(Text.literal("§e计时开始！"), false);

            } else {
                // 已有开始时间 -> 结束计时并输出结果
                Long start = startTimes.remove(uuid);
                if (start != null) {
                    long elapsedMs = now - start;
                    double seconds = elapsedMs / 1000.0;
                    String msg = String.format("§a计时结束！耗时：%.3f 秒", seconds);
                    player.sendMessage(Text.literal(msg), false);
                }
            }
        }
    }
}
