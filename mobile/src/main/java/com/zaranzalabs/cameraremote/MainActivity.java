package com.zaranzalabs.cameraremote;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private FirebaseDatabase mDatabase;

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance();

        Button button = findViewById(R.id.button);
        imageView = findViewById(R.id.imageView);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference dbReference = mDatabase.getReference("status_camera");

                dbReference.setValue(true);
            }
        });

        DatabaseReference logs = mDatabase.getReference().child("logs");
        logs.addValueEventListener(listener);
    }

    ValueEventListener listener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            Iterator<DataSnapshot> itr = dataSnapshot.getChildren().iterator();
            DataSnapshot lastElement = itr.next();

            while(itr.hasNext()) {
                lastElement=itr.next();
            }

            PictureEntry picture = lastElement.getValue(PictureEntry.class);

            loadPicture(picture);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    private void loadPicture(PictureEntry picture) {

        Glide.with(this).load(picture.image).into(imageView);
    }
}
