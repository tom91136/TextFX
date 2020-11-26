package net.kurobako.textfx.sample;

import net.kurobako.textfx.TextSelectPane;
import net.kurobako.textfx.sample.SamplerController.Sample;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class AnimatedTextSelectSample implements Sample {
	@Override
	public Node mkRoot() {


		Label label = new Label("Try to select me!");
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
		return (pane);
	}
}
