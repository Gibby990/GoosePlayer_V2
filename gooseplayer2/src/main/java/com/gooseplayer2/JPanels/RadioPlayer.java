package com.gooseplayer2.JPanels;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.gooseplayer2.Packages.Slugcat;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;
import net.beadsproject.beads.ugens.Gain;

import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;

public class RadioPlayer extends JPanel {

    private GridBagConstraints gbc;
    private GridBagLayout layout;
    private JLabel channelLabel, timeLabel, albumArtLabel, urlLabel;
    private JSlider progressBar, volumeSlider;
    private JButton playPauseButton, enterButton;
    private JTextField urlField;

    private JavaSoundAudioIO audioIO;
    private AudioContext audioContext;
    private Gain streamGain;
    private StreamingUGen streamingUGen;
    private Thread decoderThread;
    private volatile boolean isPlaying = false;
    private volatile boolean isPaused = false;
    private volatile boolean stopRequested = false;
    private String currentUrl = "";

    public RadioPlayer() {
        layout = new GridBagLayout();
        gbc = new GridBagConstraints();
        setLayout(layout);

        Slugcat helper = new Slugcat();

        playPauseButton = new JButton("Play");
        playPauseButton.addActionListener(e -> {
            if (!isPlaying) {
                String url = getStreamUrl();
                if (url == null || url.isBlank()) return;
                startStream(url);
            } else if (!isPaused) {
                pauseStream();
            } else {
                resumeStream();
            }
        });

        enterButton = new JButton("Enter");
        enterButton.addActionListener(e -> {
            String url = getStreamUrl();
            if (url == null || url.isBlank()) return;
            startStream(url);
        });

        progressBar = new JSlider(0, 0, 100, 0);
        progressBar.setEnabled(false);

        timeLabel = new JLabel("0:00 / 0:00");
        channelLabel = new JLabel("Radio");

        volumeSlider = new JSlider(0, 100, 100);
        volumeSlider.addChangeListener(e -> {
            if (streamGain != null && !volumeSlider.getValueIsAdjusting()) {
                float vol = volumeSlider.getValue() / 100.0f;
                streamGain.setGain(vol);
            }
        });

        albumArtLabel = new JLabel();
        albumArtLabel.setHorizontalAlignment(SwingConstants.CENTER);
        albumArtLabel.setVerticalAlignment(SwingConstants.CENTER);
        albumArtLabel.setPreferredSize(new Dimension(128, 128));
        setDefaultAlbumArt();

        urlLabel = new JLabel("Stream URL:");
        urlLabel.setFont(urlLabel.getFont().deriveFont(Font.PLAIN, 25f));
        urlField = new JTextField();
        urlField.setToolTipText("Enter AzuraCast stream URL");

        audioIO = new JavaSoundAudioIO();
        audioContext = new AudioContext(audioIO);

        gbc.gridheight = 3;
        gbc.gridwidth = 6;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        gbc.fill = GridBagConstraints.CENTER;
        helper.addObjects(channelLabel, this, layout, gbc, 0, 0, 4, 1);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 20, 0, 0);
        helper.addObjects(progressBar, this, layout, gbc, 0, 1, 4, 1);
        helper.addObjects(timeLabel, this, layout, gbc, 4, 1, 1, 1);

        helper.addObjects(volumeSlider, this, layout, gbc, 0, 2, 2, 1);

