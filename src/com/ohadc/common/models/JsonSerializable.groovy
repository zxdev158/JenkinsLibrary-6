package com.ohadc.common.models;

// to support converting object to json
import com.google.gson.Gson;
// to support pretty print json
import com.google.gson.GsonBuilder;

class JsonSerializable<T>
{
	public String toJson(boolean pretty = true)
	{
		String jsonStr = "";
		Gson gson = null;

		gson = pretty ? new GsonBuilder().setPrettyPrinting().create() : new Gson();

		jsonStr = gson.toJson(this);
		return jsonStr;
	}

	public T fromJson(String jsonString)
	{
		Gson gson = new Gson();
		T obj = gson.fromJson(jsonString, T.class);

		return obj;
	}
}
