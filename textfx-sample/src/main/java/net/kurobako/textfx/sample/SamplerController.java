package net.kurobako.textfx.sample;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

public class SamplerController implements Initializable {

	private static final String URI = "https://github.com/tom91136/textfx";

	static class SampleEntry {
		final String name;
		final Supplier<? extends Sample> sampleFactory;

		SampleEntry(String name, Supplier<? extends Sample> sampleFactory) {
			this.name = name;
			this.sampleFactory = sampleFactory;
		}
	}

	interface Sample {
		Node mkRoot();
	}

	@FXML private VBox root;
	@FXML private Hyperlink link;
	@FXML private TabPane tabs;

	HostServices hostServices;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		List<SampleEntry> samples = Arrays.asList(
				new SampleEntry("Arbitrary text selection", TextSelectSample::new),
				new SampleEntry("Animated text selection", AnimatedTextSelectSample::new),
				new SampleEntry("Highlighted search", HighlightedSearchSample::new),
				new SampleEntry("Highlighted timer", HighlightedTimerSample::new),
				new SampleEntry("Highlighted spellchecker", HighlightedSpellcheckerSample::new),
				new SampleEntry("Highlighted text list", HighlightedListSample::new)
//				new SampleEntry("Arbitrary Node", ArbitraryNodeSample::new)
		);

		samples.forEach(s -> tabs.getTabs().add(new Tab(s.name, s.sampleFactory.get().mkRoot())));

		link.setOnAction(e -> {
			// for cases where java was started in something like i3wm
			if (hostServices == null) {
				TextInputDialog dialog = new TextInputDialog(URI);
				dialog.setTitle("HostService missing");
				dialog.setHeaderText("Unable to open URL due to missing HostService");
				dialog.setContentText("URL");
				dialog.showAndWait();
			} else hostServices.showDocument(URI);
		});

	}

}
