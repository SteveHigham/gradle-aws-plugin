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
package jp.classmethod.aws.gradle.ec2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.gradle.api.GradleException;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;

public class AmazonEC2WaitInstanceStatusTask extends ConventionTask { // NOPMD
	
	@Getter
	@Setter
	private String instanceId;
	
	@Getter
	@Setter
	private Collection<Filter> filters = new ArrayList<Filter>();
	
	@Getter
	@Setter
	private List<String> successStatuses = Arrays.asList(
			"running",
			"stopped",
			"terminated");
	
	@Getter
	@Setter
	private List<String> waitStatuses = Arrays.asList(
			"pending",
			"shutting-down",
			"stopping",
			"not found");
	
	@Getter
	@Setter
	private int loopTimeout = 900; // sec
	
	@Getter
	@Setter
	private int loopWait = 10; // sec
	
	@Getter
	private Instance instance;
	
	@Getter
	private String lastStatus;
	
	
	public AmazonEC2WaitInstanceStatusTask() {
		setDescription("Wait EC2 instance for specific status.");
		setGroup("AWS");
	}
	
	public void addFilter(String name, String value) {
		List<String> values = new ArrayList<String>();
		values.add(value);
		addFilter(name, values);
	}
	
	public void addFilter(String name, List<String> values) {
		filters.add(new Filter(name, values));
	}
	
	@TaskAction
	public void waitInstanceForStatus() { // NOPMD
		
		// to enable conventionMappings feature
		int loopTimeout = getLoopTimeout();
		int loopWait = getLoopWait();
		
		AmazonEC2PluginExtension ext = getProject().getExtensions().getByType(AmazonEC2PluginExtension.class);
		AmazonEC2 ec2 = ext.getClient();
		
		long start = System.currentTimeMillis();
		while (true) {
			if (System.currentTimeMillis() > start + (loopTimeout * 1000)) {
				throw new GradleException("Timeout");
			}
			checkCurrentStatus(ec2);
			if (instance != null) {
				break;
			}
			try {
				Thread.sleep(loopWait * 1000);
			} catch (InterruptedException e) {
				throw new GradleException("Sleep interrupted", e);
			}
		}
	}
	
	void checkCurrentStatus(AmazonEC2 ec2) {
		// to enable conventionMappings feature
		String instanceId = getInstanceId();
		Collection<Filter> filters = getFilters();
		List<String> successStatuses = getSuccessStatuses();
		List<String> waitStatuses = getWaitStatuses();
		
		try {
			DescribeInstancesResult dir = ec2.describeInstances(new DescribeInstancesRequest()
				.withInstanceIds(instanceId)
				.withFilters(filters));
			switch (dir.getReservations().size()) {
				case 0:
					if (instanceId != null) {
						throw new GradleException(instanceId + " does not exist");
					}
					break;
				case 1:
					instance = dir.getReservations().get(0).getInstances().get(0);
					lastStatus = instance.getState().getName();
					if (successStatuses.contains(lastStatus)) {
						getLogger().info("Status of instance {} is now {}.", instanceId, lastStatus);
						setInstanceId(instance.getInstanceId());
					} else if (waitStatuses.contains(lastStatus)) {
						getLogger().info("Status of instance {} is {}...", instanceId, lastStatus);
						instance = null;
					} else {
						// fail when current status is not waitStatuses or successStatuses
						throw new GradleException(
								"Status of instance is " + lastStatus + ".  It seems to be failed.");
					}
					break;
				default:
					throw new GradleException("Query returned more than one instance");
			}
		} catch (AmazonServiceException e) {
			if (instance == null) {
				throw new GradleException("Fail to describe instance: " + instanceId, e);
			}
		}
	}
	
}
