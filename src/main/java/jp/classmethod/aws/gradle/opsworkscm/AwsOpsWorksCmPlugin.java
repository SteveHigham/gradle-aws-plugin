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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import jp.classmethod.aws.gradle.AwsPlugin;

public class AwsOpsWorksCmPlugin implements Plugin<Project> {
	
	private static Logger logger =
			LoggerFactory.getLogger(AwsOpsWorksCmPlugin.class);
	
	
	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(AwsPlugin.class);
		project.getExtensions().create(AwsOpsWorksCmPluginExtension.NAME,
				AwsOpsWorksCmPluginExtension.class,
				project);
		applyTasks(project);
	}
	
	private void applyTasks(Project project) { // NOPMD
		logger.info("applyTasks");
		AwsOpsWorksCmPluginExtension ext =
				project.getExtensions().findByType(AwsOpsWorksCmPluginExtension.class);
		
		//AwsOpsWorksCmCreateServerTask awsOpsWorksCmCreateServerTask =
		project.getTasks().create("awsOpsWorksCmCreateServerTask", AwsOpsWorksCmCreateServerTask.class, task -> {
			logger.info("ConventionMapping serverName to: " + ext.getServerName());
			task.conventionMapping("serverName", () -> ext.getServerName());
			logger.info("ConventionMapping serviceRoleArn to: " + ext.getServiceRoleArn());
			task.conventionMapping("serviceRoleArn", () -> ext.getServiceRoleArn());
		});
	}
}
