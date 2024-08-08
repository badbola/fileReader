
import java.io.FileNotFoundException;
public class Main {
    public static void main(String[] args) throws FileNotFoundException {


        FileWatcher fileWatcher = new FileWatcher();

        fileWatcher.readFile("src/logFile.log");
    }
}