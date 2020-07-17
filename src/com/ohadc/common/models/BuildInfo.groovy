package com.ohadc.common.models;

import java.util.Map;
import groovy.util.Expando;

public class BuildInfo extends Expando implements Serializable
{
	public static String k_HardLink = "~HardLNK~";
	private static BuildInfo instance;

	private BuildInfo()
	{
		super();
	}

	private BuildInfo(Map objectProperties)
	{
		super(objectProperties);
	}

	public static BuildInfo get()
	{
		return BuildInfo.get([:]);
	}

	public static BuildInfo get(Map objectProperties)
	{
		if(this.instance == null)
		{
			this.instance = new BuildInfo(objectProperties);
		}

		objectProperties.each
		{
			this.instance.add(it.key, it.value);
		}

		return this.instance;
	}

	public void add(String name, def value)
	{
		this.getProperties().put(name, value);
	}

	public sync()
	{
		ArrayList<String> removeProps = [];
		for(def prop in getProperties())
		{
			String lower = prop.key.toString().toLowerCase();
			if(lower.contains("url") && !lower.contains(BuildInfo.k_HardLink))
			{
				if(!isResponsive(prop.value))
				{
					removeProps << prop.key;
				}
			}
		}

		for(def prop in removeProps)
		{
			super.getProperties().remove(prop);
		}
	}

	private boolean isResponsive(String url)
	{
		def get = new URL(url).openConnection();
		return get.getResponseCode().equals(200);
	}
}