package net.filebot.ui;

import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.util.ui.SwingUI.*;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import net.filebot.ResourceManager;

public class GettingStartedStage {

	public static void start() {
		invokeJavaFX(() -> {
			// libjfxwebkit.dylib cannot be deployed on the MAS due to deprecated dependencies
			if (isAppStore()) {
				ask();
			} else {
				create().show();
			}
		});
	}

	private static void ask() {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("FileBot");
		alert.setHeaderText("Hello! Do you need help Getting Started?");
		alert.setContentText("If you have never used FileBot before, please have a look at the video tutorials first.");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK) {
			openURI(getEmbeddedHelpURL());
		}
	}

	private static GettingStartedStage create() {
		Stage stage = new Stage();
		stage.setResizable(true);

		if (isWindowsApp()) {
			stage.getIcons().setAll(ResourceManager.getApplicationIconResources().map(URL::toString).map(Image::new).toArray(Image[]::new));
			stage.initStyle(StageStyle.DECORATED);
			stage.initModality(Modality.NONE);
		} else {
			stage.initStyle(StageStyle.UTILITY);
			stage.initModality(Modality.NONE);
		}

		return new GettingStartedStage(stage);
	}

	private Stage stage;

	public GettingStartedStage(Stage stage) {
		this.stage = stage;

		WebView webview = new WebView();
		webview.getEngine().load(getEmbeddedHelpURL());
		webview.setPrefSize(750, 490);

		// intercept target _blank click events and open links in a new browser window
		webview.getEngine().setCreatePopupHandler(c -> onPopup(webview));

		webview.getEngine().getLoadWorker().stateProperty().addListener((v, o, n) -> {
			if (n == Worker.State.SUCCEEDED) {
				stage.setTitle(webview.getEngine().getTitle());
				stage.toFront();
				webview.requestFocus();
			} else if (n == Worker.State.FAILED) {
				stage.close();
			}
		});

		stage.setTitle("Loading …");
		stage.setScene(new Scene(webview, webview.getPrefWidth(), webview.getPrefHeight(), Color.BLACK));

		// force black background while page is loading
		setBackground(webview.getEngine(), 0xFF000000);

		// make sure that we can read the user locale in JS
		webview.getEngine().executeScript(String.format("navigator.locale = '%s'", Locale.getDefault()));
	}

	public void show() {
		stage.setOpacity(0.0);
		stage.show();

		Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(stage.opacityProperty(), 1.0, Interpolator.EASE_IN)));
		timeline.setOnFinished((evt) -> {
			stage.setOpacity(1.0);
			stage.requestFocus();
		});
		timeline.play();
	}

	protected void setBackground(WebEngine engine, int color) {
		try {
			// use reflection to retrieve the WebEngine's private 'page' field
			Field f = engine.getClass().getDeclaredField("page");
			f.setAccessible(true);
			com.sun.webkit.WebPage page = (com.sun.webkit.WebPage) f.get(engine);
			page.setBackgroundColor(color);
		} catch (Exception e) {
			debug.log(Level.WARNING, "Failed to set background", e);
		}
	}

	protected WebEngine onPopup(WebView webview) {
		// get currently select image via Galleria API
		Object uri = webview.getEngine().executeScript("$('.galleria').data('galleria').getData().link");
		openURI(uri.toString());

		// prevent current web view from opening the link
		return null;
	}

}
