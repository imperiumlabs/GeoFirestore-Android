package org.imperiumlabs.geofirestoreexample

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.android.synthetic.main.activity_main.*
import org.imperiumlabs.geofirestore.GeoFirestore
import org.imperiumlabs.geofirestore.callbacks.GeoQueryDataEventListener
import org.imperiumlabs.geofirestore.callbacks.SingleGeoQueryDataEventListener
import org.imperiumlabs.geofirestore.extension.getLocation
import org.imperiumlabs.geofirestore.extension.setLocation

class MainActivity: AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val POST_COLLECTION = "POSTS"

        private val QUERY_CENTER = GeoPoint(37.7853889, -122.4056973)
        private const val QUERY_RADIUS = 5.0
    }

    private val db = FirebaseFirestore.getInstance()
    private val posts = db.collection(POST_COLLECTION)
    private val geoFirestore = GeoFirestore(posts)

    private val postList = arrayListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, postList)
        post_list_view.adapter = adapter

        testSimpleGeoQuery()
    }

    private fun testSimpleGeoQuery() {
        val singleGeoQuery = geoFirestore.getAtLocation(QUERY_CENTER, QUERY_RADIUS)
        singleGeoQuery.addSingleGeoQueryEventListener(object : SingleGeoQueryDataEventListener {
            override fun onSuccess(documentSnapshots: List<DocumentSnapshot>) {
                documentSnapshots.forEach {
                    val desc = it["DESCRIPTION"] as? String
                    Log.i(TAG, "onSuccess: $desc")
                    adapter.insert(desc, adapter.count)
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onError(exception: Exception) {
                Log.e(TAG, "onError: ", exception)
            }
        })
    }

    private fun testGeoQuery() {
        val geoQuery = geoFirestore.queryAtLocation(QUERY_CENTER, QUERY_RADIUS)
        geoQuery.addGeoQueryDataEventListener(object : GeoQueryDataEventListener {
            override fun onDocumentEntered(documentSnapshot: DocumentSnapshot, location: GeoPoint) {
                val desc = documentSnapshot["DESCRIPTION"] as? String
                Log.i(TAG, "onDocumentEntered: $desc")
                adapter.insert(desc, adapter.count)
                adapter.notifyDataSetChanged()
            }

            override fun onDocumentExited(documentSnapshot: DocumentSnapshot) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDocumentMoved(documentSnapshot: DocumentSnapshot, location: GeoPoint) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDocumentChanged(documentSnapshot: DocumentSnapshot, location: GeoPoint) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onGeoQueryReady() {
                Log.i(TAG, "onGeoQueryReady: ")
            }

            override fun onGeoQueryError(exception: Exception) {
                Log.e(TAG, "onGeoQueryError: ", exception)
            }
        })
    }
}