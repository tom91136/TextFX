package net.kurobako.textfx.sample;

import net.kurobako.textfx.HighlightPane;
import net.kurobako.textfx.sample.SamplerController.Sample;

import java.util.Collections;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class HighlightedListSample implements Sample {


	@Override
	public Node mkRoot() {


		var xs = SampleText.load("lorem_ipsum.txt").stream()
				.limit(100)
				.filter(s -> !s.isBlank())
				.map(s -> s.substring(0, Math.min(s.length(), 150)))
				.collect(Collectors.toList());


		var view = new ListView<>(FXCollections.observableList(xs));

		var shuffle = new Button("Shuffle");
		shuffle.setOnAction(e -> Collections.shuffle(view.getItems()));
		var filter = new TextField("Lorem");
		HBox.setHgrow(filter, Priority.ALWAYS);
		VBox.setVgrow(view, Priority.ALWAYS);

		view.setCellFactory(lv -> new ListCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				setText(null);
				if (empty || item == null) {
					setGraphic(null);
				} else {
					HighlightPane pane = new HighlightPane(new Label(item));

					var highlight =
							pane.addHighlight(HighlightPane.SELECT_TEXT)
									.update(HighlightPane.textFuzzyMatch(filter.getText(), true))
									.applyStyle(c -> c.setFill(Color.GREENYELLOW.desaturate()));

					filter.textProperty().addListener(x -> {
						highlight.update(HighlightPane.textFuzzyMatch(filter.getText(), true));
					});

					setGraphic(pane);
				}

			}
		});

		HBox filterBar = new HBox(new Label("Filter:"), filter, shuffle);
		filterBar.setAlignment(Pos.CENTER);
		filterBar.setSpacing(4);
		filterBar.setPadding(new Insets(4));
		VBox root = new VBox(filterBar, view);
		root.setAlignment(Pos.CENTER);
		return root;
	}
}
