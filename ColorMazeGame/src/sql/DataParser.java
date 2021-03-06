package sql;

import sql.InfoPackage;
import etc.Constants;

public class DataParser {
	private static String data;
	
	public DataParser() {
		
	}
	
	public DataParser(InfoPackage pack) {
		data = "|";
		data = data.concat(pack.getTime());
		data = data.concat("|");
		int [] actions = pack.getActions();
		int index = 0;
		while(actions[index] != 0 && index < 500) {
			switch(actions[index]) {
			case Constants.DIR_LEFT:
				data = data.concat("l");
				break;
			case Constants.DIR_RIGHT:
				data = data.concat("r");
				break;
			case Constants.DIR_UP:
				data = data.concat("u");
				break;
			case Constants.DIR_DOWN:
				data = data.concat("d");
				break;
			}
			index++;
		}
		
		data = data.concat("|");
	}
	
	public String getData() {
		return data;
	}
}
