package tools;


public class PasswordGenerator{


	/**
	 * @param length Given length must be greater or equals to 8
	 * @return a random password for the given length.
	 * the generated password guarantee to have at least one lower case alphabet, one upper case, one number and one symbol of these four - @ $ _
	 */
	public static String generatePswForLength(int length){
		
		if(length < 8) throw new IllegalArgumentException("Given length must be greater or equals to 8");
		
		String password = "";
		
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		String ALPHABET = alphabet.toUpperCase();
		String number = "0123456789";
		String symbol = "_@$-";
		String[] pswCharsSet = new String[]{alphabet, ALPHABET, number, symbol};
		
		while(true){
			// used to monitor which charsSet (alphabet, ALPHABET, number or symbol) has been used
			// all false at the start
			boolean[] charsSetUsed = new boolean[4];
			
			for(int i=0; i<length; i++){
				// I want the chance to have alphabet and number is two times more than symbol
				// decide which char set (alphabet, ALPHABET, number, symbol) is selected
				int charsSetPost = (int)(Math.random() * 7) % 4;
				String charsSet = pswCharsSet[charsSetPost];
				
				// decide which char
				int charPost = (int)(Math.random() * charsSet.length());
				password += charsSet.charAt(charPost);
				
				// set charsSet at charsSetPost to true to tell that it has been used
				charsSetUsed[charsSetPost] = true; 
			}
			
			// if there's any chars set has not been used at least once, we need to regenerate the password
			// otherwise, we stop generate and break the while loop
			if(charsSetUsed[0] && charsSetUsed[1] && charsSetUsed[2] && charsSetUsed[3]) 
				break;
			else
				password = "";
		}
		
		return password;
	}

}
