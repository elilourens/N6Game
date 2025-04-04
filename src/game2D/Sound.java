package game2D;

import javax.sound.sampled.*;
import java.io.File;
import java.util.List;

public class Sound {


	private List<String> playlist;
	private int currentIndex = 0;
	private Clip clip;
	private boolean stoppedManually = false;


	private boolean bitCrusherEnabled = false;

	public Sound(List<String> playlist) {
		this.playlist = playlist;
	}

	public void startLooping() {
		playNext();
	}

	public void enableBitCrusher(boolean enable) {
		this.bitCrusherEnabled = enable;
	}


	//playing the wav sound effects I use this method:
	public static void playOnce(String filepath) {
		new Thread(() -> {
			try {
				File file = new File(filepath);
				if (!file.exists()) {
					System.err.println("Sound file not found: " + file.getAbsolutePath());
					return;
				}

				AudioInputStream stream = AudioSystem.getAudioInputStream(file);
				AudioFormat base = stream.getFormat();
				AudioFormat decoded = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
						base.getSampleRate(), 16, base.getChannels(),
						base.getChannels() * 2, base.getSampleRate(), false);
				stream = AudioSystem.getAudioInputStream(decoded, stream);

				Clip clip = AudioSystem.getClip();
				clip.open(stream);
				clip.start();
			} catch (Exception e) {

			}
		}).start();
	}

	//Method to create the distortion effect when you are 1hp.
	private byte[] applyBitCrusher(byte[] samples) {
		byte[] result = new byte[samples.length];

		int step = 548;

		//go through the array two bytes at a time
		for (int i = 0; i < samples.length - 1; i += 2) {

			//gets two bytes and combine them.
			int low = samples[i] & 0xFF;
			int high = samples[i + 1];
			int sample = (high << 8) | low;


			sample = (sample / step) * step;

			//make sure the values are valid.
			if (sample > Short.MAX_VALUE) sample = Short.MAX_VALUE;
			if (sample < Short.MIN_VALUE) sample = Short.MIN_VALUE;


			result[i] = (byte) (sample & 0xFF);
			result[i + 1] = (byte) ((sample >> 8) & 0xFF);
		}

		return result;
	}


	//main method to play background music !
	private void playNext() {
		try { //incase there is an issue with loading music.
			String filename = playlist.get(currentIndex);
			File file = new File(filename);


			AudioInputStream stream = AudioSystem.getAudioInputStream(file);
			AudioFormat baseFormat = stream.getFormat();
			AudioFormat decodedFormat = new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED,
					baseFormat.getSampleRate(),
					16,
					baseFormat.getChannels(),
					baseFormat.getChannels() * 2,
					baseFormat.getSampleRate(),
					false
			);

			AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, stream);

			// If bitcrusher is enabled
			if (bitCrusherEnabled) {
				byte[] rawBytes = decodedStream.readAllBytes();
				byte[] crushed = applyBitCrusher(rawBytes);

				decodedStream = new AudioInputStream(
						new java.io.ByteArrayInputStream(crushed),
						decodedFormat,
						crushed.length / decodedFormat.getFrameSize()
				);
			}

			clip = AudioSystem.getClip();
			clip.open(decodedStream);
			clip.start();

			clip.addLineListener(event -> {
				if (event.getType() == LineEvent.Type.STOP) {
					clip.close();
					clip = null;

					if (!stoppedManually) {
						currentIndex = (currentIndex + 1) % playlist.size();
						playNext();
					}
					stoppedManually = false; //reset for next track
				}
			});


		} catch (Exception e) {

		}
	}

	//Stop the music. Used to reset bg music.
	public void stop() {
		if (clip != null && clip.isRunning()) {
			stoppedManually = true;
			clip.stop();
			clip.close();
			clip = null;
		}
	}


}