import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Main class for Pretzel clicker game. This class contains main, and all of the
 * methods required to run the pretzel clicker game.
 * 
 * @author Quentyn May
 * @author Andrew Giacalone
 *
 */
public class Main extends Application implements Initializable {

	@FXML
	Text pretzelText = new Text(String.format("Pretzels: ,%f", Player.getPretzels()));
	@FXML
	Text PPSText;
	@FXML
	Button clickerButton = Clicker.getGraphic();
	@FXML
	ImageView pretzelImage;
	@FXML
	Text playerStats = new Text(Player.getStats());
	@FXML
	Button saveButton;
	@FXML
	Button resetButton;
	@FXML
	Button muteButton;
	@FXML 
	Button upgradeClicker1;

	boolean mute = false;

	boolean leftWasClicked = false; // This is used to only left click pretzel once

	public Clip music = playSound("BackgroundMusic.wav");

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		loadSave(new Scanner(new File("Save")));

		Parent p = FXMLLoader.load(getClass().getResource("PretzelClickerFXML.fxml"));

		Scene root = new Scene(p);
		primaryStage.setScene(root);
		primaryStage.setTitle("Pretzel Clicker");
		primaryStage.getIcons().add(new Image("PretzelImage.png"));
		primaryStage.setResizable(false);
		primaryStage.show();

	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		music.start();
		
		// Loop the music, only if the sounds are not muted
		Timeline musicTime = new Timeline(new KeyFrame(Duration.seconds(46.35), e -> {
			if (music != null)
				if (!mute)
					music = playSound("BackgroundMusic.wav");
			music.start();
		}));
		musicTime.setCycleCount(Animation.INDEFINITE);
		musicTime.play();

