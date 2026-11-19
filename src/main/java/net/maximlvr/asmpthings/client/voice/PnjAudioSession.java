package net.maximlvr.asmpthings.client.voice;

import net.maximlvr.asmpthings.AsmpThingsMod;
import net.maximlvr.asmpthings.ai.voice.RealtimeProtocol;
import net.maximlvr.asmpthings.network.payload.PnjSpeakerPayload;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.AL11.AL_SAMPLE_OFFSET;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.*;

/** Each session owns its audio devices. Its thread-local context leaves Minecraft's sound engine alone. */
final class PnjAudioSession implements AutoCloseable {
    private static final int SAMPLES_PER_FRAME = 960; // 40 ms
    private static final int MAX_BUFFERED_BYTES = 480000; // ten seconds, including device buffers
    final UUID id;
    final String npcName;
    final AtomicInteger pendingMicrophone = new AtomicInteger();
    volatile boolean microphoneOpen;
    volatile float microphoneLevel;
    private volatile boolean running = true;
    private final String microphoneName;
    private final String speakerName;
    private final double volume;
    private final AtomicInteger bufferedBytes = new AtomicInteger();
    private final ConcurrentLinkedQueue<Chunk> incoming = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Interruption> interruptions = new ConcurrentLinkedQueue<>();
    private final AtomicInteger epoch = new AtomicInteger();
    private final Deque<PlayingBuffer> playing = new ArrayDeque<>();
    private final Deque<Integer> freeBuffers = new ArrayDeque<>();
    private final Map<String, Integer> completedSamples = new LinkedHashMap<>();
    private final List<Integer> allocatedBuffers = new ArrayList<>();
    private final Thread thread;
    private long capture;
    private long outputDevice;
    private long context;
    private int source;

    PnjAudioSession(UUID id, String npcName, String microphoneName, String speakerName, double volume) {
        this.id = id;
        this.npcName = npcName;
        this.microphoneName = microphoneName;
        this.speakerName = speakerName;
        this.volume = volume;
        thread = new Thread(this::run, "ASMP-PNJ-audio");
        thread.setDaemon(true);
    }

    void start() { thread.start(); }

    void enqueue(PnjSpeakerPayload audio) {
        if (!running || audio.audio().length == 0 || (audio.audio().length & 1) != 0) return;
        if (bufferedBytes.addAndGet(audio.audio().length) > MAX_BUFFERED_BYTES) {
            bufferedBytes.addAndGet(-audio.audio().length);
            PnjVoiceClient.failed(this, "Réponse audio trop longue. Conversation arrêtée.");
            return;
        }
        incoming.add(new Chunk(audio, epoch.get()));
    }

    void interruptPlayback(String item, int index) {
        interruptions.add(new Interruption(item, index, epoch.incrementAndGet()));
    }

    private void run() {
        try {
            open();
            ShortBuffer microphone = BufferUtils.createShortBuffer(SAMPLES_PER_FRAME);
            while (running) {
                recyclePlayedBuffers();
                Interruption interruption;
                while ((interruption = interruptions.poll()) != null) interruptOnAudioThread(interruption);
                fillPlayback();
                int available = alcGetInteger(capture, ALC_CAPTURE_SAMPLES);
                if (alcGetError(capture) != ALC_NO_ERROR) throw new IllegalStateException("Micro déconnecté");
                for (int frames = 0; available >= SAMPLES_PER_FRAME && frames < 8 && running; frames++) {
                    microphone.clear();
                    alcCaptureSamples(capture, microphone, SAMPLES_PER_FRAME);
                    byte[] pcm = new byte[SAMPLES_PER_FRAME * 2];
                    ByteBuffer encoded = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);
                    double power = 0;
                    for (int sample = 0; sample < SAMPLES_PER_FRAME; sample++) {
                        short value = microphone.get(sample);
                        encoded.putShort(value);
                        power += (double) value * value;
                    }
                    microphoneLevel = (float) (Math.sqrt(power / SAMPLES_PER_FRAME) / 32768);
                    if (pendingMicrophone.incrementAndGet() > 5)
                        throw new IllegalStateException("Client trop lent pour le flux micro");
                    PnjVoiceClient.microphone(this, pcm);
                    available -= SAMPLES_PER_FRAME;
                }
                Thread.sleep(10);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Throwable error) {
            if (running) {
                AsmpThingsMod.LOGGER.warn("[PNJ vocal] Audio indisponible : {}", error.getClass().getSimpleName());
                PnjVoiceClient.failed(this, "Impossible d'ouvrir ou maintenir le micro/la sortie audio. Vérifie les périphériques système et config/asmpthingsmod-voice-client.toml.");
            }
        } finally {
            running = false;
            microphoneOpen = false;
            release();
        }
    }

