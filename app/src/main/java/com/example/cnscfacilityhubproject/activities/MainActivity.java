package com.example.cnscfacilityhubproject.activities;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cnscfacilityhubproject.R;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    Map<String, Object> test = new HashMap<>();
    FirebaseAuth auth ;
    FirebaseFirestore db ;


    TextInputLayout msgTxt;
    TextInputLayout nameTxt;
    Button insertBtn;
    Button readBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);





        // connection
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        msgTxt = findViewById(R.id.messageTxt);
        nameTxt = findViewById(R.id.nameTxt);
        insertBtn = findViewById(R.id.addBtn);
        readBtn = findViewById(R.id.readBtn);





        insertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test.put("message", msgTxt.getEditText().getText().toString());
                test.put("myName", nameTxt.getEditText().getText().toString());


                db.collection("test")
                        .add(test)
                        .addOnSuccessListener(documentReference -> {
                            Log.d("FIREBASE", "Data added");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FIREBASE", "Error", e);
                        });
            }
        });


        readBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection("test")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {


                                        Log.d(TAG, document.getId() + " => " + document.getData());
                                    }

                                } else {
                                    Log.w(TAG, "Error getting documents.", task.getException());
                                }
                            }
                        });
            }
        });








        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}