package net.kurobako.textfx.sample;

import net.kurobako.textfx.TextSelectPane;
import net.kurobako.textfx.sample.SamplerController.Sample;

import java.util.stream.IntStream;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.collections.FXCollections;
import javafx.geometry.Point3D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;

import static java.util.stream.Collectors.toCollection;

public class AnimatedSample implements Sample {
	@Override public Node mkRoot() {


		Label label = new Label("The quick brown fox jumps over the lazy dog");
		label.setFont(Font.font(30));

		label.minHeightProperty().bind(label.widthProperty());

		RotateTransition rt = new RotateTransition(Duration.millis(5000), label);
		rt.setByAngle(360);
		rt.setAxis(new Point3D(0.5, 1, 0.5));
		rt.setCycleCount(Animation.INDEFINITE);
		rt.setInterpolator(Interpolator.LINEAR);
		rt.play();

		TextSelectPane pane = new TextSelectPane(new StackPane(label));
		label.rotateProperty().addListener(o -> pane.invalidate());
		return  (pane);
	}
}
