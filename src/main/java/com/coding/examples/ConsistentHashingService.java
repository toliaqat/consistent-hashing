/*
 * Copyright (c) 2016 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, without warranties or
 * conditions of any kind, EITHER EXPRESS OR IMPLIED.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.coding.examples;

import com.vmware.xenon.common.*;
import com.vmware.xenon.services.common.NodeState;

import java.util.Map;

public class ConsistentHashingService extends NodeGroupBaseService {
    public static final String SELF_LINK = "/core/consistent-hashing";


    @Override
    public void handleGet(Operation get) {
        Map<String,String> queryMap = UriUtils.parseUriQueryParams(get.getUri());
        String key = queryMap.get("key");
        selectNode(get, key);
    }

    private void selectNode(Operation op, String key) {

        int neighbourCount = 2;
        ClosestNNeighbours closestNodes = new ClosestNNeighbours(neighbourCount);

        long keyHash = FNVHash.compute(key);
        for (NodeState m : this.cachedGroupState.nodes.values()) {
            if (NodeState.isUnAvailable(m)) {
                continue;
            }

            long distance = FNVHash.compute(m.id) - keyHash;

            distance *= distance;
            // We assume first key (smallest) will be one with closest distance. The hashing
            // function can return negative numbers however, so a distance of zero (closest) will
            // not be the first key. Take the absolute value to cover that case and create a logical
            // ring
            distance = Math.abs(distance);

            closestNodes.put(distance, m);
        }

        NodeState closest = closestNodes.firstEntry().getValue();
        System.out.printf("C-Hashing - Key: %s, Host: %s\n", key, closest.id);
        op.setBody(closest.id);
        op.complete();
    }
}
