// get all pipelines in folder with their last build start time

def getFolder(String folderName){
  for(def item in  Jenkins.instance.getAllItems()){
        if(item instanceof com.cloudbees.hudson.plugins.folder.Folder && item.getFullName() == folderName){
          return item;
        }
  }
  return null;
}

def isByTimer(def build){
  return build.getCause(hudson.triggers.TimerTrigger$TimerTriggerCause)!=null;
}

def fixLog(def build)
{
	def baos = new ByteArrayOutputStream();
	build.getLogText().writeLogTo(0, baos);
	return baos.toString();
}

def folder = getFolder("full folder name");
for(def project in folder.items)
{
  boolean foundTimer = false;
  
  def currentBuild = project.getLastBuild();
  def currentBuildNumber = currentBuild == null ? 0 : currentBuild.number;
  
  while(currentBuild != null && !foundTimer && currentBuildNumber > 0){
		if(isByTimer(currentBuild)){
			foundTimer = true;
		}
		else{
			currentBuildNumber--;
			currentBuild = project.getBuildByNumber(currentBuildNumber);
		}
  }
  
  if(currentBuild == null){
    println "${project.name}, ${project.getAbsoluteUrl()}, has no timer builds"
    continue;
  }
  
  def log = fixLog(currentBuild).readLines();
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
  println("${project.name}, ${project.getAbsoluteUrl()}, ${currentBuild.getTime().format('dd/MM/yyyy hh:mm:ss')}, ${slaves.join(',')}")
}

