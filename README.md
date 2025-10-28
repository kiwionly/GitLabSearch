# GitLabSearch
Simple java gitlab client that could search across gitlab projects content, compatible with JDK 8 and above.

You can use it as library or command.

### library

#### Gradle

```groovy
implementation 'io.github.kiwionly:gitlab-search:1.2.0'
```

#### Maven

```xml
<dependency>
    <groupId>io.github.kiwionly</groupId>
    <artifactId>gitlab-search</artifactId>
    <version>1.2.0</version>
</dependency>
```

### Command 

You could run as jar or compile to native image ( GitLabSearch.exe ) and run as command :

Here is an example of search projects by name:

```sh
go-gitlabsearch projects -p <you_project_name> -q <search_term> -v -u <gitlab_url> -t <api_token>
```

or ids

```sh
go-gitlabsearch projects -i <id1,id2> -q <search_term> -v -u <gitlab_url> -t <api_token>
```


Or you could search projects by groups:
```sh
go-gitlabsearch groups -g <group1,group2> -q <search_term> -v -u <gitlab_url> -t <api_token>
```

After specific the url and token and run for the first time, it will store in `.gitlab-search` file and will retrieve the url and token in future when running again the command.

Search by groups:

```sh
gitlab-search groups -g <group1,group2> -q <search_term> -v
```

### Compile to native image ( become .exe file ) using Graalvm

Install GraalVM and Visual studio on the build machine, select C++ development tool and Windows SDK when install Visual Studio.

After Visual Studio installed, open command prompt at root directory, run commands below, the first command is to set up build environment,
the Directory might be difference depend on the Visual Studio version you installed.

```cmd
> "C:\Program Files\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64"
> set JAVA_HOME=C:\graalvm-community-openjdk-20.0.1+9.1
> gradlew clean nativeCompile
```

### Library usage :

Sample usage as below:

```
String url = ""; // your domain url
String token = ""; // your gitlab token

GitLabSearch searcher = new GitLabSearch(url, token, 30); // use longer timeout for larger repository
searcher.setVerbose(true); // verbose search info for each project, default true
searcher.setPoolSize(10);  // set before call search method, default 10,

String query = "my search query"; // case insensitive

//	List<SearchResult> list = searcher.searchByGroupIds(List.of(123L, 456L), query);
List<SearchResult> list = searcher.searchWithKeyword("myproject", query);

// print search result
for (SearchResult sr : list) {
								
	for (Result res : sr.getResultList()) {
		System.out.println(res.getName());
		System.out.println(res.getUrl());
		
		for (SearchBlob sb : sr.getSearchBlobList()) {
			System.out.println(sb.getData());
		}
	}
	
}	
```
