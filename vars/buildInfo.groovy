import com.ohadc.common.models.BuildInfo;
import com.ohadc.common.models.gerrit.GerritTriggerInfo;
import groovy.json.*;
import groovy.xml.*;

GerritTriggerInfo getGerritTriggerInfo()
{
	GerritTriggerInfo triggerInfo = null;
	def thisBuild = currentBuild.rawBuild; // should be org.jenkinsci.plugins.workflow.job.WorkflowRun
	echo "this build: ${thisBuild.dump()}"

	def thisRun = thisBuild.getParent(); // should be org.jenkinsci.plugins.workflow.job.WorkflowJob
	echo "this run: ${thisRun.dump()}"

	def props = thisRun.properties;
	echo "props: ${props.dump()}"

	for(prop in props)
	{
		// Checking class name since i dont wanna download the relevant jar
		if(prop.value.getClass().getName() == "org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty")
		{
			echo "this prop is related to trigger"
			echo "prop value triggers: ${prop.value.triggers.dump()}"
			for(trig in prop.value.triggers)
			{
				echo "trigger: ${trig.dump()}"
				// if(trig instanceof com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger)
				if(trig.getClass().getName() =="com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger")
				{
					triggerInfo = new GerritTriggerInfo(trig);
					break;
				}
			}
			if(triggerInfo != null)
			{
				break;
			}
		}

	}
	return triggerInfo
}

List<String> getCauseDescriptionList()
{
	List<String> causes = [];
	def buildCauses = currentBuild.rawBuild.getCauses();

	for(def cause in buildCauses)
	{
		causes << "${cause.getShortDescription()}"
	}

	return causes;
}

boolean isByUser()
{
	return currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause) != null;
}

boolean isBySCM()
{
	return currentBuild.rawBuild.getCause(hudson.triggers.SCMTrigger$SCMTriggerCause) != null;
}

boolean isByUpstream()
{
	return currentBuild.rawBuild.getCause(hudson.model.Cause$UpstreamCause) != null;
}

boolean isByTimer()
{
	return currentBuild.rawBuild.getCause(hudson.triggers.TimerTrigger$TimerTriggerCause) != null;
}

boolean isByRemote()
{
	return currentBuild.rawBuild.getCause(hudson.model.Cause$RemoteCause) != null;
}

int stageIndex(String name)
{
	BuildInfo info = get();

	if(info.stages != null)
	{
		return info.stages.findIndexOf({it.stageName == name});
	}

	return -1;
}

def isLastStage(String name)
{
	int index = stageIndex(name);

	if(index > -1)
	{
		// this means that stageindex worked - so stages exists
		BuildInfo info = get();
		if(index == info.stages.size() - 1)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	return null;
}

def getStages()
{
	return get().stages;
}

String asJson(BuildInfo providedInfo = null)
{
	BuildInfo info = providedInfo != null ? providedInfo : getRawInfo();
	String result = json.stringify(info);
	return result;
}

String result()
{
	return currentBuild.result == null ? "SUCCESS" : currentBuild.result;
}

void setResult(String result = "FAILURE")
{
	currentBuild.result = result;
}

String description(String text = "")
{
	if(text != null && text != "")
	{
		if(currentBuild.description == null || currentBuild.description == "")
		{
			currentBuild.description = text;
		}
		else
		{
			currentBuild.description += "\n${text}";
		}

	}
	//	echo "current description: ${currentBuild.description}"
	return currentBuild.description;
}

void clearDescription()
{
	currentBuild.description = null;
}

void unstable()
{
	setResult("UNSTABLE");
}

void abort(){
	setResult("ABORTED")
}

void fail()
{
	setResult()
}

BuildInfo getRawInfo()
{
	BuildInfo info = BuildInfo.get([
		cause: getCauseDescriptionList().join("\n"),
		status: result(),
		jobUrl: env.JOB_URL,
		buildUrl: env.BUILD_URL,
		testsUrl: "${env.JOB_URL}test_results_analyzer/",
		htmlReportUrl: "${env.BUILD_URL}HTML_Report/",
		consoleUrl: "${env.BUILD_URL}console/"
	]);
	info.sync();
	return info;
}

BuildInfo get()
{
	return getRawInfo();
}

void addInfo(String name, def value)
{
	BuildInfo.get().add(name, value);
}

void addHardLink(String name, def value)
{
	BuildInfo.get().add(name+BuildInfo.k_HardLink, value);
}

String asXml(BuildInfo providedInfo = null)
{
	// Create the markup builder
	StringWriter writer = new StringWriter();
	MarkupBuilder xml = new MarkupBuilder(writer);
	xml.setDoubleQuotes(true);

	// set root element "buildInfo"
	xml.buildInfo();

	// Parse markup builder into a groov xml node
	def root = new XmlParser().parseText(writer.toString())

	try
	{
		BuildInfo info = providedInfo != null ? providedInfo : getRawInfo();

		for(def prop in info.getProperties())
		{
			boolean valid = prop.value instanceof String || prop.value instanceof GString ||prop.value.getClass().isPrimitive();

			if(valid)
			{
				String lowerKey = prop.key.toLowerCase();
				root.appendNode(new QName("property"), [name:prop.key.replace(BuildInfo.k_HardLink, ""), value:prop.value, type: lowerKey.contains("url") || prop.key.contains(BuildInfo.k_HardLink) ? "link" : "string"]);
			}
		}
	}
	catch(e)
	{
		echo "buildInfo.asXml Error: ${e.message}"
	}

	String xmlText = XmlUtil.serialize(root);
	xmlText = xmlText.replace("?>", "?>\n").replace("xmlns=\"\" ", "");
	return xmlText;
}

void writeTriggerInfo()
{
	String gerritInfo = "";
	String projectName = env.GERRIT_PROJECT != null ? env.GERRIT_PROJECT : "";
	String projectBranch = env.GERRIT_BRANCH != null ? env.GERRIT_BRANCH : "";
	String changeOwnerName = env.GERRIT_CHANGE_OWNER_NAME != null ? env.GERRIT_CHANGE_OWNER_NAME : "";
	String changeOwnerEmail = env.GERRIT_CHANGE_OWNER_EMAIL != null ? env.GERRIT_CHANGE_OWNER_EMAIL : "";
	String commitMessage = env.GERRIT_CHANGE_SUBJECT != null ? env.GERRIT_CHANGE_SUBJECT : "";
	commitMessage = commitMessage.replace(",", ".");
	String gerritUrl = env.GERRIT_CHANGE_URL != null ? env.GERRIT_CHANGE_URL : "";

	if(projectName != "")
	{
		gerritInfo = "${projectName},${projectBranch},${changeOwnerName}(${changeOwnerEmail}),${commitMessage},${gerritUrl}"
		artifacts.writeFile("trigger.txt", gerritInfo);
	}
}
