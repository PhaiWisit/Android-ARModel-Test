package com.example.modeltest2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.filament.ColorGrading;
import com.google.android.filament.Material;
import com.google.android.filament.gltfio.Animator;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.EngineInstance;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.Renderer;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ArShowWithoutAnimation extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener {

    private ArFragment arFragment;

    private boolean matrixDetected = false;
    private boolean rabbitDetected = false;

    private boolean page1Detected = false;
    private boolean page2Detected = false;
    private boolean page3Detected = false;
    private boolean page4Detected = false;


    private AugmentedImageDatabase database;

    private final boolean useSingleImage = false;
    private Renderable renderable;

    private Renderable plainVideoModel;
    private Material plainVideoMaterial;
    private MediaPlayer mediaPlayer;

    private static class AnimationInstance {
        Animator animator;
        Long startTime;
        float duration;
        int index;

        AnimationInstance(Animator animator, int index, Long startTime) {
            this.animator = animator;
            this.startTime = startTime;
            this.duration = animator.getAnimationDuration(index);
            this.index = index;
        }
    }

    private final Set<AnimationInstance> animators = new ArraySet<>();

    private final List<Color> colors =
            Arrays.asList(
                    new Color(0, 0, 0, 1),
                    new Color(1, 0, 0, 1),
                    new Color(0, 1, 0, 1),
                    new Color(0, 0, 1, 1),
                    new Color(1, 1, 0, 1),
                    new Color(0, 1, 1, 1),
                    new Color(1, 0, 1, 1),
                    new Color(1, 1, 1, 1));
    private int nextColor = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ar_show_without_animation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            ((ViewGroup.MarginLayoutParams) toolbar.getLayoutParams()).topMargin = insets.getSystemWindowInsetTop();
            return insets.consumeSystemWindowInsets();
        });
        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }

        // .glb models can be loaded at runtime when needed or when app starts
        // This method loads ModelRenderable when app starts
