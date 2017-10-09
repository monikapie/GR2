package sample;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Controller {
    @FXML GridPane gridPane;
    @FXML Button openPPMbtn;
    @FXML ImageView PPMimageView;

    @FXML
    private void openPPM(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose resource");
        Stage stage = (Stage) gridPane.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if(file != null){
            openFile(file);
        }
    }

    private void openFile(File file) {
        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            convertToPPM(bufferedImage);
            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
            PixelReader pixelReader = image.getPixelReader();
            PPMimageView.setImage(image);
        } catch (IOException ex) {
            Logger.getLogger(
                    Controller.class.getName()).log(
                    Level.SEVERE, null, ex
            );
        }
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
