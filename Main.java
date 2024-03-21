import java.io.*;
import java.net.UnknownHostException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main {

    private static final String JAVA_EXTENSION = ".java";

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        // GitHub URL'si
        System.out.println("GitHub URL'sini giriniz: ");
        String urlString = scanner.nextLine();

        // URL'den dosya indirme
        File tempDirectory = createTempDirectory();
        try {
            File zipFile = downloadFile(urlString, tempDirectory);
            unzipFile(zipFile, tempDirectory);
        } catch (UnknownHostException e) {
            System.err.println("Hata: Bilinmeyen ana bilgisayar: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Hata: Dosya indirme işlemi sırasında bir hata oluştu: " + e.getMessage());
        } finally {
            // Geçici dizini silme (her durumda yürütülsün)
            deleteTempDirectory(tempDirectory);
        }

        // *.java uzantılı dosyaları listeleme
        File[] files = tempDirectory.listFiles((dir, name) -> name.endsWith(JAVA_EXTENSION));

        // Her dosya için analiz
        for (File file : files) {
            if (file.isFile()) {
                analizEt(file);
            }
        }

        // Geçici dizini silme
        deleteTempDirectory(tempDirectory);
    }

    private static File createTempDirectory() throws IOException {
        Path tempDirectory = Files.createTempDirectory("kod-analizi");
        return tempDirectory.toFile();
    }

    private static File downloadFile(String urlString, File tempDirectory) throws IOException {
        URL url = new URL(urlString);
        InputStream inputStream = url.openStream();
        File zipFile = new File(tempDirectory, "kod-analizi.zip");
        try (OutputStream outputStream = new FileOutputStream(zipFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return zipFile;
    }

    private static void unzipFile(File zipFile, File tempDirectory) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File file = new File(tempDirectory, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdir();
                } else {
                    try (OutputStream outputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }
    }

    private static void deleteTempDirectory(File tempDirectory) {
        File[] files = tempDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteTempDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        tempDirectory.delete();
    }

    private static void analizEt(File file) throws IOException {
        int javadocSatirSayisi = 0;
        int yorumSatirSayisi = 0;
        int kodSatirSayisi = 0;
        int loc = 0;
        int fonksiyonSayisi = 0;

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                loc++;

                if (line.trim().startsWith("/**")) {
                    javadocSatirSayisi++;
                } else if (line.trim().startsWith("//")) {
                    yorumSatirSayisi++;
                } else if (!line.trim().isEmpty()) {
                    kodSatirSayisi++;

                    if (line.contains("(") && line.contains(")")) {
                        fonksiyonSayisi++;
                    }
                }
            }
        }

        // Hesaplamalar
        double yg = ((javadocSatirSayisi + yorumSatirSayisi) * 0.8) / fonksiyonSayisi;
        double yh = (kodSatirSayisi / fonksiyonSayisi) * 0.3;
        double yorumSapmaYuzdesi = ((100 * yg) / yh) - 100;

        // Ekran çıktısı
        System.out.println("**Sınıf:** " + file.getName());
        System.out.println("Javadoc Satır Sayısı: " + javadocSatirSayisi);
        System.out.println("Yorum Satır Sayısı: " + yorumSatirSayisi);
        System.out.println("Kod Satır Sayısı: " + kodSatirSayisi);
        System.out.println("LOC: " + loc);
        System.out.println("Fonksiyon Sayısı: " + fonksiyonSayisi);
        System.out.println("Yorum Sapma Yüzdesi: " + String.format("%.2f", yorumSapmaYuzdesi) + "%");
        System.out.println();
    }
}
