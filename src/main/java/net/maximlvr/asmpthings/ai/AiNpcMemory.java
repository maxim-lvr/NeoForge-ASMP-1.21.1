package net.maximlvr.asmpthings.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class AiNpcMemory {

    private static final int MAX_MESSAGES = 5;

    private final Deque<String> recentMessages = new ArrayDeque<>();

    public void remember(String message) {
        recentMessages.addLast(message);

        while (recentMessages.size() > MAX_MESSAGES) {
            recentMessages.removeFirst();
        }
    }

    public List<String> getRecentMessages() {
        return new ArrayList<>(recentMessages);
    }

    public String getLastMessage() {
        return recentMessages.peekLast();
    }

    public int size() {
        return recentMessages.size();
    }
}