		// Running a time-line to add pretzels every 0.01 seconds
		// base on the current PPS, using 0.01 seconds to make the pretzels
		// go up smoothly over time
		Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0.1), e -> {
			Player.updatePretzels(Player.getPPS() / 10);
			Player.updateTotalPretzels(Player.getPPS() / 10);
			updatePretzels();
			updatePPS();
			playerStats.setText(Player.getStats());
			clickerButton
					.setText(String.format("%d Clickers%nCost: %,.0f", Clicker.getNumClickers(), Clicker.getCost()));
		}));

		// Have the time-line run indefinitely and start it
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.play();

		pretzelImage.setOnMousePressed((e) -> { // Clicking the cookie

			if (e.isPrimaryButtonDown()) { // Check to see if the click was a left click

				if (!leftWasClicked) { // Make sure only one click is being registered
					
					Player.updatePretzels(Player.getClickValue());
					Player.updateTotalPretzels(Player.getClickValue());
					pretzelImage.setScaleX(pretzelImage.getScaleX() + 0.1);
					pretzelImage.setScaleY(pretzelImage.getScaleY() + 0.1);
					updatePretzels();

					Clip click = playSound("ClickSound.wav");
					if (click != null)
						click.start();

					// Used for when you release the mouse, tells the method that
					// it was the left mouse that was clicked
					leftWasClicked = true;
				}
			}
		});

		makeToolTip(upgradeClicker1, String.format("Doubles clickers PPS and click value%nCost: %d", Clicker.getUpgradeCost()));
		upgradeClicker1.setOnAction(e -> {
			if(Player.getPretzels() >= Clicker.getUpgradeCost()) {
				Player.updatePretzels(-Clicker.getUpgradeCost());
				new Clicker(1);
				makeToolTip(upgradeClicker1, String.format("Doubles clickers PPS and click value%nCost: %d", Clicker.getUpgradeCost()));
			}
		});

		pretzelImage.setOnMouseReleased((e) -> { // Releasing click on the cookie
			if (leftWasClicked) { // Check to see if it was the left mouse that was released
				pretzelImage.setScaleX(pretzelImage.getScaleX() - 0.1);
				pretzelImage.setScaleY(pretzelImage.getScaleY() - 0.1);
				leftWasClicked = false;
			}
		});

		clickerButton.setOnAction(e -> { // Buying a clicker
			if (Player.getPretzels() >= Clicker.getCost()) {
				new Clicker();

				updatePretzels();
				clickerButton.setText(
						String.format("%d Clickers%nCost: %,.0f", Clicker.getNumClickers(), Clicker.getCost()));
				Clip click = playSound("clickDown.wav");
				if (click != null)
					click.start();
			}
		});

		muteButton.setOnAction(e -> {
			if (mute) {
				mute = false;
				muteButton.setText("Mute");
				music.start();
			} else {
				mute = true;
				muteButton.setText("Unmute");
				music.stop();
			}
		});

		saveButton.setOnAction(e -> { // This is used to save stats to a save file
			saveGame();
			saveButton.setText("Saved!");
			Clip click = playSound("clickDown.wav");
			if (click != null)
				click.start();
		});

		resetButton.setOnAction(e -> { // Sets stats to BlankSave and saves the game
			try {
				loadSave(new Scanner(new File("BlankSave")));
				saveGame();
				Clip click = playSound("clickDown.wav");
				if (click != null)
					click.start();
			} catch (FileNotFoundException e1) {
			}
		});

	}

	/**
	 * This method takes in a string that contains the path to a .WAV file and
	 * converts it to a Clip object that can be played. 
	 * @param string
	 * @return Clip clip
	 */
	private Clip playSound(String string) {
		if (!mute) {
			try {

				// Open an audio input stream.
				URL url = this.getClass().getClassLoader().getResource(string);
				AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
				// Get a sound clip resource.
				Clip clip = AudioSystem.getClip();
				// Open audio clip and load samples from the audio input stream.
				clip.open(audioIn);

				return clip;
			} catch (UnsupportedAudioFileException f) {
			} catch (IOException g) {
			} catch (LineUnavailableException h) {
			}
		}
		return null;
	}

	/**
	 * This method is used for loading the save when you open the game. It takes in
	 * as scanner that reads from a save file, and sets all saved stats to all
	 * applicable stats, such as player pretzels and player PPS.
	 * 
	 * @param Scanner s
	 */
	private void loadSave(Scanner s) {
		Player.setPretzels(Double.parseDouble(s.nextLine()));
		Player.setTotalPretzels(Double.parseDouble(s.nextLine()));
		Player.setClickValue(Double.parseDouble(s.nextLine()));
		Player.setBuildings((int) Double.parseDouble(s.nextLine()));
		Player.setUpgrades((int) Double.parseDouble(s.nextLine()));
		Player.setPPS(Double.parseDouble(s.nextLine()));
		Clicker.setPPS(Double.parseDouble(s.nextLine()));
		Clicker.setCost(Double.parseDouble(s.nextLine()));
		Clicker.setNumClickers((int) Double.parseDouble(s.nextLine()));
		Clicker.setMyClickerPPS(Double.parseDouble(s.nextLine()));
		Clicker.setUpgrades((int) Double.parseDouble(s.nextLine()));

	}

	/**
	 * This method creates a PrinterWriter and writes all current game stats to a
	 * file called "Save"
	 */
	private void saveGame() {
		try {
			PrintWriter writeSave = new PrintWriter(new File("Save"));
			writeSave.print(String.format("%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n%s%n", Player.getPretzels(),
					Player.getTotalPretzels(), Player.getClickValue(), Player.getBuildings(), Player.getUpgrades(),
					Player.getPPS(), Clicker.getPPS(), Clicker.getCost(), Clicker.getNumClickers(),
					Clicker.getMyClickerPPS(), Clicker.getUpgrades()));

			writeSave.close();
		} catch (FileNotFoundException e1) {
		}

	}

	/**
	 * Method for updating the text field for the pretzels. This method requests the
	 * focus for the text field that has the number of pretzels, and updates with
	 * the new number of pretzels. If the number of pretzels is >= 1 billion, the
	 * number of pretzels will be shortened to just xxx.xx format followed by the
	 * magnitude of the pretzels (example: 32.79 Trillion).
	 */
	public void updatePretzels() {
		if (Player.getPretzels() >= 1000000000000000000.0) {
			pretzelText.setText(
					String.format("Pretzels: %,.2f %nQuintillion", Player.getPretzels() / 1000000000000000000.0));
		} else if (Player.getPretzels() >= 1000000000000000.0) {
			pretzelText
					.setText(String.format("Pretzels: %,.2f %nQuadrillion", Player.getPretzels() / 1000000000000000.0));
		} else if (Player.getPretzels() >= 1000000000000.0) {
			pretzelText.setText(String.format("Pretzels: %,.2f %nTrillion", Player.getPretzels() / 1000000000000.0));
		} else if (Player.getPretzels() >= 1000000000.0) {
			pretzelText.setText(String.format("Pretzels: %,.2f %nBillion", Player.getPretzels() / 1000000000.0));
		} else
			pretzelText.setText(String.format("Pretzels: %,.0f", Player.getPretzels()));
	}

	/**
	 * Method for updating the text field for the PPS. This method requests the
	 * focus for the text field that has the PPS and updates with the new PPS
	 */
	public void updatePPS() {
		Player.setPPS(Clicker.getMyClickerPPS());
		PPSText.setText(String.format("PPS: %,.1f", Player.getPPS()));
	}

	/**
	 * Method for creating a Tool Tip for a button. This method takes in a button and
	 * a string you would like to put into the tool tip, and puts the tool tip onto
	 * the button
	 * 
	 * @param button
	 * @param s
	 */
	public void makeToolTip(Button button, String s) {
		final Tooltip toolTip = new Tooltip();
		toolTip.setText(s);
		button.setTooltip(toolTip);
	}
}
