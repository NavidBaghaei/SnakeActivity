package csc133.snakeactivity;

import android.content.Context;

public class AudioContext {
    private static Audio audioStrategy;

    public AudioContext(Audio strategy) {
        this.audioStrategy = strategy;
    }

    public void setStrategy(Audio strategy) {
        this.audioStrategy = strategy;
    }

    public void loadSounds(Context context) {
        audioStrategy.loadSounds(context);
    }

    public void playEatSound() {
        audioStrategy.playEatSound();
    }

    public void playBadEatSound() {
        audioStrategy.playBadEatSound();
    }

    public static void playSharkSwimSound() {
        audioStrategy.playSharkSwimSound();
    }

    public static void stopSharkSwimSound() {
        audioStrategy.stopSharkSwimSound();
    }

    public static void playSharkBiteSound() {
        audioStrategy.playSharkBiteSound();
    }
    public void playCrashSound() {
        audioStrategy.playCrashSound();
    }

    // Add methods for background music and power-up sound
    public void startBackgroundMusic() {
        audioStrategy.startBackgroundMusic();
    }

    public void stopBackgroundMusic() {
        audioStrategy.stopBackgroundMusic();
    }

    public void playPowerUpMusic() {
        audioStrategy.playPowerUpMusic();
    }

    public void stopPowerUpMusic() {
        audioStrategy.stopPowerUpMusic();
    }

    public boolean isMusicPlaying() {
        return audioStrategy.isMusicPlaying();
    }

    public void playSpeedBoostSound() {
        audioStrategy.playSpeedBoostSound();
    }
    void playSharkDeathSound() {
        audioStrategy.playSharkDeathSound();
    }

    public void setMusicVolume(float volume) {
        audioStrategy.setMusicVolume(volume);
    }

}
