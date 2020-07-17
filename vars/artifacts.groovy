// ################################################################
// ################################################################
// ############ All Methods Must Be Called Inside Node ############
// ################################################################
// ################################################################
import java.util.List
import com.ohadc.common.utils.ListUtils

void inform(boolean verbose, String message)
{
	if(verbose)
	{
		echo "${message}"
	}
}

/**
 * Gets the artifacts dir as java.io.File
 * to get the path, call the result with .getAbsolutePath()
 * @return the artifacts dir as File
 */
File getArtifactsDir(boolean verbose = true)
{
	File artifactsDir = null;
	try
	{
		// getArtifactsDir returns a java.io.File object
		artifactsDir = manager.build.getArtifactManager().getArtifactsDir();
	}
	catch(err)
	{
		echo "Error: Failed to get artifacts directory";
	}

	inform(verbose, "Archive directory: ${artifactsDir}");

	return artifactsDir;
}

String createArchiveDir(verbose = false)
{
	String archivePath = null;
	try
	{
		File archiveDir = getArtifactsDir();
		archivePath = archiveDir.getAbsolutePath();
		def filePath = new hudson.FilePath(archiveDir);

		if(!filePath.exists())
		{
			filePath.mkdirs();
			inform(verbose, "Directory \"${archivePath}\" Created!");
		}
	}
	catch(err)
	{
		echo "createArchiveDir Failed: ${err.dump()}"
	}

	return archivePath;
}

/**
 * Reads a file from archive using its partial name.
 *
 * @param partialName the partial name of the file
 * @return the content of the file, null if encountered some error
 */
List<String> readFile(String partialName)
{
	List<String> content = null;

	try
	{
		String fileFullPath = getFile(partialName);
		if(fileFullPath != null)
		{
			File file = new File(fileFullPath);
			content = file.readLines();
		}
	}
	catch(err)
	{
		inform(true, err.message);
	}

	return content;
}

/**
 * Write file directly to archive.
 *
 * @param relativePath the relative path to the archive folder
 * @param content the content
 */
void writeFile(String relativePath, String content, boolean verbose = true)
{
	File archivedFile = new File(getArtifactsDir(false).getAbsolutePath(), relativePath);
	if(!archivedFile.getParentFile().exists())
	{
		archivedFile.getParentFile().mkdirs();
	}
	archivedFile.write(content);
	inform(verbose, "File ${relativePath} created in archive");
}

/**
 * Get files from artifacts of current build.
 *
 * @param filter - A ant style mask to select desired files, if null, returns all files, 
 * @return the files requested as list of hudson.FilePath
 */
List<hudson.FilePath> getFiles(String filter = null, String directory = null)
{
	List<hudson.FilePath> files = [];

	try
	{
		// Returns a List<Run.Artifact>
		def buildArtifacts = manager.build.getArtifacts();
		String artifactsDir = getArtifactsDir().getAbsolutePath();

		for(art in buildArtifacts)
		{
			if(filter != null)
			{
				List<String> filters = filter.replace(' ', '').replace('*', '').split(',');
				boolean match = filters.any { art.relativePath.endsWith(it); }
				if(match && (directory == null || art.relativePath.contains(directory)))
				{
					files << new hudson.FilePath(new File(artifactsDir, art.relativePath));
				}
			}
			else
			{
				if(directory == null || art.relativePath.contains(directory))
				{
					files << new hudson.FilePath(new File(artifactsDir, art.relativePath));
				}
			}
		}
	}
	catch(err)
	{
		echo "artifacts.getFiles Error: ${err.dump()}"
	}

	ListUtils.sortFilePathABC(files);

	return files;
}

/**
 * Gets a file from artifacts by partial name.
 * 
 * NOTE: Works on the CURRENT build artifacts
 *
 * @param partialName the partial name
 * @return the file
 */
