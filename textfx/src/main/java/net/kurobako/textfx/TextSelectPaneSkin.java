package net.kurobako.textfx;

import net.kurobako.textfx.TextSelectPane.Selection;
import net.kurobako.textfx.TextSelectPane.Token;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javafx.geometry.BoundingBox;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SkinBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ScrollEvent.HorizontalTextScrollUnits;
import javafx.scene.input.ScrollEvent.VerticalTextScrollUnits;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.util.Pair;

import static java.util.stream.Collectors.toList;
import static javafx.scene.input.KeyCombination.CONTROL_ANY;

public class TextSelectPaneSkin extends SkinBase<TextSelectPane> {

	private final TextSelectPane pane;
	private final Pane selectionHighlight = new Pane();
	private final ContextMenu menu = new ContextMenu();


	TextSelectPaneSkin(TextSelectPane control) {
		super(control);
		this.pane = control;

		var parentClip = new Rectangle();
		selectionHighlight.setClip(parentClip);
		pane.setFocusTraversable(true);
		pane.setCursor(Cursor.TEXT);

		Runnable bindContent = () -> {
			var node = pane.content.get();
			if (node == null) return;
			node.layoutBoundsProperty().addListener((o1, p1, bound) -> {
				if (bound == null) return;
				parentClip.setWidth(bound.getWidth());
				parentClip.setHeight(bound.getHeight());
			});
			getChildren().setAll(selectionHighlight, node);
		};
		pane.content.addListener(o -> bindContent.run());
		bindContent.run();

		pane.selected.addListener(o -> invalidateSelections());

		Map<KeyCodeCombination, Runnable> keyboardEvents = Map.of(
				new KeyCodeCombination(KeyCode.COPY), pane::copySelectedToClipboard,
				new KeyCodeCombination(KeyCode.C, CONTROL_ANY), pane::copySelectedToClipboard,
				new KeyCodeCombination(KeyCode.INSERT, CONTROL_ANY), pane::copySelectedToClipboard,
				new KeyCodeCombination(KeyCode.A, CONTROL_ANY), pane::selectAll);

		pane.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
			for (Entry<KeyCodeCombination, Runnable> e : keyboardEvents.entrySet()) {
				if (e.getKey().match(ke)) e.getValue().run();
			}
		});

		pane.addEventFilter(MouseEvent.MOUSE_PRESSED, initial -> {
			initial.consume();
			pane.requestFocus();
			menu.hide();

			var source = pane.content.get();
			if (source == null) return;

			var origin = new Point2D(initial.getX(), initial.getY());
			var originInScene = new Point2D(initial.getSceneX(), initial.getSceneY());


			var textNodes = TextSelectPane.findTextNodes(source);

			var scrollTarget = Nodes.findParent(pane, x -> {
				if (x.getAccessibleRole() == AccessibleRole.SCROLL_PANE)
					return Optional.of(new Pair<>(x, x.localToScene(x.getLayoutBounds())));
				return Optional.empty();
			});

			if (initial.getClickCount() == 2) {
				pane.updateSelection(textNodes,
						TextSelectPane.resolveTextBounds(textNodes).stream()
								.filter(p -> p.bounds().contains(originInScene))
								.map(Selection::single));
			} else if (initial.getClickCount() == 3) {
				pane.updateSelection(textNodes,
						TextSelectPane.resolveTextBounds(textNodes).stream()
								.filter(p -> originInScene.getY() >= p.bounds().getMinY() &&
										originInScene.getY() <= p.bounds().getMaxY())
								.map(Selection::single));
			} else {
				pane.setOnMouseDragged(rest -> {
					rest.consume();
					var current = new Point2D(rest.getX(), rest.getY());
					var resolved = TextSelectPane.resolveTextBounds(textNodes);
					pane.updateSelection(textNodes,
							createSelectionFromPoint(resolved, origin, current));

					scrollTarget.ifPresent(scrollable -> {
						var xInScene = rest.getSceneX();
						var yInScene = rest.getSceneY();
						var boundInScene = scrollable.getValue();
						if (boundInScene.contains(xInScene, yInScene)) return;
						var scale = 10.0;
						var dx = boundInScene.getCenterX() - xInScene;
						var dy = boundInScene.getCenterY() - yInScene;
						pane.fireEvent(new ScrollEvent(this, initial.getTarget(),
								ScrollEvent.SCROLL, 0, 0, 0, 0,
								false, false, false, false, false, false,
								dx / scale, dy / scale, 0, 0,
								HorizontalTextScrollUnits.NONE, 0,
								VerticalTextScrollUnits.NONE, 0,
								0, rest.getPickResult()
						));
					});


				});
				if (initial.getEventType() == MouseEvent.MOUSE_PRESSED) {
					switch (initial.getButton()) {
						case PRIMARY:
							pane.updateSelection(textNodes, Stream.empty());
							break;
						case SECONDARY:
							menu.show(pane, originInScene.getX(), originInScene.getY());
							break;
						default:
							break;
					}
				}
			}
		});

	}

	private void invalidateSelections() {
		selectionHighlight.getChildren().setAll(
				pane.selected.get().stream()
						.map(s -> s.select(pane, pane.highlightFill))
						.toArray(Node[]::new));
	}

	private Stream<Selection> createSelectionFromPoint(List<Token> texts,
	                                                   Point2D origin, Point2D current) {

		var dim = current.subtract(origin);
		var sorted = texts.stream().sorted(
				Comparator.<Token>comparingDouble(z -> z.bounds().getMinY())
						.thenComparingDouble((z -> z.bounds().getMinX())))
				.collect(toList());
		var sceneSelectionBound = pane.localToScene(
				new BoundingBox(origin.getX(), origin.getY(), dim.getX(), dim.getY()));

		switch (pane.selectionMode.get()) {
			case COLUMN:
				return sorted.stream().flatMap(p -> {
					var bound = p.bounds();
					if (sceneSelectionBound.contains(bound)) return Stream.of(Selection.single(p));
					else if (sceneSelectionBound.intersects(bound))
						return Selection.single(p, sceneSelectionBound).stream();
					else return Stream.empty();
				});
			case ROW:
				var startPoint = pane.localToScene(origin);
				var endPoint0 = pane.localToScene(current);
				var min = sorted.stream().findFirst().map(p ->
						new Point2D(p.bounds().getMinX(), p.bounds().getMinY())).orElse(Point2D.ZERO);
				var endPoint = new Point2D(
						Math.max(endPoint0.getX(), min.getX()),
						Math.max(endPoint0.getY(), min.getY()));

				var starts = Nodes.indexOf(sorted, false, p -> {
					var b = p.bounds();
					return b.getMaxX() >= startPoint.getX() && b.getMaxY() >= startPoint.getY();
				});

				var ends = Nodes.indexOf(sorted, true, p -> {
					var b = p.bounds();
					return b.getMinX() <= endPoint.getX() && b.getMinY() <= endPoint.getY();
				});

				return starts.stream().flatMap(s -> ends.stream().flatMap(e -> {
					int startIdx = s.getValue();
					int endIdx = e.getValue();
					var start = s.getKey();
					var end = e.getKey();
					if (startIdx == endIdx)
						return Selection.single(start, sceneSelectionBound).stream();
					else {
						BiFunction<Text, Point2D, HitInfo> hitX =
								(n, p) -> {
									Point2D point2D = n.sceneToLocal(pane.localToScene(p));
									return n.hitTest(new Point2D(point2D.getX(), 0));
								};
						var startHit = hitX.apply(start.text(), origin);
						var endHit = hitX.apply(end.text(), current);

//						if(startHit.getInsertionIndex() > endHit.getInsertionIndex()) return Stream.empty();

						if (startIdx < endIdx) {
							return Stream.concat(
									sorted.subList(startIdx + 1, endIdx).stream().map(Selection::single),
									Stream.concat(
											Selection.single(start, startHit.getCharIndex(),
													start.text().getText().length()).stream(),
											Selection.single(end, 0, endHit.getCharIndex()).stream()));

						} else {
							return Stream.concat(
									sorted.subList(endIdx + 1, startIdx).stream().map(Selection::single),
									Stream.concat(
											Selection.single(end, endHit.getCharIndex(),
													end.text().getText().length()).stream(),
											Selection.single(start, 0,
													startHit.getCharIndex()).stream()));

						}
					}

				}));

			default:
				throw new AssertionError();
		}

	}


	@Override protected void layoutChildren(double contentX, double contentY,
	                                        double contentWidth, double contentHeight) {


		layoutInArea(selectionHighlight, contentX, contentY, contentWidth, contentHeight, -1,
				HPos.LEFT, VPos.TOP);

		Node content = pane.content.get();
		if (content != null) {
			layoutInArea(content,
					contentX, contentY,
					contentWidth, contentHeight,
					-1,
					HPos.LEFT, VPos.TOP);
		}

		invalidateSelections();

	}
}
