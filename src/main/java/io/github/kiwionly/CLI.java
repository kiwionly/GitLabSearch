package io.github.kiwionly;

import io.github.kiwionly.model.Result;
import io.github.kiwionly.model.SearchResult;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import static org.fusesource.jansi.Ansi.ansi;
import static picocli.CommandLine.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

@Command(name = "GitLabSearch", description = "For searching in gitlab repositories", version="1.1.0", mixinStandardHelpOptions = true)
public class CLI implements Callable<Integer> {

    // required

    @Option(names = {"-q", "--query"}, description = "keywords to match")
    private String keywords;


    // optional or get from file if not exist

    @Option(names = {"-u", "--url"}, description = "domain url")
    private String url = "";

    @Option(names = {"-t", "--token"}, description = "api token")
    private String token = "";


    // optional

    @Option(names = {"-o", "--timeout"}, description = "timeout in seconds, default : 30 seconds", defaultValue = "30")
    private int timeout = 30;

    @Option(names = {"-n", "--poolSize"}, description = "pool size for search in difference projects", defaultValue = "10")
    private int poolSize = 10;

    @Option(names = {"-v", "--verbose"}, description = "verbose more information to screen")
    private boolean verbose = false;


    // search by either one of the method

    @ArgGroup(multiplicity = "1")
    Dependent searchDependent;

    static class Dependent {
        @Option(names = {"-g", "--groups"}, split = ",", description = "search by group id, separate multiple id in comma")
        private List<Long> groupIds = new ArrayList<>();

//        @Option(names = {"-p", "--project"}, description = "search by project id, use \"0\" for user's own projects")
//        private Long projectId = -1L;

        @Option(names = {"-p", "--projects"}, split = ",", description = "search by project ids, s, separate multiple id in comma")
        private List<Long> projectIds = new ArrayList<>();

        @Option(names = {"-s", "--search"}, description = "search by project name in gitlab")
        private String search = "";
    }

    private List<SearchResult> search(GitLabSearch searcher) throws Exception {

        if(!searchDependent.groupIds.isEmpty() ) {
            searcher.print("Search in group ids :" +  searchDependent.groupIds);
            return  searcher.searchByGroupIds(searchDependent.groupIds, keywords);
        }

        if(!searchDependent.projectIds.isEmpty() ) {
            searcher.print("Search in project ids :" +  searchDependent.projectIds);
            return  searcher.searchByProjectIds(searchDependent.projectIds, keywords);
        }

//        if(searchDependent.projectId == 0) {
//            searcher.print("Search my projects ...");
//            return searcher.searchMyProjects(keywords);
//        }

//        if(searchDependent.projectId > 0) {
//            searcher.print("Search in project id :" +  searchDependent.projectId);
//            return searcher.searchByProjectId(searchDependent.projectId, keywords);
//        }

        if(!searchDependent.search.equals("")) {
            searcher.print("Search in project :" +  searchDependent.search);
            return searcher.searchByProject(searchDependent.search, keywords);
        }

        throw new IllegalStateException("no specific search method, check is optional args for search is valid");
    }


    @Override
    public Integer call() throws Exception {

        // check if config file exist
        loadUrlTokenIfExistFromFile();

        GitLabSearch gitLabSearch = new GitLabSearch(url, token, timeout);
        gitLabSearch.setVerbose(verbose);
        gitLabSearch.setPoolSize(poolSize);

        System.out.printf("api version : %s\n", ansi().fgBrightCyan().a(gitLabSearch.getVersion()).reset());

        long start = System.currentTimeMillis();

        List<SearchResult> list = search(gitLabSearch);

        System.out.printf("Found %s results :\n\n", ansi().fgBrightBlue().a(list.size()).reset());

        int count = 0;

        for (SearchResult sr : list) {

            count += sr.getResultList().size();

            for (Result res : sr.getResultList()) {
                System.out.printf("project : %s\n", ansi().fgMagenta().a(res.getName()).reset());
                System.out.printf("url     : %s\n", res.getUrl());
                System.out.printf("data    : %s\n", ansi().render(getHighlightedData(res.getData(), keywords)));

                System.out.println();
            }
        }

        System.out.printf("search result(s) = %d\n", count);
        System.out.printf("total time used  = %s ms\n", System.currentTimeMillis() - start);

        return 0;
    }

    /**
     * Load url and token from properties file, is url and token is pass in args, overwrite file with
     * pass in value.
     *
     * @throws IOException
     */
    private void loadUrlTokenIfExistFromFile() throws IOException {

        final String FILE = ".GitLabSearch";

        // write to file
        Properties properties = new Properties();

        if(!Files.exists(Paths.get(FILE))) {
            File file = new File(FILE);
            file.createNewFile();
        }

        properties.load(new FileReader(FILE));

        if(url.equals("")) {
            url = properties.getProperty("url");
        } else {
            properties.setProperty("url", url);
        }

        if(token.equals("")) {
            token = properties.getProperty("token");
        } else {
            properties.setProperty("token", token);
        }

        properties.store(new FileWriter(FILE), "properties file for " + FILE);
    }

    private String getHighlightedData(String data, String query) {

        String lower = data.toLowerCase();
        int index = lower.indexOf(query.toLowerCase());

        // if data no search string
        if (index < 0) {
            return data;
        }

        String original =  data.substring(index, index + query.length());

        return data.replaceAll("(?i)" + query, "@|red " + original + "|@");
    }


    public static void main(String[] args) {

        CommandLine cmd = new CommandLine(new CLI());

        if(args.length <= 0) {
            cmd.usage(System.out);
            return;
        }

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

}


