package com.conan.cai.tilt;

import java.io.IOException;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.AlphaModifier;
import org.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.MoveModifier;
import org.andengine.entity.modifier.SequenceEntityModifier;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.extension.physics.box2d.PhysicsConnectorWithOffset;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder.TextureAtlasBuilderException;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.Constants;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.SAXUtils;
import org.andengine.util.debug.Debug;
import org.andengine.util.level.IEntityLoader;
import org.andengine.util.level.LevelLoader;
import org.andengine.util.modifier.IModifier;
import org.xml.sax.Attributes;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.widget.Toast;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;

public class Tilt extends SimpleBaseGameActivity implements IAccelerationListener, ContactListener{
	
	/* Resolution */
	private static final int CAMERA_WIDTH = 1280;
	private static final int CAMERA_HEIGHT = 720;
	private Camera mCamera;
	private Scene scene;
	private Scene menu_scene;
	private Scene option_scene;
	private Scene level_scene;
	
	/* Level Result Textures */
	private Scene result_scene;
	private int tries;
	private  int time;
	private long start;
	private long end;
	private int total_time;
	private int total_tries;
	
	private Font mFont;
	
	/* Splash Textures */
	private BitmapTextureAtlas splashTextureAtlas;
	private ITextureRegion splashTextureRegion;
	private Scene splash_scene;
	
	/* Textures */
	private BitmapTextureAtlas mBitmapTextureAtlas;
	private BuildableBitmapTextureAtlas mAnimateBitmapTextureAtlas;
	
	private TiledTextureRegion mBallTextureRegion;
	private ITextureRegion mHoleTextureRegion;
	private ITextureRegion mH_LTextureRegion;
	private ITextureRegion mH_MTextureRegion;
	private ITextureRegion mH_RTextureRegion;
	private ITextureRegion mHTextureRegion;
	private ITextureRegion mV_BTextureRegion;
	private ITextureRegion mV_MTextureRegion;
	private ITextureRegion mV_TTextureRegion;
	private ITextureRegion mVTextureRegion;
	private ITextureRegion mGoalTextureRegion;
	 
	/* Physics */
	private PhysicsWorld mPhysicsWorld;
	private AnimatedSprite ball;
	private AnimatedSprite ball2;
	private Body ball_body;
	private Body ball_body2;
	
	/* Fixture Defs */
	private static final FixtureDef WALL_FIXTURE_DEF = PhysicsFactory.createFixtureDef(0, .3f, 0.5f, false);
	private static final FixtureDef BALL_FIXTURE_DEF = PhysicsFactory.createFixtureDef(0.1f, .3f, 0.5f, false);
	private static final FixtureDef HOLE_FIXTURE_DEF = PhysicsFactory.createFixtureDef(0, 0, 0, true);

	/* Options */
	private boolean hardcore = false;
	private boolean vibrate = true;
	private boolean music = true;
	
	/* Misc */
	private Sprite level_background;
	private Sprite options_background;
	private int num_balls;
	private int completed;
	private int count;
	private int currLevel;
	private String bg_select = "carbon.png";
	private int currScene;
	private final int menu_num = 0;
	private final int level_num = 1;
	private final int option_num = 2;
	private final int game_num = 3;
	private TimerHandler ball_timer;
	private TimerHandler ball_timer2;
	