    private void open() {
        // OpenAL Soft, shipped with Minecraft, understands the system's default PulseAudio/CoreAudio/WASAPI devices.
        AsmpThingsMod.LOGGER.info("[PNJ vocal] Micros disponibles : {}", ALUtil.getStringList(0L, ALC_CAPTURE_DEVICE_SPECIFIER));
        outputDevice = alcOpenDevice(speakerName.isBlank() ? (String) null : speakerName);
        if (outputDevice == 0) throw new IllegalStateException("Sortie audio absente");
        ALCCapabilities capabilities = ALC.createCapabilities(outputDevice);
        if (!capabilities.ALC_EXT_thread_local_context)
            throw new IllegalStateException("OpenAL ne fournit pas de contexte audio par thread");
        context = alcCreateContext(outputDevice, (IntBuffer) null);
        if (context == 0 || !EXTThreadLocalContext.alcSetThreadContext(context))
            throw new IllegalStateException("Contexte audio indisponible");
        AL.createCapabilities(capabilities);
        source = alGenSources();
        alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);
        alSourcef(source, AL_ROLLOFF_FACTOR, 0);
        alSourcef(source, AL_GAIN, (float) volume);
        for (int i = 0; i < 6; i++) {
            int buffer = alGenBuffers();
            allocatedBuffers.add(buffer);
            freeBuffers.add(buffer);
        }
        capture = alcCaptureOpenDevice(microphoneName.isBlank() ? (String) null : microphoneName,
                RealtimeProtocol.SAMPLE_RATE, AL_FORMAT_MONO16, RealtimeProtocol.SAMPLE_RATE);
        if (capture == 0) throw new IllegalStateException("Micro absent ou inaccessible");
        alcCaptureStart(capture);
        microphoneOpen = true;
        AsmpThingsMod.LOGGER.info("[PNJ vocal] Micro ouvert, PCM16 mono 24000 Hz.");
    }

    private void fillPlayback() {
        while (!freeBuffers.isEmpty()) {
            Chunk chunk = incoming.poll();
            if (chunk == null) break;
            if (chunk.epoch != epoch.get()) { bufferedBytes.addAndGet(-chunk.audio.audio().length); continue; }
            int buffer = freeBuffers.removeFirst();
            byte[] pcm = chunk.audio.audio();
            // OpenAL buffers use native-endian PCM16, while the network uses little endian.
            ShortBuffer samples = BufferUtils.createShortBuffer(pcm.length / 2);
            samples.put(ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()).flip();
            alBufferData(buffer, AL_FORMAT_MONO16, samples, RealtimeProtocol.SAMPLE_RATE);
            alSourceQueueBuffers(source, buffer);
            playing.addLast(new PlayingBuffer(buffer, chunk));
        }
        if (!playing.isEmpty() && alGetSourcei(source, AL_SOURCE_STATE) != AL_PLAYING) alSourcePlay(source);
        if (alGetError() != AL_NO_ERROR) throw new IllegalStateException("Lecture audio impossible");
    }

    private void recyclePlayedBuffers() {
        int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED);
        for (int i = 0; i < processed; i++) {
            int buffer = alSourceUnqueueBuffers(source);
            PlayingBuffer played = playing.removeFirst();
            String key = audioKey(played.chunk.audio.itemId(), played.chunk.audio.contentIndex());
            completedSamples.merge(key, played.chunk.audio.audio().length / 2, Integer::sum);
            if (completedSamples.size() > 64) completedSamples.remove(completedSamples.keySet().iterator().next());
            bufferedBytes.addAndGet(-played.chunk.audio.audio().length);
            freeBuffers.add(buffer);
        }
    }

    private void interruptOnAudioThread(Interruption interruption) {
        recyclePlayedBuffers();
        String key = audioKey(interruption.item, interruption.index);
        int samples = completedSamples.getOrDefault(key, 0);
        PlayingBuffer current = playing.peekFirst();
        if (current != null && current.chunk.audio.itemId().equals(interruption.item)
                && current.chunk.audio.contentIndex() == interruption.index)
            samples += Math.min(current.chunk.audio.audio().length / 2, alGetSourcei(source, AL_SAMPLE_OFFSET));
        alSourceStop(source);
        for (PlayingBuffer buffer : playing) {
            freeBuffers.add(alSourceUnqueueBuffers(source));
            bufferedBytes.addAndGet(-buffer.chunk.audio.audio().length);
        }
        playing.clear();
        // New-turn chunks received during interruption belong to the new epoch and must survive.
        incoming.removeIf(chunk -> {
            if (chunk.epoch >= interruption.epoch) return false;
            bufferedBytes.addAndGet(-chunk.audio.audio().length);
            return true;
        });
        PnjVoiceClient.played(this, interruption.item, interruption.index, samples * 1000 / RealtimeProtocol.SAMPLE_RATE);
    }

    private static String audioKey(String item, int index) { return item + ":" + index; }

    private void release() {
        if (capture != 0) {
            alcCaptureStop(capture);
            alcCaptureCloseDevice(capture);
        }
        if (source != 0) {
            alSourceStop(source);
            alDeleteSources(source);
            allocatedBuffers.forEach(AL10::alDeleteBuffers);
        }
        if (context != 0) {
            EXTThreadLocalContext.alcSetThreadContext(0);
            AL.setCurrentThread(null);
            alcDestroyContext(context);
        }
        if (outputDevice != 0) alcCloseDevice(outputDevice);
        incoming.clear();
    }

    @Override public void close() {
        running = false;
        thread.interrupt();
    }

    private record Chunk(PnjSpeakerPayload audio, int epoch) {}
    private record PlayingBuffer(int buffer, Chunk chunk) {}
    private record Interruption(String item, int index, int epoch) {}
}
