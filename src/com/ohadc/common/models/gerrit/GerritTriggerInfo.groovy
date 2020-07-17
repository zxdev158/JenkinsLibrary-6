package com.ohadc.common.models.gerrit;

import java.io.Serializable;
import java.util.Map;
import com.ohadc.common.models.JsonSerializable;

/**
 * @author ohadc
 *
 */
public class GerritTriggerInfo extends JsonSerializable<GerritTriggerInfo> implements Serializable
{
	List<GerritTriggerProjectInfo> gerritProjects;

	/**
	 * gerritTriggerObject should be of type com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger
	 */
	public GerritTriggerInfo(def gerritTriggerObject)
	{
		if(gerritTriggerObject.getClass().getName() =="com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger")
		{
			def projects = gerritTriggerObject.gerritProjects;
			if(projects != null)
			{
				this.gerritProjects = [];
				for(project in projects)
				{
					this.gerritProjects << new GerritTriggerProjectInfo(project);
				}
			}
			else
			{
				this.gerritProjects = projects;
			}
		}
		else
		{
			throw new Exception("Unable to resolve gerrit trigger ${gerritTriggerObject.dump()}")
		}
	}
}
