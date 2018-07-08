package org.imperiumlabs.geofirestoreexample;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.util.*;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;
import org.imperiumlabs.geofirestore.GeoFirestore;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference posts = db.collection("posts");
        final GeoFirestore geoFirestore = new GeoFirestore(posts);

        Map<String, Object> post1 = new HashMap<>();
        post1.put("description", "I went fishing in my home town!");
        post1.put("likes", 5);
        Map<String, Object> post2 = new HashMap<>();
        post2.put("description", "I am on my way to the high school!");
        post2.put("likes", 1);
        Map<String, Object> post3 = new HashMap<>();
        post3.put("description", "I have a view of the entire city!");
        post3.put("likes", 3);

        posts.add(post1).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (task.isSuccessful()){
                    geoFirestore.setLocation(task.getResult().getId(), new GeoPoint(37.264091, -122.022821));
                }
            }
        });
        posts.add(post2).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (task.isSuccessful()){
                    geoFirestore.setLocation(task.getResult().getId(), new GeoPoint(37.264382, -122.023787));
                }
            }
        });
        posts.add(post3).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (task.isSuccessful()){
                    geoFirestore.setLocation(task.getResult().getId(), new GeoPoint(40.748441, -73.985664));
                }
            }
        });



    }
}
