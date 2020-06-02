package net.kurobako.textfx.sample;

import com.google.common.collect.Streams;

import net.kurobako.textfx.HighlightPane;
import net.kurobako.textfx.TextSelectPane;
import net.kurobako.textfx.sample.SamplerController.Sample;

import org.languagetool.JLanguageTool;
import org.languagetool.language.BritishEnglish;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.SuggestedReplacement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point3D;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.util.Pair;

public class SpellcheckerSample implements Sample {
	@Override public Node mkRoot() {


		var area = new TextArea("Foo bar baz\nFoo bar baz\nFoo bar baz");
		area.layout();
//		Platform.runLater(() -> ((Region) area.lookup(".content")).setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null))));
		area.setWrapText(true);


		HighlightPane pane = new HighlightPane(area);
		pane.setSelectionOverContent(true);

		var highlighter = pane.addHighlight(HighlightPane.SELECT_TEXT).applyStyle(c -> {
			c.setCursor(Cursor.TEXT);
			c.setStroke(null);
			c.setFill(Color.RED.deriveColor(1, 1, 1, 0.3));
		});


		var langTool = ThreadLocal.withInitial(() -> new JLanguageTool(new BritishEnglish()));

		var resultRef = new AtomicReference<List<Pair<Integer, RuleMatch>>>(List.of());
		var ec = Executors.newCachedThreadPool();


		area.scrollTopProperty().addListener(o -> highlighter.update());


		area.getParagraphs().addListener(new ListChangeListener<CharSequence>() {
			@Override public void onChanged(Change<? extends CharSequence> c) {

				ec.submit(() -> {
					var xs = new ArrayList<>(c.getList());
					resultRef.set(
							IntStream.range(0, xs.size())
									.mapToObj(i -> new Pair<>(i, xs.get(i)))
									.parallel()
									.flatMap(x -> {
										try {
											var offset = IntStream.range(0, x.getKey()).map(i -> xs.get(i).length() + 1).sum();
											return langTool.get().check(x.getValue().toString()).stream().map(r -> new Pair<>(offset, r));
										} catch (IOException ignored) { return Stream.empty(); }
									}).collect(Collectors.toList()));

					Platform.runLater(() -> {
						highlighter.highlightGroup().setVisible(true);	highlighter.update();
					});
				});


			}
		});
		area.textProperty().addListener((o, p, n) -> {

			highlighter.highlightGroup().setVisible(false);
			highlighter.update(text -> {


				var z = resultRef.get().stream().map(zz -> {

					var offset = zz.getKey();
					var r = zz.getValue();

					var path = new Path(text.rangeShape(offset + r.getFromPos(),
							offset + r.getToPos()));
					path.setPickOnBounds(false);
					path.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
						if (e.getButton() == MouseButton.SECONDARY) {
							e.consume();
							var menu = new ContextMenu(r.getSuggestedReplacementObjects()
									.stream()
									.sorted(Comparator.comparingDouble(x -> x.getConfidence() == null ? 0 : x.getConfidence()))
									.limit(10)
									.map(rep -> {
										var suffix = (rep.getShortDescription() == null) ?
												"" :
												" (" + rep.getShortDescription() + ")";
										var item = new MenuItem(rep.getReplacement() + suffix);
										item.setOnAction(e1 -> {
											area.replaceText(offset + r.getFromPos(),
													offset + r.getToPos(),
													rep.getReplacement());
//												  area.setText(head + rep.getReplacement() + last);
											;
										});
										return item;
									}).toArray(MenuItem[]::new));
							menu.getItems().add(0, new SeparatorMenuItem());
							var hint = new MenuItem(r.getMessage());
							hint.setDisable(true);
							menu.getItems().add(0, hint);
							menu.show(path, Side.RIGHT, 0, 0);
						}
					});

					return (Shape) path;
				}).collect(Collectors.toList());


				return z;


			});
		});

		return (pane);
	}
}