	@Override
	public EngineOptions onCreateEngineOptions(){
		mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera);
	}
	
	@Override
	public void onCreateResources(){
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/splash/");
        splashTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 2048, 1024, TextureOptions.BILINEAR);
        splashTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(splashTextureAtlas, this, "menu.png", 0, 0);
        
        splashTextureAtlas.load();
	}
	
	@Override
	public Scene onCreateScene(){
		splash_scene = new Scene();
		
		Sprite splash = new Sprite(0, 0, splashTextureRegion, mEngine.getVertexBufferObjectManager());
    	Rectangle black = new Rectangle(0, 0, 1280, 720, mEngine.getVertexBufferObjectManager());
    	black.setColor(0, 0, 0);
		black.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    	black.registerEntityModifier(new AlphaModifier(1.75f, 1f, 0f, new IEntityModifierListener() {
    		 @Override
    		 public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
    			 //Main Menu
                 loadMenu();
                 
    		 }
    		 @Override
    		 public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) { 
    			 mEngine.setScene(menu_scene);
                 currScene = menu_num;
                 loadResources();
                 loadOptions();
                 loadLevelSelect();
    		 }
    	})
    	);
			
    	splash_scene.attachChild(splash);
		splash_scene.attachChild(black);
            
    	return splash_scene;
	}
	
	/*------------------------------------------------------------------------------------------------------------------*/
	/* Load Game Resources */
	public void loadResources(){
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/game/");
		this.mEngine.enableVibrator(this);
		
		mBitmapTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 512, 256, TextureOptions.BILINEAR);
		mAnimateBitmapTextureAtlas = new BuildableBitmapTextureAtlas(this.getTextureManager(), 512, 256, TextureOptions.BILINEAR);
		
		mV_BTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "v_b.png", 0, 0);
		mV_MTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "v_m.png", 26, 0);
		mV_TTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "v_t.png", 52, 0);
		mVTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "v.png", 78, 0);
		
		mH_LTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "h_l.png", 104, 0);
		mH_MTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "h_m.png", 104, 26);
		mH_RTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "h_r.png", 104, 52);
		mHTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "h.png", 104, 78);
		
		mHoleTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "hole.png", 0, 161);
		
		mGoalTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBitmapTextureAtlas, this, "goal.png", 81, 161);
		
		mBallTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mAnimateBitmapTextureAtlas, this, "ball_tile.png", 6, 2);
		
		mBitmapTextureAtlas.load();
		try {
			this.mAnimateBitmapTextureAtlas.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(0, 0, 1));
			this.mAnimateBitmapTextureAtlas.load();
		} catch (TextureAtlasBuilderException e) {
			Debug.e(e);
		}
	}
	
	/*------------------------------------------------------------------------------------------------------------------*/
	/* Load Level */
	public void loadLevel(String level){
		scene = new Scene();
		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
		currScene = game_num;
		completed = 0;
		ball = null;
		ball2 = null;
		/*------------------------------------------------------------------------------------------------------------------*/
		/* New Physics World */
		mPhysicsWorld = new PhysicsWorld(new Vector2(0,SensorManager.GRAVITY_EARTH), false);
		
		/*------------------------------------------------------------------------------------------------------------------*/
		/* Set the Background */
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/background/");

		BitmapTextureAtlas backgroundTextureAtlas = new BitmapTextureAtlas(getTextureManager(), 2048, 1024, TextureOptions.BILINEAR);
		ITextureRegion backgroundTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(backgroundTextureAtlas, this, bg_select, 0, 0);
		backgroundTextureAtlas.load();
		Sprite background = new Sprite(0, 0, backgroundTexture, vertexBufferObjectManager){
			 @Override
			    protected void preDraw(final GLState pGLState, final Camera pCamera)
			    {
			        super.preDraw(pGLState, pCamera);
			        pGLState.enableDither();
			    }
		};
		
		scene.attachChild(background);
		/*------------------------------------------------------------------------------------------------------------------*/
		/* Bounding Walls */
		final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT, CAMERA_WIDTH, 1, vertexBufferObjectManager);
		final Rectangle roof = new Rectangle(0, -1, CAMERA_WIDTH, 1, vertexBufferObjectManager);
		final Rectangle left = new Rectangle(-1, 0, 1, CAMERA_HEIGHT, vertexBufferObjectManager);
		final Rectangle right = new Rectangle(CAMERA_WIDTH, 0, 1, CAMERA_HEIGHT, vertexBufferObjectManager);

		Body ground_body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, WALL_FIXTURE_DEF);
		Body roof_body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof, BodyType.StaticBody, WALL_FIXTURE_DEF);
		Body left_body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, WALL_FIXTURE_DEF);
		Body right_body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, WALL_FIXTURE_DEF);
		
		ground_body.setUserData("y");
		roof_body.setUserData("y");
		left_body.setUserData("x");
		right_body.setUserData("x");

		scene.attachChild(ground);
		scene.attachChild(roof);
		scene.attachChild(left);
		scene.attachChild(right);
		
		/*------------------------------------------------------------------------------------------------------------------*/
		/* Level Loader */
		final LevelLoader levelLoader = new LevelLoader();
		levelLoader.setAssetBasePath("lvl/");
		
		levelLoader.registerEntityLoader("level", new IEntityLoader() {
			@Override
			public IEntity onLoadEntity(final String pEntityName, final Attributes pAttributes) {
				num_balls = SAXUtils.getIntAttributeOrThrow(pAttributes, "num_balls");
				return scene;
			}
		});
		
		levelLoader.registerEntityLoader("entity", new IEntityLoader() {
			
			@Override
			public IEntity onLoadEntity(final String pEntityName, final Attributes pAttributes) {
				final int x = SAXUtils.getIntAttributeOrThrow(pAttributes, "x");
				final int y = SAXUtils.getIntAttributeOrThrow(pAttributes, "y");
				final String type = SAXUtils.getAttributeOrThrow(pAttributes, "type");

				final Sprite face;
				
				//HORZ WALLS
				if(type.equals("h_l")) {
					face = new Sprite(x, y, Tilt.this.mH_LTextureRegion, vertexBufferObjectManager);
					Body wall_body = PhysicsFactory.createBoxBody(mPhysicsWorld, face, BodyType.StaticBody, WALL_FIXTURE_DEF);
					wall_body.setUserData("y");
					mPhysicsWorld.registerPhysicsConnector(new PhysicsConnectorWithOffset(face, wall_body, true, false,0, 0));
				} else if(type.equals("h_m")) {
					face = new Sprite(x, y, Tilt.this.mH_MTextureRegion, vertexBufferObjectManager);
					Body wall_body = PhysicsFactory.createBoxBody(mPhysicsWorld, face, BodyType.StaticBody, WALL_FIXTURE_DEF);
					wall_body.setUserData("y");
					mPhysicsWorld.registerPhysicsConnector(new PhysicsConnectorWithOffset(face, wall_body, true, false,0, 0));
				} else if(type.equals("h_r")) {
					face = new Sprite(x, y, Tilt.this.mH_RTextureRegion, vertexBufferObjectManager);
					Body wall_body = PhysicsFactory.createBoxBody(mPhysicsWorld, face, BodyType.StaticBody, WALL_FIXTURE_DEF);
					wall_body.setUserData("y");
					mPhysicsWorld.registerPhysicsConnector(new PhysicsConnectorWithOffset(face, wall_body, true, false,0, 0));
				} else if(type.equals("h")) {
					face = new Sprite(x, y, Tilt.this.mHTextureRegion, vertexBufferObjectManager);
					Body wall_body = PhysicsFactory.createBoxBody(mPhysicsWorld, face, BodyType.StaticBody, WALL_FIXTURE_DEF);
					wall_body.setUserData("y");
					mPhysicsWorld.registerPhysicsConnector(new PhysicsConnectorWithOffset(face, wall_body, true, false,0, 0));
				//VERT WALLS
				} else if(type.equals("v_m")) {
					face = new Sprite(x, y, Tilt.this.mV_MTextureRegion, vertexBufferObjectManager);
					Body wall_body = PhysicsFactory.createBoxBody(mPhysicsWorld, face, BodyType.StaticBody, WALL_FIXTURE_DEF);
					wall_body.setUserData("x");
					mPhysicsWorld.registerPhysicsConnector(new PhysicsConnectorWithOffset(face, wall_body, true, false,0, 0));
				} else if(type.equals("v_b")) {
					face = new Sprite(x, y, Tilt.this.mV_BTextureRegion, vertexBufferObjectManager);
					Body wall_body = PhysicsFactory.createBoxBody(mPhysicsWorld, face, BodyType.StaticBody, WALL_FIXTURE_DEF);
					wall_body.setUserData("x");
					mPhysicsWorld.registerPhysicsConnector(new PhysicsConnectorWithOffset(face, wall_body, true, false,0, 0));
				} else if(type.equals("v_t")) {
					face = new Sprite(x, y, Tilt.this.mV_TTextureRegion, vertexBufferObjectManager);
					Body wall_body = PhysicsFactory.createBoxBody(mPhysicsWorld, face, BodyType.StaticBody, WALL_FIXTURE_DEF);
					wall_body.setUserData("x");
				} else if(type.equals("v")) {
					face = new Sprite(x, y, Tilt.this.mVTextureRegion, vertexBufferObjectManager);
					Body wall_body = PhysicsFactory.createBoxBody(mPhysicsWorld, face, BodyType.StaticBody, WALL_FIXTURE_DEF);
					wall_body.setUserData("x");
					mPhysicsWorld.registerPhysicsConnector(new PhysicsConnectorWithOffset(face, wall_body, true, false,0, 0));
					mPhysicsWorld.registerPhysicsConnector(new PhysicsConnectorWithOffset(face, wall_body, true, false,0, 0));
				//HOLE
				} else if(type.equals("hole")) {
					boolean move_line = SAXUtils.getBooleanAttributeOrThrow(pAttributes, "move_line");
					boolean move_square = SAXUtils.getBooleanAttributeOrThrow(pAttributes, "move_square");

					face = new Sprite(x, y, Tilt.this.mHoleTextureRegion, vertexBufferObjectManager);
					float[] sceneCenterCoordinates = face.getSceneCenterCoordinates();
					Body hole_body = PhysicsFactory.createCircleBody(mPhysicsWorld, sceneCenterCoordinates[Constants.VERTEX_INDEX_X], sceneCenterCoordinates[Constants.VERTEX_INDEX_Y], 13f, BodyType.KinematicBody, HOLE_FIXTURE_DEF);
					hole_body.setUserData("hole");
					mPhysicsWorld.registerPhysicsConnector(new PhysicsConnectorWithOffset(face, hole_body, true, false, 0, 0));
					
					if(move_line == true){
						float startX = x;
						float endX = SAXUtils.getFloatAttributeOrThrow(pAttributes, "endX");
						float startY = y;
						float endY = SAXUtils.getFloatAttributeOrThrow(pAttributes, "endY");
						float duration = SAXUtils.getFloatAttributeOrThrow(pAttributes, "duration");
						
						face.registerEntityModifier(make_move_line(startX,endX,startY,endY,duration,hole_body));
					}
					
					if(move_square == true){
						float startX = x;
						float startY = y;
						float side = SAXUtils.getFloatAttributeOrThrow(pAttributes, "side");
						boolean direction = SAXUtils.getBooleanAttributeOrThrow(pAttributes, "clockwise");
						float duration = SAXUtils.getFloatAttributeOrThrow(pAttributes, "duration");
						
						face.registerEntityModifier(make_move_square(startX, startY, side, direction, duration, hole_body));
					}
				//GOAL
				} else if(type.equals("goal_text")) {
					face = new Sprite(x, y, Tilt.this.mGoalTextureRegion, vertexBufferObjectManager);
				} else if(type.equals("goal")) {
					face = new Sprite(x, y, Tilt.this.mHoleTextureRegion, vertexBufferObjectManager);
					float[] sceneCenterCoordinates = face.getSceneCenterCoordinates();
					Body hole_body = PhysicsFactory.createCircleBody(mPhysicsWorld, sceneCenterCoordinates[Constants.VERTEX_INDEX_X], sceneCenterCoordinates[Constants.VERTEX_INDEX_Y], 15f, BodyType.StaticBody, HOLE_FIXTURE_DEF);
					hole_body.setUserData("goal");
					mPhysicsWorld.registerPhysicsConnector(new PhysicsConnectorWithOffset(face, hole_body, true, false, 0, 0));
				//BALL
				} else if(type.equals("ball")) {
					face = new AnimatedSprite(x, y, Tilt.this.mBallTextureRegion, vertexBufferObjectManager);
					ball = (AnimatedSprite) face;
					ball.stopAnimation(0);
					float[] sceneCenterCoordinates = face.getSceneCenterCoordinates();
					ball_body = PhysicsFactory.createCircleBody(mPhysicsWorld, sceneCenterCoordinates[Constants.VERTEX_INDEX_X], sceneCenterCoordinates[Constants.VERTEX_INDEX_Y], 31.5f, BodyType.DynamicBody, BALL_FIXTURE_DEF);
					ball_body.setUserData(new Object[] {(x + 48.3f) / 32, (y + 47.8f) / 32, face});
					mPhysicsWorld.registerPhysicsConnector(new PhysicsConnectorWithOffset(ball, ball_body, true, false, 2.75f, -.2f));
					//BALL
				} else if(type.equals("ball2")) {
					face = new AnimatedSprite(x, y, Tilt.this.mBallTextureRegion, vertexBufferObjectManager);
					ball2 = (AnimatedSprite) face;
					ball2.stopAnimation(0);
					float[] sceneCenterCoordinates = face.getSceneCenterCoordinates();
					ball_body2 = PhysicsFactory.createCircleBody(mPhysicsWorld, sceneCenterCoordinates[Constants.VERTEX_INDEX_X], sceneCenterCoordinates[Constants.VERTEX_INDEX_Y], 31.5f, BodyType.DynamicBody, BALL_FIXTURE_DEF);
					ball_body2.setUserData(new Object[] {(x + 48.3f) / 32, (y + 47.8f) / 32, face});
					mPhysicsWorld.registerPhysicsConnector(new PhysicsConnectorWithOffset(ball2, ball_body2, true, false, 2.75f, -.2f));
				} else {
					throw new IllegalArgumentException();
				}

				return face;
			}
		});
		try {
			levelLoader.loadLevelFromAsset(getAssets(), level);
		} catch (final IOException e) {
			Debug.e(e);
		}
		
		/*------------------------------------------------------------------------------------------------------------------*/
		/* Return the scene */
		scene.registerUpdateHandler(this.mPhysicsWorld);
        mPhysicsWorld.setContactListener(this);	
	}
	
	
	/*------------------------------------------------------------------------------------------------------------------*/
	/* Ball Acceleration */
	@Override
	public void onAccelerationAccuracyChanged(final AccelerationData pAccelerationData) {
	}
	
	@Override
	public void onAccelerationChanged(final AccelerationData pAccelerationData) {
		if(this.mPhysicsWorld != null){
			final Vector2 gravity = Vector2Pool.obtain(pAccelerationData.getX()*10, pAccelerationData.getY()*10);
			this.mPhysicsWorld.setGravity(gravity);
			Vector2Pool.recycle(gravity);
		}
	}
	
	/*------------------------------------------------------------------------------------------------------------------*/
	/* System Stuff */ 
	@Override
	public void onResumeGame() {
		super.onResumeGame();

		this.enableAccelerationSensor(this);
	}

	@Override
	public void onPauseGame() {
		super.onPauseGame();

		this.disableAccelerationSensor();
	}
	
	@Override
	public void onBackPressed()
	{
	    if(currScene == menu_num)
	    	finish();
	    if(currScene == level_num){
	    	currScene = menu_num;
	    	mEngine.setScene(menu_scene);
	    }
	    if(currScene == option_num){
	    	currScene = menu_num;
	    	mEngine.setScene(menu_scene);
	    }
	    if(currScene == game_num){
	    	currScene = menu_num;
	    	mEngine.setScene(menu_scene);
	    }
	}
	
	/*------------------------------------------------------------------------------------------------------------------*/
	/* Body Collisions */
	@Override
	public void beginContact (final Contact contact){
		Object a = contact.getFixtureA().getBody().getUserData();
		final Body ball_ = contact.getFixtureB().getBody();
		final Body hole;
		float y = ball_.getLinearVelocity().y;
		float x = ball_.getLinearVelocity().x;
		if(a.equals("y"))
			mEngine.vibrate((long) (Math.abs(y)*1.5));
		if(a.equals("x"))
			mEngine.vibrate((long) (Math.abs(x)*1.5));
		if(a.equals("hole") && (Math.sqrt(Math.pow(y,2) + Math.pow(x,2)) < 35)){
			hole = contact.getFixtureA().getBody();
			tries++;
			resetBall(ball_, hole);
		}
		if(a.equals("goal") && (Math.sqrt(Math.pow(y,2) + Math.pow(x,2)) < 35)){
			completed++;
			Object[] temp = (Object[])ball_.getUserData();
			final AnimatedSprite sprite = (AnimatedSprite) temp[2];
			if(sprite==ball){
				mEngine.unregisterUpdateHandler(ball_timer);
			}
			if(sprite==ball2){
				mEngine.unregisterUpdateHandler(ball_timer2);
			}
			sprite.setVisible(true);
			runOnUpdateThread(new Runnable() {
			    @Override
			    public void run() {
					ball_.setTransform(contact.getFixtureA().getBody().getPosition(), 0);
					ball_.setActive(false);
					mEngine.vibrate(35);
					sprite.animate(new long[] {150,25,25,25,25,25,25,25,25}, 2, 10, false);
			    }
			});
			if(completed == num_balls){
				end = System.currentTimeMillis();
				tries++;
				time = (int) ((end - start)/1000); 
				mEngine.registerUpdateHandler(new TimerHandler(.5f, true, new ITimerCallback(){
					@Override
					public void onTimePassed(TimerHandler pTimerHandler) {
						nextLevel();
						getEngine().unregisterUpdateHandler(pTimerHandler);
					}
				}));
			}
		}
	}

	public void endContact (Contact contact){
		
	}

	public void preSolve (Contact contact, Manifold oldManifold){
	
	}

	public void postSolve (Contact contact, ContactImpulse impulse){
		
	}

	/*------------------------------------------------------------------------------------------------------------------*/
	/* mmmmm Toast! */
	public void gameToast(final String msg) {
	    this.runOnUiThread(new Runnable() {
	        @Override
	        public void run() {
	           Toast.makeText(Tilt.this, msg, Toast.LENGTH_SHORT).show();
	        }
	    });
	}
	
	/*------------------------------------------------------------------------------------------------------------------*/
	/* Reset Ball */
	public void resetBall(final Body ball_, final Body hole){
		count = 0;
		if(ball_==ball_body){
			mEngine.unregisterUpdateHandler(ball_timer);
			ball.setVisible(true);
		}
		if(ball_==ball_body2){
			mEngine.unregisterUpdateHandler(ball_timer2);
			ball2.setVisible(true);
		}
		Object[] userdata = (Object[]) ball_.getUserData();
		final float x = (Float) userdata[0];
		final float y = (Float) userdata[1];
		final AnimatedSprite sprite = (AnimatedSprite) userdata[2];
		runOnUpdateThread(new Runnable() {
		    @Override
		    public void run() {
				ball_.setTransform(hole.getPosition(), 0);
				ball_.setActive(false);
				mEngine.vibrate(35);
				sprite.animate(new long[] {150,25,25,25,25,25,25,25,25}, 2, 10, false);
		    }
		});
		mEngine.registerUpdateHandler(new TimerHandler(.025f,true,new ITimerCallback(){
			@Override
			public void onTimePassed(TimerHandler pTimerHandler){
				runOnUpdateThread(new Runnable() {
				    @Override
				    public void run() {
				    	ball_.setTransform(hole.getPosition(),0);
				    }
				});
				count++;
				if (count >= 14)
					mEngine.unregisterUpdateHandler(pTimerHandler);
			}
		}));
		mEngine.registerUpdateHandler(new TimerHandler(.5f, new ITimerCallback(){
			@Override
	        public void onTimePassed(TimerHandler pTimerHandler) {
				runOnUpdateThread(new Runnable() {
				    @Override
				    public void run() {
						sprite.stopAnimation(0);
				    	ball_.setTransform(x,y,0);
						ball_.setLinearVelocity(0, 0);
				    }
				});
				getEngine().unregisterUpdateHandler(pTimerHandler);
				mEngine.registerUpdateHandler(new TimerHandler(.5f, new ITimerCallback(){
					@Override
					public void onTimePassed(TimerHandler pTimerHandler) {
						if(hardcore){
                            if(ball_==ball_body){
                                ball_blink(ball);
                            }
                            if(ball_==ball_body2){
                                ball_blink(ball2);
                            }
                        }
						ball_.setActive(true);
						getEngine().unregisterUpdateHandler(pTimerHandler);
					}
				}));
			}
		}));
	}
	/*------------------------------------------------------------------------------------------------------------------*/
	/* Load Menu */
	public void loadMenu(){
		menu_scene = new Scene();
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/menu/");
		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
		
		BitmapTextureAtlas menuTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 2048, 1024, TextureOptions.BILINEAR);
		
		ITextureRegion mStartTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(menuTextureAtlas, this, "menu_start.png", 0, 0);
		ITextureRegion mLevelTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(menuTextureAtlas, this, "menu_levels.png", 0, 145);
		ITextureRegion mOptionsTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(menuTextureAtlas, this, "menu_options.png", 0, 290);
		ITextureRegion mBackgroundTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(menuTextureAtlas, this, "carbon.png", 428, 0);

		menuTextureAtlas.load();
		
		Sprite start_button = new Sprite(423, 71, mStartTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(1,true);
				}
				return true;
			}
		};
        
		Sprite level = new Sprite(423, 287, mLevelTexture, vertexBufferObjectManager){
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					currScene = level_num;
					mEngine.setScene(level_scene);
				}
				return true;
			}
		};
		
		Sprite option = new Sprite(423, 504, mOptionsTexture, vertexBufferObjectManager){
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					Intent option = new Intent(Tilt.this, Options.class);
	            	Tilt.this.startActivityForResult(option,1234);
					//currScene = option_num;
					//mEngine.setScene(option_scene);	
				}
				return true;
			}
		};
		
		Sprite menu_background = new Sprite(0, 0, mBackgroundTexture, vertexBufferObjectManager){
			 @Override
			    protected void preDraw(final GLState pGLState, final Camera pCamera)
			    {
			        super.preDraw(pGLState, pCamera);
			        pGLState.enableDither();
			    }
		};
		
		level_background = new Sprite(0, 0, mBackgroundTexture, vertexBufferObjectManager){
			 @Override
			    protected void preDraw(final GLState pGLState, final Camera pCamera)
			    {
			        super.preDraw(pGLState, pCamera);
			        pGLState.enableDither();
			    }
		};
		
		options_background = new Sprite(0, 0, mBackgroundTexture, vertexBufferObjectManager){
			 @Override
			    protected void preDraw(final GLState pGLState, final Camera pCamera)
			    {
			        super.preDraw(pGLState, pCamera);
			        pGLState.enableDither();
			    }
		};
		
		menu_scene.registerTouchArea(start_button);
		menu_scene.registerTouchArea(level);
		menu_scene.registerTouchArea(option);

		menu_scene.attachChild(menu_background);
		menu_scene.attachChild(start_button);
		menu_scene.attachChild(level);
		menu_scene.attachChild(option);

	}
	
	/*------------------------------------------------------------------------------------------------------------------*/
	/* Load Level Select */
	public void loadLevelSelect(){
		level_scene = new Scene();
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/level/");
		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
		
		BitmapTextureAtlas levelTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 1024, 512, TextureOptions.BILINEAR);
		BitmapTextureAtlas textTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 128, 1024, TextureOptions.BILINEAR);
		
		ITextureRegion oneTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "1.png", 0, 0);
		ITextureRegion twoTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "2.png", 145, 0);
		ITextureRegion threeTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "3.png", 290, 0);
		ITextureRegion fourTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "4.png", 435, 0);
		ITextureRegion fiveTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "5.png", 580, 0);
		ITextureRegion sixTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "6.png", 725, 0);
		ITextureRegion sevenTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "7.png", 870, 0);
		ITextureRegion eightTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "8.png", 0, 145);
		ITextureRegion nineTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "9.png", 145, 145);
		ITextureRegion tenTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "10.png", 290, 145);
		ITextureRegion elevenTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "11.png", 435, 145);
		ITextureRegion twelveTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "12.png", 580, 145);
		ITextureRegion thirteenTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "13.png", 725, 145);
		ITextureRegion fourteenTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "14.png", 870, 145);
		ITextureRegion fifteenTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "15.png", 0, 290);
		ITextureRegion levelTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(textTextureAtlas, this, "select_level.png", 0, 0);
	
		levelTextureAtlas.load();
		textTextureAtlas.load();
		
		Sprite level_text = new Sprite(71, 71, levelTexture, vertexBufferObjectManager);
		Sprite one = new Sprite(215, 71, oneTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(1,true);
				}
				return true;
			}
		};
		
		Sprite two = new Sprite(431, 71, twoTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(2,true);
				}
				return true;
			}
		};
		
		Sprite three = new Sprite(647, 71, threeTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(3,true);
				}
				return true;
			}
		};
		
		Sprite four = new Sprite(863, 71, fourTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(4,true);
				}
				return true;
			}
		};
		
		Sprite five = new Sprite(1079, 71, fiveTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(5,true);
				}
				return true;
			}
		};
		Sprite six = new Sprite(215,287, sixTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(6,true);
				}
				return true;
			}
		};
		
		Sprite seven = new Sprite(431, 287, sevenTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(7,true);
				}
				return true;
			}
		};
		
		Sprite eight = new Sprite(647, 287, eightTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(8,true);
				}
				return true;
			}
		};
		
		Sprite nine = new Sprite(863, 287, nineTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(9,true);
				}
				return true;
			}
		};
		
		Sprite ten = new Sprite(1079, 287, tenTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(10,true);
				}
				return true;
			}
		};
		
		Sprite eleven = new Sprite(215, 503, elevenTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(11,true);
				}
				return true;
			}
		};
		
		Sprite twelve = new Sprite(431, 503, twelveTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(12,true);
				}
				return true;
			}
		};
		
		Sprite thirteen = new Sprite(647, 503, thirteenTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(13,true);
				}
				return true;
			}
		};
		
		Sprite fourteen = new Sprite(863, 503, fourteenTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(14,true);
				}
				return true;
			}
		};
		
		Sprite fifteen = new Sprite(1079, 503, fifteenTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP){
					setLevel(15,true);
				}
				return true;
			}
		};
		
		level_scene.registerTouchArea(one);
		level_scene.registerTouchArea(two);
		level_scene.registerTouchArea(three);
		level_scene.registerTouchArea(four);
		level_scene.registerTouchArea(five);
		level_scene.registerTouchArea(six);
		level_scene.registerTouchArea(seven);
		level_scene.registerTouchArea(eight);
		level_scene.registerTouchArea(nine);
		level_scene.registerTouchArea(ten);
		level_scene.registerTouchArea(eleven);		
		level_scene.registerTouchArea(twelve);
		level_scene.registerTouchArea(thirteen);
		level_scene.registerTouchArea(fourteen);
		level_scene.registerTouchArea(fifteen);
		
		level_scene.attachChild(level_background);
		level_scene.attachChild(level_text);
		level_scene.attachChild(one);
		level_scene.attachChild(two);
		level_scene.attachChild(three);
		level_scene.attachChild(four);
		level_scene.attachChild(five);
		level_scene.attachChild(six);
		level_scene.attachChild(seven);
		level_scene.attachChild(eight);
		level_scene.attachChild(nine);
		level_scene.attachChild(ten);
		level_scene.attachChild(eleven);
		level_scene.attachChild(twelve);
		level_scene.attachChild(thirteen);
		level_scene.attachChild(fourteen);
		level_scene.attachChild(fifteen);
	}
	
	/*------------------------------------------------------------------------------------------------------------------*/
	/* Load Options Menu */
	public void loadOptions(){
		option_scene = new Scene();
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/background/");
		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
		
		BitmapTextureAtlas optionsTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 1024, 512, TextureOptions.BILINEAR);
		BitmapTextureAtlas levelTextureAtlas = new BitmapTextureAtlas(this.getTextureManager(), 512, 1024, TextureOptions.BILINEAR);
		
		ITextureRegion levelTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(levelTextureAtlas, this, "select_background.png", 0, 0);
		ITextureRegion woodTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(optionsTextureAtlas, this, "wood_icon.png", 0, 0);
		ITextureRegion lsuTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(optionsTextureAtlas, this, "lsu_icon.png", 236, 0);
		ITextureRegion calTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(optionsTextureAtlas, this, "cal_icon.png", 472, 0);
		ITextureRegion carbonTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(optionsTextureAtlas, this, "carbon_icon.png", 708, 0);
		ITextureRegion slateTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(optionsTextureAtlas, this, "slate_icon.png", 0, 236);
		ITextureRegion grassTexture = BitmapTextureAtlasTextureRegionFactory.createFromAsset(optionsTextureAtlas, this, "grass_icon.png", 236, 236);

		optionsTextureAtlas.load();
		levelTextureAtlas.load();
		
		Sprite level = new Sprite(0, 0, levelTexture, vertexBufferObjectManager); 
		Sprite wood = new Sprite(431, 71, woodTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				bg_select = "wood.png";				
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP)
				    mEngine.setScene(menu_scene);
				return true;
			}
		};
		
		Sprite lsu = new Sprite(719, 71, lsuTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				bg_select = "lsu.png";			
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP)
				    mEngine.setScene(menu_scene);
				return true;
			}
		};
		
		Sprite cal = new Sprite(1007, 71, calTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				bg_select = "cal.png";				
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP)
				    mEngine.setScene(menu_scene);
				return true;
			}
		};
		
		Sprite carbon = new Sprite(431, 431, carbonTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				bg_select = "carbon.png";					
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP)
				    mEngine.setScene(menu_scene);
				return true;
			}
		};
		
		Sprite slate = new Sprite(719, 431, slateTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				bg_select = "slate.png";					
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP)
				    mEngine.setScene(menu_scene);
				return true;
			}
		};
		
		Sprite grass = new Sprite(1007, 431, grassTexture, vertexBufferObjectManager) 
		{
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY){
				bg_select = "grass.png";				
				if(pSceneTouchEvent.getAction() == TouchEvent.ACTION_UP)
				    mEngine.setScene(menu_scene);
				return true;
			}
		};
		
		option_scene.registerTouchArea(wood);
		option_scene.registerTouchArea(cal);
		option_scene.registerTouchArea(lsu);
		option_scene.registerTouchArea(carbon);
		option_scene.registerTouchArea(slate);
		option_scene.registerTouchArea(grass);
		
		option_scene.attachChild(options_background);
		option_scene.attachChild(level);
		option_scene.attachChild(wood);
		option_scene.attachChild(cal);
		option_scene.attachChild(lsu);
		option_scene.attachChild(carbon);
		option_scene.attachChild(slate);
		option_scene.attachChild(grass);
		
	}
	
	/*------------------------------------------------------------------------------------------------------------------*/
	/* Load Next Level */
	public void nextLevel(){
		currLevel++;
		total_time += time;
		total_tries += tries;		
		
		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();

		//Font level_font = FontFactory.create(this.getFontManager(), this.getTextureManager(), 256, 256, Typeface.create(Typeface.DEFAULT, Typeface.BOLD),125, Color.WHITE);
		this.mFont = FontFactory.create(this.getFontManager(), this.getTextureManager(), 256, 256, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 72, Color.WHITE);
		this.mFont.load();
		//level_font.load();

		result_scene = new Scene();
		result_scene.setBackground(new Background(0,0,0));
		final Text result = new Text(100, 40, this.mFont, "Level Time: " + time + " seconds" + "\n" + "Level Attempts: " + tries + "\n\n" + "Total Time: " + total_time + " seconds" + "\n" + "Total Attempts: " + total_tries, new TextOptions(HorizontalAlign.LEFT), vertexBufferObjectManager);
    	result.setPosition((CAMERA_WIDTH - result.getWidth()) * 0.5f, (CAMERA_HEIGHT - result.getHeight()) * 0.5f);
		result_scene.attachChild(result);
		
		mEngine.setScene(result_scene);
			
    	mEngine.registerUpdateHandler(new TimerHandler(3f, new ITimerCallback() {
            @Override
            public void onTimePassed(final TimerHandler pTimerHandler) {
                    // Unregister this timer handler
                    mEngine.unregisterUpdateHandler(pTimerHandler);
                    if(currLevel == 15){
                    	Scene over = new Scene();
                		over.setBackground(new Background(0,0,0));
                		final Text game_over = new Text(100, 40, mFont, "You beat the game!", new TextOptions(HorizontalAlign.LEFT), vertexBufferObjectManager);
                    	game_over.setPosition((CAMERA_WIDTH - result.getWidth()) * 0.5f, (CAMERA_HEIGHT - result.getHeight()) * 0.5f);
                    	over.attachChild(game_over);
                    	mEngine.setScene(over);
                    	mEngine.registerUpdateHandler(new TimerHandler(1.5f, new ITimerCallback() {
                            @Override
                            public void onTimePassed(final TimerHandler pTimerHandler) {
                                mEngine.unregisterUpdateHandler(pTimerHandler);
                            	mEngine.setScene(menu_scene);
                            }
                    	})
                    	);	  
                    }
                    else{
                    setLevel(currLevel, false);
                  }
            }
    	})
    	);
	}
	
	public void setLevel(int curr_lvl, boolean reset){
		currLevel = curr_lvl;
		Font level_font = FontFactory.create(Tilt.this.getFontManager(), Tilt.this.getTextureManager(), 256, 256, Typeface.create(Typeface.DEFAULT, Typeface.BOLD),125, Color.WHITE);
		level_font.load();
		final Scene level = new Scene();
		level.setBackground(new Background(0,0,0));
		final Text lev = new Text(100, 40, level_font, "Level " + currLevel, new TextOptions(HorizontalAlign.CENTER), this.getVertexBufferObjectManager());
    	lev.setPosition((CAMERA_WIDTH - lev.getWidth()) * 0.5f, (CAMERA_HEIGHT - lev.getHeight()) * 0.5f);
    	level.attachChild(lev);
		mEngine.setScene(level);
		
		if(reset == true){
			total_time = 0;
			total_tries = 0;
		}
		tries = 0;
		time = 0;
		loadLevel("level" + curr_lvl+ ".lvl");
		ball_body.setActive(false);
		mEngine.registerUpdateHandler(new TimerHandler(1.5f, new ITimerCallback() {
			@Override
	        public void onTimePassed(final TimerHandler pTimerHandler) {
				// Unregister this timer handler
	            mEngine.unregisterUpdateHandler(pTimerHandler);
	            mEngine.setScene(scene);
	        }
	    })
	    );
		mEngine.registerUpdateHandler(new TimerHandler(2f, new ITimerCallback() {
	        @Override
	        public void onTimePassed(final TimerHandler pTimerHandler) {
	            // Unregister this timer handler
	            mEngine.unregisterUpdateHandler(pTimerHandler);
	            ball_body.setActive(true);
	            if(hardcore == true){
	            	if(ball != null){
	            		ball_blink(ball);
	            	}
	            	if(ball2 != null){
	            		ball_blink(ball2);
	            	}
	            }
	            start = System.currentTimeMillis();
	        }
	    })
	    );
	}
	public LoopEntityModifier make_move_line (float startX, float endX, float startY, float endY, float duration, Body body){
		final LoopEntityModifier entityModifier =
				new LoopEntityModifier(
						new SequenceEntityModifier(
								new MoveBodyModifier(duration, startX, endX, startY, endY, body),
								new MoveBodyModifier(duration, endX, startX, endY, startY, body)
						)
				);
		return entityModifier;
	}
	
	public LoopEntityModifier make_move_square (float startX, float startY, float side, boolean direction, float duration, Body body){
		SequenceEntityModifier sequence;
		float dur = duration/4;
		if(direction == true){
			sequence = new SequenceEntityModifier(
						new MoveBodyModifier(dur, startX, startX+side, startY, startY, body),
						new MoveBodyModifier(dur, startX+side, startX+side, startY, startY+side, body),
						new MoveBodyModifier(dur, startX+side, startX, startY+side, startY+side, body),
						new MoveBodyModifier(dur, startX, startX, startY+side, startY, body)
					);
		}
		else {
			sequence = new SequenceEntityModifier(
						new MoveBodyModifier(dur, startX, startX, startY, startY+side, body),
						new MoveBodyModifier(dur, startX, startX+side, startY+side, startY+side, body),
						new MoveBodyModifier(dur, startX+side, startX+side, startY+side, startY, body),
						new MoveBodyModifier(dur, startX+side, startX, startY, startY, body)
					);
		}
		
		final LoopEntityModifier entityModifier =
				new LoopEntityModifier(sequence);
		return entityModifier;
	}
	
	public class MoveBodyModifier extends MoveModifier{
		Body mBody;
		public MoveBodyModifier (float pDuration, float pFromX, float pToX, float pFromY, float pToY, Body body) {
			super(pDuration,pFromX,pToX,pFromY,pToY);
			mBody = body;
		}
		@Override
		protected void onSetInitialValues(final IEntity pEntity, final float pX, final float pY) {
			mBody.setTransform((pX+40)/32, (pY+40)/32, mBody.getAngle());
		}

		@Override
		protected void onSetValues(final IEntity pEntity, final float pPercentageDone, final float pX, final float pY) {
			mBody.setTransform((pX+40)/32, (pY+40)/32, mBody.getAngle());
		}
		
	}
	
	public void ball_blink(final AnimatedSprite ball_){
		ball_.setVisible(true);
		if(ball_==ball){
			ball_timer = new TimerHandler(1f, true, new ITimerCallback() {
	        @Override
	        public void onTimePassed(final TimerHandler pTimerHandler) {
	            // Unregister this timer handler
	            //mEngine.unregisterUpdateHandler(pTimerHandler);
	            ball_.setVisible(true);
	            mEngine.registerUpdateHandler(new TimerHandler(.3f, new ITimerCallback() {
	    	        @Override
	    	        public void onTimePassed(final TimerHandler pTimerHandler) {
	    	            // Unregister this timer handler
	    	            mEngine.unregisterUpdateHandler(pTimerHandler);
	    	        	ball_.setVisible(false);
	    	        }
	    	    })
	    	    );
	        }
	    });
	mEngine.registerUpdateHandler(ball_timer);
	}
		if(ball_==ball2){
			ball_timer2 = new TimerHandler(1f, true, new ITimerCallback() {
	        @Override
	        public void onTimePassed(final TimerHandler pTimerHandler) {
	            // Unregister this timer handler
	            //mEngine.unregisterUpdateHandler(pTimerHandler);
	            ball_.setVisible(true);
	            mEngine.registerUpdateHandler(new TimerHandler(.3f, new ITimerCallback() {
	    	        @Override
	    	        public void onTimePassed(final TimerHandler pTimerHandler) {
	    	            // Unregister this timer handler
	    	            mEngine.unregisterUpdateHandler(pTimerHandler);
	    	        	ball_.setVisible(false);
	    	        }
	    	    })
	    	    );
	        }
	    });
	mEngine.registerUpdateHandler(ball_timer2);
	}
	}
	
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		  // Collect data from the intent and use it
		hardcore = data.getBooleanExtra("hc", false);
		}
}