        gbc.fill = GridBagConstraints.NONE;
        helper.addObjects(albumArtLabel, this, layout, gbc, 5, 0, 1, 3);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 20, 0, 5);
        helper.addObjects(playPauseButton, this, layout, gbc, 0, 3, 1, 1);

        JPanel urlPanel = new JPanel(new GridLayout(1, 3));
        urlPanel.add(urlLabel);
        urlPanel.add(urlField);
        urlPanel.add(enterButton);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        helper.addObjects(urlPanel, this, layout, gbc, 1, 4, 6, 1);
    }

    public String getStreamUrl() {
        return urlField != null ? urlField.getText() : "";
    }

    public void setStreamUrl(String url) {
        if (urlField != null) {
            urlField.setText(url != null ? url : "");
        }
    }

    private void setDefaultAlbumArt() {
        try {
            URL url = getClass().getResource("/icons/albumMissing.png");
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                Image scaled = icon.getImage().getScaledInstance(128, 128, Image.SCALE_SMOOTH);
                albumArtLabel.setIcon(new ImageIcon(scaled));
            } else {
                albumArtLabel.setIcon(null);
            }
        } catch (Exception e) {
            albumArtLabel.setIcon(null);
        }
    }

    private void startStream(String urlString) {
        try {
            stopStream();
            currentUrl = urlString;

            if (streamingUGen == null) {
                streamingUGen = new StreamingUGen(audioContext, 2);
            } else {
                streamingUGen.clear();
            }

            streamGain = new Gain(audioContext, 2, volumeSlider.getValue() / 100.0f);
            streamGain.addInput(streamingUGen);

            audioContext.out.removeAllConnections(streamGain);
            audioContext.out.addInput(streamGain);
            if (!audioContext.isRunning()) {
                audioContext.start();
            }

            stopRequested = false;
            isPaused = false;

            decoderThread = new Thread(() -> decodeLoop(currentUrl, streamingUGen));
            decoderThread.setName("RadioStreamDecoder");
            decoderThread.setDaemon(true);
            decoderThread.start();

            isPlaying = true;
            updatePlayPauseLabel();
        } catch (Exception ex) {
            ex.printStackTrace();
            stopStream();
        }
    }

    private void pauseStream() {
        isPaused = true;
        updatePlayPauseLabel();
    }

    private void resumeStream() {
        isPaused = false;
        updatePlayPauseLabel();
    }

    private void stopStream() {
        stopRequested = true;
        isPaused = false;
        if (decoderThread != null) {
            try {
                decoderThread.interrupt();
            } catch (Exception ignored) {}
            decoderThread = null;
        }
        if (streamingUGen != null) {
            streamingUGen.clear();
        }
        try {
            if (streamGain != null) {
                audioContext.out.removeAllConnections(streamGain);
            }
        } catch (Exception ignored) {}
        isPlaying = false;
        updatePlayPauseLabel();
    }

    private void updatePlayPauseLabel() {
        SwingUtilities.invokeLater(() -> {
            if (playPauseButton != null) {
                playPauseButton.setText(!isPlaying ? "Play" : (isPaused ? "Play" : "Pause"));
            }
        });
    }

    private void decodeLoop(String urlString, StreamingUGen sink) {
        AudioInputStream stream = null;
        try {
            String resolved = resolveStreamUrl(urlString);
            URL url = new URL(resolved);

            URLConnection conn = openConnectionWithHeaders(url);
            String contentType = conn.getContentType();
            if (contentType != null) {
                String ct = contentType.toLowerCase();
                if (ct.contains("aac") || ct.contains("aacp") || ct.contains("x-aac")) {
                    throw new javax.sound.sampled.UnsupportedAudioFileException(
                            "AAC/AACP stream not supported. Use an MP3 mount.");
                }
                if (ct.contains("application/vnd.apple.mpegurl") || ct.contains("vnd.apple.mpegurl") || ct.contains("mpegurl")
                        || ct.contains("hls")) {
                    throw new javax.sound.sampled.UnsupportedAudioFileException(
                            "HLS playlist not supported. Use a direct MP3 stream URL.");
                }
            }

            InputStream raw = conn.getInputStream();
            BufferedInputStream buffered = new BufferedInputStream(raw, 64 * 1024);

            if (!looksLikeMp3(buffered)) {
                throw new javax.sound.sampled.UnsupportedAudioFileException(
                        "Incoming stream does not look like MP3 data (might be AAC/HLS/HTML).");
            }

            try {
                stream = AudioSystem.getAudioInputStream(buffered);
            } catch (Exception primaryFail) {
                try {
                    MpegAudioFileReader reader = new MpegAudioFileReader();
                    stream = reader.getAudioInputStream(buffered);
                } catch (Exception fallbackFail) {
                    primaryFail.addSuppressed(fallbackFail);
                    throw primaryFail;
                }
            }

            float targetSampleRate = audioContext.getSampleRate();
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    targetSampleRate,
                    16,
                    2,
                    4,
                    targetSampleRate,
                    false
            );

            if (!isTargetFormat(stream.getFormat(), targetFormat)) {
                stream = AudioSystem.getAudioInputStream(targetFormat, stream);
            }

            byte[] byteBuf = new byte[4096];
            while (!stopRequested) {
                if (isPaused) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                    }
                    continue;
                }
                int read = stream.read(byteBuf, 0, byteBuf.length);
                if (read <= 0) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ie) {
                    }
                    continue;
                }
                int frames = (read / 4);
                if (frames <= 0) continue;
                int bytesToConvert = frames * 4;
                float[] out = new float[frames * 2];
                int bi = 0;
                int oi = 0;
                while (bi + 3 < bytesToConvert) {
                    int loL = byteBuf[bi] & 0xFF;
                    int hiL = byteBuf[bi + 1];
                    int valL = (hiL << 8) | loL;
                    bi += 2;
                    int loR = byteBuf[bi] & 0xFF;
                    int hiR = byteBuf[bi + 1];
                    int valR = (hiR << 8) | loR;
                    bi += 2;
                    out[oi++] = clamp16ToFloat(valL);
                    out[oi++] = clamp16ToFloat(valR);
                }
                sink.enqueue(out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String resolveStreamUrl(String urlString) {
        try {
            String lower = urlString.toLowerCase();
            if (lower.endsWith(".m3u") || lower.endsWith(".m3u8") || lower.endsWith(".pls")) {
                URL u = new URL(urlString);
                URLConnection conn = u.openConnection();
                conn.setRequestProperty("User-Agent", "GoosePlayer2/1.0 (Java)");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        if (lower.endsWith(".pls")) {
                            int idx = line.indexOf('=');
                            if (idx > 0) {
                                String key = line.substring(0, idx).trim().toLowerCase();
                                String val = line.substring(idx + 1).trim();
                                if (key.startsWith("file") && isProbablyStreamUrl(val)) {
                                    return val;
                                }
                            }
                        } else if (isProbablyStreamUrl(line)) {
                            return line;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return urlString;
    }

    private boolean isProbablyStreamUrl(String s) {
        String sl = s.toLowerCase();
        return sl.startsWith("http://") || sl.startsWith("https://");
    }

    private URLConnection openConnectionWithHeaders(URL url) throws Exception {
        URLConnection conn = url.openConnection(Proxy.NO_PROXY);
        if (conn instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) conn;
            http.setInstanceFollowRedirects(true);
        }
        conn.setRequestProperty("User-Agent", "GoosePlayer2/1.0 (Java; like Winamp)");
        conn.setRequestProperty("Accept", "audio/mpeg, audio/*;q=0.5, */*;q=0.1");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        return conn;
    }

    private boolean looksLikeMp3(BufferedInputStream in) {
        try {
            in.mark(4096);
            byte[] buf = new byte[4096];
            int n = in.read(buf);
            in.reset();
            if (n <= 0) return false;
            if (n >= 3 && buf[0] == 'I' && buf[1] == 'D' && buf[2] == '3') return true;
            for (int i = 0; i < n - 1; i++) {
                int b0 = buf[i] & 0xFF;
                int b1 = buf[i + 1] & 0xFF;
                if (b0 == 0xFF && (b1 & 0xE0) == 0xE0) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTargetFormat(AudioFormat src, AudioFormat target) {
        return src.getEncoding().equals(target.getEncoding())
                && Math.abs(src.getSampleRate() - target.getSampleRate()) < 1.0
                && src.getSampleSizeInBits() == target.getSampleSizeInBits()
                && src.getChannels() == target.getChannels()
                && src.isBigEndian() == target.isBigEndian();
    }

    private float clamp16ToFloat(int sample16) {
        float f = sample16 / 32768f;
        if (f > 1f) f = 1f;
        if (f < -1f) f = -1f;
        return f;
    }

    private static class StreamingUGen extends UGen {
        private final LinkedBlockingQueue<float[]> queue = new LinkedBlockingQueue<>(64);
        private float[] current;
        private int cursor = 0;

        public StreamingUGen(AudioContext ac, int outs) {
            super(ac, outs);
        }

        public void enqueue(float[] chunk) {
            if (chunk == null || chunk.length == 0) return;
            if (!queue.offer(chunk)) {
                queue.poll();
                queue.offer(chunk);
            }
        }

        public void clear() {
            queue.clear();
            current = null;
            cursor = 0;
        }

        @Override
        public void calculateBuffer() {
            int bufferSize = bufOut[0].length;
            for (int i = 0; i < bufferSize; i++) {
                float left = 0f, right = 0f;
                if (ensureDataAvailable()) {
                    left = current[cursor++];
                    if (cursor < current.length) {
                        right = current[cursor++];
                    } else {
                        if (ensureDataAvailable()) {
                            right = current[cursor++];
                            if (cursor < current.length) cursor++;
                        }
                    }
                }
                bufOut[0][i] = left;
                if (bufOut.length > 1) {
                    bufOut[1][i] = right;
                }
            }
        }

        private boolean ensureDataAvailable() {
            if (current != null && cursor + 1 < current.length) return true;
            current = queue.poll();
            cursor = 0;
            return current != null && current.length >= 2;
        }
    }
}
