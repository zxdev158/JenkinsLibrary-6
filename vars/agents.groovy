import com.ohadc.common.models.ExecuteResult;
import com.ohadc.common.utils.Paths;
import com.ohadc.common.utils.ListUtils;

primaryWindowsNetworkPath = "";
primaryLinuxNetworkPath = "";

void inform(boolean verbose, String message)
{
	if(verbose)
	{
		echo "${message}"
	}
}

boolean unix()
{
	if(env.NODE_NAME == null)
	{
		error "env.NODE_NAME is not set"
	}
	else if(env.NODE_NAME.equals(Jenkins.instance.toComputer().getDisplayName()))
	{
		return Jenkins.instance.toComputer().isUnix();
	}
	else
	{
		def node = Jenkins.instance.getNode(env.NODE_NAME)
		def comp = node.getComputer();
		return comp.isUnix();
	}
}

String getPathDelim()
{
	return unix() ? "/" : "\\";
}

String join(String base, String ext)
{
	String joined = null;
	if(base != null && ext != null)
	{
		String pathDelim = getPathDelim();
		joined = Paths.buildPath([base, ext], pathDelim);
	}
	else
	{
		joined = base == null ? ext : base;
	}

	return joined;
}

/**
 * Gets the remote file path for the given path under current node
 * @param path the full path of the requested file
 * @return hudson.FilePath for the given file
 */
public getRFP(String path)
{
	if (env.NODE_NAME == null)
	{
		error "envvar NODE_NAME is not set, probably not inside an node {} or running an older version of Jenkins!";
	}
	else if (env.NODE_NAME.equals("master"))
	{
		return new hudson.FilePath(new File(path));
	}
	else
	{
		return new hudson.FilePath(Jenkins.getInstance().getComputer(env.NODE_NAME).getChannel(), path);
	}
}


String fixNetworkFile(String originPath)
{
	boolean unix = unix();
	if(unix && originPath.contains(primaryWindowsNetworkPath))
	{
		// we are in Unix slave and got windows path
		originPath = originPath.replace(primaryWindowsNetworkPath, primaryLinuxNetworkPath);
	}
	else if(!unix && originPath.contains(primaryLinuxNetworkPath))
	{
		// we are in windows slave and got Unix path
		originPath = originPath.replace(primaryLinuxNetworkPath, primaryWindowsNetworkPath);
	}

	// Either way - fix the path delimiters
	return fixPathDelim(originPath);
}

String fixPathDelim(String originPath, String pathDelim = null)
{
	pathDelim = pathDelim == null ? getPathDelim() : pathDelim;
	String nonPathDelim = pathDelim == "/" ? "\\" : "/";

	return originPath.replace(nonPathDelim, pathDelim);
}

String readFile(String path)
{
	String content = null;
	def filePath = getRFP(path);

	if(filePath.exists())
	{
		if(!filePath.isDirectory())
		{
			content = filePath.readToString();
		}
		else
		{
			echo "Read Error: File ${path} is a directory"
		}
	}
	else
	{
		echo "Read Error: File ${path} does not exist"
	}

	return content;
}

/**
 * Write file.
 *
 * @param path the path to the file
 * @param content - the content to write
 * @param encoding - Null to use the platform default encoding.
 * @return the string
 */
String writeFile(String path, String content, String encoding=null, boolean append=true, int mode = -1, boolean verbose = true, long timeMili=-1)
{
	def filePath = getRFP(path);

	if(timeMili == -1)
	{
		timeMili = System.currentTimeMillis();
	}

	if(!filePath.exists())
	{
		filePath.getParent().mkdirs();
		filePath.touch(timeMili);
	}

	if(append)
	{
		content = filePath.readToString() + content;
	}


	filePath.write(content, encoding);

	if(mode > -1)
	{
		filePath.chmod(mode);
	}

	inform(verbose, "Wrote a file at: ${filePath.getRemote()}");

	return filePath.getRemote();
}

/**
 * Checks if filePath is directory.
 *
 * @param filePath the file path
 * @return true, if is directory
 */
boolean isDirectory(String filePath)
{
	def remotePath = getRFP(filePath);
	return remotePath.isDirectory();
}

boolean isFile(String path)
{
	def remotePath = getRFP(path);
	return remotePath.exists() && !remotePath.isDirectory();
}