//        loadMatrixModel();
//        loadMatrixMaterial();
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
            arFragment.setOnViewCreatedListener(this);
        }
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        // Disable plane detection
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);

        if (useSingleImage) {
            Bitmap augmentedImageBitmap = loadAugmentedImageBitmap();
            if (augmentedImageBitmap == null) {
            }

            database = new AugmentedImageDatabase(session);
            database.addImage("image_name", augmentedImageBitmap);
            // If the physical size of the image is known, you can instead use:
            //     augmentedImageDatabase.addImage("image_name", augmentedImageBitmap, widthInMeters);
            // This will improve the initial detection speed. ARCore will still actively estimate the
            // physical size of the image as it is viewed from multiple viewpoints.
        } else {
            // This is an alternative way to initialize an AugmentedImageDatabase instance,
            // load a pre-existing augmented image database.
            try (InputStream is = getAssets().open("all_images.imgdb")) {
                database = AugmentedImageDatabase.deserialize(session, is);
            } catch (IOException e) {
                Log.e("TAG", "IO exception loading augmented image database.", e);
            }
        }

        config.setAugmentedImageDatabase(database);

        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);



    }

    private Bitmap loadAugmentedImageBitmap() {
        try (InputStream is = getAssets().open("default.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e("TAG", "IO exception loading augmented image bitmap.", e);
        }
        return null;
    }

    @Override
    public void onViewCreated(ArFragment arFragment, ArSceneView arSceneView) {
        // Currently, the tone-mapping should be changed to FILMIC
        // because with other tone-mapping operators except LINEAR
        // the inverseTonemapSRGB function in the materials can produce incorrect results.
        // The LINEAR tone-mapping cannot be used together with the inverseTonemapSRGB function.
        Renderer renderer = arSceneView.getRenderer();

        if (renderer != null) {
            renderer.getFilamentView().setColorGrading(
                    new ColorGrading.Builder()
                            .toneMapping(ColorGrading.ToneMapping.FILMIC)
                            .build(EngineInstance.getEngine().getFilamentEngine())
            );
        }

        // Hide plane indicating dots
        arSceneView.getPlaneRenderer().setVisible(false);
        // Disable the rendering of detected planes.
        arSceneView.getPlaneRenderer().setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
    }

    // Every time new image is processed by ARCore and ready, this method is called
    public void onUpdate(FrameTime frameTime) {

        if (page1Detected && page2Detected && page3Detected && page4Detected)
            return;

        Frame frame = arFragment.getArSceneView().getArFrame();
        try {
            // This is collection of all images from our AugmentedImageDatabase that are currently detected by ARCore in this session
            Collection<AugmentedImage> augmentedImageCollection = frame.getUpdatedTrackables(AugmentedImage.class);

            for (AugmentedImage image : augmentedImageCollection) {
                if (image.getTrackingState() == TrackingState.TRACKING) {
                    arFragment.getPlaneDiscoveryController().hide();

                    if (!page1Detected && image.getName().equals("a03")) {
                        page1Detected = true;
                        Toast.makeText(this, "page1 tag detected", Toast.LENGTH_LONG).show();

                        WeakReference<ArShowWithoutAnimation> weakActivity = new WeakReference<>(this);
                        ModelRenderable.builder()
                                .setSource(this, Uri.parse("models/a01-old.glb"))
                                .setIsFilamentGltf(true)
                                .build()
                                .thenAccept(rabbitModel -> {
                                    ArShowWithoutAnimation activity = weakActivity.get();
                                    if (activity != null) {

                                        // Setting anchor to the center of AR tag
                                        AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));

                                        arFragment.getArSceneView().getScene().addChild(anchorNode);

                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                                        modelNode.setParent(anchorNode);
                                        modelNode.setRenderable(rabbitModel);

                                        // Removing shadows
                                        modelNode.getRenderableInstance().setShadowCaster(true);
                                        modelNode.getRenderableInstance().setShadowReceiver(true);
                                    }
                                })
                                .exceptionally(
                                        throwable -> {
                                            Toast.makeText(this, "Unable to load Horse model", Toast.LENGTH_LONG).show();
                                            return null;
                                        });
                    }

                    if (!page2Detected && image.getName().equals("a04")) {
                        page2Detected = true;
                        Toast.makeText(this, "page2 tag detected", Toast.LENGTH_LONG).show();

                        WeakReference<ArShowWithoutAnimation> weakActivity = new WeakReference<>(this);
                        ModelRenderable.builder()
                                .setSource(this, Uri.parse("models/a02-old.glb"))
                                .setIsFilamentGltf(true)
                                .build()
                                .thenAccept(rabbitModel -> {
                                    ArShowWithoutAnimation activity = weakActivity.get();
                                    if (activity != null) {

                                        // Setting anchor to the center of AR tag
                                        AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));

                                        arFragment.getArSceneView().getScene().addChild(anchorNode);

                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                                        modelNode.setParent(anchorNode);
                                        modelNode.setRenderable(rabbitModel);

                                        // Removing shadows
                                        modelNode.getRenderableInstance().setShadowCaster(true);
                                        modelNode.getRenderableInstance().setShadowReceiver(true);
                                    }
                                })
                                .exceptionally(
                                        throwable -> {
                                            Toast.makeText(this, "Unable to load Horse model", Toast.LENGTH_LONG).show();
                                            return null;
                                        });
                    }

                    if (!page3Detected && image.getName().equals("a05")) {
                        page3Detected = true;
                        Toast.makeText(this, "page3 tag detected", Toast.LENGTH_LONG).show();

                        WeakReference<ArShowWithoutAnimation> weakActivity = new WeakReference<>(this);
                        ModelRenderable.builder()
                                .setSource(this, Uri.parse("models/a03-old.glb"))
                                .setIsFilamentGltf(true)
                                .build()
                                .thenAccept(rabbitModel -> {
                                    ArShowWithoutAnimation activity = weakActivity.get();
                                    if (activity != null) {

                                        // Setting anchor to the center of AR tag
                                        AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));

                                        arFragment.getArSceneView().getScene().addChild(anchorNode);

                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                                        modelNode.setParent(anchorNode);
                                        modelNode.setRenderable(rabbitModel);

                                        // Removing shadows
                                        modelNode.getRenderableInstance().setShadowCaster(true);
                                        modelNode.getRenderableInstance().setShadowReceiver(true);
                                    }
                                })
                                .exceptionally(
                                        throwable -> {
                                            Toast.makeText(this, "Unable to load Monkey model", Toast.LENGTH_LONG).show();
                                            return null;
                                        });
                    }

                    if (!page4Detected && image.getName().equals("a06")) {
                        page4Detected = true;
                        Toast.makeText(this, "page4 tag detected", Toast.LENGTH_LONG).show();

                        WeakReference<ArShowWithoutAnimation> weakActivity = new WeakReference<>(this);
                        ModelRenderable.builder()
                                .setSource(this, Uri.parse("models/a04-old.glb"))
                                .setIsFilamentGltf(true)
                                .build()
                                .thenAccept(rabbitModel -> {
                                    ArShowWithoutAnimation activity = weakActivity.get();
                                    if (activity != null) {

                                        // Setting anchor to the center of AR tag
                                        AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));

                                        arFragment.getArSceneView().getScene().addChild(anchorNode);

                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                                        modelNode.setParent(anchorNode);
                                        modelNode.setRenderable(rabbitModel);

                                        // Removing shadows
                                        modelNode.getRenderableInstance().setShadowCaster(true);
                                        modelNode.getRenderableInstance().setShadowReceiver(true);
                                    }
                                })
                                .exceptionally(
                                        throwable -> {
                                            Toast.makeText(this, "Unable to load Monkey model", Toast.LENGTH_LONG).show();
                                            return null;
                                        });
                    }


                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}