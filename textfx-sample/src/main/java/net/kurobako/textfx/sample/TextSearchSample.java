package net.kurobako.textfx.sample;

import net.kurobako.textfx.HighlightPane;
import net.kurobako.textfx.NoCacheScrollPane;
import net.kurobako.textfx.sample.SamplerController.Sample;

import java.util.Random;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class TextSearchSample implements Sample {


	@Override
	public Node mkRoot() {


		String join = String.join("\n", SampleText.load("lorem_ipsum.txt"));


		var haystack = new Label(join);
		haystack.setWrapText(true);
		haystack.setTextOverrun(OverrunStyle.CLIP);
		haystack.setMaxHeight(Double.MAX_VALUE);


		var pane = new HighlightPane(haystack);

		var scrollPane = new NoCacheScrollPane((pane));
		scrollPane.setFitToWidth(true);
		scrollPane.setFitToHeight(false);
		VBox.setVgrow(scrollPane, Priority.ALWAYS);


		Button addNeedle = new Button("Add search");
		VBox needles = new VBox();
		needles.setSpacing(4);
		var rand = new Random(42);


		Consumer<String> addRow = string -> {

			var needle = new TextField();

			HBox.setHgrow(needle, Priority.ALWAYS);
			var remove = new Button("Remove");


			var picker = new ColorPicker(Color.color(rand.nextDouble(), rand.nextDouble(),
					rand.nextDouble(), 0.8));

			var fuzzy = new CheckBox("fuzzy");

			HBox row = new HBox(remove, needle, fuzzy, picker);
			row.setSpacing(4);
			var highlighter = pane.addHighlight(HighlightPane.SELECT_TEXT).applyStyle(s -> {
				s.fillProperty().bind(picker.valueProperty());
				s.setStroke(null);
			});
			remove.setOnAction(e1 -> {
				needles.getChildren().remove(row);
				highlighter.discard();
			});

			Runnable bindMatch = () -> highlighter.update(fuzzy.isSelected() ?
					HighlightPane.textFuzzyMatch(needle.getText(), true) :
					HighlightPane.textMatch(needle.getText(), true));

			fuzzy.selectedProperty().addListener(o -> bindMatch.run());
			needle.textProperty().addListener(o -> bindMatch.run());
			needle.setText(string);
			needles.getChildren().add(row);
		};

		addNeedle.setOnAction(e -> addRow.accept(""));
		addRow.accept("Lorem");
		addRow.accept("IPSUM");
		addRow.accept("Sit");
		addRow.accept(",");

		var searchView = new VBox(
				addNeedle,
				needles,
				scrollPane);
		searchView.setSpacing(8);
		searchView.setPadding(new Insets(16));
		searchView.setMaxWidth(Double.MAX_VALUE);


		return searchView;
	}
}
