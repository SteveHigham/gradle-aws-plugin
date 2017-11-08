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

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.gradle.api.Project;

import com.amazonaws.services.opsworkscm.AWSOpsWorksCMClient;

import jp.classmethod.aws.gradle.common.BaseRegionAwarePluginExtension;

public class AwsOpsWorksCmPluginExtension extends BaseRegionAwarePluginExtension<AWSOpsWorksCMClient> {
	
	private static Logger logger = LoggerFactory.getLogger(AwsOpsWorksCmPluginExtension.class);
	
	public static final String NAME = "opsworkscm";
	
	@Getter
	@Setter
	private String serverName;
	
	@Getter
	@Setter
	private String serviceRoleArn;
	
	
	public AwsOpsWorksCmPluginExtension(Project project) {
		super(project, AWSOpsWorksCMClient.class);
	}
}
