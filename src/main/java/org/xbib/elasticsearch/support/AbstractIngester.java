
package org.xbib.elasticsearch.support;

import org.elasticsearch.ElasticSearchTimeoutException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.settings.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.status.IndicesStatusRequest;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.action.admin.indices.status.ShardStatus;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;

import java.io.IOException;
import java.net.URI;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Transport client support
 *
 * @author <a href="mailto:joergprante@gmail.com">J&ouml;rg Prante</a>
 */
public abstract class AbstractIngester extends AbstractClient
        implements DocumentIngester, ClientIngester, ClientFactory {

    private final static ESLogger logger = Loggers.getLogger(AbstractIngester.class);

    /**
     * An optional mapping
     */
    private String mapping;

    private boolean dateDetection = false;

    private boolean timeStampFieldEnabled = false;

    private String timeStampField = "@timestamp";

    /**
     * The default index
     */
    private String index;
    /**
     * The default type
     */
    private String type;

    public AbstractIngester newClient() {
        super.newClient();
        return this;
    }

    public AbstractIngester newClient(URI uri) {
        super.newClient(uri);
        return this;
    }

    @Override
    public AbstractIngester setIndex(String index) {
        this.index = index;
        return this;
    }

    @Override
    public String getIndex() {
        return index;
    }

    @Override
    public AbstractIngester setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public AbstractIngester waitForCluster() throws IOException {
        return waitForCluster(ClusterHealthStatus.YELLOW, TimeValue.timeValueSeconds(30));
    }

    @Override
    public AbstractIngester waitForCluster(ClusterHealthStatus status, TimeValue timeout) throws IOException {
        try {
            logger.info("waiting for cluster state {}", status.name());
            ClusterHealthResponse healthResponse =
                    client.admin().cluster().prepareHealth().setWaitForStatus(status).setTimeout(timeout).execute().actionGet();
            if (healthResponse.isTimedOut()) {
                throw new IOException("cluster state is " + healthResponse.getStatus().name()
                        + " and not " + status.name()
                        + ", cowardly refusing to continue with operations");
            } else {
                logger.info("... cluster state ok");
            }
        } catch (ElasticSearchTimeoutException e) {
            throw new IOException("timeout, cluster does not respond to health request, cowardly refusing to continue with operations");
        }
        return this;
    }

    public int waitForRecovery() {
        if (getIndex() == null) {
            logger.warn("not waiting for recovery, index not set");
            return -1;
        }
        IndicesStatusResponse response = client.admin().indices()
                .status(new IndicesStatusRequest(getIndex())
                        .recovery(true)
                ).actionGet();
        logger.info("indices status response = {}, failed = {}", response.getTotalShards(), response.getFailedShards());
        for (ShardStatus status : response.getShards()) {
            logger.info("shard {} status {}", status.getShardId(), status.getState().name());
        }
        return response.getTotalShards();
    }

    public int updateReplicaLevel(int level) throws IOException {
        if (getIndex() == null) {
            logger.warn("no index name given");
            return -1;
        }
        waitForCluster(ClusterHealthStatus.YELLOW, TimeValue.timeValueSeconds(30));
        update("number_of_replicas", level);
        return waitForRecovery();
    }


    public AbstractIngester shards(int shards) {
        return setting("index.number_of_shards", shards);
    }

    public AbstractIngester replica(int replica) {
        return setting("index.number_of_replicas", replica);
    }

    /**
     * Optional settings
     */
    private ImmutableSettings.Builder settingsBuilder;


    public AbstractIngester setting(String key, String value) {
        if (settingsBuilder == null) {
            settingsBuilder = ImmutableSettings.settingsBuilder();
        }
        settingsBuilder.put(key, value);
        return this;
    }

    public AbstractIngester setting(String key, Boolean value) {
        if (settingsBuilder == null) {
            settingsBuilder = ImmutableSettings.settingsBuilder();
        }
        settingsBuilder.put(key, value);
        return this;
    }

    public AbstractIngester setting(String key, Integer value) {
        if (settingsBuilder == null) {
            settingsBuilder = ImmutableSettings.settingsBuilder();
        }
        settingsBuilder.put(key, value);
        return this;
    }

    public ImmutableSettings.Builder settings() {
        return settingsBuilder != null ? settingsBuilder : null;
    }

    public AbstractIngester dateDetection(boolean dateDetection) {
        this.dateDetection = dateDetection;
        return this;
    }

    public boolean dateDetection() {
        return dateDetection;
    }

    public AbstractIngester timeStampField(String timeStampField) {
        this.timeStampField = timeStampField;
        return this;
    }

    public String timeStampField() {
        return timeStampField;
    }

    public AbstractIngester timeStampFieldEnabled(boolean enable) {
        this.timeStampFieldEnabled = enable;
        return this;
    }

    public AbstractIngester mapping(String mapping) {
        this.mapping = mapping;
        return this;
    }

    public String mapping() {
        return mapping;
    }

    public String defaultMapping() {
        try {
            XContentBuilder b =
                    jsonBuilder()
                            .startObject()
                            .startObject("_default_")
                            .field("date_detection", dateDetection);
            if (timeStampFieldEnabled) {
                            b.startObject("_timestamp")
                            .field("enabled", timeStampFieldEnabled)
                            .field("path", timeStampField)
                            .endObject();
            }

                            b.startObject("properties")
                            .startObject("@fields")
                            .field("type", "object")
                            .field("dynamic", true)
                            .field("path", "full")
                            .endObject()
                            .startObject("@message")
                            .field("type", "string")
                            .field("index", "analyzed")
                            .endObject()
                            .startObject("@source")
                            .field("type", "string")
                            .field("index", "not_analyzed")
                            .endObject()
                            .startObject("@source_host")
                            .field("type", "string")
                            .field("index", "not_analyzed")
                            .endObject()
                            .startObject("@source_path")
                            .field("type", "string")
                            .field("index", "not_analyzed")
                            .endObject()
                            .startObject("@tags")
                            .field("type", "string")
                            .field("index", "not_analyzed")
                            .endObject()
                            .startObject("@timestamp")
                            .field("type", "date")
                            .endObject()
                            .startObject("@type")
                            .field("type", "string")
                            .field("index", "not_analyzed")
                            .endObject()
                            .endObject()
                            .endObject()
                            .endObject();
            return b.string();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    protected AbstractIngester enableRefreshInterval() {
        update("refresh_interval", 1000);
        return this;
    }

    protected AbstractIngester disableRefreshInterval() {
        update("refresh_interval", -1);
        return this;
    }

    public synchronized AbstractIngester newIndex(boolean ignoreException) {
        if (client == null) {
            logger.warn("no client");
            return this;
        }
        if (getIndex() == null) {
            logger.warn("no index name given to create");
            return this;
        }
        if (getType() == null) {
            logger.warn("no type name given to create");
            return this;
        }
        CreateIndexRequest request = new CreateIndexRequest(getIndex());
        if (settings() != null) {
            request.settings(settings());
        }
        if (mapping() == null) {
            mapping(defaultMapping());
        }
        request.mapping(getType(), mapping());
        logger.info("creating index = {} type = {} settings = {} mapping = {}",
                getIndex(),
                getType(),
                settings() != null ? settings().build().getAsMap() : "",
                mapping());
        try {
            client.admin().indices().create(request).actionGet();
        } catch (IndexAlreadyExistsException e) {
            if (!ignoreException) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    protected void update(String key, Object value) {
        if (client == null) {
            return;
        }
        if (value == null) {
            return;
        }
        if (getIndex() == null) {
            return;
        }
        ImmutableSettings.Builder settingsBuilder = ImmutableSettings.settingsBuilder();
        settingsBuilder.put(key, value.toString());
        UpdateSettingsRequest updateSettingsRequest = new UpdateSettingsRequest(getIndex())
                .settings(settingsBuilder);
        client.admin().indices()
                .updateSettings(updateSettingsRequest)
                .actionGet();
    }


}
