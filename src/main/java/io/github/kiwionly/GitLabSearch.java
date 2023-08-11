package io.github.kiwionly;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import io.github.kiwionly.model.Project;
import io.github.kiwionly.model.Result;
import io.github.kiwionly.model.SearchBlob;
import io.github.kiwionly.model.SearchResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

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

	private <T> List<T> getWithMapper(String url, Mapper<T> mapper) throws IOException {

		String data = httpGet(url);

		JSONArray arr = JSON.parseArray(data);

		List<T> list = new ArrayList<>();

        for (Object o : arr) {

            JSONObject obj = (JSONObject) o;

            T t = mapper.map(obj);
            list.add(t);
        }

		return list;
	}

	@NotNull
	private String httpGet(String url) throws IOException {

		Request request = new Request.Builder().url(url).addHeader("Authorization", "Bearer " + token).build();

		Response response = client.newCall(request).execute();

		if(!response.isSuccessful()) {
			throw new IOException(response.message());
		}

		String data = response.body().string();

		response.close();

		return data;
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


	public void print(String format, Object... args) {

		if (!verbose) {
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

			List<T> res = getWithMapper(fetchUrl, mapper);
			list.addAll(res);

			if (res.size() < rows) {
				break;
			}
		}

		return list;
	}

	private List<Project> groupsProject(List<Long> groupIds) {

		ExecutorService executor = Executors.newFixedThreadPool(groupIds.size());
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
			try {
				projects.addAll(future.get());
			}
			catch(Exception ex){
				System.err.println(ex.getMessage());
			}
		}

		executor.shutdown();

		return projects;
	}

	private List<Project> projects(List<Long> projectIds) {

		ExecutorService executor = Executors.newFixedThreadPool(projectIds.size());
		List<Future<Project>> futureList = new ArrayList<>();

		List<Project> projects = new ArrayList<>();

		for (Long id : projectIds) {

			futureList.add(executor.submit(new Callable<Project>() {

				@Override
				public Project call() throws Exception {

					String url = String.format("%s/api/v4/projects/%d", domain, id);

					String json = httpGet(url);
					JSONObject obj = JSON.parseObject(json);

					Project project = new ProjectMapper().map(obj);

					return project;
				}
			}));
		}

		for (Future<Project> future : futureList) {
			try {
				projects.add(future.get());
			}
			catch(Exception ex) {
				System.err.println(ex.getMessage());
			}
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

	public List<SearchResult> searchByProjectIds(List<Long> projectIds, String keywords) throws Exception {
		return search(projects(projectIds), keywords);
	}

	@Deprecated
	public List<SearchResult> searchMyProject(String keywords) throws Exception {
		return search(myProjects(), keywords);
	}

	public List<SearchResult> searchByProject(String query, String keywords) throws Exception {
		return search(searchProject(query), keywords);
	}

	public String getVersion() {
		return "V4";
	}

	private List<SearchResult> search(List<Project> projects, String keywords) {

		if (keywords == null || keywords.trim().equals("")) {
			throw new IllegalStateException("keywords cannot be null or empty");
		}

		if (projects.isEmpty()) {
			throw new IllegalStateException("No projects found, nothing to search : " + keywords);
		}

		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("id", "%-10s");
		headers.put("project", "%-" + getLen(projects) + "s");
		headers.put("took (ms)", "%-15s");
		headers.put("result", "%-10s");
		headers.put("debug url", "%-100s");
		headers.put("error", "%-20s");

		String pattern = getPattern(headers);

		ExecutorService executor = Executors.newFixedThreadPool(poolSize);
		List<Future<SearchResult>> futureList = new ArrayList<>();

		for (Project project : projects) {

			futureList.add(executor.submit(new Callable<SearchResult>() {

				@Override
				public SearchResult call() {

					long start = System.currentTimeMillis();

					SearchResult sr = new SearchResult();
					String q = keywords.replace(" ", "%20");

					try {

						String url = domain + "/api/v4/projects/" + project.getId() + "/search?scope=blobs&search=" + q;

						List<SearchBlob> list = continueFetch(url, true, new SearchBlobMapper());
						sr.setSearchBlobList(list);

						sr.setId(project.getId());
						sr.setName(project.getName());
						sr.setCount(list.size());

					} catch (Exception ex) {

						String url = String.format("%s/api/v4/projects/%s/search?scope=blobs&search=%s", domain, project.getId(), q);

						sr.setDebugUrl(url);
						sr.setError(ex.getMessage());
						sr.setCount(-1);
					}

					print(pattern, sr.getId(), sr.getName(), System.currentTimeMillis() - start, sr.getCount(), sr.getDebugUrl(), sr.getError());

					return sr;
				}

			}));
		}

		print("Searching in %d projects ...\n", projects.size());

		print(pattern, getHeaders(headers, false).toArray());
		print(pattern, getHeaders(headers, true).toArray());

		List<SearchResult> searchResultList = new ArrayList<>();

		long start = System.currentTimeMillis();

		for (Future<SearchResult> future : futureList) {
			try {
				searchResultList.add(future.get());
			}
			catch(Exception ex){
				System.err.println(ex.getMessage());
			}
		}

		long end = System.currentTimeMillis() - start;

		print("\nDone search for %d projects, total time took %dms\n", projects.size(), end);


		for (SearchResult sr : searchResultList) {
			for (Project project : projects) {

				if (project.getId() == sr.getId()) {

					List<Result> rList = new ArrayList<>();

					for(SearchBlob searchBlob :sr.getSearchBlobList())
					{
						String name = project.getName();
						String data = searchBlob.getData();
						String url = String.format("%s/-/blob/%s/%s", project.getWebUrl(), searchBlob.getRef(), searchBlob.getFilename());

						Result rs = new Result(name, url, data);
						rList.add(rs);
					}

					sr.setResultList(rList);
				}
			}
		}

		executor.shutdown();

		return searchResultList;
	}

	private String getPattern(Map<String, String> headers) {

		StringBuilder pattern = new StringBuilder();

		for (String key: headers.keySet()) {
			pattern.append(headers.get(key));
		}

		return pattern.toString();
	}

	private List<String> getHeaders(Map<String, String> headers, boolean padding) {

		List<String> list = new ArrayList<>();

		for (String key: headers.keySet()) {

			if(padding) {
				String pad = key.replaceAll(".", "-");
				list.add(pad);
			}
			else {
				list.add(key);
			}
		}

		return list;
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






}