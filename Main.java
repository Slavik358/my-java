import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Date;

interface MyCallback {
    void run(Object error, Object result);
}

interface MySimpleCallback {
    void run(Object error);
}


class FileManagerOldStyle {
    private String myBaseDir;

    public FileManagerOldStyle(String baseDir) {
        this.myBaseDir = baseDir;
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("создал папку: " + baseDir);
        }
    }


    public void createFileThing(String filename, String content, MyCallback cb) {
        String fullpath = this.myBaseDir + File.separator + filename;
        try {
            FileWriter fw = new FileWriter(fullpath);
            fw.write(content);
            fw.close();
            cb.run(null, fullpath);
        } catch (IOException e) {
            cb.run(e, null);
        }
    }


    public void readFileThing(String filename, MyCallback cb) {
        String fullpath = this.myBaseDir + File.separator + filename;
        try {
            FileReader fr = new FileReader(fullpath);
            BufferedReader br = new BufferedReader(fr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            br.close();
            cb.run(null, sb.toString().trim());
        } catch (IOException e) {
            cb.run(e, null);
        }
    }


    public void getFileStatsThing(String filename, MyCallback cb) {
        String fullpath = this.myBaseDir + File.separator + filename;
        try {
            Path p = Paths.get(fullpath);
            BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);


            long sizeVal = attr.size();
            String createdVal = attr.creationTime().toString();
            String modifiedVal = attr.lastModifiedTime().toString();
            boolean isFileVal = attr.isRegularFile();

            String result = "размер=" + sizeVal + " создан=" + createdVal + " изменен=" + modifiedVal + " этоФайл=" + isFileVal;
            cb.run(null, result);
        } catch (IOException e) {
            cb.run(e, null);
        }
    }


    public void deleteFileThing(String filename, MySimpleCallback cb) {
        String fullpath = this.myBaseDir + File.separator + filename;
        File f = new File(fullpath);
        if (f.delete()) {
            cb.run(null);
        } else {
            cb.run(new Exception("не получилось удалить"));
        }
    }


    public void listFilesThing(MyCallback cb) {
        File dir = new File(this.myBaseDir);
        File[] filesArr = dir.listFiles();
        if (filesArr == null) {
            cb.run(new Exception("папка не найдена"), null);
            return;
        }
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < filesArr.length; i++) {
            File f = filesArr[i];
            if (f.isFile()) {
                names.add(f.getName());
            }
        }
        cb.run(null, names);
    }
}



class FileManagerNewStyle {
    private String myBaseDir;

