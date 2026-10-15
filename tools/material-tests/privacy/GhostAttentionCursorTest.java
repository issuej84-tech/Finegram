package com.th3nekit.finegram.privacy;
import java.util.Random;
public final class GhostAttentionCursorTest {
    public static void main(String[] args) {
        int checks = 0;
        int[] boundaries = {Integer.MIN_VALUE, -1, 0, 1, 2, Integer.MAX_VALUE};
        for (int previous : boundaries) for (int next : boundaries) {
            boolean expected = previous >= 0 && next > 0 && (previous == 0 || next < previous);
            if (FinegramGhostAttentionSync.validCursor(previous, next) != expected) throw new AssertionError("Unsafe boundary cursor");
            checks++;
        }
        Random random = new Random(617);
        for (int i = 0; i < 10000; i++) {
            int previous = random.nextInt(Integer.MAX_VALUE), next = random.nextInt(Integer.MAX_VALUE);
            if (FinegramGhostAttentionSync.validCursor(previous, next) != (next > 0 && (previous == 0 || next < previous)))
                throw new AssertionError("Unsafe pagination");
            checks++;
        }
        System.out.println("PASS: " + checks + " ghost-sync pagination assertions");
    }
}
