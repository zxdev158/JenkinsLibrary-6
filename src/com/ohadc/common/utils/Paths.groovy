package com.ohadc.common.utils

import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;

public class Paths
{
	/**
	 * SHOULD REMOVE
	 * @param delim
	 * @param paths
	 * @return
	 */
	public static String commonPath(String delim, def paths)
	{
		// this code adapted from http://rosettacode.org/wiki/Find_common_directory_path#Groovy

		String commonPath = "";

		def folders = [paths.size()][];

		for(int i = 0; i < paths.size(); i++)
		{
			folders[i] = paths[i].split(Pattern.quote(delim)); //split on file separator
		}

		for(int j = 0; j < folders[0].size(); j++)
		{
			String thisFolder = folders[0][j]; //grab the next folder name in the first path
			boolean allMatched = true; //assume all have matched in case there are no more paths
			for(int i = 1; i < folders.size() && allMatched; i++){ //look at the other paths
				if(folders[i].size() < j){ //if there is no folder here
					allMatched = false; //no match
					break; //stop looking because we've gone as far as we can
				}

				//otherwise
				allMatched &= folders[i][j].equals(thisFolder); //check if it matched
			}
			if(allMatched){ //if they all matched this folder name
				commonPath += thisFolder + delim; //add it to the answer
			}else{//otherwise
				break;//stop looking
			}
		}

		// if common path contains a dot, check if it ends with 'delim' and remove it, afterwards - get parent.
		if(commonPath.contains("."))
		{
			if(commonPath.endsWith(delim))
			{
				// remove delim - e.g. last char
				commonPath = commonPath.substring(0, commonPath.length() - 1)
			}

			def ext = FilenameUtils.getExtension(commonPath)
			if(ext.length() >=2 && ext.length() <= 4)
			{
				commonPath = new File(commonPath).getParent();
			}
		}

		return commonPath;
	}

	public static String getMaxCommonPath(String delim, def paths)
	{
		// this code adapted from http://rosettacode.org/wiki/Find_common_directory_path#Groovy

		String commonPath = "";

		def folders = [paths.size()][];

		for(int i = 0; i < paths.size(); i++)
		{
			folders[i] = paths[i].split(Pattern.quote(delim)); //split on file separator
		}

		for(int j = 0; j < folders[0].size(); j++)
		{
			String thisFolder = folders[0][j]; //grab the next folder name in the first path
			boolean allMatched = true; //assume all have matched in case there are no more paths
			for(int i = 1; i < folders.size() && allMatched; i++){ //look at the other paths
				if(folders[i].size() < j){ //if there is no folder here
					allMatched = false; //no match
					break; //stop looking because we've gone as far as we can
				}

				//otherwise
				allMatched &= folders[i][j].equals(thisFolder); //check if it matched
			}
			if(allMatched){ //if they all matched this folder name
				commonPath += thisFolder + delim; //add it to the answer
			}else{//otherwise
				break;//stop looking
			}
		}

		// if common path contains a dot, check if it ends with 'delim' and remove it, afterwards - get parent.
		if(commonPath.contains("."))
		{
			if(commonPath.endsWith(delim))
			{
				// remove delim - e.g. last char
				commonPath = commonPath.substring(0, commonPath.length() - 1)
			}

			def ext = FilenameUtils.getExtension(commonPath)
			if(ext.length() >=2 && ext.length() <= 4)
			{
				commonPath = new File(commonPath).getParent();
			}
		}

		return commonPath;
	}

	public List<String> sortPaths(List<String> paths, def closure)
	{
		Map<Integer, List<String>>testsMap = [:];
		paths.each {
			int size = closure(it);
			testsMap.containsKey(size) ? testsMap.put(size, testsMap.get(size) + it) : testsMap.put(size, [it])
		}
		testsMap = testsMap.sort();

		List<String> tempList = [];
		testsMap.each {
			it.value.sort();
			tempList.addAll(it.value);
		}

		return tempList;
	}

	/**
	 * Gets the extension of the given filePath 
	 * Splits it by dot and returns the last portion
	 * @param filePath the file path whom extension is required
	 * @return the extension of the given file (without the dot)
	 */
	public static String extension(String filePath)
	{
		String ext = "";

		if(filePath!=null)
		{
			String[] splitted = filePath.split("\\.");
			ext = splitted.last();
		}

		return ext;
	}

	public static nameNoExt(String filePath)
	{
		String name = "";
		try
		{
			File f = new File(filePath);
			name = f.getName();
			String[] splitted = name.split("\\.");
			splitted = splitted.dropRight(1);
			name = splitted.join(".");
		}
		catch(Exception e)
		{
		}
		return name;
	}

	public static String buildPath(List<String> parts, String delim)
	{
		String path = null;

		if(parts != null)
		{
			for(String part in parts)
			{
				if(path != null)
				{
					while(path.endsWith(delim))
					{
						// remove trailing delimiters (as many as there are)
						path = path.substring(0, path.length() - delim.length());
					}

					path = "${path}${delim}${part}"
				}
				else
				{
					path = part;
				}
			}
		}

		return path;
	}

	public static String resolve(List<String> parts, String delim)
	{
		String path = null;
		String backDir = "..";
		if(parts != null)
		{
			for(String part in parts)
			{
				if(path != null)
				{
					// hadle path
					while(path.endsWith(delim))
					{
						// remove trailing delimiters (as many as there are)
						path = path.substring(0, path.length() - delim.length());
					}

					// handle part
					while(part.startsWith(delim))
					{
						// remove leading delimiters (as many as there are)
						part = part.substring(1, part.length());
					}
					
					while(part.startsWith(backDir))
					{
						// fix path
						path = path.substring(0, path.lastIndexOf(delim));
						// fix part
						part = part.substring(backDir.length());
						// handle part
						while(part.startsWith(delim))
						{
							// remove leading delimiters (as many as there are)
							part = part.substring(1, part.length());
						}
					}


					path = "${path}${delim}${part}"
				}
				else
				{
					path = part;
				}
			}
		}

		return path;
	}
}
