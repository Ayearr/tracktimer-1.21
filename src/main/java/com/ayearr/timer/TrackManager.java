package com.ayearr.timer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundCategory;
import net.minecraft.registry.Registries;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrackManager {

    private static volatile BlockPos startPos1 = null;
    private static volatile BlockPos startPos2 = null;

    private static final Map<UUID, Long> startTimes = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> wasInArea = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> lapCount = new ConcurrentHashMap<>();
    private static final Map<UUID, ServerBossBar> bossBars = new ConcurrentHashMap<>();
    private static final Set<UUID> joinedPlayers = ConcurrentHashMap.newKeySet();
    //private final MinecraftClient client;

    private static int lapTarget = 3;
    private static boolean raceStarted = false; // 比赛是否开始
    private static int nthRanking = 1;
    /** 设置起点区域 */
    public static void setStartPoint(BlockPos p1, BlockPos p2) {
        startPos1 = p1.toImmutable();
        startPos2 = p2.toImmutable();
    }

    /** 设置目标圈数 */
    public static void setLapTarget(int laps) {
        lapTarget = Math.max(1, laps);
    }
    /** 计算速度(actionbar)**/
    private static Vec3d lastPos = null;
    public static double getHorizontalSpeedKmh(ServerPlayerEntity player) {
        Entity target = player.getVehicle() != null ? player.getVehicle() : player;
        Vec3d pos = target.getPos();

        // 第一次调用，直接记录位置返回0
        if (lastPos == null) {
            lastPos = pos;
            return 0.0;
        }

        // 计算水平位移
        double dx = pos.x - lastPos.x;
        double dz = pos.z - lastPos.z;
        double distancePerTick = Math.sqrt(dx * dx + dz * dz);

        // 更新位置
        lastPos = pos;

        // 距离(方块/刻) × 20(刻/秒) × 3.6 = km/h
        return distancePerTick * 20 * 3.6;

    }

    //发出发车前的声音
    public static void playExperienceOrbSound(ServerPlayerEntity player) {
        if (player == null) return;

        Identifier soundId = Identifier.of("minecraft", "entity.experience_orb.pickup");
        // 获取 SoundEvent 对象
        SoundEvent soundEvent = Registries.SOUND_EVENT.get(soundId);

        if (soundEvent != null) {
            // 将 SoundEvent 转换为 RegistryEntry<SoundEvent>
            RegistryEntry<SoundEvent> soundEntry = Registries.SOUND_EVENT.getEntry(soundId).orElse(null);

            if (soundEntry != null) {
                // 发送声音播放数据包
                PlaySoundS2CPacket packet = new PlaySoundS2CPacket(
                        soundEntry,                       // 声音事件的 RegistryEntry
                        SoundCategory.MASTER,             // 声音类别
                        player.getX(), player.getY(), player.getZ(), // 播放位置
                        1.0f,                             // 音量
                        1.0f,                             // 音调
                        player.getRandom().nextLong()     // 随机种子
                );

                player.networkHandler.sendPacket(packet);
            }
        }
    }


    /** 重置所有状态 */
    public static void resetAll() {

        startTimes.clear();
        wasInArea.clear();
        lapCount.clear();
        bossBars.clear();
        joinedPlayers.clear();
        raceStarted = false;
        startPos1 = null;
        startPos2 = null;
        nthRanking = 1;
    }

    /** 玩家加入赛事模式 */
    public static void joinRaceMode(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (joinedPlayers.add(uuid)) {
            player.sendMessage(Text.literal("§a你已进入赛事模式！").formatted(Formatting.GREEN), false);
           // updatePlayerTabList(player);
        }
    }

    /** 玩家退出赛事模式 */
    public static void leaveRaceMode(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        if (joinedPlayers.remove(uuid)) {
            //player.setCustomName(Text.literal(player.getName().getString()).formatted(Formatting.WHITE));
            player.sendMessage(Text.literal("§c你已退出赛事模式！").formatted(Formatting.RED), false);
        }
    }

    /** 判断玩家是否在赛事模式 */
    public static boolean isInRaceMode(ServerPlayerEntity player) {
        return joinedPlayers.contains(player.getUuid());
    }

    /** 管理员开始比赛 */
    public static void startRace(ServerPlayerEntity admin) {
        if (raceStarted) {
            admin.sendMessage(Text.literal("§c比赛已经在进行中！").formatted(Formatting.RED), false);
            return;
        }
        if (joinedPlayers.isEmpty()) {
            admin.sendMessage(Text.literal("§e当前没有进入赛事模式的玩家，无法开始比赛。").formatted(Formatting.YELLOW), false);
            return;
        }


        new Thread(() -> {
            try {
                // 倒计时
                for (UUID uuid : joinedPlayers) {
                    ServerPlayerEntity p = admin.getServer().getPlayerManager().getPlayer(uuid);
                    if (p != null) {
                        sendTitle(p,Text.literal("比赛马上开始..."), Text.literal(""), 5, 40, 5);
                        playExperienceOrbSound(p);
                    }
                }
                Thread.sleep(3000);
                    for (UUID uuid : joinedPlayers) {
                        ServerPlayerEntity p = admin.getServer().getPlayerManager().getPlayer(uuid);
                        if (p != null) {
                            sendTitle(p,Text.literal("§f⚪⚪⚪⚪§c⚪"), Text.literal(""), 5, 40, 5);
                            playExperienceOrbSound(p);
                        }
                    }
                    Thread.sleep(1000);
                for (UUID uuid : joinedPlayers) {
                    ServerPlayerEntity p = admin.getServer().getPlayerManager().getPlayer(uuid);
                    if (p != null) {
                        sendTitle(p,Text.literal("§f⚪⚪⚪§c⚪⚪"), Text.literal(""), 5, 40, 5);
                        playExperienceOrbSound(p);

                    }
                }
                Thread.sleep(1000);
                for (UUID uuid : joinedPlayers) {
                    ServerPlayerEntity p = admin.getServer().getPlayerManager().getPlayer(uuid);
                    if (p != null) {
                        sendTitle(p,Text.literal("§f⚪⚪§c⚪⚪⚪"), Text.literal(""), 5, 40, 5);
                        playExperienceOrbSound(p);
                    }
                }
                Thread.sleep(1000);
                for (UUID uuid : joinedPlayers) {
                    ServerPlayerEntity p = admin.getServer().getPlayerManager().getPlayer(uuid);
                    if (p != null) {
                        sendTitle(p,Text.literal("§f⚪§c⚪⚪⚪⚪"), Text.literal(""), 5, 40, 5);
                        playExperienceOrbSound(p);
                    }
                }
                Thread.sleep(1000);
                for (UUID uuid : joinedPlayers) {
                    ServerPlayerEntity p = admin.getServer().getPlayerManager().getPlayer(uuid);
                    if (p != null) {
                        sendTitle(p,Text.literal("§c⚪⚪⚪⚪⚪"), Text.literal(""), 5, 40, 5);
                        playExperienceOrbSound(p);
                    }
                }
                Thread.sleep(1000);
                // 正式开始比赛
                raceStarted = true;
                //countdownRunning = false;
                long now = System.currentTimeMillis();
                for (UUID uuid : joinedPlayers) {
                    startTimes.put(uuid, now);
                    lapCount.put(uuid, 0);
                    ServerPlayerEntity p = admin.getServer().getPlayerManager().getPlayer(uuid);
                    if (p != null) {
                        sendTitle(p,Text.literal("⚪⚪⚪⚪⚪"), Text.literal(""), 5, 40, 5);
                    }
                }
                admin.sendMessage(Text.literal("§a比赛已开始！所有赛事模式玩家已开始计时！").formatted(Formatting.GREEN), false);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "race-countdown-thread").start();
    }

    /** 更新 BossBar */
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
            return newBar;
        });

        bossBar.setPercent(percent);
        bossBar.setName(Text.literal(String.format("§aLAP: %d / %d", currentLap, lapTarget)));

        if (currentLap >= lapTarget) {
            bossBar.setColor(BossBar.Color.RED);
        } else if (currentLap == lapTarget - 1) {
            bossBar.setColor(BossBar.Color.PINK);
        } else {
            bossBar.setColor(BossBar.Color.WHITE);
        }

        /*if (percent >= 0.9f && currentLap < lapTarget) {
            long time = System.currentTimeMillis() / 500;
            bossBar.setColor(time % 2 == 0 ? BossBar.Color.YELLOW : BossBar.Color.PINK);
        }*/
    }

    /**
     * 每 tick 调用，检查玩家是否通过起点
     * 注意：当玩家第一次检测到进入区域时会把 startTimes/ lapCount 初始化（如果缺失的话）
     */
    public static void maybeTriggerLap(ServerPlayerEntity player) {
        if (!raceStarted) return; // 没有开始比赛不计
        if (!isInRaceMode(player)) return;
        if (startPos1 == null || startPos2 == null) return;

        UUID uuid = player.getUuid();
        Entity entityToCheck = player.getRootVehicle() != null ? player.getRootVehicle() : player;
        boolean inArea = isEntityInStartArea(entityToCheck);
        boolean wasInside = wasInArea.getOrDefault(uuid, false);
        double speedKmh = getHorizontalSpeedKmh(player);
        player.sendMessage(Text.literal(String.format("§bv:§f %.2f §bkm/h", speedKmh)), true);
        if (inArea && !wasInside) {
            // 确保有开始时间（如果玩家在比赛开始后加入或首次触发）
            startTimes.putIfAbsent(uuid, System.currentTimeMillis());

            if (!lapCount.containsKey(uuid) || lapCount.get(uuid) == 0) {
                // 玩家第一次通过起点：开始第 1 圈
                lapCount.put(uuid, 1);
                player.sendMessage(Text.literal("§e开始第 1 圈").formatted(Formatting.GOLD), false);
                updateBossBar(player, 1);
                //sendTitle(player, Text.literal("第 1 圈"), Text.literal("继续努力！"), 10, 60, 10);
            } else {
                int previousLap = lapCount.getOrDefault(uuid, 1);
                int nextLap = previousLap + 1;

                // 当 nextLap > lapTarget 表示刚刚完成了最后一圈（例如 lapTarget=3：previous=3 -> next=4 -> 完成）
                if (nextLap > lapTarget) {
                    long totalTime = System.currentTimeMillis() - startTimes.getOrDefault(uuid, System.currentTimeMillis());
                    double seconds = totalTime / 1000.0;

                    player.sendMessage(Text.literal(String.format(
                                    "§a完成第 %d 圈，比赛结束！用时: %.2f 秒", lapTarget, seconds))
                            .formatted(Formatting.GREEN), false);

                    ServerBossBar bossBar = bossBars.get(uuid);
                    if (bossBar != null) {
                        bossBar.setName(Text.literal("§cRace Ended"));
                        bossBar.setColor(BossBar.Color.RED);
                        bossBar.setPercent(1.0f);
                    }
                    player.getServer().getPlayerManager().broadcast(
                            Text.literal(String.format("#%d : ",nthRanking++) + player.getGameProfile().getName() + String.format(" >>>  %.2f sec.",seconds)),
                            false
                    );

                    //sendTitle(player, Text.literal("Race Ended!"), Text.literal(String.format("用时: %.2f 秒", seconds)), 10, 100, 20);

                    spawnFireworks(player);

                    // 清理玩家的比赛数据（延迟以保留 bossbar / 信息显示）
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ServerBossBar bar = bossBars.remove(uuid);
                        if (bar != null) {
                            bar.removePlayer(player);
                        }
                        //leaveRaceMode(uuid);
                        lapCount.remove(uuid);
                        wasInArea.remove(uuid);
                        startTimes.remove(uuid);
                    }).start();

                } else {
                    // 进到下一圈
                    lapCount.put(uuid, nextLap);
                    player.sendMessage(Text.literal(String.format(
                                    "§a完成第 %d 圈，开始第 %d 圈", previousLap, nextLap))
                            .formatted(Formatting.GREEN), false);
                    updateBossBar(player, nextLap);
                    //sendTitle(player, Text.literal("第 " + nextLap + " 圈"), Text.literal("继续努力！"), 10, 60, 10);
                }
            }
        }

        // 更新 wasInArea 状态（无论是否触发）
        wasInArea.put(uuid, inArea);
    }

    /** 终点烟花效果 */
    private static void spawnFireworks(ServerPlayerEntity player) {
        var world = player.getServerWorld();
        var pos = player.getBlockPos();

        for (int i = 0; i < 3; i++) {
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

    /** 更新玩家名字颜色(预留) */
    public static void updatePlayerTabList(ServerPlayerEntity player) {
        if (isInRaceMode(player)) {
            player.setCustomName(Text.literal(player.getName().getString()).formatted(Formatting.RED));
        } else {
            player.setCustomName(Text.literal(player.getName().getString()).formatted(Formatting.WHITE));
        }
    }

    /** 检查实体是否在起点区域内 */
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

    /**
     * 发送标题
     */
    private static void sendTitle(ServerPlayerEntity player, Text title, Text subtitle, int fadeIn, int stay, int fadeOut) {
        // 先发送动画时长包，再发送标题和副标题
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(fadeIn, stay, fadeOut));
        player.networkHandler.sendPacket(new TitleS2CPacket(title));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
    }
}