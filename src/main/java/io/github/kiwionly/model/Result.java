package io.github.kiwionly.model;

import java.util.Objects;

public class Result {
	
    private final String name;
    private final String url;
    private final String data;
    
    public Result(String name, String url, String data) {
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
        Result that = (Result) o;
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