package io.github.kiwionly;

import static org.fusesource.jansi.Ansi.ansi;

import java.util.List;

import org.junit.jupiter.api.Assertions;

import io.github.kiwionly.GitLabSearch.SearchResult;
import org.junit.jupiter.api.Test;

class GitLabSearchTest {

	@Test
	public void testInitialFail() {
		IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
				() -> new GitLabSearch("", "", 30)
		);

		String expectedMessage = "url cannot be null or empty";
		String actualMessage = exception.getMessage();
		Assertions.assertEquals(expectedMessage, actualMessage);
	}

}
