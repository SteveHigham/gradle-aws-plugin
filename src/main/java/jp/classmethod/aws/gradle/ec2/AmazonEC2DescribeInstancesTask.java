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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

public class AmazonEC2DescribeInstancesTask extends ConventionTask {
	
	@Getter
	@Setter
	private List<String> instanceIds = new ArrayList<>();
	
	/**
	* For testing (stubbing)
	*/
	@Getter
	@Setter
	private AmazonEC2 client;
	
	@Getter
	private DescribeInstancesResult describeInstancesResult;
	
	@Getter
	private Map<String, Instance> instances = new HashMap<>();
	
	
	public AmazonEC2DescribeInstancesTask() {
		setDescription("Describe EC2 instances.");
		setGroup("AWS");
	}
	
	@TaskAction
	public void describeInstances() {
		// to enable conventionMappings feature
		
		List<String> instanceIds = getInstanceIds();
		
		AmazonEC2 ec2 = getClient();
		if (ec2 == null) {
			ec2 = createClient();
		}
		
		DescribeInstancesRequest request = new DescribeInstancesRequest()
			.withInstanceIds(instanceIds);
		
		describeInstancesResult = ec2.describeInstances(request);
		List<Reservation> reservations =
				describeInstancesResult.getReservations();
		for (Reservation res : reservations) {
			List<Instance> insts = res.getInstances();
			for (Instance i : insts) {
				String id = i.getInstanceId();
				assert !instances.containsKey(id);
				instances.put(id, i);
			}
		}
		getLogger().info("Describe EC2 instances requested: {}", instanceIds);
		
	}
	
	private AmazonEC2 createClient() {
		AmazonEC2PluginExtension ext = getProject().getExtensions().getByType(AmazonEC2PluginExtension.class);
		return ext.getClient();
	}
}
