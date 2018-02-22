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
package jp.classmethod.aws.gradle.s3;

import lombok.Getter;
import lombok.Setter;

import org.gradle.api.Project;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3Client;

import jp.classmethod.aws.gradle.common.BaseRegionAwarePluginExtension;

public class AmazonS3PluginExtension extends BaseRegionAwarePluginExtension<AmazonS3Client> {
	
	public static final String NAME = "s3";
	
	@Getter
	@Setter
	private Integer maxErrorRetry = -1;
	
	@Setter
	private boolean regionRequired = false;
	
	
	public AmazonS3PluginExtension(Project project) {
		super(project, AmazonS3Client.class);
	}
	
	/*
	public void Region(String region) {
		AwsPluginExtension aws = getProject().getExtensions().getByType(AwsPluginExtension.class);
		setRegion(aws.getActiveRegion(region));		
	}
	 */
	
	@Override
	protected ClientConfiguration buildClientConfiguration() {
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		if (maxErrorRetry > 0) {
			clientConfiguration.setMaxErrorRetry(maxErrorRetry);
		}
		
		return clientConfiguration;
	}
	
	@Override
	protected boolean isRegionRequired() {
		return regionRequired;
	}
}
