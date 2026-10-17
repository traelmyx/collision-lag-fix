package io.github.traelmyx.collisionlagfix.test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.nio.file.Files;

// Run with -Dsmoketest=true: creates a flat world, fires a volley of fast TNT into a stone wall, takes a screenshot and quits.
public final class ClientSmoke {

    public static final boolean ENABLED = Boolean.getBoolean("smoketest");
    private static final long TIMEOUT_MS = 5 * 60 * 1000;

    private static int stage;
    private static int ticks;

    private ClientSmoke() {
    }

    public static boolean start() {
        if (!ENABLED) {
            return false;
        }
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(TIMEOUT_MS);
                fail("timed out");
            } catch (InterruptedException ignored) {
            }
        }, "smoketest-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        return true;
    }

    public static void tick(Minecraft mc) {
        try {
            switch (stage) {
                case 0 -> {
                    Screen screen = SmokeCompat.screen(mc);
                    if (screen != null && SmokeCompat.overlayHidden(mc) && ++ticks >= 20) {
                        if (screen.getClass().getSimpleName().equals("LoadingErrorScreen")) {
                            fail("mod loading errors");
                        }
                        ticks = 0;
                        mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                        SmokeCompat.createFlatWorld(mc, GameType.CREATIVE);
                        stage = 1;
                    }
                }
                case 1 -> {
                    if (mc.level != null && mc.player != null && SmokeCompat.screen(mc) == null) {
                        stage = 2;
                    }
                }
                case 2 -> {
                    ticks++;
                    if (ticks == 40) {
                        fire(mc);
                    }
                    if (ticks == 50) {
                        SmokeCompat.screenshot(mc, "volley.png");
                    }
                    if (ticks >= 120) {
                        if (mc.level == null || mc.player == null) {
                            fail("left the world");
                        }
                        Files.writeString(mc.gameDirectory.toPath().resolve("smoke-ok"), "ok\n");
                        stage = 3;
                        mc.stop();
                    }
                }
                default -> {
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            fail(t.toString());
        }
    }

    // a stone wall 30 blocks ahead and 32 TNT flying at it at 30 blocks per tick
    private static void fire(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            var player = server.getPlayerList().getPlayers().get(0);
            var level = server.overworld();
            var base = player.blockPosition();
            for (int x = -12; x <= 12; x++)
                for (int y = 0; y < 16; y++)
                    for (int z = 30; z < 34; z++)
                        level.setBlockAndUpdate(base.offset(x, y, z), Blocks.STONE.defaultBlockState());
            for (int i = 0; i < 32; i++) {
                //? if >=26 {
                /*PrimedTnt tnt = (PrimedTnt) net.minecraft.world.entity.EntityTypes.TNT.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                *///?} else if >=1.21.11 {
                /*PrimedTnt tnt = EntityType.TNT.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                *///?} else
                PrimedTnt tnt = EntityType.TNT.create(level);
                tnt.setPos(base.getX() + (i % 8) - 3.5, base.getY() + 2 + i / 8, base.getZ() + 2.5);
                tnt.setDeltaMovement((i % 8) - 3.5, 0.4, 30);
                tnt.setFuse(30);
                level.addFreshEntity(tnt);
            }
            System.out.println("[clientsmoke] fired 32 TNT at 30 blocks per tick");
        });
    }

    public static void fail(String why) {
        System.err.println("Smoke test failed: " + why);
        System.out.println("[clientsmoke] FAILED: " + why);
        System.err.flush();
        Runtime.getRuntime().halt(1);
    }
}