    public FileManagerNewStyle(String baseDir) {
        this.myBaseDir = baseDir;
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("создал папку для промисов: " + baseDir);
        }
    }


    public CompletableFuture<String> createFilePromise(String filename, String content) {
        return CompletableFuture.supplyAsync(() -> {
            String fullpath = this.myBaseDir + File.separator + filename;
            try {
                FileWriter fw = new FileWriter(fullpath);
                fw.write(content);
                fw.close();
                return fullpath;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }


    public CompletableFuture<String> readFilePromise(String filename) {
        return CompletableFuture.supplyAsync(() -> {
            String fullpath = this.myBaseDir + File.separator + filename;
            try {
                FileReader fr = new FileReader(fullpath);
                BufferedReader br = new BufferedReader(fr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                br.close();
                return sb.toString().trim();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }


    public CompletableFuture<String> getFileStatsPromise(String filename) {
        return CompletableFuture.supplyAsync(() -> {
            String fullpath = this.myBaseDir + File.separator + filename;
            try {
                Path p = Paths.get(fullpath);
                BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
                long sizeVal = attr.size();
                String createdVal = attr.creationTime().toString();
                String modifiedVal = attr.lastModifiedTime().toString();
                boolean isFileVal = attr.isRegularFile();
                return "размер=" + sizeVal + " создан=" + createdVal + " изменен=" + modifiedVal + " этоФайл=" + isFileVal;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }


    public CompletableFuture<Void> deleteFilePromise(String filename) {
        return CompletableFuture.runAsync(() -> {
            String fullpath = this.myBaseDir + File.separator + filename;
            File f = new File(fullpath);
            if (!f.delete()) {
                throw new CompletionException(new Exception("не удалилось"));
            }
        });
    }


    public CompletableFuture<List<String>> listFilesPromise() {
        return CompletableFuture.supplyAsync(() -> {
            File dir = new File(this.myBaseDir);
            File[] filesArr = dir.listFiles();
            List<String> names = new ArrayList<String>();
            if (filesArr != null) {
                for (int i = 0; i < filesArr.length; i++) {
                    if (filesArr[i].isFile()) {
                        names.add(filesArr[i].getName());
                    }
                }
            }
            return names;
        });
    }
}


class FileManagerHybrid {
    private FileManagerOldStyle oldOne;
    private FileManagerNewStyle newOne;
    private String myDir;

    public FileManagerHybrid(String baseDir) {
        this.myDir = baseDir;
        this.oldOne = new FileManagerOldStyle(baseDir + "-old");
        this.newOne = new FileManagerNewStyle(baseDir + "-new");
    }


    public void createFileSmart(String filename, String content, MyCallback cb) {

        this.oldOne.createFileThing(filename, content, new MyCallback() {
            public void run(Object error, Object result) {
                if (error != null) {

                    newOne.createFilePromise(filename, content)
                        .thenAccept(path -> {
                            cb.run(null, path);
                        })
                        .exceptionally(ex -> {
                            cb.run(ex, null);
                            return null;
                        });
                } else {
                    cb.run(null, result);
                }
            }
        });
    }


    public CompletableFuture<String> createFileSmartPromise(String filename, String content) {
        CompletableFuture<String> future = new CompletableFuture<String>();
        this.createFileSmart(filename, content, new MyCallback() {
            public void run(Object error, Object result) {
                if (error != null) {
                    future.completeExceptionally((Throwable) error);
                } else {
                    future.complete((String) result);
                }
            }
        });
        return future;
    }
}


public class Main {

    public static void main(String[] args) {
        System.out.println("ЛАБОРАТОРНАЯ 13");
        System.out.println("задание 1: колбэки");
        System.out.println("задание 2: промисы");
        System.out.println("задание 3: гибрид");
        System.out.println("");


        System.out.println("ЗАДАНИЕ 1: КОЛБЭКИ");
        final FileManagerOldStyle oldManager = new FileManagerOldStyle("./data-callbacks");


        System.out.println("1. создаю файл");
        oldManager.createFileThing("test1.txt", "привет из колбэков!", new MyCallback() {
            public void run(Object error, Object result) {
                if (error != null) {
                    System.out.println("  ошибка: " + error);
                    return;
                }
                System.out.println("  файл создан: " + result);


                System.out.println("2. читаю файл");
                oldManager.readFileThing("test1.txt", new MyCallback() {
                    public void run(Object error, Object result) {
                        if (error != null) {
                            System.out.println("  ошибка чтения: " + error);
                            return;
                        }
                        System.out.println("  содержимое: \"" + result + "\"");


                        System.out.println("3. статистика");
                        oldManager.getFileStatsThing("test1.txt", new MyCallback() {
                            public void run(Object error, Object result) {
                                if (error != null) {
                                    System.out.println("  ошибка стат: " + error);
                                    return;
                                }
                                System.out.println("  стат: " + result);


                                System.out.println("4. список файлов");
                                oldManager.listFilesThing(new MyCallback() {
                                    public void run(Object error, Object result) {
                                        if (error != null) {
                                            System.out.println("  ошибка списка: " + error);
                                            return;
                                        }
                                        List<String> files = (List<String>) result;
                                        System.out.println("  найдено файлов: " + files.size());
                                        for (int i = 0; i < files.size(); i++) {
                                            System.out.println("   - " + files.get(i));
                                        }


                                        System.out.println("5. удаляю файл");
                                        oldManager.deleteFileThing("test1.txt", new MySimpleCallback() {
                                            public void run(Object error) {
                                                if (error != null) {
                                                    System.out.println("  ошибка удаления: " + error);
                                                    return;
                                                }
                                                System.out.println("  файл удален");
                                                System.out.println("  задание 1 готово!\n");


                                                runTask2();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }


    static void runTask2() {
        System.out.println("ЗАДАНИЕ 2: ПРОМИСЫ");
        final FileManagerNewStyle newManager = new FileManagerNewStyle("./data-promises");

        System.out.println("1. создаю файл");
        newManager.createFilePromise("test2.txt", "привет из промисов!")
            .thenAccept(path -> {
                System.out.println("  файл создан: " + path);

                System.out.println("2. читаю файл");
                newManager.readFilePromise("test2.txt")
                    .thenAccept(content -> {
                        System.out.println("  содержимое: \"" + content + "\"");

                        System.out.println("3. статистика");
                        newManager.getFileStatsPromise("test2.txt")
                            .thenAccept(stats -> {
                                System.out.println("  стат: " + stats);

                                System.out.println("4. список файлов");
                                newManager.listFilesPromise()
                                    .thenAccept(files -> {
                                        System.out.println("  найдено файлов: " + files.size());
                                        for (String f : files) {
                                            System.out.println("   - " + f);
                                        }

                                        System.out.println("5. удаляю файл ");
                                        newManager.deleteFilePromise("test2.txt")
                                            .thenRun(() -> {
                                                System.out.println("  файл удален");
                                                System.out.println("  задание 2 готово!\n");


                                                runTask3();
                                            })
                                            .exceptionally(ex -> {
                                                System.out.println("  ошибка удаления: " + ex.getMessage());
                                                return null;
                                            });
                                    })
                                    .exceptionally(ex -> {
                                        System.out.println("  ошибка списка: " + ex.getMessage());
                                        return null;
                                    });
                            })
                            .exceptionally(ex -> {
                                System.out.println("  ошибка стат: " + ex.getMessage());
                                return null;
                            });
                    })
                    .exceptionally(ex -> {
                        System.out.println("  ошибка чтения: " + ex.getMessage());
                        return null;
                    });
            })
            .exceptionally(ex -> {
                System.out.println("  ошибка создания: " + ex.getMessage());
                return null;
            });


        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }


    static void runTask3() {
        System.out.println("ЗАДАНИЕ 3");
        FileManagerHybrid hybrid = new FileManagerHybrid("./data-hybrid");

        System.out.println("1. создаю файл через гибрид");
        hybrid.createFileSmart("test3.txt", "привет из гибрида", new MyCallback() {
            public void run(Object error, Object result) {
                if (error != null) {
                    System.out.println("  ошибка: " + error);
                    return;
                }
                System.out.println("  файл создан: " + result);

                System.out.println("2. создаю файл через гибрид ");
                hybrid.createFileSmartPromise("test4.txt", "еще один привет!")
                    .thenAccept(path -> {
                        System.out.println("  файл создан: " + path);
                        System.out.println("  задание 3 готово!");
                        System.out.println("\nвсе задания выполнены!");
                    })
                    .exceptionally(ex -> {
                        System.out.println("  ошибка: " + ex.getMessage());
                        return null;
                    });
            }
        });


        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }
}