import javax.sound.sampled.*;
import java.io.*;

public class Audio {
    public static void playWav(String path) {
        try {
            InputStream wavStream = Audio.class.getClassLoader().getResourceAsStream(path);
            AudioInputStream stream;
            AudioFormat format;
            DataLine.Info info;
            Clip clip;
            
            stream = AudioSystem.getAudioInputStream(new BufferedInputStream(wavStream));
            format = stream.getFormat();
            info = new DataLine.Info(Clip.class, format);
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);
            clip.start();
            while (!clip.isRunning())
                Thread.sleep(10);
            while (clip.isRunning())
                Thread.sleep(10);
            clip.close();
        }
        catch (Exception e) {
            System.err.println("WARNING: couldn't find or play sound: " + path);
        }
    }
}
