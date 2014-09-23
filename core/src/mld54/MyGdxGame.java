package mld54;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class MyGdxGame extends ApplicationAdapter {
	private static final String YOU_WIN = "You win!";
	private static final String PRESS_SPACE_TO_RESTART = "Press SPACE to restart";
	private static int ROWS = 200;
	private SpriteBatch batch;
	private float time;

	private float[] left = new float[ROWS];
	private float[] right = new float[ROWS];
	private Color[] innerColor = new Color[ROWS];
	private ImmediateModeRenderer renderer;
	private Matrix4 projectionMatrix;

	private int width;
	private int height;
	private float middle;
	private Texture bombTexture;
	private Texture explosionTexture;
	private Texture planetTexture;
	private Texture starsTexture;
	private BitmapFont font;

	private float progress;
	private float tunnelX;
	private float opening;
	private float speed;
	private float depth;
	private float playerX;
	private float playerY;
	private float playerVelX;
	private float playerSize;
	private boolean playerDied;
	private Vector2 explosion;
	private float playerDeadTime;
	private boolean playerWins;
	private Music music;
	private Vector3[] tracers = new Vector3[10];
	private Texture tracerTexture;
	private Sound explosionSound;
	private Sound winExplosionSound;
	private boolean winExplosionSounded;
	private float accelUsed;

	@Override
	public void create() {
		projectionMatrix = new Matrix4();
		projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		renderer = new ImmediateModeRenderer20(1000, false, true, 0);
		batch = new SpriteBatch();
		font = new BitmapFont(Gdx.files.internal("sprites/default.fnt"));

		bombTexture = new Texture(Gdx.files.internal("sprites/bomb.png"));
		explosionTexture = new Texture(Gdx.files.internal("sprites/expl.png"));
		starsTexture = new Texture(Gdx.files.internal("sprites/stars.png"));
		planetTexture = new Texture(Gdx.files.internal("sprites/planet.png"));
		tracerTexture = new Texture(Gdx.files.internal("sprites/tracer.png"));

		music = Gdx.audio.newMusic(Gdx.files.internal("audio/background.mp3"));
		explosionSound = Gdx.audio.newSound(Gdx.files
				.internal("audio/explosion.wav"));
		winExplosionSound = Gdx.audio.newSound(Gdx.files
				.internal("audio/winExplosion.wav"));

		middle = Gdx.graphics.getWidth() / 2f;
		height = Gdx.graphics.getHeight();
		width = Gdx.graphics.getWidth();

		reset();
	}

	private void reset() {
		explosion = null;
		playerDeadTime = 0;
		playerDied = false;
		playerWins = false;
		opening = width * 0.5f;
		tunnelX = middle;

		for (int i = 0; i < ROWS; i++) {
			left[i] = 0;
			right[i] = width;
			innerColor[i] = new Color(1f, 1f, 1f, 1);
		}

		playerX = middle;
		playerY = height * 3 / 4;
		playerSize = width / 20;
		depth = -ROWS;
		playerVelX = 0;
		speed = 200;
		progress = 0;
		time = 0;
		accelUsed = 0;
		music.play();

		for (int i = 0; i < tracers.length; i++) {
			tracers[i] = new Vector3(0, 0, 0);
			placeTracerAtRandomLocation(tracers[i]);
		}
		winExplosionSounded = false;
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		float deltaTime = Gdx.graphics.getDeltaTime();
		update(deltaTime);

		batch.begin();
		for (int i = 0; i < tracers.length; i++) {
			batch.draw(tracerTexture, tracers[i].x, tracers[i].y);
		}

		if (!playerDied) {
			float rot = playerVelX / 10;
			batch.draw(bombTexture, playerX - playerSize / 2, playerY
					- playerSize / 2, playerSize / 2, playerSize / 2,
					playerSize, playerSize, 1, 1, rot, 0, 0,
					bombTexture.getWidth(), bombTexture.getHeight(), false,
					false);
		}
		batch.end();

		if (!playerWins) {
			renderTunnel();
		}

		batch.begin();
		if (playerDied) {
			renderWinningOrLosing();
		}
		if (depth < 5000) {
			String depthFormatted = String.format("%.2f", depth);
			font.draw(batch, "Depth: " + depthFormatted + "m", 20,
					font.getLineHeight() + 20);
		} else {
			String depthFormatted = String.format("%.1f", depth / 1000);
			font.draw(batch, "Depth: " + depthFormatted + "km", 20,
					font.getLineHeight() + 20);
		}
		String thrustUsed = String.format("%.2f", accelUsed);
		font.draw(batch, "Fuel used: " + thrustUsed + " kg", 20,
				font.getLineHeight() * 2f + 20);
		batch.end();
	}

	private void renderTunnel() {
		float heightPerRow = height / (ROWS - 1f);
		renderer.begin(projectionMatrix, GL20.GL_TRIANGLE_STRIP);
		for (int i = 0; i < ROWS; i++) {
			renderer.color(innerColor[i]);
			float y = i * heightPerRow;
			renderer.vertex(0, y, 0);
			renderer.color(innerColor[i]);
			renderer.vertex(left[i], y, 0);
		}
		renderer.flush();
		for (int i = 0; i < ROWS; i++) {
			renderer.color(innerColor[i]);
			float y = i * heightPerRow;
			renderer.vertex(width, y, 0);
			renderer.color(innerColor[i]);
			renderer.vertex(right[i], y, 0);
		}
		renderer.end();
	}

	private void renderWinningOrLosing() {
		if (playerWins) {
			batch.draw(starsTexture, 0, 0, width, height);
			float scale = 3 / (float) Math.log(playerDeadTime + 10);
			if (playerDeadTime < 8) {
				if (playerDeadTime > 6) {
					batch.setColor(1, 1, 1, 1 - (playerDeadTime - 4) / 4);
				}
				batch.draw(planetTexture,
						middle - planetTexture.getWidth() / 2, height / 2
								- planetTexture.getHeight() / 2,
						planetTexture.getWidth() / 2,
						planetTexture.getHeight() / 2,
						planetTexture.getWidth(), planetTexture.getHeight(),
						scale, scale, playerDeadTime, 0, 0,
						planetTexture.getWidth(), planetTexture.getHeight(),
						false, false);
				batch.setColor(1, 1, 1, 1);
			}
			if (playerDeadTime > 6 && playerDeadTime < 16) {
				if (!winExplosionSounded) {
					winExplosionSounded = true;
					winExplosionSound.play();
				}
				batch.setColor(1, 1, 1, 1 - (playerDeadTime - 6) / 10);
				batch.draw(explosionTexture,
						middle - explosionTexture.getWidth() / 2, height / 2
								- explosionTexture.getHeight() / 2,
						explosionTexture.getWidth() / 2,
						explosionTexture.getHeight() / 2,
						explosionTexture.getWidth(),
						explosionTexture.getHeight(), playerDeadTime / 2,
						playerDeadTime / 2, playerDeadTime * 10, 0, 0,
						explosionTexture.getWidth(),
						explosionTexture.getHeight(), false, false);
				batch.setColor(1, 1, 1, 1);
			}
			if (playerDeadTime > 16) {
				if (playerDeadTime < 20) {
					font.setColor(1, 1, 1, (playerDeadTime - 16) / 4);
					font.setScale((playerDeadTime - 16) / 2);
				} else {
					font.setScale(2);
				}
				TextBounds bounds = font.getBounds(YOU_WIN);
				font.draw(batch, YOU_WIN, (width - bounds.width) / 2,
						(height - bounds.height) / 2);

				font.setColor(1, 1, 1, 1);
				font.setScale(1);
			}

		} else if (playerDeadTime < 4) {
			batch.setColor(1, 1, 1, 1 - playerDeadTime / 4);
			batch.draw(explosionTexture,
					explosion.x - explosionTexture.getWidth() / 2, explosion.y
							- explosionTexture.getHeight() / 2,
					explosionTexture.getWidth() / 2,
					explosionTexture.getHeight() / 2,
					explosionTexture.getWidth(), explosionTexture.getHeight(),
					playerDeadTime, playerDeadTime, playerDeadTime * 20, 0, 0,
					explosionTexture.getWidth(), explosionTexture.getHeight(),
					false, false);
			batch.setColor(1, 1, 1, 1);
		}
		TextBounds bounds = font.getBounds(PRESS_SPACE_TO_RESTART);
		font.draw(batch, PRESS_SPACE_TO_RESTART, (width - bounds.width) / 2,
				(height + bounds.height) / 2);
	}

	private void update(float deltaTime) {
		time += deltaTime;

		if (playerDied) {
			playerDeadTime += deltaTime;
			if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
				reset();
			}
			return;
		}

		if (speed < 400) {
			speed = Math.min(400, speed + deltaTime * 50);
		}

		for (int i = 0; i < tracers.length; i++) {
			tracers[i].y += deltaTime * speed * tracers[i].z;
			if (tracers[i].y > height) {
				placeTracerAtRandomLocation(tracers[i]);
			}
		}
		depth += deltaTime * speed;
		if (opening > 0) {
			opening -= deltaTime * 7;
		}
		progress += deltaTime * speed;

		int t_progress = (int) progress;
		if (t_progress > 0) {
			progress -= t_progress;
			System.arraycopy(left, 0, left, t_progress, left.length
					- t_progress);
			System.arraycopy(right, 0, right, t_progress, right.length
					- t_progress);
			Color[] tmp = new Color[t_progress];
			System.arraycopy(innerColor, innerColor.length - t_progress - 1,
					tmp, 0, t_progress);
			System.arraycopy(innerColor, 0, innerColor, t_progress,
					innerColor.length - t_progress);
			System.arraycopy(tmp, 0, innerColor, 0, t_progress);
			float nextPos = middle;
			float maxPos = middle - opening / 2;
			for (float tf = 1f, scale = 0.43f; tf < 200; tf *= 1.73, scale *= 0.7) {
				nextPos += Math.sin(time * tf) * maxPos * scale;
			}
			if (depth > 11800) {
				opening = 0;
				if (depth > 12000) {
					playerWins();
				}
			}
			double logDepth = Math.log(depth + 500);

			double colorFunc = Math.sin(time) + 1;
			colorFunc += Math.sin(time * 3) / 3;
			colorFunc += Math.sin(time * 37) / 5;
			colorFunc += Math.sin(time * 117) / 8;
			float color = (float) (colorFunc / logDepth) * 0.6f + 0.3f;
			for (int i = 0; i < t_progress; i++) {
				float sampled_pos = (nextPos - tunnelX) * (t_progress - i)
						/ t_progress + tunnelX;
				left[i] = sampled_pos - opening / 2;
				right[i] = sampled_pos + opening / 2;
				innerColor[i].set(color, color, color, 1);
			}
			tunnelX = nextPos;
		}
		float maxSpeed = 600;
		float accel = 2500;
		float dampening = 200;
		if (Gdx.input.isKeyPressed(Input.Keys.A)
				|| Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			float oldPlayerVelX = playerVelX;
			playerVelX = Math.max(-maxSpeed, playerVelX - deltaTime * accel);
			accelUsed += Math.abs(playerVelX - oldPlayerVelX) / 50;
		} else if (Gdx.input.isKeyPressed(Input.Keys.D)
				|| Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			float oldPlayerVelX = playerVelX;
			playerVelX = Math.min(maxSpeed, playerVelX + deltaTime * accel);
			accelUsed += Math.abs(playerVelX - oldPlayerVelX) / 50;
		} else if (Math.abs(playerVelX) > 0.000001f) {
			float velXSize = Math.abs(playerVelX);
			float delta = playerVelX / velXSize * deltaTime * dampening;
			if (delta > velXSize) {
				playerVelX = 0;
			} else {
				playerVelX -= delta;
			}
		}

		playerX += playerVelX * deltaTime;
		checkForPlayerCollision();
	}

	private void placeTracerAtRandomLocation(Vector3 tracer) {
		tracer.set((float) (Math.random() * width),
				(float) (Math.random() * -20), (float) (2 + Math.random() * 4));
	}

	private void playerWins() {
		playerWins = true;
		playerDied();
	}

	private void checkForPlayerCollision() {
		if (playerDied) {
			return;
		}
		boolean collides = false;
		float heightPerRow = height / (ROWS - 1f);
		int startY = (int) (playerY / heightPerRow) - 1;
		float endY = (playerY + playerSize) / heightPerRow - 1;
		for (int i = startY; i <= endY; i++) {
			if (left[i] > playerX || right[i] < playerX) {
				collides = true;
				break;
			}
		}
		if (collides) {
			playerDied();
			playerExploded();
		}
	}

	private void playerExploded() {
		explosion = new Vector2(playerX, playerY);
		explosionSound.play();
	}

	private void playerDied() {
		music.stop();
		playerDied = true;
	}

}
