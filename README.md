# GitLabSearch
Simple java gitlab client that could search across gitlab projects content, compatible with JDK 8 and above.

You can use it as library or command.

### library

#### Gradle

```groovy
implementation 'io.github.kiwionly:gitlab-search:1.1.1'
```

#### Maven

```xml
<dependency>
    <groupId>io.github.kiwionly</groupId>
    <artifactId>gitlab-search</artifactId>
    <version>1.1.1</version>
</dependency>
```

### Command 

You could run as jar or compile to native image ( GitLabSearch.exe ) and run as command :

```sh
Usage: GitLabSearch [-hvV] [-n=<poolSize>] [-o=<timeout>] -q=<keywords>
                    [-t=<token>] [-u=<url>] (-g=<groups>[,<groups>...]
                    [-g=<groups>[,<groups>...]]... | -p=<projectId> |
                    -s=<search>)
For searching in gitlab repositories
  -g, --groups=<groups>[,<groups>...]
                            search by group id, separate multiple group in comma
  -h, --help                Show this help message and exit.
  -n, --poolSize=<poolSize> pool size for search in difference projects
  -o, --timeout=<timeout>   timeout in seconds, default : 30 seconds
  -p, --project=<projectId> search by project id, use "0" for user's own
                              projects
  -q, --query=<keywords>    keywords to match
  -s, --search=<search>     search by project name in gitlab
  -t, --token=<token>       api token
  -u, --url=<url>           domain url
  -v, --verbose             verbose more information to screen
  -V, --version             Print version information and exit.

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
for (SearchResult res : list) {
	System.out.printf("project : %s\n", res.getName());
	System.out.printf("url     : %s\n", res.getUrl());
	System.out.printf("data    : %s\n", res.getData());

	System.out.println();
}
```
