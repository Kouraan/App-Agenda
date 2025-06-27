
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        Group root = new Group();
        Scene scene = new Scene(root, Color.BLUE); // Cor do BackGround

        Image icon = new Image("../resources/icon.png"); //puxar uma imagem de um diretorio
        primaryStage.getIcons().add(icon); // Adicionar o icon a janela
        primaryStage.setTitle("App Agenda"); // Mudar o titulo da janela
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds(); // Pegar o tamanho da tela
        primaryStage.setWidth(screenBounds.getWidth()); // Mudar a largura da janela
        primaryStage.setHeight(screenBounds.getHeight()); // Mudar a altura da janela
        primaryStage.setResizable(false); // Tornar a janela não redimensionável
        
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
