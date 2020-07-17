package com.ohadc.common.models

import com.ohadc.common.utils.Str

/**
 * @author ohadc
 *
 */
public class ExecuteResult implements Serializable
{
	public String logPath;

	public String dataPath;

	public String log;

	public String data;

	public Exception exception;

	public ExecuteResult()
	{
	}

	public ExecuteResult(String dataPath, String logPath)
	{
		this.dataPath = dataPath;
		this.logPath = logPath;
	}

	public ExecuteResult cutLog()
	{
		if(this.log != null)
		{
			List<String> lines = this.log.readLines();
			int cutFrom = 0;
			for(int i = 0; i < lines.size();i++)
			{
				if(!Str.noe(lines[i]) && !lines[i].trim().startsWith("+") && !lines[i].trim().startsWith("["))
				{
					cutFrom = i;
					break;
				}
			}
			lines = lines.subList(cutFrom, lines.size());
			this.log = lines.join('\n');
		}
		return this;
	}

	public String debug()
	{
		return "ex: ${this.exception}"
	}
}
