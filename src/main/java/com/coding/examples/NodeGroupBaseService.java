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
import com.vmware.xenon.services.common.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;

public class NodeGroupBaseService extends StatelessService {

    private NodeSelectorState cachedState;
    protected NodeGroupService.NodeGroupState cachedGroupState;

    @Override
    public void handleStart(Operation start) {
        NodeSelectorState state = null;
        if (!start.hasBody()) {
            state = new NodeSelectorState();
            state.nodeGroupLink = ServiceUriPaths.DEFAULT_NODE_GROUP;
        } else {
            state = start.getBody(NodeSelectorState.class);
        }

        state.documentSelfLink = getSelfLink();
        state.documentKind = Utils.buildKind(NodeSelectorState.class);
        state.documentOwner = getHost().getId();
        this.cachedState = state;
        startHelperServices(start);
    }

    private void startHelperServices(Operation op) {

        AtomicInteger remaining = new AtomicInteger(2);
        Operation.CompletionHandler h = (o, e) -> {

            this.log(Level.INFO, "startHelperServices handler called");

            if (e != null) {
                op.fail(e);
                return;
            }
            if (remaining.decrementAndGet() != 0) {
                return;
            }
            this.log(Level.INFO, "Service created");

            op.complete();
        };

        Operation subscribeToNodeGroup = Operation.createPost(
                UriUtils.buildSubscriptionUri(getHost(), this.cachedState.nodeGroupLink))
                .setCompletion(h)
                .setReferer(getUri());
        getHost().startSubscriptionService(subscribeToNodeGroup, handleNodeGroupNotification());

        // we subscribe to avoid GETs on node group state, per operation, but we need to have the initial
        // node group state, before service is available.
        sendRequest(Operation.createGet(this, this.cachedState.nodeGroupLink).setCompletion(
                (o, e) -> {
                    if (e == null) {
                        NodeGroupService.NodeGroupState ngs = o.getBody(NodeGroupService.NodeGroupState.class);
                        updateCachedNodeGroupState(ngs, null);
                    } else if (!getHost().isStopping()) {
                        logSevere(e);
                    }
                    h.handle(o, e);
                }));

    }

    private Consumer<Operation> handleNodeGroupNotification() {
        return (notifyOp) -> {
            this.log(Level.INFO, "Node group changed");
            notifyOp.complete();
            NodeGroupService.NodeGroupState ngs = null;
            if (notifyOp.getAction() == Action.PATCH) {
                NodeGroupService.UpdateQuorumRequest bd = notifyOp.getBody(NodeGroupService.UpdateQuorumRequest.class);
                if (NodeGroupService.UpdateQuorumRequest.KIND.equals(bd.kind)) {
                    updateCachedNodeGroupState(null, bd);
                    return;
                }
            } else if (notifyOp.getAction() != Action.POST) {
                return;
            }

            ngs = notifyOp.getBody(NodeGroupService.NodeGroupState.class);
            if (ngs.nodes == null || ngs.nodes.isEmpty()) {
                return;
            }
            updateCachedNodeGroupState(ngs, null);
        };
    }

    private void updateCachedNodeGroupState(NodeGroupService.NodeGroupState ngs, NodeGroupService.UpdateQuorumRequest quorumUpdate) {
        if (ngs != null) {
            NodeGroupService.NodeGroupState currentState = this.cachedGroupState;
            boolean isAvailable = NodeSelectorState.isAvailable(getHost(), ngs);
            boolean isCurrentlyAvailable = currentState != null
                    && NodeSelectorState.isAvailable(getHost(), currentState);
            boolean logMsg = isAvailable != isCurrentlyAvailable
                    || (currentState != null && currentState.nodes.size() != ngs.nodes.size());
            if (currentState != null && logMsg) {
                logInfo("Node count: %d, available: %s, update time: %d (%d)",
                        ngs.nodes.size(),
                        isAvailable,
                        ngs.membershipUpdateTimeMicros, ngs.localMembershipUpdateTimeMicros);
            }
        } else if (quorumUpdate.membershipQuorum != null) {
            logInfo("Quorum update: %d", quorumUpdate.membershipQuorum);
        }

        long now = Utils.getNowMicrosUtc();
        synchronized (this.cachedState) {
            this.cachedState.status = NodeSelectorState.Status.UNAVAILABLE;
            if (quorumUpdate != null) {
                this.cachedState.documentUpdateTimeMicros = now;
                if (quorumUpdate.membershipQuorum != null) {
                    this.cachedState.membershipQuorum = quorumUpdate.membershipQuorum;
                }
                if (this.cachedGroupState != null) {
                    if (quorumUpdate.membershipQuorum != null) {
                        this.cachedGroupState.nodes.get(
                                getHost().getId()).membershipQuorum = quorumUpdate.membershipQuorum;
                    }
                    if (quorumUpdate.locationQuorum != null) {
                        this.cachedGroupState.nodes.get(
                                getHost().getId()).locationQuorum = quorumUpdate.locationQuorum;
                    }
                }
                return;
            }

            if (this.cachedGroupState == null) {
                this.cachedGroupState = ngs;
            }

            if (this.cachedGroupState.documentUpdateTimeMicros <= ngs.documentUpdateTimeMicros) {
                NodeSelectorState.updateStatus(getHost(), ngs, this.cachedState);
                this.cachedState.documentUpdateTimeMicros = now;
                this.cachedState.membershipUpdateTimeMicros = ngs.membershipUpdateTimeMicros;
                this.cachedGroupState = ngs;
                // every time we update cached state, request convergence check
            } else {
                return;
            }
        }
    }
}
