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

import com.vmware.xenon.common.FactoryService;
import com.vmware.xenon.common.ServiceDocument;
import com.vmware.xenon.common.StatefulService;
import com.vmware.xenon.services.common.ServiceUriPaths;

public class DemoService extends StatefulService {

    public static final String FACTORY_LINK = ServiceUriPaths.CORE + "/demo";

    public static FactoryService createFactory() {
        return FactoryService.create(DemoService.class);
    }

    public static class State extends ServiceDocument {
        public String name;
    }

    public DemoService() {
        super(State.class);
        toggleOption(ServiceOption.PERSISTENCE, true);
    }
}
