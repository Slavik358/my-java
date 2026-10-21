import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class Main {
    static String fio = "Коротченя Вячеслав Романович";
    static String gruppa = "477";
    static int variant = 11;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("1")) {
            task1();
        } else if (args.length > 0 && args[0].equals("2")) {
            task2();
        } else if (args.length > 0 && args[0].equals("3")) {
            task3();
        } else {
            task1();
            System.out.println("\n-----------------------------------\n");
            task2();
            System.out.println("\n-----------------------------------\n");
            task3();
        }
    }

    static void task1() {
        String osName = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch");
        String version = System.getProperty("os.version");
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {}

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeSec = uptimeMs / 1000;
        long hours = uptimeSec / 3600;
        long min = (uptimeSec % 3600) / 60;
        long sec = uptimeSec % 60;

        String platform = "linux";
        if (osName.contains("win")) platform = "win32";
        else if (osName.contains("mac")) platform = "darwin";

        System.out.println("=== Информация о системе (группа " + gruppa + ") ===");
        System.out.println("Платформа: " + platform);
        System.out.println("Тип ОС: " + System.getProperty("os.name"));
        System.out.println("Архитектура: " + arch);
        System.out.println("Версия ОС: " + version);
        System.out.println("Имя хоста: " + hostname);
        System.out.println("Время работы: " + hours + " ч " + min + " мин " + sec + " сек");

        if (osName.contains("win")) {
            System.out.println("Вы работаете в Windows");
        } else if (osName.contains("mac")) {
            System.out.println("Вы работаете в macOS");
        } else if (osName.contains("nix") || osName.contains("nux")) {
            System.out.println("Вы работаете в Linux");
        } else {
            System.out.println("Неизвестная платформа");
        }
    }

    static void task2() {
        int cores = Runtime.getRuntime().availableProcessors();
        String cpuModel = System.getenv("PROCESSOR_IDENTIFIER");
        if (cpuModel == null) cpuModel = System.getProperty("os.arch") + " processor";

        long physTotal = 0;
        long physFree = 0;
        double loadAvg = -1.0;

        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            physTotal = osBean.getTotalPhysicalMemorySize() / (1024 * 1024 * 1024);
            physFree = osBean.getFreePhysicalMemorySize() / (1024 * 1024 * 1024);
            loadAvg = osBean.getSystemLoadAverage();
        } catch (Exception e) {
            physTotal = Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024);
            physFree = Runtime.getRuntime().freeMemory() / (1024 * 1024 * 1024);
        }

        long used = physTotal - physFree;
        double percent = physTotal > 0 ? (used * 100.0) / physTotal : 0;

        System.out.println("=== Информация о процессоре ===");
        System.out.println("Количество логических ядер: " + cores);
        System.out.println("Модель процессора: " + cpuModel);
        System.out.println("=== Информация о памяти ===");
        System.out.println("Общий объём: " + physTotal + " ГБ");
        System.out.println("Свободно: " + physFree + " ГБ");
        System.out.println("Использовано: " + used + " ГБ (" + String.format("%.1f", percent) + "%)");
        System.out.println("Группа: " + gruppa);

        if (percent > 80) {
            System.out.println("ВНИМАНИЕ: Мало свободной памяти!");
        } else {
            System.out.println("Память в норме");
        }

        if (loadAvg < 0) {
            System.out.println("Средняя загрузка (1, 5, 15 мин): недоступно (Windows)");
        } else {
            System.out.println("Средняя загрузка (1, 5, 15 мин): " + String.format("%.2f", loadAvg));
        }
    }

    static void task3() {
        System.out.println("=== Сетевые интерфейсы ===");
        int interfaceCount = 0;
        String mainInterface = "";

        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                if (netint.isUp() && !netint.isLoopback()) {
                    interfaceCount++;
                    System.out.println("Интерфейс: " + netint.getDisplayName());

                    Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                    for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                            String ip = inetAddress.getHostAddress();
                            System.out.println("IPv4: " + ip);
                            if (mainInterface.isEmpty()) {
                                mainInterface = netint.getDisplayName() + " (" + ip + ")";
                            }
                        }
                    }

                    byte[] mac = netint.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                        }
                        String macStr = sb.toString();
                        String maskedMac = macStr.length() > 8 ? macStr.substring(0, 8) + "*:*:*" : macStr;
                        System.out.println("MAC: " + maskedMac);
                    }
                }
            }
        } catch (Exception e) {}

        System.out.println("Всего интерфейсов: " + interfaceCount);
        System.out.println("Основной интерфейс: " + (mainInterface.isEmpty() ? "не найден" : mainInterface));

        System.out.println("=== Информация о пользователе ===");
        System.out.println("Имя пользователя: " + System.getProperty("user.name"));
        System.out.println("Домашняя директория: " + System.getProperty("user.home"));
        System.out.println("Группа: " + gruppa);

        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            System.out.println("UID/GID: недоступно в Windows");
            System.out.println("Оболочка: cmd.exe");
            System.out.println("Проверка root: нет (Windows)");
        } else {
            System.out.println("UID: " + System.getProperty("user.name").hashCode());
            System.out.println("Оболочка: " + System.getenv("SHELL"));
            System.out.println("Проверка root: " + (System.getProperty("user.name").equals("root") ? "да" : "нет"));
        }
    }
}