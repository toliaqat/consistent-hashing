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

import com.vmware.xenon.services.common.NodeState;

import java.util.TreeMap;


public class ClosestNNeighbours extends TreeMap<Long, NodeState> {
    private static final long serialVersionUID = 0L;

    private final int maxN;

    public ClosestNNeighbours(int maxN) {
        super(Long::compare);
        this.maxN = maxN;
    }

    @Override
    public NodeState put(Long key, NodeState value) {
        if (size() < this.maxN) {
            return super.put(key, value);
        } else {
            // only attempt to write if new key can displace one of the top N entries
            if (comparator().compare(key, this.lastKey()) <= 0) {
                NodeState old = super.put(key, value);
                if (old == null) {
                    // sth. was added, remove last
                    this.remove(this.lastKey());
                }

                return old;
            }

            return null;
        }
    }
}