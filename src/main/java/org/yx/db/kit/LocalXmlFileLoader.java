/**
 * Copyright (C) 2016 - 2030 youtongluan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yx.db.kit;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.yx.bean.Loader;
import org.yx.conf.AppInfo;
import org.yx.conf.MultiResourceLoader;
import org.yx.exception.SumkException;
import org.yx.main.SumkThreadPool;
import org.yx.util.FileUtil;

public class LocalXmlFileLoader implements MultiResourceLoader, Runnable {
	private Consumer<MultiResourceLoader> consumer;
	private FileTime[] times;
	private final String rootUri;

	public LocalXmlFileLoader(String rootUri) {
		this.rootUri = Objects.requireNonNull(rootUri);
	}

	private File pathInClassPath(String uri) throws URISyntaxException, IOException {
		Enumeration<URL> urls = Loader.getResources(uri);
		if (urls == null) {
			SumkException.throwException(356453425, "can not find path " + uri);
		}
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			File f = new File(url.toURI());
			if (f.exists() && f.isDirectory()) {
				return f;
			}
		}
		return null;
	}

	private File getRoot() throws Exception {
		String uri = rootUri;
		if (uri.startsWith(AppInfo.CLASSPATH_ALL_URL_PREFIX)) {
			return pathInClassPath(uri.substring(AppInfo.CLASSPATH_ALL_URL_PREFIX.length()));
		}
		if (uri.startsWith(AppInfo.CLASSPATH_URL_PREFIX)) {
			return pathInClassPath(uri.substring(AppInfo.CLASSPATH_URL_PREFIX.length()));
		}
		return new File(uri);
	}

	@Override
	public Map<String, byte[]> openResources(String db) throws Exception {
		List<File> files = new ArrayList<>();
		File root = getRoot();
		if (root == null) {
			return Collections.emptyMap();
		}
		Map<String, byte[]> map = new HashMap<>();
		FileUtil.listAllSubFiles(files, root);
		List<FileTime> timeList = new ArrayList<>(files.size());
		for (int i = 0; i < files.size(); i++) {
			File f = files.get(i);
			if (!f.getName().endsWith(".xml")) {
				continue;
			}
			map.put(f.getName(), Files.readAllBytes(f.toPath()));
			timeList.add(new FileTime(f.getAbsolutePath(), f.lastModified()));
		}
		this.times = timeList.toArray(new FileTime[timeList.size()]);
		return map;
	}

	@Override
	public synchronized boolean startListen(Consumer<MultiResourceLoader> consumer) {
		if (this.consumer != null) {
			return false;
		}
		this.consumer = consumer;
		SumkThreadPool.scheduledExecutor.scheduleWithFixedDelay(this, 60, AppInfo.getLong("sumk.db.sdb.delay", 60),
				TimeUnit.SECONDS);
		return true;
	}

	@Override
	public void run() {
		for (FileTime ft : this.times) {
			File f = new File(ft.file);
			if (f.lastModified() > ft.lastModify) {
				consumer.accept(this);
				return;
			}
		}
	}

	private static class FileTime {

		String file;
		long lastModify;

		private FileTime(String file, long lastModify) {
			super();
			this.file = file;
			this.lastModify = lastModify;
		}

	}
}