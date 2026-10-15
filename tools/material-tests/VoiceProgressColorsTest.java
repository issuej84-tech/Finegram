import com.th3nekit.finegram.appearance.material.VoiceProgressColors;
import java.util.Random;
public final class VoiceProgressColorsTest {
    public static void main(String[] args) {
        Random random = new Random(615);
        for (int i = 0; i < 10000; i++) {
            int bubble = random.nextInt() | 0xff000000;
            int requested = i % 2 == 0 ? bubble : random.nextInt();
            int fill = VoiceProgressColors.fill(requested, bubble);
            int track = VoiceProgressColors.track(requested, fill, bubble);
            int selected = VoiceProgressColors.track(random.nextInt(), fill, bubble);
            if (VoiceProgressColors.contrast(fill, bubble) < 4.5
                    || VoiceProgressColors.contrast(fill, track) < 3
                    || VoiceProgressColors.contrast(fill, selected) < 3)
                throw new AssertionError("Invisible voice progress: sample " + i);
        }
        System.out.println("PASS: 30,000 voice-progress contrast assertions across 10,000 theme/alpha combinations");
    }
}
