package com.ohadc.common.models.gerrit;

import java.io.Serializable;
import java.util.Map;
import com.ohadc.common.models.JsonSerializable;

/**
 * @author ohadc
 *
 */
public class GerritTriggerProjectInfo extends JsonSerializable<GerritTriggerProjectInfo> implements Serializable
{

	String name;
	List<GerritTriggerBranchInfo> branches;

	public GerritTriggerProjectInfo(def gerritProjectObject)
	{
		if(gerritProjectObject.getClass().getName() == "com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.GerritProject")
		{
			this.branches = [];
			this.name = gerritProjectObject.pattern;
			if(gerritProjectObject.branches != null)
			{
				for(branch in gerritProjectObject.branches)
				{
					this.branches << new GerritTriggerBranchInfo(branch);
				}
			}
		}
		else
		{
			throw new Exception("Unable to resolve gerrit project ${gerritProjectObject.dump()}");
		}
	}
}
