import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileWriter;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;


interface MyListener {
    void doSomething(Object d);
}

public class Main {

    static class EventEmitter {

        private Map<String, List<MyListener>> myMap = new HashMap<String, List<MyListener>>();

        public void on(String name, MyListener listener) {
            List<MyListener> list = myMap.get(name);
            if (list == null) {
                list = new ArrayList<MyListener>();
                myMap.put(name, list);
            }
            list.add(listener);
        }

        public void emit(String name, Object data) {
            List<MyListener> list = myMap.get(name);
            if (list != null) {

                int i = 0;
                while (i < list.size()) {
                    MyListener l = list.get(i);
                    l.doSomething(data);
                    i = i + 1;
                }
            }
        }
    }

    static void setupLogger(final EventEmitter emitter) {
        emitter.on("server:started", new MyListener() {
            public void doSomething(Object data) {
                writeLog("server:started - порт " + data);
            }
        });

        emitter.on("server:stopped", new MyListener() {
            public void doSomething(Object data) {
                writeLog("server:stopped - сервер умер");
            }
        });

        emitter.on("request:received", new MyListener() {
            public void doSomething(Object data) {
                Map<String, String> req = (Map<String, String>) data;
                String m = req.get("method");
                String u = req.get("url");
                writeLog("request:received - " + m + " " + u);
            }
        });
    }

    static void writeLog(String msg) {
        try {
            String timeStr = LocalDateTime.now().toString();
            String prefix = "[ВРЕМЯ] ";
            String actionStr = "действие: ";
            String fullMsg = prefix + timeStr + " " + actionStr + msg + "\n";

            FileWriter fw = new FileWriter("logs.txt", true);
            fw.write(fullMsg);
            fw.close();
            System.out.println("в файл записал: " + fullMsg);
        } catch (IOException e) {
            System.out.println("ошибка аписи: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        int portNumber = 3000;
        final EventEmitter app = new EventEmitter();

        setupLogger(app);

        app.on("server:started", new MyListener() {
            public void doSomething(Object data) {
                System.out.println("Сервер запустился на порту " + data);
            }
        });

        app.on("server:stopped", new MyListener() {
            public void doSomething(Object data) {
                System.out.println("Сервер остановлен");
            }
        });

        app.on("request:received", new MyListener() {
            public void doSomething(Object data) {
                Map<String, String> req = (Map<String, String>) data;
                System.out.println("пришел запрос: " + req.get("method") + " " + req.get("url"));
            }
        });

        final HttpServer server = HttpServer.create(new InetSocketAddress(portNumber), 0);

        server.createContext("/", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                Map<String, String> reqData = new HashMap<String, String>();

                String methodStr = exchange.getRequestMethod();
                String urlStr = exchange.getRequestURI().toString();

                reqData.put("method", methodStr);
                reqData.put("url", urlStr);

                app.emit("request:received", reqData);

                String responseText = "Hello from Event-Driven Server!";
                byte[] bytesArray = responseText.getBytes("UTF-8");

                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytesArray.length);

                OutputStream os = exchange.getResponseBody();
                os.write(bytesArray);
                os.close();
            }
        });

        server.start();
        app.emit("server:started", portNumber);
        System.out.println("Сервер работает, через 10 секунд он умрет");

        Thread stopTimer = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    System.out.println("ошибка");
                }
                server.stop(0);
                app.emit("server:stopped", null);
            }
        });
        stopTimer.start();
    }
}