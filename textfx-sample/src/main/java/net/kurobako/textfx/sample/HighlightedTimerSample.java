package net.kurobako.textfx.sample;

import net.kurobako.textfx.HighlightPane;
import net.kurobako.textfx.sample.SamplerController.Sample;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class HighlightedTimerSample implements Sample {


	@Override
	public Node mkRoot() {


		Label label = new Label();
		label.setFont(Font.font(30));

		label.minHeightProperty().bind(label.widthProperty());


		HighlightPane pane = new HighlightPane(new StackPane(label));

		for (int i = 0; i < 10; i++) {
			var range = ((double) i / 10);
			pane.addHighlight(HighlightPane.SELECT_TEXT)
					.update(HighlightPane.textFuzzyMatch(String.valueOf(i), true))
					.applyStyle(s -> {
						s.setStroke(null);
						s.setFill(Color.color(range, 0.6, 0.6));
					});
		}


		var timeline = new Timeline(new KeyFrame(Duration.millis(33), e -> {
			label.setText("System.currentTimeMillis = " + System.currentTimeMillis());
		}
		));
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.play();

		return pane;
	}
}