String getFile(String partialName)
{
	String archivePath = getArtifactsDir(false).getAbsolutePath();
	String requestedFile = null;
	def files = [];

	if(archivePath != null && archivePath != "")
	{
		def artifacts = manager.build.getArtifacts();
		if(artifacts != null)
		{
			def artifactsSize = artifacts.size();

			for(def i = 0; i < artifactsSize; i++)
			{
				if(artifacts[i].relativePath.toLowerCase().contains(partialName.toLowerCase()))
				{
					requestedFile = new File(archivePath, "${artifacts[i].relativePath}").getAbsolutePath();
					break;
				}
			}
		}
	}

	return requestedFile;
}


/**
 * Copy all artifacts from another job single build.
 *
 * @param jobFullName the job full name
 * @param buildNumber the build number
 * @return true, if copy successful
 */
boolean copyArtifactsFrom(String jobFullName, int buildNumber, boolean verbose = true)
{
	def jenkins = Jenkins.getInstance();
	def jobInstance = jenkins.getItemByFullName(jobFullName);
	def build = jobInstance.getBuildByNumber(buildNumber);

	String otherBuildArtifactsDir = build.getArtifactManager().getArtifactsDir();
	String myArtifactDir = manager.build.getArtifactManager().getArtifactsDir();

	def otherBuildArtDir = new File(otherBuildArtifactsDir);

	if(otherBuildArtDir.exists())
	{
		inform(verbose, "Collecting artifacts from ${jobFullName} build #${buildNumber}");

		copy(otherBuildArtifactsDir, myArtifactDir, null, verbose);
		return true;
	}
	else
	{
		inform(verbose, "Job ${jobFullName} build #${buildNumber} has no artifacts");
		return false;
	}
}

/**
 * Copy artifacts from another job multiple builds.
 *
 * @param jobFullName the job full name
 * @param buildCountFromLast the build count from last
 */
void copyArtifactsFromBuilds(String jobFullName, int buildCountFromLast)
{
	def jenkins = Jenkins.getInstance();
	def jobInstance = jenkins.getItemByFullName(jobFullName);

	int lastBuildNumber = jobInstance.getLastBuild().number;
	int startFromBuildNumber = lastBuildNumber - buildCountFromLast + 1;

	List<Boolean> copyStatus = [];

	echo "Copying archives from ${jobFullName} builds [${startFromBuildNumber}-${lastBuildNumber}]"
	for(int i = startFromBuildNumber; i <= lastBuildNumber; i++)
	{
		copyStatus << copyArtifactsFrom(jobFullName, i, true);
	}

	int successfulCopy = copyStatus.findAll { it == true }.size()
	echo "Collected ${successfulCopy} builds archives"
}

/**
 * 
 * @param itemFullName
 * @param fileSuffix
 * @param yesterdayTime
 * @param recursive
 * @param excludeContains Comma separated values to check item full name contains
 * @return
 */
