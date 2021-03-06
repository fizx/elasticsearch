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

package org.elasticsearch.test.integration.client.transport;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.test.integration.AbstractNodesTests;
import org.testng.annotations.AfterMethod;

import static org.elasticsearch.client.Requests.createIndexRequest;
import static org.elasticsearch.client.Requests.pingSingleRequest;

/**
 *
 */
public class DiscoveryTransportClientTests extends AbstractNodesTests {

    private TransportClient client;

    @AfterMethod
    public void closeServers() {
        if (client != null) {
            client.close();
        }
        closeAllNodes();
    }

    /*@Test*/

    public void testWithDiscovery() throws Exception {
        startNode("server1");
        client = new TransportClient(ImmutableSettings.settingsBuilder()
                .put("cluster.name", "test-cluster-" + NetworkUtils.getLocalAddress().getHostName())
                .put("discovery.enabled", true)
                .build());
        // wait a bit so nodes will be discovered
        Thread.sleep(1000);
        client.admin().indices().create(createIndexRequest("test")).actionGet();
        Thread.sleep(500);

        client.admin().cluster().ping(pingSingleRequest("test").type("person").id("1")).actionGet();
        startNode("server2");
        Thread.sleep(1000);
        client.admin().cluster().ping(pingSingleRequest("test").type("person").id("1")).actionGet();
        closeNode("server1");
        Thread.sleep(10000);
        client.admin().cluster().ping(pingSingleRequest("test").type("person").id("1")).actionGet();
        closeNode("server2");
        client.admin().cluster().ping(pingSingleRequest("test").type("person").id("1")).actionGet();
    }

}
