import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.List;

public class FileWatcher {

    public void readFile(String fileName) {
        try {
            // Get the directory path and the file path
            Path filePath = Paths.get(fileName).toAbsolutePath();
            Path dirPath = filePath.getParent();
            Path logFileName = filePath.getFileName();

            System.out.println("Directory to watch: " + dirPath);
            System.out.println("File to watch: " + logFileName);

            // Create a WatchService to monitor the directory
            WatchService watchService = FileSystems.getDefault().newWatchService();
            dirPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            File file = filePath.toFile();
            if (!file.exists()) {
                System.out.println("File does not exist");
                return;
            }

            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {

                List<String> lines = getLastLines(randomAccessFile,10);
                for (String line : lines) {
                    System.out.println(line);
                }
                long filePointer = file.length();
                while (true) {
//                    System.out.println("Waiting for file change...");
                    WatchKey watchKey = watchService.take(); // This will wait for an event

                    for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
                        WatchEvent.Kind<?> kind = watchEvent.kind();

                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Path changed = (Path) watchEvent.context();
//                            System.out.println("Modified file: " + changed);

                            // Check if the modified file is the target log file
                            if (changed.equals(logFileName)) {
//                                System.out.println("Target file modified: " + logFileName);

                                long fileLength = file.length();
                                if (fileLength > filePointer) {
                                    randomAccessFile.seek(filePointer);
                                    String line;
                                    while ((line = randomAccessFile.readLine()) != null) {
                                        System.out.println(line);
                                        lines.add(line);
                                        if (lines.size() >= 10) {
                                            lines.remove(0);
                                        }
                                    }
                                    filePointer = randomAccessFile.getFilePointer();
                                }
                            }
                        }
                    }
                    watchKey.reset();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getLastLines(RandomAccessFile randomAccessFile, int n) throws IOException {

        long fileLength = randomAccessFile.length();

        long filePointer = 0;

        List<String> lines = new LinkedList<>();

        randomAccessFile.seek(filePointer);

        while (filePointer < fileLength) {
            randomAccessFile.seek(filePointer);

            String readByte = randomAccessFile.readLine();

            if (!readByte.isBlank()) {
                if (filePointer<fileLength-1){
                    lines.add(readByte);
                    if (lines.size() > n){
                        lines.remove(0);
                    }
                }
            }
            filePointer+=readByte.length()+1;
        }

        return lines;

    }

    public static void main(String[] args) {
        FileWatcher fileWatcher = new FileWatcher();
        fileWatcher.readFile("src/logFile.log");
    }
}
