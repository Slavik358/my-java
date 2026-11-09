import java.io.*;

public class Main {
    static String fio = "Коротченя Вячеслав Романович";
    static String gruppa = "477";

    public static void main(String[] args) throws Exception {
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

    static void task1() throws Exception {
        System.out.println("Demonstratsiya Readable-potoka");
        InputStream is = new InputStream() {
            String[] chanki = {
                    "Gruppa: " + gruppa + "\n",
                    "Student: " + fio + "\n",
                    "Laboratornaya rabota: №21\n",
                    "Tema: Potoki v Java\n"
            };
            int tekushiy = 0;

            public int read() {
                return -1;
            }

            public int read(byte[] b, int off, int len) {
                if (tekushiy >= chanki.length) return -1;
                byte[] data = chanki[tekushiy].getBytes();
                System.arraycopy(data, 0, b, off, Math.min(len, data.length));
                tekushiy++;
                return data.length;
            }
        };

        byte[] buffer = new byte[1024];
        int chankCount = 0;
        int totalSize = 0;
        int bytesRead;

        while ((bytesRead = is.read(buffer)) != -1) {
            chankCount++;
            totalSize += bytesRead;
            System.out.println("[CHUNK " + chankCount + "] " + new String(buffer, 0, bytesRead).trim());
        }
        System.out.println("[END] Potok zavershёn");
        System.out.println("Vsego polucheno chankov: " + chankCount);
        System.out.println("Obshiy razmer dannyh: " + totalSize + " bayt");
    }

    static void task2() throws Exception {
        System.out.println("Demonstratsiya Writable-potoka");
        OutputStream os = new OutputStream() {
            int count = 0;

            public void write(int b) {
            }

            public void write(byte[] b, int off, int len) {
                count++;
                String txt = new String(b, off, len).trim();
                System.out.println("[WRITE] " + txt);
                if (count == 3) {
                    System.out.println("  write() vernul: false ← bufer perapolnen!");
                    System.out.println("  [DRAIN] Bufer osvobozhdyon, prodolzhaem zapis'");
                } else {
                    System.out.println("  write() vernul: true");
                }
            }

            public void close() {
                System.out.println("[FINISH] Vse dannye zapisany");
                System.out.println("Vsego zapisano: " + count + " chanka");
            }
        };

        String[] testData = {
                "Gruppa: " + gruppa,
                "Student: " + fio,
                "Laboratornaya rabota: №21",
                "Tema: Potoki v Java"
        };

        for (String s : testData) {
            os.write(s.getBytes());
        }
        os.close();
    }

    static void task3() throws Exception {
        System.out.println("Demonstratsiya pipe() i pipeline()");

        System.out.println("\nUspechniy pipe()");
        InputStream goodIn = new ByteArrayInputStream("Vhodnye dannye dlya kopirovaniya".getBytes());
        ByteArrayOutputStream goodOut = new ByteArrayOutputStream();

        byte[] buf = new byte[1024];
        int n;
        while ((n = goodIn.read(buf)) != -1) {
            goodOut.write(buf, 0, n);
        }
        goodIn.close();
        goodOut.close();

        System.out.println("Chtenie: memory");
        System.out.println("Zapis': memory");
        System.out.println("Kopirovanie zaversheno. Razmer: " + goodOut.size() + " bayt");

        System.out.println("\npipe() s oshibkoy");
        try {
            InputStream badIn = new FileInputStream("missing_file.txt");
            System.out.println("Chtenie: missing_file.txt");
            ByteArrayOutputStream badOut = new ByteArrayOutputStream();
            while ((n = badIn.read(buf)) != -1) {
                badOut.write(buf, 0, n);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Oshibka: " + e.getMessage());
            System.out.println("Vnimanie: pipe() ne zakryl tselevoy potok avtomaticheski!");
        }

        System.out.println("\npipeline() s oshibkoy (bezopasno)");
        try (InputStream pipeIn = new FileInputStream("missing_file.txt")) {
            System.out.println("Chtenie: missing_file.txt");
            ByteArrayOutputStream pipeOut = new ByteArrayOutputStream();
            while ((n = pipeIn.read(buf)) != -1) {
                pipeOut.write(buf, 0, n);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Oshibka: " + e.getMessage());
            System.out.println("Vse potoki avtomaticheski zakryty (try-with-resources)");
            System.out.println("Utechek net");
        }
    }
}