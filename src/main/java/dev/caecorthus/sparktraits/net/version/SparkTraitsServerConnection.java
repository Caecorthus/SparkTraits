package dev.caecorthus.sparktraits.net.version;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import dev.caecorthus.sparktraits.SparkTraits;

/**
 * Tracks whether the current client connection was confirmed by a SparkTraits login or play query.
 * 记录当前客户端连接是否已经通过 SparkTraits 登录阶段或 play 阶段确认。
 */
public final class SparkTraitsServerConnection {
    private static volatile boolean confirmedServer;
    /*
     * True from a login-phase confirmation until that connection's play session starts. A login query always precedes
     * the JOIN of its own connection, so JOIN keeps a confirmation that arrived this way and drops any other one.
     * 从登录阶段确认起，到该连接的 play 会话开始为止为 true。登录查询必然早于本连接的 JOIN，
     * 因此 JOIN 保留这样得到的确认，并丢弃其他来源的旧确认。
     */
    private static volatile boolean loginConfirmationAwaitingJoin;

    private SparkTraitsServerConnection() {
    }

    public static void confirmServer() {
        confirmedServer = true;
    }

    /**
     * Login-phase confirmation of the connection that is being established; it survives that connection's JOIN.
     * 正在建立的连接在登录阶段的确认；它会保留到该连接的 JOIN 之后。
     */
    public static void confirmServerAtLogin() {
        loginConfirmationAwaitingJoin = true;
        confirmedServer = true;
    }

    /**
     * Client JOIN: a proxy backend switch starts a new play session without a DISCONNECT, so a confirmation from the
     * previous backend must not carry over. Only this connection's own login confirmation is kept; otherwise the new
     * backend's play confirmation packet (sent on every server-side JOIN) confirms again.
     * 客户端 JOIN：代理切换后端会在没有 DISCONNECT 的情况下开始新的 play 会话，上一个后端的确认不能沿用。
     * 只保留本连接自己的登录确认；否则由新后端的 play 确认包（服务端每次 JOIN 都会发送）重新确认。
     */
    public static void beginPlaySession() {
        if (!loginConfirmationAwaitingJoin) {
            confirmedServer = false;
        }
        loginConfirmationAwaitingJoin = false;
    }

    public static void reset() {
        confirmedServer = false;
        loginConfirmationAwaitingJoin = false;
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
