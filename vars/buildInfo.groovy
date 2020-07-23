

/*
 runs on all pipelines in selected folder and finds last build by cron and prints:
 pipeline name, url, last build start time (by cron), duration, nodeA, nodeB,...,nodeZ
 */

/**
 * Gets a folder by its full name
 * @param folderName the full name of the folder
 * @return the folder instance or null if not exists
 */
def getFolder(String folderName)
{
	for(def item in  Jenkins.instance.getAllItems())
	{
		if(item instanceof com.cloudbees.hudson.plugins.folder.Folder && item.getFullName() == folderName)
		{
			return item;
		}
	}
	return null;
}

void writeToArchive(String relativePath, String content)
{
	File archivedFile = new File(manager.build.getArtifactManager().getArtifactsDir().getAbsolutePath(), relativePath);
	if(!archivedFile.getParentFile().exists())
	{
		archivedFile.getParentFile().mkdirs();
	}
	archivedFile.write(content);
}

/**
 * Checks for a given build if it was started by timer
 * @param build the build instance
 * @return true if started by cron, false otherwise
 */
boolean isByTimer(def build)
{
	return build.getCause(hudson.triggers.TimerTrigger$TimerTriggerCause) != null;
}

/**
 * gets the log for the given build and removes all unreadble characters from it
 * source: https://issues.jenkins-ci.org/browse/JENKINS-52510
 * @param build the build instance
 * @return the fixed log
 */
def getFixedLog(def build)
{
	def baos = new ByteArrayOutputStream();
	build.getLogText().writeLogTo(0, baos);
	return baos.toString();
}

/**
 * Converts a duration given in mili-seconds into short readable time
 * output ex: 37sec    |    1.5h
 * source: https://issues.jenkins-ci.org/browse/JENKINS-52510
 * @param the duration in mili-seconds
 * @return a short string representing the duration
 */
String getDurationStrFromMili(long durationMilisec){
  int seconds = ((int) (durationMilisec / 1000)) % 60 ;
  int minutes = ((int) ((durationMilisec / (1000*60))) % 60);
  int hours   = ((int) ((durationMilisec / (1000*60*60))) % 24);
  //println "mili: ${durationMilisec} ; sec: ${seconds}  ; min: ${minutes} ; h: ${hours}"
  
  def formattedStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)
  //println formattedStr
  return formattedStr
  
}



def createFromLastTimerBuild(String folderName)
{
    List<String> csvLines = ["Project name, Url, Started at, Duration, Stations"]
	def folder = getFolder(folderName);
	for(def project in folder.items)
	{
		boolean foundTimer = false;

		if(!(project instanceof com.cloudbees.hudson.plugins.folder.Folder))
		{
			def currentBuild = project.getLastBuild();
			def currentBuildNumber = currentBuild == null ? 0 : currentBuild.number;

			while(currentBuild != null && !foundTimer && currentBuildNumber > 0)
			{
				if(isByTimer(currentBuild))
				{
					foundTimer = true;
				}
				else
				{
					currentBuildNumber--;
					currentBuild = project.getBuildByNumber(currentBuildNumber);
				}
			}

			if(currentBuild == null)
			{
				println "${project.name}, ${project.getAbsoluteUrl()}, has no timer builds"
				continue;
			}

			
			def log = getFixedLog(currentBuild).readLines();
			//def log = currentBuild.logFile.text.readLines();
			def runningon = log.findAll({it.contains("Running on")})
			def slaves = [];
			for(def line in runningon)
			{
				def nameMatch = line =~ /(?i).*Running on (.*) in.*/;
				if(nameMatch)
				{ // need 1st group
					if(!slaves.contains(nameMatch.group(1))){
						slaves << nameMatch.group(1)
					}
				}
			}
			line = "${project.name}, ${currentBuild.getAbsoluteUrl()}, ${currentBuild.getTime().format('dd/MM/yyyy hh:mm:ss')}, ${getDurationStrFromMili(currentBuild.duration)}, ${slaves.join(';')}";
			println(line)
			csvLines << line
		}
	}
	
	if(env != null)
    {
    	// we are inside a pipeline and not in script console 
    	writeToArchive("nodes.csv", csvLines.join("\n"));
    }
}

def createFromBuildProperties(String folderName)
{
    List<String> csvLines = ["Project name, Url, Cron, Duration, Stations"]
	def folder = getFolder(folderName);
	for(def project in folder.items)
	{
	    cronTab = null;

		if(!(project instanceof com.cloudbees.hudson.plugins.folder.Folder))
		{
			def lastBuild = project.getLastBuild();
			
			if(lastBuild == null)
			{
				echo "Project: ${project.name} has no builds"
			}
			else
			{
    			def pipe = lastBuild.getParent();
    			
    			def pipelineProperties = pipe.properties;
    			
    			
    			for(def prop in pipelineProperties)
    			{
    			    if(prop.value.getClass().getName() == "org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty")
		            {
            			for(trig in prop.value.triggers)
            			{
            				// echo "trigger: ${trig.dump()}"
            				if(trig.getClass().getName() == "hudson.triggers.TimerTrigger")
            				{
								cronTab = trig.spec
            				}
            			}
            		
            		}
    			}
			}
			
			def log = getFixedLog(lastBuild).readLines();
			def runningon = log.findAll({it.contains("Running on")})
			def slaves = [];
			for(def line in runningon)
			{
				def nameMatch = line =~ /(?i).*Running on (.*) in.*/;
				if(nameMatch)
				{ // need 1st group
					if(!slaves.contains(nameMatch.group(1))){
						slaves << nameMatch.group(1)
					}
				}
			}
			line = "${project.name}, ${currentBuild.getAbsoluteUrl()}, ${cronTab}, ${getDurationStrFromMili(currentBuild.duration)}, ${slaves.join(';')}";
                			    println(line)
                			    csvLines << line
		}
	}
	if(env != null)
    {
    	// we are inside a pipeline and not in script console 
    	writeToArchive("nodes.csv", csvLines.join("\n"));
    }
}

String folderName = ""

if(params != null)
{
    folderName = params.FOLDER_NAME;
}

createFromBuildProperties(folderName);

