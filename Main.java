import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    static int variant = 11;

    public static void main(String[] args) {
        System.out.println("вар " + variant);
        zadanie1();
        zadanie2();
        zadanie3(args);
    }

    static void zadanie1() {
        System.out.println("\n1");
        String imyaFaila = "student_" + variant + ".txt";
        String segodnya = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String[] spisokKnig = {
                "Проект конец света",
                "Дентельмены",
                "Адольф Гитлер Маинкампф",
                "Роберт Киосаки Бедный папа Богатый папа",
                "Инструкция к пылесосу"
        };

        String tekst = "Коротченя Вячеслав Романович\n";
        tekst = tekst + "группа: 477\n";
        tekst = tekst + "вар: " + variant + "\n";
        tekst = tekst + "дата: " + segodnya + "\n";
        tekst = tekst + "любимые фильмы и игры:\n";

        for (int i = 0; i < spisokKnig.length; i++) {
            tekst = tekst + (i + 1) + ". " + spisokKnig[i] + "\n";
        }

        String[] strokiDlyaScheta = tekst.split("\n");
        int kolvoZapis = strokiDlyaScheta.length;
        tekst = tekst + "\nКол-во писей: " + kolvoZapis;

        try {
            FileWriter fw = new FileWriter(imyaFaila);
            fw.write(tekst);
            fw.close();
            System.out.println("файл: " + imyaFaila);

            System.out.println("внутри:");
            Scanner sc = new Scanner(new File(imyaFaila));
            while (sc.hasNextLine()) {
                System.out.println(sc.nextLine());
            }
            sc.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static void zadanie2() {
        System.out.println("\n2");
        String papkaProekta = "project_" + variant;

        try {
            String[] spsokPapok = {
                    papkaProekta + "/src/modules",
                    papkaProekta + "/src/components",
                    papkaProekta + "/src/utils",
                    papkaProekta + "/data/input",
                    papkaProekta + "/data/output",
                    papkaProekta + "/data/temp",
                    papkaProekta + "/temp"
            };

            if (variant % 2 != 0) {
                String[] dopPapki = {
                        papkaProekta + "/src/components/1",
                        papkaProekta + "/src/components/2",
                        papkaProekta + "/src/components/3"
                };
                String[] vsePapki = new String[spsokPapok.length + dopPapki.length];
                for (int i = 0; i < spsokPapok.length; i++) {
                    vsePapki[i] = spsokPapok[i];
                }
                for (int i = 0; i < dopPapki.length; i++) {
                    vsePapki[spsokPapok.length + i] = dopPapki[i];
                }
                spsokPapok = vsePapki;
            }

            for (int i = 0; i < spsokPapok.length; i++) {
                File dir = new File(spsokPapok[i]);
                dir.mkdirs();
                File info = new File(spsokPapok[i] + "/info.txt");
                FileWriter fw = new FileWriter(info);
                fw.write("папка " + dir.getName());
                fw.close();
            }
            System.out.println("структура");

            System.out.println("\nдерево:");
            pokazatDerevo(new File(papkaProekta), "");

            File starayaTemp = new File(papkaProekta + "/temp");
            File novayaTemp = new File(papkaProekta + "/data/temp");
            Files.move(starayaTemp.toPath(), novayaTemp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println(" ");

            File starOut = new File(papkaProekta + "/data/output");
            File newOut = new File(papkaProekta + "/data/results");
            starOut.renameTo(newOut);
            System.out.println(" ");

            File tempDelete = new File(papkaProekta + "/data/temp");
            udalitPapku(tempDelete);
            System.out.println("data/temp udalena");

            System.out.println("\nновое древо:");
            pokazatDerevo(new File(papkaProekta), "");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    static void pokazatDerevo(File papka, String otstup) {
        try {
            File[] vse = papka.listFiles();
            if (vse == null) return;
            Arrays.sort(vse);
            for (int i = 0; i < vse.length; i++) {
                File f = vse[i];
                if (f.isDirectory()) {
                    System.out.println(otstup + "[DIR] " + f.getName());
                    pokazatDerevo(f, otstup + "  ");
                } else {
                    System.out.println(otstup + "      " + f.getName());
                }
            }
        } catch (Exception e) {
        }
    }

    static void udalitPapku(File papka) {
        if (papka.isDirectory()) {
            File[] vse = papka.listFiles();
            if (vse != null) {
                for (int i = 0; i < vse.length; i++) {
                    udalitPapku(vse[i]);
                }
            }
        }
        papka.delete();
    }

    static int schetFailov = 0;
    static int schetPapok = 0;
    static long obshiyRazmer = 0;
    static Map<String, List<String>> poRasshireniyam = new HashMap<>();
    static Map<String, Long> razmeriFailov = new HashMap<>();

    static boolean podhoditRasshirenie(String imya) {
        String[] razresh = {".js", ".json", ".txt", ".md"};
        String imyaNiz = imya.toLowerCase();
        for (int i = 0; i < razresh.length; i++) {
            if (imyaNiz.endsWith(razresh[i])) {
                return true;
            }
        }
        return false;
    }

    static void skanRekursivno(File papka) {
        try {
            File[] vse = papka.listFiles();
            if (vse == null) return;
            for (int i = 0; i < vse.length; i++) {
                File f = vse[i];
                if (f.isDirectory()) {
                    schetPapok = schetPapok + 1;
                    skanRekursivno(f);
                } else {
                    if (!podhoditRasshirenie(f.getName())) {
                        continue;
                    }
                    long razm = f.length();
                    if (razm > 10 * 1024 * 1024) {
                        continue;
                    }
                    schetFailov = schetFailov + 1;
                    obshiyRazmer = obshiyRazmer + razm;
                    String imya = f.getName();
                    String rasshirenie = "нет реш";
                    int tochka = imya.lastIndexOf(".");
                    if (tochka != -1) {
                        rasshirenie = imya.substring(tochka);
                    }
                    if (!poRasshireniyam.containsKey(rasshirenie)) {
                        poRasshireniyam.put(rasshirenie, new ArrayList<>());
                    }
                    poRasshireniyam.get(rasshirenie).add(imya);
                    razmeriFailov.put(f.getAbsolutePath(), razm);
                }
            }
        } catch (Exception e) {
        }
    }

    static void zadanie3(String[] args) {
        System.out.println("\n3");
        String put = ".";
        if (args.length > 0) {
            put = args[0];
        }
        System.out.println("скан: " + put);

        File koren = new File(put);
        skanRekursivno(koren);

        System.out.println("папок: " + schetPapok);
        System.out.println("файлов: " + schetFailov);

        double kb = obshiyRazmer / 1024.0;
        double mb = kb / 1024.0;
        System.out.println("size: " + obshiyRazmer + " байт (" + String.format("%.2f", kb) + " KB, " + String.format("%.2f", mb) + " MB)");

        System.out.println("\nрасширения:");
        for (String rassh : poRasshireniyam.keySet()) {
            List<String> spsok = poRasshireniyam.get(rassh);
            System.out.println("  " + rassh + ": " + spsok.size() + " файлов");
        }

        List<Map.Entry<String, Long>> spsokRazmerov = new ArrayList<>(razmeriFailov.entrySet());
        spsokRazmerov.sort(new Comparator<Map.Entry<String, Long>>() {
            public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                return Long.compare(b.getValue(), a.getValue());
            }
        });

        System.out.println("\nТОП 5 биг :");
        int limit1 = 5;
        if (spsokRazmerov.size() < 5) limit1 = spsokRazmerov.size();
        for (int i = 0; i < limit1; i++) {
            Map.Entry<String, Long> e = spsokRazmerov.get(i);
            File ff = new File(e.getKey());
            System.out.println("  " + (i + 1) + ". " + ff.getName() + " (" + e.getValue() + " байт)");
        }

        System.out.println("\nТОП 5 мал:");
        int limit2 = 5;
        if (spsokRazmerov.size() < 5) limit2 = spsokRazmerov.size();
        for (int i = 0; i < limit2; i++) {
            Map.Entry<String, Long> e = spsokRazmerov.get(spsokRazmerov.size() - 1 - i);
            File ff = new File(e.getKey());
            System.out.println("  " + (i + 1) + ". " + ff.getName() + " (" + e.getValue() + " байт)");
        }

        try {
            FileWriter rep = new FileWriter("report_" + variant + ".json");
            rep.write("{\n");
            rep.write("  \"вариант\": " + variant + ",\n");
            rep.write("  \"папок\": " + schetPapok + ",\n");
            rep.write("  \"мамок\": " + schetFailov + ",\n");
            rep.write("  \"size байт\": " + obshiyRazmer + ",\n");
            rep.write("  \"расширения\": {\n");
            int count = 0;
            for (String rassh : poRasshireniyam.keySet()) {
                count++;
                int kolvo = poRasshireniyam.get(rassh).size();
                rep.write("    \"" + rassh + "\": " + kolvo);
                if (count < poRasshireniyam.size()) {
                    rep.write(",");
                }
                rep.write("\n");
            }
            rep.write("  }\n");
            rep.write("}\n");
            rep.close();
            System.out.println("\nотчет: report_" + variant + ".json");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}