String resolveNode(String nodeName)
{
	def jenkins = Jenkins.instance;
	def computers = jenkins.computers;
	String name = null;
	if(nodeName.toLowerCase() == "master")
	{
		name = "master";
	}
	else
	{
		for(def computer in computers)
		{
			if(computer.nodeName != null && computer.nodeName.toLowerCase() == nodeName.toLowerCase())
			{
				name = computer.nodeName;
				break;
			}
		}
	}

	return name;
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
		def sourceFilePath = getRFP(source);
		def delim = getPathDelim();
		if(sourceFilePath.exists())
		{
			def targetFilePath = getRFP(destination);

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
					//					def	targetFile = getRFP(new File(targetFilePath.getRemote(), sourceFilePath.getName()).getAbsolutePath());
					def	targetFile = getRFP("${targetFilePath.getRemote()}${delim}${sourceFilePath.getName()}");
					sourceFilePath.copyTo(targetFile);
					destination = targetFile.getRemote();
					echo "source: ${sourceFilePath.mode()} dest: ${targetFile.mode()}"
					targetFile.chmod(sourceFilePath.mode());
				}
				else if(!targetFilePath.getRemote().contains('.'))
				{
					targetFilePath.mkdirs();
					def	targetFile = getRFP(new File(targetFilePath.getRemote(), sourceFilePath.getName()).getAbsolutePath());
					sourceFilePath.copyTo(targetFile);
				}
				else
				{
					sourceFilePath.copyTo(targetFilePath);
				}
				copied = 1;
			}

			String mask = filter == null ? "*" : filter;
			inform(verbose,"Copied ${copied} files from '${source}' to '${destination}' mask: '${mask}'");
		}
		else
		{
			throw new Exception("Copy Error: file \"${source}\" does not exist");
		}
	}
	catch(err)
	{
		echo "copyFiles Failed: ${err.message}}"
	}

	return copied;
}

/**
 * Copy the source file into the destination path
 * @param sourceFile - the file you want to copy (ex. c:\tikiya\stam.txt)
 * @param destinationPath - the destination path (ex. c:\od_tikiya)
 * @return the new copeid file full path 
 */
String copyFile(String sourceFile, String destinationPath)
{
	def sourceFilePath = getRFP(sourceFile);

	def destinationFilePath = getRFP(new File(destinationPath, sourceFilePath.getName()).getAbsolutePath());

	sourceFilePath.copyTo(destinationFilePath);
	return destinationFilePath.getRemote();
}

int move(String source, String destination, String filter = null, boolean includeSub = false, boolean verbose = true)
{
	int moved = 0;

	List files = getAllFiles(source, filter, includeSub);

	for(file in files)
	{
		String relativeToSource = file.getRemote().replace(source, "");
		def tempFile = getRFP(new File(destination, relativeToSource).getAbsolutePath());
		file.copyTo(tempFile);
		file.delete();
		moved++;
	}

	inform(verbose, "Moved ${moved} files matching \"${filter}\" to ${destination}");

	return moved;
}

/**
 * Deletes Files/Folders on the agent.
 *
 * @param path the path to delete (from)
 * @param filter - Ant GLOB pattern.
 * 		String like "foo/bar/*.xml" Multiple patterns can be separated by ',', and whitespace can surround ','
 * 			(so that you can write "abc, def" and "abc,def" to mean the same thing.
 * 		if filter is null - delete will perform on all files
 * filter example: '*.xml, inner/*.xml')
 * @param deleteEmptyFolders - if true, after deleting a file, check parent if empty and delete it.
 * @param verbose - if true, prints the deleted items paths
 *
 */
void delete(String path, String filter = null, boolean deleteEmptyFolders = false, boolean verbose = true)
{
	List deletedFiles = [];

	try
	{
		def context = getRFP(path);
		if(context.exists())
		{
			if(filter != null)
			{
				def directoryWalker =
				{ directories ->
					for(directory in directories)
					{
						def files = directory.list(filter);

						for(file in files)
						{
							def parent = file.getParent();
							file.delete();
							deletedFiles << file.getRemote();

							if(deleteEmptyFolders)
							{
								// After deleting a file, check if parent's file list is empty
								if(parent.list().size() == 0)
								{
									parent.delete();
									deletedFiles << directory.getRemote();
								}
							}
						}
					}
				}

				// Requested to delete a folder with a filter, filter files
				directoryWalker([context]);
			}
			else
			{
				if(context.isDirectory())
				{
					// Requested to delete a folder
					context.deleteRecursive();
				}
				else
				{
					// Requested to delete a single file
					context.delete();
				}
				deletedFiles << context.getRemote();
			}
			inform(verbose, "Deleting ${path} with filter \"${filter}\", items: ${deletedFiles}");
		}
		else
		{
			echo "Warning: ${path} Does not exist"
		}
	}
	catch(err)
	{
		echo "Delete Failed: ${err.dump()}"
	}
}

