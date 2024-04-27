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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

import org.gradle.api.GradleException;
import org.gradle.api.file.EmptyFileVisitor;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.*;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import groovy.lang.Closure;

@Getter
public class SyncTask extends ConventionTask {
	
	private static String md5(File file) {
		try {
			return Files.hash(file, Hashing.md5()).toString();
		} catch (IOException e) {
			return "";
		}
	}
	
	
	@Setter
	@Input
	private String bucketName;
	
	@Setter
	@Input
	private String prefix = "";
	
	@Setter
	@InputFiles
	private File source;
	
	@Setter
	@Input
	private boolean delete;
	
	@Setter
	@Input
	private int threads = 5;
	
	@Setter
	@Input
	private StorageClass storageClass = StorageClass.Standard;
	
	@Setter
	@Input
	@Optional
	private Closure<ObjectMetadata> metadataProvider;
	
	@Internal
	private CannedAccessControlList acl;
	
	/**git status
	* Externally exposed client for stubbing
	*/
	@Setter
	@Input
	@Optional
	private AmazonS3 client;
	
	
	public void setAcl(String aclName) {
		acl = CannedAccessControlList.valueOf(aclName);
	}
	
	@TaskAction
	public void uploadAction() throws InterruptedException {
		// to enable conventionMappings feature
		String bucketName = getBucketName();
		String prefix = getPrefix();
		File source = getSource();
		AmazonS3 s3 = getClient();
		
		prefix = prefix.startsWith("/") ? prefix.substring(1) : prefix;
		checkUploadPrerequisites(bucketName, source);
		
		if (s3 == null) {
			if (!source.isDirectory()) {
				throw new GradleException("source must be directory");
			}
			
			AmazonS3PluginExtension ext = getProject().getExtensions().getByType(AmazonS3PluginExtension.class);
			s3 = ext.getClient();
			
			// Only upload if no client provided - ignore if stubbed out
			upload(s3, prefix);
			if (isDelete()) {
				deleteAbsent(s3, prefix);
			}
		}
	}
	
	private void checkUploadPrerequisites(String bucketName, File source) {
		if (bucketName == null) {
			throw new GradleException("bucketName is not specified");
		}
		if (source == null) {
			throw new GradleException("source is not specified");
		}
	}
	
	private void upload(AmazonS3 s3, String prefix) throws InterruptedException {
		// to enable conventionMappings feature
		String bucketName = getBucketName();
		File source = getSource();
		Closure<ObjectMetadata> metadataProvider = getMetadataProvider();
		CannedAccessControlList acl = getAcl();
		
		ExecutorService es = Executors.newFixedThreadPool(threads);
		getLogger().info("Start uploading");
		getLogger().info("Uploading... {} to s3://{}/{}", source, bucketName, prefix);
		getProject().fileTree(source).visit(new EmptyFileVisitor() {
			
			public void visitFile(FileVisitDetails element) {
				es.execute(
						new UploadTask(s3, element, bucketName, prefix, storageClass, acl, metadataProvider,
								getLogger()));
			}
		});
		
		es.shutdown();
		es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		getLogger().info("Finish uploading");
	}
	
	private void deleteAbsent(AmazonS3 s3, String prefix) {
		// to enable conventionMappings feature
		String bucketName = getBucketName();
		String pathPrefix = getNormalizedPathPrefix();
		
		s3.listObjects(bucketName, prefix).getObjectSummaries().forEach(os -> {
			File f = getProject().file(pathPrefix + os.getKey().substring(prefix.length()));
			if (!f.exists()) {
				getLogger().info("deleting... s3://{}/{}", bucketName, os.getKey());
				s3.deleteObject(bucketName, os.getKey());
			}
		});
	}
	
	private String getNormalizedPathPrefix() {
		String pathPrefix = getSource().toString();
		pathPrefix += pathPrefix.endsWith("/") ? "" : "/";
		return pathPrefix;
	}
	
	
	private static class UploadTask implements Runnable {
		
		private AmazonS3 s3;
		
		private FileVisitDetails element;
		
		private String bucketName;
		
		private String prefix;
		
		private Closure<ObjectMetadata> metadataProvider;
		
		private StorageClass storageClass;
		
		private CannedAccessControlList acl;
		
		private Logger logger;
		
		
		UploadTask(AmazonS3 s3, FileVisitDetails element, String bucketName, String prefix,
				StorageClass storageClass, CannedAccessControlList acl, Closure<ObjectMetadata> metadataProvider,
				Logger logger) {
			this.s3 = s3;
			this.element = element;
			this.bucketName = bucketName;
			this.prefix = prefix;
			this.storageClass = storageClass;
			this.acl = acl;
			this.metadataProvider = metadataProvider;
			this.logger = logger;
		}
		
		@Override
		public void run() {
			// to enable conventionMappings feature
			
			String relativePath = prefix + element.getRelativePath();
			String key = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
			
			boolean doUpload = false;
			try {
				ObjectMetadata metadata = s3.getObjectMetadata(bucketName, key);
				if (!metadata.getETag().equalsIgnoreCase(md5(element.getFile()))) {
					doUpload = true;
				}
			} catch (AmazonS3Exception e) {
				doUpload = true;
			}
			
			if (doUpload) {
				logger.info(" => s3://{}/{}", bucketName, key);
				s3.putObject(new PutObjectRequest(bucketName, key, element.getFile())
					.withStorageClass(storageClass)
					.withCannedAcl(acl)
					.withMetadata(metadataProvider == null ? null
							: metadataProvider.call(bucketName, key, element.getFile())));
			} else {
				logger.info(" => s3://{}/{} (SKIP)", bucketName, key);
			}
		}
	}
}
