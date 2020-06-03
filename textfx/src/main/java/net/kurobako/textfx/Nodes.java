package net.kurobako.textfx;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.util.Pair;


@SuppressWarnings("unused")
public class Nodes {

	static <T> Optional<Pair<T, Integer>> indexOf(
			List<T> xs, boolean reverseOrder, Predicate<T> p) {
		var forward = IntStream.range(0, xs.size());
		return (reverseOrder ? forward.map(i -> xs.size() - i - 1) : forward)
				.filter(i -> p.test(xs.get(i)))
				.mapToObj(i -> new Pair<>(xs.get(i), i))
				.findFirst();
	}

	public static <T> List<T> collectNodes(Parent parent, Function<Node, Optional<T>> f) {
		var ys = new ArrayList<T>();
		var xs = new ArrayList<>(parent.getChildrenUnmodifiable());
		while (!xs.isEmpty()) {
			for (var it = xs.listIterator(); it.hasNext(); ) {
				var x = it.next();
				it.remove();
				if (x instanceof Parent) ((Parent) x).getChildrenUnmodifiable().forEach(it::add);
				else f.apply(x).ifPresent(ys::add);
			}
		}
		return ys;
	}

	public static <T> Optional<T> findParent(Node node, Function<Node, Optional<T>> f) {
		if (node == null) return Optional.empty();
		var current = node;
		while (current != null) {
			var x = f.apply(current);
			if (x.isPresent()) return x;
			current = current.getParent();
		}
		return Optional.empty();
	}

	public static <T> Deque<T> collectParentUntil(Node node,
	                                              boolean reverseOrder,
	                                              Function<Node, Optional<T>> f) {
		var out = new LinkedList<T>();
		var current = node;
		while (current != null) {
			var t = f.apply(current);
			if (t.isPresent()) {
				if (reverseOrder)  out.push(t.get());
				else out.add(t.get());
			} else break;
			current = current.getParent();
		}
		return out;
	}

	public static <T> Deque<T> collectAllParent(Node node,
	                                           boolean reverseOrder,
	                                           Function<Node, T> f) {
		return Nodes.collectParentUntil(node, reverseOrder, x -> Optional.of(f.apply(x)));
	}


}
