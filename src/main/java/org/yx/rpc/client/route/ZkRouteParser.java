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
package org.yx.rpc.client.route;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.yx.common.Host;
import org.yx.common.matcher.Matchers;
import org.yx.conf.AppInfo;
import org.yx.log.Log;
import org.yx.main.SumkThreadPool;
import org.yx.rpc.ZKConst;
import org.yx.rpc.data.RouteInfo;
import org.yx.rpc.data.ZKPathData;
import org.yx.rpc.data.ZkDataOperators;
import org.yx.util.StringUtil;
import org.yx.util.ZkClientHelper;

public final class ZkRouteParser {
	private final String zkUrl;
	private Set<String> childs = Collections.emptySet();
	private final Predicate<String> includes;
	private final Predicate<String> excludes;
	private final String SOA_ROOT = AppInfo.get("sumk.rpc.zk.route", "sumk.rpc.client.zk.route", ZKConst.SUMK_SOA_ROOT);
	private Logger logger = Log.get("sumk.rpc.client");
	private final ThreadPoolExecutor executor;
	private final BlockingQueue<RouteEvent> queue = new LinkedBlockingQueue<>();

	private ZkRouteParser(String zkUrl) {
		this.zkUrl = zkUrl;
		String temp = AppInfo.getLatin("sumk.rpc.server.includes");
		includes = StringUtil.isEmpty(temp) ? null : Matchers.createWildcardMatcher(temp, 1);

		temp = AppInfo.getLatin("sumk.rpc.server.excludes");
		excludes = StringUtil.isEmpty(temp) ? null : Matchers.createWildcardMatcher(temp, 1);
		executor = new ThreadPoolExecutor(1, 1, 5000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(10000),
				SumkThreadPool.createThreadFactory("rpc-client-"), new ThreadPoolExecutor.DiscardPolicy());
		executor.allowCoreThreadTimeOut(true);
	}

	public static ZkRouteParser get(String zkUrl) {
		return new ZkRouteParser(zkUrl);
	}

	public void readRouteAndListen() throws IOException {
		Map<Host, RouteInfo> datas = new HashMap<>();
		ZkClient zk = ZkClientHelper.getZkClient(zkUrl);
		ZkClientHelper.makeSure(zk, SOA_ROOT);

		final IZkDataListener nodeListener = new IZkDataListener() {
			ZkRouteParser parser = ZkRouteParser.this;

			@Override
			public void handleDataChange(String dataPath, Object data) throws Exception {
				logger.trace("{} node changed", dataPath);
				int index = dataPath.lastIndexOf("/");
				if (index > 0) {
					dataPath = dataPath.substring(index + 1);
				}
				RouteInfo d = ZkDataOperators.inst().deserialize(new ZKPathData(dataPath, (byte[]) data));
				if (d == null || d.intfs().isEmpty()) {
					logger.debug("{} has no interface or is invalid node", dataPath);
					parser.handle(RouteEvent.deleteEvent(Host.create(dataPath)));
					return;
				}
				parser.handle(RouteEvent.modifyEvent(d));
			}

			@Override
			public void handleDataDeleted(String dataPath) throws Exception {

			}

		};

		List<String> paths = zk.subscribeChildChanges(SOA_ROOT, new IZkChildListener() {
			ZkRouteParser parser = ZkRouteParser.this;

			@Override
			public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
				if (currentChilds == null) {
					currentChilds = Collections.emptyList();
				}
				List<String> ips = filter(currentChilds);

				List<String> createChilds = new ArrayList<>();
				Set<String> deleteChilds = new HashSet<>(parser.childs);
				for (String zkChild : ips) {
					boolean exist = deleteChilds.remove(zkChild);

					if (!exist) {
						createChilds.add(zkChild);
					}
				}
				ZkClient zkClient = ZkClientHelper.getZkClient(zkUrl);
				parser.childs = new HashSet<>(ips);
				for (String create : createChilds) {
					logger.trace("{} node created", create);
					RouteInfo d = parser.getZkNodeData(zkClient, create);
					if (d == null) {
						continue;
					}
					parser.handle(RouteEvent.createEvent(d));
					zk.subscribeDataChanges(parentPath + "/" + create, nodeListener);
				}

				for (String delete : deleteChilds) {
					logger.trace("{} node deleted", delete);
					parser.handle(RouteEvent.deleteEvent(Host.create(delete)));
					zk.unsubscribeDataChanges(parentPath + "/" + delete, nodeListener);
				}
			}

		});
		if (paths == null) {
			paths = Collections.emptyList();
		}
		paths = filter(paths);
		if (logger.isDebugEnabled()) {
			logger.debug("valid rpc servers: {}", paths);
		}
		this.childs = new HashSet<>(paths);
		for (String path : paths) {
			RouteInfo d = getZkNodeData(zk, path);
			if (d == null) {
				continue;
			}
			zk.subscribeDataChanges(SOA_ROOT + "/" + path, nodeListener);
			datas.put(d.host(), d);
		}
		RpcRoutes.refresh(datas.values());
	}

	private List<String> filter(List<String> currentChilds) {

		if (includes != null) {
			List<String> ips = new ArrayList<>();
			for (String ip : currentChilds) {
				if (includes.test(ip)) {
					ips.add(ip);
				}
			}
			return ips;
		}

		if (excludes != null) {
			List<String> ips = new ArrayList<>();
			for (String ip : currentChilds) {
				if (!excludes.test(ip)) {
					ips.add(ip);
				}
			}
			return ips;
		}

		return currentChilds;
	}

	private RouteInfo getZkNodeData(ZkClient zk, String path) {
		byte[] data = zk.readData(SOA_ROOT + "/" + path);
		try {
			return ZkDataOperators.inst().deserialize(new ZKPathData(path, (byte[]) data));
		} catch (Exception e) {
			logger.error("解析" + path + "的zk数据失败", e);
			return null;
		}
	}

	public void handle(RouteEvent event) {
		if (event == null) {
			return;
		}
		queue.offer(event);
		this.executor.execute(() -> {
			if (queue.isEmpty()) {
				return;
			}
			synchronized (ZkRouteParser.this) {
				List<RouteEvent> list = new ArrayList<>();
				queue.drainTo(list);
				if (list.isEmpty()) {
					return;
				}
				List<RouteInfo> data = RpcRoutes.currentDatas();
				Map<Host, RouteInfo> map = new HashMap<>();
				for (RouteInfo r : data) {
					map.put(r.host(), r);
				}
				if (handleData(map, list) > 0) {
					RpcRoutes.refresh(map.values());
				}
			}
		});
	}

	private int handleData(Map<Host, RouteInfo> data, List<RouteEvent> list) {
		int count = 0;
		for (RouteEvent event : list) {
			if (event == null) {
				continue;
			}
			switch (event.getType()) {
			case CREATE:
			case MODIFY:
				if (logger.isDebugEnabled()) {
					logger.debug("{}: {} {}", count, event.getType(), event.getUrl());
					if (logger.isTraceEnabled()) {
						logger.trace("event的接口列表：{}", event.getRoute().intfs());
					}
				}
				data.put(event.getUrl(), event.getRoute());
				count++;
				break;
			case DELETE:

				if (data.remove(event.getUrl()) != null) {
					logger.debug("{}: {} {}", count, event.getType(), event.getUrl());
					count++;
				}
				break;
			default:
				break;
			}
		}
		return count;
	}

}
