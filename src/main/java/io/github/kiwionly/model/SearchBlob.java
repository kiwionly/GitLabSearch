package io.github.kiwionly.model;

public class SearchBlob {

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

