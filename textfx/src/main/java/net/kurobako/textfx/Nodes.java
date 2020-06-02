package net.kurobako.textfx;

import java.util.ArrayList;
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


	public static <T> Optional<T> findParent(Parent parent, Function<Parent, Optional<T>> f) {
		if (parent == null) return Optional.empty();
		var current = parent;
		while (current != null) {
			var x = f.apply(current);
			if (x.isPresent()) return x;
			current = current.getParent();
		}
		return Optional.empty();
	}
}
