/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.accumulo.test.functional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.accumulo.core.cli.BatchWriterOpts;
import org.apache.accumulo.core.cli.ScannerOpts;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.ClientConfiguration.ClientProperty;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.harness.AccumuloClusterIT;
import org.apache.accumulo.test.TestIngest;
import org.apache.accumulo.test.VerifyIngest;
import org.junit.Test;

public class WriteLotsIT extends AccumuloClusterIT {

  @Override
  protected int defaultTimeoutSeconds() {
    return 90;
  }

  @Test
  public void writeLots() throws Exception {
    final Connector c = getConnector();
    final String tableName = getUniqueNames(1)[0];
    c.tableOperations().create(tableName);
    final AtomicReference<Exception> ref = new AtomicReference<>();
    List<Thread> threads = new ArrayList<>();
    final ClientConfiguration clientConfig = getCluster().getClientConfig();
    for (int i = 0; i < 10; i++) {
      final int index = i;
      Thread t = new Thread() {
        @Override
        public void run() {
          try {
            TestIngest.Opts opts = new TestIngest.Opts();
            opts.startRow = index * 10000;
            opts.rows = 10000;
            opts.setTableName(tableName);
            if (clientConfig.getBoolean(ClientProperty.INSTANCE_RPC_SASL_ENABLED.getKey(), false)) {
              opts.updateKerberosCredentials(clientConfig);
            } else {
              opts.setPrincipal(getAdminPrincipal());
            }
            TestIngest.ingest(c, opts, new BatchWriterOpts());
          } catch (Exception ex) {
            ref.set(ex);
          }
        }
      };
      t.start();
      threads.add(t);
    }
    for (Thread thread : threads) {
      thread.join();
    }
    if (ref.get() != null) {
      throw ref.get();
    }
    VerifyIngest.Opts vopts = new VerifyIngest.Opts();
    vopts.rows = 10000 * 10;
    vopts.setTableName(tableName);
    if (clientConfig.getBoolean(ClientProperty.INSTANCE_RPC_SASL_ENABLED.getKey(), false)) {
      vopts.updateKerberosCredentials(clientConfig);
    } else {
      vopts.setPrincipal(getAdminPrincipal());
    }
    VerifyIngest.verifyIngest(c, vopts, new ScannerOpts());
  }

}
