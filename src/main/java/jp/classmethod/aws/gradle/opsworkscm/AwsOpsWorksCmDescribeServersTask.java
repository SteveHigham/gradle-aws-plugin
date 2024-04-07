/*
* Copyright 2015-2016 the original author or authors.
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
package jp.classmethod.aws.gradle.opsworkscm;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;

import com.amazonaws.services.opsworkscm.AWSOpsWorksCM;
import com.amazonaws.services.opsworkscm.model.DescribeServersRequest;
import com.amazonaws.services.opsworkscm.model.DescribeServersResult;
import com.amazonaws.services.opsworkscm.model.Server;

public class AwsOpsWorksCmDescribeServersTask extends ConventionTask {
	
	@Getter
	@Setter
	private String serverName;
	
	/**
	* For testing (stubbing)
	*/
	@Getter
	@Setter
	private AWSOpsWorksCM client;
	
	@Getter
	private DescribeServersResult result;
	
	
	public Server getServer(String name) {
		List<Server> servers = result.getServers();
		for (Server i : servers) {
			if (i.getServerName().equals(name)) {
				return i;
			}
		}
		return null;
	}
	
	@TaskAction
	public void describeServers() {
		
		AWSOpsWorksCM client = getClient();
		if (client == null) {
			client = createClient();
		}
		
		// We call the getters to allow the convention task to work
		DescribeServersRequest request = new DescribeServersRequest()
			.withServerName(getServerName());
		result = client.describeServers(request);
	}
	
	private AWSOpsWorksCM createClient() {
		AwsOpsWorksCmPluginExtension ext =
				getProject().getExtensions()
					.getByType(AwsOpsWorksCmPluginExtension.class);
		return ext.getClient();
	}
}

// End of file.
