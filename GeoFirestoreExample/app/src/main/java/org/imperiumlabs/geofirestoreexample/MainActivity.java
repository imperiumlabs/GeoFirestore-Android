package org.imperiumlabs.geofirestoreexample;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import java.util.*;
import com.google.firebase.firestore.*;
import org.imperiumlabs.geofirestore.*;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements GeoQueryDataEventListener{

    private static final String POST_COLLECTION  = "POSTS";

    private static final GeoPoint QUERY_CENTER = new GeoPoint(36.963817, -122.018284);
    private static final double QUERY_RADIUS = 5;

    private ListView listView;
    private ArrayList<String> postList = new ArrayList<>();

    private GeoFirestore geoFirestore;
    private GeoQuery geoQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.post_list_view);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        CollectionReference posts = db.collection(POST_COLLECTION);

        // Create a GeoFirestore instance
        this.geoFirestore = new GeoFirestore(posts);

        /*
        * Currently 5 posts exist in the database with the following descriptions and locations:
        *
        * "It is a beautiful day here at the beach!", GeoPoint(36.963719, -122.020704)
        *
        * "I just saw a huge fish while swimming!", GeoPoint(36.961182, -122.014996)
        *
        * "I want to build a sand castle as big as our home!", GeoPoint(36.963805, -122.017722)
        *
        * "I have a view of the entire city!", GeoPoint(40.748441, -73.985664)
        *
        * "This is the happiest place on Earth!", GeoPoint(33.812092, -117.918974)
        *
        */

        // Create a GeoQuery to find all posts within a radius of a certain location
        this.geoQuery = geoFirestore.queryAtLocation(QUERY_CENTER, QUERY_RADIUS);

        this.geoQuery.removeAllListeners();

        // Listen to documents regarding this GeoQuery
        this.geoQuery.addGeoQueryDataEventListener(this);
    }


    @Override
    public void onDocumentEntered(DocumentSnapshot documentSnapshot, GeoPoint location) {
        try {
            Map<String, Object> data = documentSnapshot.getData();
            String description = (String) data.get("DESCRIPTION");
            if (description != null){
                postList.add(description);
            }
        } catch (NullPointerException e) {
            Log.e("DOCUMENT_ERROR", e.getLocalizedMessage());
        } catch (ClassCastException e) {
            Log.e("DOCUMENT_ERROR", e.getLocalizedMessage());
        }
    }

    @Override
    public void onDocumentExited(DocumentSnapshot documentSnapshot) {}
    @Override
    public void onDocumentMoved(DocumentSnapshot documentSnapshot, GeoPoint location) {}
    @Override
    public void onDocumentChanged(DocumentSnapshot documentSnapshot, GeoPoint location) {}

    @Override
    public void onGeoQueryReady() {
        this.geoQuery.removeGeoQueryEventListener(this);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, postList);
        listView.setAdapter(adapter);
    }

    @Override
    public void onGeoQueryError(Exception exception) {
        Log.e("QUERY_ERROR", exception.getLocalizedMessage());
    }


}
