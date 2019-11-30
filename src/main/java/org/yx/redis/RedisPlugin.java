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
package org.yx.redis;

import java.util.concurrent.TimeUnit;

import org.yx.annotation.Bean;
import org.yx.bean.Plugin;
import org.yx.common.lock.Locker;
import org.yx.common.sequence.SeqHolder;
import org.yx.conf.AppInfo;
import org.yx.log.Log;
import org.yx.main.SumkThreadPool;

@Bean
public class RedisPlugin implements Plugin {

	@Override
	public void startAsync() {
		if (AppInfo.subMap("s.redis.").isEmpty()) {
			return;
		}
		try {
			Class.forName("redis.clients.jedis.Jedis");
		} catch (Throwable e) {
			Log.get(Redis.LOG_NAME).warn("Jedis is not in use because of " + e.getMessage());
			return;
		}
		try {
			Log.get(Redis.LOG_NAME).debug("redis pool init");
			RedisLoader.init();
			initSeqUtilCounter();
			SumkThreadPool.scheduledExecutor().scheduleWithFixedDelay(RedisChecker.get(), 5,
					AppInfo.getInt("sumk.redis.check.period", 5), TimeUnit.SECONDS);
		} catch (Exception e) {
			Log.get(Redis.LOG_NAME).error(e.getMessage(), e);
			System.exit(1);
		}
		Locker.init();
	}

	private static void initSeqUtilCounter() {
		if (SeqHolder.inst().getCounter() != null) {
			return;
		}
		Redis redis = RedisPool.getRedisExactly(AppInfo.get("sumk.counter.name", RedisConfig.COUNTER));
		if (redis == null) {
			redis = RedisPool.getRedisExactly("session");
		}
		if (redis != null) {
			Log.get(Redis.LOG_NAME).debug("use redis counter");
			SeqHolder.inst().setCounter(new RedisCounter(redis));
		}
	}

}
