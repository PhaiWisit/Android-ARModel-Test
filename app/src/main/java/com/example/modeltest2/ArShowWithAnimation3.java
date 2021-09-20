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
import android.view.View;
import android.widget.Button;
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
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.Sun;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ArShowWithAnimation3 extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnSessionConfigurationListener,
        ArFragment.OnViewCreatedListener {


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

    private boolean isDetected1 = false;
    private boolean isDetected2 = false;

    private boolean arIsShow = false;
    private String currentImageName = "";
    Frame frame2;
    AnchorNode anchorNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_show_with_animation3);

        Button button = findViewById(R.id.button4);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
                for (Node node : children) {
                    if (node instanceof AnchorNode) {
                        if (((AnchorNode) node).getAnchor() != null) {
                            ((AnchorNode) node).getAnchor().detach();
                        }
                    }
                    if (!(node instanceof Camera) && !(node instanceof Sun)) {
                        node.setParent(null);
                    }
                }




            }
        });

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
    public void onViewCreated(ArFragment arFragment, ArSceneView arSceneView) {
        Renderer renderer = arSceneView.getRenderer();
        if (renderer != null) {
            renderer.getFilamentView().setColorGrading(new ColorGrading.Builder().toneMapping(ColorGrading.ToneMapping.FILMIC).build(EngineInstance.getEngine().getFilamentEngine()));
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

//        Frame frame = arFragment.getArSceneView().getArFrame();
        frame2 = arFragment.getArSceneView().getArFrame();
        try {
            // This is collection of all images from our AugmentedImageDatabase that are currently detected by ARCore in this session
            Collection<AugmentedImage> augmentedImageCollection = frame2.getUpdatedTrackables(AugmentedImage.class);
            WeakReference<ArShowWithAnimation3> weakActivity = new WeakReference<>(this);

            for (AugmentedImage image : augmentedImageCollection){
                if (image.getTrackingState() == TrackingState.TRACKING) {
                    arFragment.getPlaneDiscoveryController().hide();
                    weakActivity = new WeakReference<>(this);

                    String imageName = image.getName();

                    Long time = System.nanoTime();
                    for (ArShowWithAnimation3.AnimationInstance animator : animators) {
                        animator.animator.applyAnimation(
                                animator.index,
                                (float) ((time - animator.startTime) / (double) SECONDS.toNanos(1))
                                        % animator.duration);
                        animator.animator.updateBoneMatrices();
                    }


                    if (!imageName.equals(currentImageName) && !page1Detected) {
                        page1Detected = true;
                        frame2 = arFragment.getArSceneView().getArFrame();
                            Toast.makeText(this, "page1 tag detected", Toast.LENGTH_LONG).show();
//                            RemoveModels(weakActivity,image);
//                            removeAnchorNode(anchorNode);
                        anchorNode = null;

                        ShowModels(weakActivity,image);
                            currentImageName = imageName;
                    }
                    if (!imageName.equals(currentImageName) && !page2Detected) {

                        page2Detected = true;
                        Toast.makeText(this, "page2 tag detected", Toast.LENGTH_LONG).show();
//                        RemoveModels(weakActivity,image);
//                        removeAnchorNode(anchorNode);
                        anchorNode = null;
                        ShowModels(weakActivity,image);
                        currentImageName = imageName;
                    }

                    if (!imageName.equals(currentImageName) && !page3Detected) {

                        page3Detected = true;
                        Toast.makeText(this, "page3 tag detected", Toast.LENGTH_LONG).show();
//                        RemoveModels(weakActivity,image);
//                        removeAnchorNode(anchorNode);
                        anchorNode = null;
                        ShowModels(weakActivity,image);
                        currentImageName = imageName;
                    }

                    if (!imageName.equals(currentImageName) && !page4Detected) {

                        page4Detected = true;
                        Toast.makeText(this, "page4 tag detected", Toast.LENGTH_LONG).show();
//                        RemoveModels(weakActivity,image);
//                        removeAnchorNode(anchorNode);
                        anchorNode = null;
                        ShowModels(weakActivity,image);
                        currentImageName = imageName;
                    }


                }
            }

//            for (AugmentedImage image : augmentedImageCollection) {
//                if (image.getTrackingState() == TrackingState.TRACKING) {
//                    arFragment.getPlaneDiscoveryController().hide();
//                    weakActivity = new WeakReference<>(this);
//
//                    if (!page1Detected && image.getName().equals("a03")) {
//                        page1Detected = true;
//                        Toast.makeText(this, "page1 tag detected", Toast.LENGTH_LONG).show();
//
////                        WeakReference<ArShowWithAnimation2> weakActivity = new WeakReference<>(this);
//                        WeakReference<ArShowWithAnimation3> finalWeakActivity = weakActivity;
//                        ModelRenderable.builder()
//                                .setSource(this, Uri.parse("models/a03.glb"))
//                                .setIsFilamentGltf(true)
//                                .build()
//                                .thenAccept(rabbitModel -> {
//                                    ArShowWithAnimation3 activity = finalWeakActivity.get();
//                                    activity.renderable = rabbitModel;
//                                    if (activity != null) {
//
//                                        // Setting anchor to the center of AR tag
//                                        AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));
//
//                                        arFragment.getArSceneView().getScene().addChild(anchorNode);
//
//                                        Quaternion rotation = Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 180f);
//
//                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
//                                        modelNode.setParent(anchorNode);
//                                        modelNode.setRenderable(rabbitModel);
//                                        modelNode.getScaleController().setMaxScale(0.02f);
//                                        modelNode.getScaleController().setMinScale(0.01f);
//                                        modelNode.setLocalRotation(rotation);
//
//                                        // Removing shadows
//                                        modelNode.getRenderableInstance().setShadowCaster(true);
//                                        modelNode.getRenderableInstance().setShadowReceiver(true);
//
//                                        modelNode.setRenderable(renderable);
//
//                                        modelNode.select();
//
////                                            FilamentAsset filamentAsset = modelNode.getRenderableInstance().getFilamentAsset();
//                                        FilamentAsset filamentAsset = modelNode.getRenderableInstance().getFilamentAsset();
//
//                                        if (filamentAsset.getAnimator().getAnimationCount() > 0) {
//                                            animators.add(new ArShowWithAnimation3.AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
//                                        }
//
//                                        Color color = colors.get(nextColor);
//                                        nextColor++;
//                                        for (int i = 0; i < renderable.getSubmeshCount(); ++i) {
//                                            Material material = renderable.getMaterial(i);
//                                            material.setFloat4("baseColorFactor", color);
//                                        }
//                                    } else {
//                                        finalWeakActivity.clear();
//                                    }
//
//                                })
//                                .exceptionally(
//                                        throwable -> {
//                                            Toast.makeText(this, "Unable to load Horse model", Toast.LENGTH_LONG).show();
//                                            return null;
//                                        });
//                    }
//
//                    if (!page2Detected && image.getName().equals("a04")) {
//                        page2Detected = true;
//                        Toast.makeText(this, "page2 tag detected", Toast.LENGTH_LONG).show();
//
////                        WeakReference<ArShowWithAnimation2> weakActivity = new WeakReference<>(this);
//                        WeakReference<ArShowWithAnimation3> finalWeakActivity1 = weakActivity;
//                        ModelRenderable.builder()
//                                .setSource(this, Uri.parse("models/a04.glb"))
//                                .setIsFilamentGltf(true)
//                                .build()
//                                .thenAccept(rabbitModel -> {
//                                    ArShowWithAnimation3 activity = finalWeakActivity1.get();
//                                    activity.renderable = rabbitModel;
//                                    if (activity != null) {
//
//                                        // Setting anchor to the center of AR tag
//                                        AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));
//
//                                        arFragment.getArSceneView().getScene().addChild(anchorNode);
//
//                                        Quaternion rotation = Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 180f);
//
//                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
//                                        modelNode.setParent(anchorNode);
//                                        modelNode.setRenderable(rabbitModel);
//                                        modelNode.getScaleController().setMaxScale(0.02f);
//                                        modelNode.getScaleController().setMinScale(0.01f);
//                                        modelNode.setLocalRotation(rotation);
//
//                                        // Removing shadows
//                                        modelNode.getRenderableInstance().setShadowCaster(true);
//                                        modelNode.getRenderableInstance().setShadowReceiver(true);
//
//                                        modelNode.setRenderable(renderable);
//
//                                        modelNode.select();
//
////                                            FilamentAsset filamentAsset = modelNode.getRenderableInstance().getFilamentAsset();
//                                        FilamentAsset filamentAsset = modelNode.getRenderableInstance().getFilamentAsset();
//
//                                        if (filamentAsset.getAnimator().getAnimationCount() > 0) {
//                                            animators.add(new ArShowWithAnimation3.AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
//                                        }
//
//                                        Color color = colors.get(nextColor);
//                                        nextColor++;
//                                        for (int i = 0; i < renderable.getSubmeshCount(); ++i) {
//                                            Material material = renderable.getMaterial(i);
//                                            material.setFloat4("baseColorFactor", color);
//                                        }
//                                    } else {
//                                        finalWeakActivity1.clear();
//                                    }
//
//                                })
//                                .exceptionally(
//                                        throwable -> {
//                                            Toast.makeText(this, "Unable to load Horse model", Toast.LENGTH_LONG).show();
//                                            return null;
//                                        });
//                    }
//
//                    if (!page3Detected && image.getName().equals("a05")) {
//
//                        page3Detected = true;
//                        Toast.makeText(this, "page3 tag detected", Toast.LENGTH_LONG).show();
//
////                        WeakReference<ArShowWithAnimation2> weakActivity = new WeakReference<>(this);
//                        WeakReference<ArShowWithAnimation3> finalWeakActivity2 = weakActivity;
//                        ModelRenderable.builder()
//                                .setSource(this, Uri.parse("models/a05.glb"))
//                                .setIsFilamentGltf(true)
//                                .build()
//                                .thenAccept(rabbitModel -> {
//                                    ArShowWithAnimation3 activity = finalWeakActivity2.get();
//                                    activity.renderable = rabbitModel;
//                                    if (activity != null) {
//
//                                        // Setting anchor to the center of AR tag
//                                        AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));
//
//                                        arFragment.getArSceneView().getScene().addChild(anchorNode);
//
//                                        Quaternion rotation = Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 180f);
//
//                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
//                                        modelNode.setParent(anchorNode);
//                                        modelNode.setRenderable(rabbitModel);
//                                        modelNode.getScaleController().setMaxScale(0.02f);
//                                        modelNode.getScaleController().setMinScale(0.01f);
//                                        modelNode.setLocalRotation(rotation);
//
//                                        // Removing shadows
//                                        modelNode.getRenderableInstance().setShadowCaster(true);
//                                        modelNode.getRenderableInstance().setShadowReceiver(true);
//
//                                        modelNode.setRenderable(renderable);
//
//                                        modelNode.select();
//
////                                            FilamentAsset filamentAsset = modelNode.getRenderableInstance().getFilamentAsset();
//                                        FilamentAsset filamentAsset = modelNode.getRenderableInstance().getFilamentAsset();
//
//                                        if (filamentAsset.getAnimator().getAnimationCount() > 0) {
//                                            animators.add(new ArShowWithAnimation3.AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
//                                        }
//
//                                        Color color = colors.get(nextColor);
//                                        nextColor++;
//                                        for (int i = 0; i < renderable.getSubmeshCount(); ++i) {
//                                            Material material = renderable.getMaterial(i);
//                                            material.setFloat4("baseColorFactor", color);
//                                        }
//                                    } else {
//                                        finalWeakActivity2.clear();
//                                    }
//
//
//                                })
//                                .exceptionally(
//                                        throwable -> {
//                                            Toast.makeText(this, "Unable to load Monkey model", Toast.LENGTH_LONG).show();
//                                            return null;
//                                        });
//                    }
//
//
//                    /** This section 4 test **/
//
//                    Long time = System.nanoTime();
//                    for (ArShowWithAnimation3.AnimationInstance animator : animators) {
//                        animator.animator.applyAnimation(
//                                animator.index,
//                                (float) ((time - animator.startTime) / (double) SECONDS.toNanos(1))
//                                        % animator.duration);
//                        animator.animator.updateBoneMatrices();
//                    }
//
//                    if (!page4Detected && image.getName().equals("a06")) {
//                        page4Detected = true;
//                        Toast.makeText(this, "page4 tag detected", Toast.LENGTH_LONG).show();
//
////                            WeakReference<ArShowWithAnimation2> weakActivity3 = new WeakReference<>(this);
//                        WeakReference<ArShowWithAnimation3> finalWeakActivity3 = weakActivity;
//                        ModelRenderable.builder()
//                                .setSource(this, Uri.parse("models/a06.glb"))
//                                .setIsFilamentGltf(true)
//                                .build()
//                                .thenAccept(rabbitModel -> {
//                                    ArShowWithAnimation3 activity = finalWeakActivity3.get();
//                                    activity.renderable = rabbitModel;
//                                    if (activity != null) {
//
//                                        // Setting anchor to the center of AR tag
//                                        AnchorNode anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));
//
//                                        arFragment.getArSceneView().getScene().addChild(anchorNode);
//
//                                        Quaternion rotation = Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 180f);
//
//                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
//                                        modelNode.setParent(anchorNode);
//                                        modelNode.setRenderable(rabbitModel);
//                                        modelNode.getScaleController().setMaxScale(0.02f);
//                                        modelNode.getScaleController().setMinScale(0.01f);
//                                        modelNode.setLocalRotation(rotation);
//
//                                        // Removing shadows
//                                        modelNode.getRenderableInstance().setShadowCaster(true);
//                                        modelNode.getRenderableInstance().setShadowReceiver(true);
//
//                                        modelNode.setRenderable(renderable);
//
//                                        modelNode.select();
//
////                                            FilamentAsset filamentAsset = modelNode.getRenderableInstance().getFilamentAsset();
//                                        FilamentAsset filamentAsset = modelNode.getRenderableInstance().getFilamentAsset();
//
//                                        if (filamentAsset.getAnimator().getAnimationCount() > 0) {
//                                            animators.add(new ArShowWithAnimation3.AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
//                                        }
//
//                                        Color color = colors.get(nextColor);
//                                        nextColor++;
//                                        for (int i = 0; i < renderable.getSubmeshCount(); ++i) {
//                                            Material material = renderable.getMaterial(i);
//                                            material.setFloat4("baseColorFactor", color);
//                                        }
//                                    } else {
//                                        finalWeakActivity3.clear();
//                                    }
//
//                                })
//                                .exceptionally(
//                                        throwable -> {
//                                            Log.e("throwable","Unable to load Monkey model",throwable);
//                                            Toast.makeText(this, "Unable to load Monkey model", Toast.LENGTH_LONG).show();
//                                            return null;
//                                        });
//
//                    }
//
//
//                    /** End of test **/
//
//                }
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeAnchorNode(AnchorNode nodeToremove) {
        //Remove an anchor node
        if (nodeToremove != null) {
            arFragment.getArSceneView().getScene().removeChild(nodeToremove);
            nodeToremove.getAnchor().detach();
            nodeToremove.setParent(null);
            nodeToremove = null;
            Toast.makeText(getApplicationContext(), "Test Delete - anchorNode removed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Test Delete - markAnchorNode was null", Toast.LENGTH_SHORT).show();
        }
    }

    private void RemoveModels(WeakReference<ArShowWithAnimation3> weakActivity,AugmentedImage image){
        List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
        for (Node node : children) {
            if (node instanceof AnchorNode) {
                if (((AnchorNode) node).getAnchor() != null) {
                    ((AnchorNode) node).getAnchor().detach();
                    Toast.makeText(getApplicationContext(),"detach",Toast.LENGTH_SHORT).show();
                }
            }
//            if (!(node instanceof Camera) && !(node instanceof Sun)) {
//                arFragment.getArSceneView().getScene().removeChild(node);
//                Toast.makeText(getApplicationContext(),"remove",Toast.LENGTH_SHORT).show();
//
//            }
        }
    }

    private void ShowModels(WeakReference<ArShowWithAnimation3> weakActivity,AugmentedImage image){

        WeakReference<ArShowWithAnimation3> finalWeakActivity = weakActivity;
        ModelRenderable.builder()
                .setSource(this, Uri.parse("models/" + image.getName() + ".glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(rabbitModel -> {
                    ArShowWithAnimation3 activity = finalWeakActivity.get();
                    activity.renderable = rabbitModel;
                    if (activity != null) {


                        // Setting anchor to the center of AR tag
                        anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));

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
                            animators.add(new AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
                        }

                        Color color = colors.get(nextColor);
                        nextColor++;
                        for (int i = 0; i < renderable.getSubmeshCount(); ++i) {
                            Material material = renderable.getMaterial(i);
                            material.setFloat4("baseColorFactor", color);
                        }


                        arIsShow = true;
                    }

                })
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load Horse model", Toast.LENGTH_LONG).show();
                            return null;
                        });
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

    private final Set<ArShowWithAnimation3.AnimationInstance> animators = new ArraySet<>();

    private final List<Color> colors =
            Arrays.asList(new Color(0, 0, 0, 1),
                    new Color(1, 0, 0, 1),
                    new Color(0, 1, 0, 1),
                    new Color(0, 0, 1, 1),
                    new Color(1, 1, 0, 1),
                    new Color(0, 1, 1, 1),
                    new Color(1, 0, 1, 1),
                    new Color(1, 1, 1, 1));
    private int nextColor = 0;

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