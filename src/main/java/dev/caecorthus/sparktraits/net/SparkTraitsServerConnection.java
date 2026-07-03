package dev.caecorthus.sparktraits.net;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * Tracks whether the current client connection was confirmed by a SparkTraits server login query.
 * 记录当前客户端连接是否已经通过 SparkTraits 服务端登录查询确认。
 */
public final class SparkTraitsServerConnection {
    private static volatile boolean confirmedServer;

    private SparkTraitsServerConnection() {
    }

    public static void confirmServer() {
        confirmedServer = true;
    }

    public static void reset() {
        confirmedServer = false;
    }

    public static boolean isConfirmedServer() {
        return confirmedServer;
    }

    public static boolean isUnconfirmedClientWorld(World world) {
        return world != null && world.isClient() && !confirmedServer;
    }

    public static boolean isUnconfirmedClientEntity(Entity entity) {
        return entity != null && isUnconfirmedClientWorld(entity.getWorld());
    }
}