/**
 * Gets the folder path with max integer value after removing ignoreSub from its name.
 * Flat search - no sub-directories
 *
 * @param baseDir the base dir to look in
 * @param ignoreSub the partial name to ignore
 * @return the folder path with the max int value
 */
String getMaxFolderPath(String baseDir, String ignoreSub, boolean verbose=false)
{
	String resultPath = null;
	try
	{
		inform(verbose, "Searching max in ${baseDir}");
		def basePath = getRFP(baseDir);
		def folders = basePath.listDirectories();
		if(folders.size() > 0)
		{
			int max = 0;
			for(folder in folders)
			{
				if(folder.getName().contains(ignoreSub))
				{
					CharSequence requiredPortion = (CharSequence)folder.getName().replace(ignoreSub, "");
					if(requiredPortion.isInteger())
					{
						int current = requiredPortion as Integer;
						if(current > max)
						{
							max = current;
							resultPath = new File(basePath.getRemote(), "${ignoreSub}${max}").getAbsolutePath();
						}
					}
					else
					{
						inform(verbose, "${requiredPortion} is not int?");
					}
				}
				else
				{
					inform(verbose, "${folder.getName()} not valid");
				}
			}
		}
		else
		{
			inform(verbose, "Found 0 folders in ${basePath}, Permissions Error?", true);
		}
	}
	catch(err)
	{
		echo "getMaxFolder Failed: ${err.dump()}"
	}

	return resultPath;
}

/**
 * Clears the given directory content.
 *
 * dirPath - the path of the folder to delete it's content
 *
 */
ExecuteResult clearFolder(String path, boolean verbose = true)
{
	ExecuteResult result = new ExecuteResult();
	try
	{
		def filePath = getRFP(path);

		if(filePath.exists())
		{
			filePath.deleteContents();
			inform(verbose, "Cleared content of ${path}");
		}
	}
	catch(err)
	{
		result.exception = err;
		inform(true, "ClearFolder Error: ${err.message}")
	}

	return result;
}

boolean exists(String path)
{
	def f = getRFP(path);
	return f.exists();
}

/**
 * Returns all files  as list of hudson.filepath from the given directory
 */
List getAllFiles(String directory, String filter = null, boolean includeSubDirectories = false, int depth = -1)
{
	def directoryFilePath = getRFP(directory);
	List files = [];

	if(directoryFilePath.exists())
	{
		List content = filter == null ? directoryFilePath.list() : directoryFilePath.list(filter);

		List subdirs = directoryFilePath.listDirectories();

		//		echo "subdirs of ${directory}: ${subdirs}"

		for (def subdir in subdirs)
		{
			if (!content.contains(subdir))
			{
				//				echo "adding subdir: ${subdir}"
				content.add(subdir);
			}
		}


		for(item in content)
		{
			if(includeSubDirectories && item.isDirectory())
			{
				if(depth == -1)
				{
					files.addAll(getAllFiles(item.getRemote(), filter, includeSubDirectories));
				}
				else if(depth > 0)
				{
					files.addAll(getAllFiles(item.getRemote(), filter, includeSubDirectories, depth-1));
				}
			}
			else if(!item.isDirectory())
			{
				files << item;
			}
		}
	}

	return files;
}

List getDirectories(String directory, boolean includeSubDirectories = false)
{
	def directoryFilePath = getRFP(directory);
	List directories = [];

	if(directoryFilePath.exists())
	{
		List subdirs = directoryFilePath.listDirectories();

		for (def subdir in subdirs)
		{
			if (!directories.contains(subdir))
			{
				//				echo "adding subdir: ${subdir}"
				directories.add(subdir);
			}
		}

		for(item in subdirs)
		{
			if(includeSubDirectories && item.isDirectory())
			{
				directories.addAll(getDirectories(item.getRemote(), includeSubDirectories));
			}
		}
	}

	return directories;
}

