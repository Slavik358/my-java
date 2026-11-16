import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.*;

public class Main {
    static String fio = "Коротченя Вячеслав Романович";
    static String gruppa = "477";
    static String dbUrl = "jdbc:sqlserver://localhost:1433;databaseName=bbmo_01_23;encrypt=false;trustServerCertificate=true;";
    static String dbUser = "student";
    static String dbPass = "password123";

    static class ProstoyPool {
        List<Connection> svobodnye = new ArrayList<>();
        public Connection poluchit() throws SQLException {
            if (svobodnye.isEmpty()) {
                return DriverManager.getConnection(dbUrl, dbUser, dbPass);
            }
            return svobodnye.remove(0);
        }
        public void osvobodit(Connection c) {
            svobodnye.add(c);
        }
        public int razmer() {
            return svobodnye.size();
        }
    }
    static ProstoyPool pool = new ProstoyPool();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);
        server.createContext("/task1/single", new Task1SingleHandler());
        server.createContext("/task1/pool", new Task1PoolHandler());
        server.createContext("/task1/error", new Task1ErrorHandler());
        server.createContext("/task2/create", new Task2CreateHandler());
        server.createContext("/task2/insert", new Task2InsertHandler());
        server.createContext("/task2/select", new Task2SelectHandler());
        server.createContext("/task2/update", new Task2UpdateHandler());
        server.createContext("/task2/delete", new Task2DeleteHandler());
        server.createContext("/task2/injection", new Task2InjectionHandler());
        server.createContext("/task3/errors", new Task3ErrorsHandler());
        server.createContext("/task3/stream", new Task3StreamHandler());
        server.createContext("/task3/transaction/commit", new Task3TransactionCommitHandler());
        server.createContext("/task3/transaction/rollback", new Task3TransactionRollbackHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server zapushen na portu 3000. Gruppa: " + gruppa);
    }

    static String ekranirovat(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] baiti = json.getBytes("UTF-8");
        ex.sendResponseHeaders(code, baiti.length);
        ex.getResponseBody().write(baiti);
        ex.getResponseBody().close();
    }

    static String mapaVJson(Map<String, String> mapa) {
        StringBuilder sb = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, String> entry : mapa.entrySet()) {
            if (count > 0) sb.append(",");
            sb.append("\"").append(ekranirovat(entry.getKey())).append("\":\"").append(ekranirovat(entry.getValue())).append("\"");
            count++;
        }
        sb.append("}");
        return sb.toString();
    }

    static class Task1SingleHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            try {
                Connection c = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT @@VERSION, DB_NAME(), SUSER_SNAME()");
                rs.next();
                String versiya = ekranirovat(rs.getString(1).substring(0, 30));
                String db = ekranirovat(rs.getString(2));
                String user = ekranirovat(rs.getString(3));
                String otvet = "{\"status\":\"ok\",\"version\":\"" + versiya + "\",\"db\":\"" + db + "\",\"user\":\"" + user + "\",\"msg\":\"Soedinenie zakryto\"}";
                c.close();
                sendJson(ex, 200, otvet);
            } catch (Exception e) {
                sendJson(ex, 500, "{\"status\":\"error\",\"msg\":\"" + ekranirovat(e.getMessage()) + "\"}");
            }
        }
    }

    static class Task1PoolHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            try {
                Connection c = pool.poluchit();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT @@VERSION, DB_NAME()");
                rs.next();
                String versiya = ekranirovat(rs.getString(1).substring(0, 30));
                String db = ekranirovat(rs.getString(2));
                String otvet = "{\"status\":\"ok\",\"version\":\"" + versiya + "\",\"db\":\"" + db + "\",\"pool_size\":" + (pool.razmer() + 1) + ",\"msg\":\"Pul gotov\"}";
                pool.osvobodit(c);
                sendJson(ex, 200, otvet);
            } catch (Exception e) {
                sendJson(ex, 500, "{\"status\":\"error\",\"msg\":\"" + ekranirovat(e.getMessage()) + "\"}");
            }
        }
    }

    static class Task1ErrorHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            try {
                DriverManager.getConnection("jdbc:sqlserver://localhost:1433;databaseName=bad_db;encrypt=false;", "student", "nepravilniy_parol");
            } catch (SQLException e) {
                String kod = ekranirovat(e.getSQLState());
                String msg = ekranirovat(e.getMessage());
                sendJson(ex, 401, "{\"status\":\"error\",\"code\":\"" + kod + "\",\"msg\":\"" + msg + "\"}");
            }
        }
    }

    static class Task2CreateHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            try {
                Connection c = pool.poluchit();
                Statement st = c.createStatement();
                st.execute("IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='students' and xtype='U') " +
                        "CREATE TABLE students (" +
                        "id INT IDENTITY(1,1) PRIMARY KEY, " +
                        "name VARCHAR(100) NOT NULL, " +
                        "group_name VARCHAR(20) NOT NULL, " +
                        "course INT NOT NULL, " +
                        "grade DECIMAL(3,2), " +
                        "created_at DATETIME DEFAULT GETDATE()" +
                        ")");
                pool.osvobodit(c);
                sendJson(ex, 200, "{\"status\":\"ok\",\"msg\":\"Tablitsa students sozdana\"}");
            } catch (Exception e) {
                sendJson(ex, 500, "{\"status\":\"error\",\"msg\":\"" + ekranirovat(e.getMessage()) + "\"}");
            }
        }
    }

    static class Task2InsertHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            try {
                Connection c = pool.poluchit();
                PreparedStatement ps = c.prepareStatement("INSERT INTO students (name, group_name, course, grade) OUTPUT INSERTED.id VALUES (?, ?, ?, ?)");
                ps.setString(1, "Ivanov Ivan");
                ps.setString(2, gruppa);
                ps.setInt(3, 2);
                ps.setDouble(4, 4.5);
                ResultSet rs = ps.executeQuery();
                rs.next();
                int id = rs.getInt("id");
                pool.osvobodit(c);
                sendJson(ex, 200, "{\"status\":\"ok\",\"id\":" + id + ",\"affected\":1}");
            } catch (Exception e) {
                sendJson(ex, 500, "{\"status\":\"error\",\"msg\":\"" + ekranirovat(e.getMessage()) + "\"}");
            }
        }
    }

    static class Task2SelectHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            try {
                Connection c = pool.poluchit();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT TOP 5 id, name, group_name, grade FROM students ORDER BY grade DESC");
                StringBuilder sb = new StringBuilder("[");
                int count = 0;
                while (rs.next()) {
                    if (count > 0) sb.append(",");
                    sb.append("{\"id\":").append(rs.getInt("id"));
                    sb.append(",\"name\":\"").append(ekranirovat(rs.getString("name"))).append("\"");
                    sb.append(",\"group\":\"").append(ekranirovat(rs.getString("group_name"))).append("\"");
                    sb.append(",\"grade\":").append(rs.getDouble("grade")).append("}");
                    count++;
                }
                sb.append("]");
                pool.osvobodit(c);
                sendJson(ex, 200, sb.toString());
            } catch (Exception e) {
                sendJson(ex, 500, "{\"status\":\"error\",\"msg\":\"" + ekranirovat(e.getMessage()) + "\"}");
            }
        }
    }

    static class Task2UpdateHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            try {
                Connection c = pool.poluchit();
                PreparedStatement ps = c.prepareStatement("UPDATE students SET grade = ? WHERE name = ?");
                ps.setDouble(1, 4.5);
                ps.setString(2, "Ivanov Ivan");
                int affected = ps.executeUpdate();
                pool.osvobodit(c);
                sendJson(ex, 200, "{\"status\":\"ok\",\"affected\":" + affected + "}");
            } catch (Exception e) {
                sendJson(ex, 500, "{\"status\":\"error\",\"msg\":\"" + ekranirovat(e.getMessage()) + "\"}");
            }
        }
    }

    static class Task2DeleteHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            try {
                Connection c = pool.poluchit();
                PreparedStatement ps = c.prepareStatement("DELETE FROM students WHERE name = 'Ivanov Ivan'");
                int affected = ps.executeUpdate();
                pool.osvobodit(c);
                sendJson(ex, 200, "{\"status\":\"ok\",\"affected\":" + affected + "}");
            } catch (Exception e) {
                sendJson(ex, 500, "{\"status\":\"error\",\"msg\":\"" + ekranirovat(e.getMessage()) + "\"}");
            }
        }
    }

    static class Task2InjectionHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            try {
                Connection c = pool.poluchit();
                String opasnayaStroka = "' OR '1'='1";
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT name FROM students WHERE name = '" + opasnayaStroka + "'");
                int count = 0;
                while(rs.next()) count++;

                PreparedStatement ps = c.prepareStatement("SELECT name FROM students WHERE name = ?");
                ps.setString(1, opasnayaStroka);
                ResultSet rsSafe = ps.executeQuery();
                int countSafe = 0;
                while(rsSafe.next()) countSafe++;

                pool.osvobodit(c);
                sendJson(ex, 200, "{\"status\":\"ok\",\"unsafe_count\":" + count + ",\"safe_count\":" + countSafe + ",\"msg\":\"Parametrizovanniy zapros bezopasen\"}");
            } catch (Exception e) {
                sendJson(ex, 500, "{\"status\":\"error\",\"msg\":\"" + ekranirovat(e.getMessage()) + "\"}");
            }
        }
    }

    static class Task3ErrorsHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            Map<String, String> errors = new HashMap<>();
            try {
                DriverManager.getConnection("jdbc:sqlserver://localhost:9999;databaseName=db;encrypt=false;", "u", "p");
            } catch (SQLException e) { errors.put("ECONNREFUSED", ekranirovat(e.getSQLState()) + ": " + ekranirovat(e.getMessage())); }

            try {
                Connection c = pool.poluchit();
                c.createStatement().executeQuery("SELECT * FRM students");
            } catch (SQLException e) { errors.put("SYNTAX_ERROR", ekranirovat(e.getSQLState()) + ": " + ekranirovat(e.getMessage())); }

            try {
                Connection c = pool.poluchit();
                c.createStatement().execute("INSERT INTO students (id, name, group_name, course) VALUES (1, 'Test', '477', 1)");
                c.createStatement().execute("INSERT INTO students (id, name, group_name, course) VALUES (1, 'Test', '477', 1)");
            } catch (SQLException e) { errors.put("DUP_ENTRY", ekranirovat(e.getSQLState()) + ": " + ekranirovat(e.getMessage())); }

            sendJson(ex, 200, "{\"status\":\"ok\",\"errors\":" + mapaVJson(errors) + "}");
        }
    }

    static class Task3StreamHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            try {
                Connection c = pool.poluchit();
                Statement st = c.createStatement();
                st.setFetchSize(1000);
                ResultSet rs = st.executeQuery("SELECT id, name FROM students");
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                pool.osvobodit(c);
                sendJson(ex, 200, "{\"status\":\"ok\",\"streamed_rows\":" + count + ",\"msg\":\"Cherez ResultSet (stream)\"}");
            } catch (Exception e) {
                sendJson(ex, 500, "{\"status\":\"error\",\"msg\":\"" + ekranirovat(e.getMessage()) + "\"}");
            }
        }
    }

    static class Task3TransactionCommitHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            Connection c = null;
            try {
                c = pool.poluchit();
                c.setAutoCommit(false);
                PreparedStatement ps1 = c.prepareStatement("INSERT INTO students (name, group_name, course) VALUES ('Trans1', '477', 1)");
                ps1.executeUpdate();
                PreparedStatement ps2 = c.prepareStatement("INSERT INTO students (name, group_name, course) VALUES ('Trans2', '477', 2)");
                ps2.executeUpdate();
                c.commit();
                sendJson(ex, 200, "{\"status\":\"ok\",\"msg\":\"Transaction committed\"}");
            } catch (Exception e) {
                try {
                    if (c != null) c.rollback();
                } catch (SQLException exRoll) {
                    exRoll.printStackTrace();
                }
                sendJson(ex, 500, "{\"status\":\"error\",\"msg\":\"Rollback: " + ekranirovat(e.getMessage()) + "\"}");
            } finally {
                if (c != null) {
                    try {
                        c.setAutoCommit(true);
                    } catch (SQLException exAuto) {
                        exAuto.printStackTrace();
                    }
                    pool.osvobodit(c);
                }
            }
        }
    }

    static class Task3TransactionRollbackHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            Connection c = null;
            try {
                c = pool.poluchit();
                c.setAutoCommit(false);
                PreparedStatement ps1 = c.prepareStatement("INSERT INTO students (name, group_name, course) VALUES ('Rollback1', '477', 1)");
                ps1.executeUpdate();
                PreparedStatement ps2 = c.prepareStatement("INSERT INTO students (id, name, group_name, course) VALUES (999999999, 'Rollback2', '477', 2)");
                ps2.executeUpdate();
                c.commit();
            } catch (Exception e) {
                try {
                    if (c != null) c.rollback();
                } catch (SQLException exRoll) {
                    exRoll.printStackTrace();
                }
                sendJson(ex, 200, "{\"status\":\"ok\",\"msg\":\"Transaction rolled back due to error: " + ekranirovat(e.getMessage()) + "\"}");
            } finally {
                if (c != null) {
                    try {
                        c.setAutoCommit(true);
                    } catch (SQLException exAuto) {
                        exAuto.printStackTrace();
                    }
                    pool.osvobodit(c);
                }
            }
        }
    }
}