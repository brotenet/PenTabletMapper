package openjfx.os.linux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class LinuxClient {

	public static ArrayList<String> bash(String... commands) throws Exception {
		ArrayList<String> output = new ArrayList<>();
        for (String command : commands) {
            ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder lineBuffer = new StringBuilder();
            String line; while ((line = reader.readLine()) != null) { lineBuffer.append(line).append("\n"); }
            output.add(lineBuffer.toString().trim());
            process.waitFor();
        }
        return output;
    }
	
}
