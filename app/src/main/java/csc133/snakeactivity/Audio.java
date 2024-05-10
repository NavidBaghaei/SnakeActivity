package csc133.snakeactivity;

import android.content.Context;

public interface Audio {
    void loadSounds(Context context);
    void playEatSound();
    void playCrashSound();
    void startBackgroundMusic();
    void stopBackgroundMusic();
    void playPowerUpMusic();
    void stopPowerUpMusic();
    boolean isMusicPlaying();
    void setMusicVolume(float volume);
    void playSpeedBoostSound();
    void playBadEatSound();
    void playSharkSwimSound();
    void stopSharkSwimSound();
    void playSharkBiteSound();
    void playSharkDeathSound();
}

