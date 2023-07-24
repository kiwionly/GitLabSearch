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

	public static void main(String[] args) throws Exception {
		
		String url = ""; // your domain url
		String token = ""; // your gitlab token

		GitLabSearch searcher = new GitLabSearch(url, token, 30); // use longer timeout for larger repository
		searcher.setVerbose(true); // verbose search info for each project, default true
		searcher.setPoolSize(10);  // set before call search method, default 10,

		String query = "my search query"; // case insensitive

//		List<SearchResult> list = searcher.searchByGroupIds(List.of(123L, 456L), query);
//		List<SearchResult> list = searcher.searchByProjectId(1L, query);
//		List<SearchResult> list = searcher.searchMyProjects(query);
		List<SearchResult> list = searcher.searchWithKeyword("myproject", query);

		System.out.printf("Found %s results :\n\n", ansi().fgBrightBlue().a(list.size()).reset());

		for (SearchResult res : list) {
			System.out.printf("project : %s\n", ansi().fgMagenta().a(res.getName()).reset());
			System.out.printf("url     : %s\n", res.getUrl());
			System.out.printf("data    : %s\n", ansi().render(getHighlightedData(res.getData(), query)));

			System.out.println();
		}
	}
	
	public static String getHighlightedData(String data, String query) {

		String lower = data.toLowerCase();
		int index = lower.indexOf(query.toLowerCase());
		String original =  data.substring(index, index + query.length());

		return data.replaceAll("(?i)" + query, "@|red " + original + "|@");
	}

}
