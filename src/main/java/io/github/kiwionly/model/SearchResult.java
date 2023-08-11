package io.github.kiwionly.model;

import java.util.ArrayList;
import java.util.List;

public class SearchResult {
	private long id;
	private String name;
	private int count;
	private String error = "";
	private String DebugUrl = "";

	private List<SearchBlob> searchBlobList = new ArrayList<>();

	private List<Result> resultList = new ArrayList<>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getDebugUrl() {
		return DebugUrl;
	}

	public void setDebugUrl(String debugUrl) {
		DebugUrl = debugUrl;
	}

	public List<SearchBlob> getSearchBlobList() {
		return searchBlobList;
	}

	public void setSearchBlobList(List<SearchBlob> searchBlobList) {
		this.searchBlobList = searchBlobList;
	}

	public List<Result> getResultList() {
		return resultList;
	}

	public void setResultList(List<Result> resultList) {
		this.resultList = resultList;
	}
}