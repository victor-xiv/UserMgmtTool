package beans;

import java.util.TreeMap;

import tools.CountryCode;

public class Countries {
	TreeMap<String,String> countries;
	
	/**
	 * read CountryCode.xml (pathToTomcatConfFolder/CountryCode.xml) assign the key,value pair into countries TreeMap
	 * @return a TreeMap that contains key,value pair of all countries. i.e. {(AF, Afghanistan), ...}
	 */
	public Countries(){
		countries = CountryCode.getCountryNameMap();
	}
	
	/**
	 * setter of countries
	 * @param countries a TreeMap that contains key,value pair of all countries. i.e. {(AF, Afghanistan), ...}
	 */
	public void setCountries(TreeMap<String,String> countries){
		this.countries = countries;
	}
	
	/**
	 * getter of countries
	 * @return countries  a TreeMap that contains key,value pair of all countries. i.e. {(AF, Afghanistan), ...}
	 */
	public TreeMap<String,String> getCountries(){
		return this.countries;
	}
}
