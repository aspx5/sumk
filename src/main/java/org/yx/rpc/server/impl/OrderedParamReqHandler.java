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
package org.yx.rpc.server.impl;

import org.yx.annotation.Bean;
import org.yx.common.CalleeNode;
import org.yx.common.context.ActionContext;
import org.yx.exception.SumkException;
import org.yx.rpc.RpcActionHolder;
import org.yx.rpc.RpcActionNode;
import org.yx.rpc.RpcGson;
import org.yx.rpc.codec.Protocols;
import org.yx.rpc.codec.Request;
import org.yx.rpc.server.RequestHandler;
import org.yx.rpc.server.Response;
import org.yx.rpc.server.impl.ProxyRpcVisitor.AbstractRpcVisitor;

@Bean
public class OrderedParamReqHandler implements RequestHandler {

	@Override
	public Response handle(Request req) {
		if (!Protocols.hasFeature(req.protocol(), Protocols.REQ_PARAM_ORDER)) {
			return null;
		}
		Response resp = new Response(req.getSn());
		try {
			String method = req.getApi();
			RpcActionNode node = RpcActionHolder.getActionNode(method);
			if (node == null) {
				SumkException.throwException(123546, method + " is not a valid rpc interface");
			}
			Object ret = node.accept(ProxyRpcVisitor.proxy(new RpcVisitor(req)));
			resp.json(RpcGson.toJson(ret));
			resp.exception(null);
		} catch (Throwable e) {
			ServerExceptionHandler.handle(req, resp, e);
		} finally {
			resp.serviceInvokeMilTime(System.currentTimeMillis() - req.getStartInServer());
			ActionContext.remove();
		}
		return resp;
	}

	private static final class RpcVisitor extends AbstractRpcVisitor {

		public RpcVisitor(Request req) {
			super(req);
		}

		@Override
		public Object visit(CalleeNode info) throws Throwable {
			return RpcActionNode.class.cast(info).invokeByOrder(req.getParamArray());
		}

	};

}
