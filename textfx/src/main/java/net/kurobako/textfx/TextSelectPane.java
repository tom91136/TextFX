package net.kurobako.textfx;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.Pair;

import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("unused")
public class TextSelectPane extends Control {

	static List<Text> findTextNodes(Parent parent) {
		return Nodes.collectNodes(parent, x -> x instanceof Text ?
				Optional.of((Text) x) : Optional.empty());
	}

	static List<Token> resolveTextBounds(List<Text> xs) {
		return xs.stream()
				.map(x -> new Token(x, x.localToScene(x.getLayoutBounds())))
				.collect(toList());
	}

	public enum SelectionMode {
		COLUMN, ROW
	}

	public static class Selection {

		private final Token token;
		private final int selectStart, selectEnd;

		static Selection single(Token token) {
			var text = token.text;
			return new Selection(token, 0, text.getText().length());
		}

		static Optional<Selection> single(Token token, int start, int end) {
			return start != -1 && end != -1 ?
					Optional.of(new Selection(token, start, end)) :
					Optional.empty();
		}

		static Optional<Selection> single(Token token, Bounds bounds) {
			var localSelection = token.text.sceneToLocal(bounds);
			return single(token,
					token.text.hitTest(
							new Point2D(localSelection.getMinX(), localSelection.getMinY() * 0)
					                  ).getInsertionIndex(),
					token.text.hitTest(
							new Point2D(localSelection.getMaxX(), localSelection.getMaxY() * 0)
					                  ).getInsertionIndex());
		}

		public Token token() { return token; }
		public int selectStart() { return selectStart; }
		public int selectEnd() { return selectEnd; }

		private Selection(Token token, int selectStart, int selectEnd) {
			this.token = Objects.requireNonNull(token);

			var max = token.text.getText().length();

//			this.selectStart = Math.max(0, selectStart);
//			this.selectEnd =   Math.min(selectEnd, max);
			if (selectEnd < selectStart) {
				this.selectStart = 0;
				this.selectEnd = 0;
			} else {

				this.selectStart = Math.max(0, selectStart);
				this.selectEnd = Math.min(selectEnd, max);
			}


		}

		Path select(Parent parent, ReadOnlyObjectProperty<Paint> highlightFill) {
			token.text.setSelectionStart(selectStart);
			token.text.setSelectionEnd(selectEnd);
			var path = new Path(token.text.getSelectionShape());


			path.getTransforms().setAll(Nodes.collectParentUntil(token.text, true,
					x -> x == parent ?
							Optional.empty() :
							Optional.of(x.getLocalToParentTransform())));


			path.fillProperty().bind(highlightFill);
			path.setStroke(null);
			return path;
		}

		public String selectedText() {
			return token.text.getText().substring(selectStart, selectEnd);
		}
	}

	public static class Token {
		private final Text text;
		private final Bounds bounds;
		public Text text() { return text; }
		public Bounds bounds() { return bounds; }
		public Token(Text text, Bounds bounds) {
			this.text = Objects.requireNonNull(text);
			this.bounds = Objects.requireNonNull(bounds);
		}
	}


	public static Callback<List<Selection>, List<String>> selectionToLines(String columnDelimiter) {
		return xs -> xs.stream()
				.collect(groupingBy(t -> (int) t.token.bounds.getCenterY(),
						TreeMap::new,
						toCollection(() -> new TreeSet<>(
								comparingDouble(t -> t.token.bounds.getCenterX()))))).values()
				.stream().map(x -> x.stream()
						.map(Selection::selectedText)
						.collect(joining(columnDelimiter)))
				.collect(toList());
	}


	final ObjectProperty<Parent> content = new SimpleObjectProperty<>();
	final ObjectProperty<SelectionMode> selectionMode =
			new SimpleObjectProperty<>(SelectionMode.ROW);
	final ObjectProperty<List<Selection>> selected = new SimpleObjectProperty<>(List.of());


	final ObjectProperty<Callback<List<Selection>, Pair<DataFormat, Object>>> clipboardDataFactory =
			new SimpleObjectProperty<>(xs -> new Pair<>(
					DataFormat.PLAIN_TEXT,
					String.join("\n", selectionToLines(" ").call(xs))
			));

	final ObjectProperty<Paint> highlightFill = new SimpleObjectProperty<>(Color.DODGERBLUE);


	public TextSelectPane(Parent parent) { content.set(parent); }

	public TextSelectPane() { }

	public SelectionMode getSelectionMode() { return selectionMode.get(); }
	public ObjectProperty<SelectionMode> selectionModeProperty() { return selectionMode; }
	public void setSelectionMode(
			SelectionMode selectionMode) { this.selectionMode.set(selectionMode); }

	public List<Selection> getSelected() { return selected.get(); }
	public ReadOnlyObjectProperty<List<Selection>> selectedProperty() { return selected; }


	public Paint getHighlightFill() { return highlightFill.get(); }
	public ObjectProperty<Paint> highlightFillProperty() { return highlightFill; }
	public void setHighlightFill(Paint highlightFill) { this.highlightFill.set(highlightFill); }

	public Callback<List<Selection>, Pair<DataFormat, Object>> getClipboardDataFactory() {
		return clipboardDataFactory.get();
	}
	public ObjectProperty<Callback<List<Selection>, Pair<DataFormat, Object>>> clipboardDataFactoryProperty() {
		return clipboardDataFactory;
	}
	public void setClipboardDataFactory(
			Callback<List<Selection>, Pair<DataFormat, Object>> factory) {
		this.clipboardDataFactory.set(factory);
	}


	public void selectAll() {
		var ts = findTextNodes(this);
		updateSelection(ts, resolveTextBounds(ts).stream().map(Selection::single));
	}


	public void selectNone() { updateSelection(findTextNodes(this), Stream.empty()); }

	public void copySelectedToClipboard() {
		var pane = this;
		var entry = pane.clipboardDataFactory.get().call(pane.selected.get());
		Clipboard.getSystemClipboard().setContent(Map.of(entry.getKey(), entry.getValue()));
	}

	public void invalidate() {
		var node = content.get();
		if (node != null) {
			var textNodes = findTextNodes(node);
			var resolved = resolveTextBounds(textNodes);
			updateSelection(textNodes, getSelected().stream());
		}
	}


	@Override
	protected Skin<?> createDefaultSkin() { return new TextSelectPaneSkin(this); }


	void updateSelection(List<Text> all, Stream<Selection> selected) {
		all.forEach(text -> {
			text.setSelectionStart(-1);
			text.setSelectionEnd(-1);
		});
		var xs = selected.collect(toList());
		this.selected.set(xs);

	}


}
