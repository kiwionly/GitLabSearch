package io.github.kiwionly;

import org.junit.jupiter.api.Assertions;

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
