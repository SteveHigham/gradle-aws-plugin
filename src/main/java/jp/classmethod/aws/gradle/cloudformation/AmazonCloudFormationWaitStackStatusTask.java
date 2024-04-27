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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.gradle.api.GradleException;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;

public class AmazonCloudFormationWaitStackStatusTask extends ConventionTask {
	
	@Input
	@Getter
	@Setter
	private String stackName;
	
	@Input
	@Getter
	@Setter
	private List<String> successStatuses = Arrays.asList(
			"CREATE_COMPLETE",
			"UPDATE_COMPLETE",
			"DELETE_COMPLETE");
	
	@Input
	@Getter
	@Setter
	private List<String> waitStatuses = Arrays.asList(
			"CREATE_IN_PROGRESS",
			"ROLLBACK_IN_PROGRESS",
			"DELETE_IN_PROGRESS",
			"UPDATE_IN_PROGRESS",
			"UPDATE_COMPLETE_CLEANUP_IN_PROGRESS",
			"UPDATE_ROLLBACK_IN_PROGRESS",
			"UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS");
	
	@Getter
	@Setter
	@Internal
	private int loopTimeout = 900; // sec
	
	@Getter
	@Setter
	@Internal
	private int loopWait = 10; // sec
	
	@Getter
	@Internal
	private String lastStatus;
	
	private List<String> printedEvents;
	
	@Getter
	@Internal
	private Stack stack;
	
	/**
	* For testing (stubbing)
	*/
	@Getter
	@Setter
	@Input
	AmazonCloudFormation client;
	
	
	public AmazonCloudFormationWaitStackStatusTask() {
		setDescription("Wait cfn stack for specific status.");
		setGroup("AWS");
	}
	
	@TaskAction
	public void waitStackForStatus() throws InterruptedException {
		// to enable conventionMappings feature
		String stackName = getStackName();
		AmazonCloudFormation client = getClient();
		
		if (stackName == null && client == null) {
			throw new GradleException("stackName is not specified");
		}
		
		if (client == null) {
			AmazonCloudFormationPluginExtension ext =
					getProject().getExtensions()
						.getByType(AmazonCloudFormationPluginExtension.class);
			client = ext.getClient();
		}
		
		doWaitLoop(client);
	}
	
	void doWaitLoop(AmazonCloudFormation client) throws InterruptedException {
		int loopTimeout = getLoopTimeout();
		//String stackName = getStackName();
		List<String> successStatuses = getSuccessStatuses();
		long start = System.currentTimeMillis();
		printedEvents = new LinkedList<>();
		
		List<StackEvent> stackEvents = null;
		while (true) {
			if (System.currentTimeMillis() > start + (loopTimeout * 1000L)) {
				throw new GradleException("Timeout");
			}
			try {
				getStackInfo();
				stackEvents = getStackEventsInfo();
				
				// Always output new events; might be the last time you can
				//printEvents(stackEvents);
				
				if (!doWaitLoopBlock(successStatuses.contains(lastStatus))) {
					break;
				}
			} catch (AmazonCloudFormationException e) {
				handleLoopWaitException(e);
				break;
				/*
				if (e.getMessage().contains("does not exist")) {
					lastStatus = "DELETE_COMPLETE";
					//printEvents(stackEvents);
					//printOutputs(stack);
					break;
				} else {
					throw new GradleException("Unexpected exception for stack: " + stackName, e);
				}
				*/
			}
		}
		if (stackEvents != null) {
			printEvents(stackEvents);
		}
		if (stack != null) {
			printOutputs(stack);
		}
	}
	
	private void getStackInfo() {
		DescribeStacksRequest describeStackRequest =
				new DescribeStacksRequest().withStackName(stackName);
		DescribeStacksResult describeStackResult =
				client.describeStacks(describeStackRequest);
		// If stack doesn't exist we get an exception
		stack = describeStackResult.getStacks().get(0);
		lastStatus = stack.getStackStatus();
	}
	
	private List<StackEvent> getStackEventsInfo() {
		List<StackEvent> stackEvents = null;
		
		DescribeStackEventsRequest request =
				new DescribeStackEventsRequest().withStackName(stackName);
		// We generally get an exception here once the deletion has completed
		DescribeStackEventsResult result =
				client.describeStackEvents(request);
		stackEvents = new LinkedList<>(result.getStackEvents());
		Collections.reverse(stackEvents);
		return stackEvents;
	}
	
	private void handleLoopWaitException(AmazonCloudFormationException e) {
		if (e.getMessage().contains("does not exist")) {
			lastStatus = "DELETE_COMPLETE";
			//printEvents(stackEvents);
			//printOutputs(stack);
			//break;
		} else {
			throw new GradleException("Unexpected exception for stack: " + stackName, e);
		}
	}
	
	private void printEvents(List<StackEvent> stackEvents) {
		if (printedEvents.isEmpty()) {
			getLogger().info("==== Events ====");
		}
		stackEvents.stream()
			.forEach(o -> {
				String eventId = o.getEventId();
				// If we haven't printed the event, then print it and add to list of printed events so we won't print again
				if (!printedEvents.contains(eventId)) {
					getLogger().info("{} {} {}: {} {}",
							o.getTimestamp(),
							o.getResourceStatus(),
							o.getResourceType(),
							o.getLogicalResourceId(),
							(o.getResourceStatusReason() == null) ? "" : o.getResourceStatusReason());
					printedEvents.add(eventId);
				}
			});
	}
	
	private void printOutputs(Stack stack) {
		getLogger().info("==== Outputs ====");
		stack.getOutputs().stream()
			.forEach(o -> getLogger().info("{} ({}) = {}", o.getOutputKey(), o.getDescription(), o.getOutputValue()));
	}
	
	// returns false for break
	private boolean doWaitLoopBlock(boolean successContainsLast) throws InterruptedException {
		String stackName = getStackName();
		
		// If completed successfully, output status and outputs of
		// stack, then break out of while loop
		if (successContainsLast) {
			getLogger().debug("Status of stack {} is now {} - exiting loop.",
					stackName, lastStatus);
			//printOutputs(stack);
			return false;
			
			// Else if still going, sleep some then loop again
		} else if (waitStatuses.contains(lastStatus)) {
			getLogger().debug("Status of stack {} is {}...", stackName, lastStatus);
			Thread.sleep(loopWait * 1000L);
			
			// Else, it must have failed, so get out of while loop
		} else {
			throw new GradleException(
					"Status of stack " + stackName + " is " + lastStatus + ".  It seems to be failed.");
		}
		return true;
	}
}
