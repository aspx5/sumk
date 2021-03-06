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
package org.yx.rpc.codec;

public final class Protocols {

	public static final int ONE = 0x01;
	public static final int TWO = 0x02;
	public static final int FOUR = 0x04;

	public static final int FORMAT_JSON = 0x010000;

	public static final int RESPONSE_JSON = 0x1000;

	public static final int REQ_PARAM_JSON = 0x0100;

	public static final int REQ_PARAM_ORDER = 0x0200;

	public static int profile() {
		return ONE | TWO | FOUR | FORMAT_JSON | REQ_PARAM_JSON | REQ_PARAM_ORDER | RESPONSE_JSON;
	}

	public static boolean hasFeature(int protocol, int feature) {
		return (protocol & feature) != 0;
	}

	public static final int MAX_LENGTH = 0x3FFFFFFF;

	public static final int MAX_ONE = 0xFF;
	public static final int MAX_TWO = 0xFFFF;

	public static final String LINE_SPLIT = "\n";

	public static final int MAGIC = 0x8F000000;

}
