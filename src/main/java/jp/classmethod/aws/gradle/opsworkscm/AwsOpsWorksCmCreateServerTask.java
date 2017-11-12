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
import org.gradle.api.tasks.TaskAction;

import com.amazonaws.services.opsworkscm.AWSOpsWorksCM;
import com.amazonaws.services.opsworkscm.model.CreateServerRequest;
import com.amazonaws.services.opsworkscm.model.CreateServerResult;
import com.amazonaws.services.opsworkscm.model.Server;

public class AwsOpsWorksCmCreateServerTask extends ConventionTask {
	
	/*
		@Getter
	@Setter
	private Boolean associatePublicIpAddress;
	 */
	@Getter
	@Setter
	private String backupId;
	
	@Getter
	@Setter
	private Integer backupRetentionCount;
	
	@Getter
	@Setter
	private Boolean disableAutomatedBackup = true;
	
	@Getter
	@Setter
	private String engine = "Chef";
	
	@Getter
	@Setter
	private String engineModel = "Single";
	
	@Getter
	@Setter
	private String engineVersion;
	
	@Getter
	@Setter
	private String instanceProfileArn;
	
	@Getter
	@Setter
	private String instanceType;
	
	@Getter
	@Setter
	private String keyPair;
	
	@Getter
	@Setter
	private String preferredBackupWindow;
	
	@Getter
	@Setter
	private String preferredMaintenanceWindow;
	
	@Getter
	@Setter
	private String serverName;
	
	@Getter
	@Setter
	private String serviceRoleArn;
	
	@Getter
	@Setter
	private Collection<String> subnetIds = new ArrayList<String>();
	
	@Getter
	private Server server;
	
	
	public AwsOpsWorksCmCreateServerTask() {
		setDescription("Create Chef server.");
		setGroup("AWS");
		//setAssociatePublicIpAddress(true);
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
	
	@TaskAction
	public void createServer() {
		
		AwsOpsWorksCmPluginExtension ext =
				getProject().getExtensions().getByType(AwsOpsWorksCmPluginExtension.class);
		AWSOpsWorksCM client = ext.getClient();
		// We call the getters to allow the convention task to work
		CreateServerRequest request = new CreateServerRequest()
			.withBackupId(getBackupId())
			.withBackupRetentionCount(getBackupRetentionCount())
			.withDisableAutomatedBackup(getDisableAutomatedBackup())
			.withEngine(getEngine())
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
}

// End of file.
