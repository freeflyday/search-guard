package com.floragunn.searchguard.rest;

import static org.elasticsearch.rest.RestRequest.Method.GET;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;

import com.floragunn.searchguard.configuration.PrivilegesEvaluator;
import com.floragunn.searchguard.support.ConfigConstants;
import com.floragunn.searchguard.user.User;

public class PermissionAction extends BaseRestHandler {
    private final PrivilegesEvaluator evaluator;
    private final ThreadContext threadContext;

    public PermissionAction(final Settings settings, final RestController controller,
            final PrivilegesEvaluator evaluator, final ThreadPool threadPool) {
        super(settings);
        this.threadContext = threadPool.getThreadContext();
        this.evaluator = evaluator;
        controller.registerHandler(GET, "/_searchguard/permission", this);
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        List<String> permissions = Arrays.asList(request.paramAsStringArray("permissions", new String[0]));
        
        return new RestChannelConsumer() {

            @Override
            public void accept(RestChannel channel) throws Exception {
                
                User user = (User) threadContext.getTransient(ConfigConstants.SG_USER);
                TransportAddress caller = Objects.requireNonNull(
                        (TransportAddress) threadContext.getTransient(ConfigConstants.SG_REMOTE_ADDRESS));

                Map<String, Boolean> evaluationResult = evaluator.evaluateApplicationPrivileges(user, caller,
                        permissions);
                
                try (XContentBuilder builder = channel.newBuilder()) {
                    builder.startObject();
                    builder.startObject("permissions");

                    for (String permission : permissions) {
                        builder.field(permission, evaluationResult.get(permission));
                    }

                    builder.endObject();
                    builder.endObject();

                    BytesRestResponse response = new BytesRestResponse(RestStatus.OK, builder);
                    channel.sendResponse(response);

                } 

            }
        };
    }

    @Override
    public String getName() {
        return "Permission Action";
    }
}
