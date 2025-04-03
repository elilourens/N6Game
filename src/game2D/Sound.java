package game2D;

import javax.sound.sampled.*;
import java.io.File;
import java.util.List;

public class Sound {

	private List<String> playlist;
	private int currentIndex = 0;
	private Clip clip;

	public Sound(List<String> playlist) {
		this.playlist = playlist;
	}

	public void startLooping() {
		playNext();
	}

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
				System.err.println("Error playing one-shot sound: " + filepath);
				e.printStackTrace();
			}
		}).start();
	}


	private void playNext() {
		try {
			String filename = playlist.get(currentIndex);
			File file = new File(filename);
			if (!file.exists()) {
				System.err.println("File not found: " + file.getAbsolutePath());
				return;
			}

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

			clip = AudioSystem.getClip();
			clip.open(decodedStream);
			clip.start();

			// When it ends, move to next track
			clip.addLineListener(event -> {
				if (event.getType() == LineEvent.Type.STOP) {
					clip.close();
					currentIndex = (currentIndex + 1) % playlist.size();
					playNext();
				}
			});

		} catch (Exception e) {
			System.err.println("Error playing track: " + playlist.get(currentIndex));
			e.printStackTrace();
		}
	}
	public void stop() {

		if (clip != null) {
			clip.stop();
			clip.close();
		}

	}

}
