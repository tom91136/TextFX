package net.kurobako.textfx.sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main {

	public static void main(String[] args) {
		Application.launch(JFXApp.class, args);
	}


	public static final class JFXApp extends Application {

		@Override
		public void start(Stage primaryStage) throws Exception {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/Sampler.fxml"));
			Parent parent = loader.load();
			loader.<SamplerController>getController().hostServices = getHostServices(); // seems ugly
			primaryStage.setTitle("TextFX samples");
			primaryStage.setScene(new Scene(parent));
			primaryStage.show();
		}
	}


}
