/*
 * (C) Copyright 2012, IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.jaggr.blueprint;

import com.ibm.jaggr.service.impl.AggregatorCommandProvider;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

import java.util.Arrays;

@Command(scope = AggregatorCommandProvider.EYECATCHER, name = AggregatorCommandProvider.CMD_SHOWCONFIG)
public class ShowConfigShellCommand extends AbstractOsgiCommandSupport {

	@Argument(index = 0, name = "servlet", required = true, multiValued = false)
    String servlet = null;

	@Override
	protected void exec(AggregatorCommandProvider provider) throws Exception {
		provider._aggregator(new CommandInterpreterWrapper(Arrays.asList(AggregatorCommandProvider.CMD_SHOWCONFIG, servlet)));
	}
}
