package com.example.modeltest2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
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

public class ArShowWithAnimation4 extends AppCompatActivity implements
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

    private boolean isShowModel = false;
    private String currentImageName = "";
    //    Frame frame2;
    AnchorNode anchorNode;
    TransformableNode modelNode;
    Button button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_show_with_animation4);


        button = findViewById(R.id.button4);

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

        button.setVisibility(View.INVISIBLE);
        button.setText("สแกนหน้าอื่น");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(ArShowWithAnimation4.this,ArShowWithAnimation4.class);
                startActivity(i);
                finish();

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

        Frame frame = arFragment.getArSceneView().getArFrame();
//        frame2 = arFragment.getArSceneView().getArFrame();
        try {
            // This is collection of all images from our AugmentedImageDatabase that are currently detected by ARCore in this session
            Collection<AugmentedImage> augmentedImageCollection = frame.getUpdatedTrackables(AugmentedImage.class);
            WeakReference<ArShowWithAnimation4> weakActivity = new WeakReference<>(this);

            for (AugmentedImage image : augmentedImageCollection){

                if (image.getTrackingState() == TrackingState.TRACKING) {
                    arFragment.getPlaneDiscoveryController().hide();
//                    weakActivity = new WeakReference<>(this);

                    button.setVisibility(View.VISIBLE);

                    String imageName = image.getName();

                    Long time = System.nanoTime();
                    for (ArShowWithAnimation4.AnimationInstance animator : animators) {
                        animator.animator.applyAnimation(
                                animator.index,
                                (float) ((time - animator.startTime) / (double) SECONDS.toNanos(1))
                                        % animator.duration);
                        animator.animator.updateBoneMatrices();
                    }

                    if (!page1Detected){
                        page1Detected = true;
                        removeAnchorNode(anchorNode);
                        ShowModels(weakActivity,image);
                    }
//                    if (!page2Detected && imageName.equals("a04")){
//                        page2Detected = true;
//                        removeAnchorNode(anchorNode);
//                        ShowModels(weakActivity,image);
//                    }
//                    if (!page3Detected && imageName.equals("a05")){
//                        page3Detected = true;
////                        weakActivity.clear();
////                        weakActivity = new WeakReference<>(this);
//                        removeAnchorNode(anchorNode);
//                        ShowModels(weakActivity,image);
//                    }
//                    if (!page4Detected && imageName.equals("a06")){
//                        page4Detected = true;
////                        weakActivity.clear();
////                        weakActivity = new WeakReference<>(this);
//                        removeAnchorNode(anchorNode);
//                        ShowModels(weakActivity,image);
//                    }

                }

            }

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
//            nodeToremove = null;
            Toast.makeText(getApplicationContext(), "Test Delete - anchorNode removed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Test Delete - markAnchorNode was null", Toast.LENGTH_SHORT).show();
        }
    }

    private void RemoveModels(){
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

    private void ShowModels(WeakReference<ArShowWithAnimation4> weakActivity,AugmentedImage image){

//        WeakReference<ArShowWithAnimation4> finalWeakActivity = weakActivity;
        ModelRenderable.builder()
                .setSource(this, Uri.parse("models/" + image.getName() + ".glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(rabbitModel -> {
                    ArShowWithAnimation4 activity = weakActivity.get();
                    activity.renderable = rabbitModel;
                    if (activity != null) {


                        // Setting anchor to the center of AR tag
                        anchorNode = new AnchorNode(image.createAnchor(image.getCenterPose()));
                        anchorNode.setParent(arFragment.getArSceneView().getScene());

//                        arFragment.getArSceneView().getScene().addChild(anchorNode);

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
                            animators.add(new ArShowWithAnimation4.AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
                        }

                        Color color = colors.get(nextColor);
                        nextColor++;
                        for (int i = 0; i < renderable.getSubmeshCount(); ++i) {
                            Material material = renderable.getMaterial(i);
                            material.setFloat4("baseColorFactor", color);
                        }


                    }

                })
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load model", Toast.LENGTH_LONG).show();
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

    private final Set<ArShowWithAnimation4.AnimationInstance> animators = new ArraySet<>();

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