def copyArtifactsFromLastDay(String itemFullName, String fileSuffix, String yesterdayTime = "21:00:00", boolean recursive = false, String excludeContains = null)
{
	def jenkins = Jenkins.getInstance();
	Map<String, List<String>> resultsMap = [:];

	def items = [];

	if(itemFullName == null || itemFullName.trim() == "")
	{
		// we dont have any item requested, get all
		items = jenkins.getAllItems();
	}
	else
	{
		// get requested item by full name
		items = jenkins.getItemByFullName(itemFullName);

		if(items instanceof com.cloudbees.hudson.plugins.folder.Folder)
		{
			// if its a folder, get its content
			items = items.items;
		}
		else
		{
			// if its a job/workflow/pipeline wrap it in array
			items = [items];
		}
	}

	if(items.size() > 0)
	{
		def yesterdayStr = new Date().previous().format('yyyyMMdd');
		def yesterdayAt21 = Date.parse("yyyyMMdd hh:mm:ss", "${yesterdayStr} ${yesterdayTime}");
		String archivePath = createArchiveDir();
		items.each({ item ->
			if(!(item instanceof com.cloudbees.hudson.plugins.folder.Folder))
			{
				boolean addToDaily = true;
				echo "Exclude: ${excludeContains}"
				if(excludeContains != null)
				{
					def excList = excludeContains.split(",");
					for(String ex in excList)
					{
						echo "Checking \"${ex}\" with item \"${item.fullName}\""
						if(item.fullName.contains(ex))
						{
							addToDaily = false;
							echo "item \"${item.fullName}\" will not be in daily"
							break;
						}
					}
				}
				if(addToDaily) {
					def lastBuild = item.getLastBuild();
					def relevantBuild = null;
					if(lastBuild != null) // we have a build
					{
						def tempBuild = lastBuild;
						while(relevantBuild == null)
						{
							// relevant time
							if(tempBuild.getTime() > yesterdayAt21)
							{
								// it completed
								if(tempBuild.result != null)
								{
									relevantBuild = tempBuild;
								}
								else
								{
									if(tempBuild.number == 1)
									{
										break;
									}
									tempBuild = item.getBuildByNumber(tempBuild.number - 1)
								}
							}
							else
							{
								break;
							}
						}
					}

					if(relevantBuild != null)
					{
						// this build started after yesterdayTime yesterday and completed
						echo "Job: ${item.fullName} started after ${yesterdayTime} yesteday (${relevantBuild.getTime()} > ${yesterdayAt21} [${relevantBuild.number}])"

						def arts = relevantBuild.getArtifacts();
						def buildArchivePath = relevantBuild.getArtifactManager().getArtifactsDir();
						for(def file : arts)
						{
							if(file.relativePath.contains(fileSuffix))
							{
								String fileName = new File(file.relativePath).getName();
								String sourcePath = "${buildArchivePath}\\${file.relativePath}";
								String targetPath = new File(archivePath, "${item.name}_${fileName}").getAbsolutePath()
								copy(sourcePath, targetPath);
								if(!resultsMap.containsKey(item.fullName)){
									resultsMap[item.fullName] = [];
								}
								resultsMap[item.fullName] << "${item.name}_${fileName}";
							}
						}
					}
				}
			}
			else if(recursive)
			{
				def childMap = copyArtifactsFromLastDay(item.fullName, fileSuffix, yesterdayTime, recursive, excludeContains);
				resultsMap << childMap;
			}
		});
	}
	return resultsMap;
}

/**
 * Copies the files that match the given filter to the specified target node.
 * 
 * filter - Ant GLOB pattern. 
 * 		String like "foo/bar/*.xml" Multiple patterns can be separated by ',', and whitespace can surround ',' 
 * 			(so that you can write "abc, def" and "abc,def" to mean the same thing. 
 * 		if filter is null - copy will perform on all files
 * filter example: '*.xml, inner/*.xml')
 *
 * @param source the source dir
 * @param destination the path of the folder to copy to
 * @return the number of files copied
 */
int copy(String source, String destination, String filter = null, boolean verbose = true)
{
	int copied = 0;
	try
	{
		def sourceFilePath = new hudson.FilePath(new File(source));

		if(sourceFilePath.exists())
		{
			def targetFilePath = new hudson.FilePath(new File(destination));

			if(sourceFilePath.isDirectory())
			{
				// if its not empty, copy
				if(sourceFilePath.list().size() > 0)
				{
					copied = filter == null ? sourceFilePath.copyRecursiveTo(targetFilePath) : sourceFilePath.copyRecursiveTo(filter, targetFilePath);
				}
				else
				{
					targetFilePath.mkdirs();
					copied = 1;
				}
			}
			else
			{
				if(targetFilePath.isDirectory())
				{
					def	targetFile = agents.getRFP(new File(targetFilePath.getRemote(), sourceFilePath.getName()).getAbsolutePath());
					sourceFilePath.copyTo(targetFile);
				}
				else
				{
					sourceFilePath.copyTo(targetFilePath);
				}
				copied = 1;
			}

			inform(verbose,"Copied ${source} to ${destination}");
		}
		else
		{
			throw new Exception("Copy Error: file \"${source}\" does not exist");
		}
	}
	catch(err)
	{
		echo "copyFiles Failed: ${err.message}"
	}

	return copied;
}

