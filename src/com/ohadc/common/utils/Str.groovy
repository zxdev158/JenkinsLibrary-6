package com.ohadc.common.utils;

/**
 * @author ohadc
 *
 */
public class Str implements Serializable
{
	public static boolean noe(String str)
	{
		return str == null || str.trim() == "";
	}

	public static String startsWithLongest(String str, List<String> options)
	{
		String longest= null;
		if(!Str.noe(str))
		{
			for(String option in options)
			{
				if(str.startsWith(option) && (longest == null || option.length() > longest.length()))
				{
					longest = option;
				}
			}
		}
		return longest;
	}

	public static String removeUTF8BOM(String s)
	{
		if (s.startsWith("\uFEFF"))
		{
			s = s.substring(1);
		}
		return s;
	}
}
