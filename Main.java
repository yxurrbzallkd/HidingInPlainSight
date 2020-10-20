import java.util.Scanner;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
	public static void Example() throws IOException{
		String imgPath = "img.png";
		String message = readFromFile("text.txt");
		System.out.println("Text to hide:\n"+message);
		incode(imgPath, message, "imgOut.png");
		outcode("imgOut.png", "out.txt");
		System.out.println("Decoded text:\n"+readFromFile("out.txt"));
	}

	public static void main(String[] args) throws IOException{
        Scanner scanner = new Scanner(System.in);
		Example();
	}

	public static String readFromFile(String filePath) throws IOException{
		String content = Files.readString(Path.of(filePath));
		return content;
	}

	public static void writeToFile(String str, String fileName) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
	    writer.write(str);
	    writer.close();
	}

	public static int[] textToTnts(String text){
		/*
		Split bytes of chars into individual bits - return array of 1s and 0s
		*/
		byte[] message = text.getBytes();
		int[] splitMessage = new int[message.length*8];
		int c = 0;
		for (int i = 0; i < message.length; i++){
			for (int j = 0; j < 8; j++){
				splitMessage[c] = (message[i] >> (7-j)) & 1;
				c++;
			}
		}
		return splitMessage;
	}
    public static int incodeMessage(int i, int c, int[] message){
		/*
		change 2 last bits of color component value for letters in message
		*/
		if (i >= message.length){
			return c;
		}
		return  ((c >> 2) << 2) | (((message[i] << 1) | (message[i+1])) & 3);
	}

	public static void incode(String imgPath, String input, String outPath) throws IOException{
		/*
		hide given string into the given image
		save the result into outPath.png
		! only hide information in RGB
		(alpha value can give your secret away (strange mist on the image with transparent background - suspicios))
		*/
		String text = input.length()+";"+input;
		BufferedImage bf = ImageIO.read(new File(imgPath));
		int[] message = textToTnts(text);

		int c = 0;
		for (int i = 0; i < bf.getWidth(); i++){
			for (int j = 0; j < bf.getHeight(); j++){
				Color color = new Color(bf.getRGB(i, j), true);
				int a = color.getAlpha();

				int r = color.getRed();
				int g = color.getGreen();
				int b = color.getBlue();

				r = incodeMessage(c, r, message);
				c += 2;
				g = incodeMessage(c, g, message);
				c += 2;
				b = incodeMessage(c, b, message);
				c += 2;

				int newPixel = (a << 24) | (r << 16) | (g << 8) | b;
				bf.setRGB(i, j, newPixel);
				if (c >= message.length){
					break;
				}
			}
			if (c >= message.length){
				break;
			}
		}
		ImageIO.write(bf, "png", new File(outPath));
	}

	public static char[] groupChars(int[] message){
		/*
		group pieces, 2 bits in each into bytes (1 char - 4 pieces - 1 byte) 
		*/
		char[] mergedChars = new char[message.length/4];
		int id = 0;
		int i = 0;
		while (i < mergedChars.length){
			int c = 0;
			for (int j = 0; j < 4; j++){
				//shift bits we have so far 2 positions to the left to free space for next 2
				//add next 2
				c = c<<2 | (message[i+j]&3);
			}
			mergedChars[id] = (char) c;
			id++;
			i += 4;
		}
		return mergedChars;
	}

	public static int outcodeMessage(int c){
		return c & 3;
	}

	public static void outcode(String imgPath, String outPath) throws IOException{
		/*
		decode message hidden in given image and write result to "out.txt""
		*/
		BufferedImage bf = ImageIO.read(new File(imgPath));
		int[] message = new int[(bf.getWidth()*bf.getHeight()/4)*4]; //2 bits per 1 color component, 8 bits in a char
	
		int c = 0;
		for (int i = 0; i < bf.getWidth()-1; i++){
			for (int j = 0; j < bf.getHeight()-1; j++){
				Color color = new Color(bf.getRGB(i, j), true);
				int a = color.getAlpha();

				int r = color.getRed();
				int g = color.getGreen();
				int b = color.getBlue();

				if (c < message.length){
					message[c] = outcodeMessage(r);
					if (c+1 < message.length){
						message[c+1] = outcodeMessage(g);
						if (c+2 < message.length){
							message[c+2] = outcodeMessage(b);
						}
					}
				}
				c += 3;
				if (c >= message.length){
					break;
				}
			}
			if (c >= message.length){
				break;
			}
		}
		char[] mergedChars = groupChars(message);

		//find message size (integer before first semicolon)
		StringBuilder bs = new StringBuilder();
		for (int i = 0; i < mergedChars.length; i++){
			if (mergedChars[i] == ';'){
				break;
			}
			bs.append(mergedChars[i]);
		}

		//cut message out
		String messageSizeStr = bs.toString(); //the one we incoded previously
		int messageSize = Integer.parseInt(messageSizeStr);
		String decoded = "";
		for (int i = messageSizeStr.length()+1; i < messageSizeStr.length()+messageSize+1; i++){
			decoded += mergedChars[i];
		}
		writeToFile(decoded, outPath);
	}
}