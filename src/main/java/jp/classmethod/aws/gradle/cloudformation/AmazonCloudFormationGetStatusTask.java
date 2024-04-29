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
package jp.classmethod.aws.gradle.cloudformation;

import lombok.Getter;
import lombok.Setter;

import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;

public class AmazonCloudFormationGetStatusTask extends ConventionTask {
	
	@Getter
	@Setter
	@Input
	private String cfnStackName;
	
	@Getter
	@Internal
	private DescribeStacksResult statusResult;
	
	/**
	* For testing (stubbing)
	*/
	@Getter
	@Setter
	@Input
	@Optional
	AmazonCloudFormation client;
	
	
	public AmazonCloudFormationGetStatusTask() {
		setDescription("Get stack status.");
		setGroup("AWS");
	}
	
	@TaskAction
	public void getStatus() {
		
		String cfnStackName = getCfnStackName();
		if (client == null) {
			AmazonCloudFormationPluginExtension ext =
					getProject().getExtensions().getByType(AmazonCloudFormationPluginExtension.class);
			client = ext.getClient();
		}
		DescribeStacksRequest request = new DescribeStacksRequest().withStackName(cfnStackName);
		statusResult = client.describeStacks(request);
	}
}

// End of file.
