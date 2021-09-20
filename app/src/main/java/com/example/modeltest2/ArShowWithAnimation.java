package com.example.modeltest2;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.filament.gltfio.Animator;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ArShowWithAnimation extends AppCompatActivity {

    private static final String TAG = ArShowWithAnimation.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private Renderable renderable;


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

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_ar_show_with_animation);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        WeakReference<ArShowWithAnimation> weakActivity = new WeakReference<>(this);
        ModelRenderable.builder()
                .setSource(
                        this,
                        Uri.parse(
//                "https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
                                "models/a03.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            ArShowWithAnimation activity = weakActivity.get();
                            if (activity != null) {
                                activity.renderable = modelRenderable;
                            }
                        })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load Tiger renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (renderable == null) {
                        return;
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());


                    // Create the transformable model and add it to the anchor.
                    Quaternion rotation = Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 180f);

                    TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
                    model.getScaleController().setMaxScale(0.02f);
                    model.getScaleController().setMinScale(0.01f);
                    model.setLocalRotation(rotation);

                    model.setParent(anchorNode);
                    model.setRenderable(renderable);

                    model.select();

                    FilamentAsset filamentAsset = model.getRenderableInstance().getFilamentAsset();
                    if (filamentAsset.getAnimator().getAnimationCount() > 0) {
                        animators.add(new AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
                    }

                    Color color = colors.get(nextColor);
                    nextColor++;
                    for (int i = 0; i < renderable.getSubmeshCount(); ++i) {
                        Material material = renderable.getMaterial(i);
                        material.setFloat4("baseColorFactor", color);
                    }

                    Node tigerTitleNode = new Node();
                    tigerTitleNode.setParent(model);
                    tigerTitleNode.setEnabled(false);
                    tigerTitleNode.setLocalPosition(new Vector3(0.0f, 1.0f, 0.0f));
                    ViewRenderable.builder()
                            .setView(this, R.layout.tiger_card_view)
                            .build()
                            .thenAccept(
                                    (renderable) -> {
                                        tigerTitleNode.setRenderable(renderable);
                                        tigerTitleNode.setEnabled(true);
                                    })
                            .exceptionally(
                                    (throwable) -> {
                                        throw new AssertionError("Could not load card view.", throwable);
                                    }
                            );
                });

        arFragment
                .getArSceneView()
                .getScene()
                .addOnUpdateListener(
                        frameTime -> {
                            Long time = System.nanoTime();
                            for (AnimationInstance animator : animators) {
                                animator.animator.applyAnimation(
                                        animator.index,
                                        (float) ((time - animator.startTime) / (double) SECONDS.toNanos(1))
                                                % animator.duration);
                                animator.animator.updateBoneMatrices();
                            }
                        });
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
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

