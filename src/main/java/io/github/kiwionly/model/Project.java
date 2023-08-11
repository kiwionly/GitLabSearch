package io.github.kiwionly.model;

public class Project {

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