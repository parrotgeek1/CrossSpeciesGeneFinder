import java.net.*;
import java.io.*;

public class WebUtils {
	// HTTP POST request
	public static InputStream doPost(String url, String query) throws MalformedURLException{
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			
			//add request header
			con.setRequestMethod("POST");
			
			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(query);
			wr.flush();
			wr.close();
			
			return con.getInputStream();
		} catch(IOException e) {
			return null;
		}
	}
}
