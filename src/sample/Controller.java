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
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {
    @FXML GridPane gridPane;
    @FXML Button openPPMbtn;
    @FXML Button openJPGbtn;
    @FXML Button savePPMbtn;
    @FXML Button saveJPGbtn;
    @FXML ImageView PPMimageView;

    @FXML
    private void openJPG() throws IOException{
        File file = getFile();
        if(file != null){
            openFile(file);
        }
    }

    @FXML
    private void saveJPG(){
        openDialog();
    }

    @FXML
    private void readPPM() throws IOException{
        File file = getFile();
        if(file.getName().endsWith(".ppm")){
            setPPMImage(file);
        }
    }

    private void setPPMImage(File file) throws IOException {
        RGBImage rgbImage = readChoosenPPM(file.getPath());
        BufferedImage image = new BufferedImage(rgbImage.getNumrows(), rgbImage.getNumcolumns(), BufferedImage.TYPE_INT_RGB);

        for(int y = 0; y < rgbImage.getNumcolumns(); y++){
            for(int x = 0; x < rgbImage.getNumrows(); x++){
                int rgb = rgbImage.getRed()[y][x];
                rgb = (rgb << 8) + rgbImage.getGreen()[y][x];
                rgb = (rgb << 8) + rgbImage.getBlue()[y][x];
                image.setRGB(x,y,rgb);
            }
        }
        Image imageToSet = SwingFXUtils.toFXImage(image, null);
        PPMimageView.setImage(imageToSet);
    }

    private File getFile(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose resource");
        Stage stage = (Stage) gridPane.getScene().getWindow();
        return fileChooser.showOpenDialog(stage);
    }

    private void saveChoosenJPG(String compressionLvl){
        JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
        jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        float compression = Float.valueOf(compressionLvl);
        jpegParams.setCompressionQuality(compression);
        File file = getFile();
        final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try {
            if(file != null){
                writer.setOutput(new FileImageOutputStream(
                        new File(file.getAbsolutePath().split("\\.")[0] +  "AfterCmp" + ".jpg")));
                BufferedImage bufferedImage = ImageIO.read(file);
                writer.write(null, new IIOImage(bufferedImage, null, null), jpegParams);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openDialog(){
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(gridPane.getScene().getWindow());
        VBox dialogVbox = new VBox(20);
        final TextField textField = new TextField();
        Button button = new Button();
        button.setText("Ok");
        dialogVbox.getChildren().add(new Text("Enter compression level ( ex. 0.7f)"));
        dialogVbox.getChildren().add(textField);
        dialogVbox.getChildren().add(button);
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                saveChoosenJPG(textField.getText());
            }
        });
        Scene dialogScene = new Scene(dialogVbox, 300, 200);
        dialog.setScene(dialogScene);
        dialog.show();
    }
    private void openFile(File file) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(file);
        convertToPPM(bufferedImage);
        Image image = SwingFXUtils.toFXImage(bufferedImage, null);
        PPMimageView.setImage(image);
    }

    private RGBImage readChoosenPPM(String filename)throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
        String p3 = reader.readLine();
        String dimensions = checkIfNotComment(reader.readLine(), reader);
        Pattern demensionPattern = Pattern.compile("(\\d+) (\\d+)");
        Matcher dimensionMatcher = demensionPattern.matcher(dimensions);
        String range = checkIfNotComment(reader.readLine(),reader);
        if(dimensionMatcher.matches()){
            int numCols = Integer.parseInt(dimensionMatcher.group(1));
            int numRows = Integer.parseInt(dimensionMatcher.group(2));
            short[][] r = new short[numRows][numCols];
            short[][] g = new short[numRows][numCols];
            short[][] b = new short[numRows][numCols];
            String line;
            int loc = 0;
            int row;
            int column;
            while((line = reader.readLine())!=null){
                String[] numbers = line.split("\\s+");
                for(int i = 0; i < numbers.length; i++){
                    if(!numbers[i].isEmpty()){
                        int rawLoc = loc / 3;
                        row = rawLoc / numCols;
                        column = rawLoc % numCols;
                        int color = loc % 3;
                        switch (color){
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
            return new RGBImage(r,g,b);
        } else{
            throw new IOException("Could not read this file. Check if it is ppm file.");
        }
    }

    private String checkIfNotComment(String line, BufferedReader bufferedReader) throws IOException{
        return line.startsWith("#") ? checkIfNotComment(bufferedReader.readLine(),bufferedReader) : line;
    }

    private void convertToPPM(BufferedImage bufferedImage){
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                int color = bufferedImage.getRGB(i,j);
                int alpha = (color >> 24) & 0xff;
                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;
            }
        }
    }
}
