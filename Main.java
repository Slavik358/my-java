import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    static String fio = "Коротченя Вячеслав Романович";
    static String gruppa = "477";
    static int variant = 11;
    static long MAX_BODY_SIZE = 1048576;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);
        server.createContext("/", new Task1Handler());
        server.createContext("/headers", new Task2Handler());
        server.createContext("/echo", new Task3Handler());
        server.createContext("/form", new Task3Handler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server zapushen na portu 3000");
    }

    static void logRequest(HttpExchange ex, int status) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("[" + time + "] " + ex.getRequestMethod() + " " + ex.getRequestURI().getPath() + " " + status);
    }

    static void sendResponse(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] baiti = body.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        ex.getResponseHeaders().set("X-Group", gruppa);
        ex.sendResponseHeaders(code, baiti.length);
        OutputStream os = ex.getResponseBody();
        os.write(baiti);
        os.close();
    }

    static class Task1Handler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            int status = 200;
            String html = "";

            if (path.equals("/")) {
                html = "<h1>Лабораторная работа №20</h1><p>Группа: " + gruppa + "</p><p>Метод: " + ex.getRequestMethod() + "</p><p>URL: " + path + "</p>";
            } else if (path.equals("/about")) {
                html = "<h1>О студенте</h1><p>Группа: " + gruppa + "</p><p>Студент: " + fio + "</p>";
            } else {
                status = 404;
                html = "<h1>404 Not Found</h1><p>Путь " + path + " не найден</p>";
            }
            sendResponse(ex, status, "text/html", html);
            logRequest(ex, status);
        }
    }

    static class Task2Handler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            int status = 200;
            String response = "";

            if (path.equals("/headers")) {
                Map<String, Object> data = new HashMap<>();
                data.put("method", ex.getRequestMethod());
                data.put("url", ex.getRequestURI().toString());
                data.put("httpVersion", ex.getProtocol());
                Map<String, String> heads = new HashMap<>();
                for (Map.Entry<String, List<String>> entry : ex.getRequestHeaders().entrySet()) {
                    heads.put(entry.getKey().toLowerCase(), entry.getValue().get(0));
                }
                data.put("headers", heads);
                response = mapaVJson(data);
            } else if (path.equals("/headers/set")) {
                ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                ex.getResponseHeaders().set("X-Powered-By", "Java");
                ex.getResponseHeaders().set("X-Group", gruppa);
                ex.getResponseHeaders().set("Cache-Control", "no-cache");
                ex.sendResponseHeaders(200, 0);
                ex.getResponseBody().close();
                logRequest(ex, 200);
                return;
            } else if (path.equals("/headers/check")) {
                Map<String, Boolean> checks = new HashMap<>();
                checks.put("hasContentType", ex.getRequestHeaders().containsKey("Content-Type"));
                checks.put("hasXGroup", ex.getRequestHeaders().containsKey("X-Group"));
                checks.put("hasUnknown", ex.getRequestHeaders().containsKey("X-Unknown"));
                response = mapaVJson(checks);
            }
            sendResponse(ex, status, "application/json", response);
            logRequest(ex, status);
        }
    }

    static class Task3Handler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            int status = 200;
            String response = "";

            if (ex.getRequestMethod().equals("POST")) {
                InputStream is = ex.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                long totalRead = 0;

                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    totalRead += nRead;
                    if (totalRead > MAX_BODY_SIZE) {
                        status = 413;
                        response = "{\"error\":\"Payload Too Large\"}";
                        break;
                    }
                    buffer.write(data, 0, nRead);
                }
                is.close();

                if (status != 413) {
                    String telo = buffer.toString("UTF-8");
                    String contentType = ex.getRequestHeaders().getFirst("Content-Type");

                    if (path.equals("/echo")) {
                        if (contentType != null && contentType.contains("application/json")) {
                            telo = telo.replaceFirst("\\}$", ", \"group\":\"" + gruppa + "\"}");
                        }
                        response = telo;
                    } else if (path.equals("/form")) {
                        Map<String, String> formData = new HashMap<>();
                        String[] pairs = telo.split("&");
                        for (String pair : pairs) {
                            String[] kv = pair.split("=", 2);
                            if (kv.length == 2) {
                                formData.put(kv[0], kv[1]);
                            }
                        }
                        response = mapaVJson(formData);
                    }
                }
            } else {
                status = 405;
                response = "{\"error\":\"Method Not Allowed\"}";
            }
            sendResponse(ex, status, "application/json", response);
            logRequest(ex, status);
        }
    }

    static String mapaVJson(Map<String, ?> mapa) {
        StringBuilder sb = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, ?> entry : mapa.entrySet()) {
            if (count > 0) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String) {
                sb.append("\"").append(val).append("\"");
            } else if (val instanceof Map) {
                sb.append(mapaVJson((Map<String, ?>) val));
            } else if (val instanceof Boolean) {
                sb.append(val);
            } else {
                sb.append("\"").append(val).append("\"");
            }
            count++;
        }
        sb.append("}");
        return sb.toString();
    }
}