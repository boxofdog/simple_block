package io.github.boxofdog.simpleblock.gametest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Evidence lines in the test log, prefixed so they are easy to grep out of a run. */
public final class SimpleBlockTestLog {
	private static final Logger LOGGER = LoggerFactory.getLogger("simple_block-evidence");

	private SimpleBlockTestLog() {
	}

	public static void info(String message, Object... args) {
		LOGGER.info(message, args);
	}
}
