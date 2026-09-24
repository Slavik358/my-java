import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Server extends NanoHTTPD {

    static List<Map<String, String>> users = new ArrayList<>();
    static AtomicInteger userIdCounter = new AtomicInteger(1);

    public Server() throws IOException {
        super(3000);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        long start = System.currentTimeMillis();

        try {
            if (uri.equals("/")) {
                return handleRoot();
            } else if (uri.equals("/api/users") && method.equals(Method.GET)) {
                return handleGetUsers();
            } else if (uri.equals("/api/users") && method.equals(Method.POST)) {
                return handleCreateUser(session);
            } else if (uri.startsWith("/api/users/") && method.equals(Method.GET)) {
                String id = uri.replace("/api/users/", "");
                return handleGetUser(id);
            } else if (uri.startsWith("/api/users/") && method.equals(Method.PUT)) {
                String id = uri.replace("/api/users/", "");
                return handleUpdateUser(id, session);
            } else if (uri.startsWith("/api/users/") && method.equals(Method.DELETE)) {
                String id = uri.replace("/api/users/", "");
                return handleDeleteUser(id);
            } else if (uri.equals("/protected")) {
                return handleProtected(session);
            } else if (uri.equals("/error")) {
                throw new RuntimeException("тут ошибка!");
            } else {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
            }
        } finally {
            long ms = System.currentTimeMillis() - start;
            System.out.println("[" + new Date() + "] " + method + " " + uri + " - " + ms + "ms");
        }
    }

    private Response handleRoot() {
        String html = "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'><title>LR 15</title></head>" +
                "<body style='font-family:Arial;margin:40px;'>" +
                "<h1>Лабораторная работа №15</h1>" +
                "<div style='background:#e3f2fd;padding:15px;border-radius:5px;'>" +
                "<p><b>Студент:</b> Коротченя Вячеслав Романович</p>" +
                "<p><b>Группа:</b> 477</p>" +
                "<p><b>Вариант:</b> 11</p>" +
                "<p><b>Дата:</b> " + new Date() + "</p>" +
                "</div>" +
                "<p style='color:green;font-size:18px;'>Сервер rabotaet!</p>" +
                "</body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html);
    }

    private Response handleGetUsers() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) json.append(",");
            json.append("{\"id\":").append(users.get(i).get("id"));sss
            json.append(",\"name\":\"").append(users.get(i).get("name")).append("\"");
            json.append(",\"group\":\"").append(users.get(i).get("group")).append("\"}");
        }
        json.append("]");
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
    }

    private Response handleCreateUser(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);

            String body = "";
            for (Map.Entry<String, String> entry : files.entrySet()) {
                body = entry.getValue();
            }

            if (body.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json",
                        "{\"error\":\"Pustoe telo zaprosa\",\"status\":400}");
            }

            String name = extractJsonField(body, "name");
            String group = extractJsonField(body, "group");

            if (name == null || group == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json",
                        "{\"error\":\"не укаано\",\"status\":400}");
            }

            Map<String, String> newUser = new HashMap<>();
            int id = userIdCounter.getAndIncrement();
            newUser.put("id", String.valueOf(id));
            newUser.put("name", name);
            newUser.put("group", group);
            users.add(newUser);

            String response = "{\"id\":" + id + ",\"name\":\"" + name + "\",\"group\":\"" + group + "\"}";
            return newFixedLengthResponse(Response.Status.CREATED, "application/json", response);

        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    "{\"error\":\"" + e.getMessage() + "\",\"status\":500}");
        }
    }

    private Response handleGetUser(String id) {
        for (Map<String, String> user : users) {
            if (user.get("id").equals(id)) {
                String json = "{\"id\":" + user.get("id") + ",\"name\":\"" + user.get("name") +
                        "\",\"group\":\"" + user.get("group") + "\"}";
                return newFixedLengthResponse(Response.Status.OK, "application/json", json);
            }
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json",
                "{\"error\":\"нету польователя\",\"status\":404}");
    }

    private Response handleUpdateUser(String id, IHTTPSession session) {
        for (Map<String, String> user : users) {
            if (user.get("id").equals(id)) {
                try {
                    Map<String, String> files = new HashMap<>();
                    session.parseBody(files);
                    String body = "";
                    for (Map.Entry<String, String> entry : files.entrySet()) {
                        body = entry.getValue();
                    }

                    String name = extractJsonField(body, "name");
                    String group = extractJsonField(body, "group");

                    if (name != null) user.put("name", name);
                    if (group != null) user.put("group", group);

                    String json = "{\"id\":" + user.get("id") + ",\"name\":\"" + user.get("name") +
                            "\",\"group\":\"" + user.get("group") + "\"}";
                    return newFixedLengthResponse(Response.Status.OK, "application/json", json);

                } catch (Exception e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                            "{\"error\":\"" + e.getMessage() + "\",\"status\":500}");
                }
            }
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json",
                "{\"error\":\"нету\",\"status\":404}");
    }

    private Response handleDeleteUser(String id) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).get("id").equals(id)) {
                users.remove(i);
                return newFixedLengthResponse(Response.Status.OK, "application/json",
                        "{\"message\":\"Polzovatel udalen\",\"id\":" + id + "}");
            }
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json",
                "{\"error\":\"нету\",\"status\":404}");
    }

    private Response handleProtected(IHTTPSession session) {
        String auth = session.getHeaders().get("authorization");
        if (auth == null || auth.isEmpty()) {
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json",
                    "{\"error\":\"надо авториация\",\"status\":401}");
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json",
                "{\"message\":\"доступ есть!\",\"user\":\"Коротченя Вячеслав\",\"group\":\"477\"}");
    }

    private String extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("опен http://localhost:3000");
        } catch (IOException e) {
            System.err.println("Oshybka zapuska servera: " + e.getMessage());
            e.printStackTrace();
        }
    }
}