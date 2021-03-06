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
package org.xbib.elasticsearch.support;

import org.elasticsearch.client.Client;

/**
 * ClientIngest interface. Minimal API for node client ingesting.
 * Useful for river implementations.
 *
 * @author <a href="mailto:joergprante@gmail.com">J&ouml;rg Prante</a>
 */
public interface DocumentIngester {

    Client client();

    /**
     * Set the default index
     *
     * @param index the index
     * @return this TransportClientIndexer
     */
    DocumentIngester setIndex(String index);

    /**
     * Returns the default index
     *
     * @return the index
     */
    String getIndex();

    /**
     * Set the default type
     *
     * @param type the type
     * @return this TransportClientIndexer
     */
    DocumentIngester setType(String type);

    /**
     * Returns the default type
     *
     * @return the type
     */
    String getType();

    /**
     * Create document
     *
     * @param index
     * @param type
     * @param id
     * @param source
     * @return this ClientIngest
     */
    DocumentIngester createDocument(String index, String type, String id, String source);

    /**
     * Index document
     *
     * @param index
     * @param type
     * @param id
     * @param source
     * @return this ClientIngest
     */
    DocumentIngester indexDocument(String index, String type, String id, String source);

    /**
     * Delete document
     *
     * @param index
     * @param type
     * @param id
     * @return this ClientIngest
     */
    DocumentIngester deleteDocument(String index, String type, String id);

    /**
     * Ensure that all documents arrive.
     *
     * @return this ClientIngest
     */
    DocumentIngester flush();

    /**
     *
     * Shutdown this client
     */
    void shutdown();
}
