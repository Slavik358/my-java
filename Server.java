import java.util.Scanner;
import java.time.LocalDate;

public class Server {
    static String fio = "Коротченя Вячеслав Романович";
    static String gruppa = "477";
    static int variant = 11;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Использование: my-cli <команда> [аргументы]");
            System.out.println("Доступные команды:");
            System.out.println("  greet <имя> - поприветствовать пользователя");
            System.out.println("  info - вывести информацию о группе");
            System.out.println("  generate [опции] - сгенерировать отчёт");
            System.out.println("  convert [опции] - конвертировать файл");
            System.out.println("  init [опции] - инициализация проекта");
            System.out.println("  --version - версия приложения");
            System.exit(0);
        }

        String cmd = args[0];

        if (cmd.equals("greet")) {
            if (args.length < 2) {
                System.err.println("Ошибка: укажите имя");
                System.exit(1);
            }
            System.out.println("Привет, " + args[1] + "! Добро пожаловать в CLI-приложение группы " + gruppa + ".");
            System.exit(0);
        }

        if (cmd.equals("info")) {
            System.out.println("Группа: " + gruppa);
            System.out.println("Студент: " + fio);
            System.out.println("Лабораторная работа: №17");
            System.out.println("Дата: " + LocalDate.now());
            System.exit(0);
        }

        if (cmd.equals("generate")) {
            String tip = "html";
            String output = "./output.html";
            boolean dryRun = false;
            boolean verbose = false;
            boolean force = false;

            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("-t") || args[i].equals("--type")) {
                    if (i + 1 < args.length) tip = args[++i];
                } else if (args[i].equals("-o") || args[i].equals("--output")) {
                    if (i + 1 < args.length) output = args[++i];
                } else if (args[i].equals("--dry-run")) {
                    dryRun = true;
                } else if (args[i].equals("-v") || args[i].equals("--verbose")) {
                    verbose = true;
                } else if (args[i].equals("-f") || args[i].equals("--force")) {
                    force = true;
                }
            }

            if (!tip.equals("html") && !tip.equals("pdf") && !tip.equals("json") && !tip.equals("csv")) {
                System.err.println("Ошибка: недопустимый тип отчёта \"" + tip + "\".");
                System.err.println("Допустимые значения: html, pdf, json, csv");
                System.exit(1);
            }

            if (verbose) {
                System.out.println("[VERBOSE] Запуск генерации отчёта...");
                System.out.println("[VERBOSE] Тип отчёта: " + tip);
                System.out.println("[VERBOSE] Путь сохранения: " + output);
            }

            if (dryRun) {
                System.out.println("[DRY-RUN] Будет сгенерирован отчёт типа: " + tip);
                System.out.println("[DRY-RUN] Файл будет сохранён в: " + output);
                System.out.println("[DRY-RUN] Действия не выполнены (режим проверки)");
            } else {
                System.out.println("Отчёт успешно сгенерирован: " + output);
            }
            System.exit(0);
        }

        if (cmd.equals("convert")) {
            String input = null;
            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("-i") || args[i].equals("--input")) {
                    if (i + 1 < args.length) input = args[++i];
                }
            }
            if (input == null) {
                System.err.println("Ошибка: не указан входной файл. Используйте -i");
                System.exit(1);
            }
            System.out.println("Файл " + input + " успешно конвертирован.");
            System.exit(0);
        }

        if (cmd.equals("init")) {
            boolean noInteractive = false;
            String name = null;
            String type = null;
            boolean ts = false;
            boolean prettier = false;
            boolean git = false;

            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("--no-interactive")) noInteractive = true;
                else if (args[i].equals("--name") && i + 1 < args.length) name = args[++i];
                else if (args[i].equals("--type") && i + 1 < args.length) type = args[++i];
                else if (args[i].equals("--typescript")) ts = true;
                else if (args[i].equals("--prettier")) prettier = true;
                else if (args[i].equals("--git")) git = true;
            }

            if (noInteractive) {
                if (name == null || type == null) {
                    System.err.println("Ошибка: в неинтерактивном режиме необходимо указать --name и --type");
                    System.exit(1);
                }
                printInitSuccess(name, type, ts, prettier, git);
                System.exit(0);
            }

            if (name == null || type == null) {
                Scanner sc = new Scanner(System.in);
                System.out.print("? Введите название проекта: ");
                name = sc.nextLine();
                System.out.print("? Выберите тип проекта (cli/web/lib): ");
                type = sc.nextLine();
                System.out.print("? Использовать TypeScript? (y/n): ");
                String tsStr = sc.nextLine();
                if (tsStr.equalsIgnoreCase("y")) ts = true;
                System.out.print("? Использовать Git? (y/n): ");
                String gitStr = sc.nextLine();
                if (gitStr.equalsIgnoreCase("y")) git = true;
            }

            printInitSuccess(name, type, ts, prettier, git);
            System.exit(0);
        }

        if (cmd.equals("--version") || cmd.equals("-V")) {
            System.out.println("1.0.0");
            System.exit(0);
        }

        System.err.println("Ошибка: неизвестная команда \"" + cmd + "\"");
        System.err.println("Для справки используйте: my-cli --help");
        System.exit(1);
    }

    static void printInitSuccess(String name, String type, boolean ts, boolean prettier, boolean git) {
        System.out.println("\n✓ Проект \"" + name + "\" успешно инициализирован!");
        System.out.println("Тип: " + type);
        String opts = "";
        if (ts) opts += "TypeScript, ";
        if (prettier) opts += "Prettier, ";
        if (!opts.isEmpty()) System.out.println("Опции: " + opts.substring(0, opts.length() - 2));
        System.out.println("Git: " + (git ? "да" : "нет"));
    }
}