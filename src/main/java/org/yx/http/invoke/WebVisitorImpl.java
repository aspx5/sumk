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
package org.yx.http.invoke;

import org.yx.http.act.HttpActionNode;
import org.yx.http.handler.WebContext;

public class WebVisitorImpl implements WebVisitor {

	@Override
	public Object visit(WebContext ctx) throws Throwable {
		HttpActionNode http = ctx.httpNode();
		return http.execute(http.buildArgPojo(ctx.data()));
	}

}