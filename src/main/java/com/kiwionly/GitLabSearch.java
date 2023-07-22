
package com.kiwionly;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.gitlab4j.api.Constants.ProjectSearchScope;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.SearchBlob;

/**
 * Simple GitLab Search, use async pool to search across gitlab projects content.
 * <br/><br/>
 * See Unit Test case for usage
 */
public class GitLabSearch {

	private final GitLabApi gitLabApi;
	private final String url;

	private int poolSize = 10;
	private boolean verbose = true;

	public GitLabSearch(String token, int timeoutSeconds) {
		this("https://www.gitlab.com", token, timeoutSeconds);
	}

	public GitLabSearch(String url, String token, int timeoutSeconds) {

		this.url = url;

		check("url", url);
		check("token", token);

		gitLabApi = new GitLabApi(url, token);
		gitLabApi.setIgnoreCertificateErrors(true);
		gitLabApi.setRequestTimeout(null, 1000 * timeoutSeconds);
	}

	private void check(String name, String input) {

		if(input == null || input.trim().equals("")) {
			throw new IllegalStateException(name + " cannot be null or empty");
		}
	}

	private void print(String format, Object... args) {
		print(false, format, args);
	}

	private void print(boolean error, String format, Object... args) {

		if(!verbose) {
			return;
		}

		if (error) {
			System.err.printf(format + "\n", args);
			return;
		}

		System.out.printf(format + "\n", args);
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	public List<SearchResult> searchByGroupIds(List<Long> groupIds, String query) throws Exception {

		List<Project> projects = new ArrayList<>();

		for (Long id : groupIds) {
			List<Project> list = gitLabApi.getGroupApi().getProjects(id); // cannot call in async
			projects.addAll(list);
		}

		return search(projects, query);
	}

	public List<SearchResult> searchByProjectId(Long projectId, String query) throws Exception {

		List<Project> projects = List.of(gitLabApi.getProjectApi().getProject(projectId));

		return search(projects, query);
	}

	public List<SearchResult> searchWithKeyword(String search, String query) throws Exception {

		List<Project> projects = gitLabApi.getProjectApi().getProjects(search);

		return search(projects, query);
	}

	public List<SearchResult> searchMyProjects(String query) throws Exception {

		List<Project> projects = gitLabApi.getProjectApi().getMemberProjects();

		return search(projects, query);
	}

	private List<SearchResult> search(List<Project> projects, String query) throws Exception {

		check("query", query);

		if(projects.isEmpty()) {
			print(true, "No projects found, nothing to search : " + query);

			return new ArrayList<>();
		}


		int len = 30;

		for (Project project : projects) {
			int length = project.getName().length();

			if(length > len) {
				len = length + 10;
			}
		}

		String pattern = "%-" + len + "s %-10s";

		ExecutorService executor = Executors.newFixedThreadPool(poolSize);
		List<Future<List<SearchBlob>>> futureList = new ArrayList<>();

		for (Project project : projects) {

			futureList.add(executor.submit(new Callable<>() {

				@Override
				public List<SearchBlob> call() {

					List<SearchBlob> list = new ArrayList<>();

					String q = query.replace(" ", "%20");

					try {

						long start = System.currentTimeMillis();

						List<?> searchResult = gitLabApi.getSearchApi().projectSearch(project.getId(), ProjectSearchScope.BLOBS, q);

						print(pattern, project.getName(), System.currentTimeMillis() - start);

						for (Object obj : searchResult) {
							SearchBlob sb = (SearchBlob) obj;
							list.add(sb);
						}

						return list;

					} catch (GitLabApiException ex) {

						print(true, "Fail to search project : %-30s url:	%s/api/v4/projects/%s/search?scope=blobs&search=%s",
								project.getName(), url, project.getId(), q);

						return list;
					}

				}

			}));
		}

		print("Search %d projects ...\n", projects.size());

		print(pattern, "Project", "took (ms)");
		print(pattern, "-------", "---------");

		List<SearchBlob> result = new ArrayList<>();

		for (Future<List<SearchBlob>> future : futureList) {
			result.addAll(future.get());
		}

		print("\n");

		List<SearchResult> resultList = new ArrayList<>();

		for (SearchBlob searchBlob : result) {

			for (Project project : projects) {

				if (project.getId().equals(searchBlob.getProjectId())) {

					String name = project.getName();
					String data = searchBlob.getData();
					String url = String.format("%s/-/blob/%s/%s", project.getWebUrl(), searchBlob.getRef(), searchBlob.getFilename());

					SearchResult sr = new SearchResult(name, url, data);
					resultList.add(sr);
				}
			}
		}

		executor.shutdown();

		return resultList;
	}

	public static class SearchResult {

		private final String name;
		private final String url;
		private final String data;

		public SearchResult(String name, String url, String data) {
			this.name = name;
			this.url = url;
			this.data = data;
		}

		public String getName() {
			return name;
		}

		public String getData() {
			return data;
		}

		public String getUrl() {
			return url.replace(" ", "%20");
		}

		@Override
		public int hashCode() {
			return Objects.hash(name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if ((obj == null) || (getClass() != obj.getClass()))
				return false;
			SearchResult other = (SearchResult) obj;
			return Objects.equals(name, other.name);
		}

		@Override
		public String toString() {
			return "SearchResult [name=" + name + ", url=" + url + ", data=" + data + "]";
		}

	}


}
