package net.kurobako.textfx;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

@SuppressWarnings("unused")
public class HighlightPane extends StackPane {

	public interface Highlighter<T> {
		ObservableList<T> matching();
		ObservableList<Shape> highlightShapes();
		Group highlightGroup();
		default Highlighter<T> applyStyle(Consumer<Shape> c) {
			highlightShapes().forEach(c);
			return this;
		}
		Highlighter<T> update(Function<T, List<Shape>> highlighter);
		Highlighter<T> update();
		void discard();
	}

	final BooleanProperty selectionOverContent = new SimpleBooleanProperty(false);
	final ObjectProperty<Parent> content = new SimpleObjectProperty<>();
	final ObservableSet<Highlighter<?>> highlighters =
			FXCollections.observableSet(new LinkedHashSet<>());
	private final Pane selectionHighlight = new Pane();


	public static Function<Text, List<Shape>> textMatch(String text, boolean caseInsensitive) {
		return t -> {
			if (text.isEmpty()) return List.of();
			var haystack = caseInsensitive ? t.getText().toLowerCase() : t.getText();
			var needle = caseInsensitive ? text.toLowerCase() : text;
			var paths = IntStream.iterate(
					haystack.indexOf(needle),
					i -> i >= 0,
					i -> haystack.indexOf(needle, i + needle.length()))
					.boxed()
					.flatMap(i -> Stream.of(t.rangeShape(i, i + needle.length())))
					.toArray(PathElement[]::new);
			return paths.length == 0 ? List.of() : List.of(new Path(paths));
		};
	}

	public static Function<Node, Optional<Text>> SELECT_TEXT =
			x -> x instanceof Text ? Optional.of((Text) x) : Optional.empty();


	{
		setAlignment(Pos.TOP_LEFT);
		Runnable bind = () -> {
			var node = content.get();
			if (node == null) return;
			getChildren().setAll(selectionHighlight, node);
		};
		selectionHighlight.setPickOnBounds(false);
		selectionHighlight.setFocusTraversable(false);
		content.addListener(o -> bind.run());

		highlighters.addListener((SetChangeListener<Highlighter<?>>) change -> {
			var xs = selectionHighlight.getChildren();
			if (change.wasRemoved()) xs.remove(change.getElementRemoved().highlightGroup());
			if (change.wasAdded()) xs.add(change.getElementAdded().highlightGroup());
		});
		selectionOverContent.addListener((o, p, n) -> {
			if (n) selectionHighlight.toFront();
			else selectionHighlight.toBack();
		});
		selectionOverContent.set(false);
	}

	public HighlightPane(Parent parent) { content.set(parent); }
	public HighlightPane() { }


	public HighlightPane clearAll() { highlighters.forEach(Highlighter::discard); return this;}

	public HighlightPane updateAll() { highlighters.forEach(Highlighter::update); return this; }

	public boolean isSelectionOverContent() { return selectionOverContent.get(); }
	public BooleanProperty selectionOverContentProperty() { return selectionOverContent; }
	public void setSelectionOverContent(boolean over) { this.selectionOverContent.set(over); }

	public <T extends Node> Highlighter<T> addHighlight(Function<Node, Optional<T>> filter) {
		var group = new Group();
		var highlighted = new Highlighter<T>() {
			private Function<T, List<Shape>> highlighter = t -> List.of();
			private Consumer<Shape> style = c -> {};
			private final ObservableList<T> matching = FXCollections.observableArrayList();
			private final ObservableList<Shape> highlightShapes =
					FXCollections.observableArrayList();
			@Override public ObservableList<T> matching() { return matching; }
			@Override public ObservableList<Shape> highlightShapes() { return highlightShapes; }
			@Override public Group highlightGroup() { return group; }
			@Override public Highlighter<T> applyStyle(Consumer<Shape> c) {
				this.style = c;
				return this;
			}
			@Override public Highlighter<T> update(Function<T, List<Shape>> highlighter) {
				this.highlighter = highlighter;
				var parent = content.get();
				if (parent == null) return this;
				var matching = Nodes.collectNodes(parent, filter);
				var shapes = matching.stream().flatMap(m ->
						highlighter.apply(m).stream().peek(path -> {
							path.getTransforms().add(m.getLocalToSceneTransform());
							var sceneOrigin = parent.sceneToLocal(Point2D.ZERO);
							path.setLayoutX(sceneOrigin.getX());
							path.setLayoutY(sceneOrigin.getY());
						})).peek(style).toArray(Shape[]::new);
				this.matching.setAll(matching);
				this.highlightShapes.setAll(shapes);
				group.getChildren().setAll(shapes);
				return this;
			}
			@Override public Highlighter<T> update() { return update(highlighter); }
			@Override public void discard() { highlighters.remove(this); }
		};
		highlighters.add(highlighted);
		return highlighted;
	}

	@Override protected void layoutChildren() {
		super.layoutChildren();
		updateAll();
	}
}
