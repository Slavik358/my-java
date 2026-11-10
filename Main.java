import java.util.*;
import java.util.function.Consumer;

public class Main {
    static String fio = "Коротченя Вячеслав Романович";
    static String gruppa = "477";
    static int variant = 11;

    static class EventEmitter {
        Map<String, List<Consumer<Object[]>>> slushateli = new HashMap<>();
        Map<String, List<Consumer<Object[]>>> onceSlushateli = new HashMap<>();

        void on(String sobitie, Consumer<Object[]> callback) {
            slushateli.computeIfAbsent(sobitie, k -> new ArrayList<>()).add(callback);
        }

        void once(String sobitie, Consumer<Object[]> callback) {
            onceSlushateli.computeIfAbsent(sobitie, k -> new ArrayList<>()).add(callback);
        }

        boolean emit(String sobitie, Object... args) {
            boolean est = false;
            if (slushateli.containsKey(sobitie)) {
                List<Consumer<Object[]>> copy = new ArrayList<>(slushateli.get(sobitie));
                for (int i = 0; i < copy.size(); i++) {
                    copy.get(i).accept(args);
                    est = true;
                }
            }
            if (onceSlushateli.containsKey(sobitie)) {
                List<Consumer<Object[]>> copy = new ArrayList<>(onceSlushateli.get(sobitie));
                for (int i = 0; i < copy.size(); i++) {
                    copy.get(i).accept(args);
                    est = true;
                }
                onceSlushateli.remove(sobitie);
            }
            return est;
        }

        int listenerCount(String sobitie) {
            int count = 0;
            if (slushateli.containsKey(sobitie)) count += slushateli.get(sobitie).size();
            if (onceSlushateli.containsKey(sobitie)) count += onceSlushateli.get(sobitie).size();
            return count;
        }

        void removeListener(String sobitie, Consumer<Object[]> callback) {
            if (slushateli.containsKey(sobitie)) {
                slushateli.get(sobitie).remove(callback);
            }
        }

        void removeAllListeners(String sobitie) {
            slushateli.remove(sobitie);
            onceSlushateli.remove(sobitie);
        }
    }

    static class DatabaseConnection extends EventEmitter {
        void connect() {
            System.out.println("[EVENT] Подключение к БД");
            emit("connect");
        }

        void query(String sql) {
            System.out.println("[EVENT] Выполнение запроса: " + sql);
            emit("query", sql, variant + " записей");
        }

        void close() {
            System.out.println("[EVENT] Закрытие соединения");
            emit("close");
        }

        void triggerError() {
            System.out.println("[EVENT] Ошибка: Connection timeout");
            emit("error", "Connection timeout");
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            task1();
            System.out.println("\n-----------------------------------\n");
            task2();
            System.out.println("\n-----------------------------------\n");
            task3();
        } else if (args[0].equals("1")) {
            task1();
        } else if (args[0].equals("2")) {
            task2();
        } else if (args[0].equals("3")) {
            task3();
        }
    }

    static void task1() {
        System.out.println("Демонстрация EventEmitter (группа " + gruppa + ")");
        EventEmitter emitter = new EventEmitter();

        emitter.on("greet", (args) -> System.out.println("[greet] Привет, " + args[0] + "!"));
        emitter.on("info", (args) -> System.out.println("[info] Группа: " + gruppa));
        emitter.on("bye", (args) -> System.out.println("[bye] До свидания!"));

        emitter.emit("greet", "Cлава");
        emitter.emit("info");
        emitter.emit("bye");

        boolean resultUnknown = emitter.emit("unknown");
        System.out.println("Событие \"unknown\" без слушателей: " + resultUnknown);

        boolean resultGreet = emitter.emit("greet", "Петр");
        System.out.println("Событие \"greet\" со слушателями: " + resultGreet);
    }

    static void task2() {
        System.out.println("Сравнение on и once (группа " + gruppa + ")");
        EventEmitter emitter = new EventEmitter();
        int[] onCount = {0};
        int[] onceCount = {0};

        emitter.on("tick", (args) -> {
            System.out.println("[tick#" + args[0] + "] on-слушатель");
            onCount[0]++;
        });

        emitter.once("tick", (args) -> {
            System.out.println("[tick#" + args[0] + "] once-слушатель");
            onceCount[0]++;
        });

        emitter.emit("tick", 1);
        emitter.emit("tick", 2);
        emitter.emit("tick", 3);

        System.out.println("on-слушатель вызван: " + onCount[0] + " раза");
        System.out.println("once-слушатель вызван: " + onceCount[0] + " раз");

        System.out.println("\nУправление подписками");
        Consumer<Object[]> tempListener = (args) -> {};
        emitter.on("test", tempListener);
        emitter.on("test", tempListener);
        emitter.on("test", tempListener);

        System.out.println("Слушателей до удаления: " + emitter.listenerCount("test"));
        emitter.removeListener("test", tempListener);
        System.out.println("Слушателей после удаления одного: " + emitter.listenerCount("test"));
        emitter.removeAllListeners("test");
        System.out.println("Слушателей после removeAllListeners: " + emitter.listenerCount("test"));

        System.out.println("\n=== Порядок вызова ===");
        emitter.on("order", (args) -> System.out.println("1. Первый слушатель (группа " + gruppa + ")"));
        emitter.on("order", (args) -> System.out.println("2. Второй слушатель"));
        emitter.on("order", (args) -> System.out.println("3. Третий слушатель"));
        emitter.emit("order");
    }

    static void task3() {
        System.out.println("DatabaseConnection (группа " + gruppa + ")");
        DatabaseConnection db = new DatabaseConnection();

        db.on("connect", (args) -> System.out.println("[EVENT] Соединение установлено"));
        db.on("query", (args) -> System.out.println("[EVENT] Результат: " + args[1]));
        db.on("close", (args) -> System.out.println("[EVENT] Соединение закрыто"));

        db.on("error", (args) -> System.out.println("[EVENT] Обработана ошибка: " + args[0] + " (без слушателя процесс упал бы)"));

        db.connect();
        db.query("SELECT * FROM students");
        db.query("INSERT INTO students");
        db.close();

        System.out.println("\nДемонстрация ошибки");
        db.triggerError();
    }
}