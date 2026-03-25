package application.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;

public class MainForm {

    @FXML
    private Label helloLabel;
    
    @FXML
    private void onButtonClick() {
        helloLabel.setText("Hello, JavaFX FXML!");
    }
    
    @FXML
    private void onDownloadSqliteConnector() {
    	helloLabel.setText("Downlading SQLiteConnector...");
    	try {//https://github.com/pgjdbc/pgjdbc/releases
//			downloadLatestJar("https://github.com/xerial/sqlite-jdbc/releases", "/home/user/Documents/MEGA/Eclipse/workspace/x_pointer_redirect/lib/");
    		downloadLatestJar("https://github.com/pgjdbc/pgjdbc/releases", "/home/user/Documents/MEGA/Eclipse/workspace/x_pointer_redirect/lib/");
    	} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static void downloadLatestJar(String releasesUrl, String outputDir) 
            throws IOException, InterruptedException {
        
        // Step 1: Parse owner and repo from releasesUrl
        URI uri = URI.create(releasesUrl);
        String path = uri.getPath();
        if (!path.endsWith("/releases")) {
            throw new IllegalArgumentException("URL must end with '/releases'");
        }
        String repoPath = path.substring(1, path.length() - "/releases".length()); // e.g., "xerial/sqlite-jdbc"
        String[] parts = repoPath.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid GitHub repo path in URL");
        }
        String owner = parts[0];
        String repo = parts[1];
        
        // Build API URL
        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";
        
        // Step 2: Create HttpClient with explicit redirect following
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        
        HttpRequest apiRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("User-Agent", "Java-App")  // Required by GitHub API
                .header("Accept", "application/vnd.github.v3+json")
                .build();

        HttpResponse<String> apiResponse = client.send(apiRequest, HttpResponse.BodyHandlers.ofString());
        if (apiResponse.statusCode() != 200) {
            throw new IOException("API request failed: " + apiResponse.statusCode());
        }

        String json = apiResponse.body();

        // Step 3: Parse JSON to find .jar asset (regex fixed for field order: id -> name -> browser_download_url)
        // Matches the first asset where name ends with .jar
        Pattern assetPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+).*?\"name\"\\s*:\\s*\"([^\"]+\\.jar)\".*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = assetPattern.matcher(json);
        if (!matcher.find()) {
            throw new IOException("No .jar asset found in release");
        }

        long assetId = Long.parseLong(matcher.group(1));
        String jarName = matcher.group(2);
        String browserDownloadUrl = matcher.group(3);

        System.out.println("Latest JAR: " + jarName);
        System.out.println("Asset ID: " + assetId);
        System.out.println("Browser Download URL: " + browserDownloadUrl);

        // Step 4: Try asset API endpoint first (more reliable for binary)
        String assetApiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/assets/" + assetId;
        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(assetApiUrl))
                .header("User-Agent", "Java-App")
                .header("Accept", "application/octet-stream")  // Required for binary download
                .build();

        HttpResponse<InputStream> downloadResponse = client.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream());
        int status = downloadResponse.statusCode();

        // Handle potential 302 redirect manually
        if (status == 302) {
            Optional<String> locOpt = downloadResponse.headers().firstValue("Location");
            if (locOpt.isPresent()) {
                String location = locOpt.get();
                System.out.println("Following redirect to: " + location);
                HttpRequest followRequest = HttpRequest.newBuilder()
                        .uri(URI.create(location))
                        .header("User-Agent", "Java-App")
                        .header("Accept", "application/octet-stream")
                        .build();
                downloadResponse = client.send(followRequest, HttpResponse.BodyHandlers.ofInputStream());
                status = downloadResponse.statusCode();
            }
        }

        // Fallback to browser_download_url if asset API fails (e.g., 404 or auth issue)
        if (status != 200) {
            System.out.println("Asset API failed (" + status + "); falling back to browser URL");
            HttpRequest fallbackRequest = HttpRequest.newBuilder()
                    .uri(URI.create(browserDownloadUrl))
                    .header("User-Agent", "Java-App")
                    .build();
            downloadResponse = client.send(fallbackRequest, HttpResponse.BodyHandlers.ofInputStream());
            status = downloadResponse.statusCode();
            
            if (status == 302) {
                Optional<String> locOpt = downloadResponse.headers().firstValue("Location");
                if (locOpt.isPresent()) {
                    String location = locOpt.get();
                    System.out.println("Following fallback redirect to: " + location);
                    HttpRequest followRequest = HttpRequest.newBuilder()
                            .uri(URI.create(location))
                            .header("User-Agent", "Java-App")
                            .build();
                    downloadResponse = client.send(followRequest, HttpResponse.BodyHandlers.ofInputStream());
                    status = downloadResponse.statusCode();
                }
            }
            
            if (status != 200) {
                throw new IOException("Download failed after fallback: " + status);
            }
        }

        Path outputPath = Paths.get(outputDir).resolve(jarName);
        Files.copy(downloadResponse.body(), outputPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Downloaded to: " + outputPath.toAbsolutePath());
    }
}
