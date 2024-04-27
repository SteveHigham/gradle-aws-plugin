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

import java.util.ArrayList;
import java.util.Collection;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import com.amazonaws.services.opsworkscm.AWSOpsWorksCM;
import com.amazonaws.services.opsworkscm.model.CreateServerRequest;
import com.amazonaws.services.opsworkscm.model.CreateServerResult;
import com.amazonaws.services.opsworkscm.model.EngineAttribute;
import com.amazonaws.services.opsworkscm.model.Server;

public class AwsOpsWorksCmCreateServerTask extends ConventionTask {
	
	/*
		@Getter
	@Setter
	private Boolean associatePublicIpAddress;
	*/
	
	@Getter
	@Setter
	@Input
	@Optional
	private String backupId;
	
	@Getter
	@Setter
	@Input
	@Optional
	private Integer backupRetentionCount;
	
	@Getter
	@Setter
	@Input
	private Boolean disableAutomatedBackup = true;
	
	@Getter
	@Setter
	@Input
	private String engine = "Chef";
	
	@Getter
	@Setter
	@Input
	private Collection<EngineAttribute> engineAttributes =
			new ArrayList<>();
	
	@Getter
	@Setter
	@Input
	private String engineModel = "Single";
	
	@Getter
	@Setter
	@Input
	@Optional
	private String engineVersion;
	
	@Getter
	@Setter
	@Input
	@Optional
	private String instanceProfileArn;
	
	@Getter
	@Setter
	@Input
	@Optional
	private String instanceType;
	
	@Getter
	@Setter
	@Input
	@Optional
	private String keyPair;
	
	@Getter
	@Setter
	@Internal
	private String preferredBackupWindow;
	
	@Getter
	@Setter
	@Internal
	private String preferredMaintenanceWindow;
	
	@Getter
	@Setter
	@Input
	@Optional
	private String serverName;
	
	@Getter
	@Setter
	@Input
	@Optional
	private String serviceRoleArn;
	
	@Getter
	@Setter
	@Input
	private Collection<String> subnetIds = new ArrayList<>();
	
	/**
	* For testing (stubbing)
	*/
	@Getter
	@Setter
	@Input
	@Optional
	private AWSOpsWorksCM client;
	
	@Getter
	@Internal
	private Server server;
	
	
	public AwsOpsWorksCmCreateServerTask() {
		setDescription("Create Chef server.");
		setGroup("AWS");
	}
	
	public void addSubnetId(String id) {
		subnetIds.add(id);
	}
	
	public void setBackupWindow(String window) {
		if (StringUtils.isBlank(window)) {
			disableAutomatedBackup = true;
			preferredBackupWindow = null;
		} else {
			disableAutomatedBackup = false;
			preferredBackupWindow = window;
		}
	}
	
	public void addEngineAttribute(String name, String value) {
		EngineAttribute attr = new EngineAttribute()
			.withName(name)
			.withValue(value);
		engineAttributes.add(attr);
	}
	
	@TaskAction
	public void createServer() {
		
		AWSOpsWorksCM client = getClient();
		if (client == null) {
			client = createClient();
		}
		// We call the getters to allow the convention task to work
		CreateServerRequest request = new CreateServerRequest()
			.withBackupId(getBackupId())
			.withBackupRetentionCount(getBackupRetentionCount())
			.withDisableAutomatedBackup(getDisableAutomatedBackup())
			.withEngine(getEngine())
			.withEngineAttributes(getEngineAttributes())
			.withEngineModel(getEngineModel())
			.withEngineVersion(getEngineVersion())
			.withInstanceProfileArn(getInstanceProfileArn())
			.withInstanceType(getInstanceType())
			.withKeyPair(getKeyPair())
			.withPreferredBackupWindow(getPreferredBackupWindow())
			.withPreferredMaintenanceWindow(getPreferredMaintenanceWindow())
			.withServerName(getServerName())
			.withServiceRoleArn(getServiceRoleArn())
			.withSubnetIds(getSubnetIds());
		CreateServerResult result = client.createServer(request);
		server = result.getServer();
	}
	
	private AWSOpsWorksCM createClient() {
		AwsOpsWorksCmPluginExtension ext =
				getProject().getExtensions()
					.getByType(AwsOpsWorksCmPluginExtension.class);
		return ext.getClient();
	}
}

// End of file.
