package com.example.newsight;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VoiceCommandHelper {

    private static final String TAG = "VoiceCommandHelper";

    // Audio recording parameters
    private static final int RECORDING_SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private TtsHelper ttsHelper;

    // Voice Activity Detection parameters
    private static final double SILENCE_THRESHOLD = 1500.0;
    private static final long SILENCE_DURATION_MS = 2000;
    private static final long MAX_RECORDING_DURATION_MS = 15000;
    private static final long MIN_RECORDING_DURATION_MS = 500;

    // Wake word detection parameters
    private static final double WAKE_WORD_ENERGY_THRESHOLD = 2000.0;
    private static final long WAKE_WORD_CHECK_INTERVAL_MS = 5000;

    private Context context;
    private AudioRecord audioRecord;
    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private AtomicBoolean isListeningForWakeWord = new AtomicBoolean(false);
    private ExecutorService executorService;
    private OkHttpClient httpClient;
    private NoiseSuppressor noiseSuppressor;
    private AcousticEchoCanceler echoCanceler;
    private Handler mainHandler;

    private static final String BACKEND_URL = "https://cis4398-project-newsight-backend.onrender.com/voice/transcribe";
    private static final String WAKE_WORD_URL = "https://cis4398-project-newsight-backend.onrender.com/voice/wake-word";

    private String sessionId; // Session ID for navigation tracking

    public interface VoiceCommandCallback {
        void onWakeWordDetected();
        void onCommandStarted();
        void onCommandProcessing();
        void onResponseReceived(String jsonResponse);
        void onNavigateToFeature(String feature, JSONObject extractedParams);
        void onError(String error);
        void onComplete();
    }

    private VoiceCommandCallback callback;

    public VoiceCommandHelper(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(2);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());

        this.ttsHelper = new TtsHelper(context);
    }

    /**
     * Set session ID for navigation tracking
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setCallback(VoiceCommandCallback callback) {
        this.callback = callback;
    }

    public void startWakeWordDetection() {
        if (isListeningForWakeWord.get() || isRecording.get()) {
            Log.w(TAG, "Already listening, skipping wake word start");
            return;
        }

        Log.d(TAG, "👂 Starting wake word detection...");
        showToast("Listening for 'Hey Guide'");
        isListeningForWakeWord.set(true);

        executorService.execute(() -> {
            try {
                listenForWakeWord();
            } catch (Exception e) {
                Log.e(TAG, "Error in wake word detection: " + e.getMessage(), e);
                notifyError("Wake word detection error");
                isListeningForWakeWord.set(false);
            }
        });
    }

    public void startDirectRecording() {
        // For manual button press - skip wake word detection
        // First stop any existing wake word detection
        if (isListeningForWakeWord.get()) {
            stopListening();
        }

        executorService.execute(() -> {
            try {
                mainHandler.post(()-> ttsHelper.speak("Hello, How Can I help you?"));
                recordCommandWithVAD();
            } catch (Exception e) {
                Log.e(TAG, "Error in recording: " + e.getMessage(), e);
                notifyError("Recording error");

                // Restart wake word detection even on error
                mainHandler.postDelayed(() -> {
                    try {
                        startWakeWordDetection();
                    } catch (Exception ex) {
                        Log.e(TAG, "Error restarting wake word detection: " + ex.getMessage());
                    }
                }, 1000);
            }
        });
    }

    public void stopListening() {
        isListeningForWakeWord.set(false);
        isRecording.set(false);
        cleanupAudioRecord();
    }

    private void listenForWakeWord() throws IOException {
        int bufferSize = AudioRecord.getMinBufferSize(RECORDING_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        bufferSize = Math.max(bufferSize, RECORDING_SAMPLE_RATE * 2);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            notifyError("Microphone permission required");
            return;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                RECORDING_SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed");
            notifyError("Failed to initialize microphone");
            return;
        }

        setupAudioProcessing();
        audioRecord.startRecording();
        isRecording.set(true);

        ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufferSize / 4];
        long lastCheckTime = System.currentTimeMillis();
        double maxEnergyInWindow = 0;

        Log.d(TAG, "👂 Listening for wake word...");

        while (isRecording.get() && isListeningForWakeWord.get()) {
            int bytesRead = audioRecord.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                bufferStream.write(buffer, 0, bytesRead);

                double rms = calculateRMS(buffer, bytesRead);
                maxEnergyInWindow = Math.max(maxEnergyInWindow, rms);

                long currentTime = System.currentTimeMillis();

                if (currentTime - lastCheckTime >= WAKE_WORD_CHECK_INTERVAL_MS &&
                        maxEnergyInWindow > WAKE_WORD_ENERGY_THRESHOLD) {

                    byte[] audioData = bufferStream.toByteArray();
                    int bytesToCheck = Math.min(audioData.length, RECORDING_SAMPLE_RATE * 2 * 5);
                    byte[] checkData = new byte[bytesToCheck];
                    System.arraycopy(audioData, audioData.length - bytesToCheck, checkData, 0, bytesToCheck);

                    File tempFile = createWavFile(checkData, RECORDING_SAMPLE_RATE);
                    checkForWakeWordAsync(tempFile);

                    lastCheckTime = currentTime;
                    maxEnergyInWindow = 0;

                    bufferStream.reset();
                    int keepBytes = RECORDING_SAMPLE_RATE * 2 * 2;
                    if (audioData.length > keepBytes) {
                        bufferStream.write(audioData, audioData.length - keepBytes, keepBytes);
                    }
                }
            }
        }

        cleanupAudioRecord();
    }

    private void checkForWakeWordAsync(File audioFile) {
        executorService.execute(() -> {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("audio", audioFile.getName(),
                            RequestBody.create(audioFile, MediaType.parse("audio/wav")))
                    .build();

            Request request = new Request.Builder()
                    .url(WAKE_WORD_URL)
                    .post(requestBody)
                    .build();

            try {
                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();

                    try {
                        JSONObject json = new JSONObject(responseBody);
                        boolean wakeWordDetected = json.optBoolean("wake_word_detected", false);
                        String text = json.optString("text", "");

                        Log.d(TAG, "Wake word check: '" + text + "' -> " + wakeWordDetected);

                        if (wakeWordDetected) {
                            Log.d(TAG, "✅ Wake word 'Hey Guide' detected!");
                            isListeningForWakeWord.set(false);
                            cleanupAudioRecord();

                            if (callback != null) {
                                mainHandler.post(() -> {
                                    callback.onWakeWordDetected();
                                    ttsHelper.speak("Hello, How Can I help you?");
                                });
                            }
                            showToast("Listening for command...");

                            Thread.sleep(300);
                            recordCommandWithVAD();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parse error: " + e.getMessage());
                    }
                }
                response.close();
            } catch (Exception e) {
                Log.d(TAG, "Wake word check error: " + e.getMessage());
            } finally {
                audioFile.delete();
            }
        });
    }

    private void recordCommandWithVAD() throws IOException {
        int bufferSize = AudioRecord.getMinBufferSize(RECORDING_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            notifyError("Microphone permission required");
            return;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                RECORDING_SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            notifyError("Failed to initialize microphone");
            return;
        }

        setupAudioProcessing();
        audioRecord.startRecording();
        isRecording.set(true);

        if (callback != null) {
            mainHandler.post(() -> callback.onCommandStarted());
        }
        showToast("Speak now");

        ByteArrayOutputStream recordingStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufferSize / 4];

        long recordingStartTime = System.currentTimeMillis();
        long lastSpeechTime = System.currentTimeMillis();
        boolean speechDetected = false;

        Log.d(TAG, "🎙️ Recording command...");

        while (isRecording.get()) {
            long currentTime = System.currentTimeMillis();
            long recordingDuration = currentTime - recordingStartTime;

            if (recordingDuration > MAX_RECORDING_DURATION_MS) {
                Log.d(TAG, "⏱️ Max duration reached");
                break;
            }

            int bytesRead = audioRecord.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                recordingStream.write(buffer, 0, bytesRead);

                double rms = calculateRMS(buffer, bytesRead);

                if (rms > SILENCE_THRESHOLD) {
                    lastSpeechTime = currentTime;
                    speechDetected = true;
                } else if (speechDetected) {
                    long silenceDuration = currentTime - lastSpeechTime;

                    if (silenceDuration > SILENCE_DURATION_MS &&
                            recordingDuration > MIN_RECORDING_DURATION_MS) {
                        Log.d(TAG, "🔇 Silence detected");
                        break;
                    }
                }
            }
        }

        isRecording.set(false);
        cleanupAudioRecord();

        byte[] audioData = recordingStream.toByteArray();
        Log.d(TAG, "📊 Recorded " + audioData.length + " bytes");

        if (callback != null) {
            mainHandler.post(() -> callback.onCommandProcessing());
            mainHandler.post(() -> ttsHelper.speak("Processing you request"));
        }
        showToast("Processing...");

        File wavFile = createWavFile(audioData, RECORDING_SAMPLE_RATE);
        sendAudioToBackend(wavFile);
    }

    private void sendAudioToBackend(File audioFile) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audio", audioFile.getName(),
                        RequestBody.create(audioFile, MediaType.parse("audio/wav")))
                .build();

        Request.Builder requestBuilder = new Request.Builder()
                .url(BACKEND_URL)
                .post(requestBody);

        // Add session ID header if available (for navigation)
        if (sessionId != null && !sessionId.isEmpty()) {
            Log.d(TAG, "📤 Adding session ID header: " + sessionId);
            requestBuilder.addHeader("X-Session-Id", sessionId);
        } else {
            Log.w(TAG, "⚠️ Session ID is NULL - navigation won't work!");
        }

        Request request = requestBuilder.build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ Network error: " + e.getMessage());
                notifyError("Connection error: " + e.getMessage());

                // Restart wake word detection after error
                mainHandler.postDelayed(() -> {
                    try {
                        startWakeWordDetection();
                    } catch (Exception ex) {
                        Log.e(TAG, "Error restarting wake word detection: " + ex.getMessage());
                    }
                }, 1000);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                audioFile.delete(); // Clean up the audio file

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "📦 Response received: " + responseBody);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        // Extract feature from extracted_params
                        JSONObject extractedParams = jsonResponse.optJSONObject("extracted_params");
                        String feature = null;

                        if (extractedParams != null) {
                            feature = extractedParams.optString("feature", null);
                        }

                        // First notify with full response
                        if (callback != null) {
                            final String finalFeature = feature;
                            final JSONObject finalExtractedParams = extractedParams;

                            mainHandler.post(() -> {
                                callback.onResponseReceived(responseBody);

                                // If feature is present and not null, trigger navigation
                                if (finalFeature != null && !finalFeature.isEmpty() &&
                                        !finalFeature.equals("null") && !finalFeature.equals("None")) {
                                    Log.d(TAG, "🧭 Navigating to feature: " + finalFeature);
                                    callback.onNavigateToFeature(finalFeature, finalExtractedParams);
                                }

                                callback.onComplete();
                            });
                        }

                        // Restart wake word detection after processing
                        mainHandler.postDelayed(() -> {
                            try {
                                startWakeWordDetection();
                            } catch (Exception e) {
                                Log.e(TAG, "Error restarting wake word detection: " + e.getMessage());
                            }
                        }, 1000);

                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        notifyError("Error parsing response");

                        // Still restart wake word detection
                        mainHandler.postDelayed(() -> {
                            try {
                                startWakeWordDetection();
                            } catch (Exception ex) {
                                Log.e(TAG, "Error restarting wake word detection: " + ex.getMessage());
                            }
                        }, 1000);
                    }

                } else {
                    notifyError("Server error: " + response.code());

                    // Restart wake word detection after error
                    mainHandler.postDelayed(() -> {
                        try {
                            startWakeWordDetection();
                        } catch (Exception e) {
                            Log.e(TAG, "Error restarting wake word detection: " + e.getMessage());
                        }
                    }, 1000);
                }
            }
        });
    }

    private double calculateRMS(byte[] audioData, int length) {
        long sum = 0;
        int samples = 0;

        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sum += sample * sample;
            samples++;
        }

        return samples == 0 ? 0 : Math.sqrt((double) sum / samples);
    }

    private void setupAudioProcessing() {
        if (audioRecord == null) return;

        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());
            if (noiseSuppressor != null) {
                noiseSuppressor.setEnabled(true);
                Log.d(TAG, "🔇 Noise suppressor enabled");
            }
        }

        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
            if (echoCanceler != null) {
                echoCanceler.setEnabled(true);
                Log.d(TAG, "🔊 Echo canceler enabled");
            }
        }
    }

    private void cleanupAudioRecord() {
        if (audioRecord != null) {
            try {
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioRecord: " + e.getMessage());
            }
            audioRecord = null;
        }

        if (noiseSuppressor != null) {
            noiseSuppressor.release();
            noiseSuppressor = null;
        }

        if (echoCanceler != null) {
            echoCanceler.release();
            echoCanceler = null;
        }
    }

    private File createWavFile(byte[] audioData, int sampleRate) throws IOException {
        File wavFile = new File(context.getCacheDir(), "vc_" + System.currentTimeMillis() + ".wav");
        FileOutputStream fos = new FileOutputStream(wavFile);

        int totalDataLen = audioData.length + 36;
        int channels = 1;
        int byteRate = sampleRate * channels * 2;

        byte[] header = new byte[44];
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0; header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 2); header[33] = 0;
        header[34] = 16; header[35] = 0;
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (audioData.length & 0xff);
        header[41] = (byte) ((audioData.length >> 8) & 0xff);
        header[42] = (byte) ((audioData.length >> 16) & 0xff);
        header[43] = (byte) ((audioData.length >> 24) & 0xff);

        fos.write(header);
        fos.write(audioData);
        fos.close();

        return wavFile;
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    private void notifyError(String error) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onError(error);
            }
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
        });
    }

    public void cleanup() {
        stopListening();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}