package net.filebot.ui;

import static net.filebot.Settings.*;

import java.awt.Desktop;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import net.filebot.ResourceManager;
import net.filebot.Settings;

public class GettingStartedStage {

	public static void start() {
		// initialize JavaFX
		new JFXPanel();

		// initialize and show webview
		Platform.setImplicitExit(false);
		Platform.runLater(() -> {
			Stage stage = new Stage();
			stage.setResizable(false);

			if (isMacApp()) {
				// Mac OS X specific configuration
				stage.initStyle(StageStyle.DECORATED);
				stage.initModality(Modality.NONE);
			} else {
				// Windows / Linux specific configuration
				stage.initStyle(StageStyle.UTILITY);
				stage.initModality(Modality.NONE);
				stage.getIcons().addAll(ResourceManager.getApplicationIconURLs().stream().map((url) -> new Image(url.toString())).collect(Collectors.toList()));
			}

			GettingStartedStage view = new GettingStartedStage(stage);
			view.show();
		});
	}

	private Stage stage;

	public GettingStartedStage(Stage stage) {
		this.stage = stage;

		WebView webview = new WebView();
		webview.getEngine().load(Settings.getEmbeddedHelpURL());
		webview.setPrefSize(750, 490);

		// intercept target _blank click events and open links in a new browser window
		webview.getEngine().setCreatePopupHandler((config) -> onPopup(webview));

		webview.getEngine().getLoadWorker().stateProperty().addListener((v, o, n) -> {
			if (n == Worker.State.SUCCEEDED) {
				stage.setTitle(webview.getEngine().getTitle());
				stage.toFront();
				webview.requestFocus();
			} else if (n == Worker.State.FAILED) {
				stage.close();
			}
		});

		stage.setTitle("🚀 Loading …");
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
			Logger.getLogger(GettingStartedStage.class.getName()).log(Level.WARNING, "Failed to set background", e);
		}
	}

	protected WebEngine onPopup(WebView webview) {
		// get currently select image via Galleria API
		Object link = webview.getEngine().executeScript("$('.galleria').data('galleria').getData().link");

		try {
			Desktop.getDesktop().browse(new URI(link.toString()));
		} catch (Exception e) {
			Logger.getLogger(GettingStartedStage.class.getName()).log(Level.WARNING, "Failed to browse URI", e);
		}

		// prevent current web view from opening the link
		return null;
	}

}
