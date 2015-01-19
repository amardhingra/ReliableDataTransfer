import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

public class FileIO {

	public static void writeToFile(String filename, String data)
			throws FileNotFoundException {

		if (filename.equals("stdout")) {
			System.out.println(data);
		} else {
			PrintWriter outputWriter = new PrintWriter(new File(filename));
			outputWriter.write(data);
			outputWriter.flush();
			outputWriter.close();
		}

	}

	public static byte[] readFromFile(String filename) throws IOException {

		if (filename.contains(".jpg") || filename.contains(".png")
				|| filename.contains(".jpeg")) {
			
			BufferedImage image = ImageIO.read(new File(filename));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			
			return baos.toByteArray();
			
		} else {
			BufferedReader inputReader = new BufferedReader(new FileReader(
					filename));
			String lines = "";
			String line = inputReader.readLine();
			while (line != null) {
				lines += line + "\n";
				line = inputReader.readLine();
			}
			inputReader.close();

			lines = lines.substring(0, lines.length() - 1);
			
			return lines.getBytes();
		}
	}

}
