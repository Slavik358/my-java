import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class Main {
    // Твои данные. Замени текст в кавычках на свои!
    static String name = "Коротченя Вячеслав Романович";
    static String group = "477";

    // Замени число 10 на свой номер в журнале
    static int number = 11;
    static double pi = 0;

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < 10000000; i++) {
            if (i % 2 == 0) {
                pi += 1.0 / (2 * i + 1);
            } else {
                pi -= 1.0 / (2 * i + 1);
            }
        }
        pi = pi * 4;
        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);

        server.createContext("/", exchange -> {
            String response = "<h1>" + name + "</h1>" +
                    "<h2>" + group + "</h2>" +
                    "<p>Число Пи: " + String.format("%." + journalNumber + "f", pi) + "</p>";

            byte[] bytes = response.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, bytes.length());
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        });

        server.start();
    }
}