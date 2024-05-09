package csc133.snakeactivity;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

import java.io.IOException;

public class ExtendedAudioStrategy implements Audio {
    private SoundPool soundPool;
    private MediaPlayer backgroundMusic;
    private MediaPlayer powerUpMusic; // Separate MediaPlayer for power-up sound
    private int eatSoundId;
    private int crashSoundId;
    private int speedBoostSoundId;
    private int badWormSoundID;
    private int sharkSwimSoundID;
    private int sharkSwimStreamId = 0;
    private int sharkBiteSoundID;

    @Override
    public void loadSounds(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        AssetManager assetManager = context.getAssets();

        try {
            AssetFileDescriptor descriptor = assetManager.openFd("get_worm.ogg");
            eatSoundId = soundPool.load(descriptor, 1);
            descriptor = assetManager.openFd("death.ogg");
            crashSoundId = soundPool.load(descriptor, 1);
            descriptor = assetManager.openFd("speed_boost.ogg");
            speedBoostSoundId = soundPool.load(descriptor, 1);
            descriptor = assetManager.openFd("get_bad_worm.ogg");
            badWormSoundID = soundPool.load(descriptor, 1);
            descriptor = assetManager.openFd("shark_swim.ogg");
            sharkSwimSoundID = soundPool.load(descriptor, 1);
            descriptor = assetManager.openFd("shark_bite.ogg");
            sharkBiteSoundID = soundPool.load(descriptor, 1);
            loadBackgroundMusic(context);
            loadPowerUpMusic(context);
        } catch (IOException e) {
            Log.e("ExtendedAudioStrategy", "Error loading sounds", e);
        }
    }

    private void loadBackgroundMusic(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor = assetManager.openFd("background_music.ogg");
            backgroundMusic = new MediaPlayer();
            backgroundMusic.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            backgroundMusic.setLooping(true);
            backgroundMusic.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            backgroundMusic.prepareAsync(); // Use asynchronous preparation
        } catch (IOException e) {
            Log.e("ExtendedAudioStrategy", "Error loading background music", e);
        }
    }



    private void loadPowerUpMusic(Context context) throws IOException {
        AssetFileDescriptor descriptor = context.getAssets().openFd("power_up.ogg");
        powerUpMusic = new MediaPlayer();
        powerUpMusic.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
        powerUpMusic.prepare();
    }

    @Override
    public void playEatSound() {
        if (soundPool != null && eatSoundId != 0) {
            soundPool.play(eatSoundId, 0.5F, 0.5F, 0, 0, 1);
        } else {
            Log.e("ExtendedAudioStrategy", "SoundPool or eatSoundId not initialized");
        }
    }

    @Override
    public void playBadEatSound() {
        soundPool.play(badWormSoundID, 0.8F, 0.8F, 0, 0, 1);
    }
    @Override
    public void playSpeedBoostSound() {
        soundPool.play(speedBoostSoundId, 0.2F, 0.2F, 0, 0, 1);
    }

    @Override
    public void playSharkSwimSound() {
        if (soundPool != null && sharkSwimSoundID != 0) {
            // Store the stream ID so it can be stopped later
            sharkSwimStreamId = soundPool.play(sharkSwimSoundID, 1, 1, 0, 1, 1);
        } else {
            Log.e("ExtendedAudioStrategy", "SoundPool or sharkSwimSoundID not initialized");
        }
    }

    @Override
    public void stopSharkSwimSound() {
        if (soundPool != null && sharkSwimStreamId != 0) {
            soundPool.stop(sharkSwimStreamId);
        }
    }

    @Override
    public void playSharkBiteSound() {
        soundPool.play(sharkBiteSoundID, 1, 1, 0, 0, 1);
    }

    @Override
    public void playCrashSound() {
        soundPool.play(crashSoundId, 1, 1, 0, 0, 1);
    }


    @Override
    public void startBackgroundMusic() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.start();
        }
    }

    @Override
    public void stopBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }
    @Override
    public void playPowerUpSound() {
        if (powerUpMusic != null) {
            stopBackgroundMusic();  // Stop background music
            powerUpMusic.start();   // Start playing power-up sound
            powerUpMusic.setOnCompletionListener(mp -> startBackgroundMusic()); // Resume background music after power-up sound finishes
        }
    }
    public boolean isMusicPlaying() {
        return (backgroundMusic != null && backgroundMusic.isPlaying());
    }
    public void setMusicVolume(float volume) {
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(volume, volume);
        }
    }

}
