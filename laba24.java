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

    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection(dbUrl, dbUser, dbPass); Statement st = c.createStatement()) {
            st.execute("IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='users' and xtype='U') " +
                    "CREATE TABLE users (id INT IDENTITY(1,1) PRIMARY KEY, name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) UNIQUE NOT NULL, group_name VARCHAR(20) NOT NULL, age INT NOT NULL, course INT NOT NULL)");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);
        server.createContext("/api/users", new UsersHandler());
        server.createContext("/api/users/search", new SearchHandler());
        server.createContext("/api/users/stats", new StatsHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server zapushen na portu 3000. Gruppa: " + gruppa);
    }

    static String ekranirovat(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("X-Group", gruppa);
        byte[] baiti = json.getBytes("UTF-8");
        ex.sendResponseHeaders(code, baiti.length);
        ex.getResponseBody().write(baiti);
        ex.getResponseBody().close();
    }

    static Map<String, String> parseJson(String json) {
        Map<String, String> res = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
        for (String part : json.split(",")) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) res.put(kv[0].trim().replace("\"", ""), kv[1].trim().replace("\"", ""));
        }
        return res;
    }

    static class UsersHandler implements HttpHandler {
        public void handle(HttpExchange ex) {
            try {
                String method = ex.getRequestMethod();
                String path = ex.getRequestURI().getPath();
                String[] parts = path.split("/");

                if (method.equals("GET") && parts.length == 3) handleGetAll(ex, ex.getRequestURI().getQuery());
                else if (method.equals("GET") && parts.length == 4) handleGetById(ex, parts[3]);
                else if (method.equals("POST") && parts.length == 3) handleCreate(ex);
                else if (method.equals("PUT") && parts.length == 4) handleUpdate(ex, parts[3]);
                else if (method.equals("DELETE") && parts.length == 4) handleDelete(ex, parts[3]);
                else sendJson(ex, 404, "{\"error\":\"Not found\"}");
            } catch (Exception e) {
                try { sendJson(ex, 500, "{\"error\":\"" + ekranirovat(e.getMessage()) + "\"}"); } catch (IOException io) {}
            }
        }

        void handleGetAll(HttpExchange ex, String query) throws Exception {
            int page = 1, limit = 10;
            String groupFilter = null, sort = "id ASC";
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=");
                    if (kv.length == 2) {
                        if (kv[0].equals("page")) page = Integer.parseInt(kv[1]);
                        if (kv[0].equals("limit")) limit = Integer.parseInt(kv[1]);
                        if (kv[0].equals("group_name")) groupFilter = kv[1];
                        if (kv[0].equals("sort")) sort = kv[1].startsWith("-") ? kv[1].substring(1) + " DESC" : kv[1] + " ASC";
                    }
                }
            }
            int offset = (page - 1) * limit;

            try (Connection c = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                // ИСПРАВЛЕНО: Безопасный подсчёт total
                int total = 0;
                String sqlCount = "SELECT COUNT(*) FROM users" + (groupFilter != null ? " WHERE group_name = ?" : "");
                try (PreparedStatement psCount = c.prepareStatement(sqlCount)) {
                    if (groupFilter != null) psCount.setString(1, groupFilter);
                    try (ResultSet rsCount = psCount.executeQuery()) {
                        if (rsCount.next()) {
                            total = rsCount.getInt(1);
                        }
                    }
                }

                String sql = "SELECT id, name, email, group_name, age, course FROM users" +
                        (groupFilter != null ? " WHERE group_name = ?" : "") +
                        " ORDER BY " + sort + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    int paramIdx = 1;
                    if (groupFilter != null) ps.setString(paramIdx++, groupFilter);
                    ps.setInt(paramIdx++, offset);
                    ps.setInt(paramIdx, limit);

                    try (ResultSet rs = ps.executeQuery()) {
                        StringBuilder sb = new StringBuilder("{\"data\":[");
                        int count = 0;
                        while (rs.next()) {
                            if (count > 0) sb.append(",");
                            sb.append("{\"id\":").append(rs.getInt("id"))
                                    .append(",\"name\":\"").append(ekranirovat(rs.getString("name"))).append("\"")
                                    .append(",\"email\":\"").append(ekranirovat(rs.getString("email"))).append("\"")
                                    .append(",\"group_name\":\"").append(ekranirovat(rs.getString("group_name"))).append("\"")
                                    .append(",\"age\":").append(rs.getInt("age"))
                                    .append(",\"course\":").append(rs.getInt("course")).append("}");
                            count++;
                        }
                        sb.append("],\"pagination\":{\"total\":").append(total).append(",\"page\":").append(page)
                                .append(",\"limit\":").append(limit).append(",\"pages\":").append((int) Math.ceil((double) total / limit)).append("}}");
                        sendJson(ex, 200, sb.toString());
                    }
                }
            }
        }

        void handleGetById(HttpExchange ex, String idStr) throws Exception {
            try (Connection c = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                try (PreparedStatement ps = c.prepareStatement("SELECT id, name, email, group_name, age, course FROM users WHERE id = ?")) {
                    ps.setInt(1, Integer.parseInt(idStr));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String json = "{\"data\":{\"id\":" + rs.getInt("id") + ",\"name\":\"" + ekranirovat(rs.getString("name")) +
                                    "\",\"email\":\"" + ekranirovat(rs.getString("email")) + "\",\"group_name\":\"" +
                                    ekranirovat(rs.getString("group_name")) + "\",\"age\":" + rs.getInt("age") +
                                    ",\"course\":" + rs.getInt("course") + "}}";
                            sendJson(ex, 200, json);
                        } else {
                            sendJson(ex, 404, "{\"error\":\"User not found\",\"status\":404}");
                        }
                    }
                }
            }
        }

        void handleCreate(HttpExchange ex) throws Exception {
            String body = new String(ex.getRequestBody().readAllBytes(), "UTF-8");
            Map<String, String> data = parseJson(body);
            String name = data.get("name"), email = data.get("email"), group_name = data.get("group_name");
            int age = Integer.parseInt(data.get("age")), course = Integer.parseInt(data.get("course"));

            if (name == null || name.length() < 2) throw new Exception("name min 2 symbols");
            if (email == null || !email.contains("@")) throw new Exception("invalid email");
            if (group_name == null) throw new Exception("group_name required");
            if (age < 16 || age > 100) throw new Exception("age must be 16-100");

            try (Connection c = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                try (PreparedStatement psCheck = c.prepareStatement("SELECT id FROM users WHERE email = ?")) {
                    psCheck.setString(1, email);
                    try (ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            sendJson(ex, 409, "{\"error\":\"Email already exists\",\"status\":409}");
                            return;
                        }
                    }
                }

                String safeName = name.replace("'", "''");
                String safeEmail = email.replace("'", "''");
                String safeGroup = group_name.replace("'", "''");
                String sql = "INSERT INTO users (name, email, group_name, age, course) OUTPUT INSERTED.id VALUES (" +
                        "'" + safeName + "', '" + safeEmail + "', '" + safeGroup + "', " + age + ", " + course + ")";

                // ИСПРАВЛЕНО: Безопасное получение INSERTED.id
                int newId = -1;
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    if (rs.next()) {
                        newId = rs.getInt("id");
                    } else {
                        throw new Exception("Failed to get inserted ID");
                    }
                }

                sendJson(ex, 201, "{\"data\":{\"id\":" + newId + ",\"name\":\"" + ekranirovat(name) +
                        "\",\"email\":\"" + ekranirovat(email) + "\",\"group_name\":\"" + ekranirovat(group_name) +
                        "\",\"age\":" + age + ",\"course\":" + course + "}}");
            }
        }

        void handleUpdate(HttpExchange ex, String idStr) throws Exception {
            int id = Integer.parseInt(idStr);
            try (Connection c = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                try (PreparedStatement psCheck = c.prepareStatement("SELECT name, email, group_name, age, course FROM users WHERE id = ?")) {
                    psCheck.setInt(1, id);
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (!rs.next()) {
                            sendJson(ex, 404, "{\"error\":\"User not found\",\"status\":404}");
                            return;
                        }

                        String body = new String(ex.getRequestBody().readAllBytes(), "UTF-8");
                        Map<String, String> data = parseJson(body);
                        String name = data.getOrDefault("name", rs.getString("name"));
                        String email = data.getOrDefault("email", rs.getString("email"));
                        String group_name = data.getOrDefault("group_name", rs.getString("group_name"));
                        int age = data.containsKey("age") ? Integer.parseInt(data.get("age")) : rs.getInt("age");
                        int course = data.containsKey("course") ? Integer.parseInt(data.get("course")) : rs.getInt("course");

                        if (!email.equals(rs.getString("email"))) {
                            try (PreparedStatement psEmail = c.prepareStatement("SELECT id FROM users WHERE email = ?")) {
                                psEmail.setString(1, email);
                                try (ResultSet rsEmail = psEmail.executeQuery()) {
                                    if (rsEmail.next()) {
                                        sendJson(ex, 409, "{\"error\":\"Email already exists\",\"status\":409}");
                                        return;
                                    }
                                }
                            }
                        }

                        try (PreparedStatement ps = c.prepareStatement("UPDATE users SET name=?, email=?, group_name=?, age=?, course=? WHERE id=?")) {
                            ps.setString(1, name); ps.setString(2, email); ps.setString(3, group_name);
                            ps.setInt(4, age); ps.setInt(5, course); ps.setInt(6, id);
                            ps.executeUpdate();
                        }

                        sendJson(ex, 200, "{\"data\":{\"id\":" + id + ",\"name\":\"" + ekranirovat(name) + "\",\"email\":\"" + ekranirovat(email) +
                                "\",\"group_name\":\"" + ekranirovat(group_name) + "\",\"age\":" + age + ",\"course\":" + course + "}}");
                    }
                }
            }
        }

        void handleDelete(HttpExchange ex, String idStr) throws Exception {
            try (Connection c = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    ps.setInt(1, Integer.parseInt(idStr));
                    if (ps.executeUpdate() > 0) sendJson(ex, 200, "{\"data\":{\"deleted\":true,\"id\":" + idStr + "}}");
                    else sendJson(ex, 404, "{\"error\":\"User not found\",\"status\":404}");
                }
            }
        }
    }

    static class SearchHandler implements HttpHandler {
        public void handle(HttpExchange ex) {
            try {
                String query = ex.getRequestURI().getQuery();
                String q = "";
                if (query != null && query.startsWith("q=")) q = query.substring(2);

                List<String> found = new ArrayList<>();
                try (Connection c = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                    try (PreparedStatement ps = c.prepareStatement("SELECT id, name, email FROM users WHERE name LIKE ? OR email LIKE ?")) {
                        ps.setString(1, "%" + q + "%");
                        ps.setString(2, "%" + q + "%");
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                found.add("{\"id\":" + rs.getInt("id") + ",\"name\":\"" + ekranirovat(rs.getString("name")) + "\",\"email\":\"" + ekranirovat(rs.getString("email")) + "\"}");
                            }
                        }
                    }
                }

                StringBuilder sb = new StringBuilder();
                sb.append("{\"found\":").append(found.size()).append(",\"data\":[");
                for (int i = 0; i < found.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(found.get(i));
                }
                sb.append("]}");
                sendJson(ex, 200, sb.toString());
            } catch (Exception e) {
                try { sendJson(ex, 500, "{\"error\":\"" + ekranirovat(e.getMessage()) + "\"}"); } catch (IOException io) {}
            }
        }
    }

    static class StatsHandler implements HttpHandler {
        public void handle(HttpExchange ex) {
            try {
                int total = 0;
                double avgAge = 0.0;
                try (Connection c = DriverManager.getConnection(dbUrl, dbUser, dbPass); Statement st = c.createStatement()) {
                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) as total, AVG(CAST(age AS FLOAT)) as avg_age FROM users")) {
                        // ИСПРАВЛЕНО: Безопасная проверка наличия строки
                        if (rs.next()) {
                            total = rs.getInt("total");
                            avgAge = rs.getDouble("avg_age");
                            if (rs.wasNull()) avgAge = 0.0; // Защита от NULL, если таблица пустая
                        }
                    }
                }
                String json = "{\"data\":{\"total\":" + total + ",\"averageAge\":" + avgAge + "}}";
                sendJson(ex, 200, json);
            } catch (Exception e) {
                try { sendJson(ex, 500, "{\"error\":\"" + ekranirovat(e.getMessage()) + "\"}"); } catch (IOException io) {}
            }
        }
    }
}