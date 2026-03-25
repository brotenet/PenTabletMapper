package application.controllers;

import java.util.ArrayList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import openjfx.os.linux.LinuxClient;

public class FrmMain {
	
	@FXML private BorderPane rootPane;
	@FXML private TextArea txtLog;
	@FXML private ListView<String> lvPointers;
	@FXML private ListView<String> lvDisplays;
	@FXML private Button btnApply;
	
	@FXML
	public void initialize() {

		try {
			txtLog.setPrefHeight(200);			
			ArrayList<String> shell_output = LinuxClient.bash(
				"xinput list --name-only | grep -E 'Pointer|pointer|Mouse|mouse|Pen|pen'",
				"xrandr | grep \" connected\" | awk '{print $1}'"
			);
			String[] pointer_options = shell_output.get(0).split("\n");
			if(pointer_options.length > 0) { for(String option : pointer_options) lvPointers.getItems().add(option.trim()); }
			String[] display_options = shell_output.get(1).split("\n");
			if(display_options.length > 0) { lvDisplays.getItems().add("all"); for(String option : display_options) lvDisplays.getItems().add(option.trim()); }
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	@FXML
	private void applyPointerRedirect() throws Exception {
		if(lvPointers.getSelectionModel().getSelectedItems().size() > 0 && lvDisplays.getSelectionModel().getSelectedItems().size() > 0) {
			String selected_pointer = "\"" + lvPointers.getSelectionModel().getSelectedItem().trim() + "\"";
			String selected_display = "\"" + lvDisplays.getSelectionModel().getSelectedItem().trim() + "\"";			
			String bash_command = "xinput map-to-output " + selected_pointer + " " + selected_display;
			LinuxClient.bash(bash_command);
			txtLog.setText(selected_pointer + " redirected to " + selected_display + " using:\n");
			txtLog.setText(txtLog.getText() + bash_command);
		}else {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Invalid User Input");
			alert.setHeaderText("\nPlease select a pointer device and one or more\ndisplay device(s) before applying.\n  ");
			alert.show();
		}
	}
}