//    @Override
//    public void onSessionConfiguration(Session session, Config config) {
//        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
//
//        if (useSingleImage) {
//            Bitmap augmentedImageBitmap = loadAugmentedImageBitmap();
//            if (augmentedImageBitmap == null) {
//            }
//
//            database = new AugmentedImageDatabase(session);
//            database.addImage("image_name", augmentedImageBitmap);
//            // If the physical size of the image is known, you can instead use:
//            //     augmentedImageDatabase.addImage("image_name", augmentedImageBitmap, widthInMeters);
//            // This will improve the initial detection speed. ARCore will still actively estimate the
//            // physical size of the image as it is viewed from multiple viewpoints.
//        } else {
//            // This is an alternative way to initialize an AugmentedImageDatabase instance,
//            // load a pre-existing augmented image database.
//            try (InputStream is = getAssets().open("all_images.imgdb")) {
//                database = AugmentedImageDatabase.deserialize(session, is);
//            } catch (IOException e) {
//                Log.e("TAG", "IO exception loading augmented image database.", e);
//            }
//        }
//
//        config.setAugmentedImageDatabase(database);
//        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);
//
//    }
//
//    private Bitmap loadAugmentedImageBitmap() {
//        try (InputStream is = getAssets().open("default.jpg")) {
//            return BitmapFactory.decodeStream(is);
//        } catch (IOException e) {
//            Log.e("TAG", "IO exception loading augmented image bitmap.", e);
//        }
//        return null;
//    }
//
//
//    public void onUpdate(FrameTime frameTime) {
//
//        if (page1Detected && page2Detected && page3Detected && page4Detected)
//            return;
//
//        Frame frame = arFragment.getArSceneView().getArFrame();
//        try {
//            // This is collection of all images from our AugmentedImageDatabase that are currently detected by ARCore in this session
//            Collection<AugmentedImage> augmentedImageCollection = frame.getUpdatedTrackables(AugmentedImage.class);
//
//            for (AugmentedImage image : augmentedImageCollection) {
//                if (image.getTrackingState() == TrackingState.TRACKING) {
//                    arFragment.getPlaneDiscoveryController().hide();
//
//                    if (!page1Detected && image.getName().equals("a03")) {
//                        page1Detected = true;
//                        Toast.makeText(this, "page1 tag detected", Toast.LENGTH_LONG).show();
//
//                        WeakReference<ArShowActivity2> weakActivity = new WeakReference<>(this);
//                        ModelRenderable.builder()
//                                .setSource(this, Uri.parse("models/a01-old.glb"))
//                                .setIsFilamentGltf(true)
//                                .build()
//                                .thenAccept(rabbitModel -> {
//                                    ArShowActivity2 activity = weakActivity.get();
//                                    if (activity != null) {
//
//                                        // Setting anchor to the center of AR tag
//                                        AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));
//
//                                        arFragment.getArSceneView().getScene().addChild(anchorNode);
//
//                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
//                                        modelNode.setParent(anchorNode);
//                                        modelNode.setRenderable(rabbitModel);
//
//                                        // Removing shadows
////                                        modelNode.getRenderableInstance().setShadowCaster(true);
////                                        modelNode.getRenderableInstance().setShadowReceiver(true);
//                                    }
//                                })
//                                .exceptionally(
//                                        throwable -> {
//                                            Toast.makeText(this, "Unable to load Rabbit model", Toast.LENGTH_LONG).show();
//                                            return null;
//                                        });
//                    }
//
//                    if (!page2Detected && image.getName().equals("a04")) {
//                        page2Detected = true;
//                        Toast.makeText(this, "page2 tag detected", Toast.LENGTH_LONG).show();
//
//                        WeakReference<ArShowActivity2> weakActivity = new WeakReference<>(this);
//                        ModelRenderable.builder()
//                                .setSource(this, Uri.parse("models/a02-old.glb"))
//                                .setIsFilamentGltf(true)
//                                .build()
//                                .thenAccept(rabbitModel -> {
//                                    ArShowActivity2 activity = weakActivity.get();
//                                    if (activity != null) {
//
//                                        // Setting anchor to the center of AR tag
//                                        AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));
//
//                                        arFragment.getArSceneView().getScene().addChild(anchorNode);
//
//                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
//                                        modelNode.setParent(anchorNode);
//                                        modelNode.setRenderable(rabbitModel);
//
//                                        // Removing shadows
////                                        modelNode.getRenderableInstance().setShadowCaster(true);
////                                        modelNode.getRenderableInstance().setShadowReceiver(true);
//                                    }
//                                })
//                                .exceptionally(
//                                        throwable -> {
//                                            Toast.makeText(this, "Unable to load Horse model", Toast.LENGTH_LONG).show();
//                                            return null;
//                                        });
//                    }
//
//                    if (!page3Detected && image.getName().equals("a05")) {
//                        page3Detected = true;
//                        Toast.makeText(this, "page3 tag detected", Toast.LENGTH_LONG).show();
//
//                        WeakReference<ArShowActivity2> weakActivity = new WeakReference<>(this);
//                        ModelRenderable.builder()
//                                .setSource(this, Uri.parse("models/a03-old.glb"))
//                                .setIsFilamentGltf(true)
//                                .build()
//                                .thenAccept(rabbitModel -> {
//                                    ArShowActivity2 activity = weakActivity.get();
//                                    if (activity != null) {
//
//                                        // Setting anchor to the center of AR tag
//                                        AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));
//
//                                        arFragment.getArSceneView().getScene().addChild(anchorNode);
//
//                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
//                                        modelNode.setParent(anchorNode);
//                                        modelNode.setRenderable(rabbitModel);
//
//                                        // Removing shadows
////                                        modelNode.getRenderableInstance().setShadowCaster(true);
////                                        modelNode.getRenderableInstance().setShadowReceiver(true);
//                                    }
//                                })
//                                .exceptionally(
//                                        throwable -> {
//                                            Toast.makeText(this, "Unable to load Monkey model", Toast.LENGTH_LONG).show();
//                                            return null;
//                                        });
//                    }
//
//                    if (!page4Detected && image.getName().equals("a06")) {
//                        page4Detected = true;
//                        Toast.makeText(this, "page4 tag detected", Toast.LENGTH_LONG).show();
//
//                        WeakReference<ArShowActivity2> weakActivity = new WeakReference<>(this);
//                        ModelRenderable.builder()
//                                .setSource(this, Uri.parse("models/a04-old.glb"))
//                                .setIsFilamentGltf(true)
//                                .build()
//                                .thenAccept(rabbitModel -> {
//                                    ArShowActivity2 activity = weakActivity.get();
//                                    if (activity != null) {
//
//                                        // Setting anchor to the center of AR tag
//                                        AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));
//
//                                        arFragment.getArSceneView().getScene().addChild(anchorNode);
//
//                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
//                                        modelNode.setParent(anchorNode);
//                                        modelNode.setRenderable(rabbitModel);
//
//                                        // Removing shadows
////                                        modelNode.getRenderableInstance().setShadowCaster(true);
////                                        modelNode.getRenderableInstance().setShadowReceiver(true);
//                                    }
//                                })
//                                .exceptionally(
//                                        throwable -> {
//                                            Toast.makeText(this, "Unable to load Monkey model", Toast.LENGTH_LONG).show();
//                                            return null;
//                                        });
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//


}