void copyToSlave(String copyTo = env.WORKSPACE, String directory = null)
{
	// Get all artifacts as list of hudson.FilePath
	def files = getFiles();

	// Get the absolute path to the artifacts in master
	String artDir = getArtifactsDir(false).getAbsolutePath();

	Map successCopyMap = [:];
	def failedMessages = [];

	String pathDelim = agents.getPathDelim();

	// Iterate all files
	for(def f in files)
	{
		try
		{
			// Create a hudson.FilePath from every java.io.File from the master as the source
			def sourceFilePath = f;

			// Get the relative path of the file from archive
			String relativePath = f.getRemote().replace(artDir, '');

			if(directory != null && !relativePath.contains(directory))
			{
				continue;
			}

			// Create and fix the path on the agent
			def pathInAgent = agents.fixPathDelim("${copyTo}${relativePath}", pathDelim);

			// Create a hudson.FilePath from the constructed path
			def destinationPath = agents.getRFP(pathInAgent);

			// Use hudson.FilePath to perform a copy from these two channels
			sourceFilePath.copyTo(destinationPath);

			successCopyMap << ["${sourceFilePath.getRemote()}": "${destinationPath.getRemote()}"]
		}
		catch(Exception err)
		{
			failedMessages << "${err.getMessage()} ${err.dump()}"
		}
	}

	String mapString = "";
	successCopyMap.each { k,v ->
		def from = k;
		def to = v;
		mapString = """${mapString}${from} 
	-> ${to}
"""
	}

	//	successCopyMap.each { k,v ->
	//		def from = k.replace(artDir, '').substring(1);
	//		def to = v.replace(copyTo, '').substring(1);
	//		mapString = """${mapString}${from}
	//	-> ${to}
	//"""
	//	}

	// Make a summary text
	echo """Copied ${successCopyMap.size()} items from artifacts to ${copyTo}, 
${mapString}
Failures: ${failedMessages}
""";

}

/**
 * Archives the files that accepts the mask.
 */
void archive(def mask = "**")
{
	try
	{
		echo "Archiving from ${pwd()}, mask: ${mask}"
		archiveArtifacts allowEmptyArchive: true, artifacts: mask
	}
	catch(err)
	{
		echo "Archive(${mask}) failed, ${err.dump()}"
	}
}

/**
 * Gets the link for an archived file.
 *
 * @param fileRelativePath the file relative path
 * @return the link
 */
public String getLink(String fileRelativePath, boolean verbose = true)
{
	// Returns a List<Run.Artifact>
	List artifacts = manager.build.getArtifacts();

	// get the artifact dir without telling the build
	File artifactsDir = getArtifactsDir(false);

	if(fileRelativePath.contains(artifactsDir.getAbsolutePath()))
	{
		// remove the artifacts dir path if exists
		fileRelativePath = fileRelativePath.replace(artifactsDir.getAbsolutePath(), "")
	}

	def candidates = [];
	def selected = null;

	for(def artifact : artifacts)
	{
		if(artifact.relativePath == fileRelativePath)
		{
			if(verbose)
			{
				echo """file: ${artifact}
				dump: ${artifact.dump()}"""
			}
			selected = artifact;
			break;
		}
		else if(artifact.name == fileRelativePath)
		{
			candidates << artifact
		}
	}

	if(selected == null)
	{
		if(candidates.size() == 0)
		{
			echo "Couldn't find an artifact that match \"${fileRelativePath}\""
		}
		else
		{
			if(candidates.size() > 1)
			{
				echo "found ${candidates.size()} possible matches, using first"
			}
			selected =  candidates.first();
		}
	}

	if(selected != null)
	{
		String artifactPH = 'artifact';
		return "${env.BUILD_URL}${artifactPH}/${selected.getHref()}";
	}
}

Map<String, String> linkMap(String directory = null)
{
	Map<String, String> linksMap = [:]
	// Returns a List<Run.Artifact>
	List artifacts = manager.build.getArtifacts();
	String artifactPH = 'artifact';

	if(directory != null)
	{
		directory = directory.toLowerCase();
	}

	for(def art in artifacts)
	{
		if(directory == null || art.relativePath.toLowerCase().contains(directory))
		{
			linksMap.put(art.relativePath, "${env.BUILD_URL}${artifactPH}/${art.getHref()}")
		}
	}

	return linksMap;
}

