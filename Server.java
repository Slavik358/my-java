import fi.iki.elonen.NanoHTTPD;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server extends NanoHTTPD {

    static List<Map<String, Object>> knigi = new ArrayList<>();
    static int knigaIdCounter = 1;
    static Map<String, AtomicInteger> zaprosyPoIP = new ConcurrentHashMap<>();
    static int limitZaprosov = 100;

    public Server() throws IOException {
        super(3000);
        dobavitTestovyeKnigi();
    }

    private Response addCorsHeaders(Response response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        return response;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        long nachalo = System.currentTimeMillis();

        if (method.equals(Method.OPTIONS)) {
            return addCorsHeaders(newFixedLengthResponse(Response.Status.OK, "text/plain", ""));
        }

        try {
            if (!proveritLimit(session)) {
                logirovanie(session, 429, nachalo);
                return addCorsHeaders(newFixedLengthResponse(Response.Status.TOO_MANY_REQUESTS, "application/json",
                        "{\"error\":\"Слишком много запросов\",\"status\":429}"));
            }

            Response resp;
            int status = 200;

            if (uri.equals("/")) {
                resp = handleRoot();
            } else if (uri.equals("/about")) {
                resp = handleAbout();
            } else if (uri.equals("/contacts")) {
                resp = handleContacts();
            } else if (uri.equals("/error")) {
                status = 500;
                resp = handleError();
            } else if (uri.equals("/async-error")) {
                status = 500;
                resp = handleAsyncError();
            } else if (uri.equals("/api/books") || uri.startsWith("/api/books/")) {
                resp = handleBooks(session, uri, method);
            } else {
                status = 404;
                resp = newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json",
                        "{\"error\":\"Маршрут не найден\",\"status\":404}");
            }

            try {
                status = resp.getStatus().getRequestStatus();
            } catch (Exception e) {}

            logirovanie(session, status, nachalo);
            return addCorsHeaders(resp);

        } catch (Exception e) {
            logirovanie(session, 500, nachalo);
            return addCorsHeaders(newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    "{\"error\":\"" + e.getMessage() + "\",\"status\":500}"));
        }
    }

    private Response handleRoot() {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>LR 16</title></head>" +
                "<body style='font-family:Arial;margin:40px;'>" +
                "<h1>Лабораторная работа №16</h1>" +
                "<div style='background:#e3f2fd;padding:15px;border-radius:5px;'>" +
                "<p><b>Студент:</b> Коротченя Вячеслав Романович</p>" +
                "<p><b>Группа:</b> 477</p>" +
                "<p><b>Вариант:</b> 11</p>" +
                "<p><b>Дата:</b> " + LocalDateTime.now() + "</p>" +
                "</div>" +
                "<p style='color:green;font-size:18px;'>✅ Сервер работает!</p>" +
                "<h3>Доступные маршруты:</h3>" +
                "<ul>" +
                "<li><a href='/'>/</a> - главная</li>" +
                "<li><a href='/about'>/about</a> - о разработчике</li>" +
                "<li><a href='/contacts'>/contacts</a> - контакты</li>" +
                "<li><a href='/api/books'>/api/books</a> - список книг</li>" +
                "<li><a href='/error'>/error</a> - тест ошибки</li>" +
                "</ul></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html);
    }

    private Response handleAbout() {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>About</title></head>" +
                "<body style='font-family:Arial;margin:40px;'>" +
                "<h1>О разработчике</h1>" +
                "<p><b>ФИО:</b> Коротченя Вячеслав Романович</p>" +
                "<p><b>Группа:</b> 477</p>" +
                "<p><b>Вариант:</b> 11</p>" +
                "<p><b>Университет:</b> БТК</p>" +
                "</body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html);
    }

    private Response handleContacts() {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Contacts</title></head>" +
                "<body style='font-family:Arial;margin:40px;'>" +
                "<h1>Контакты</h1>" +
                "<p><b>Email:</b> 23072008slava@gmail.com</p>" +
                "<p><b>GitHub:</b> Slavik358</p>" +
                "<p><b>Телефон:</b> +375 29 123-45-67</p>" +
                "</body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html);
    }

    private Response handleBooks(IHTTPSession session, String uri, Method method) throws IOException, InterruptedException, ExecutionException {
        if (uri.equals("/api/books") && method.equals(Method.GET)) {
            String query = session.getQueryParameterString();
            if (query != null && query.startsWith("author=")) {
                String avtor = query.substring(7);
                List<Map<String, Object>> naydennye = new ArrayList<>();
                for (Map<String, Object> k : knigi) {
                    if (((String)k.get("author")).toLowerCase().contains(avtor.toLowerCase())) {
                        naydennye.add(k);
                    }
                }
                return newFixedLengthResponse(Response.Status.OK, "application/json", massivVJson(naydennye));
            }
            return newFixedLengthResponse(Response.Status.OK, "application/json", massivVJson(knigi));

        } else if (uri.startsWith("/api/books/") && method.equals(Method.GET)) {
            String idStr = uri.replace("/api/books/", "");
            int id = Integer.parseInt(idStr);
            Map<String, Object> naydennaya = null;
            for (Map<String, Object> k : knigi) {
                if ((int)k.get("id") == id) {
                    naydennaya = k;
                    break;
                }
            }
            if (naydennaya == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Книга не найдена\",\"status\":404}");
            }
            return newFixedLengthResponse(Response.Status.OK, "application/json", mapaVJson(naydennaya));

        } else if (uri.equals("/api/books") && method.equals(Method.POST)) {
            String telo = poluchitTelo(session);
            Map<String, Object> novaya = jsonVMapu(telo);
            if (!novaya.containsKey("title") || !novaya.containsKey("author")) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Не указаны title или author\",\"status\":400}");
            }
            novaya.put("id", knigaIdCounter++);
            if (!novaya.containsKey("year")) novaya.put("year", 2024);
            if (!novaya.containsKey("genre")) novaya.put("genre", "неизвестно");
            knigi.add(novaya);
            return newFixedLengthResponse(Response.Status.CREATED, "application/json", mapaVJson(novaya));

        } else if (uri.startsWith("/api/books/") && method.equals(Method.PUT)) {
            String idStr = uri.replace("/api/books/", "");
            int id = Integer.parseInt(idStr);
            Map<String, Object> obnavlyayemaya = null;
            for (Map<String, Object> k : knigi) {
                if ((int)k.get("id") == id) {
                    obnavlyayemaya = k;
                    break;
                }
            }
            if (obnavlyayemaya == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Книга не найдена\",\"status\":404}");
            }
            String telo = poluchitTelo(session);
            Map<String, Object> dannye = jsonVMapu(telo);
            if (dannye.containsKey("title")) obnavlyayemaya.put("title", dannye.get("title"));
            if (dannye.containsKey("author")) obnavlyayemaya.put("author", dannye.get("author"));
            if (dannye.containsKey("year")) obnavlyayemaya.put("year", dannye.get("year"));
            if (dannye.containsKey("genre")) obnavlyayemaya.put("genre", dannye.get("genre"));
            return newFixedLengthResponse(Response.Status.OK, "application/json", mapaVJson(obnavlyayemaya));

        } else if (uri.startsWith("/api/books/") && method.equals(Method.DELETE)) {
            String idStr = uri.replace("/api/books/", "");
            int id = Integer.parseInt(idStr);
            boolean udaleno = false;
            for (int i = 0; i < knigi.size(); i++) {
                if ((int)knigi.get(i).get("id") == id) {
                    knigi.remove(i);
                    udaleno = true;
                    break;
                }
            }
            if (!udaleno) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Книга не найдена\",\"status\":404}");
            }
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"message\":\"Книга удалена\",\"id\":" + id + "}");
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"Маршрут не найден\",\"status\":404}");
    }

    private Response handleError() {
        throw new RuntimeException("Это тестовая ошибка для проверки middleware!");
    }

    private Response handleAsyncError() throws InterruptedException, ExecutionException {
        CompletableFuture.runAsync(() -> {
            try { Thread.sleep(100); } catch (InterruptedException ex) {}
            throw new RuntimeException("Асинхронная ошибка!");
        }).get();
        return newFixedLengthResponse(Response.Status.OK, "text/plain", "ok");
    }

    private String poluchitTelo(IHTTPSession session) {
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (Exception e) {
            return "";
        }
        for (Map.Entry<String, String> entry : files.entrySet()) {
            return entry.getValue();
        }
        return "";
    }

    private boolean proveritLimit(IHTTPSession session) {
        String ip = session.getRemoteIpAddress();
        if (ip == null) ip = "127.0.0.1";
        AtomicInteger schetchik = zaprosyPoIP.computeIfAbsent(ip, k -> new AtomicInteger(0));
        schetchik.incrementAndGet();
        return schetchik.get() <= limitZaprosov;
    }

    private void logirovanie(IHTTPSession session, int status, long nachalo) {
        long ms = System.currentTimeMillis() - nachalo;
        String time = LocalDateTime.now().toString();
        System.out.println("[" + time + "] " + session.getMethod() + " " + session.getUri() + " " + status + " - " + ms + "ms");
    }

    private void dobavitTestovyeKnigi() {
        String[][] testovyeKnigi = {
                {"Война и мир", "Толстой", "1869", "роман"},
                {"Преступление и наказание", "Достоевский", "1866", "роман"},
                {"Мастер и Маргарита", "Булгаков", "1967", "роман"},
                {"1984", "Оруэлл", "1949", "антиутопия"},
                {"Дюна", "Герберт", "1965", "фантастика"}
        };
        for (String[] k : testovyeKnigi) {
            Map<String, Object> kniga = new HashMap<>();
            kniga.put("id", knigaIdCounter++);
            kniga.put("title", k[0]);
            kniga.put("author", k[1]);
            kniga.put("year", Integer.parseInt(k[2]));
            kniga.put("genre", k[3]);
            knigi.add(kniga);
        }
    }

    private String massivVJson(List<Map<String, Object>> massiv) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < massiv.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(mapaVJson(massiv.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String mapaVJson(Map<String, Object> mapa) {
        StringBuilder sb = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, Object> entry : mapa.entrySet()) {
            if (count > 0) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String) {
                sb.append("\"").append(val).append("\"");
            } else {
                sb.append(val);
            }
            count++;
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, Object> jsonVMapu(String json) {
        Map<String, Object> mapa = new HashMap<>();
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
        String[] pari = json.split(",");
        for (String par : pari) {
            String[] kv = par.split(":", 2);
            if (kv.length == 2) {
                String klyuch = kv[0].trim().replace("\"", "");
                String znachenie = kv[1].trim().replace("\"", "");
                try {
                    mapa.put(klyuch, Integer.parseInt(znachenie));
                } catch (NumberFormatException e) {
                    mapa.put(klyuch, znachenie);
                }
            }
        }
        return mapa;
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("Сервер запущен на порту 3000");
            System.out.println("Откройте http://localhost:3000");
        } catch (IOException e) {
            System.err.println("Ошибка запуска: " + e.getMessage());
            e.printStackTrace();
        }
    }
}