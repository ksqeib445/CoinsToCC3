package fun.ksmc.RRCC;

import com.greatmancode.craftconomy3.Cause;
import com.greatmancode.craftconomy3.Common;
import com.greatmancode.craftconomy3.account.Account;
import com.greatmancode.craftconomy3.currency.Currency;
import com.greatmancode.craftconomy3.tools.interfaces.Loader;
import com.ksqeib.ksapi.manager.MysqlPoolManager;
import com.ksqeib.ksapi.mysql.ConnectionPool;
import com.ksqeib.ksapi.mysql.MysqlConnectobj;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static com.ksqeib.ksapi.KsAPI.um;

public class CTC extends JavaPlugin implements Listener {
    ConnectionPool connectionPool;
    String tablename = "Coins_Data";
    Plugin plugin;
    Common craftconomy;


    @Override
    public void onEnable() {
        super.onEnable();
        plugin = Bukkit.getPluginManager().getPlugin("Craftconomy3");
        if (plugin != null) {
            craftconomy = (Common) ((Loader) plugin).getCommon();
        }
        String url = "jdbc:mysql://localhost/Kingsworld?autoReconnect=true&useUnicode=true&amp&characterEncoding=UTF-8&useSSL=false";
        MysqlConnectobj mysqlConnectobj = new MysqlConnectobj(url, "password", "root");
        connectionPool = MysqlPoolManager.getPool(mysqlConnectobj);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        UUID uuid = e.getPlayer().getUniqueId();
        String name = e.getPlayer().getName();
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            if (!p.isOnline()) return;
            if (!has(uuid.toString())) return;
            Currency currency = Common.getInstance().getCurrencyManager().getDefaultCurrency();
            String worldName = Account.getWorldGroupOfPlayerCurrentlyIn(name);
            double many = getMoney(uuid.toString());
            if (many == -1) return;
            craftconomy.getAccountManager().getAccount(name, false).deposit(many, worldName, currency.getName(), Cause.CONVERT, "导入");
            del(uuid.toString());
            p.sendMessage("已成功为您导入" + many + "金钱");
        }, 20L);
    }

    public void del(String key) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = this.createConnection();
            pstmt = conn.prepareStatement(String.format("DELETE FROM %s WHERE uuid = ?", this.tablename));
            pstmt.setString(1, key);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closePreparedStatement(pstmt);
            closeConnection(conn);
        }

    }

    public boolean has(String key) {
        boolean result = false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = connectionPool.getConnection();
//            LIMIT 1
            pstmt = conn.prepareStatement(String.format("SELECT nick FROM %s WHERE uuid = ? LIMIT 1", this.tablename));
            pstmt.setString(1, key);
            rs = pstmt.executeQuery();
            result = rs.next();
        } catch (SQLException var15) {
            var15.printStackTrace();
        } finally {
            closeResultSet(rs);
            closePreparedStatement(pstmt);
            closeConnection(conn);
        }

        return result;
    }

    public Connection createConnection() {
        return this.connectionPool.getConnection();
    }

    public double getMoney(String by) {
        Connection con = null;
        String ret = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = createConnection();
            ps = con.prepareStatement("SELECT balance FROM " + tablename + " WHERE uuid = ? LIMIT 1");
            ps.setString(1, by);
            rs = ps.executeQuery();

            while (rs.next()) {
                InputStream input = rs.getBinaryStream(1);
                if (input != null) {
                    InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    ret = br.readLine();
                    closeBufferedReader(br);
                    closeInputStreamReader(isr);
                }
                closeInputStream(input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResultSet(rs);
            closePreparedStatement(ps);
            closeConnection(con);
        }
        return ret == null ? -1 : Double.parseDouble(ret);
    }

    public static void closeInputStreamReader(InputStreamReader inputStreamReader) {
        if (inputStreamReader != null) {
            try {
                inputStreamReader.close();
            } catch (IOException e) {
                e.printStackTrace();
                um.getTip().send("InputStreamReader关闭失败", Bukkit.getConsoleSender(), null);
            }
        }
    }

    public static void closeBufferedReader(BufferedReader bufferedReader) {
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
                um.getTip().send("BufferedReader关闭失败", Bukkit.getConsoleSender(), null);
            }
        }
    }

    public static void closeInputStream(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                um.getTip().send("inputStream关闭失败", Bukkit.getConsoleSender(), null);
            }
        }
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
                um.getTip().send("数据库连接关闭失败", Bukkit.getConsoleSender(), null);
            }
        }
    }

    public static void closePreparedStatement(PreparedStatement preparedStatement) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                um.getTip().send("preparedStatement关闭失败", Bukkit.getConsoleSender(), null);
            }
        }
    }

    public static void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
                um.getTip().send("ResultSet关闭失败", Bukkit.getConsoleSender(), null);
            }
        }
    }


}
