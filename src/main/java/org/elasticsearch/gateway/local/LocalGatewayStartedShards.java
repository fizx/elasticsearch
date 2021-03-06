/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.gateway.local;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.shard.ShardId;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class LocalGatewayStartedShards {

    private final long version;

    private final ImmutableMap<ShardId, Long> shards;

    public LocalGatewayStartedShards(long version, Map<ShardId, Long> shards) {
        this.version = version;
        this.shards = ImmutableMap.copyOf(shards);
    }

    public long version() {
        return version;
    }

    public ImmutableMap<ShardId, Long> shards() {
        return shards;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private long version;

        private Map<ShardId, Long> shards = Maps.newHashMap();

        public Builder state(LocalGatewayStartedShards state) {
            this.version = state.version();
            this.shards.putAll(state.shards);
            return this;
        }

        public Builder version(long version) {
            this.version = version;
            return this;
        }

        /**
         * Returns <tt>true</tt> if something really changed.
         */
        public boolean remove(ShardId shardId) {
            return shards.remove(shardId) != null;
        }

        /**
         * Returns <tt>true</tt> if something really changed.
         */
        public boolean put(ShardId shardId, long version) {
            Long lVersion = shards.get(shardId);
            if (lVersion != null && lVersion == version) {
                return false;
            }
            this.shards.put(shardId, version);
            return true;
        }

        public LocalGatewayStartedShards build() {
            return new LocalGatewayStartedShards(version, shards);
        }

        public static void toXContent(LocalGatewayStartedShards state, XContentBuilder builder, ToXContent.Params params) throws IOException {
            builder.startObject("state");

            builder.field("version", state.version());

            builder.startArray("shards");
            for (Map.Entry<ShardId, Long> entry : state.shards.entrySet()) {
                builder.startObject();
                builder.field("index", entry.getKey().index().name());
                builder.field("id", entry.getKey().id());
                builder.field("version", entry.getValue());
                builder.endObject();
            }
            builder.endArray();

            builder.endObject();
        }

        public static LocalGatewayStartedShards fromXContent(XContentParser parser) throws IOException {
            Builder builder = new Builder();

            String currentFieldName = null;
            XContentParser.Token token = parser.nextToken();
            if (token == null) {
                // no data...
                return builder.build();
            }
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else if (token == XContentParser.Token.START_ARRAY) {
                    if ("shards".equals(currentFieldName)) {
                        while ((token = parser.nextToken()) != XContentParser.Token.END_ARRAY) {
                            if (token == XContentParser.Token.START_OBJECT) {
                                String shardIndex = null;
                                int shardId = -1;
                                long version = -1;
                                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                                    if (token == XContentParser.Token.FIELD_NAME) {
                                        currentFieldName = parser.currentName();
                                    } else if (token.isValue()) {
                                        if ("index".equals(currentFieldName)) {
                                            shardIndex = parser.text();
                                        } else if ("id".equals(currentFieldName)) {
                                            shardId = parser.intValue();
                                        } else if ("version".equals(currentFieldName)) {
                                            version = parser.longValue();
                                        }
                                    }
                                }
                                builder.shards.put(new ShardId(shardIndex, shardId), version);
                            }
                        }
                    }
                } else if (token.isValue()) {
                    if ("version".equals(currentFieldName)) {
                        builder.version = parser.longValue();
                    }
                }
            }

            return builder.build();
        }

        public static LocalGatewayStartedShards readFrom(StreamInput in) throws IOException {
            LocalGatewayStartedShards.Builder builder = new Builder();
            builder.version = in.readLong();
            int size = in.readVInt();
            for (int i = 0; i < size; i++) {
                builder.shards.put(ShardId.readShardId(in), in.readLong());
            }
            return builder.build();
        }

        public static void writeTo(LocalGatewayStartedShards state, StreamOutput out) throws IOException {
            out.writeLong(state.version());

            out.writeVInt(state.shards.size());
            for (Map.Entry<ShardId, Long> entry : state.shards.entrySet()) {
                entry.getKey().writeTo(out);
                out.writeLong(entry.getValue());
            }
        }
    }

}
