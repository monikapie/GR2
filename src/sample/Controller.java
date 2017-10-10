package sample;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {
    @FXML
    GridPane gridPane;
    @FXML
    Button openPPMbtn;
    @FXML
    Button openJPGbtn;
    @FXML
    Button saveJPGbtn;
    @FXML
    ImageView PPMimageView;

    @FXML
    private void openJPG() throws IOException {
        File file = getFile();
        if (file != null && file.getName().endsWith(".jpg")) {
            openFile(file);
        }
    }

    @FXML
    private void compressAndSaveJPG() {
        openDialog();
    }

    @FXML
    private void openPPM() throws IOException {
        File file = getFile();
        if (file.getName().endsWith(".ppm")) {
            setPPMImage(file);
        }
    }

    private void setPPMImage(File file) throws IOException {
        Image imageToSet = SwingFXUtils.toFXImage(readChoosenPPM(file.getPath()), null);
        PPMimageView.setImage(imageToSet);
    }

    private File getFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose resource");
        Stage stage = (Stage) gridPane.getScene().getWindow();
        return fileChooser.showOpenDialog(stage);
    }

    private void saveChosenJPG(String compressionLvl) {
        JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
        jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        float compression = Float.valueOf(compressionLvl);
        jpegParams.setCompressionQuality(compression);
        File file = getFile();
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try {
            if (file != null) {
                writer.setOutput(new FileImageOutputStream(
                        new File(file.getAbsolutePath().split("\\.")[0] + "AfterCmp" + ".jpg")));
                BufferedImage bufferedImage = ImageIO.read(file);
                writer.write(null, new IIOImage(bufferedImage, null, null), jpegParams);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openDialog() {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(gridPane.getScene().getWindow());
        VBox dialogVbox = new VBox(30);
        final TextField textField = new TextField();
        Button button = new Button();
        button.setText("Ok");
        dialogVbox.getChildren().add(new Text("Enter compression level ( ex. 0.7f)"));
        dialogVbox.getChildren().add(textField);
        dialogVbox.getChildren().add(button);
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                saveChosenJPG(textField.getText());
                dialog.close();
            }
        });
        Scene dialogScene = new Scene(dialogVbox, 300, 200);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    private void openFile(File file) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(file);
        Image image = SwingFXUtils.toFXImage(bufferedImage, null);
        PPMimageView.setImage(image);
    }

    private BufferedImage readChoosenPPM(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
        String px = reader.readLine();
        String dimensions = getUncommentedLine(reader.readLine(), reader);
        Pattern demensionPattern = Pattern.compile("(\\d+) (\\d+)");
        Matcher dimensionMatcher = demensionPattern.matcher(dimensions);
        String range = getUncommentedLine(reader.readLine(), reader);
        if (dimensionMatcher.matches()) {
            if(px.endsWith("3")){
                return getBufferedImageFromP3(reader, dimensionMatcher);
            } else if(px.endsWith("6")){
                return getBufferedImageFromP6(filePath,
                        Integer.parseInt(dimensionMatcher.group(1)),
                        Integer.parseInt(dimensionMatcher.group(2)),
                        Integer.parseInt(range));
            } else {
                throw new IOException("Could not read this file. Check if it is ppm file.");
            }
        } else {
            throw new IOException("Could not read this file. Check if it is ppm file.");
        }
    }

    private BufferedImage getBufferedImageFromP3(BufferedReader reader, Matcher dimensionMatcher) throws IOException {
        RGBImage rgbImage =  getRgbImage(reader, dimensionMatcher);
        BufferedImage image = new BufferedImage(rgbImage.getNumrows(), rgbImage.getNumcolumns(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < rgbImage.getNumcolumns(); y++) {
            for (int x = 0; x < rgbImage.getNumrows(); x++) {
                int rgb = rgbImage.getRed()[y][x];
                rgb = (rgb << 8) + rgbImage.getGreen()[y][x];
                rgb = (rgb << 8) + rgbImage.getBlue()[y][x];
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    private RGBImage getRgbImage(BufferedReader reader, Matcher dimensionMatcher) throws IOException {
        int numCols = Integer.parseInt(dimensionMatcher.group(1));
        int numRows = Integer.parseInt(dimensionMatcher.group(2));
        short[][] r = new short[numRows][numCols];
        short[][] g = new short[numRows][numCols];
        short[][] b = new short[numRows][numCols];
        String line;
        int loc = 0;
        int row;
        int column;
        while ((line = reader.readLine()) != null) {
            String[] numbers = line.split("\\s+");
            for (int i = 0; i < numbers.length; i++) {
                if (!numbers[i].isEmpty()) {
                    int rawLoc = loc / 3;
                    row = rawLoc / numCols;
                    column = rawLoc % numCols;
                    int color = loc % 3;
                    switch (color) {
                        case 0:
                            r[row][column] = Short.parseShort(numbers[i]);
                            break;
                        case 1:
                            g[row][column] = Short.parseShort(numbers[i]);
                            break;
                        case 2:
                            b[row][column] = Short.parseShort(numbers[i]);
                            break;
                    }
                    loc += 1;
                }
            }
        }
        return new RGBImage(r, g, b);
    }

    private String getUncommentedLine(String line, BufferedReader bufferedReader) throws IOException {
        return line.startsWith("#") ? getUncommentedLine(bufferedReader.readLine(), bufferedReader) : line;
    }

    private BufferedImage getBufferedImageFromP6(String path, int width, int height, int max) throws IOException {
        RandomAccessFile af = new RandomAccessFile(path, "r");
        MappedByteBuffer buff = af.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, af.length());
        byte[] pixels = new byte[width * height * 3];
        try {
            int r, g, b;
            double wsp;
            int offset = 0;
            if (max > 255) {
                wsp = 255.0 / max;
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        r = ((buff.get() << 8) | buff.get());
                        g = ((buff.get() << 8) | buff.get());
                        b = ((buff.get() << 8) | buff.get());
                        r = (int) (r * wsp) & 0xff;
                        g = (int) (g * wsp) & 0xff;
                        b = (int) (b * wsp) & 0xff;
                        pixels[offset] = (byte) r;
                        pixels[offset + 1] = (byte) g;
                        pixels[offset + 2] = (byte) b;
                        offset += 3;
                    }
                }
            } else {
                if (max < 255) {
                    wsp = 255.0 / max;
                } else {
                    wsp = 1;
                }
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        r = buff.get();
                        g = buff.get();
                        b = buff.get();
                        if (wsp != 1) {
                            r = (int) (r * wsp) & 0xff;
                            g = (int) (g * wsp) & 0xff;
                            b = (int) (b * wsp) & 0xff;
                        } else {
                            r = r & 0xff;
                            g = g & 0xff;
                            b = b & 0xff;
                        }

                        pixels[offset] = (byte) r;
                        pixels[offset + 1] = (byte) g;
                        pixels[offset + 2] = (byte) b;
                        offset += 3;
                    }
                }
            }
            BufferedImage newImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            WritableRaster writableRaster = newImg.getRaster();
            writableRaster.setDataElements(0,0,width, height,pixels);
            return newImg;
        } catch (BufferUnderflowException e) {
            throw new IOException("Za mały bufor");
        }
    }
}
