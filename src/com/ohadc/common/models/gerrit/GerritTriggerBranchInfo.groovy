package com.ohadc.common.models.gerrit;

import java.io.Serializable;
import java.util.Map;
import com.ohadc.common.models.JsonSerializable;

/**
 * @author ohadc
 *
 */
public class GerritTriggerBranchInfo extends JsonSerializable<GerritTriggerBranchInfo> implements Serializable
{
	String name;

	public GerritTriggerBranchInfo(def gerritBranchObj)
	{
		if(gerritBranchObj.getClass().getName() == "com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.data.Branch")
		{
			this.name = gerritBranchObj.pattern;
		}
		else
		{
			throw new Exception("Unable to resolve gerrit branch ${gerritBranchObj.dump()}");
		}
	}
}
