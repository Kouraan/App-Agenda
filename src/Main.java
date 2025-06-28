import controller.Controller;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.fxml.FXMLLoader;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        /*Group root2 = new Group();
        Scene scene2 = new Scene(root2, Color.BLUE); // Cor do BackGround

        Image icon2 = new Image("../resources/simples.jpg"); //puxar uma imagem de um diretorio
        primaryStage.getIcons().add(icon2); // Adicionar o icon a janela
        primaryStage.setTitle("App Agenda"); // Mudar o titulo da janela
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds(); // Pegar o tamanho da tela
        primaryStage.setWidth(screenBounds.getWidth()); // Mudar a largura da janela
        primaryStage.setHeight(screenBounds.getHeight()); // Mudar a altura da janela
        primaryStage.setResizable(false); // Tornar a janela não redimensionável
        
        primaryStage.setScene(scene2);
        primaryStage.show();*/

        /*Group root = new Group();
        Scene scene = new Scene(root,600,600,Color.LIGHTSKYBLUE);
        Stage stage = new Stage();

        Text text = new Text();
        text.setText("OMG!");
        text.setX(50);
        text.setY(50);
        text.setFont(Font.font("Verdana", 50));
        text.setFill(Color.LIMEGREEN);

        Line line = new Line();
        line.setStartX(200);
        line.setStartY(200);
        line.setEndX(500);
        line.setEndY(200);
        line.setStrokeWidth(5);
        line.setStroke(Color.RED);
        line.setOpacity(0.5);
        line.setRotate(45);

        Rectangle rectangle = new Rectangle();
        rectangle.setX(100);
        rectangle.setY(100);
        rectangle.setWidth(100);
        rectangle.setHeight(100);
        rectangle.setFill(Color.BLUE);
        rectangle.setStrokeWidth(5);
        rectangle.setStroke(Color.BLACK);

        Polygon triangle = new Polygon();
        triangle.getPoints().setAll(
            200.0,200.0,
            300.0,300.0,
            200.0,300.0
            );
        triangle.setFill(Color.YELLOW);

        Circle circle = new Circle();
        circle.setCenterX(350);
        circle.setCenterY(350);
        circle.setRadius(50);
        circle.setFill(Color.ORANGE);

        Image image = new Image("file:resources/simples.jpg");
        ImageView imageView = new ImageView(image);
        imageView.setX(400);
        imageView.setY(400);

        imageView.setFitWidth(150);    // largura máxima em px
        imageView.setFitHeight(150);   // altura máxima em px
        imageView.setPreserveRatio(true); // manter a proporção da imagem

        root.getChildren().add(text);
        root.getChildren().add(line);
        root.getChildren().add(rectangle);
        root.getChildren().add(triangle);
        root.getChildren().add(circle);
        root.getChildren().add(imageView);
        stage.setScene(scene);
        stage.show();*/

        /*FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/test.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();*/

        Controller appController = new Controller(primaryStage);
        appController.startApp();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
