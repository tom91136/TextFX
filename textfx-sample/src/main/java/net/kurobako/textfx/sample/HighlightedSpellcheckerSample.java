package net.kurobako.textfx.sample;

import net.kurobako.textfx.HighlightPane;
import net.kurobako.textfx.sample.SamplerController.Sample;

import org.languagetool.JLanguageTool;
import org.languagetool.language.BritishEnglish;
import org.languagetool.rules.RuleMatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.util.Pair;

import static java.util.stream.Collectors.toList;

public class HighlightedSpellcheckerSample implements Sample {
	@Override
	public Node mkRoot() {

		var area = new TextArea(String.join("\n", SampleText.load("javafx.txt")));
		area.layout();
		area.setWrapText(true);

		HighlightPane pane = new HighlightPane(area);
		pane.setSelectionOverContent(true);
		var resultRef = new AtomicReference<List<Pair<Integer, RuleMatch>>>(List.of());


		var highlighter = pane.addHighlight(HighlightPane.SELECT_TEXT).applyStyle(c -> {
			c.setCursor(Cursor.TEXT);
			c.setStroke(null);
			c.setFill(Color.RED.deriveColor(1, 1, 1, 0.3));
		}).update(text -> resultRef.get().stream().map(matchPair -> {
			var offset = matchPair.getKey();
			var match = matchPair.getValue();

			var path = new Path(text.rangeShape(
					offset + match.getFromPos(), offset + match.getToPos()));
			path.setPickOnBounds(false);
			path.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
				if (e.getButton() == MouseButton.SECONDARY) {
					e.consume();
					var menu = new ContextMenu(match.getSuggestedReplacementObjects()
							.stream()
							.sorted(Comparator.comparingDouble(x -> x.getConfidence() == null
									? 0 : x.getConfidence()))
							.limit(10)
							.map(rep -> {
								var suffix = (rep.getShortDescription() == null) ?
										"" :
										" (" + rep.getShortDescription() + ")";
								var item = new MenuItem(rep.getReplacement() + suffix);
								item.setOnAction(e1 -> {
									area.replaceText(
											offset + match.getFromPos(),
											offset + match.getToPos(),
											rep.getReplacement());
								});
								return item;
							}).toArray(MenuItem[]::new));
					menu.getItems().add(0, new SeparatorMenuItem());
					var hint = new MenuItem(match.getMessage());
					hint.setDisable(true);
					menu.getItems().add(0, hint);
					menu.show(path, Side.RIGHT, 0, 0);
				}
			});

			return path;
		}).collect(toList()));

		area.scrollTopProperty().addListener(o -> highlighter.update());
		area.textProperty().addListener(o -> highlighter.highlightGroup().setVisible(false));


		var ec = Executors.newCachedThreadPool();

		ec.submit(() -> {
			var langTool = ThreadLocal.withInitial(() -> new JLanguageTool(new BritishEnglish()));

			Consumer<List<CharSequence>> executeSpellcheck = (xs) -> {
				ec.submit(() -> {
					resultRef.set(IntStream.range(0, xs.size()).boxed().parallel().flatMap(i -> {
						try {
							var offset = IntStream.range(0, i).map(j -> xs.get(j).length() + 1).sum();
							return langTool.get().check(xs.get(i).toString()).stream().map(r -> new Pair<>(offset, r));
						} catch (IOException ignored) {
							return Stream.empty();
						}
					}).collect(toList()));

					Platform.runLater(() -> {
						highlighter.highlightGroup().setVisible(true);
						highlighter.update();
					});
				});
			};

			area.getParagraphs().addListener(new ListChangeListener<CharSequence>() {
				@Override
				public void onChanged(Change<? extends CharSequence> c) {
					// make a copy here in case paragraph changes half way
					executeSpellcheck.accept(new ArrayList<>(c.getList()));
				}
			});

			executeSpellcheck.accept(area.getParagraphs());

		});


		return pane;
	}
}
