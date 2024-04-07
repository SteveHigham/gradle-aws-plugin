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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import lombok.Getter;
import lombok.Setter;

import org.gradle.api.GradleException;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;

public class AmazonCloudFormationValidateTemplateFileTask extends ConventionTask {
	
	@Getter
	@Setter
	private File cfnTemplateFile;
	
	@Getter
	@Setter
	private String region;
	
	/**
	* For testing (stubbing)
	*/
	@Getter
	@Setter
	AmazonCloudFormation client;
	
	
	public AmazonCloudFormationValidateTemplateFileTask() {
		setDescription("Validate template file.");
		setGroup("AWS");
	}
	
	@TaskAction
	public void validateTemplateFile() throws InterruptedException, IOException {
		// to enable conventionMappings feature
		File cfnTemplateFile = getCfnTemplateFile();
		String region = getRegion();
		if (region != null) {
			region = region.trim();
		}
		AmazonCloudFormation client = getClient();
		if (client == null) {
			if (cfnTemplateFile == null || !cfnTemplateFile.exists()) {
				throw new GradleException("No cloudformation template defined");
			}
			
			AmazonCloudFormationPluginExtension ext =
					getProject().getExtensions().getByType(AmazonCloudFormationPluginExtension.class);
			if (region != null && region.length() > 0) {
				ext.setRegion(region);
			}
			
			if (!isValidTemplateFile(ext, cfnTemplateFile)) {
				throw new GradleException("cloudFormation template has invalid format");
			}
		}
	}
	
	private boolean isValidTemplateFile(AmazonCloudFormationPluginExtension ext, File templateFile) throws IOException {
		Charset chars = Charset.forName("UTF-8");
		String body = new String(Files.readAllBytes(templateFile.toPath()),
				chars);
		return ext.isValidTemplateBody(body);
	}
}

// End of file.
