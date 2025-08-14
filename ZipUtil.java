import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    public static void main(String[] args) {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }

        List<String> sources = new ArrayList<>();
        String output = null;
        boolean recursive = true;
        boolean contentOnly = false;
        boolean separate = false;

        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                switch (arg) {
                    case "-nr":
                        recursive = false;
                        break;
                    case "-content":
                        contentOnly = true;
                        break;
                    case "-separate":
                        separate = true;
                        break;
                    case "-o":
                        i++;
                        if (i >= args.length) {
                            System.err.println("Missing argument for -o");
                            usage();
                            System.exit(1);
                        }
                        output = args[i];
                        break;
                    default:
                        System.err.println("Unknown option: " + arg);
                        usage();
                        System.exit(1);
                }
            } else {
                sources.add(arg);
            }
            i++;
        }

        if (sources.isEmpty()) {
            System.err.println("No sources provided");
            usage();
            System.exit(1);
        }

        if (!separate && output == null) {
            System.err.println("Need -o output.zip when not using -separate");
            usage();
            System.exit(1);
        }

        if (separate && output != null) {
            System.err.println("-o not supported with -separate");
            usage();
            System.exit(1);
        }

        try {
            if (separate) {
                for (String src : sources) {
                    File f = new File(src);
                    if (!f.exists()) {
                        System.err.println("Source not found: " + src);
                        continue;
                    }
                    String zipName = f.getName() + ".zip";
                    if (f.isFile()) {
                        zipFile(src, zipName);
                    } else if (f.isDirectory()) {
                        zipDir(src, zipName, recursive, !contentOnly);
                    } else {
                        System.err.println("Not a file or directory: " + src);
                    }
                }
            } else {
                try (FileOutputStream fos = new FileOutputStream(output);
                     ZipOutputStream zos = new ZipOutputStream(fos)) {
                    for (String src : sources) {
                        File f = new File(src);
                        if (!f.exists()) {
                            System.err.println("Source not found: " + src);
                            continue;
                        }
                        if (f.isFile()) {
                            addFileToZip(f, zos, f.getName());
                        } else if (f.isDirectory()) {
                            String base = contentOnly ? "" : f.getName() + "/";
                            addDirToZip(f, zos, base, recursive);
                        } else {
                            System.err.println("Skipping: " + src);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error during zipping: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void zipFile(String sourceFile, String zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(sourceFile)) {
            ZipEntry zipEntry = new ZipEntry(new File(sourceFile).getName());
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }

    private static void zipDir(String dirPath, String zipFile, boolean recursive, boolean includeDirName) throws IOException {
        File dir = new File(dirPath);
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            String base = includeDirName ? dir.getName() + "/" : "";
            addDirToZip(dir, zos, base, recursive);
        }
    }

    private static void addDirToZip(File dir, ZipOutputStream zos, String base, boolean recursive) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                if (recursive) {
                    addDirToZip(file, zos, base + file.getName() + "/", recursive);
                }
            } else {
                addFileToZip(file, zos, base + file.getName());
            }
        }
    }

    private static void addFileToZip(File file, ZipOutputStream zos, String entryName) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }

    private static void usage() {
        System.out.println("Usage: java ZipUtil [options] source1 [source2 ...]");
        System.out.println("Options:");
        System.out.println("  -o output.zip    : Specify the output zip file (required if not -separate)");
        System.out.println("  -separate        : Zip each source separately to source.zip");
        System.out.println("  -nr              : Do not recurse into subfolders (default: recurse)");
        System.out.println("  -content         : For directories, zip only the contents without the folder name");
    }
                      }
