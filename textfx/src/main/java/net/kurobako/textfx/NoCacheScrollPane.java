package net.kurobako.textfx;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

public class NoCacheScrollPane extends ScrollPane {

	private Node viewport;

	{
		needsLayoutProperty().addListener((o, p, n) -> {
			if (viewport == null) {
				viewport = lookup(".viewport");
			}
			if (viewport != null) viewport.setCache(false);

		});
	}


	public NoCacheScrollPane(Node content) {  super(content);  }
}
