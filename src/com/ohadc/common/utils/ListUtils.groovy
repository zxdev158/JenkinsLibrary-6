package com.ohadc.common.utils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author ohadc
 *
 */
public class ListUtils implements Serializable
{
	/**
	 * Maps the given string array to (value, [indexes]).
	 *
	 * @param the list to map
	 * @return the map of values to indexes
	 */
	public static Map<String, List<Integer>> mapLine(String[] line)
	{
		Map<String, List<Integer>> map = [:];

		for(int i=0; i < line.size(); i++)
		{
			if(map.containsKey(line[i]))
			{
				map[line[i]] << i;
			}
			else
			{
				map[line[i]] = [i];
			}
		}

		return map
	}

	public static int maxSequentialValue(List lst, Closure equalFunc)
	{
		int maxSequence = 0;
		int currentCount = 0;

		for(def item in lst)
		{
			if(equalFunc(item))
			{
				currentCount++;
			}
			else
			{
				currentCount = 0;
			}

			if(currentCount > maxSequence)
			{
				maxSequence = currentCount;
			}
		}

		return maxSequence;
	}

	public static void sortFilePathABC(List lst)
	{
		if (lst == null || lst.size() == 0)
		{
			return;
		}
		Closure abcCompare =
		{a, b ->
			a.getRemote() <=> b.getRemote()
		}

		int high = lst.size()-1;
		quickSort(lst, 0, high, abcCompare);
	}

	public static void sort(List lst, Closure c)
	{

		if (lst == null || lst.size() == 0)
		{
			return;
		}

		int high = lst.size()-1;
		quickSort(lst, 0, high, c);
	}

	public static void quickSort(List lst, int lowerIndex, int higherIndex, Closure c)
	{

		int i = lowerIndex;
		int j = higherIndex;
		// calculate pivot number, I am taking pivot as middle index number
		def dec = lowerIndex+(higherIndex-lowerIndex)/2;
		def idx = Integer.valueOf(dec.intValue())
		def pivot = lst[idx];
		// Divide into two arrays
		while (i <= j) {
			while (c(lst[i], pivot) == 1) {
				i++;
			}
			while (c(lst[j], pivot) == -1) {
				j--;
			}
			if (i <= j) {
				lst.swap(i, j);
				//move index to next position on both sides
				i++;
				j--;
			}
		}
		// call quickSort() method recursively
		if (lowerIndex < j)
			quickSort(lst, lowerIndex, j, c);
		if (i < higherIndex)
			quickSort(lst, i, higherIndex, c);
	}
}
