package com.example.modeltest2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;

import com.google.android.filament.ColorGrading;
import com.google.android.filament.gltfio.Animator;
import com.google.android.filament.gltfio.FilamentAsset;
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
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.EngineInstance;
import com.google.ar.sceneform.rendering.Material;
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

import static java.util.concurrent.TimeUnit.SECONDS;

public class ArShowWithAnimation2 extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener {


    /**
      This class has a function of scan to show model with animation
      It has work in onUpdate method, in section 4 test.
      That import glb file with animation name is a03.glb
      Can test function by scan book 1 page butterfly: index page about 4 or 5?

      (☞ﾟヮﾟ)☞  (❁´◡`❁)  ☜(ﾟヮﾟ☜)
     * **/

    private static final String TAG = ArShowWithAnimation.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private Renderable renderable;
    private final boolean useSingleImage = false;
    private AugmentedImageDatabase database;

    private boolean page1Detected = false;
    private boolean page2Detected = false;
    private boolean page3Detected = false;
    private boolean page4Detected = false;



    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            Log.w("onAttach","fragment == ux_fragment");
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
            arFragment.setOnViewCreatedListener(this);
        }
    }

    @Override
    public void onViewCreated(ArFragment arFragment, ArSceneView arSceneView) {
        Renderer renderer = arSceneView.getRenderer();

        if (renderer != null) {
            Log.w("onViewCreated","renderer != null");
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
    public void onSessionConfiguration(Session session, Config config) {
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
        if (useSingleImage) {
            Bitmap augmentedImageBitmap = loadAugmentedImageBitmap();
            if (augmentedImageBitmap == null) {
            }
            database = new AugmentedImageDatabase(session);
            database.addImage("image_name", augmentedImageBitmap);
        } else {
            try (InputStream is = getAssets().open("all_images.imgdb")) {
                database = AugmentedImageDatabase.deserialize(session, is);
            } catch (IOException e) {
                Log.e("TAG", "IO exception loading augmented image database.", e);
            }
        }
        config.setAugmentedImageDatabase(database);

        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);
    }

    public void onUpdate(FrameTime frameTime) {

        if (page1Detected && page2Detected && page3Detected && page4Detected)
            return;

        Frame frame = arFragment.getArSceneView().getArFrame();
        try {
            // This is collection of all images from our AugmentedImageDatabase that are currently detected by ARCore in this session
            Collection<AugmentedImage> augmentedImageCollection = frame.getUpdatedTrackables(AugmentedImage.class);


            WeakReference<ArShowWithAnimation2> weakActivity = new WeakReference<>(this);
//            WeakReference<ArShowWithAnimation2> weakActivity2 = new WeakReference<>(this);
//            WeakReference<ArShowWithAnimation2> weakActivity3 = new WeakReference<>(this);



            for (AugmentedImage image : augmentedImageCollection) {
                if (image.getTrackingState() == TrackingState.TRACKING) {
                    arFragment.getPlaneDiscoveryController().hide();
                     weakActivity = new WeakReference<>(this);

                    if (!page1Detected && image.getName().equals("a03")) {
                        page1Detected = true;
                        Toast.makeText(this, "page1 tag detected", Toast.LENGTH_LONG).show();

//                        WeakReference<ArShowWithAnimation2> weakActivity = new WeakReference<>(this);
                        WeakReference<ArShowWithAnimation2> finalWeakActivity = weakActivity;
                        ModelRenderable.builder()
                                .setSource(this, Uri.parse("models/a03.glb"))
                                .setIsFilamentGltf(true)
                                .build()
                                .thenAccept(rabbitModel -> {
                                    ArShowWithAnimation2 activity = finalWeakActivity.get();
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

//                        WeakReference<ArShowWithAnimation2> weakActivity = new WeakReference<>(this);
                        WeakReference<ArShowWithAnimation2> finalWeakActivity1 = weakActivity;
                        ModelRenderable.builder()
                                .setSource(this, Uri.parse("models/a02-old.glb"))
                                .setIsFilamentGltf(true)
                                .build()
                                .thenAccept(rabbitModel -> {
                                    ArShowWithAnimation2 activity = finalWeakActivity1.get();
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

//                        WeakReference<ArShowWithAnimation2> weakActivity = new WeakReference<>(this);
                        WeakReference<ArShowWithAnimation2> finalWeakActivity2 = weakActivity;
                        ModelRenderable.builder()
                                .setSource(this, Uri.parse("models/a03-old.glb"))
                                .setIsFilamentGltf(true)
                                .build()
                                .thenAccept(rabbitModel -> {
                                    ArShowWithAnimation2 activity = finalWeakActivity2.get();
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


                    /** This section 4 test **/

                    Long time = System.nanoTime();
                    for (ArShowWithAnimation2.AnimationInstance animator : animators) {
                        animator.animator.applyAnimation(
                                animator.index,
                                (float) ((time - animator.startTime) / (double) SECONDS.toNanos(1))
                                        % animator.duration);
                        animator.animator.updateBoneMatrices();
                    }

                        if (!page4Detected && image.getName().equals("a06")) {
                            page4Detected = true;
                            Toast.makeText(this, "page4 tag detected", Toast.LENGTH_LONG).show();

//                            WeakReference<ArShowWithAnimation2> weakActivity3 = new WeakReference<>(this);
                            WeakReference<ArShowWithAnimation2> finalWeakActivity3 = weakActivity;
                            ModelRenderable.builder()
                                    .setSource(this, Uri.parse("models/a03.glb"))
                                    .setIsFilamentGltf(true)
                                    .build()
                                    .thenAccept(rabbitModel -> {
                                        ArShowWithAnimation2 activity = finalWeakActivity3.get();
                                        activity.renderable = rabbitModel;
                                        if (activity != null) {

                                            // Setting anchor to the center of AR tag
                                            AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));

                                            arFragment.getArSceneView().getScene().addChild(anchorNode);

                                            Quaternion rotation = Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 180f);

                                            TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                                            modelNode.setParent(anchorNode);
                                            modelNode.setRenderable(rabbitModel);
                                            modelNode.getScaleController().setMaxScale(0.02f);
                                            modelNode.getScaleController().setMinScale(0.01f);
                                            modelNode.setLocalRotation(rotation);

                                            // Removing shadows
                                            modelNode.getRenderableInstance().setShadowCaster(true);
                                            modelNode.getRenderableInstance().setShadowReceiver(true);

                                            modelNode.setRenderable(renderable);

                                            modelNode.select();

//                                            FilamentAsset filamentAsset = modelNode.getRenderableInstance().getFilamentAsset();
                                            FilamentAsset filamentAsset = modelNode.getRenderableInstance().getFilamentAsset();

                                            if (filamentAsset.getAnimator().getAnimationCount() > 0) {
                                                animators.add(new ArShowWithAnimation2.AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
                                            }

                                            Color color = colors.get(nextColor);
                                            nextColor++;
                                            for (int i = 0; i < renderable.getSubmeshCount(); ++i) {
                                                Material material = renderable.getMaterial(i);
                                                material.setFloat4("baseColorFactor", color);
                                            }
                                        } else {
                                            finalWeakActivity3.clear();
                                        }

                                    })
                                    .exceptionally(
                                            throwable -> {
                                                Log.e("throwable","Unable to load Monkey model",throwable);
                                                Toast.makeText(this, "Unable to load Monkey model", Toast.LENGTH_LONG).show();
                                                return null;
                                            });




                        }


                    /** End of test **/

                }

//                if (image.getTrackingState() == TrackingState.PAUSED){
//                    weakActivity.clear();
//                }
//
//                if (image.getTrackingState() == TrackingState.STOPPED){
//                    Log.w("TrackingState","STOPPED");
//                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



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

    private final Set<ArShowWithAnimation2.AnimationInstance> animators = new ArraySet<>();

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

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_ar_show_without_animation);
//


        getSupportFragmentManager().addFragmentOnAttachListener(this);

//        arFragment = (ArFragment)
//                getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }



//        WeakReference<ArShowWithAnimation2> weakActivity = new WeakReference<>(this);
//        ModelRenderable.builder()
//                .setSource(
//                        this,
//                        Uri.parse(
////                "https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
//                                "models/a03.glb"))
//                .setIsFilamentGltf(true)
//                .build()
//                .thenAccept(
//                        modelRenderable -> {
//                            ArShowWithAnimation2 activity = weakActivity.get();
//                            if (activity != null) {
//                                activity.renderable = modelRenderable;
//                            }
//                        })
//                .exceptionally(
//                        throwable -> {
//                            Toast toast =
//                                    Toast.makeText(this, "Unable to load Tiger renderable", Toast.LENGTH_LONG);
//                            toast.setGravity(Gravity.CENTER, 0, 0);
//                            toast.show();
//                            return null;
//                        });

//        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
//                    if (renderable == null) {
//                        return;
//                    }
//
//                    // Create the Anchor.
//                    Anchor anchor = hitResult.createAnchor();
//                    AnchorNode anchorNode = new AnchorNode(anchor);
//                    anchorNode.setParent(arFragment.getArSceneView().getScene());
//
//
//                    // Create the transformable model and add it to the anchor.
//                    Quaternion rotation = Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 180f);
//
//                    TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
//                    model.getScaleController().setMaxScale(0.02f);
//                    model.getScaleController().setMinScale(0.01f);
//                    model.setLocalRotation(rotation);
//
//                    model.setParent(anchorNode);
//                    model.setRenderable(renderable);
//
//                    model.select();
//
//                    FilamentAsset filamentAsset = model.getRenderableInstance().getFilamentAsset();
//                    if (filamentAsset.getAnimator().getAnimationCount() > 0) {
//                        animators.add(new ArShowWithAnimation2.AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
//                    }
//
//                    Color color = colors.get(nextColor);
//                    nextColor++;
//                    for (int i = 0; i < renderable.getSubmeshCount(); ++i) {
//                        Material material = renderable.getMaterial(i);
//                        material.setFloat4("baseColorFactor", color);
//                    }
//
//                    Node tigerTitleNode = new Node();
//                    tigerTitleNode.setParent(model);
//                    tigerTitleNode.setEnabled(false);
//                    tigerTitleNode.setLocalPosition(new Vector3(0.0f, 1.0f, 0.0f));
//                    ViewRenderable.builder()
//                            .setView(this, R.layout.tiger_card_view)
//                            .build()
//                            .thenAccept(
//                                    (renderable) -> {
//                                        tigerTitleNode.setRenderable(renderable);
//                                        tigerTitleNode.setEnabled(true);
//                                    })
//                            .exceptionally(
//                                    (throwable) -> {
//                                        throw new AssertionError("Could not load card view.", throwable);
//                                    }
//                            );
//                });

//        arFragment.getArSceneView().getScene().addOnUpdateListener(
//                        frameTime -> {
//                            Long time = System.nanoTime();
//                            for (ArShowWithAnimation2.AnimationInstance animator : animators) {
//                                animator.animator.applyAnimation(
//                                        animator.index,
//                                        (float) ((time - animator.startTime) / (double) SECONDS.toNanos(1))
//                                                % animator.duration);
//                                animator.animator.updateBoneMatrices();
//                            }
//                        });


    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    private Bitmap loadAugmentedImageBitmap() {
        try (InputStream is = getAssets().open("default.jpg")) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e("TAG", "IO exception loading augmented image bitmap.", e);
        }
        return null;
    }
}
