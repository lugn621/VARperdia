package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class Create {
	private Button searchButton;
	private TextField search = new TextField();
	private Label create = new Label();
	private HBox searchBar;
	private VBox contents;
	private Label message = new Label();
	private Tab _tab;
	private int lineCount = 0;
	private String _term;
	private View _view;
	private String _name;
	private Popup _popup;
	private File _file;
	private final String EMPTY = "Empty";
	private final String VALID = "Valid";
	private final String DUPLICATE = "Duplicate";
	private final String INVALID = "Invalid";

	public Create(Tab tab, Popup popup) {
		_tab = tab;
		_popup = popup;
	}

	public void setView(View view) {
		_view = view;
	}

	public void setContents() {
		create.setText("Enter term to search for: ");
		create.setFont(new Font("Arial", 16));

		searchButton = new Button("Search");

		searchBar = new HBox(create, search, searchButton);
		searchBar.setSpacing(15);

		message.setFont(new Font("Arial", 14));

		searchButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				String reply = search.getText();
				searchTerm(reply);
			}
		});

		contents = new VBox(searchBar, message);
		contents.setPadding(new Insets(15,10,10,15));
		_tab.setContent(contents);
	}

	public void searchTerm(String term) {
		_popup.computeStagePopup();
		Task<Void> task = new Task<Void>() {
			@Override public Void call() {
				_file = new File ("text.txt");
				ProcessBuilder builder = new ProcessBuilder("wikit", term);
				try {
					Process process = builder.start();
					BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
					BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

					PrintWriter out = new PrintWriter(new FileWriter(_file));

					int exitStatus = process.waitFor();

					if (exitStatus == 0) {
						String line;
						while ((line = stdout.readLine()) != null) {
							out.println(line);
						}

						out.close();

						String[] cmd = {"sed", "-i", "s/[.] /&\\n/g", _file.toString()};
						ProcessBuilder editFile = new ProcessBuilder(cmd);
						Process edit = editFile.start();

						BufferedReader stdout2 = new BufferedReader(new InputStreamReader(edit.getInputStream()));
						BufferedReader stderr2 = new BufferedReader(new InputStreamReader(edit.getErrorStream()));

						int exitStatus2 = edit.waitFor();

						if (exitStatus2 == 0) {
							String line2;
							while ((line2 = stdout2.readLine()) != null) {
								System.out.println(line2);
							}
						} else {
							String line2;
							while ((line2 = stderr2.readLine()) != null) {
								System.err.println(line2);
							}
						}

					} else {
						String line;
						while ((line = stderr.readLine()) != null) {
							System.err.println(line);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Platform.runLater(new Runnable(){
					@Override public void run() {
						_popup.closeComputeStagePopup();
						try(BufferedReader fileReader = new BufferedReader(new FileReader(_file.toString()))){
							String line = fileReader.readLine();
							if(line.contains("not found :^(")) {
								message.setText("Search term is invalid, please try again with another search term.");
								setContents();
							} else {
								message.setText("");
								_term = term;
								displayLines(term);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				return null;
			}
		};
		new Thread(task).start();
	}

	public void displayLines(String reply) {
		BorderPane lineContents = new BorderPane();
		lineContents.setPadding(new Insets(15,10,10,15));
		lineContents.setMaxHeight(360);

		Label title = new Label("Results for \"" + reply + "\"");
		title.setFont(new Font("Arial", 16));
		//lineContents.setTop(title);

		Label prompt = new Label("How many lines do you want in your creation:");
		prompt.setFont(new Font("Arial", 14));
		TextField numberTextField = new TextField();
		// Allow only numbers to be entered into the text field.
		numberTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.matches("\\d*")) {
				numberTextField.setText(newValue.replaceAll("[^\\d]", ""));
			}
		});
		numberTextField.setMaxWidth(100);

		ListView<String> list = new ListView<String>();
		ObservableList<String> listLines = FXCollections.observableArrayList("");
		list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
//		try {
//			reader = new BufferedReader(new FileReader(_file.toString()));
//			String line = null;
//			int i = 1;
//			while((line = reader.readLine()) != null) {
//				listLines.add(i + ". " + line);
//				i++;
//			}
//			//listLines.remove(i-2);
//			lineCount = i;
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		list.setItems(listLines);
		//lineContents.setCenter(list);
		BorderPane.setMargin(list, new Insets(10,0,10,0));
		
		//text area
		HBox views= new HBox();
		
		TextArea textArea = new TextArea();
		textArea.setEditable(true);
		textArea.prefHeightProperty().bind(views.heightProperty());
		textArea.prefWidthProperty().bind(views.widthProperty().subtract(200));
		
		BufferedReader fileContent;
		try {
			fileContent = new BufferedReader(new FileReader(_file));
			String line;
			int i = 1;
			while ((line = fileContent.readLine()) != null) {
					textArea.appendText(line + "\n");
					i++;
				}
			fileContent.close();
			} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
		
		Label lblList = new Label("Saved audio");
		lblList.setFont(new Font("Arial", 16));
		
		VBox text = new VBox(title, textArea);
		VBox listView = new VBox(lblList, list);
		
		listView.setSpacing(10);
		text.setSpacing(10);
		
		views.getChildren().addAll(text, listView);
		//views.setPadding(new Insets(10));
		views.setSpacing(10);
		HBox.setHgrow(views, Priority.ALWAYS);

		Button butNum = new Button("Submit");
		butNum.setOnAction(e -> {
			String inNum = numberTextField.getText();
			try {
				int num = Integer.parseInt(inNum);
				if(getLines(num, reply)) {
					getName();
				}
			}catch(NumberFormatException | NullPointerException nfe) {
				_popup.showStage("", "Please enter an integer number. Would you like to continue?", "Yes", "No", false);
			}
		});
		
		ObservableList<String> voices = FXCollections.observableArrayList("Default", "Espeak");
		final ComboBox<String> combobox = new ComboBox<String>(voices);
		combobox.setValue("Default");
		Label lblVoice = new Label("Voice: ");
		Button butPlay = new Button("Play");
		Button butSave = new Button("Save");

		Slider slider = new Slider();
		slider.setMin(1);
		slider.setMax(10);
		slider.setValue(1);
		slider.setMajorTickUnit(1f);
		slider.isSnapToTicks();
		slider.setShowTickLabels(true);
		slider.setShowTickMarks(true);
		
		Label photos = new Label("Number of pictures");
		
		slider.valueProperty().addListener((obs, oldval, newVal) -> 
				slider.setValue(newVal.intValue()));
		
	
		HBox lineOptions = new HBox(lblVoice, combobox,  butPlay, butSave);
		lineOptions.setSpacing(15);
		
		VBox layout = new VBox(views, lineOptions,photos,slider);
		layout.setPadding(new Insets(10));
		layout.setSpacing(10);
		//lineContents.setBottom(lineOptions);
		_tab.setContent(layout);
		
		butPlay.setOnAction(e -> {
			Task<Void> task = new Task<Void>() {
				@Override
				protected Void call() throws Exception {				
					String voice;
					String selection = combobox.getSelectionModel().getSelectedItem();
					if ( selection.equals("Default")) {
						voice = "festival --tts";
					} else {
						voice = "espeak";
					}
					
					String command = "echo \"" + textArea.getSelectedText() + " \" | " + voice ;
					ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
					
					try {
						Process p = pb.start();
						BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
						int exitStatus = p.waitFor();
						
						if (exitStatus != 0) {
							String line2;
							while ((line2 = stderr.readLine()) != null) {
								System.err.println(line2);
							}
						}
						
					} catch (Exception e) {
						e.printStackTrace();
					}
					return null;
				}
			};
			new Thread(task).start();
		});
	}

//		butText.setOnAction(e -> {
//			_popup.editText();
//			list.setEditable(true);
//			list.setCellFactory(TextFieldListCell.forListView());
//			lineOptions.getChildren().removeAll(prompt, numberTextField, butNum, butPreview, butText);
//			lineOptions.getChildren().add(butDone);
//		});

//		butDone.setOnAction(e -> {
//			list.setEditable(false);
//			lineOptions.getChildren().remove(butDone);
//			lineOptions.getChildren().addAll(prompt, numberTextField, butNum, butPreview, butText);
//			try {
//				String fileName = _file.getName();
//				FileWriter fw = new FileWriter(fileName, false);
//				fw.write("");
//				fw.close();
//				fw = new FileWriter(fileName, true);
//				int count = 1;
//				for (String s: listLines) {
//					if (s.length() < 4) {
//						continue;
//					}
//					String newString= "";
//					if (count < 10) {
//						String[] sArray = s.substring(3).split("\\. ");
//						for (String st: sArray) {
//							if (st.endsWith(".")) {
//								newString += st + "\n";
//							} else {
//								newString += st + ".\n";
//							}
//						}
//					} else {
//						String[] sArray = s.substring(4).split("\\. ");
//						for (String st: sArray) {
//							if (st.endsWith(".")) {
//								newString += st + "\n";
//							} else {
//								newString += st + ".\n";
//							}
//						}
//					}
//					fw.write(newString);
//					count++;
//				}
//				fw.close();
//			} catch (IOException ioe){
//				ioe.getMessage();
//			}
//			displayLines(reply);
//		});
//
//		butPreview.setOnAction(e -> {
//			_popup.previewText(_file);
//		});
//	}

	public boolean getLines(int input, String reply) {
		if(input>=lineCount || input<=0) {
			_popup.showStage("", "Please enter a number between 1 and " + (lineCount-1), "OK", "Cancel", false);
			return false;
		} else {
			input++;
			if (input < lineCount && input > 1) {
				String[] cmd = {"sed", "-i",  input + ","+ lineCount + "d", _file.toString()};
				ProcessBuilder builder = new ProcessBuilder(cmd);
				try {
					Process process = builder.start();
					process.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else if (input == lineCount) {
				String[] cmd= {"sed", "-i", "$d", _file.toString()};
				ProcessBuilder builder = new ProcessBuilder(cmd);
				try {
					Process process = builder.start();
					process.waitFor();
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}
			return true;
		}
	}

	public void getName() {
		VBox cont;
		Button butNam = new Button("Create");

		Label cre = new Label("Enter name for your creation: ");
		cre.setFont(new Font("Arial", 16));
		TextField wordTextField = new TextField();
		// Disallow / and \0 characters which Ubuntu doesn't use for file names.
		wordTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			if ((newValue.contains("/")) || (newValue.contains("\0"))) {
				wordTextField.setText(oldValue);
			}
		});

		HBox nameBar = new HBox(cre, wordTextField, butNam);
		nameBar.setSpacing(15);

		Label mes = new Label();
		mes.setFont(new Font("Arial", 14));

		butNam.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				String reply = wordTextField.getText();
				String validity = checkName(reply);
				_name = reply;
				if (validity.equals(EMPTY)) {
					mes.setText("You haven't entered a creation name! Please try again.");
				} else if (validity.equals(VALID)) {
					mes.setText("");
					_name = reply;
					addCreation();
				} else if (validity.equals(DUPLICATE)) {
					_popup.showStage(_name, "Creation name already exists.\nWould you like to rename or overwrite?", "Rename", "Overwrite", false);
				}
				else if (validity.equals(INVALID)){
					mes.setText("Creation name contains invalid characters, please try again.");
				}
			}
		});

		cont = new VBox(nameBar, mes);
		cont.setPadding(new Insets(15,10,10,15));
		_tab.setContent(cont);
	}

	public String checkName(String reply) {
		File file = new File(reply + ".mp4");
		if(file.exists()) {
			return DUPLICATE;
		} else {
			String newName = reply.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
			if(newName == reply) {
				if (reply.isEmpty() == false) {
					return VALID;
				} else {
					return EMPTY;
				}	
			}else {
				return INVALID;
			}
		}
	}

	public void addCreation() {
		_popup.computeStagePopup();
		Task<Void> task = new Task<Void>() {
			@Override public Void call() {
				String cmd = "cat " + _file.toString() + " | text2wave -o temp.wav";
				ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", cmd);

				try {
					Process process = builder.start();
					process.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				String cMD = "ffmpeg -f lavfi -i color=c=blue:s=320x240:d=$(soxi -D temp.wav) -vf \"drawtext=fontsize=30:fontcolor=white:x=(w-text_w)/2:y=(h-text_h)/2:text=\'" + _term + "\'\" visual.mp4 &>/dev/null ; ffmpeg -i visual.mp4 -i temp.wav -c:v copy -c:a aac -strict experimental -y " + _name + ".mp4 &>/dev/null ; rm visual.mp4";
				ProcessBuilder builderr = new ProcessBuilder("/bin/bash", "-c", cMD);
				try {
					Process vidProcess = builderr.start();
					vidProcess.waitFor();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Platform.runLater(new Runnable(){
					@Override public void run() {
						_view.setContents();
						_popup.showFeedback(_name, false);
						setContents();
						_popup.closeComputeStagePopup();
					}
				});
				return null;
			}
		};
		new Thread(task).start();
	}

	public void removeCreation(String name) {
		File file = new File(name + ".mp4");
		file.delete();
	}
}
