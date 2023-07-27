# GitLabSearch
Simple java gitlab client that could search across gitlab projects content, compatible with JDK 8 and above.

### Gradle

```
implementation 'io.github.kiwionly:gitlab-search:1.0.1'
```

### Maven

```
<dependency>
    <groupId>io.github.kiwionly</groupId>
    <artifactId>gitlab-search</artifactId>
    <version>1.0.1</version>
</dependency>
```
### Sample

You could run the sample in the GitLabSearchTest.java, but it needs dependency as below: 

```
implementation 'org.fusesource.jansi:jansi:2.4.0'
```

Usage ( see GitLabSearchTest.java ) :

```
String url = ""; // your domain url
String token = ""; // your gitlab token

GitLabSearch searcher = new GitLabSearch(url, token, 30); // use longer timeout for larger repository
searcher.setVerbose(true); // verbose search info for each project, default true
searcher.setPoolSize(10);  // set before call search method, default 10,

String query = "my search query"; // case insensitive

//	List<SearchResult> list = searcher.searchByGroupIds(List.of(123L, 456L), query);
//	List<SearchResult> list = searcher.searchByProjectId(1L, query);
//	List<SearchResult> list = searcher.searchMyProjects(query);
List<SearchResult> list = searcher.searchWithKeyword("myproject", query);

System.out.printf("Found %s results :\n\n", ansi().fgBrightBlue().a(list.size()).reset());

// print search result
for (SearchResult res : list) {
	System.out.printf("project : %s\n", ansi().fgMagenta().a(res.getName()).reset());
	System.out.printf("url     : %s\n", res.getUrl());
	System.out.printf("data    : %s\n", ansi().render(getHighlightedData(res.getData(), query)));

	System.out.println();
}
```
