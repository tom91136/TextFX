package net.kurobako.textfx.sample;

import com.google.common.base.Throwables;
import com.google.common.io.Resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class SampleText {
	private SampleText() {
	}

	@SuppressWarnings("UnstableApiUsage")
	static List<String> load(String file) {
		try {
			return Resources.readLines(
					Resources.getResource(file), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return Throwables.getStackTraceAsString(e).lines().collect(Collectors.toList());
		}
	}

}
