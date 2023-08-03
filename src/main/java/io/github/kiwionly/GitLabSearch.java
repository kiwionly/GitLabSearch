package io.github.kiwionly;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GitLabSearch {

	private final OkHttpClient client;
	private final String domain;
	private final String token;

	private boolean verbose = true;

	private int poolSize = 10;

	public GitLabSearch(String domain, String token, int timeOut) throws Exception {

		this.domain = domain;
		this.token = token;

		this.client = createUnsafeOkHttpClient(timeOut);
	}

	private OkHttpClient createUnsafeOkHttpClient(int timeOut) throws Exception {

		// Create a trust manager that does not validate certificate chains
		final TrustManager[] trustAllCerts = new TrustManager[]{

				new X509TrustManager() {
					@Override
					public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
					}

					@Override
					public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
					}

					@Override
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return new java.security.cert.X509Certificate[]{};
					}
				}};

		// Install the trust manager
		final SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

		// Create an OkHttpClient that trusts all certificates
		final OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);

		builder.callTimeout(timeOut, TimeUnit.SECONDS);
		builder.connectTimeout(180, TimeUnit.SECONDS);

		return builder.build();

	}

	private <T> List<T> get(String url, Mapper<T> mapper) throws IOException {

		Request request = new Request.Builder().url(url).addHeader("Authorization", "Bearer " + token).build();

		Response response = client.newCall(request).execute();

		if(!response.isSuccessful()) {
			throw new IOException(response.message());
		}

		String data = response.body().string();

		response.close();

		JSONArray arr = JSON.parseArray(data);

		List<T> list = new ArrayList<>();

        for (Object o : arr) {

            JSONObject obj = (JSONObject) o;

            T t = mapper.map(obj);
            list.add(t);
        }

		return list;
	}

	private interface Mapper<T> {
		T map(JSONObject obj);
	}

	private static class ProjectMapper implements Mapper<Project> {
		@Override
		public Project map(JSONObject obj) {

			long id = obj.getLong("id");
			String name = obj.getString("name");
			String webUrl = obj.getString("web_url");

			return new Project(id, name, webUrl);
		}
	}

	private static class SearchBlobMapper implements Mapper<SearchBlob> {
		@Override
		public SearchBlob map(JSONObject obj) {

			long id = obj.getLong("project_id");
			String data = obj.getString("data");
			String ref = obj.getString("ref");
			String fileName = obj.getString("filename");

			return new SearchBlob(id, data, ref, fileName);
		}
	}

	private void check(String name, String input) {

		if (input == null || input.trim().equals("")) {
			throw new IllegalStateException(name + " cannot be null or empty");
		}
	}

	public void print(String format, Object... args) {
		print(false, format, args);
	}

	private void print(boolean error, String format, Object... args) {

		if (!verbose) {
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

	private <T> List<T> continueFetch(String url, boolean isInQuery, Mapper<T> mapper) throws IOException {

		List<T> list = new ArrayList<>();

		final int rows = 100;
		int page = 0;

		while (true) {

			page += 1;

			String join = "?";

			if(isInQuery) {
				join = "&";
			}

			String fetchUrl = String.format("%s%spage=%d&per_page=%d", url, join, page, rows);

			List<T> res = get(fetchUrl, mapper);
			list.addAll(res);

			if (res.size() < rows) {
				break;
			}
		}

		return list;
	}

	private List<Project> groupsProject(List<Long> groupIds) throws Exception {

		ExecutorService executor = Executors.newFixedThreadPool(10);
		List<Future<List<Project>>> futureList = new ArrayList<>();

		List<Project> projects = new ArrayList<>();

		for (Long group : groupIds) {

			futureList.add(executor.submit(new Callable<List<Project>>() {

				@Override
				public List<Project> call() throws Exception {

					String url = String.format("%s/api/v4/groups/%d/projects", domain, group);

					List<Project> projects = continueFetch(url, false, new ProjectMapper());

					return projects;
				}
			}));
		}

		for (Future<List<Project>> future : futureList) {
			projects.addAll(future.get());
		}

		executor.shutdown();

		return projects;
	}

	@Deprecated
	private List<Project> myProjects() throws Exception {

		String url = String.format("%s/api/v4/projects", domain);

		List<Project> projects = continueFetch(url, false, new ProjectMapper());

		return projects;
	}

	private List<Project> searchProject(String query) throws Exception {

		String url = String.format("%s/api/v4/search?scope=projects&search=%s", domain, query);

		List<Project> projects = continueFetch(url, true, new ProjectMapper());

		return projects;
	}

	public List<SearchResult> searchByGroupIds(List<Long> groupIds, String keywords) throws Exception {
		return search(groupsProject(groupIds), keywords);
	}

	@Deprecated
	public List<SearchResult> searchMyProject(String keywords) throws Exception {
		return search(myProjects(), keywords);
	}

	public List<SearchResult> searchByProject(String query, String keywords) throws Exception {
		return search(searchProject(query), keywords);
	}

	private List<SearchResult> search(List<Project> projects, String keywords) throws Exception {

		check("keywords", keywords);

		if (projects.isEmpty()) {
			print(true, "No projects found, nothing to search : " + keywords);

			return new ArrayList<>();
		}

		int len = getLen(projects);

		String pattern = "%-" + len + "s %-10s";

		ExecutorService executor = Executors.newFixedThreadPool(poolSize);
		List<Future<List<SearchBlob>>> futureList = new ArrayList<>();

		for (Project project : projects) {

			futureList.add(executor.submit(new Callable<List<SearchBlob>>() {

				@Override
				public List<SearchBlob> call() {

					List<SearchBlob> list = new ArrayList<>();

					String q = keywords.replace(" ", "%20");

					try {

						long start = System.currentTimeMillis();

						String url = domain + "/api/v4/projects/" + project.getId() + "/search?scope=blobs&search=" + keywords;

						List<SearchBlob> searchResult = continueFetch(url, true, new SearchBlobMapper());

						print(pattern, project.getName(), System.currentTimeMillis() - start);

						list.addAll(searchResult);

						return list;

					} catch (Exception ex) {
						print(true, "%-" + len + "s <- Fail to search project, retry url: %s/api/v4/projects/%s/search?scope=blobs&search=%s \terror:%s",
								project.getName(), domain, project.getId(), q, ex.getMessage());

						return list;
					}
				}

			}));
		}

		print("api version : %s", "V4");

		print("Searching in %d projects ...\n", projects.size());

		print(pattern, "Project", "took (ms)");
		print(pattern, "-------", "---------");

		List<SearchBlob> result = new ArrayList<>();

		long start = System.currentTimeMillis();

		for (Future<List<SearchBlob>> future : futureList) {
			result.addAll(future.get());
		}

		long end = System.currentTimeMillis() - start;

		print("\nDone search for %d projects, total time took %dms\n", projects.size(), end);

		List<SearchResult> resultList = new ArrayList<>();

		for (SearchBlob searchBlob : result) {

			for (Project project : projects) {

				if (project.getId() == searchBlob.getProjectId()) {

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

	private static int getLen(List<Project> projects) {

		int len = 30;

		for (Project project : projects) {
			int length = project.getName().length();

			if (length > len) {
				len = length + 10;
			}
		}
		return len;
	}

	private static class SearchBlob {

		private long projectId;
		private String data;
		private String ref;
		private String filename;

		public SearchBlob(long projectId, String data, String ref, String filename) {
			this.projectId = projectId;
			this.data = data;
			this.ref = ref;
			this.filename = filename;
		}

		public long getProjectId() {
			return projectId;
		}

		public void setProjectId(long projectId) {
			this.projectId = projectId;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		public String getRef() {
			return ref;
		}

		public void setRef(String ref) {
			this.ref = ref;
		}

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}
	}

	private static class Project {

		private final long id;
		private final String name;
		private final String webUrl;

		public Project(long id, String name, String webUrl) {
			this.id = id;
			this.name = name;
			this.webUrl = webUrl;
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getWebUrl() {
			return webUrl;
		}

		@Override
		public String toString() {
			return "Project [id=" + id + ", name=" + name + ", webUrl=" + webUrl + "]";
		}
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
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SearchResult that = (SearchResult) o;
			return Objects.equals(name, that.name) && Objects.equals(url, that.url) && Objects.equals(data, that.data);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, url, data);
		}

		@Override
		public String toString() {
			return "SearchResult [name=" + name + ", url=" + url + ", data=" + data + "]";
		}

	}
}