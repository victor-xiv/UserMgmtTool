/**
 * validate the given mobile number
 * The valid phone numbers are those that are in below forms:
0225254566			NZ phone (without country code)
 64225254566		With country code, without "+" sign
+64225254566		without "0" infront of the carrier 
+640225254566		with "0" infront of the carrier
+064225254566		with "0" infornt of the country code
+0640225254566		with "0" infront of country code and infront of the carrier
+64-0225254566		"-" seperates country code and other parts 
+64-022-525-4566	"-" seperates parts
00640225254566		with "dial out" code
(64)0225254566		brackets wrapping country code
(064)0225254566		brackets wrapping country code and with "0" infornt of the country code
(+64)0225254566		brackets wrapping country code
 */
function mobileValidator(mobile) {

	mobile = mobile.replace(new RegExp("\\(0\\)|[-()\\s]", 'g'), ""); //replace (0) ( ) - white space with empty char
	if (mobile.length < 8 || mobile.length > 18) {
		validated = false;
	} else {
		if (mobile.indexOf("+", 1) == -1) { // if it doesn't contain a + sign in the middle of the string
			if (mobile.search(/[^0-9+]/g) != -1) { // if mobile contains any alphabet (not a digit number and not + sign)
				validated = false;
			} else {
				validated = true;
			}
		} else { // if it contains a + sign in the middle of the string
			validated = false;
		}
	}

	return validated;
}




/**
 * validate password psw1 and the confirm password psw2
 * psw1 and psw2 must be at least 8 chars long
 * 				 must contain at least one lowercase alphabet a-z
 * 				 must contain at least one uppercase alphabet A-Z
 * 				 must contain at least one number 0-9
 * 				 must contain at least one symbol from: _ @ $ -  
 */
function passwordValidator(psw1, psw2) {
	if (psw1 != null && psw2 != null) {
		var indices = "" + psw1.search(/[a-z]/g) + psw1.search(/[A-Z]/g)
				+ psw1.search(/[0-9]/g) + psw1.search(/[_@$-]/g);
		if (psw1.length < 8 || indices.indexOf("-1") != -1) {
			alert("The password is not incorrect. A valid password must have 8 characters, and it contains at least one lowercase alphabet, one uppercase alphabet, one number and one of these four symbols: @ _ $ -");
			return false;
		} else {
			if (psw1 === psw2) {
				return true;
			} else {
				alert("The confirmation password is not the correct.");
				return false;
			}

		}
	}
	
	return false;
}
