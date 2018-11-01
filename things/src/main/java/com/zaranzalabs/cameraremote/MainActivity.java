package com.zaranzalabs.cameraremote;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.zaranzalabs.cameraremote.camera.ZCamera;

import java.nio.ByteBuffer;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;
    private ZCamera mCamera;

    private Handler mCameraHandler;
    private HandlerThread mCameraThread;

    private Handler mCloudHandler;
    private HandlerThread mCloudThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkSelfPermission(android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No permission");
            return;
        }

        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();
        mCamera = ZCamera.getInstance();

        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

        DatabaseReference dbReference = mDatabase.getReference("status_camera");
        dbReference.setValue(false);

        dbReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                boolean value = dataSnapshot.getValue(Boolean.class);

                if (value == true) {
                    mCamera.takePicture();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w("LOG", "Failed to read value.", error.toException());
            }
        });

        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        mCloudThread = new HandlerThread("CloudThread");
        mCloudThread.start();
        mCloudHandler = new Handler(mCloudThread.getLooper());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.shutDown();

        mCameraThread.quitSafely();
        mCloudThread.quitSafely();
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    // get image bytes
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    onPictureTaken(imageBytes);
                }
            };

    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {
            DatabaseReference dbReference = mDatabase.getReference("status_camera");
            dbReference.setValue(false);

            final DatabaseReference log = mDatabase.getReference("logs").push();
            final StorageReference imageRef = mStorage.getReference().child(log.getKey());

            UploadTask task = imageRef.putBytes(imageBytes);
            task.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();

                        Log.i(TAG, "Image upload successful");
                        log.child("timestamp").setValue(ServerValue.TIMESTAMP);
                        log.child("image").setValue(downloadUri.toString());
                    }
                }
            });

            task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // clean up this entry
                    Log.w(TAG, "Unable to upload image to Firebase");
                    log.removeValue();
                }
            });
        }
    }
}
