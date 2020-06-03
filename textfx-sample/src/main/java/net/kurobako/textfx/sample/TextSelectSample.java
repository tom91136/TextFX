package net.kurobako.textfx.sample;

import net.kurobako.textfx.NoCacheScrollPane;
import net.kurobako.textfx.TextSelectPane;
import net.kurobako.textfx.TextSelectPane.SelectionMode;
import net.kurobako.textfx.sample.SamplerController.Sample;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedAreaChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;

import static java.util.stream.Collectors.toCollection;

public class TextSelectSample implements Sample {
	@Override public Node mkRoot() {

		var division = 30;
		var range = 2.0 * Math.PI;
		var chart = new StackedAreaChart<>(new NumberAxis(1, division, 1), new NumberAxis());
		chart.setTitle("Sine");
		chart.setLegendVisible(false);
		// remove chart background so that selection comes through
		((Region) chart.lookup(".chart-plot-background"))
				.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
		chart.getData().add(
				new Series<>("Sine", IntStream.range(0, division)
						.mapToObj(i -> {
							double degree =
									Math.toDegrees(Math.sin((double) i * range / division));
							var data = new Data<Number, Number>(i, degree);
							Label label = new Label();
							label.setScaleX(0.8);
							label.setScaleY(0.8);
							label.textProperty().bind(data.YValueProperty().asString("%,.1f°"));
							label.translateYProperty().bind(label.heightProperty().multiply(-1.2));
							label.setRotate(-degree + 30);
							var pane = new Pane(label);
							pane.setShape(new Circle(6.0));
							pane.setScaleShape(false);
							data.setNode(pane);
							return data;
						})
						.collect(toCollection(FXCollections::observableArrayList))));


		var loremIpsum = new FlowPane(
				SampleText.load("lorem_ipsum.txt").stream().flatMap(line -> Stream.of(line.split(
						" "))
						.map(text -> {
							var label1 = new Text(text);
							label1.setFontSmoothingType(FontSmoothingType.LCD);
							return label1;
						})
						.map(Stream::of)
						.reduce((x, y) -> Stream.concat(x,
								Stream.concat(Stream.of(new Text(" ")), y)))
						.orElse(Stream.empty())).toArray(Node[]::new));

		VBox content = new VBox(
				new Label("Everything in this tab is selectable including this label," +
						" go ahead select using the cursor"),
				new Separator(),
				new HBox(new RadioButton("A"), new RadioButton("B"), new RadioButton("C")),
				chart,
				new FlowPane(IntStream.rangeClosed(0, 36).map(i -> i * 10).mapToObj(i -> {
					Label label = new Label("Rotated " + i + "°");
					label.prefHeightProperty().bind(label.widthProperty().multiply(2.0 / 3));
					label.setScaleX(0.8);
					label.setScaleY(0.8);
					label.setRotate(i);
					return label;
				}).toArray(Node[]::new)),
				loremIpsum
		);
		content.setPadding(new Insets(8));


		var pane = new TextSelectPane(content);


		var mode = new ChoiceBox<>(FXCollections.observableArrayList(SelectionMode.values()));
		mode.getSelectionModel().select(pane.getSelectionMode());
		pane.selectionModeProperty().bind(mode.valueProperty());

		var picker = new ColorPicker();
		var fill = pane.getHighlightFill();
		if (fill instanceof Color) picker.setValue((Color) fill);
		pane.highlightFillProperty().bind(picker.valueProperty());

		TextArea selected = new TextArea();
		selected.setWrapText(true);

		pane.selectedProperty().addListener((o, p, n) -> {
			selected.setText(String.join("\n", TextSelectPane.selectionToLines(" ").call(n)));
		});


		GridPane options = new GridPane();
		options.setHgap(4);
		options.setVgap(4);
		options.setPadding(new Insets(4));
		options.setPrefWidth(300);
		options.getRowConstraints().addAll(
				new RowConstraints(),
				new RowConstraints(),
				new RowConstraints(),
				new RowConstraints(0, Region.USE_PREF_SIZE,
						Region.USE_COMPUTED_SIZE, Priority.ALWAYS, VPos.TOP, true));

		options.addColumn(0,
				new Label("SelectMode"),
				new Label("SelectionFill"),
				new Label("Selection"));
		options.addColumn(1, mode, picker);
		options.addRow(3, selected);
		GridPane.setColumnSpan(selected, GridPane.REMAINING);

		var scrollPane = new NoCacheScrollPane(pane);
		scrollPane.setFitToWidth(true);
		HBox.setHgrow(scrollPane, Priority.ALWAYS);
		return new HBox(options, scrollPane);
	}
}