/**
 * Makes a dir on the slave machine.
 *
 * param path the dir path
 */
String makeDir(String path, boolean verbose = true)
{
	String dirPath = null;
	try
	{
		def filePath = getRFP(path);

		if(!filePath.exists())
		{
			filePath.mkdirs();
			inform(verbose, "Directory \"${path}\" Created!");
		}
		else if(!filePath.isDirectory())
		{
			throw new Exception("${path} exists as a file, cannot create dir");
		}

		dirPath = filePath.getRemote();
	}
	catch(err)
	{
		echo "makeDir Failed: ${err.dump()}"
	}

	return dirPath;
}

/**
 * Delete files older than X days (date modified).
 *
 * @param directoryFilePathObj the directory path to delete files from
 * @param days the amount of days before today to delete
 * @param extension if null, takes all the files, else gets only the files with 'extension' extension.
 * @param includeSubDirectories if true - searches sub directories as well
 */
void deleteFilesOlderThanXDays(String directoryPath, int days, String filter = null, boolean includeSubDirectories = true)
{
	List files = getAllFiles(directoryPath, filter, includeSubDirectories);
	Date pensia = (new Date() - days).clearTime();
	echo "Cleaning files in ${directoryPath} older than ${pensia.format('dd/MM/yyyy')}"
	int deleteCount = 0;
	for(file in files)
	{
		def vFile = file.toVirtualFile();
		Date fileDate = new Date(vFile.lastModified());
		// Delete only if its older then requested, and if we have extension check that it has same extension as user specified
		if(fileDate < pensia)
		{
			try{
				file.delete();
				deleteCount++;
			}
			catch(e){
				echo "Unable to delete ${file.dump()} - ${e.dump()}"
			}

		}
	}
	if(deleteCount > 0)
	{
		echo "Deleted ${deleteCount} old files (from before: ${pensia.format('dd/MM/yyyy')}) from \"${directoryPath}\""
	}
	else
	{
		echo "Folder \"${directoryPath}\" is up to date, no old files found"
	}
}


/**
 * Sorts the given directory by date, and deletes old items where index > X.
 * in other words it keeps only the X latest files/directories depends on the filter
 *
 * @param directoryPath the directory path
 * @param numToKeep the num to keep
 * @param filter can be (d)(dir)(directory)(folder) for directory, and (file)(files) for files
 */
void keepXItems(String directoryPath, int numToKeep, String filter = "d", boolean includeSubDirectories = false, boolean verbose = false)
{
	List items = [];
	filter = filter == null || filter == "" ? "d" : filter;
	String lookingFor = "directories";

	if(filter.toLowerCase().contains("fi"))
	{
		lookingFor = "files";
		items = getAllFiles(directoryPath, null, includeSubDirectories);
	}
	else
	{
		items = getDirectories(directoryPath, includeSubDirectories);
	}

	// compare closure for hudson.filepath against date modified
	Closure modifiedCompare = {a, b ->
		new Date(a.toVirtualFile().lastModified()) <=> new Date(b.toVirtualFile().lastModified())
	}
	ListUtils.sort(items, modifiedCompare);

	List<String> deletedItems = [];
	String message = "Kept latest ${numToKeep}/${items.size()} ${lookingFor} on \"${directoryPath}\" -> ";

	for(int i = numToKeep; i < items.size(); i++)
	{
		try
		{
			items[i].delete();
			deletedItems << items[i].getName();
		}
		catch(e)
		{
			echo "keepXItems Error: ${e.message}"
		}
	}

	if(deletedItems.size() > 0)
	{
		inform(!verbose, "${message}Deleted ${deletedItems.size()} sub ${lookingFor}");
		inform(verbose, "${message}Deleted: ${deletedItems}");
	}
	else
	{
		echo "${message}No old files found!"
	}
}

def getFileSizeInMb(String fileFullPath)
{
	def fileContext = getRFP(fileFullPath);
	def bytes = fileContext.length();
	def kilobytes = (bytes / 1024);
	def megabytes = (kilobytes / 1024);
	return megabytes;
}

// ------------------------- Utilities based on the above ---------------------------------




