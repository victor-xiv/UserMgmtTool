package beans;

import java.util.TreeMap;

import tools.CountryCode;

public class Countries {
	TreeMap<String,String> countries;
	public Countries(){
		countries = CountryCode.getCountryNameMap();
	}
	
	public void setCountries(TreeMap<String,String> countries){
		this.countries = countries;
	}
	
	public TreeMap<String,String> getCountries(){
		return this.countries;
	}
}
